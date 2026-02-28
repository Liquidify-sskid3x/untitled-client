package com.liquidify.untitled.module.settings;

import java.util.Arrays;
import java.util.List;

public class StringSetting extends Setting<String> {
    private final List<String> options;

    public StringSetting(String name, String defaultValue, String... options) {
        super(name, defaultValue);
        this.options = Arrays.asList(options);
    }

    public List<String> getOptions() { return options; }

    public void next() {
        int idx = options.indexOf(value);
        value = options.get((idx + 1) % options.size());
    }

    public void prev() {
        int idx = options.indexOf(value);
        value = options.get((idx - 1 + options.size()) % options.size());
    }
}