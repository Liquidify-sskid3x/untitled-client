package com.liquidify.untitled.module.modules.render;

import com.liquidify.untitled.module.Category;
import com.liquidify.untitled.module.Module;
import com.liquidify.untitled.module.settings.*;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class ESP extends Module {
    public final FloatSetting range        = addSetting(new FloatSetting("Range", 64f, 10f, 128f));
    public final BooleanSetting players    = addSetting(new BooleanSetting("Players", true));
    public final BooleanSetting mobs       = addSetting(new BooleanSetting("Mobs", true));
    public final BooleanSetting filled     = addSetting(new BooleanSetting("Filled", false));
    public final BooleanSetting showNames  = addSetting(new BooleanSetting("Names", true));
    public final BooleanSetting showHealth = addSetting(new BooleanSetting("Health Bar", true));
    public final StringSetting colorMode   = addSetting(new StringSetting("Color", "Default",
            "Default", "Rainbow", "Custom"));
    public final ColorSetting customColor  = addSetting(new ColorSetting("Custom Color", 0xFF00AAFF));
    public final KeybindSetting keybind    = addSetting(new KeybindSetting("Keybind"));

    public ESP() {
        super("ESP", Category.RENDER);
        HudRenderCallback.EVENT.register((graphics, delta) -> {
            if (!isEnabled()) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;

            int screenW = mc.getWindow().getGuiScaledWidth();
            int screenH = mc.getWindow().getGuiScaledHeight();
            float aspect = (float) screenW / screenH;
// Remove currentFov entirely, just use:
            float fov = mc.options.fov().get().floatValue();
            Matrix4f proj = new Matrix4f().perspective(
                    (float) Math.toRadians(fov), aspect, 0.05f, 1000f);
            // Build view matrix from camera rotation
            var cam = mc.gameRenderer.getMainCamera();
            Vec3 camPos = cam.position();
            Matrix4f view = new Matrix4f();
            cam.rotation().get(view);
            view.invert();

            long now = System.currentTimeMillis();

            for (LivingEntity entity : mc.level.getEntitiesOfClass(
                    LivingEntity.class,
                    mc.player.getBoundingBox().inflate(range.getValue()))) {
                if (entity == mc.player) continue;

                boolean isPlayer = entity instanceof Player;
                if (isPlayer && !players.getValue()) continue;
                if (!isPlayer && !mobs.getValue()) continue;

                var box = entity.getBoundingBox();
                float minX = (float)(box.minX - camPos.x);
                float maxX = (float)(box.maxX - camPos.x);
                float minY = (float)(box.minY - camPos.y);
                float maxY = (float)(box.maxY - camPos.y);
                float minZ = (float)(box.minZ - camPos.z);
                float maxZ = (float)(box.maxZ - camPos.z);

                float[][] corners = {
                        {minX,minY,minZ},{maxX,minY,minZ},
                        {minX,maxY,minZ},{maxX,maxY,minZ},
                        {minX,minY,maxZ},{maxX,minY,maxZ},
                        {minX,maxY,maxZ},{maxX,maxY,maxZ}
                };

                float sMinX = Float.MAX_VALUE, sMinY = Float.MAX_VALUE;
                float sMaxX = -Float.MAX_VALUE, sMaxY = -Float.MAX_VALUE;
                boolean anyVisible = false;

                for (float[] corner : corners) {
                    Vector4f pos = new Vector4f(corner[0], corner[1], corner[2], 1f);
                    pos.mul(view).mul(proj);
                    if (pos.w <= 0) continue;
                    anyVisible = true;
                    float sx = (pos.x / pos.w * 0.5f + 0.5f) * screenW;
                    float sy = (1f - (pos.y / pos.w * 0.5f + 0.5f)) * screenH;
                    if (sx < sMinX) sMinX = sx;
                    if (sx > sMaxX) sMaxX = sx;
                    if (sy < sMinY) sMinY = sy;
                    if (sy > sMaxY) sMaxY = sy;
                }

                if (!anyVisible) continue;
                sMinX = Math.max(0, sMinX);
                sMinY = Math.max(0, sMinY);
                sMaxX = Math.min(screenW, sMaxX);
                sMaxY = Math.min(screenH, sMaxY);
                if (sMaxX <= sMinX || sMaxY <= sMinY) continue;

                int color = getColor(isPlayer, now);
                int fillColor = (color & 0x00FFFFFF) | 0x30000000;

                if (filled.getValue())
                    graphics.fill((int)sMinX,(int)sMinY,(int)sMaxX,(int)sMaxY, fillColor);
                graphics.renderOutline((int)sMinX,(int)sMinY,
                        (int)(sMaxX-sMinX),(int)(sMaxY-sMinY), color);

                if (showNames.getValue()) {
                    String name = entity.getName().getString();
                    int nameW = mc.font.width(name);
                    graphics.drawString(mc.font, name,
                            (int)((sMinX+sMaxX)/2) - nameW/2,
                            (int)sMinY - 10, color, true);
                }

                if (showHealth.getValue()) {
                    float hpPct = entity.getHealth() / entity.getMaxHealth();
                    int barH = (int)(sMaxY - sMinY);
                    int barX = (int)sMinX - 4;
                    graphics.fill(barX,(int)sMinY,barX+2,(int)sMaxY, 0x99000000);
                    int greenY = (int)(sMinY + barH * (1f - hpPct));
                    int hpColor = hpPct > 0.5f ? 0xFF00FF00
                            : hpPct > 0.25f ? 0xFFFFAA00 : 0xFFFF0000;
                    graphics.fill(barX, greenY, barX+2, (int)sMaxY, hpColor);
                }
            }
        });
    }
    public static float currentFov = 70.0f;
    private int getColor(boolean isPlayer, long now) {
        return switch (colorMode.getValue()) {
            case "Rainbow" -> {
                float hue = (now % 2000) / 2000.0f;
                yield java.awt.Color.HSBtoRGB(hue, 1f, 1f) | 0xFF000000;
            }
            case "Custom" -> customColor.getValue() | 0xFF000000;
            default -> isPlayer ? 0xFFFF4444 : 0xFF4444FF;
        };
    }
}