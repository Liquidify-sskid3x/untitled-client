package com.liquidify.untitled.mixin;

import com.liquidify.untitled.UntitledClient;
import com.liquidify.untitled.module.modules.player.NoFall;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Connection.class)
public class ClientConnectionMixin {

    @ModifyVariable(method = "send(Lnet/minecraft/network/protocol/Packet;)V",
            at = @At("HEAD"), argsOnly = true)
    private Packet<?> onSendPacket(Packet<?> packet) {
        if (!(packet instanceof ServerboundMovePlayerPacket move)) return packet;

        NoFall nf = (NoFall) UntitledClient.moduleManager.getModules()
                .stream().filter(m -> m instanceof NoFall).findFirst().orElse(null);
        if (nf == null || !nf.isEnabled()) return packet;

        return new ServerboundMovePlayerPacket.PosRot(
                move.getX(0), move.getY(0), move.getZ(0),
                move.getYRot(0), move.getXRot(0),
                true, false
        );
    }
}