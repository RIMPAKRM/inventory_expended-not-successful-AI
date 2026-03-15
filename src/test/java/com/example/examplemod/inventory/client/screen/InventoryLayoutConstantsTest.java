package com.example.examplemod.inventory.client.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M8+ Layout constants: проверяем, что зоны GUI не перекрываются.
 */
class InventoryLayoutConstantsTest {

    // ── Hotbar не выходит за правую границу GUI ───────────────────────────

    @Test
    void hotbarFitsInsideGui() {
        int hotbarRight = InventoryLayoutConstants.HOTBAR_X
                + 9 * InventoryLayoutConstants.SLOT_STEP;
        // Должен остаться левее начала крафт-панели
        assertTrue(hotbarRight <= InventoryLayoutConstants.CRAFT_X,
                "Hotbar right=" + hotbarRight + " должен быть <= CRAFT_X=" + InventoryLayoutConstants.CRAFT_X);
    }

    @Test
    void hotbarBottomFitsInsideGui() {
        int bottom = InventoryLayoutConstants.HOTBAR_Y + InventoryLayoutConstants.SLOT_STEP;
        assertTrue(bottom <= InventoryLayoutConstants.GUI_HEIGHT,
                "Hotbar bottom=" + bottom + " должен быть <= GUI_HEIGHT=" + InventoryLayoutConstants.GUI_HEIGHT);
    }

    // ── Equipment-колонки не перекрывают зону силуэта ─────────────────────

    @Test
    void leftEquipColumnDoesNotOverlapPlayer() {
        int leftColRight = InventoryLayoutConstants.EQUIP_LEFT_X + InventoryLayoutConstants.SLOT_INNER;
        assertTrue(leftColRight <= InventoryLayoutConstants.PLAYER_X,
                "Левая экип-колонка rightEdge=" + leftColRight
                        + " должна быть <= PLAYER_X=" + InventoryLayoutConstants.PLAYER_X);
    }

    @Test
    void playerDoesNotOverlapRightEquipColumn() {
        int playerRight = InventoryLayoutConstants.PLAYER_X + InventoryLayoutConstants.PLAYER_W;
        assertTrue(playerRight <= InventoryLayoutConstants.EQUIP_RIGHT_X,
                "Силуэт rightEdge=" + playerRight
                        + " должен быть <= EQUIP_RIGHT_X=" + InventoryLayoutConstants.EQUIP_RIGHT_X);
    }

    @Test
    void rightEquipColumnDoesNotOverlapCraftPanel() {
        int rightColRight = InventoryLayoutConstants.EQUIP_RIGHT_X + InventoryLayoutConstants.SLOT_INNER;
        assertTrue(rightColRight <= InventoryLayoutConstants.CRAFT_X,
                "Правая экип-колонка rightEdge=" + rightColRight
                        + " должна быть <= CRAFT_X=" + InventoryLayoutConstants.CRAFT_X);
    }

    // ── Dynamic slots не перекрывают hotbar ──────────────────────────────

    @Test
    void dynamicZoneDoesNotOverlapHotbar() {
        // Максимум 4 строки dynamic slots (72 px) — проверяем, что они заканчиваются до hotbar
        int maxDynamicRows = 4;
        int dynamicBottom = InventoryLayoutConstants.DYNAMIC_Y
                + maxDynamicRows * InventoryLayoutConstants.SLOT_STEP;
        assertTrue(dynamicBottom <= InventoryLayoutConstants.HOTBAR_Y,
                "Dynamic zone bottom (4 rows)=" + dynamicBottom
                        + " должен быть <= HOTBAR_Y=" + InventoryLayoutConstants.HOTBAR_Y);
    }

    // ── Craft panel не выходит за правый край GUI ─────────────────────────

    @Test
    void craftPanelFitsInsideGui() {
        int craftRight = InventoryLayoutConstants.CRAFT_X + InventoryLayoutConstants.CRAFT_W;
        assertTrue(craftRight <= InventoryLayoutConstants.GUI_WIDTH,
                "Craft panel rightEdge=" + craftRight
                        + " должен быть <= GUI_WIDTH=" + InventoryLayoutConstants.GUI_WIDTH);
    }

    @Test
    void craftPanelFitsInsideGuiHeight() {
        int craftBottom = InventoryLayoutConstants.CRAFT_Y + InventoryLayoutConstants.CRAFT_H;
        assertTrue(craftBottom <= InventoryLayoutConstants.GUI_HEIGHT,
                "Craft panel bottom=" + craftBottom
                        + " должен быть <= GUI_HEIGHT=" + InventoryLayoutConstants.GUI_HEIGHT);
    }

    // ── Craft button внутри panel ─────────────────────────────────────────

    @Test
    void craftButtonInsidePanel() {
        int btnBottom = InventoryLayoutConstants.CRAFT_BTN_REL_Y + InventoryLayoutConstants.CRAFT_BTN_H;
        assertTrue(btnBottom <= InventoryLayoutConstants.CRAFT_H,
                "Craft button bottom (relative)=" + btnBottom
                        + " должен быть <= CRAFT_H=" + InventoryLayoutConstants.CRAFT_H);
    }

    // ── GUI умещается в экран 854×480 ─────────────────────────────────────

    @Test
    void guiWidthReasonable() {
        assertTrue(InventoryLayoutConstants.GUI_WIDTH <= 320,
                "GUI_WIDTH=" + InventoryLayoutConstants.GUI_WIDTH + " слишком велик для 854px");
    }

    @Test
    void guiHeightReasonable() {
        assertTrue(InventoryLayoutConstants.GUI_HEIGHT <= 240,
                "GUI_HEIGHT=" + InventoryLayoutConstants.GUI_HEIGHT + " слишком велик для 480px");
    }
}

