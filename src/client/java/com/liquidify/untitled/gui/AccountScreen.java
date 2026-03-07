package com.liquidify.untitled.gui;

import com.liquidify.untitled.accounts.CrackedAccount;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class AccountScreen extends Screen {
    private final Screen parent;

    public static final List<CrackedAccount> accounts = new ArrayList<>();
    private static CrackedAccount activeAccount = null;

    private String inputBuffer = "";
    private boolean inputFocused = false;
    private long lastKey = 0;

    private String message = null;
    private int messageColor = 0xFF44FF88;
    private long messageTime = 0;

    private int scrollOffset = 0;
    private boolean wasLeftDown = false;

    private static final int PANEL_W   = 320;
    private static final int PANEL_H   = 300;
    private static final int ENTRY_H   = 22;
    private static final int COLOR_BG  = 0xF0080818;
    private static final int COLOR_HDR = 0xFF1a1a2e;
    private static final int COLOR_ACC = 0xFF0a84ff;
    private static final int COLOR_ACT = 0xFF16213e;
    private static final int COLOR_HOV = 0x220a84ff;
    private static final int COLOR_TXT = 0xFFFFFFFF;
    private static final int COLOR_SUB = 0xFF8888AA;
    private static final int COLOR_RED = 0xFFFF4444;
    private static final int COLOR_GRN = 0xFF44FF88;

    public AccountScreen(Screen parent) {
        super(Component.literal("Account Manager"));
        this.parent = parent;
    }

    private int px() { return (width  - PANEL_W) / 2; }
    private int py() { return (height - PANEL_H) / 2; }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xBB000000);

        int px = px(), py = py();

        // Panel
        g.fill(px, py, px + PANEL_W, py + PANEL_H, COLOR_BG);
        g.renderOutline(px, py, PANEL_W, PANEL_H, COLOR_ACC);

        // Header
        g.fill(px, py, px + PANEL_W, py + 22, COLOR_HDR);
        g.fill(px, py, px + 3, py + 22, COLOR_ACC);
        g.drawCenteredString(font, "Untitled Client — Accounts",
                px + PANEL_W / 2, py + 7, COLOR_TXT);

        // Current account
        String current = Minecraft.getInstance().getUser().getName();
        g.drawString(font, "Current: " + current +
                        (activeAccount != null ? " (cracked)" : " (original)"),
                px + 8, py + 28, COLOR_SUB, false);
        g.fill(px + 4, py + 38, px + PANEL_W - 4, py + 39, 0xFF222244);

        // Input label
        g.drawString(font, "Add Account:", px + 8, py + 44, COLOR_SUB, false);

        // Input box
        int boxW = PANEL_W - 16 - 52;
        int boxX = px + 8, boxY = py + 54;
        g.fill(boxX, boxY, boxX + boxW, boxY + 16, 0xFF0a0a1a);
        g.renderOutline(boxX, boxY, boxW, 16,
                inputFocused ? COLOR_ACC : 0xFF333355);

        String display = inputBuffer.isEmpty() && !inputFocused
                ? "Click here to type..." : inputBuffer + (inputFocused ? "|" : "");
        int displayColor = inputBuffer.isEmpty() && !inputFocused ? 0xFF333355 : COLOR_TXT;
        g.drawString(font, display, boxX + 4, boxY + 4, displayColor, false);

        // Add button
        int addX = px + PANEL_W - 50, addY = py + 54;
        boolean addHov = inBounds(mouseX, mouseY, addX, addY, 44, 16);
        g.fill(addX, addY, addX + 44, addY + 16,
                addHov ? 0xFF005599 : 0xFF003366);
        g.renderOutline(addX, addY, 44, 16, COLOR_ACC);
        g.drawCenteredString(font, "Add", addX + 22, addY + 4, COLOR_TXT);

        // Divider + list header
        g.fill(px + 4, py + 75, px + PANEL_W - 4, py + 76, 0xFF222244);
        g.drawString(font, "Saved Accounts (" + accounts.size() + ")",
                px + 8, py + 80, COLOR_SUB, false);

        // List
        int listY = py + 92;
        int listH = PANEL_H - 92 - 36;
        g.enableScissor(px, listY, px + PANEL_W, listY + listH);

        if (accounts.isEmpty()) {
            g.drawCenteredString(font, "No accounts saved yet.",
                    px + PANEL_W / 2, listY + listH / 2 - 4, 0xFF444466);
        }

        for (int i = scrollOffset; i < accounts.size(); i++) {
            CrackedAccount acc = accounts.get(i);
            int ey = listY + (i - scrollOffset) * ENTRY_H;
            if (ey + ENTRY_H > listY + listH) break;

            boolean isActive = acc == activeAccount;
            boolean rowHov = inBounds(mouseX, mouseY, px, ey, PANEL_W, ENTRY_H);

            g.fill(px, ey, px + PANEL_W, ey + ENTRY_H,
                    isActive ? COLOR_ACT : (rowHov ? COLOR_HOV : 0));
            if (isActive) g.fill(px, ey, px + 2, ey + ENTRY_H, COLOR_ACC);

            g.drawString(font, acc.name, px + 8, ey + 4,
                    isActive ? COLOR_ACC : COLOR_TXT, false);
            g.drawString(font, acc.uuid.toString().substring(0, 8) + "...",
                    px + 8, ey + 13, 0xFF555577, false);

            int btnY = ey + 5;

            // Login button
            int useX = px + PANEL_W - 88;
            boolean useHov = inBounds(mouseX, mouseY, useX, btnY, 40, 12);
            g.fill(useX, btnY, useX + 40, btnY + 12,
                    useHov ? 0xFF005599 : 0xFF003366);
            g.renderOutline(useX, btnY, 40, 12,
                    isActive ? COLOR_GRN : COLOR_ACC);
            g.drawCenteredString(font, isActive ? "Active" : "Login",
                    useX + 20, btnY + 2,
                    isActive ? COLOR_GRN : COLOR_TXT);
            // Delete button
            int delX = px + PANEL_W - 44;
            boolean delHov = inBounds(mouseX, mouseY, delX, btnY, 38, 12);
            g.fill(delX, btnY, delX + 38, btnY + 12,
                    delHov ? 0xFF660000 : 0xFF2a0000);
            g.renderOutline(delX, btnY, 38, 12, COLOR_RED);
            g.drawCenteredString(font, "Delete", delX + 19, btnY + 2, COLOR_RED);
        }

        g.disableScissor();
        g.fill(px + 4, listY + listH, px + PANEL_W - 4, listY + listH + 1, 0xFF222244);

        // Back button
        int backX = px + 6, backY = py + PANEL_H - 28;
        boolean backHov = inBounds(mouseX, mouseY, backX, backY, 50, 18);
        g.fill(backX, backY, backX + 50, backY + 18,
                backHov ? 0xFF222244 : 0xFF111133);
        g.renderOutline(backX, backY, 50, 18, 0xFF333355);
        g.drawCenteredString(font, "Back", backX + 25, backY + 5, COLOR_SUB);

        // Message
        if (message != null) {
            if (System.currentTimeMillis() - messageTime > 3000) {
                message = null;
            } else {
                g.drawCenteredString(font, message,
                        px + PANEL_W / 2, py + PANEL_H - 30, messageColor);
            }
        }

        // GLFW mouse polling — fixes click detection in 1.21.11
        long window = Minecraft.getInstance().getWindow().handle();
        double[] cx = new double[1], cy = new double[1];
        GLFW.glfwGetCursorPos(window, cx, cy);
        double scale = Minecraft.getInstance().getWindow().getGuiScale();
        int mx = (int)(cx[0] / scale);
        int my = (int)(cy[0] / scale);

        boolean leftDown = GLFW.glfwGetMouseButton(window,
                GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (leftDown && !wasLeftDown) {
            handleClick(mx, my);
        }
        wasLeftDown = leftDown;

        // Scroll via GLFW not available directly — handled in mouseScrolled
        pollKeyboard();

        super.render(g, mouseX, mouseY, delta);
    }

    private void handleClick(int mouseX, int mouseY) {
        int px = px(), py = py();

        // Back button
        if (inBounds(mouseX, mouseY, px + 6, py + PANEL_H - 28, 50, 18)) {
            Minecraft.getInstance().setScreen(parent);
            return;
        }

        // Input box focus
        int boxW = PANEL_W - 16 - 52;
        inputFocused = inBounds(mouseX, mouseY, px + 8, py + 54, boxW, 16);

        // Add button
        if (inBounds(mouseX, mouseY, px + PANEL_W - 50, py + 54, 44, 16)) {
            addAccount();
            return;
        }

        // Account list
        int listY = py + 92;
        int listH = PANEL_H - 92 - 36;

        for (int i = scrollOffset; i < accounts.size(); i++) {
            CrackedAccount acc = accounts.get(i);
            int ey = listY + (i - scrollOffset) * ENTRY_H;
            if (ey + ENTRY_H > listY + listH) break;

            int btnY = ey + 5;

            // Login
            if (inBounds(mouseX, mouseY, px + PANEL_W - 88, btnY, 40, 12)) {
                loginAs(acc);
                return;
            }
            // Delete
            if (inBounds(mouseX, mouseY, px + PANEL_W - 44, btnY, 38, 12)) {
                if (acc == activeAccount) {
                    showMessage("Cannot delete active account!", COLOR_RED);
                } else {
                    accounts.remove(acc);
                    showMessage("Account removed.", COLOR_GRN);
                }
                return;
            }
        }
    }

    private void pollKeyboard() {
        if (!inputFocused) return;
        long window = Minecraft.getInstance().getWindow().handle();
        long now = System.currentTimeMillis();
        if (now - lastKey < 75) return;

        for (int key = 0; key < GLFW.GLFW_KEY_LAST; key++) {
            if (GLFW.glfwGetKey(window, key) != GLFW.GLFW_PRESS) continue;

            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!inputBuffer.isEmpty()) {
                    inputBuffer = inputBuffer.substring(0, inputBuffer.length() - 1);
                    lastKey = now;
                }
                return;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                addAccount();
                lastKey = now;
                return;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                inputFocused = false;
                lastKey = now;
                return;
            }

            String keyName = GLFW.glfwGetKeyName(key, 0);
            if (keyName != null && keyName.length() == 1 && inputBuffer.length() < 16) {
                char c = keyName.charAt(0);
                boolean shift =
                        GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
                if (Character.isLetterOrDigit(c) || c == '_') {
                    inputBuffer += shift ? Character.toUpperCase(c) : c;
                    lastKey = now;
                    return;
                }
            }
        }
    }

    private void addAccount() {
        String name = inputBuffer.trim();
        if (name.isEmpty()) { showMessage("Enter a username first!", COLOR_RED); return; }
        if (name.length() < 3 || name.length() > 16) {
            showMessage("Name must be 3-16 characters!", COLOR_RED); return;
        }
        if (!name.matches("[a-zA-Z0-9_]+")) {
            showMessage("Only letters, numbers and _ allowed!", COLOR_RED); return;
        }
        if (accounts.stream().anyMatch(a -> a.name.equalsIgnoreCase(name))) {
            showMessage("Account already exists!", COLOR_RED); return;
        }
        CrackedAccount acc = new CrackedAccount(name);
        accounts.add(acc);
        inputBuffer = "";
        loginAs(acc);
    }

    private void loginAs(CrackedAccount acc) {
        if (acc.login()) {
            activeAccount = acc;
            showMessage("Switched to " + acc.name + "!", COLOR_GRN);
        } else {
            showMessage("Failed to switch account!", COLOR_RED);
        }
    }
    public static List<CrackedAccount> getAccounts() {
        return AccountScreen.accounts;
    }
    private void showMessage(String msg, int color) {
        message = msg;
        messageColor = color;
        messageTime = System.currentTimeMillis();
    }

    private boolean inBounds(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}