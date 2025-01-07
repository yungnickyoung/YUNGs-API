package com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.beardifier;

import com.yungnickyoung.minecraft.yungsapi.mixin.BeardifierMixin;
import com.yungnickyoung.minecraft.yungsapi.mixin.accessor.BeardifierAccessor;
import com.yungnickyoung.minecraft.yungsapi.mixin.accessor.NoiseChunkAccessor;
import com.yungnickyoung.minecraft.yungsapi.world.structure.YungJigsawStructure;
import com.yungnickyoung.minecraft.yungsapi.world.structure.jigsaw.element.YungJigsawPoolElement;
import com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.adaptations.EnhancedTerrainAdaptation;
import com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.aquiferoverride.AquiferOverride;
import com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.aquiferoverride.AquiferOverrideMask;
import com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.aquiferoverride.AquiferOverrideMaskSupplier;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import java.util.List;

/**
 * A collection of static helper methods intended to be used by {@link BeardifierMixin}.
 */
public class EnhancedBeardifierHelper {
    /**
     * Attaches additional behavior for an Enhanced Beardifier, which is used for {@link EnhancedTerrainAdaptation}s.
     * @param structureManager The StructureManager
     * @param chunkPos The ChunkPos
     * @param original The original vanilla Beardifier, created by {@link Beardifier#forStructuresInChunk(StructureManager, ChunkPos)}
     * @return The original Beardifier vanilla Beardifier, with additional data for Enhanced behaviors
     */
    public static Beardifier forStructuresInChunk(StructureManager structureManager, ChunkPos chunkPos, Beardifier original) {
        ObjectList<EnhancedBeardifierRigid> enhancedBeardifierRigidList = new ObjectArrayList<>(10);
        ObjectList<EnhancedJigsawJunction> enhancedJunctionList = new ObjectArrayList<>(10);
        int chunkMinBlockX = chunkPos.getMinBlockX();
        int chunkMinBlockZ = chunkPos.getMinBlockZ();
        List<StructureStart> structureStarts = structureManager.startsForStructure(chunkPos, structure -> structure instanceof YungJigsawStructure);
        for (StructureStart structureStart : structureStarts) {
            EnhancedTerrainAdaptation structureTerrainAdaptation = ((YungJigsawStructure) structureStart.getStructure()).enhancedTerrainAdaptation;

            // Determine max kernel radius in the structure.
            // Both the structure itself and any pieces may specify an enhanced terrain adaptation, so we must
            // check everything.
            int kernelRadius = structureTerrainAdaptation.getKernelRadius();
            for (StructurePiece structurePiece : structureStart.getPieces()) {
                if (structurePiece instanceof PoolElementStructurePiece poolPiece
                        && poolPiece.getElement() instanceof YungJigsawPoolElement yungElement
                        && yungElement.getEnhancedTerrainAdaptation().isPresent()) {
                    kernelRadius = Math.max(kernelRadius, yungElement.getEnhancedTerrainAdaptation().get().getKernelRadius());
                }
            }

            int maxKernelRadius = kernelRadius;
            if (maxKernelRadius <= 0) {
                continue;
            }

            // Use max kernel radius to get list of nearby pieces for this chunk.
            // A piece is considered nearby if its bounding box, when padded by maxKernelRadius, intersects this chunk.
            List<StructurePiece> nearbyPieces = structureStart.getPieces().stream()
                    .filter(structurePiece -> structurePiece.isCloseToChunk(chunkPos, maxKernelRadius))
                    .toList();

            for (StructurePiece nearbyPiece : nearbyPieces) {
                if (nearbyPiece instanceof PoolElementStructurePiece poolElementPiece) {
                    StructureTemplatePool.Projection projection = poolElementPiece.getElement().getProjection();

                    // Check if piece overrides terrain adaptation
                    EnhancedTerrainAdaptation pieceTerrainAdaptation = structureTerrainAdaptation;
                    if (poolElementPiece.getElement() instanceof YungJigsawPoolElement yungElement && yungElement.getEnhancedTerrainAdaptation().isPresent()) {
                        pieceTerrainAdaptation = yungElement.getEnhancedTerrainAdaptation().get();
                    }

                    // If no terrain adaptation for this piece, we can ignore it
                    if (pieceTerrainAdaptation == EnhancedTerrainAdaptation.NONE) continue;

                    int pieceKernelRadius = pieceTerrainAdaptation.getKernelRadius();

                    // Add rigid for piece
                    if (projection == StructureTemplatePool.Projection.RIGID) {
                        enhancedBeardifierRigidList.add(
                                new EnhancedBeardifierRigid(
                                        poolElementPiece.getBoundingBox(),
                                        pieceTerrainAdaptation,
                                        poolElementPiece.getGroundLevelDelta(),
                                        poolElementPiece.getRotation()
                                )
                        );
                    }

                    // Add rigid for jigsaw junctions within the intersecting piece
                    for (JigsawJunction jigsawJunction : poolElementPiece.getJunctions()) {
                        int sourceX = jigsawJunction.getSourceX();
                        int sourceZ = jigsawJunction.getSourceZ();
                        // Only consider junctions which are intersecting with this chunk (padded by kernel radius)
                        if (sourceX > chunkMinBlockX - pieceKernelRadius
                                && sourceZ > chunkMinBlockZ - pieceKernelRadius
                                && sourceX < chunkMinBlockX + 15 + pieceKernelRadius
                                && sourceZ < chunkMinBlockZ + 15 + pieceKernelRadius) {
                            enhancedJunctionList.add(new EnhancedJigsawJunction(jigsawJunction, pieceTerrainAdaptation));
                        }
                    }
                } else if (structureTerrainAdaptation != EnhancedTerrainAdaptation.NONE) {
                    enhancedBeardifierRigidList.add(new EnhancedBeardifierRigid(
                            nearbyPiece.getBoundingBox(),
                            structureTerrainAdaptation,
                            0,
                            Rotation.NONE));
                }
            }
        }

        Beardifier newBeardifier = new Beardifier(((BeardifierAccessor) original).getPieceIterator(), ((BeardifierAccessor) original).getJunctionIterator());
        EnhancedBeardifierData enhancedBeardifier = (EnhancedBeardifierData) newBeardifier;
        enhancedBeardifier.setEnhancedPieceIterator(enhancedBeardifierRigidList.iterator());
        enhancedBeardifier.setEnhancedJunctionIterator(enhancedJunctionList.iterator());
        return newBeardifier;
    }

