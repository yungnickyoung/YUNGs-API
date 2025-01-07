package com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.aquiferoverride;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Replaces the aquifer liquid blocks with a specified block state.
 */
public class ReplaceAquiferOverride extends AquiferOverride {
    public static final MapCodec<ReplaceAquiferOverride> CODEC = RecordCodecBuilder.mapCodec((builder) -> builder
            .group(
                    BlockState.CODEC.fieldOf("block_state").forGetter(aquiferOverride -> aquiferOverride.replaceBlockState))
            .apply(builder, ReplaceAquiferOverride::new));

    private final BlockState replaceBlockState;

    public ReplaceAquiferOverride(BlockState replaceBlockState) {
        super();
        this.replaceBlockState = replaceBlockState;
    }

    @Override
    public AquiferOverrideType<?> type() {
        return AquiferOverrideType.REPLACE;
    }

    @Override
    public BlockState getBlockState(BlockState defaultBlockState) {
        return replaceBlockState;
    }
}
