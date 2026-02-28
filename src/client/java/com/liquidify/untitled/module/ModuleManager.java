package com.liquidify.untitled.module;

import com.liquidify.untitled.module.modules.combat.*;
import com.liquidify.untitled.module.modules.movement.*;
import com.liquidify.untitled.module.modules.player.*;
import com.liquidify.untitled.module.modules.render.*;
import com.liquidify.untitled.module.settings.KeybindSetting;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();

    public ModuleManager() {
        modules.add(new KillAura());
        modules.add(new AirPlace());
        modules.add(new Reach());
        modules.add(new AimAssist());
        modules.add(new TriggerBot());
        modules.add(new Velocity());
        modules.add(new ESP());
        modules.add(new Speed());
        modules.add(new Fly());
        modules.add(new NoFall());
        modules.add(new Scaffold());
        modules.add(new Safewalk());
        modules.add(new Jesus());
        modules.add(new AutoTotem());
        modules.add(new AutoArmor());
        modules.add(new Criticals());
    }

    public void onTick(Minecraft client) {
        if (client.player == null) return;
        for (Module module : modules) {
            if (module.isEnabled()) module.onTick(client);
        }
    }

    public void onKeyPress(int key) {
        if (key == GLFW.GLFW_KEY_ESCAPE) return;
        for (Module module : modules) {
            module.getSettings().stream()
                    .filter(s -> s instanceof KeybindSetting)
                    .map(s -> (KeybindSetting) s)
                    .filter(ks -> ks.getValue() == key && !ks.isBinding())
                    .findFirst()
                    .ifPresent(ks -> module.toggle());
        }
    }

    public List<Module> getModules() { return modules; }

    public List<Module> getModulesByCategory(Category category) {
        return modules.stream().filter(m -> m.getCategory() == category).toList();
    }
}