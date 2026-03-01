package com.liquidify.untitled.hud;

import com.liquidify.untitled.UntitledClient;
import com.liquidify.untitled.module.Module;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;

public class HudRenderer {
    public static float currentFov = 70.0f;

    public static void register() {
        HudRenderCallback.EVENT.register((graphics, delta) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            int screenW = mc.getWindow().getGuiScaledWidth();
            int screenH = mc.getWindow().getGuiScaledHeight();

            // FPS Counter
            String fpsText = mc.getFps() + " FPS";
            graphics.fill(0, screenH - 12, mc.font.width(fpsText) + 4, screenH, 0x70000000);
            graphics.drawString(mc.font, fpsText, 2, screenH - 10, 0xFF0a84ff, false);

            // ArrayList
            var enabled = UntitledClient.moduleManager.getModules()
                    .stream()
                    .filter(Module::isEnabled)
                    .sorted((a, b) -> mc.font.width(b.getName()) - mc.font.width(a.getName()))
                    .toList();

            int yOff = 2;
            for (Module module : enabled) {
                String name = module.getName();
                int textW = mc.font.width(name);
                int xPos = screenW - textW - 4;
                graphics.fill(xPos - 1, yOff - 1, screenW - 2, yOff + 9, 0x70000000);
                graphics.fill(screenW - 2, yOff - 1, screenW, yOff + 9, 0xFF0a84ff);
                graphics.drawString(mc.font, name, xPos, yOff, 0xFFFFFFFF, false);
                yOff += 11;
            }
        });
    }
}