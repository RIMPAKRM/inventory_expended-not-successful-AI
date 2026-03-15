package com.example.examplemod.inventory.client.screen;

import org.jetbrains.annotations.Nullable;

/**
 * Pure geometry helper for vanilla inventory grid (27 main + 9 hotbar).
 *
 * <p>M8+: координаты hotbar берутся из {@link InventoryLayoutConstants}.
 * Тесты передают координаты явно, поэтому API не меняется.</p>
 */
public final class VanillaInventoryGridModel {

    private static final int SLOT_SIZE    = InventoryLayoutConstants.SLOT_INNER;
    private static final int SLOT_SPACING = InventoryLayoutConstants.SLOT_STEP;

    private VanillaInventoryGridModel() {
    }

    /**
     * Returns player inventory index in [0..35], where 0..8 hotbar and 9..35 main.
     *
     * @param includeMainInventory if {@code false}, only hotbar (0..8) is searched
     */
    public static @Nullable Integer findPlayerInventorySlot(double mouseX,
                                                            double mouseY,
                                                            int mainStartX,
                                                            int mainStartY,
                                                            int hotbarStartX,
                                                            int hotbarStartY,
                                                            boolean includeMainInventory) {
        if (includeMainInventory) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    int x = mainStartX + col * SLOT_SPACING;
                    int y = mainStartY + row * SLOT_SPACING;
                    if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                        return 9 + row * 9 + col;
                    }
                }
            }
        }

        for (int col = 0; col < 9; col++) {
            int x = hotbarStartX + col * SLOT_SPACING;
            if (mouseX >= x && mouseX < x + SLOT_SIZE
                    && mouseY >= hotbarStartY && mouseY < hotbarStartY + SLOT_SIZE) {
                return col;
            }
        }

        return null;
    }

    /** Overload with {@code includeMainInventory = true}. */
    public static @Nullable Integer findPlayerInventorySlot(double mouseX,
                                                            double mouseY,
                                                            int mainStartX,
                                                            int mainStartY,
                                                            int hotbarStartX,
                                                            int hotbarStartY) {
        return findPlayerInventorySlot(mouseX, mouseY, mainStartX, mainStartY,
                hotbarStartX, hotbarStartY, true);
    }
}
