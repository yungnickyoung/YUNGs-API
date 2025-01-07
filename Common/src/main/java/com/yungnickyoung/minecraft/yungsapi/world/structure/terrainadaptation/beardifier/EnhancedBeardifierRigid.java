package com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.beardifier;

import com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.adaptations.EnhancedTerrainAdaptation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;

/**
 * Equivalent to vanilla's {@link Beardifier.Rigid}, but with an {@link EnhancedTerrainAdaptation} instead of
 * vanilla's {@link TerrainAdjustment}.
 */
public record EnhancedBeardifierRigid(BoundingBox pieceBoundingBox, EnhancedTerrainAdaptation pieceTerrainAdaptation,
                                      int pieceGroundLevelDelta, Rotation rotation) {
}
