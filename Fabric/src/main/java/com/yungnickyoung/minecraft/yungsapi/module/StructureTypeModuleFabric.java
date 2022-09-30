package com.yungnickyoung.minecraft.yungsapi.module;

import com.yungnickyoung.minecraft.yungsapi.autoregister.AutoRegistrationManager;
import com.yungnickyoung.minecraft.yungsapi.autoregister.RegisterData;
import net.minecraft.core.Registry;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * Registration of structure types.
 */
public class StructureTypeModuleFabric {
    public static void init() {
        AutoRegistrationManager.STRUCTURE_TYPES.forEach(StructureTypeModuleFabric::register);
    }

    private static void register(RegisterData data) {
        Registry.register(Registry.STRUCTURE_TYPES, data.name(), (StructureType<?>) data.object());
    }
}