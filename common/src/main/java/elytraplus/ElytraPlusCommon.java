package elytraplus;

import elytraplus.config.ElytraPlusConfig;

/** Loader-independent startup, called from each loader's client entrypoint. */
public final class ElytraPlusCommon {
    private ElytraPlusCommon() {
    }

    public static void init() {
        ElytraPlusConfig.HANDLER.load();
        Constants.LOG.info("Elytra Plus loaded on {}", elytraplus.platform.Services.PLATFORM.getPlatformName());
    }
}
