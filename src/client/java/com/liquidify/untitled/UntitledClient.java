package com.liquidify.untitled;

import com.liquidify.untitled.gui.ClickGui;
import com.liquidify.untitled.hud.HudRenderer;
import com.liquidify.untitled.module.ModuleManager;
import com.liquidify.untitled.module.modules.render.ESP;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UntitledClient implements ClientModInitializer {
    public static final String MOD_ID = "untitled";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ModuleManager moduleManager;
    public static ClickGui clickGui;

    private static KeyMapping guiKey;

    @Override
    public void onInitializeClient() {
        moduleManager = new ModuleManager();
        clickGui = new ClickGui();

        guiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.untitled.gui",
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyMapping.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (guiKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(clickGui);
                }
            }
            moduleManager.onTick(client);
        });

        // Keybind detection via polling
        boolean[] prevKeyStates = new boolean[GLFW.GLFW_KEY_LAST];
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.getWindow() == null || client.screen != null) return;
            long window = client.getWindow().handle();       // try 2
            for (int key = 0; key < GLFW.GLFW_KEY_LAST; key++) {
                boolean down = GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
                if (down && !prevKeyStates[key]) {
                    moduleManager.onKeyPress(key);
                }
                prevKeyStates[key] = down;
            }
        });

        HudRenderer.register();
        ESP.register();

        LOGGER.info("Untitled Client loaded!");
    }
}