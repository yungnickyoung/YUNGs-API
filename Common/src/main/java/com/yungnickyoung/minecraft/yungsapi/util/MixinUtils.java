package com.yungnickyoung.minecraft.yungsapi.util;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public class MixinUtils {
    /**
     * Checks if the provided position is inside a structure that is tagged with the provided tag.
     * Thanks to TelepathicGrunt for the original implementation!
     *
     * @param worldGenRegion  the WorldGenRegion
     * @param pos             the position to check
     * @param structureTagKey the tag to check for
     * @return true if the provided position is inside a structure that is tagged with the provided tag
     */
    public static boolean isPositionInTaggedStructure(WorldGenRegion worldGenRegion, BlockPos pos, TagKey<Structure> structureTagKey) {
        Registry<Structure> structureRegistry = worldGenRegion.registryAccess().registryOrThrow(Registries.STRUCTURE);
        SectionPos sectionPos = SectionPos.of(pos);

        // Ensure chunk has generated structure references
        ChunkAccess chunkAccess = worldGenRegion.getChunk(sectionPos.x(), sectionPos.z(), ChunkStatus.STRUCTURE_REFERENCES);
        if (!chunkAccess.getHighestGeneratedStatus().isOrAfter(ChunkStatus.STRUCTURE_REFERENCES)) return false;

        // Check all structures in chunk, and return true if any match the provided tag
        Map<Structure, LongSet> allReferencesInChunk = chunkAccess.getAllReferences();
        for (Map.Entry<Structure, LongSet> entry : allReferencesInChunk.entrySet()) {
            Structure structure = entry.getKey();
            LongSet references = entry.getValue();

            Optional<ResourceKey<Structure>> structureKey = structureRegistry.getResourceKey(structure);
            boolean isTaggedStructure = structureKey.isPresent() && structureRegistry.getHolderOrThrow(structureKey.get()).is(structureTagKey);

            if (isTaggedStructure) {
                if (isAnyReferenceValidStartForStructure(worldGenRegion, structure, references, structureStart -> structureStart.getBoundingBox().isInside(pos))) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks if any of the references contain a valid structure start for the provided structure.
     * Each valid structure start must also pass the provided filter.
     * Based on vanilla's {@link StructureManager#fillStartsForStructure}.
     *
     * @param worldGenRegion the WorldGenRegion
     * @param structure      the structure to check for
     * @param references     the references to check
     * @param filter         an additional filter to apply to each structure start
     * @return true if any of the references contain a valid structure start for the provided structure
     */
    private static boolean isAnyReferenceValidStartForStructure(WorldGenRegion worldGenRegion, Structure structure, LongSet references, Predicate<StructureStart> filter) {
        StructureManager structureManager = worldGenRegion.getLevel().structureManager();

        for (long reference : references) {
            SectionPos structureStartSectionPos = SectionPos.of(new ChunkPos(reference), worldGenRegion.getMinSection());
            if (!worldGenRegion.hasChunk(structureStartSectionPos.x(), structureStartSectionPos.z())) {
                continue;
            }

            ChunkAccess structureStartChunkAccess = worldGenRegion.getChunk(structureStartSectionPos.x(), structureStartSectionPos.z(), ChunkStatus.STRUCTURE_STARTS);

            StructureStart structureStart = structureManager.getStartForStructure(structureStartSectionPos, structure, structureStartChunkAccess);
            if (structureStart != null && structureStart.isValid() && filter.test(structureStart)) {
                return true;
            }
        }

        return false;
    }
}
