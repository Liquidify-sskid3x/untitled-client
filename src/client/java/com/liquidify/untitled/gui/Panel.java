package com.liquidify.untitled.gui;

import com.liquidify.untitled.UntitledClient;
import com.liquidify.untitled.module.Category;
import com.liquidify.untitled.module.Module;
import com.liquidify.untitled.module.settings.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class Panel {
    private final Category category;
    private int x, y;

    private static final int WIDTH      = 150;
    private static final int HEADER_H   = 16;
    private static final int MODULE_H   = 14;
    private static final int SETTING_H  = 22;
    private static final int SLIDER_W   = 72;
    private static final int BOX_W      = 40;
    private static final int PADDING    = 5;

    private static final int COLOR_HEADER  = 0xFF1a1a2e;
    private static final int COLOR_MODULE  = 0xFF0f0f23;
    private static final int COLOR_ACTIVE  = 0xFF16213e;
    private static final int COLOR_ACCENT  = 0xFF0a84ff;
    private static final int COLOR_HOVER   = 0x220a84ff;
    private static final int COLOR_SETTING = 0xFF0d0d1f;
    private static final int COLOR_BANNED  = 0xFFFF4444;

    private boolean dragging = false;
    private double dragOffX, dragOffY;
    private Module expandedModule = null;
    private FloatSetting draggingSlider = null;
    private int sliderX, sliderWidth;
    private FloatSetting editingBox = null;
    private String boxBuffer = "";

    // Static so all panels share one listening state
    static KeybindSetting listeningKeybind = null;
    private static String bannedMessage = null;
    private static long bannedMessageTime = 0;

    private static final boolean[] prevKeys = new boolean[GLFW.GLFW_KEY_LAST];

    public Panel(Category category, int x, int y) {
        this.category = category;
        this.x = x;
        this.y = y;
    }

    public static boolean isListening() {
        return listeningKeybind != null;
    }

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        long window = Minecraft.getInstance().getWindow().handle();

        // ── Keybind listening poll ──
        if (listeningKeybind != null) {
            for (int key = GLFW.GLFW_KEY_SPACE; key < GLFW.GLFW_KEY_LAST; key++) {
                boolean down = GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
                if (down && !prevKeys[key]) {
                    if (key == GLFW.GLFW_KEY_ESCAPE) {
                        // ESC = unbind
                        listeningKeybind.setValue(-1);
                        listeningKeybind.setBinding(false);
                        listeningKeybind = null;
                        bannedMessage = null;
                    } else if (KeybindSetting.BANNED_KEYS.contains(key)) {
                        bannedMessage = "Key is banned!";
                        bannedMessageTime = System.currentTimeMillis();
                    } else {
                        listeningKeybind.setValue(key);
                        listeningKeybind.setBinding(false);
                        listeningKeybind = null;
                        bannedMessage = null;
                    }
                }
                prevKeys[key] = down;
            }
            if (bannedMessage != null && System.currentTimeMillis() - bannedMessageTime > 2000) {
                bannedMessage = null;
            }
        }

        // ── Textbox key polling ──
        if (editingBox != null) {
            for (int key = 0; key < GLFW.GLFW_KEY_LAST; key++) {
                boolean down = GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
                if (down && !prevKeys[key]) {
                    if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER
                            || key == GLFW.GLFW_KEY_KP_ENTER) {
                        commitBoxValue();
                    } else if (key == GLFW.GLFW_KEY_BACKSPACE && !boxBuffer.isEmpty()) {
                        boxBuffer = boxBuffer.substring(0, boxBuffer.length() - 1);
                    } else {
                        String keyName = GLFW.glfwGetKeyName(key, 0);
                        if (keyName != null && keyName.length() == 1) {
                            char c = keyName.charAt(0);
                            if (Character.isDigit(c) || c == '.') boxBuffer += c;
                        }
                        if (key == GLFW.GLFW_KEY_MINUS && boxBuffer.isEmpty()) boxBuffer = "-";
                    }
                }
                prevKeys[key] = down;
            }
        }

        List<Module> modules = UntitledClient.moduleManager.getModulesByCategory(category);

        // Header
        g.fill(x, y, x + WIDTH, y + HEADER_H, COLOR_HEADER);
        g.fill(x, y, x + 2, y + HEADER_H, COLOR_ACCENT);
        g.drawString(font, category.name(), x + 6, y + 4, 0xFFFFFFFF, false);

        int mY = y + HEADER_H;
        for (Module module : modules) {
            g.fill(x, mY, x + WIDTH, mY + MODULE_H,
                    module.isEnabled() ? COLOR_ACTIVE : COLOR_MODULE);
            if (isHovered(mouseX, mouseY, x, mY, WIDTH, MODULE_H))
                g.fill(x, mY, x + WIDTH, mY + MODULE_H, COLOR_HOVER);

            int textColor = module.isEnabled() ? COLOR_ACCENT : 0xFFAAAAAA;
            g.drawString(font, module.getName(), x + PADDING, mY + 3, textColor, false);

            if (!module.getSettings().isEmpty()) {
                String arrow = expandedModule == module ? "v" : ">";
                g.drawString(font, arrow, x + WIDTH - 10, mY + 3, 0xFF555577, false);
            }
            mY += MODULE_H;

            if (expandedModule == module) {
                for (Setting<?> setting : module.getSettings()) {
                    mY = renderSetting(g, font, setting, mY, mouseX, mouseY);
                }
            }
        }

        g.renderOutline(x, y, WIDTH, mY - y, COLOR_ACCENT);

        // Banned message only on the panel owning the listening keybind
        if (bannedMessage != null && isListeningInThisPanel()) {
            int msgW = font.width(bannedMessage) + 8;
            g.fill(x, mY + 2, x + msgW, mY + 13, 0xFF330000);
            g.renderOutline(x, mY + 2, msgW, 11, COLOR_BANNED);
            g.drawString(font, bannedMessage, x + 4, mY + 4, COLOR_BANNED, false);
        }
    }

    private int renderSetting(GuiGraphics g, net.minecraft.client.gui.Font font,
                              Setting<?> setting, int mY, int mouseX, int mouseY) {
        g.fill(x, mY, x + WIDTH, mY + SETTING_H, COLOR_SETTING);

        if (setting instanceof FloatSetting fs) {
            g.drawString(font, setting.getName(), x + PADDING, mY + 3, 0xFF8888AA, false);

            int barY = mY + 13;
            int barX = x + PADDING;

            // Track
            g.fill(barX, barY, barX + SLIDER_W, barY + 4, 0xFF333355);
            // Fill
            float pct = (fs.getValue() - fs.getMin()) / (fs.getMax() - fs.getMin());
            g.fill(barX, barY, barX + (int)(SLIDER_W * pct), barY + 4, COLOR_ACCENT);
            // Handle
            int handleX = barX + (int)(SLIDER_W * pct) - 2;
            g.fill(handleX, barY - 2, handleX + 4, barY + 6, 0xFFFFFFFF);

            // Textbox
            int boxX = barX + SLIDER_W + PADDING;
            int boxY = barY - 1;
            boolean active = editingBox == fs;
            String boxStr = active
                    ? (boxBuffer.isEmpty() ? "|" : boxBuffer + "|")
                    : String.format("%.1f", fs.getValue());
            g.fill(boxX, boxY, boxX + BOX_W, boxY + 10, 0xFF222244);
            g.renderOutline(boxX, boxY, BOX_W, 10, active ? COLOR_ACCENT : 0xFF444466);
            // Clip text
            String display = boxStr;
            while (font.width(display) > BOX_W - 4 && display.length() > 1)
                display = display.substring(1);
            g.drawString(font, display, boxX + 2, boxY + 1, 0xFFFFFFFF, false);

            mY += SETTING_H + 2;

        } else if (setting instanceof BooleanSetting bs) {
            g.drawString(font, setting.getName(), x + PADDING, mY + 7, 0xFF8888AA, false);
            int checkX = x + WIDTH - 15;
            int checkY = mY + 6;
            g.fill(checkX, checkY, checkX + 10, checkY + 10, 0xFF222244);
            g.renderOutline(checkX, checkY, 10, 10, COLOR_ACCENT);
            if (bs.getValue())
                g.fill(checkX + 2, checkY + 2, checkX + 8, checkY + 8, COLOR_ACCENT);
            mY += SETTING_H;

        } else if (setting instanceof StringSetting ss) {
            g.drawString(font, setting.getName(), x + PADDING, mY + 4, 0xFF8888AA, false);
            String val = "< " + ss.getValue() + " >";
            int valW = font.width(val);
            if (valW + font.width(setting.getName()) + PADDING * 3 > WIDTH) {
                g.drawString(font, val, x + PADDING, mY + 13, COLOR_ACCENT, false);
            } else {
                g.drawString(font, val, x + WIDTH - valW - PADDING, mY + 4, COLOR_ACCENT, false);
            }
            mY += SETTING_H;

        } else if (setting instanceof ColorSetting cs) {
            g.drawString(font, setting.getName(), x + PADDING, mY + 7, 0xFF8888AA, false);
            int previewX = x + WIDTH - 16;
            int previewY = mY + 5;
            g.fill(previewX, previewY, previewX + 12, previewY + 12,
                    cs.getValue() | 0xFF000000);
            g.renderOutline(previewX, previewY, 12, 12, 0xFF666688);
            mY += SETTING_H;

        } else if (setting instanceof KeybindSetting ks) {
            boolean isListening = listeningKeybind == ks;
            String label;
            int labelColor;
            if (isListening && bannedMessage != null) {
                label = bannedMessage;
                labelColor = COLOR_BANNED;
            } else if (isListening) {
                label = "PRESS KEY...";
                labelColor = 0xFFFFAA00;
            } else {
                label = "Key: " + ks.getKeyName();
                labelColor = 0xFF8888AA;
            }
            g.drawString(font, label, x + PADDING, mY + 7, labelColor, false);
            g.renderOutline(x + 2, mY + 2, WIDTH - 4, SETTING_H - 4,
                    isListening ? 0xFFFFAA00 : 0xFF333355);
            mY += SETTING_H;
        }

        return mY;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (listeningKeybind != null) {
            listeningKeybind.setBinding(false);
            listeningKeybind = null;
            bannedMessage = null;
            return true;
        }

        if (button == 0 && isHovered(mouseX, mouseY, x, y, WIDTH, HEADER_H)) {
            dragging = true;
            dragOffX = mouseX - x;
            dragOffY = mouseY - y;
            return true;
        }

        List<Module> modules = UntitledClient.moduleManager.getModulesByCategory(category);
        int mY = y + HEADER_H;

        for (Module module : modules) {
            if (isHovered(mouseX, mouseY, x, mY, WIDTH, MODULE_H)) {
                if (button == 0) module.toggle();
                else if (button == 1) {
                    expandedModule = (expandedModule == module) ? null : module;
                    editingBox = null;
                    boxBuffer = "";
                    bannedMessage = null;
                }
                return true;
            }
            mY += MODULE_H;

            if (expandedModule == module) {
                for (Setting<?> setting : module.getSettings()) {
                    if (setting instanceof FloatSetting fs) {
                        int barY = mY + 13;
                        int barX = x + PADDING;
                        int boxX = barX + SLIDER_W + PADDING;
                        int boxY = barY - 1;

                        if (isHovered(mouseX, mouseY, barX, barY - 2, SLIDER_W, 8)) {
                            draggingSlider = fs;
                            sliderX = barX;
                            sliderWidth = SLIDER_W;
                            updateSlider(mouseX);
                            editingBox = null;
                            return true;
                        }
                        if (isHovered(mouseX, mouseY, boxX, boxY, BOX_W, 10)) {
                            editingBox = (editingBox == fs) ? null : fs;
                            boxBuffer = editingBox != null
                                    ? String.format("%.1f", fs.getValue()) : "";
                            return true;
                        }
                        mY += SETTING_H + 2;

                    } else if (setting instanceof BooleanSetting bs) {
                        int checkX = x + WIDTH - 15;
                        int checkY = mY + 6;
                        if (isHovered(mouseX, mouseY, checkX, checkY, 10, 10)) {
                            bs.toggle();
                            return true;
                        }
                        mY += SETTING_H;

                    } else if (setting instanceof StringSetting ss) {
                        if (isHovered(mouseX, mouseY, x, mY, WIDTH, SETTING_H)) {
                            if (button == 0) ss.next();
                            else ss.prev();
                            return true;
                        }
                        mY += SETTING_H;

                    } else if (setting instanceof ColorSetting) {
                        mY += SETTING_H;

                    } else if (setting instanceof KeybindSetting ks) {
                        if (isHovered(mouseX, mouseY, x + 2, mY + 2, WIDTH - 4, SETTING_H - 4)) {
                            listeningKeybind = ks;
                            ks.setBinding(true);
                            bannedMessage = null;
                            return true;
                        }
                        mY += SETTING_H;
                    }
                }
            }
        }

        if (editingBox != null) commitBoxValue();
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY) {
        if (dragging) {
            x = (int)(mouseX - dragOffX);
            y = (int)(mouseY - dragOffY);
            return true;
        }
        if (draggingSlider != null) {
            updateSlider(mouseX);
            return true;
        }
        return false;
    }

    public void mouseReleased() {
        dragging = false;
        draggingSlider = null;
    }

    public boolean keyPressed(int keyCode) { return false; }
    public boolean charTyped(char chr) { return false; }

    private void updateSlider(double mouseX) {
        float pct = (float)((mouseX - sliderX) / sliderWidth);
        pct = Math.max(0f, Math.min(1f, pct));
        draggingSlider.setValue(draggingSlider.getMin() +
                pct * (draggingSlider.getMax() - draggingSlider.getMin()));
    }

    private void commitBoxValue() {
        try { editingBox.setValue(Float.parseFloat(boxBuffer)); }
        catch (NumberFormatException ignored) {}
        editingBox = null;
        boxBuffer = "";
    }

    private boolean isListeningInThisPanel() {
        if (listeningKeybind == null || expandedModule == null) return false;
        return expandedModule.getSettings().contains(listeningKeybind);
    }

    private boolean isHovered(double mx, double my, int px, int py, int w, int h) {
        return mx >= px && mx <= px + w && my >= py && my <= py + h;
    }
}