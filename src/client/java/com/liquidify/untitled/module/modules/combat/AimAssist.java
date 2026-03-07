package com.liquidify.untitled.module.modules.combat;

import com.liquidify.untitled.module.Category;
import com.liquidify.untitled.module.Module;
import com.liquidify.untitled.module.settings.FloatSetting;
import com.liquidify.untitled.module.settings.KeybindSetting;
import com.liquidify.untitled.module.settings.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class AimAssist extends Module {
    public final FloatSetting range    = addSetting(new FloatSetting("Range", 5.0f, 1.0f, 10.0f));
    public final FloatSetting speed    = addSetting(new FloatSetting("Speed", 3.0f, 0.05f, 10.0f));
    public final StringSetting mode    = addSetting(new StringSetting("Mode", "Linear",
            "Linear", "Sigmoid", "Instant"));
    public final KeybindSetting keybind = addSetting(new KeybindSetting("Keybind"));

    public AimAssist() {
        super("AimAssist", Category.COMBAT);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) return;

        LivingEntity target = getClosestEntity(client);
        if (target == null) return;

        Vec3 eyePos = client.player.getEyePosition(1.0f);
        Vec3 targetPos = target.getBoundingBox().getCenter();

        double dx = targetPos.x - eyePos.x;
        double dy = targetPos.y - eyePos.y;
        double dz = targetPos.z - eyePos.z;

        double dist = Math.sqrt(dx * dx + dz * dz);
        float targetYaw   = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float targetPitch = (float)(-Math.toDegrees(Math.atan2(dy, dist)));

        float currentYaw   = client.player.getYRot();
        float currentPitch = client.player.getXRot();

        float diffYaw   = wrapDegrees(targetYaw - currentYaw);
        float diffPitch = targetPitch - currentPitch;

        float s = speed.getValue();

        float newYaw, newPitch;

        switch (mode.getValue()) {
            case "Instant" -> {
                newYaw   = targetYaw;
                newPitch = targetPitch;
            }
            case "Sigmoid" -> {
                // Sigmoid easing — fast start/end, slow middle
                float tYaw   = sigmoid(Math.abs(diffYaw)   / 45f) * s;
                float tPitch = sigmoid(Math.abs(diffPitch) / 45f) * s;
                newYaw   = currentYaw   + clamp(diffYaw,   -tYaw,   tYaw);
                newPitch = currentPitch + clamp(diffPitch, -tPitch, tPitch);
            }
            default -> {
                // Linear — constant speed rotation
                newYaw   = currentYaw   + clamp(diffYaw,   -s, s);
                newPitch = currentPitch + clamp(diffPitch, -s, s);
            }
        }

        client.player.setYRot(newYaw);
        client.player.setXRot(clamp(newPitch, -90f, 90f));
        client.player.yRotO = newYaw;
        client.player.xRotO = clamp(newPitch, -90f, 90f);
    }

    private LivingEntity getClosestEntity(Minecraft client) {
        LivingEntity closest = null;
        double closestDist = range.getValue();

        for (LivingEntity entity : client.level.getEntitiesOfClass(
                LivingEntity.class,
                client.player.getBoundingBox().inflate(range.getValue()))) {
            if (entity == client.player) continue;
            if (entity instanceof Player p && p.isCreative()) continue;
            if (!entity.isAlive()) continue;

            double dist = client.player.distanceTo(entity);
            if (dist < closestDist) {
                closestDist = dist;
                closest = entity;
            }
        }
        return closest;
    }

    private float wrapDegrees(float deg) {
        deg %= 360f;
        if (deg >= 180f) deg -= 360f;
        if (deg < -180f) deg += 360f;
        return deg;
    }

    private float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    private float sigmoid(float x) {
        return (float)(1.0 / (1.0 + Math.exp(-10 * (x - 0.5))));
    }
}