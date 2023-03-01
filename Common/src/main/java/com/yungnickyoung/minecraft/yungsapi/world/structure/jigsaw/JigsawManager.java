package com.yungnickyoung.minecraft.yungsapi.world.structure.jigsaw;

import com.mojang.datafixers.util.Pair;
import com.yungnickyoung.minecraft.yungsapi.YungsApiCommon;
import com.yungnickyoung.minecraft.yungsapi.mixin.accessor.StructureTemplatePoolAccessor;
import com.yungnickyoung.minecraft.yungsapi.util.BoxOctree;
import com.yungnickyoung.minecraft.yungsapi.world.structure.context.StructureContext;
import com.yungnickyoung.minecraft.yungsapi.world.structure.jigsaw.assembler.JigsawStructureAssembler;
import com.yungnickyoung.minecraft.yungsapi.world.structure.jigsaw.element.YungJigsawSinglePoolElement;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pools.EmptyPoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;

public class JigsawManager {
    public static Optional<Structure.GenerationStub> assembleJigsawStructure(
            Structure.GenerationContext generationContext,
            Holder<StructureTemplatePool> startPool,
            Optional<ResourceLocation> startJigsawNameOptional,
            int maxDepth,
            BlockPos startPos,
            boolean useExpansionHack, // Used to be doBoundaryAdjustments
            Optional<Heightmap.Types> projectStartToHeightmap,
            int maxDistanceFromCenter, // Used to be structureBoundingBoxRadius
            Optional<Integer> maxY,
            Optional<Integer> minY
    ) {
        // Extract data from context
        RegistryAccess registryAccess = generationContext.registryAccess();
        ChunkGenerator chunkGenerator = generationContext.chunkGenerator();
        StructureTemplateManager structureManager = generationContext.structureTemplateManager();
        LevelHeightAccessor levelHeightAccessor = generationContext.heightAccessor();
        WorldgenRandom worldgenRandom = generationContext.random();
        Registry<StructureTemplatePool> registry = registryAccess.registryOrThrow(Registries.TEMPLATE_POOL);

        // Grab a random starting piece from the start pool
        Optional<PoolElementStructurePiece> startPieceOptional = getStartPiece(startPool, startJigsawNameOptional, startPos, structureManager, worldgenRandom);
        if (startPieceOptional.isEmpty()) {
            return Optional.empty();
        }
        PoolElementStructurePiece startPiece = startPieceOptional.get();

        // Grab some data regarding starting piece's bounding box & position
        BoundingBox pieceBoundingBox = startPiece.getBoundingBox();
        int pieceCenterX = (pieceBoundingBox.maxX() + pieceBoundingBox.minX()) / 2;
        int pieceCenterZ = (pieceBoundingBox.maxZ() + pieceBoundingBox.minZ()) / 2;
        int pieceCenterY = projectStartToHeightmap
                .map(types -> startPos.getY() + chunkGenerator.getFirstFreeHeight(pieceCenterX, pieceCenterZ, types, levelHeightAccessor, generationContext.randomState()))
                .orElseGet(() -> startPiece.getPosition().getY());

        int yAdjustment = pieceBoundingBox.minY() + startPiece.getGroundLevelDelta();
        startPiece.move(0, pieceCenterY - yAdjustment, 0);
        Vec3i startingPosOffset = startPos.subtract(startPiece.getPosition());
        int adjustedPieceCenterY = pieceCenterY + startingPosOffset.getY();

        // Establish max bounds of entire structure.
        // Make sure the supplied distance is large enough to cover the size of your entire structure.
        AABB aABB = new AABB(
                pieceCenterX - maxDistanceFromCenter, adjustedPieceCenterY - maxDistanceFromCenter, pieceCenterZ - maxDistanceFromCenter,
                pieceCenterX + maxDistanceFromCenter + 1, adjustedPieceCenterY + maxDistanceFromCenter + 1, pieceCenterZ + maxDistanceFromCenter + 1);
        BoxOctree maxStructureBounds = new BoxOctree(aABB); // The maximum boundary of the entire structure
        maxStructureBounds.addBox(AABB.of(pieceBoundingBox)); // Add start piece to our structure's bounds

        return Optional.of(new Structure.GenerationStub(new BlockPos(pieceCenterX, adjustedPieceCenterY, pieceCenterZ), (structurePiecesBuilder) -> {
            if (maxDepth <= 0) { // Realistically this should never be true. Why make a jigsaw config with a non-positive size?
                return;
            }

            // Create assembler + initial entry
            JigsawStructureAssembler assembler = new JigsawStructureAssembler(new JigsawStructureAssembler.Settings()
                    .poolRegistry(registry)
                    .maxDepth(maxDepth)
                    .chunkGenerator(chunkGenerator)
                    .structureTemplateManager(structureManager)
                    .randomState(generationContext.randomState())
                    .rand(worldgenRandom)
                    .maxY(maxY)
                    .minY(minY)
                    .useExpansionHack(useExpansionHack)
                    .levelHeightAccessor(levelHeightAccessor));

            // Add the start piece to the assembler & assemble the structure
            assembler.assembleStructure(startPiece, maxStructureBounds);
            assembler.addAllPiecesToStructureBuilder(structurePiecesBuilder);
        }));
    }

