package com.example.examplemod.inventory.client.screen;

import com.example.examplemod.inventory.client.InventoryScreenRouter;
import com.example.examplemod.inventory.core.craft.CraftRecipeId;
import com.example.examplemod.inventory.core.craft.CraftRecipeService;
import com.example.examplemod.inventory.core.network.InventoryNetworkHandler;
import com.example.examplemod.inventory.core.network.client.ClientCraftPanelState;
import com.example.examplemod.inventory.core.network.client.ClientInventorySyncState;
import com.example.examplemod.inventory.core.network.packet.C2SCraftCreateIntentPacket;
import com.example.examplemod.inventory.menu.ExtendedInventoryMenu;
import com.example.examplemod.inventory.menu.ModSlotLayoutResolver;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.example.examplemod.inventory.client.screen.InventoryLayoutConstants.*;

/**
 * M8+ Extended Inventory Screen.
 *
 * <p>All clickable inventory slots (including equipment/dynamic) are Forge slots from
 * {@link ExtendedInventoryMenu}. This screen paints backgrounds, craft panel and helper tooltips.</p>
 */
public final class ExtendedInventoryScreen extends AbstractContainerScreen<ExtendedInventoryMenu> {

    private static final Map<ModSlotLayoutResolver.EquipmentAnchor, int[]> EQUIP_OFFSETS;

    static {
        EQUIP_OFFSETS = new LinkedHashMap<>();
        EQUIP_OFFSETS.put(ModSlotLayoutResolver.EquipmentAnchor.HEAD, new int[]{EQUIP_LEFT_X, EQUIP_HEAD_Y});
        EQUIP_OFFSETS.put(ModSlotLayoutResolver.EquipmentAnchor.FACE, new int[]{EQUIP_LEFT_X, EQUIP_FACE_Y});
        EQUIP_OFFSETS.put(ModSlotLayoutResolver.EquipmentAnchor.UPPER, new int[]{EQUIP_LEFT_X, EQUIP_UPPER_Y});
        EQUIP_OFFSETS.put(ModSlotLayoutResolver.EquipmentAnchor.VEST, new int[]{EQUIP_LEFT_X, EQUIP_VEST_Y});
        EQUIP_OFFSETS.put(ModSlotLayoutResolver.EquipmentAnchor.PANTS, new int[]{EQUIP_LEFT_X, EQUIP_PANTS_Y});
        EQUIP_OFFSETS.put(ModSlotLayoutResolver.EquipmentAnchor.BOOTS, new int[]{EQUIP_LEFT_X, EQUIP_BOOTS_Y});
        EQUIP_OFFSETS.put(ModSlotLayoutResolver.EquipmentAnchor.BACKPACK, new int[]{EQUIP_RIGHT_X, EQUIP_BACK_Y});
        EQUIP_OFFSETS.put(ModSlotLayoutResolver.EquipmentAnchor.GLOVES, new int[]{EQUIP_RIGHT_X, EQUIP_GLOVES_Y});
    }

    public ExtendedInventoryScreen(@NotNull ExtendedInventoryMenu menu,
                                   @NotNull Inventory playerInventory,
                                   @NotNull Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.titleLabelX = 8;
        this.titleLabelY = -12;
        this.inventoryLabelY = GUI_HEIGHT + 5;
    }

