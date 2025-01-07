package com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.adaptations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.yungnickyoung.minecraft.yungsapi.YungsApiCommon;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Specifies the type of a specific {@link EnhancedTerrainAdaptation}.
 * This class also serves as the registration hub for EnhancedTerrainAdaptations and their corresponding types.
 */
public interface EnhancedTerrainAdaptationType<C extends EnhancedTerrainAdaptation> {
    /* Utility maps for codecs. Simulates the approach vanilla registries use. */
    Map<ResourceLocation, EnhancedTerrainAdaptationType<?>> ADAPTATION_TYPES_BY_NAME = new HashMap<>();
    Map<EnhancedTerrainAdaptationType<?>, ResourceLocation> NAME_BY_ADAPTATION_TYPES = new HashMap<>();

    /* Codecs */
    Codec<EnhancedTerrainAdaptationType<?>> ADAPTATION_TYPE_CODEC = ResourceLocation.CODEC
            .flatXmap(
                    resourceLocation -> Optional.ofNullable(ADAPTATION_TYPES_BY_NAME.get(resourceLocation))
                            .map(DataResult::success)
                            .orElseGet(() -> DataResult.error(() -> "Unknown enhanced terrain adaptation type: " + resourceLocation)),
                    adaptationType -> Optional.of(NAME_BY_ADAPTATION_TYPES.get(adaptationType))
                            .map(DataResult::success)
                            .orElseGet(() -> DataResult.error(() -> "No ID found for enhanced terrain adaptation type " + adaptationType + ". Is it registered?")));

    Codec<EnhancedTerrainAdaptation> ADAPTATION_CODEC = ADAPTATION_TYPE_CODEC
            .dispatch("type", EnhancedTerrainAdaptation::type, EnhancedTerrainAdaptationType::codec);

    /* Types. Add any new types here! */
    EnhancedTerrainAdaptationType<NoneAdaptation> NONE = register("none", NoneAdaptation.CODEC);
    EnhancedTerrainAdaptationType<LargeCarvedTopNoBeardAdaptation> LARGE_CARVED_TOP_NO_BEARD =
            register("carved_top_no_beard_large", LargeCarvedTopNoBeardAdaptation.CODEC);
    EnhancedTerrainAdaptationType<SmallCarvedTopNoBeardAdaptation> SMALL_CARVED_TOP_NO_BEARD =
            register("carved_top_no_beard_small", SmallCarvedTopNoBeardAdaptation.CODEC);
    EnhancedTerrainAdaptationType<CustomAdaptation> CUSTOM = register("custom", CustomAdaptation.CODEC);

    /**
     * Utility method for registering EnhancedTerrainAdaptationTypes.
     */
    static <C extends EnhancedTerrainAdaptation> EnhancedTerrainAdaptationType<C> register(ResourceLocation resourceLocation, MapCodec<C> codec) {
        EnhancedTerrainAdaptationType<C> adaptationType = () -> codec;
        ADAPTATION_TYPES_BY_NAME.put(resourceLocation, adaptationType);
        NAME_BY_ADAPTATION_TYPES.put(adaptationType, resourceLocation);
        return adaptationType;
    }

    /**
     * Private utility method for registering EnhancedTerrainAdaptationTypes native to YUNG's API.
     */
    private static <C extends EnhancedTerrainAdaptation> EnhancedTerrainAdaptationType<C> register(String id, MapCodec<C> codec) {
        return register(ResourceLocation.fromNamespaceAndPath(YungsApiCommon.MOD_ID, id), codec);
    }

    /**
     * Supplies the codec for the {@link EnhancedTerrainAdaptation} corresponding to this EnhancedTerrainAdaptationType.
     */
    MapCodec<C> codec();
}

