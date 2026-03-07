package com.liquidify.untitled.mixin;

import com.liquidify.untitled.UntitledClient;
import com.liquidify.untitled.module.modules.player.NoFall;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = Connection.class, priority = 1001)
public class ClientConnectionMixin {

    @ModifyVariable(method = "send(Lnet/minecraft/network/protocol/Packet;)V",
            at = @At("HEAD"), argsOnly = true)
    private Packet<?> onSendPacket(Packet<?> packet) {
        if (!(packet instanceof ServerboundMovePlayerPacket move)) return packet;

        NoFall nf = (NoFall) UntitledClient.moduleManager.getModules()
                .stream().filter(m -> m instanceof NoFall).findFirst().orElse(null);
        if (nf == null || !nf.isEnabled()) return packet;

        Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null) return packet;

        // Mirror Meteor's logic: only set onGround=true when falling fast
        // This avoids the insta-kill bug caused by always spoofing onGround
        if (mc.player.getDeltaMovement().y > -0.5) return packet;
        if (mc.player.getAbilities().invulnerable) return packet;

        // Reconstruct packet with onGround=true
        if (move instanceof ServerboundMovePlayerPacket.PosRot pr) {
            return new ServerboundMovePlayerPacket.PosRot(
                    pr.getX(0), pr.getY(0), pr.getZ(0),
                    pr.getYRot(0), pr.getXRot(0),
                    true, false
            );
        } else if (move instanceof ServerboundMovePlayerPacket.Pos p) {
            return new ServerboundMovePlayerPacket.Pos(
                    p.getX(0), p.getY(0), p.getZ(0),
                    true, false
            );
        } else if (move instanceof ServerboundMovePlayerPacket.Rot r) {
            return new ServerboundMovePlayerPacket.Rot(
                    r.getYRot(0), r.getXRot(0),
                    true, false
            );
        } else {
            // StatusOnly packet
            return new ServerboundMovePlayerPacket.StatusOnly(true, false);
        }
    }
}