    @Override
    protected void init() {
        super.init();
        InventoryScreenRouter.onReplacementOpened();
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int lx = this.leftPos;
        int ty = this.topPos;

        ModSlotLayoutResolver.Layout layout = buildLayout();
        boolean canCreate = canCreateSelectedRecipe();

        g.fill(lx, ty, lx + GUI_WIDTH, ty + GUI_HEIGHT, COLOR_BG_OUTER);
        g.fill(lx + 2, ty + 2, lx + GUI_WIDTH - 2, ty + GUI_HEIGHT - 2, COLOR_BG_INNER);
        g.fill(lx + CRAFT_X - 2, ty + 2, lx + CRAFT_X - 1, ty + GUI_HEIGHT - 2, 0xFF333A4A);

        DisplayMode mode = resolveDisplayMode();
        renderEquipmentAnchors(g, lx, ty, layout);
        renderDynamicSlots(g, lx, ty, layout);
        renderPlayerSilhouette(g, lx, ty, mouseX, mouseY);
        renderVanillaZones(g, lx, ty, mode);
        renderCraftPanel(g, lx + CRAFT_X, ty + CRAFT_Y, canCreate);
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
        renderModSlotTooltips(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        ClientInventorySyncState.Snapshot snapshot = ClientInventorySyncState.snapshot();

        g.drawString(this.font, this.title, 8, 2, 0xCCCCCC, false);

        if (resolveDisplayMode() == DisplayMode.MODDED_HOTBAR_ONLY) {
            g.drawString(this.font,
                    Component.translatable("screen.inventory.extended_inventory.survival_hint"),
                    DYNAMIC_X, DYNAMIC_Y - 10, 0x9FB3C8, false);
        }

        String msgKey = ClientCraftPanelState.lastMessageKey();
        if (!msgKey.isBlank()) {
            int color = ClientCraftPanelState.lastSuccess() ? 0x86EFAC : 0xFCA5A5;
            g.drawString(this.font, Component.translatable(msgKey),
                    CRAFT_X + 2, CRAFT_Y + CRAFT_H + 2, color, false);
        }

        if (!snapshot.hasUsableSnapshot()) {
            g.drawString(this.font,
                    Component.translatable("screen.inventory.extended_inventory.syncing"),
                    EQUIP_LEFT_X, DYNAMIC_Y + 12, 0xFFCC66, false);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && isCraftButtonHit(mx, my)) {
            ClientInventorySyncState.Snapshot snapshot = ClientInventorySyncState.snapshot();
            if (canCreateSelectedRecipe() && snapshot.hasUsableSnapshot()) {
                InventoryNetworkHandler.sendToServer(C2SCraftCreateIntentPacket.create(
                        CraftRecipeId.TEST_LEATHER_RIG,
                        snapshot.revision(),
                        snapshot.layoutVersion()));
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private void renderPlayerSilhouette(@NotNull GuiGraphics g, int lx, int ty, int mouseX, int mouseY) {
        int px = lx + PLAYER_X;
        int py = ty + PLAYER_Y;

        g.fill(px, py, px + PLAYER_W, py + PLAYER_H, 0xFF181E2A);
        g.fill(px + 1, py + 1, px + PLAYER_W - 1, py + PLAYER_H - 1, 0xFF0D1219);

        if (this.minecraft != null && this.minecraft.player != null) {
            int centerX = px + PLAYER_W / 2;
            int baseY = py + PLAYER_H - 3;
            float lookX = (float) (centerX - mouseX);
            float lookY = (float) (baseY - 55 - mouseY);
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    g, centerX, baseY, 40, lookX, lookY, this.minecraft.player);
        }
    }

    private void renderEquipmentAnchors(@NotNull GuiGraphics g,
                                        int lx,
                                        int ty,
                                        @NotNull ModSlotLayoutResolver.Layout layout) {
        for (Map.Entry<ModSlotLayoutResolver.EquipmentAnchor, int[]> entry : EQUIP_OFFSETS.entrySet()) {
            ModSlotLayoutResolver.EquipmentAnchor anchor = entry.getKey();
            int x = lx + entry.getValue()[0];
            int y = ty + entry.getValue()[1];
            String slotId = layout.equipmentSlotId(anchor);

            if (slotId != null) {
                drawSlotBackground(g, x, y, COLOR_SLOT_BORDER, COLOR_SLOT_BG);
            } else {
                drawSlotBackground(g, x, y, COLOR_EQUIP_EMPTY, 0xFF0A0E16);
                g.drawString(this.font, anchorHint(anchor), x + 4, y + 4, 0xFF404A5C, false);
            }
        }
    }

    private void renderDynamicSlots(@NotNull GuiGraphics g,
                                    int lx,
                                    int ty,
                                    @NotNull ModSlotLayoutResolver.Layout layout) {
        for (Map.Entry<String, ModSlotLayoutResolver.SlotPosition> entry : layout.positions().entrySet()) {
            String slotId = entry.getKey();
            if (layout.anchors().containsValue(slotId)) {
                continue;
            }
            int x = lx + entry.getValue().x();
            int y = ty + entry.getValue().y();
            drawSlotBackground(g, x, y, COLOR_SLOT_BORDER, COLOR_SLOT_BG);
        }
    }

    private void renderVanillaZones(@NotNull GuiGraphics g, int lx, int ty, @NotNull DisplayMode mode) {
        if (mode == DisplayMode.CREATIVE_FULL_VANILLA) {
            int mx = lx + VANILLA_MAIN_X - 1;
            int myy = ty + VANILLA_MAIN_Y - 1;
            g.fill(mx, myy, mx + 9 * SLOT_STEP + 1, myy + 3 * SLOT_STEP + 1, 0xFF2B3144);
        }
        int hx = lx + HOTBAR_X - 1;
        int hy = ty + HOTBAR_Y - 1;
        g.fill(hx, hy, hx + 9 * SLOT_STEP + 1, hy + SLOT_STEP + 1, 0xFF252C3D);
        g.fill(hx, hy - 2, hx + 9 * SLOT_STEP + 1, hy - 1, 0xFF333A4A);
    }

    /** Рисует крафт-панель. */
    private void renderCraftPanel(@NotNull GuiGraphics g, int x, int y, boolean canCreate) {
        // Фон
        g.fill(x, y, x + CRAFT_W, y + CRAFT_H, 0xFF252C3D);
        g.fill(x + 1, y + 1, x + CRAFT_W - 1, y + CRAFT_H - 1, 0xFF141B28);

        // Заголовок
        g.drawString(this.font,
                Component.translatable("screen.inventory.extended_inventory.craft_title"),
                x + 4, y + 4, 0xBBCCDD, false);

        // Категории (слева в панели)
        int catX = x + 2;
        int catY = y + 14;
        for (int i = 0; i < 5; i++) {
            int cy = catY + i * SLOT_STEP;
            boolean active = i == 0;
            g.fill(catX, cy, catX + 22, cy + SLOT_INNER, active ? 0xFF3D5A80 : 0xFF1E2A3A);
            g.fill(catX + 1, cy + 1, catX + 21, cy + SLOT_INNER - 1, active ? 0xFF2A3F5A : 0xFF131C28);
        }

        // Сетка рецептов (3 колонки × 4 строки)
        int gridX = x + 26;
        int gridY = y + 14;
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 2; col++) {
                int cx = gridX + col * SLOT_STEP;
                int cy = gridY + row * SLOT_STEP;
                boolean available = row == 0 && col == 0 && canCreate;
                g.fill(cx, cy, cx + SLOT_INNER, cy + SLOT_INNER, available ? 0xFF2D5A3D : 0xFF3A4555);
                g.fill(cx + 1, cy + 1, cx + SLOT_INNER - 1, cy + SLOT_INNER - 1,
                        available ? 0xFF1A3D28 : 0xFF0F1620);
            }
        }

        // Результат
        int resX = x + 26;
        int resY = y + 14 + 4 * SLOT_STEP + 4;
        g.fill(resX, resY, resX + SLOT_INNER, resY + SLOT_INNER, 0xFF2B3144);
        g.fill(resX + 1, resY + 1, resX + SLOT_INNER - 1, resY + SLOT_INNER - 1, 0xFF101720);

        // Кнопка «Создать»
        int btnX = x + 2;
        int btnY = y + CRAFT_BTN_REL_Y;
        g.fill(btnX, btnY, btnX + CRAFT_BTN_W, btnY + CRAFT_BTN_H, canCreate ? 0xFF1F5C38 : 0xFF2A3344);
        g.fill(btnX + 1, btnY + 1, btnX + CRAFT_BTN_W - 1, btnY + CRAFT_BTN_H - 1,
                canCreate ? 0xFF0D3322 : 0xFF111820);
        g.drawCenteredString(this.font,
                Component.translatable("screen.inventory.extended_inventory.create_button"),
                btnX + CRAFT_BTN_W / 2, btnY + 4,
                canCreate ? 0xE6FCEF : 0x8899AA);
    }

    /** Тултипы для mod-слотов (equipment + dynamic). */
    private void renderModSlotTooltips(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        ModSlotLayoutResolver.Layout layout = buildLayout();
        for (Map.Entry<ModSlotLayoutResolver.EquipmentAnchor, int[]> entry : EQUIP_OFFSETS.entrySet()) {
            int x = this.leftPos + entry.getValue()[0];
            int y = this.topPos + entry.getValue()[1];
            if (!isOverSlot(mouseX, mouseY, x, y)) {
                continue;
            }
            if (layout.equipmentSlotId(entry.getKey()) == null) {
                g.renderTooltip(this.font, Component.literal(anchorDisplayName(entry.getKey())), mouseX, mouseY);
                return;
            }
        }
    }

    private @NotNull ModSlotLayoutResolver.Layout buildLayout() {
        return ModSlotLayoutResolver.resolve(this.menu.orderedModSlotIds());
    }

    private static boolean isOverSlot(double mx, double my, int x, int y) {
        return mx >= x && mx < x + SLOT_INNER && my >= y && my < y + SLOT_INNER;
    }

    private static void drawSlotBackground(@NotNull GuiGraphics g, int x, int y, int borderColor, int bgColor) {
        g.fill(x, y, x + SLOT_INNER, y + SLOT_INNER, borderColor);
        g.fill(x + 1, y + 1, x + SLOT_INNER - 1, y + SLOT_INNER - 1, bgColor);
    }

    private boolean canCreateSelectedRecipe() {
        return CraftRecipeService.canCraftFromClientSlots(CraftRecipeId.TEST_LEATHER_RIG, this.menu.modSlotSnapshot());
    }

    private boolean isCraftButtonHit(double mx, double my) {
        int x = this.leftPos + CRAFT_X + 2;
        int y = this.topPos + CRAFT_Y + CRAFT_BTN_REL_Y;
        return mx >= x && mx < x + CRAFT_BTN_W && my >= y && my < y + CRAFT_BTN_H;
    }

    private @NotNull DisplayMode resolveDisplayMode() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return DisplayMode.MODDED_HOTBAR_ONLY;
        }
        return this.minecraft.player.isCreative() ? DisplayMode.CREATIVE_FULL_VANILLA : DisplayMode.MODDED_HOTBAR_ONLY;
    }

    private static String anchorDisplayName(@NotNull ModSlotLayoutResolver.EquipmentAnchor anchor) {
        return switch (anchor) {
            case HEAD -> "Head";
            case FACE -> "Face";
            case UPPER -> "Upper";
            case VEST -> "Vest";
            case BACKPACK -> "Backpack";
            case GLOVES -> "Gloves";
            case PANTS -> "Pants";
            case BOOTS -> "Boots";
        };
    }

    private static String anchorHint(@NotNull ModSlotLayoutResolver.EquipmentAnchor anchor) {
        return switch (anchor) {
            case HEAD -> "H";
            case FACE -> "F";
            case UPPER -> "U";
            case VEST -> "V";
            case BACKPACK -> "B";
            case GLOVES -> "G";
            case PANTS -> "P";
            case BOOTS -> "Bo";
        };
    }

    private enum DisplayMode { CREATIVE_FULL_VANILLA, MODDED_HOTBAR_ONLY }
}
