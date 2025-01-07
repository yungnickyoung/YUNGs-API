package com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.aquiferoverride;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockState;

public class NoneAquiferOverride extends AquiferOverride {
    private static final NoneAquiferOverride INSTANCE = new NoneAquiferOverride();
    public static final MapCodec<NoneAquiferOverride> CODEC = MapCodec.unit(() -> INSTANCE);

    public NoneAquiferOverride() {
        super();
    }

    @Override
    public AquiferOverrideType<?> type() {
        return AquiferOverrideType.NONE;
    }

    @Override
    public BlockState getBlockState(BlockState defaultBlockState) {
        return defaultBlockState;
    }
}
