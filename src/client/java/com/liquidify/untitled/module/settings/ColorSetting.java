package com.liquidify.untitled.module.settings;

public class ColorSetting extends Setting<Integer> {
    public ColorSetting(String name, int defaultColor) {
        super(name, defaultColor);
    }

    public int getR() { return (value >> 16) & 0xFF; }
    public int getG() { return (value >> 8) & 0xFF; }
    public int getB() { return value & 0xFF; }
}