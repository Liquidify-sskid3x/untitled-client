package com.liquidify.untitled.module.modules.render;

import com.liquidify.untitled.module.Category;
import com.liquidify.untitled.module.Module;
import com.liquidify.untitled.module.settings.*;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

public class ESP extends Module {
    public final FloatSetting range      = addSetting(new FloatSetting("Range", 64f, 10f, 128f));
    public final BooleanSetting players  = addSetting(new BooleanSetting("Players", true));
    public final BooleanSetting mobs     = addSetting(new BooleanSetting("Mobs", true));
    public final BooleanSetting filled   = addSetting(new BooleanSetting("Filled", false));
    public final BooleanSetting showNames   = addSetting(new BooleanSetting("Names", true));
    public final BooleanSetting showHealth  = addSetting(new BooleanSetting("Health Bar", true));
    public final StringSetting colorMode = addSetting(new StringSetting("Color", "Default",
            "Default", "Rainbow", "Custom"));
    public final ColorSetting customColor = addSetting(new ColorSetting("Custom Color", 0xFF00AAFF));
    public final KeybindSetting keybind  = addSetting(new KeybindSetting("Keybind"));

    public ESP() {
        super("ESP", Category.RENDER);
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(ESP::onWorldRender);
    }

    private static void onWorldRender(WorldRenderContext context) {
        // 2D ESP is handled in HudRenderer via screen projection
    }
}