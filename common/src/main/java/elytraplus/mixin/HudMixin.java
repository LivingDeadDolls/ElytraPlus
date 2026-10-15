package elytraplus.mixin;

import elytraplus.feature.ElytraHud;
import elytraplus.feature.Freelook;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
abstract class HudMixin {
    /** Only runs when vanilla draws the hotbar, so the slot hides with it (F1, spectator, etc.). */
    @Inject(method = "extractItemHotbar", at = @At("TAIL"))
    private void elytraplus$extractElytraSlot(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        ElytraHud.extract(graphics);
    }

    /** During Freelook the crosshair would not point where the player is aiming, so hide it. */
    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void elytraplus$hideCrosshair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (Freelook.isActive()) {
            ci.cancel();
        }
    }
}
