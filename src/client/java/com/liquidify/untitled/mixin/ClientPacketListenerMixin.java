package com.liquidify.untitled.mixin;

import com.liquidify.untitled.UntitledClient;
import com.liquidify.untitled.module.modules.player.Velocity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "handleSetEntityMotion", at = @At("TAIL"))
    private void onVelocityPacket(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (packet.getId() != mc.player.getId()) return;

        Velocity vel = (Velocity) UntitledClient.moduleManager.getModules()
                .stream().filter(m -> m instanceof Velocity).findFirst().orElse(null);
        if (vel == null || !vel.isEnabled()) return;

        // After packet is applied, scale down the velocity change
        Vec3 current = mc.player.getDeltaMovement();
        mc.player.setDeltaMovement(
                current.x * vel.horizontal.getValue(),
                current.y > 0 ? current.y * vel.vertical.getValue() : current.y,
                current.z * vel.horizontal.getValue()
        );
    }
}