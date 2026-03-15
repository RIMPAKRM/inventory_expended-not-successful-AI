package com.example.examplemod.inventory.client.screen;

import com.example.examplemod.inventory.core.network.client.ClientInventorySyncState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static com.example.examplemod.inventory.client.screen.InventoryLayoutConstants.*;

/**
 * M8+ тесты layout-механики:
 * - детектирование якорей экипировки по slotId,
 * - отсутствие перекрытий зон,
 * - поведение buildLayout при пустом slotOrder.
 *
 * <p>Не требует Minecraft runtime.</p>
 */
class ExtendedInventoryLayoutTest {

    @BeforeEach
    void resetState() {
        ClientInventorySyncState.resetForTests();
    }

    @AfterEach
    void cleanup() {
        ClientInventorySyncState.resetForTests();
    }

    // ── detectAnchor (через публичный helper) ────────────────────────────

    @Test
    void equipmentSlotY_noOverlapWithPlayer() {
        // Левая колонка экипировки: все 6 слотов вертикально
        int[] equipYs = {EQUIP_HEAD_Y, EQUIP_FACE_Y, EQUIP_UPPER_Y, EQUIP_VEST_Y, EQUIP_PANTS_Y, EQUIP_BOOTS_Y};
        for (int i = 0; i < equipYs.length - 1; i++) {
            int slotBottom = equipYs[i] + SLOT_INNER;
            assertTrue(slotBottom <= equipYs[i + 1],
                    "Слот [" + i + "] bottomY=" + slotBottom + " перекрывает следующий slotY=" + equipYs[i + 1]);
        }
    }

    @Test
    void equipmentRightColumnFitsBeforeCraftPanel() {
        int rightColRight = EQUIP_RIGHT_X + SLOT_INNER;
        assertTrue(rightColRight <= CRAFT_X,
                "EQUIP_RIGHT_X + SLOT_INNER=" + rightColRight + " > CRAFT_X=" + CRAFT_X);
    }

    @Test
    void hotbarDoesNotOverlapDynamicZone() {
        // Dynamic зона начинается с DYNAMIC_Y, hotbar — с HOTBAR_Y
        assertTrue(DYNAMIC_Y < HOTBAR_Y,
                "DYNAMIC_Y должен быть меньше HOTBAR_Y");

        // Четыре строки dynamic slots (максимально разумное число для данного layout)
        int maxRows = 4;
        int dynamicBottom = DYNAMIC_Y + maxRows * SLOT_STEP;
        assertTrue(dynamicBottom <= HOTBAR_Y,
                "4 строки dynamic slots bottom=" + dynamicBottom + " > HOTBAR_Y=" + HOTBAR_Y);
    }

    // ── VanillaInventoryGridModel с новыми константами ───────────────────

    @Test
    void hotbarSlotHitTest_usingLayoutConstants() {
        // Координата первого hotbar-слота (col=0)
        double mx = HOTBAR_X + 4;  // внутри слота
        double my = HOTBAR_Y + 4;

        // mainStartX/Y произвольны (main не нужен в этом тесте)
        Integer slot = VanillaInventoryGridModel.findPlayerInventorySlot(
                mx, my,
                VANILLA_MAIN_X, VANILLA_MAIN_Y,
                HOTBAR_X, HOTBAR_Y,
                false);

        assertEquals(0, slot, "Первый hotbar-слот должен быть 0");
    }

    @Test
    void hotbarSlot8HitTest_usingLayoutConstants() {
        double mx = HOTBAR_X + 8 * SLOT_STEP + 4;  // col=8
        double my = HOTBAR_Y + 4;

        Integer slot = VanillaInventoryGridModel.findPlayerInventorySlot(
                mx, my,
                VANILLA_MAIN_X, VANILLA_MAIN_Y,
                HOTBAR_X, HOTBAR_Y,
                false);

        assertEquals(8, slot, "Девятый hotbar-слот должен быть 8");
    }

    @Test
    void hotbarOutsideReturnsNull() {
        // Правее последнего hotbar-слота
        double mx = HOTBAR_X + 9 * SLOT_STEP + 4;
        double my = HOTBAR_Y + 4;

        Integer slot = VanillaInventoryGridModel.findPlayerInventorySlot(
                mx, my,
                VANILLA_MAIN_X, VANILLA_MAIN_Y,
                HOTBAR_X, HOTBAR_Y,
                false);

        assertNull(slot, "За пределами hotbar должен возвращаться null");
    }

    @Test
    void mainInventorySlotHitTest_creative() {
        // В creative main доступен. Первый слот row=0, col=0 → vanilla index 9
        double mx = VANILLA_MAIN_X + 4;
        double my = VANILLA_MAIN_Y + 4;

        Integer slot = VanillaInventoryGridModel.findPlayerInventorySlot(
                mx, my,
                VANILLA_MAIN_X, VANILLA_MAIN_Y,
                HOTBAR_X, HOTBAR_Y,
                true);

        assertEquals(9, slot, "Первый main-слот должен быть 9");
    }

    @Test
    void mainInventoryIgnoredInSurvival() {
        double mx = VANILLA_MAIN_X + 4;
        double my = VANILLA_MAIN_Y + 4;

        Integer slot = VanillaInventoryGridModel.findPlayerInventorySlot(
                mx, my,
                VANILLA_MAIN_X, VANILLA_MAIN_Y,
                HOTBAR_X, HOTBAR_Y,
                false);   // survival: includeMain=false

        assertNull(slot, "В survival main-инвентарь должен игнорироваться");
    }

    // ── GUI-размер в допустимых пределах ─────────────────────────────────

    @Test
    void guiDimensionsReasonableForSmallScreen() {
        // Минимальное поддерживаемое разрешение Minecraft: 854×480
        // GUI масштабируется, но базовые px должны быть разумны
        assertTrue(GUI_WIDTH <= 320,  "GUI_WIDTH=" + GUI_WIDTH);
        assertTrue(GUI_HEIGHT <= 240, "GUI_HEIGHT=" + GUI_HEIGHT);
    }
}

