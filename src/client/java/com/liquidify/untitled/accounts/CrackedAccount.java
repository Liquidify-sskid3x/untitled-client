package com.liquidify.untitled.accounts;

import com.liquidify.untitled.config.ConfigManager;
import com.liquidify.untitled.mixin.MinecraftClientAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.liquidify.untitled.gui.AccountScreen.accounts;

public class CrackedAccount {
    public final String name;
    public final UUID uuid;

    public CrackedAccount(String name) {
        this.name = name;
        this.uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes());
    }
    public static List<CrackedAccount> getAccounts() {
        return accounts;
    }
    public boolean login() {
        try {
            Minecraft mc = Minecraft.getInstance();
            MinecraftClientAccessor accessor = (MinecraftClientAccessor)(Object) mc;

            for (var ctor : User.class.getConstructors()) {
                try {
                    Object[] args = buildArgs(ctor.getParameterTypes());
                    User user = (User) ctor.newInstance(args);
                    accessor.untitled$setUser(user);
                    // Save after successful login
                    ConfigManager.save();
                    return true;
                } catch (Exception ignored) {}
            }

            // Last resort: raw field reflection
            var field = Minecraft.class.getDeclaredField("user");
            field.setAccessible(true);
            for (var ctor : User.class.getDeclaredConstructors()) {
                try {
                    ctor.setAccessible(true);
                    Object[] args = buildArgs(ctor.getParameterTypes());
                    field.set(mc, ctor.newInstance(args));
                    ConfigManager.save();
                    return true;
                } catch (Exception ignored) {}
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private Object[] buildArgs(Class<?>[] types) {
        Object[] args = new Object[types.length];
        boolean nameSet = false;
        for (int i = 0; i < types.length; i++) {
            Class<?> t = types[i];
            if (t == String.class) {
                args[i] = nameSet ? "" : name;
                nameSet = true;
            } else if (t == UUID.class) {
                args[i] = uuid;
            } else if (t == Optional.class) {
                args[i] = Optional.empty();
            } else if (t.isEnum()) {
                args[i] = t.getEnumConstants()[0];
            } else if (t == boolean.class) {
                args[i] = false;
            } else {
                args[i] = null;
            }
        }
        return args;
    }
}