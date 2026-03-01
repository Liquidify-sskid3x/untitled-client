package com.liquidify.untitled.module.modules.player;

import com.liquidify.untitled.module.Category;
import com.liquidify.untitled.module.Module;
import com.liquidify.untitled.module.settings.FloatSetting;
import com.liquidify.untitled.module.settings.KeybindSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public class Velocity extends Module {
    public final FloatSetting horizontal = addSetting(new FloatSetting("Horizontal", 0.0f, 0.0f, 1.0f));
    public final FloatSetting vertical   = addSetting(new FloatSetting("Vertical", 0.0f, 0.0f, 1.0f));
    public final KeybindSetting keybind  = addSetting(new KeybindSetting("Keybind"));

    public static volatile boolean pendingCancel = false;

    public Velocity() {
        super("Velocity", Category.PLAYER);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null) return;
        if (!pendingCancel) return;
        pendingCancel = false;

        Vec3 vel = client.player.getDeltaMovement();
        client.player.setDeltaMovement(
                vel.x * horizontal.getValue(),
                vel.y < 0 ? vel.y : vel.y * vertical.getValue(),
                vel.z * horizontal.getValue()
        );
    }
}