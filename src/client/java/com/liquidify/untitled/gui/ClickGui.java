package com.liquidify.untitled.gui;

import com.liquidify.untitled.module.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static com.liquidify.untitled.gui.Panel.listeningKeybind;

public class ClickGui extends Screen {
    private final List<Panel> panels = new ArrayList<>();

    private boolean wasLeftDownPrev  = false;
    private boolean wasRightDownPrev = false;

    public ClickGui() {
        super(Component.literal("ClickGUI"));
        int x = 10;
        for (Category cat : Category.values()) {
            panels.add(new Panel(cat, x, 10));
            x += 165;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        Minecraft mc = Minecraft.getInstance();
        long window = mc.getWindow().handle();

        boolean leftDown  = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT)  == GLFW.GLFW_PRESS;
        boolean rightDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        double[] cx = new double[1], cy = new double[1];
        GLFW.glfwGetCursorPos(window, cx, cy);
        double scale = mc.getWindow().getGuiScale();
        double mx = cx[0] / scale;
        double my = cy[0] / scale;

        if (leftDown && !wasLeftDownPrev) {
            for (Panel panel : panels) {
                if (panel.mouseClicked(mx, my, 0)) break;
            }
        }
        if (rightDown && !wasRightDownPrev) {
            for (Panel panel : panels) {
                if (panel.mouseClicked(mx, my, 1)) break;
            }
        }
        if (leftDown) {
            for (Panel panel : panels) {
                if (panel.mouseDragged(mx, my)) break;
            }
        }
        if (!leftDown && wasLeftDownPrev) {
            panels.forEach(Panel::mouseReleased);
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
            if (!Panel.isListening()) {
                mc.setScreen(null);
            }
        }

        wasLeftDownPrev  = leftDown;
        wasRightDownPrev = rightDown;

        graphics.fill(0, 0, this.width, this.height, 0x70000000);
        for (Panel panel : panels) {
            panel.render(graphics, (int) mx, (int) my);
        }
    }
    public static boolean isListening() {
        return listeningKeybind != null;
    }

    @Override
    public boolean isPauseScreen() { return true; }
}