package com.mervyn.kotafix.init;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KotaFixMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("kota_fix");

    @Override
    public void onInitialize() {
        AreaScannerHandler.init();
        LOGGER.info("KOTA Fix initialized: Area Scanner for Zenith Knowledge active. Player deaths protected.");
    }
}
