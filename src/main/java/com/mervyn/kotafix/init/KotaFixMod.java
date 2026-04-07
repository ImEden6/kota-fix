package com.mervyn.kotafix.init;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KotaFixMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("kota_fix");

    @Override
    public void onInitialize() {
        LOGGER.info("KOTA Fix initialized: Design flaw in porting_lib event system has been patched via Mixin.");
    }
}
