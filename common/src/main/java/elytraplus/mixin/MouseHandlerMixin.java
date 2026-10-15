package elytraplus.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import elytraplus.feature.Freelook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {
    /** During Freelook, mouse movement turns the camera instead of the player. */
    @WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void elytraplus$freelook(LocalPlayer player, double xo, double yo, Operation<Void> original) {
        Freelook.update(Minecraft.getInstance());
        if (Freelook.isActive()) {
            Freelook.turn(xo, yo);
        } else {
            original.call(player, xo, yo);
        }
    }
}
