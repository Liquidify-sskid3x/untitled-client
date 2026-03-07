package com.liquidify.untitled.config;

import com.google.gson.*;
import com.liquidify.untitled.UntitledClient;
import com.liquidify.untitled.accounts.CrackedAccount;
import com.liquidify.untitled.gui.AccountScreen;
import com.liquidify.untitled.module.Module;

import java.nio.file.*;

public class ConfigManager {
    private static final Path DIR      = Path.of("vvoxy");
    private static final Path MODULES  = DIR.resolve("active_modules.json");
    private static final Path ACCOUNTS = DIR.resolve("accounts_saved.json");
    private static final Gson GSON     = new GsonBuilder().setPrettyPrinting().create();

    public static void save() {
        try {
            Files.createDirectories(DIR);
            saveModules();
            saveAccounts();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void load() {
        try {
            loadModules();
            loadAccounts();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void saveModules() throws Exception {
        JsonArray arr = new JsonArray();
        for (Module m : UntitledClient.moduleManager.getModules()) {
            if (m.isEnabled()) arr.add(m.getName());
        }
        Files.writeString(MODULES, GSON.toJson(arr));
    }

    private static void loadModules() throws Exception {
        if (!Files.exists(MODULES)) return;
        JsonArray arr = JsonParser.parseString(Files.readString(MODULES)).getAsJsonArray();
        for (JsonElement el : arr) {
            String name = el.getAsString();
            UntitledClient.moduleManager.getModules().stream()
                    .filter(m -> m.getName().equals(name))
                    .findFirst()
                    .ifPresent(m -> {
                        if (!m.isEnabled()) m.toggle();
                    });
        }
    }

    private static void saveAccounts() throws Exception {
        JsonArray arr = new JsonArray();
        for (CrackedAccount acc : AccountScreen.getAccounts()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", acc.name);
            arr.add(obj);
        }
        Files.writeString(ACCOUNTS, GSON.toJson(arr));
    }

    private static void loadAccounts() throws Exception {
        if (!Files.exists(ACCOUNTS)) return;
        JsonArray arr = JsonParser.parseString(Files.readString(ACCOUNTS)).getAsJsonArray();
        for (JsonElement el : arr) {
            String name = el.getAsJsonObject().get("name").getAsString();
            // Avoid duplicates
            boolean exists = AccountScreen.getAccounts().stream()
                    .anyMatch(a -> a.name.equalsIgnoreCase(name));
            if (!exists) AccountScreen.getAccounts().add(new CrackedAccount(name));
        }
    }
}