    /**
     * Computes the updated density value at the given point, accounting for noise contributions from the EnhancedBeardifierData.
     * @param ctx the density FunctionContext
     * @param density The originally computed vanilla density value at this position
     * @param data The {@link EnhancedBeardifierData} to be used in the computation of the new density value.
     * @return The new density value at the given location, accounting for additional noise contributions.
     */
    public static double computeDensity(DensityFunction.FunctionContext ctx, double density, EnhancedBeardifierData data) {
        int x = ctx.blockX();
        int y = ctx.blockY();
        int z = ctx.blockZ();
        AquiferOverride aquiferOverride = AquiferOverride.NONE;

        while (data.getEnhancedPieceIterator() != null && data.getEnhancedPieceIterator().hasNext()) {
            EnhancedBeardifierRigid rigid = data.getEnhancedPieceIterator().next();
            BoundingBox pieceBoundingBox = rigid.pieceBoundingBox();
            EnhancedTerrainAdaptation pieceTerrainAdaptation = rigid.pieceTerrainAdaptation();
            Rotation pieceRotation = rigid.rotation();

            // Apply properties from the piece's terrain adaptation to the bounding box:
            // - bottom offset
            pieceBoundingBox = pieceBoundingBox.moved(0, (int) pieceTerrainAdaptation.getBottomOffset(), 0);

            // - x/z padding
            Direction.Axis xPaddingDirection = pieceRotation.rotate(Direction.EAST).getAxis();
            int xPadding = xPaddingDirection == Direction.Axis.X ? pieceTerrainAdaptation.getPadding().x() : pieceTerrainAdaptation.getPadding().z();
            int zPadding = xPaddingDirection == Direction.Axis.X ? pieceTerrainAdaptation.getPadding().z() : pieceTerrainAdaptation.getPadding().x();
            pieceBoundingBox = pieceBoundingBox.inflatedBy(xPadding, 0, zPadding);

            // - top/bottom padding
            if (pieceTerrainAdaptation.getPadding().top() != 0) {
                pieceBoundingBox = new BoundingBox(
                        pieceBoundingBox.minX(), pieceBoundingBox.minY(), pieceBoundingBox.minZ(),
                        pieceBoundingBox.maxX(), pieceBoundingBox.maxY() + pieceTerrainAdaptation.getPadding().top(), pieceBoundingBox.maxZ());
            }
            if (pieceTerrainAdaptation.getPadding().bottom() != 0) {
                pieceBoundingBox = new BoundingBox(
                        pieceBoundingBox.minX(), pieceBoundingBox.minY() - pieceTerrainAdaptation.getPadding().bottom(), pieceBoundingBox.minZ(),
                        pieceBoundingBox.maxX(), pieceBoundingBox.maxY(), pieceBoundingBox.maxZ());
            }

            /* Get the distance from the pieceBoundingBox along each axis.
             * If within the bounding box, all of these are simply 0.
             * Notably, the below equations grab the maximum *positive* distance.
             */
            int xDistanceToBoundingBox = Math.max(0, Math.max(pieceBoundingBox.minX() - x, x - pieceBoundingBox.maxX()));
            int yDistanceToBoundingBox = Math.max(0, Math.max(pieceBoundingBox.minY() - y, y - pieceBoundingBox.maxY()));
            int zDistanceToBoundingBox = Math.max(0, Math.max(pieceBoundingBox.minZ() - z, z - pieceBoundingBox.maxZ()));
            int yDistanceToPieceBottom = y - pieceBoundingBox.minY();

            // Calculate density factor and add to density value
            double densityFactor = 0;
            if (pieceTerrainAdaptation != EnhancedTerrainAdaptation.NONE) {
                densityFactor = pieceTerrainAdaptation.computeDensityFactor(
                        xDistanceToBoundingBox,
                        yDistanceToBoundingBox,
                        zDistanceToBoundingBox,
                        yDistanceToPieceBottom
                ) * 0.8D;
            }

            density += densityFactor;
            if (densityFactor != 0 && pieceTerrainAdaptation.getAquiferOverride() != AquiferOverride.NONE) {
                aquiferOverride = pieceTerrainAdaptation.getAquiferOverride();
            }
        }
        data.getEnhancedPieceIterator().back(Integer.MAX_VALUE);

        // Vanilla logic
        while (data.getEnhancedJunctionIterator() != null && data.getEnhancedJunctionIterator().hasNext()) {
            EnhancedJigsawJunction enhancedJigsawJunction = data.getEnhancedJunctionIterator().next();
            JigsawJunction jigsawJunction = enhancedJigsawJunction.jigsawJunction();
            EnhancedTerrainAdaptation pieceTerrainAdaptation = enhancedJigsawJunction.pieceTerrainAdaptation();

            // Apply bottom offset to the junction's ground position
            int groundY = jigsawJunction.getSourceGroundY() + (int) pieceTerrainAdaptation.getBottomOffset();

            int xDistanceToJunction = x - jigsawJunction.getSourceX();
            int yDistanceToJunction = y - groundY;
            int zDistanceToJunction = z - jigsawJunction.getSourceZ();
            double densityFactor = pieceTerrainAdaptation.computeDensityFactor(
                    xDistanceToJunction,
                    yDistanceToJunction,
                    zDistanceToJunction,
                    yDistanceToJunction
            ) * 0.4D;

            density += densityFactor;
            if (densityFactor != 0 && pieceTerrainAdaptation.getAquiferOverride() != AquiferOverride.NONE) {
                aquiferOverride = pieceTerrainAdaptation.getAquiferOverride();
            }
        }
        data.getEnhancedJunctionIterator().back(Integer.MAX_VALUE);

        // Set the aquifer override mask for this position if the density was modified and if
        // the relevant EnhancedTerrainAdaptation specifies that it should override aquifer liquids.
        if (aquiferOverride != AquiferOverride.NONE) {
            updateAquiferOverrideMask(data, aquiferOverride, x, y, z);
        }

        return density;
    }

    private static void updateAquiferOverrideMask(EnhancedBeardifierData data, AquiferOverride aquiferOverride, int x, int y, int z) {
        NoiseChunk noiseChunk = data.getNoiseChunk();
        NoiseChunkAccessor noiseChunkAccessor = (NoiseChunkAccessor) noiseChunk;
        AquiferOverrideMaskSupplier aquiferOverrideMaskSupplier = (AquiferOverrideMaskSupplier) noiseChunk;

        int chunkHeight = noiseChunkAccessor.getNoiseSettings().height();
        int minY = noiseChunkAccessor.getNoiseSettings().minY();

        AquiferOverrideMask aquiferOverrideMask = aquiferOverrideMaskSupplier.getOrCreateAquiferOverrideMask(() -> new AquiferOverrideMask(chunkHeight, minY));
        aquiferOverrideMask.set(x, y, z);
        aquiferOverrideMask.setAquiferOverride(aquiferOverride);
    }
}
