package com.liquidify.untitled.module.modules.movement;

import com.liquidify.untitled.module.Category;
import com.liquidify.untitled.module.Module;
import com.liquidify.untitled.module.settings.BooleanSetting;
import com.liquidify.untitled.module.settings.FloatSetting;
import com.liquidify.untitled.module.settings.KeybindSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public class Speed extends Module {
    public final FloatSetting multiplier      = addSetting(new FloatSetting("Multiplier", 1.6f, 1.0f, 5.0f));
    public final FloatSetting maxSpeed        = addSetting(new FloatSetting("Max Speed", 0.6f, 0.1f, 2.0f));
    public final BooleanSetting onlyOnGround  = addSetting(new BooleanSetting("Only on Ground", true));
    public final KeybindSetting keybind       = addSetting(new KeybindSetting("Keybind"));

    public Speed() {
        super("Speed", Category.MOVEMENT);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null) return;
        if (onlyOnGround.getValue() && !client.player.onGround()) return;

        // Get actual input vector from player
        var move = client.player.input.getMoveVector();
        float forward = move.y;  // forward/backward
        float strafe  = move.x;  // left/right

        // No input = don't apply speed
        if (Math.abs(forward) < 0.01 && Math.abs(strafe) < 0.01) {
            return;
        }

        // Calculate desired direction from input + player yaw
        float yaw = (float) Math.toRadians(client.player.getYRot());
        double sinYaw = Math.sin(yaw);
        double cosYaw = Math.cos(yaw);

        // Convert input to world direction
        double moveX = (-sinYaw * forward) + (cosYaw * strafe);
        double moveZ = (cosYaw * forward)  + (sinYaw * strafe);

        // Normalize
        double len = Math.sqrt(moveX * moveX + moveZ * moveZ);
        if (len > 0) {
            moveX /= len;
            moveZ /= len;
        }

        // Apply speed in correct direction
        float speed = Math.min(
                0.2873f * multiplier.getValue(), // 0.2873 = base sprint speed
                maxSpeed.getValue()
        );

        Vec3 vel = client.player.getDeltaMovement();
        client.player.setDeltaMovement(
                moveX * speed,
                vel.y,
                moveZ * speed
        );
    }
}