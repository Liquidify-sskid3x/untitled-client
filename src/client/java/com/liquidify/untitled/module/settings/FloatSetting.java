package com.liquidify.untitled.module.settings;

public class FloatSetting extends Setting<Float> {
    private final float min, max;

    public FloatSetting(String name, float defaultValue, float min, float max) {
        super(name, defaultValue);
        this.min = min;
        this.max = max;
    }

    public float getMin() { return min; }
    public float getMax() { return max; }

    public void setValue(float value) {
        this.value = Math.max(min, Math.min(max, value));
    }
}