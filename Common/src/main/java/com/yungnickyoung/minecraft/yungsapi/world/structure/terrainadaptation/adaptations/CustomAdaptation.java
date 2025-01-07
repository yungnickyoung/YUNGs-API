package com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.adaptations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.aquiferoverride.AquiferOverride;
import com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.aquiferoverride.AquiferOverrideType;
import net.minecraft.util.ExtraCodecs;

public class CustomAdaptation extends EnhancedTerrainAdaptation {
    public static final MapCodec<CustomAdaptation> CODEC = RecordCodecBuilder.mapCodec((builder) -> builder
            .group(
                    ExtraCodecs.NON_NEGATIVE_INT.fieldOf("kernel_size").forGetter(EnhancedTerrainAdaptation::getKernelSize),
                    ExtraCodecs.NON_NEGATIVE_INT.fieldOf("kernel_distance").forGetter(EnhancedTerrainAdaptation::getKernelDistance),
                    TerrainAction.CODEC.fieldOf("top").forGetter(EnhancedTerrainAdaptation::topAction),
                    TerrainAction.CODEC.fieldOf("bottom").forGetter(EnhancedTerrainAdaptation::bottomAction),
                    Codec.DOUBLE.optionalFieldOf("bottom_offset", 0.0).forGetter(EnhancedTerrainAdaptation::getBottomOffset),
                    EnhancedTerrainAdaptation.Padding.CODEC.optionalFieldOf("padding", EnhancedTerrainAdaptation.Padding.ZERO).forGetter(EnhancedTerrainAdaptation::getPadding),
                    AquiferOverrideType.AQUIFER_OVERRIDE_CODEC.optionalFieldOf("aquifer_override", AquiferOverride.NONE).forGetter(EnhancedTerrainAdaptation::getAquiferOverride))
            .apply(builder, CustomAdaptation::new));

    CustomAdaptation(int kernelSize, int kernelDistance, TerrainAction topAction, TerrainAction bottomAction,
                     double bottomOffset, Padding padding, AquiferOverride aquiferOverride) {
        super(kernelSize, kernelDistance, topAction, bottomAction, bottomOffset, padding, aquiferOverride);
    }

    @Override
    public EnhancedTerrainAdaptationType<?> type() {
        return EnhancedTerrainAdaptationType.CUSTOM;
    }
}
