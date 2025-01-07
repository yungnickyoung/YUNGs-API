package com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.adaptations;

import com.mojang.serialization.MapCodec;
import com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.aquiferoverride.AquiferOverride;

public class LargeCarvedTopNoBeardAdaptation extends EnhancedTerrainAdaptation {
    private static final LargeCarvedTopNoBeardAdaptation INSTANCE = new LargeCarvedTopNoBeardAdaptation();
    public static final MapCodec<LargeCarvedTopNoBeardAdaptation> CODEC = MapCodec.unit(() -> INSTANCE);

    public LargeCarvedTopNoBeardAdaptation() {
        super(24, 16, TerrainAction.CARVE, TerrainAction.NONE, 0, Padding.ZERO, AquiferOverride.NONE);
    }

    @Override
    public EnhancedTerrainAdaptationType<?> type() {
        return EnhancedTerrainAdaptationType.LARGE_CARVED_TOP_NO_BEARD;
    }
}
