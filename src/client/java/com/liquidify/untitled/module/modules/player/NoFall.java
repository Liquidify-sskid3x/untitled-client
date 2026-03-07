package com.liquidify.untitled.module.modules.player;

import com.liquidify.untitled.module.Category;
import com.liquidify.untitled.module.Module;
import com.liquidify.untitled.module.settings.KeybindSetting;
import net.minecraft.client.Minecraft;

public class NoFall extends Module {
    public final KeybindSetting keybind = addSetting(new KeybindSetting("Keybind"));

    public NoFall() {
        super("NoFall", Category.PLAYER);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null) return;
        client.player.fallDistance = 0f;
    }
}