package com.liquidify.untitled.module.modules.combat;

import com.liquidify.untitled.module.Category;
import com.liquidify.untitled.module.Module;
import com.liquidify.untitled.module.settings.BooleanSetting;
import com.liquidify.untitled.module.settings.FloatSetting;
import com.liquidify.untitled.module.settings.KeybindSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Reach extends Module {
    public final FloatSetting range      = addSetting(new FloatSetting("Range", 5.0f, 3.0f, 8.0f));
    public final BooleanSetting bypass   = addSetting(new BooleanSetting("Server Bypass", true));
    public final KeybindSetting keybind  = addSetting(new KeybindSetting("Keybind"));

    private long lastAttack = 0;

    public Reach() {
        super("Reach", Category.COMBAT);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) return;
        if (client.player.getAttackStrengthScale(0f) < 1.0f) return;

        // Only fire on left click
        boolean leftClick = org.lwjgl.glfw.GLFW.glfwGetMouseButton(
                client.getWindow().handle(),
                org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        if (!leftClick) return;

        // Rate limit to avoid spam flags
        long now = System.currentTimeMillis();
        if (now - lastAttack < 50) return;

        Vec3 eyePos = client.player.getEyePosition(1.0f);
        Vec3 lookVec = client.player.getLookAngle();

        // Find closest entity in look direction within range
        LivingEntity target = null;
        double closest = Double.MAX_VALUE;

        for (LivingEntity entity : client.level.getEntitiesOfClass(
                LivingEntity.class,
                client.player.getBoundingBox().inflate(range.getValue()))) {
            if (entity == client.player) continue;
            if (entity instanceof Player p && p.isCreative()) continue;
            if (!entity.isAlive()) continue;

            double dist = client.player.distanceTo(entity);
            if (dist > range.getValue()) continue;

            // Check if entity is roughly in look direction
            AABB box = entity.getBoundingBox().inflate(0.3);
            Vec3 end = eyePos.add(lookVec.scale(range.getValue()));
            if (box.clip(eyePos, end).isEmpty()) continue;

            if (dist < closest) {
                closest = dist;
                target = entity;
            }
        }

        if (target == null) return;

        if (bypass.getValue()) {
            // Send position packets to make server think we're closer
            // Cracked servers like HylexMC/BlocksMC check distance server-side
            double tx = target.getX();
            double ty = target.getY();
            double tz = target.getZ();
            double dx = tx - client.player.getX();
            double dy = ty - client.player.getY();
            double dz = tz - client.player.getZ();
            double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);

            if (dist > 3.0) {
                // Spoof position slightly toward target
                double spoofX = client.player.getX() + dx / dist * (dist - 2.9);
                double spoofY = client.player.getY() + dy / dist * (dist - 2.9);
                double spoofZ = client.player.getZ() + dz / dist * (dist - 2.9);
                client.getConnection().send(
                        new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                                spoofX, spoofY, spoofZ, true, false));
            }
        }

        client.gameMode.attack(client.player, target);
        client.player.swing(InteractionHand.MAIN_HAND);
        lastAttack = now;

        if (bypass.getValue()) {
            // Send real position back
            client.getConnection().send(
                    new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                            client.player.getX(), client.player.getY(), client.player.getZ(),
                            client.player.onGround(), false));
        }
    }
}