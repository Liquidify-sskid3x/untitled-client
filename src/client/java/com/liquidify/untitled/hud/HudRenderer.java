package com.liquidify.untitled.hud;

import com.liquidify.untitled.UntitledClient;
import com.liquidify.untitled.gui.Panel;
import com.liquidify.untitled.module.Module;
import com.liquidify.untitled.module.modules.render.ClickGuiModule;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;

import java.util.Comparator;
import java.util.List;

public class HudRenderer {
    public static void register() {
        HudRenderCallback.EVENT.register((graphics, delta) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            ClickGuiModule hs = getHudSettings();

            int screenW = mc.getWindow().getGuiScaledWidth();
            int screenH = mc.getWindow().getGuiScaledHeight();

            // ── Update panel accent color (rainbow or custom) ──────
            if (hs != null && (hs.isRainbow() || hs.rainbowAccent.getValue())) {
                Panel.dynamicAccent = hs.getRainbowColor(0) | 0xFF000000;
            } else if (hs != null) {
                Panel.dynamicAccent = hs.accentColor.getValue() | 0xFF000000;
            } else {
                Panel.dynamicAccent = 0xFF0a84ff;
            }

            // ── FPS Counter ───────────────────────────────────────
            if (hs == null || hs.showFps.getValue()) {
                int fps = mc.getFps();
                if (hs != null) hs.trackFps(fps);

                StringBuilder fpsStr = new StringBuilder();
                fpsStr.append(fps).append(" FPS");
                if (hs != null && hs.showAvg.getValue())
                    fpsStr.append("  avg:").append(hs.getAvgFps());
                if (hs != null && hs.showMin.getValue())
                    fpsStr.append("  min:").append(hs.getMinFps());
                if (hs != null && hs.showMax.getValue())
                    fpsStr.append("  max:").append(hs.getMaxFps());

                String fpsText = fpsStr.toString();
                int fpsColor = Panel.dynamicAccent;

                graphics.fill(0, screenH - 12,
                        mc.font.width(fpsText) + 4, screenH, 0x70000000);
                graphics.drawString(mc.font, fpsText, 2, screenH - 10, fpsColor, false);
            }

            // ── ArrayList ─────────────────────────────────────────
            if (hs == null || hs.showArrayList.getValue()) {
                List<Module> enabled = UntitledClient.moduleManager.getModules()
                        .stream()
                        .filter(m -> m.isEnabled() && !(m instanceof ClickGuiModule))
                        .sorted(getComparator(hs, mc))
                        .toList();

                int yOff = 2;
                int i = 0;
                for (Module module : enabled) {
                    // Accent color (right bar)
                    int accentColor;
                    if (hs != null && hs.isRainbow()) {
                        accentColor = hs.getRainbowColor(i) | 0xFF000000;
                    } else if (hs != null && hs.rainbowAccent.getValue()) {
                        accentColor = hs.getRainbowColor(i) | 0xFF000000;
                    } else {
                        accentColor = Panel.dynamicAccent;
                    }

                    // Text color
                    int listColor;
                    if (hs != null && hs.isRainbow()) {
                        listColor = hs.getRainbowColor(i) | 0xFF000000;
                    } else if (hs != null && hs.rainbowList.getValue()) {
                        listColor = hs.getRainbowColor(i) | 0xFF000000;
                    } else {
                        listColor = hs != null
                                ? hs.listColor.getValue() | 0xFF000000
                                : 0xFFFFFFFF;
                    }

                    String name = module.getName();
                    int textW = mc.font.width(name);
                    int xPos  = screenW - textW - 4;
                    graphics.fill(xPos - 1, yOff - 1,
                            screenW - 2, yOff + 9, 0x70000000);
                    graphics.fill(screenW - 2, yOff - 1,
                            screenW, yOff + 9, accentColor);
                    graphics.drawString(mc.font, name, xPos, yOff, listColor, false);
                    yOff += 11;
                    i++;
                }
            }
        });
    }

    private static Comparator<Module> getComparator(ClickGuiModule hs, Minecraft mc) {
        if (hs == null) return (a, b) ->
                mc.font.width(b.getName()) - mc.font.width(a.getName());

        return switch (hs.sortMode.getValue()) {
            case "Alphabetical" -> Comparator.comparing(Module::getName);
            case "Shortest"     -> Comparator.comparingInt(m -> mc.font.width(m.getName()));
            case "Longest"      -> (a, b) ->
                    mc.font.width(b.getName()) - mc.font.width(a.getName());
            default             -> (a, b) ->
                    mc.font.width(b.getName()) - mc.font.width(a.getName());
        };
    }

    private static ClickGuiModule getHudSettings() {
        return (ClickGuiModule) UntitledClient.moduleManager.getModules()
                .stream()
                .filter(m -> m instanceof ClickGuiModule)
                .findFirst()
                .orElse(null);
    }
}