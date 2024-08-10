package com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation;

import com.mojang.serialization.MapCodec;

public class LargeCarvedTopNoBeardAdaptation extends EnhancedTerrainAdaptation {
    private static final LargeCarvedTopNoBeardAdaptation INSTANCE = new LargeCarvedTopNoBeardAdaptation();
    public static final MapCodec<LargeCarvedTopNoBeardAdaptation> CODEC = MapCodec.unit(() -> INSTANCE);

    public LargeCarvedTopNoBeardAdaptation() {
        super(24, 16, TerrainAction.CARVE, TerrainAction.NONE, 0, Padding.ZERO);
    }

    @Override
    public EnhancedTerrainAdaptationType<?> type() {
        return EnhancedTerrainAdaptationType.LARGE_CARVED_TOP_NO_BEARD;
    }
}
