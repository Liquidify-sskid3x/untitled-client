package com.liquidify.untitled;

import com.liquidify.untitled.config.ConfigManager;
import com.liquidify.untitled.gui.ClickGui;
import com.liquidify.untitled.hud.HudRenderer;
import com.liquidify.untitled.module.ModuleManager;
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

        // Load config after modules are registered
        ConfigManager.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (guiKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(clickGui);
                }
            }
            moduleManager.onTick(client);
        });

        boolean[] prevKeyStates = new boolean[GLFW.GLFW_KEY_LAST + 1];
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.getWindow() == null || client.screen != null) return;
            long window = client.getWindow().handle();
            for (int key = GLFW.GLFW_KEY_SPACE; key <= GLFW.GLFW_KEY_LAST; key++) {
                if (key > GLFW.GLFW_KEY_SPACE && key < GLFW.GLFW_KEY_APOSTROPHE) continue;
                if (key > GLFW.GLFW_KEY_APOSTROPHE && key < GLFW.GLFW_KEY_COMMA) continue;
                if (key > GLFW.GLFW_KEY_9 && key < GLFW.GLFW_KEY_SEMICOLON) continue;
                if (key > GLFW.GLFW_KEY_SEMICOLON && key < GLFW.GLFW_KEY_EQUAL) continue;
                if (key > GLFW.GLFW_KEY_EQUAL && key < GLFW.GLFW_KEY_A) continue;
                if (key > GLFW.GLFW_KEY_Z && key < GLFW.GLFW_KEY_LEFT_BRACKET) continue;
                if (key > GLFW.GLFW_KEY_GRAVE_ACCENT && key < GLFW.GLFW_KEY_WORLD_1) continue;
                if (key > GLFW.GLFW_KEY_WORLD_2 && key < GLFW.GLFW_KEY_ESCAPE) continue;
                if (key > GLFW.GLFW_KEY_PAGE_DOWN && key < GLFW.GLFW_KEY_HOME) continue;
                if (key > GLFW.GLFW_KEY_HOME && key < GLFW.GLFW_KEY_LEFT) continue;

                try {
                    int state = GLFW.glfwGetKey(window, key);
                    boolean down = state == GLFW.GLFW_PRESS;
                    if (down && !prevKeyStates[key]) {
                        moduleManager.onKeyPress(key);
                    }
                    prevKeyStates[key] = down;
                } catch (Exception ignored) {}
            }
        });

        HudRenderer.register();

        // Save config on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(ConfigManager::save));

        LOGGER.info("Untitled Client loaded!");
    }
}