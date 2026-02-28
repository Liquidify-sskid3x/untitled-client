package com.liquidify.untitled.module.modules.combat;

import com.liquidify.untitled.module.Category;
import com.liquidify.untitled.module.Module;
import com.liquidify.untitled.module.settings.BooleanSetting;
import com.liquidify.untitled.module.settings.KeybindSetting;
import com.liquidify.untitled.module.settings.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

public class Criticals extends Module {
    public final StringSetting mode = addSetting(new StringSetting("Mode",
            "Packet", "Packet", "Bypass", "Jump", "MiniJump"));
    public final BooleanSetting onlyKillAura = addSetting(new BooleanSetting("Only KillAura", false));
    public final KeybindSetting keybind = addSetting(new KeybindSetting("Keybind"));
    private boolean sendPackets = false;
    private int sendTimer = 0;
    private double lastY = 0;
    private boolean waitingForPeak = false;
    private boolean critPending = false;

    public Criticals() {
        super("Criticals", Category.COMBAT);
    }

    @Override
    public void onEnable() {
        sendPackets = false;
        sendTimer = 0;
        lastY = 0;
        waitingForPeak = false;
        critPending = false;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) return;
        if (skipCrit(client)) return;

        switch (mode.getValue()) {
            case "Jump" -> handleJump(client);
            case "MiniJump" -> handleMiniJump(client);
            case "Bypass NCP checks" -> handleBypass(client);
            default -> handlePacket(client);
        }
    }
    private void handlePacket(Minecraft client) {
        sendHeightPacket(client, 0.0625);
        sendHeightPacket(client, 0.0);
    }
    private void handleBypass(Minecraft client) {
        sendHeightPacket(client, 0.11);
        sendHeightPacket(client, 0.1100013579);
        sendHeightPacket(client, 0.0000013579);
    }
    private void handleJump(Minecraft client) {
        if (!sendPackets) {
            sendPackets = true;
            client.player.jumpFromGround();
            waitingForPeak = true;
            lastY = client.player.getY();
            return;
        }

        if (waitingForPeak) {
            double currentY = client.player.getY();
            if (currentY <= lastY) {
                // Reached peak — attack now
                waitingForPeak = false;
                sendPackets = false;
            }
            lastY = currentY;
        }
    }
    private void handleMiniJump(Minecraft client) {
        if (!sendPackets) {
            sendPackets = true;
            Vec3 vel = client.player.getDeltaMovement();
            client.player.setDeltaMovement(vel.x, 0.25, vel.z);
            sendTimer = 4;
            return;
        }

        if (sendTimer > 0) {
            sendTimer--;
        } else {
            sendPackets = false;
        }
    }
    private void sendHeightPacket(Minecraft client, double height) {
        double x = client.player.getX();
        double y = client.player.getY();
        double z = client.player.getZ();
        client.getConnection().send(
                new ServerboundMovePlayerPacket.Pos(x, y + height, z, false, false)
        );
    }
    private boolean skipCrit(Minecraft client) {
        var p = client.player;
        if (mode.getValue().equals("Jump") || mode.getValue().equals("MiniJump")) {
            if (sendPackets) return false;
            if (!p.onGround()) return true;
        } else {
            if (!p.onGround()) return true;
        }
        if (p.isInWater()) return true;
        if (p.isInLava()) return true;
        if (p.horizontalCollision) return true;
        if (p.isUnderWater()) return true;
        return false;
    }
}