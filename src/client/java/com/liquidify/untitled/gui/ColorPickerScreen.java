package com.liquidify.untitled.gui;

import com.liquidify.untitled.module.settings.ColorSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class ColorPickerScreen extends Screen {
    private final Screen parent;
    private final ColorSetting setting;

    private float hue = 0f, sat = 1f, val = 1f;
    private boolean draggingPicker = false;
    private boolean draggingHue    = false;
    private boolean wasDown        = false;

    private static final int SV = 100; // smaller = faster
    private static final int HW = 14;
    private static final int W  = 200;
    private static final int H  = 210;

    // Cached pixel rows for SV square — only recompute when hue changes
    private final int[][] svCache = new int[SV][SV];
    private float lastCachedHue = -1f;

    // Hue bar pixels — computed once
    private final int[] hueCache = new int[SV];
    private boolean hueBuilt = false;

    public ColorPickerScreen(Screen parent, ColorSetting setting) {
        super(Component.literal("Color Picker"));
        this.parent  = parent;
        this.setting = setting;
        int col = setting.getValue();
        float r = ((col >> 16) & 0xFF) / 255f;
        float g = ((col >> 8)  & 0xFF) / 255f;
        float b = (col         & 0xFF) / 255f;
        float[] hsv = rgbToHsv(r, g, b);
        hue = hsv[0]; sat = hsv[1]; val = hsv[2];
    }

    private void rebuildSvCache() {
        for (int row = 0; row < SV; row++) {
            float v = 1f - (float) row / SV;
            for (int col = 0; col < SV; col++) {
                float s = (float) col / SV;
                svCache[row][col] = hsvToRgb(hue, s, v) | 0xFF000000;
            }
        }
        lastCachedHue = hue;
    }

    private void buildHueCache() {
        for (int row = 0; row < SV; row++) {
            hueCache[row] = hsvToRgb((float) row / SV, 1f, 1f) | 0xFF000000;
        }
        hueBuilt = true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        Minecraft mc = Minecraft.getInstance();
        long window = mc.getWindow().handle();

        // Rebuild caches only when needed
        if (!hueBuilt) buildHueCache();
        if (Math.abs(hue - lastCachedHue) > 0.008f) rebuildSvCache();

        int px = (width  - W) / 2;
        int py = (height - H) / 2;
        int svX = px + 10;
        int svY = py + 30;
        int hX  = svX + SV + 8;
        int hY  = svY;

        // Background
        g.fill(px, py, px + W, py + H, 0xF0101020);
        g.renderOutline(px, py, W, H, 0xFF0a84ff);
        g.drawCenteredString(mc.font, "Color Picker", px + W / 2, py + 8, 0xFFFFFFFF);

        // SV square — draw each pixel from cache
        for (int row = 0; row < SV; row++) {
            for (int col = 0; col < SV; col++) {
                g.fill(svX + col, svY + row, svX + col + 1, svY + row + 1, svCache[row][col]);
            }
        }
        g.renderOutline(svX, svY, SV, SV, 0xFF444466);

        // SV cursor
        int curX = svX + (int)(sat * SV);
        int curY = svY + (int)((1f - val) * SV);
        g.fill(curX - 3, curY - 1, curX + 3, curY + 1, 0xFFFFFFFF);
        g.fill(curX - 1, curY - 3, curX + 1, curY + 3, 0xFFFFFFFF);

        // Hue bar — draw each row from cache
        for (int row = 0; row < SV; row++) {
            g.fill(hX, hY + row, hX + HW, hY + row + 1, hueCache[row]);
        }
        g.renderOutline(hX, hY, HW, SV, 0xFF444466);

        // Hue cursor
        int hueCurY = hY + (int)(hue * SV);
        g.fill(hX - 2, hueCurY - 1, hX + HW + 2, hueCurY + 1, 0xFFFFFFFF);

        // Preview swatch
        int previewY = svY + SV + 10;
        int previewColor = hsvToRgb(hue, sat, val) | 0xFF000000;
        g.fill(svX, previewY, svX + 40, previewY + 16, previewColor);
        g.renderOutline(svX, previewY, 40, 16, 0xFF666688);
        String hex = String.format("#%06X", hsvToRgb(hue, sat, val));
        g.drawString(mc.font, hex, svX + 46, previewY + 4, 0xFFAAAAAA, false);

        // Done button
        int btnX = px + W - 54;
        int btnY = py + H - 24;
        g.fill(btnX, btnY, btnX + 44, btnY + 16, 0xFF003366);
        g.renderOutline(btnX, btnY, 44, 16, 0xFF0a84ff);
        g.drawCenteredString(mc.font, "Done", btnX + 22, btnY + 4, 0xFFFFFFFF);

        // Input handling
        boolean leftDown = GLFW.glfwGetMouseButton(window,
                GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        double[] cx = new double[1], cy = new double[1];
        GLFW.glfwGetCursorPos(window, cx, cy);
        double guiScale = mc.getWindow().getGuiScale();
        int mx = (int)(cx[0] / guiScale);
        int my = (int)(cy[0] / guiScale);

        if (leftDown) {
            if (!wasDown) {
                if (mx >= svX && mx <= svX + SV && my >= svY && my <= svY + SV)
                    draggingPicker = true;
                if (mx >= hX && mx <= hX + HW && my >= hY && my <= hY + SV)
                    draggingHue = true;
                if (mx >= btnX && mx <= btnX + 44 && my >= btnY && my <= btnY + 16) {
                    applyColor();
                    mc.setScreen(parent);
                }
            }
            if (draggingPicker) {
                sat = Math.max(0f, Math.min(1f, (float)(mx - svX) / SV));
                val = Math.max(0f, Math.min(1f, 1f - (float)(my - svY) / SV));
                applyColor();
            }
            if (draggingHue) {
                hue = Math.max(0f, Math.min(1f, (float)(my - hY) / SV));
                applyColor();
            }
        } else {
            draggingPicker = false;
            draggingHue    = false;
        }

        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS)
            mc.setScreen(parent);

        wasDown = leftDown;
        super.render(g, mouseX, mouseY, delta);
    }

    private void applyColor() {
        setting.setValue(hsvToRgb(hue, sat, val));
    }

    private int hsvToRgb(float h, float s, float v) {
        return java.awt.Color.HSBtoRGB(h, s, v) & 0x00FFFFFF;
    }

    private float[] rgbToHsv(float r, float g, float b) {
        float[] hsv = new float[3];
        java.awt.Color.RGBtoHSB((int)(r*255),(int)(g*255),(int)(b*255), hsv);
        return hsv;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}