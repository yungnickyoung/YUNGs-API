package com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.beardifier;

import com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.adaptations.EnhancedTerrainAdaptation;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;

/**
 * Container for JigsawJunction + additional info used when processing {@link EnhancedTerrainAdaptation}s.
 */
public record EnhancedJigsawJunction(JigsawJunction jigsawJunction, EnhancedTerrainAdaptation pieceTerrainAdaptation) {
}
