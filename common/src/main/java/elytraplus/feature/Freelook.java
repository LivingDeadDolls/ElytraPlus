package elytraplus.feature;

import elytraplus.config.ElytraPlusConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/**
 * Freelook: while gliding with the key held, the mouse turns only the camera, so you can look around
 * without changing where you fly. The view (first or third person) is left as is.
 *
 * <p>Built-in version for when the Freelook Plus mod is absent. When it is installed, this one is unused and
 * the Freelook setting is shared with that mod's elytra setting (see {@link FreelookPlusMod}).
 */
public final class Freelook {
    /** Same name as the Freelook Plus mod's key, so the binding in options.txt is shared with it. */
    public static final String KEY_NAME = "key.freelookplus.freelook";

    /** Null while the Freelook Plus mod is installed: the built-in Freelook is then never on. */
    private static KeyMapping key;
    private static boolean active;
    private static float yRot;
    private static float xRot;

    private Freelook() {
    }

    /** Set once by the loader's client entrypoint after registering the key. */
    public static void setKey(KeyMapping keyMapping) {
        key = keyMapping;
    }

    /**
     * Turns Freelook on or off to match the key. Called every client tick, and also right before mouse movement
     * is applied so that pressing or releasing the key takes effect on the very next mouse movement.
     */
    public static void update(Minecraft minecraft) {
        if (key == null) {
            return;
        }
        // Hold only; drain presses so they don't pile up.
        while (key.consumeClick()) {
        }
        LocalPlayer player = minecraft.player;
        boolean wanted = key.isDown()
                && player != null
                && player.isFallFlying()
                && minecraft.getCameraEntity() == player
                && ElytraPlusConfig.get().freelook;
        if (wanted && !active) {
            yRot = player.getYRot();
            xRot = player.getXRot();
        }
        active = wanted;
    }

    public static boolean isActive() {
        return active;
    }

    public static float yRot() {
        return yRot;
    }

    public static float xRot() {
        return xRot;
    }

    /** Mouse movement while active; same scale and clamp as Entity.turn. */
    public static void turn(double xo, double yo) {
        yRot += (float) xo * 0.15F;
        xRot = Mth.clamp(xRot + (float) yo * 0.15F, -90.0F, 90.0F);
    }
}
