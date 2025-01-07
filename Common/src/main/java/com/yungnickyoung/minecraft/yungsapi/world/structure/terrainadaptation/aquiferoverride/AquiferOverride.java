package com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.aquiferoverride;

import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus;

/**
 * Represents an override for aquifer liquid blocks.
 * This is used to modify liquid blocks placed by aquifers in positions that are also carved by an EnhancedTerrainAdaptation.
 * Note that this only applies to positions modified by EnhancedTerrainAdaptations - it does NOT apply to all aquifer liquid blocks.
 *
 * WARNING -- THIS IS AN EXPERIMENTAL FEATURE. BEHAVIOR MAY BE INCONSISTENT, ESPECIALLY IF MIXING DIFFERENT AQUIFER OVERRIDES WITHIN THE SAME STRUCTURE.
 */
@ApiStatus.Experimental
public abstract class AquiferOverride {
    public static final AquiferOverride NONE = new NoneAquiferOverride();

    abstract public AquiferOverrideType<?> type();

    abstract public BlockState getBlockState(BlockState defaultBlockState);

    AquiferOverride() {
    }
}
