package com.liquidify.untitled.module.modules.render;

import com.liquidify.untitled.module.Category;
import com.liquidify.untitled.module.Module;
import com.liquidify.untitled.module.settings.BooleanSetting;
import com.liquidify.untitled.module.settings.ColorSetting;
import com.liquidify.untitled.module.settings.FloatSetting;
import com.liquidify.untitled.module.settings.StringSetting;
import net.minecraft.client.Minecraft;

import java.util.LinkedList;

public class ClickGuiModule extends Module {
    public final BooleanSetting showFps       = addSetting(new BooleanSetting("Show FPS", true));
    public final BooleanSetting showAvg       = addSetting(new BooleanSetting("Avg FPS", false));
    public final BooleanSetting showMin       = addSetting(new BooleanSetting("Min FPS", false));
    public final BooleanSetting showMax       = addSetting(new BooleanSetting("Max FPS", false));
    public final BooleanSetting rainbowEverything = addSetting(new BooleanSetting("Rainbow All", false));
    public final BooleanSetting rainbowList       = addSetting(new BooleanSetting("Rainbow List", false));
    public final BooleanSetting rainbowAccent     = addSetting(new BooleanSetting("Rainbow Accent", false));
    public final FloatSetting rainbowSpeed        = addSetting(new FloatSetting("Rainbow Speed", 2f, 0.1f, 10f));
    public final ColorSetting fpsColor        = addSetting(new ColorSetting("FPS Color", 0x0a84ff));
    public final BooleanSetting showArrayList = addSetting(new BooleanSetting("ArrayList", true));
    public final ColorSetting listColor       = addSetting(new ColorSetting("List Color", 0xFFFFFF));
    public final ColorSetting accentColor     = addSetting(new ColorSetting("Accent Color", 0x0a84ff));
    public final StringSetting sortMode       = addSetting(new StringSetting("Sort",
            "Longest", "Longest", "Shortest", "Alphabetical"));

    private final LinkedList<Integer> fpsHistory = new LinkedList<>();
    private int minFps = Integer.MAX_VALUE;
    private int maxFps = 0;

    public ClickGuiModule() {
        super("ClickGui", Category.RENDER);
        // Always enabled — passive module
        if (!isEnabled()) toggle();
    }

    @Override
    public void toggle() {
        // Prevent disabling
        if (isEnabled()) return;
        super.toggle();
    }

    public void trackFps(int fps) {
        fpsHistory.add(fps);
        if (fpsHistory.size() > 100) fpsHistory.removeFirst();
        if (fps < minFps) minFps = fps;
        if (fps > maxFps) maxFps = fps;
    }

    public int getAvgFps() {
        if (fpsHistory.isEmpty()) return 0;
        return (int) fpsHistory.stream().mapToInt(i -> i).average().orElse(0);
    }

    public int getMinFps() {
        return minFps == Integer.MAX_VALUE ? 0 : minFps;
    }

    public int getMaxFps() {
        return maxFps;
    }
    public int getRainbowColor(int offset) {
        // Use a fixed cycle of 3000ms divided by speed
        long cycleDuration = (long)(3000 / Math.max(0.1f, rainbowSpeed.getValue()));
        float hue = (System.currentTimeMillis() % cycleDuration) / (float) cycleDuration;
        hue = (hue + offset * 0.04f) % 1f;
        return java.awt.Color.HSBtoRGB(hue, 1f, 1f) & 0x00FFFFFF;
    }

    public boolean isRainbow() {
        return rainbowEverything.getValue();
    }

    @Override
    public void onTick(Minecraft mc) {}
}