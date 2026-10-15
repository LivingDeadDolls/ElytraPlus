package elytraplus.mixin;

import elytraplus.feature.AutoSwitchElytra;
import elytraplus.feature.AutoSwitchThirdPerson;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
abstract class LocalPlayerMixin {
    /** Jump pressed in mid-air: vanilla is about to try gliding, so put an elytra on first. */
    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;tryToStartFallFlying()Z"))
    private void elytraplus$beforeStartFallFlying(CallbackInfo ci) {
        AutoSwitchElytra.beforeStartFallFlying((LocalPlayer) (Object) this);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void elytraplus$tick(CallbackInfo ci) {
        AutoSwitchElytra.tick((LocalPlayer) (Object) this);
        AutoSwitchThirdPerson.tick((LocalPlayer) (Object) this);
    }
}