    /**
     * Returns a piece from the provided pool to be used as the starting piece for a structure.
     * Pieces are chosen randomly, but some conditions as well as the isPriority flag are respected.
     *
     * Note that only some conditions are supported. Conditions checking for things like piece position or orientation
     * should not be used, as instead those checks can be performed on the structure's placement itself.
     */
    private static Optional<PoolElementStructurePiece> getStartPiece(
            Holder<StructureTemplatePool> startPoolHolder,
            Optional<ResourceLocation> startJigsawNameOptional,
            BlockPos startPos,
            StructureTemplateManager structureTemplateManager,
            RandomSource rand
    ) {
        StructureTemplatePool startPool = startPoolHolder.value();
        ObjectArrayList<Pair<StructurePoolElement, Integer>> candidatePoolElements = new ObjectArrayList<>(((StructureTemplatePoolAccessor) startPool).getRawTemplates());

        // Shuffle our candidate pool elements
        Util.shuffle(candidatePoolElements, rand);

        // Get a random orientation for starting piece
        Rotation rotation = Rotation.getRandom(rand);

        // Sum of weights in all pieces in the pool.
        // When choosing a piece, we will remove its weight from this sum.
        int totalWeightSum = candidatePoolElements.stream().mapToInt(Pair::getSecond).reduce(0, Integer::sum);

        while (candidatePoolElements.size() > 0 && totalWeightSum > 0) {
            Pair<StructurePoolElement, Integer> chosenPoolElementPair = null;

            // First, check for any priority pieces
            for (Pair<StructurePoolElement, Integer> candidatePiecePair : candidatePoolElements) {
                StructurePoolElement candidatePiece = candidatePiecePair.getFirst();
                if (candidatePiece instanceof YungJigsawSinglePoolElement yungSingleElement && yungSingleElement.isPriorityPiece()) {
                    chosenPoolElementPair = candidatePiecePair;
                    break;
                }
            }

            // Randomly choose piece if priority piece wasn't selected
            if (chosenPoolElementPair == null) {
                // Random weight used to choose random piece from the pool of candidates
                int chosenWeight = rand.nextInt(totalWeightSum) + 1;

                // Randomly choose a candidate piece
                for (Pair<StructurePoolElement, Integer> candidate : candidatePoolElements) {
                    chosenWeight -= candidate.getSecond();
                    if (chosenWeight <= 0) {
                        chosenPoolElementPair = candidate;
                        break;
                    }
                }
            }

            // Extract data from the chosen piece pair.
            StructurePoolElement chosenPoolElement = chosenPoolElementPair.getFirst();
            int chosenPieceWeight = chosenPoolElementPair.getSecond();

            if (chosenPoolElement == EmptyPoolElement.INSTANCE) {
                return Optional.empty();
            }

            BlockPos blockPos;
            if (startJigsawNameOptional.isPresent()) {
                ResourceLocation name = startJigsawNameOptional.get();
                Optional<BlockPos> optional = getPosOfJigsawBlockWithName(chosenPoolElement, name, startPos, rotation, structureTemplateManager, rand);
                if (optional.isEmpty()) {
                    YungsApiCommon.LOGGER.error("No starting jigsaw with Name {} found in start pool {}", name, startPoolHolder.unwrapKey().get().location());
                    return Optional.empty();
                }

                blockPos = optional.get();
            } else {
                blockPos = startPos;
            }

            Vec3i startingPosOffset = blockPos.subtract(startPos);
            BlockPos adjustedStartPos = startPos.subtract(startingPosOffset);

            // Validate conditions for this piece, if applicable
            if (chosenPoolElement instanceof YungJigsawSinglePoolElement yungSingleElement) {
                StructureContext ctx = new StructureContext.Builder()
                        .structureTemplateManager(structureTemplateManager)
                        .pos(adjustedStartPos)
                        .rotation(rotation)
                        .depth(0)
                        .random(rand)
                        .build();
                if (!yungSingleElement.passesConditions(ctx)) {
                    totalWeightSum -= chosenPieceWeight;
                    candidatePoolElements.remove(chosenPoolElementPair);
                    continue; // Abort this piece if it doesn't pass conditions check
                }
            }

            // Instantiate piece
            return Optional.of(new PoolElementStructurePiece(
                    structureTemplateManager,
                    chosenPoolElement,
                    adjustedStartPos,
                    chosenPoolElement.getGroundLevelDelta(),
                    rotation,
                    chosenPoolElement.getBoundingBox(structureTemplateManager, adjustedStartPos, rotation)
            ));
        }
        return Optional.empty();
    }

    /**
     * Returns a jigsaw block with the specified name in the StructurePoolElement.
     * If no such jigsaw block is found, returns an empty Optional.
     *
     * This is used for starting pieces, when you want /locate to point to a position other than the
     * corner of the start piece, such as the center of ancient cities.
     */
    private static Optional<BlockPos> getPosOfJigsawBlockWithName(
            StructurePoolElement structurePoolElement,
            ResourceLocation name,
            BlockPos startPos,
            Rotation rotation,
            StructureTemplateManager structureTemplateManager,
            RandomSource rand
    ) {
        List<StructureTemplate.StructureBlockInfo> shuffledJigsawBlocks = structurePoolElement.getShuffledJigsawBlocks(structureTemplateManager, startPos, rotation, rand);
        for (StructureTemplate.StructureBlockInfo jigsawBlockInfo : shuffledJigsawBlocks) {
            ResourceLocation jigsawBlockName = ResourceLocation.tryParse(jigsawBlockInfo.nbt.getString("name"));
            if (name.equals(jigsawBlockName)) {
                return Optional.of(jigsawBlockInfo.pos);
            }
        }

        return Optional.empty();
    }

}