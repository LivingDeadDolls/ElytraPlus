package elytraplus.feature;

import elytraplus.config.ElytraPlusConfig;
import java.lang.ref.WeakReference;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;

/**
 * Auto Switch Third Person: starting to glide in first person switches to the rear third-person view,
 * and landing switches back. If the view was changed by hand during the flight, it is left alone.
 */
public final class AutoSwitchThirdPerson {
    /**
     * The connection the flight state below belongs to; it is stale once another world or server is joined.
     * Weak, so it does not keep a closed world in memory.
     */
    private static WeakReference<ClientPacketListener> connection = new WeakReference<>(null);
    private static boolean wasFallFlying;
    /** True while the current third-person view is one we switched to. */
    private static boolean switched;

    private AutoSwitchThirdPerson() {
    }

    /** Called at the end of every player tick. */
    public static void tick(LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (connection.get() != minecraft.getConnection()) {
            // Left the previous world mid-flight: undo our view change, then start fresh here.
            connection = new WeakReference<>(minecraft.getConnection());
            if (wasFallFlying) {
                onStopFallFlying(minecraft);
            }
            wasFallFlying = false;
        }
        boolean fallFlying = player.isFallFlying();
        if (fallFlying && !wasFallFlying) {
            onStartFallFlying(minecraft);
        } else if (!fallFlying && wasFallFlying) {
            onStopFallFlying(minecraft);
        }
        wasFallFlying = fallFlying;
    }

    private static void onStartFallFlying(Minecraft minecraft) {
        if (ElytraPlusConfig.get().autoSwitchThirdPerson && minecraft.options.getCameraType() == CameraType.FIRST_PERSON) {
            setCameraType(minecraft, CameraType.THIRD_PERSON_BACK);
            switched = true;
        }
    }

    private static void onStopFallFlying(Minecraft minecraft) {
        if (switched && minecraft.options.getCameraType() == CameraType.THIRD_PERSON_BACK) {
            setCameraType(minecraft, CameraType.FIRST_PERSON);
        }
        switched = false;
    }

    /** What vanilla does on F5: entity post effects (e.g. spectating a creeper) only apply in first person. */
    private static void setCameraType(Minecraft minecraft, CameraType type) {
        minecraft.options.setCameraType(type);
        minecraft.gameRenderer.checkEntityPostEffect(type.isFirstPerson() ? minecraft.getCameraEntity() : null);
    }
}
