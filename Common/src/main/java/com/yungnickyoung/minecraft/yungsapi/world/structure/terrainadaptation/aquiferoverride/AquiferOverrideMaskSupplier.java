package com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.aquiferoverride;

import java.util.function.Supplier;

public interface AquiferOverrideMaskSupplier {
    AquiferOverrideMask getOrCreateAquiferOverrideMask(Supplier<AquiferOverrideMask> aquiferOverrideMaskSupplier);
}

