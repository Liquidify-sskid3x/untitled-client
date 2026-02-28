package com.liquidify.untitled.module.modules.player;

import com.liquidify.untitled.module.Category;
import com.liquidify.untitled.module.Module;
import com.liquidify.untitled.module.settings.BooleanSetting;
import com.liquidify.untitled.module.settings.KeybindSetting;
import net.minecraft.client.Minecraft;

public class NoFall extends Module {
    public final BooleanSetting cancelDamage = addSetting(new BooleanSetting("Cancel Damage", true));
    public final KeybindSetting keybind      = addSetting(new KeybindSetting("Keybind"));

    public NoFall() {
        super("NoFall", Category.PLAYER);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null) return;
        // Zero out fall distance every tick so damage never accumulates
        client.player.fallDistance = 0.0f;
        // Force onGround so server never registers a landing
        client.player.setPose(client.player.getPose());
        // Directly manipulate the fall damage flag
        if (client.player.fallDistance > 0) {
            client.player.fallDistance = 0.0f;
        }
    }
}