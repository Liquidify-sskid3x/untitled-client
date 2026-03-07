package com.liquidify.untitled.module.modules.player;

import com.liquidify.untitled.module.Category;
import com.liquidify.untitled.module.Module;
import com.liquidify.untitled.module.settings.FloatSetting;
import com.liquidify.untitled.module.settings.KeybindSetting;
import net.minecraft.client.Minecraft;

public class Velocity extends Module {
    public final FloatSetting horizontal = addSetting(new FloatSetting("Horizontal", 0.0f, 0.0f, 1.0f));
    public final FloatSetting vertical   = addSetting(new FloatSetting("Vertical",   0.0f, 0.0f, 1.0f));
    public final KeybindSetting keybind  = addSetting(new KeybindSetting("Keybind"));

    public Velocity() {
        super("Velocity", Category.PLAYER);
    }

    @Override
    public void onTick(Minecraft client) {
    }
}