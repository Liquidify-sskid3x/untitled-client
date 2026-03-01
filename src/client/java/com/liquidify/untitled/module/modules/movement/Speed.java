package com.liquidify.untitled.module.modules.movement;

import com.liquidify.untitled.module.Category;
import com.liquidify.untitled.module.Module;
import com.liquidify.untitled.module.settings.BooleanSetting;
import com.liquidify.untitled.module.settings.FloatSetting;
import com.liquidify.untitled.module.settings.KeybindSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public class Speed extends Module {
    public final FloatSetting multiplier     = addSetting(new FloatSetting("Multiplier", 1.6f, 1.0f, 5.0f));
    public final FloatSetting maxSpeed       = addSetting(new FloatSetting("Max Speed", 0.6f, 0.1f, 2.0f));
    public final BooleanSetting onlyOnGround = addSetting(new BooleanSetting("Only on Ground", true));
    public final KeybindSetting keybind      = addSetting(new KeybindSetting("Keybind"));

    public Speed() {
        super("Speed", Category.MOVEMENT);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null) return;
        if (onlyOnGround.getValue() && !client.player.onGround()) return;

        // Use getMoveVector to check if player is actually moving
        var move = client.player.input.getMoveVector();
        if (Math.abs(move.x) < 0.01 && Math.abs(move.y) < 0.01) return;

        Vec3 vel = client.player.getDeltaMovement();
        if (vel.horizontalDistanceSqr() < 0.001) return;

        double angle = Math.atan2(vel.z, vel.x);
        double speed = Math.min(
                Math.sqrt(vel.x * vel.x + vel.z * vel.z) * multiplier.getValue(),
                maxSpeed.getValue()
        );

        client.player.setDeltaMovement(
                Math.cos(angle) * speed,
                vel.y,
                Math.sin(angle) * speed
        );
    }
}