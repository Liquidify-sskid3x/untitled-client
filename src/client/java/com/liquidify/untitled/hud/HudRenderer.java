package com.liquidify.untitled.hud;

import com.liquidify.untitled.UntitledClient;
import com.liquidify.untitled.module.Module;
import com.liquidify.untitled.module.modules.render.ESP;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

import java.util.List;

public class HudRenderer {
    public static float currentFov = 70.0f;
    public static void register() {
        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            int screenW = mc.getWindow().getGuiScaledWidth();
            int screenH = mc.getWindow().getGuiScaledHeight();

            // FPS Counter
            String fpsText = mc.getFps() + " FPS";
            graphics.fill(0, screenH - 12, mc.font.width(fpsText) + 4, screenH, 0x70000000);
            graphics.drawString(mc.font, fpsText, 2, screenH - 10, 0xFF0a84ff, false);

            // ArrayList
            List<Module> enabled = UntitledClient.moduleManager.getModules()
                    .stream()
                    .filter(Module::isEnabled)
                    .sorted((a, b) -> mc.font.width(b.getName()) - mc.font.width(a.getName()))
                    .toList();

            int yOff = 2;
            for (Module module : enabled) {
                String name = module.getName();
                int textW = mc.font.width(name);
                int xPos = screenW - textW - 4;
                graphics.fill(xPos - 1, yOff - 1, screenW - 2, yOff + 9, 0x70000000);
                graphics.fill(screenW - 2, yOff - 1, screenW, yOff + 9, 0xFF0a84ff);
                graphics.drawString(mc.font, name, xPos, yOff, 0xFFFFFFFF, false);
                yOff += 11;
            }

            // 2D ESP
            ESP esp = (ESP) UntitledClient.moduleManager.getModules()
                    .stream().filter(m -> m instanceof ESP).findFirst().orElse(null);
            if (esp != null && esp.isEnabled() && mc.level != null) {
                render2DESP(graphics, mc, esp, screenW, screenH,
                        tickDelta.getGameTimeDeltaPartialTick(true));
            }
        });
    }

    private static void render2DESP(GuiGraphics graphics, Minecraft mc, ESP esp,
                                    int screenW, int screenH, float partialTick) {
        var cam = mc.gameRenderer.getMainCamera();
        Vec3 camPos = cam.position();
        org.joml.Quaternionf camRot = cam.rotation();
        float fov = currentFov;
        float aspect = (float) screenW / screenH;
        long now = System.currentTimeMillis();

        for (LivingEntity entity : mc.level.getEntitiesOfClass(
                LivingEntity.class,
                mc.player.getBoundingBox().inflate(esp.range.getValue()))) {
            if (entity == mc.player) continue;

            boolean isPlayer = entity instanceof Player;
            if (isPlayer && !esp.players.getValue()) continue;
            if (!isPlayer && !esp.mobs.getValue()) continue;

            var box = entity.getBoundingBox();
            double[][] corners = {
                    {box.minX, box.minY, box.minZ}, {box.maxX, box.minY, box.minZ},
                    {box.minX, box.maxY, box.minZ}, {box.maxX, box.maxY, box.minZ},
                    {box.minX, box.minY, box.maxZ}, {box.maxX, box.minY, box.maxZ},
                    {box.minX, box.maxY, box.maxZ}, {box.maxX, box.maxY, box.maxZ}
            };

            float sMinX = Float.MAX_VALUE, sMinY = Float.MAX_VALUE;
            float sMaxX = -Float.MAX_VALUE, sMaxY = -Float.MAX_VALUE;
            boolean anyVisible = false;
            float[] out = new float[2];

            for (double[] corner : corners) {
                if (!worldToScreen(camPos, camRot, fov, screenW, screenH,
                        corner[0], corner[1], corner[2], out)) continue;
                anyVisible = true;
                if (out[0] < sMinX) sMinX = out[0];
                if (out[0] > sMaxX) sMaxX = out[0];
                if (out[1] < sMinY) sMinY = out[1];
                if (out[1] > sMaxY) sMaxY = out[1];
            }

            if (!anyVisible) continue;

            sMinX = Math.max(0, sMinX);
            sMinY = Math.max(0, sMinY);
            sMaxX = Math.min(screenW, sMaxX);
            sMaxY = Math.min(screenH, sMaxY);

            if (sMaxX <= sMinX || sMaxY <= sMinY) continue;

            int color = getESPColor(esp, isPlayer, now);
            int fillColor = (color & 0x00FFFFFF) | 0x30000000;

            if (esp.filled.getValue()) {
                graphics.fill((int)sMinX, (int)sMinY, (int)sMaxX, (int)sMaxY, fillColor);
            }
            graphics.renderOutline((int)sMinX, (int)sMinY,
                    (int)(sMaxX - sMinX), (int)(sMaxY - sMinY), color);

            if (esp.showNames.getValue()) {
                String name = entity.getName().getString();
                int nameW = mc.font.width(name);
                int nameX = (int)((sMinX + sMaxX) / 2) - nameW / 2;
                graphics.drawString(mc.font, name, nameX, (int)sMinY - 10, color, true);
            }

            if (esp.showHealth.getValue()) {
                float hpPct = entity.getHealth() / entity.getMaxHealth();
                int barH = (int)(sMaxY - sMinY);
                int barX = (int)sMinX - 4;
                graphics.fill(barX, (int)sMinY, barX + 2, (int)sMaxY, 0x99000000);
                int greenY = (int)(sMinY + barH * (1.0f - hpPct));
                int hpColor = hpPct > 0.5f ? 0xFF00FF00 : hpPct > 0.25f ? 0xFFFFAA00 : 0xFFFF0000;
                graphics.fill(barX, greenY, barX + 2, (int)sMaxY, hpColor);
            }
        }
    }

    private static boolean worldToScreen(Vec3 camPos, org.joml.Quaternionf camRot,
                                         float fov, int screenW, int screenH,
                                         double wx, double wy, double wz,
                                         float[] out) {
        double dx = wx - camPos.x;
        double dy = wy - camPos.y;
        double dz = wz - camPos.z;

        org.joml.Quaternionf invRot = new org.joml.Quaternionf(camRot).conjugate();
        org.joml.Vector3f v = new org.joml.Vector3f((float)dx, (float)dy, (float)dz);
        invRot.transform(v);

        if (v.z >= 0) return false;

        float aspect = (float) screenW / screenH;
        float fovRad = (float) Math.toRadians(fov);
        float tanHalfFov = (float) Math.tan(fovRad / 2.0);

        float sx = ((-v.x / (-v.z)) / (tanHalfFov * aspect)) * 0.5f + 0.5f;
        float sy = ((v.y / (-v.z)) / tanHalfFov) * 0.5f + 0.5f;

        out[0] = sx * screenW;
        out[1] = (1.0f - sy) * screenH;
        return true;
    }

    void main() {
    }

    private static int getESPColor(ESP esp, boolean isPlayer, long now) {
        return switch (esp.colorMode.getValue()) {
            case "Rainbow" -> {
                float hue = (now % 2000) / 2000.0f;
                yield java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f) | 0xFF000000;
            }
            case "Custom" -> esp.customColor.getValue() | 0xFF000000;
            default -> isPlayer ? 0xFFFF4444 : 0xFF4444FF;
        };
    }
}