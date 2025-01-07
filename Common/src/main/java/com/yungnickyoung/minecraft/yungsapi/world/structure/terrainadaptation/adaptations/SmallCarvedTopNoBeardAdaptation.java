package com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.adaptations;

import com.mojang.serialization.MapCodec;
import com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.aquiferoverride.AquiferOverride;

public class SmallCarvedTopNoBeardAdaptation extends EnhancedTerrainAdaptation {
    private static final SmallCarvedTopNoBeardAdaptation INSTANCE = new SmallCarvedTopNoBeardAdaptation();
    public static final MapCodec<SmallCarvedTopNoBeardAdaptation> CODEC = MapCodec.unit(() -> INSTANCE);

    public SmallCarvedTopNoBeardAdaptation() {
        super(12, 6, TerrainAction.CARVE, TerrainAction.NONE, 0, Padding.ZERO, AquiferOverride.NONE);
    }

    @Override
    public EnhancedTerrainAdaptationType<?> type() {
        return EnhancedTerrainAdaptationType.SMALL_CARVED_TOP_NO_BEARD;
    }
}
