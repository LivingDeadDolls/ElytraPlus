package elytraplus.feature;

import elytraplus.Constants;
import elytraplus.platform.Services;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * The Freelook Plus mod, when installed. Its settings are read and written through reflection,
 * so this mod builds and runs without it. Our Freelook setting is its "elytra" movement setting;
 * fabric.mod.json declares that under "freelookplus:shared_settings" so Freelook Plus names us there.
 */
public final class FreelookPlusMod {
    public static final String MOD_ID = "freelookplus";
    private static final String API_CLASS = "freelookplus.api.FreelookPlusApi";
    private static final String MOVEMENT = "elytra";

    /** All null when the mod is absent. */
    private static final MethodHandle IS_ENABLED;
    private static final MethodHandle IS_MOVEMENT_ENABLED;
    private static final MethodHandle SET_MOVEMENT_ENABLED;

    static {
        MethodHandle enabled = null;
        MethodHandle getter = null;
        MethodHandle setter = null;
        if (Services.PLATFORM.isModLoaded(MOD_ID)) {
            try {
                Class<?> api = Class.forName(API_CLASS);
                MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                enabled = lookup.findStatic(api, "isEnabled", MethodType.methodType(boolean.class));
                getter = lookup.findStatic(api, "isMovementEnabled", MethodType.methodType(boolean.class, String.class));
                setter = lookup.findStatic(api, "setMovementEnabled", MethodType.methodType(void.class, String.class, boolean.class));
            } catch (ReflectiveOperationException e) {
                // Another mod using the same id: treat Freelook Plus as absent and use the built-in Freelook.
                Constants.LOG.info("The installed '{}' mod is not Freelook Plus; using the built-in Freelook", MOD_ID);
                // Some lookups may have succeeded before the failure.
                enabled = null;
                getter = null;
                setter = null;
            }
        }
        IS_ENABLED = enabled;
        IS_MOVEMENT_ENABLED = getter;
        SET_MOVEMENT_ENABLED = setter;
    }

    private FreelookPlusMod() {
    }

    public static boolean isLoaded() {
        return IS_MOVEMENT_ENABLED != null;
    }

    /** The mod's overall Freelook setting; when off, our setting has no effect. */
    public static boolean isEnabled() {
        try {
            return (boolean) IS_ENABLED.invokeExact();
        } catch (Throwable e) {
            throw new IllegalStateException("Freelook Plus API call failed", e);
        }
    }

    public static boolean isElytraEnabled() {
        try {
            return (boolean) IS_MOVEMENT_ENABLED.invokeExact(MOVEMENT);
        } catch (Throwable e) {
            throw new IllegalStateException("Freelook Plus API call failed", e);
        }
    }

    public static void setElytraEnabled(boolean value) {
        try {
            SET_MOVEMENT_ENABLED.invokeExact(MOVEMENT, value);
        } catch (Throwable e) {
            throw new IllegalStateException("Freelook Plus API call failed", e);
        }
    }
}
