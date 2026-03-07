package com.liquidify.untitled.module.settings;

import org.lwjgl.glfw.GLFW;
import java.util.Set;

public class KeybindSetting extends Setting<Integer> {
    private boolean binding = false;

    // Keys that cannot be bound
    public static final Set<Integer> BANNED_KEYS = Set.of(
            GLFW.GLFW_KEY_F1, GLFW.GLFW_KEY_F2, GLFW.GLFW_KEY_F3,
            GLFW.GLFW_KEY_F4, GLFW.GLFW_KEY_F5, GLFW.GLFW_KEY_F6,
            GLFW.GLFW_KEY_F7, GLFW.GLFW_KEY_F8, GLFW.GLFW_KEY_F9,
            GLFW.GLFW_KEY_F10, GLFW.GLFW_KEY_F11, GLFW.GLFW_KEY_F12,
            GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT,
            GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL,
            GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT,
            GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER,
            GLFW.GLFW_KEY_TAB, GLFW.GLFW_KEY_CAPS_LOCK,
            GLFW.GLFW_KEY_PRINT_SCREEN, GLFW.GLFW_KEY_PAUSE
    );

    public KeybindSetting(String name) {
        super(name, -1);
    }

    public boolean isBinding() { return binding; }
    public void setBinding(boolean binding) { this.binding = binding; }
    public boolean isUnbound() { return value == -1; }

    public String getKeyName() {
        if (value == -1) return "NONE";
        String name = GLFW.glfwGetKeyName(value, 0);
        if (name != null) return name.toUpperCase();
        return switch (value) {
            case GLFW.GLFW_KEY_SPACE        -> "SPACE";
            case GLFW.GLFW_KEY_UP           -> "UP";
            case GLFW.GLFW_KEY_DOWN         -> "DOWN";
            case GLFW.GLFW_KEY_LEFT         -> "LEFT";
            case GLFW.GLFW_KEY_RIGHT        -> "RIGHT";
            case GLFW.GLFW_KEY_INSERT       -> "INSERT";
            case GLFW.GLFW_KEY_DELETE       -> "DELETE";
            case GLFW.GLFW_KEY_HOME         -> "HOME";
            case GLFW.GLFW_KEY_END          -> "END";
            case GLFW.GLFW_KEY_PAGE_UP      -> "PGUP";
            case GLFW.GLFW_KEY_PAGE_DOWN    -> "PGDN";
            case GLFW.GLFW_KEY_NUM_LOCK     -> "NUMLOCK";
            case GLFW.GLFW_KEY_KP_ENTER     -> "KP_ENTER";
            default -> "KEY_" + value;
        };
    }
}