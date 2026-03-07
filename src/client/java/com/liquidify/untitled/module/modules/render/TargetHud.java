package com.liquidify.untitled.module.modules.render;

import com.liquidify.untitled.module.Category;
import com.liquidify.untitled.module.Module;
import com.liquidify.untitled.module.settings.StringSetting;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class TargetHud extends Module {
    public final StringSetting position = addSetting(
            new StringSetting("Position", "Right", "Right", "Left"));

    public static LivingEntity target = null;
    public static long lastTargetTime = 0;
    private static final long TARGET_TIMEOUT = 5000;

    // Panel dimensions — always fixed
    private static final int W = 130;
    private static final int H = 56;

    private final java.util.Map<Integer, Float> lastHealth = new java.util.HashMap<>();
    private static final int BG_FILL    = 0xCC05050F;
    private static final int BG_OUTLINE = 0xFF0a1a3a;
    private static final int BG_ACCENT  = 0xFF0a84ff;
    private static final int BG_INNER   = 0xAA081525;

    public TargetHud() {
        super("Target HUD", Category.RENDER);
        HudRenderCallback.EVENT.register(this::onHudRender);
    }

    private void onHudRender(GuiGraphics g, DeltaTracker delta) {
        if (!isEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Clear stale target after timeout
        if (target != null && System.currentTimeMillis() - lastTargetTime > TARGET_TIMEOUT) {
            target = null;
        }

        LivingEntity t = target;

        // If no target set by mixin, find nearest living entity
        if (t == null || !t.isAlive()) {
            t = null;
            double closest = Double.MAX_VALUE;
            for (LivingEntity e : mc.level.getEntitiesOfClass(
                    LivingEntity.class,
                    mc.player.getBoundingBox().inflate(6))) {
                if (e == mc.player || !e.isAlive()) continue;
                double d = e.distanceTo(mc.player);
                if (d < closest) { closest = d; t = e; }
            }
            if (t != null) lastTargetTime = System.currentTimeMillis();
        }

        if (t == null) return;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        // Position between center and right, or center and left
        int panelX = "Left".equals(position.getValue())
                ? screenW / 4 - W / 2
                : screenW * 3 / 4 - W / 2;
        int panelY = screenH / 2 - H / 2;

        // whats that? a random code!
        for (LivingEntity e : mc.level.getEntitiesOfClass(
                LivingEntity.class,
                mc.player.getBoundingBox().inflate(16))) {
            if (e == mc.player || !e.isAlive()) continue;
            float prev = lastHealth.getOrDefault(e.getId(), e.getHealth());
            float curr = e.getHealth();
            // If health dropped and we're close, set as target
            if (curr < prev && e.distanceTo(mc.player) < 16) {
                target = e;
                lastTargetTime = System.currentTimeMillis();
            }
            lastHealth.put(e.getId(), curr);
        }
        // Shadow
        g.fill(panelX - 1, panelY - 1,
                panelX + W + 1, panelY + H + 1, 0x55000010);
        // Main bg
        g.fill(panelX, panelY, panelX + W, panelY + H, BG_FILL);
        // Outer dark outline (depth)
        g.renderOutline(panelX - 1, panelY - 1, W + 2, H + 2, BG_OUTLINE);
        // Inner outline
        g.renderOutline(panelX, panelY, W, H, 0xFF061228);
        // Top accent bar
        g.fill(panelX, panelY, panelX + W, panelY + 1, BG_ACCENT);

     //placeholder head
        int headSize = 28;
        int headX = panelX + 5;
        int headY = panelY + (H - 10 - headSize) / 2; // vertically centered above health bar

        // Generate unique color from name hash
        int nameHash  = t.getName().getString().hashCode();
        int r         = 80  + Math.abs(nameHash >> 16 & 0xFF) % 120;
        int gC        = 80  + Math.abs(nameHash >> 8  & 0xFF) % 120;
        int b         = 120 + Math.abs(nameHash        & 0xFF) % 135;
        int headColor = 0xFF000000 | (r << 16) | (gC << 8) | b;

        // Head bg
        g.fill(headX, headY, headX + headSize, headY + headSize, 0xFF0a1020);
        // Face
        g.fill(headX + 2, headY + 2,
                headX + headSize - 2, headY + headSize - 2, headColor);
        // Eyes
        g.fill(headX + 6,  headY + 9,  headX + 10, headY + 13, 0xFF000000);
        g.fill(headX + 14, headY + 9,  headX + 18, headY + 13, 0xFF000000);
        // Mouth
        g.fill(headX + 7,  headY + 17, headX + 17, headY + 19, 0xFF000000);
        // Initial letter
        String initial = t.getName().getString().substring(0, 1).toUpperCase();
        // Head outline
        g.renderOutline(headX - 1, headY - 1, headSize + 2, headSize + 2, BG_OUTLINE);
        g.renderOutline(headX, headY, headSize, headSize, 0xFF061228);

        int textX  = headX + headSize + 6;
        int rightW = W - headSize - 16; // max width for text/items on right
        int textY  = headY + 1;

        // Name
        String name = t.getName().getString();
        while (mc.font.width(name) > rightW && name.length() > 1)
            name = name.substring(0, name.length() - 1);
        g.drawString(mc.font, name, textX, textY, 0xFF5BB8FF, true);

        // Main hand item
        ItemStack mainHand = t.getItemBySlot(EquipmentSlot.MAINHAND);
        int itemRowY = textY + mc.font.lineHeight + 3;
        if (!mainHand.isEmpty()) {
            // Item icon (16x16)
            g.renderFakeItem(mainHand, textX, itemRowY - 2);
            // Item name next to icon
            String itemName = mainHand.getHoverName().getString();
            while (mc.font.width(itemName) > rightW - 18 && itemName.length() > 1)
                itemName = itemName.substring(0, itemName.length() - 1);
            g.drawString(mc.font, itemName,
                    textX + 18, itemRowY + 2, 0xFF8899BB, true);
        } else {
            g.drawString(mc.font, "Empty hand",
                    textX, itemRowY + 2, 0xFF334455, true);
        }

        float hp    = t.getHealth();
        float maxHp = t.getMaxHealth();
        float pct   = Math.max(0f, Math.min(1f, hp / maxHp));

        int hpColor = pct > 0.6f ? 0xFF55FF55
                : pct > 0.3f ? 0xFFFFAA00 : 0xFFFF5555;

        int barH  = 5;
        int barY  = panelY + H - barH - 3;
        int barX  = panelX + 4;
        int barW  = W - 8; // always fixed width

        // HP label centered above bar
        String hpText = (int) hp + " / " + (int) maxHp;
        int hpW = mc.font.width(hpText);
        g.drawString(mc.font, hpText,
                barX + barW / 2 - hpW / 2,
                barY - mc.font.lineHeight - 1,
                hpColor, true);

        // Bar track
        g.fill(barX, barY, barX + barW, barY + barH, 0xFF0a1a2a);
        // Bar fill
        g.fill(barX, barY, barX + (int)(barW * pct), barY + barH, hpColor);
        // Bar outline
        g.renderOutline(barX, barY, barW, barH, 0xFF061020);
        // Bar accent left cap
        g.fill(barX, barY, barX + 1, barY + barH, BG_ACCENT);
    }
}