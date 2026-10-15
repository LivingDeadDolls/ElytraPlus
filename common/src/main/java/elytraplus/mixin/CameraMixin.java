package elytraplus.mixin;

import elytraplus.feature.Freelook;
import net.minecraft.client.Camera;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
abstract class CameraMixin {
    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    /**
     * Right after the camera copies the player's rotation, and before the third-person offset is applied,
     * so the camera orbits the player in the Freelook direction.
     */
    @Inject(method = "alignWithEntity", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Camera;detached:Z", opcode = Opcodes.PUTFIELD))
    private void elytraplus$freelook(float partialTicks, CallbackInfo ci) {
        if (Freelook.isActive()) {
            setRotation(Freelook.yRot(), Freelook.xRot());
        }
    }
}
