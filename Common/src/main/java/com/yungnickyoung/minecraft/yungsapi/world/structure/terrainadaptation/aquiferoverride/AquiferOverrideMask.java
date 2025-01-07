package com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.aquiferoverride;

import net.minecraft.world.level.block.state.BlockState;

import java.util.BitSet;

/**
 * Tracks which blocks in a chunk have had their density modified by an EnhancedTerrainAdaptation.
 * This is used to modify liquid blocks placed by aquifers in positions that are also carved by the EnhancedTerrainAdaptation,
 * if said EnhancedTerrainAdaptation has an AquiferOverride.
 */
public class AquiferOverrideMask {
    private final int minY;
    private final BitSet mask;
    private AquiferOverride aquiferOverride;

    public AquiferOverrideMask(int chunkHeight, int minY) {
        this.minY = minY;
        this.mask = new BitSet(256 * chunkHeight);
    }

    public void set(int x, int y, int z) {
        this.mask.set(this.getIndex(x, y, z));
    }

    public boolean get(int x, int y, int z) {
        return this.mask.get(this.getIndex(x, y, z));
    }

    public AquiferOverride getAquiferOverride() {
        return aquiferOverride;
    }

    public void setAquiferOverride(AquiferOverride aquiferOverride) {
        this.aquiferOverride = aquiferOverride;
    }

    public BlockState getBlockStateForPos(int x, int y, int z, BlockState defaultBlockState) {
        if (this.get(x, y, z) && this.aquiferOverride != null) {
            return this.aquiferOverride.getBlockState(defaultBlockState);
        }
        return defaultBlockState;
    }

    private int getIndex(int x, int y, int z) {
        return x & 15 | (z & 15) << 4 | y - this.minY << 8;
    }
}
