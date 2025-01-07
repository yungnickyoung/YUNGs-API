package com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.aquiferoverride;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.yungnickyoung.minecraft.yungsapi.YungsApiCommon;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Specifies the type for an {@link AquiferOverride}.
 * This class also serves as the registration hub for AquiferOverrides and their corresponding types.
 */
public interface AquiferOverrideType<C extends AquiferOverride> {
    /* Utility maps for codecs. Simulates the approach vanilla registries use. */
    Map<ResourceLocation, AquiferOverrideType<?>> AQUIFER_OVERRIDE_TYPE_BY_NAME = new HashMap<>();
    Map<AquiferOverrideType<?>, ResourceLocation> AQUIFER_OVERRIDE_NAME_BY_TYPE = new HashMap<>();

    /* Codecs */
    Codec<AquiferOverrideType<?>> AQUIFER_OVERRIDE_TYPE_CODEC = ResourceLocation.CODEC
            .flatXmap(
                    resourceLocation -> Optional.ofNullable(AQUIFER_OVERRIDE_TYPE_BY_NAME.get(resourceLocation))
                            .map(DataResult::success)
                            .orElseGet(() -> DataResult.error(() -> "Unknown Aquifer Override type: " + resourceLocation)),
                    type -> Optional.of(AQUIFER_OVERRIDE_NAME_BY_TYPE.get(type))
                            .map(DataResult::success)
                            .orElseGet(() -> DataResult.error(() -> "No ID found for Aquifer Override type " + type + ". Is it registered?")));

    Codec<AquiferOverride> AQUIFER_OVERRIDE_CODEC = AQUIFER_OVERRIDE_TYPE_CODEC
            .dispatch("type", AquiferOverride::type, AquiferOverrideType::codec);

    /* Types. Add any new types here! */
    AquiferOverrideType<NoneAquiferOverride> NONE = register("none", NoneAquiferOverride.CODEC);
    AquiferOverrideType<ReplaceAquiferOverride> REPLACE = register("replace", ReplaceAquiferOverride.CODEC);
    AquiferOverrideType<SolidifyAquiferOverride> SOLIDIFY = register("solidify", SolidifyAquiferOverride.CODEC);

    /**
     * Utility method for registering AquiferOverrideTypes.
     */
    static <C extends AquiferOverride> AquiferOverrideType<C> register(ResourceLocation resourceLocation, MapCodec<C> codec) {
        AquiferOverrideType<C> type = () -> codec;
        AQUIFER_OVERRIDE_TYPE_BY_NAME.put(resourceLocation, type);
        AQUIFER_OVERRIDE_NAME_BY_TYPE.put(type, resourceLocation);
        return type;
    }

    /**
     * Private utility method for registering AquiferOverrideTypes native to YUNG's API.
     */
    private static <C extends AquiferOverride> AquiferOverrideType<C> register(String id, MapCodec<C> codec) {
        return register(ResourceLocation.fromNamespaceAndPath(YungsApiCommon.MOD_ID, id), codec);
    }

    /**
     * Supplies the codec for the {@link AquiferOverride} corresponding to this AquiferOverrideType.
     */
    MapCodec<C> codec();
}

