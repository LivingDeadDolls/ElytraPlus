package elytraplus.mixin;

import elytraplus.feature.ElytraHud;
import elytraplus.feature.Freelook;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
abstract class HudMixin {
    /** Whether the attack indicator is being drawn moved, so the move must be undone after it. */
    @Unique
    private boolean elytraplus$attackIndicatorMoved;

    /**
     * Vanilla draws the hotbar attack indicator right after reading the attack strength, on the main-arm side. When
     * the Elytra Plus frame sits next to the hotbar on that side, move the indicator beyond the frame.
     */
    @Inject(method = "extractItemHotbar", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;getAttackStrengthScale(F)F"))
    private void elytraplus$moveAttackIndicator(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        int shift = ElytraHud.attackIndicatorShift();
        if (shift != 0 && Minecraft.getInstance().getCameraEntity() instanceof Player player) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(player.getMainArm() == HumanoidArm.RIGHT ? shift : -shift, 0);
            elytraplus$attackIndicatorMoved = true;
        }
    }

    /** Only runs when vanilla draws the hotbar, so the slot hides with it (F1, spectator, etc.). */
    @Inject(method = "extractItemHotbar", at = @At("TAIL"))
    private void elytraplus$extractElytraSlot(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (elytraplus$attackIndicatorMoved) {
            graphics.pose().popMatrix();
            elytraplus$attackIndicatorMoved = false;
        }
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
