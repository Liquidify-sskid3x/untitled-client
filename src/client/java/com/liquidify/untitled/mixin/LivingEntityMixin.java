package com.liquidify.untitled.mixin;

import com.liquidify.untitled.UntitledClient;
import com.liquidify.untitled.module.modules.player.Velocity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "knockback", at = @At("HEAD"), cancellable = true)
    private void onKnockback(double strength, double x, double z, CallbackInfo ci) {
        if (!((LivingEntity)(Object)this instanceof net.minecraft.client.player.LocalPlayer))
            return;

        Velocity vel = (Velocity) UntitledClient.moduleManager.getModules()
                .stream().filter(m -> m instanceof Velocity).findFirst().orElse(null);
        if (vel == null || !vel.isEnabled()) return;

        if (vel.horizontal.getValue() == 0.0f && vel.vertical.getValue() == 0.0f) {
            ci.cancel();
        } else {
            Velocity.pendingCancel = true;
        }
    }
}