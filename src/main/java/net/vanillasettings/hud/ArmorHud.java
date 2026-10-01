package net.vanillasettings.hud;

import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.vanillasettings.mixin.HudAccessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Row of the player's worn armor pieces drawn flush against the left side of the
 * hotbar, using vanilla's own hotbar-slot background sprite - same layout style
 * as uku3lig's armor-hud mod (HOTBAR anchor, LEFT side, horizontal).
 * <p>
 * Like uku's mod (its default "Offhand Slot: ADHERE" behaviour) the row steps aside
 * when something else is drawn on that side of the hotbar - the offhand slot, or the
 * attack indicator for left-handed players - instead of covering it.
 */
public final class ArmorHud {

    private static final int SIZE = 22;
    private static final int STEP = 20;
    private static final int HOTBAR_OFFSET = 98;
    /** Width vanilla's offhand slot takes up next to the hotbar (uku: OFFHAND_OFFSET). */
    private static final int OFFHAND_OFFSET = 29;
    /** Width of the hotbar attack-indicator (uku: ATTACK_INDICATOR_OFFSET). */
    private static final int ATTACK_INDICATOR_OFFSET = 23;

    private ArmorHud() {
    }

    /**
     * How much further left of the hotbar the row has to sit right now. This is uku's
     * ADHERE logic for a left-side widget anchored to the hotbar:
     * <ul>
     *   <li>Right-handed (the default): the offhand slot appears on the left of the
     *       hotbar, but only while the offhand actually holds an item.</li>
     *   <li>Left-handed: the offhand slot moves to the right side, so the left side is
     *       instead where the hotbar attack indicator shows up while it's recharging.</li>
     * </ul>
     */
    private static int extraHotbarOffset(Minecraft mc, Player player) {
        if (player.getMainArm() == HumanoidArm.LEFT) {
            boolean indicatorShown = mc.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR
                    && player.getAttackStrengthScale(0) < 1;
            return indicatorShown ? ATTACK_INDICATOR_OFFSET : 0;
        }
        return player.getOffhandItem().isEmpty() ? 0 : OFFHAND_OFFSET;
    }

    public static void render(GuiGraphicsExtractor graphics, Font font, Minecraft mc,
                              int screenWidth, int screenHeight) {
        Player player = mc.player;
        if (player == null) {
            return;
        }

        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        List<ItemStack> armorItems = new ArrayList<>(4);
        for (EquipmentSlot slot : slots) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                armorItems.add(stack);
            }
        }

        if (armorItems.isEmpty()) {
            return;
        }

        int textureWidth = SIZE + ((armorItems.size() - 1) * STEP);
        int baseX = screenWidth / 2 - (HOTBAR_OFFSET + extraHotbarOffset(mc, player)) - textureWidth;
        int baseY = screenHeight - SIZE;
        int color = 0xFFFFFFFF;

        Identifier hotbarSprite;
        try {
            hotbarSprite = HudAccessor.vs$hotbarSprite();
        } catch (Throwable t) {
            // The Hud accessor mixin didn't apply for some reason (e.g. the field
            // was renamed in a future MC update) - fall back to plain item icons
            // with no background rather than crashing the HUD render.
            hotbarSprite = null;
        }

        if (hotbarSprite != null) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, hotbarSprite, 182, 22, 0, 0, baseX, baseY, textureWidth - 3, SIZE, color);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, hotbarSprite, 182, 22, 182 - 3, 0, baseX + textureWidth - 3, baseY, 3, SIZE, color);
        }

        for (int i = 0; i < armorItems.size(); i++) {
            ItemStack stack = armorItems.get(i);
            int x = baseX + STEP * i + 3;
            int y = baseY + 3;
            graphics.item(stack, x, y);
            graphics.itemDecorations(font, stack, x, y);
        }
    }
}
