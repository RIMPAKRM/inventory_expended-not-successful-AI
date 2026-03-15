package com.example.examplemod.inventory.menu;

import com.example.examplemod.inventory.client.screen.InventoryLayoutConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Deterministic layout planner for extended (mod) slots.
 *
 * <p>Both menu and screen use this resolver so Forge slots and painted backgrounds stay aligned.</p>
 */
public final class ModSlotLayoutResolver {

    private ModSlotLayoutResolver() {
    }

    public static @NotNull Layout resolve(@NotNull List<String> orderedSlotIds) {
        Map<EquipmentAnchor, String> anchors = new EnumMap<>(EquipmentAnchor.class);
        List<String> dynamicIds = new ArrayList<>();

        for (String slotId : orderedSlotIds) {
            EquipmentAnchor anchor = detectAnchor(slotId);
            if (anchor != null && !anchors.containsKey(anchor)) {
                anchors.put(anchor, slotId);
            } else {
                dynamicIds.add(slotId);
            }
        }

        Map<String, SlotPosition> positions = new LinkedHashMap<>();

        for (EquipmentAnchor anchor : EquipmentAnchor.values()) {
            String slotId = anchors.get(anchor);
            if (slotId == null) {
                continue;
            }
            int[] point = anchorPosition(anchor);
            positions.put(slotId, new SlotPosition(point[0], point[1]));
        }

        for (int i = 0; i < dynamicIds.size(); i++) {
            int col = i % InventoryLayoutConstants.DYNAMIC_COLS;
            int row = i / InventoryLayoutConstants.DYNAMIC_COLS;
            positions.put(dynamicIds.get(i), new SlotPosition(
                    InventoryLayoutConstants.DYNAMIC_X + col * InventoryLayoutConstants.SLOT_STEP,
                    InventoryLayoutConstants.DYNAMIC_Y + row * InventoryLayoutConstants.SLOT_STEP));
        }

        return new Layout(positions, anchors);
    }

    @Nullable
    static EquipmentAnchor detectAnchor(@NotNull String slotId) {
        String s = slotId.toLowerCase(Locale.ROOT);
        if (s.contains("helmet") || s.contains("head")) return EquipmentAnchor.HEAD;
        if (s.contains("gas") || s.contains("mask") || s.contains("face")) return EquipmentAnchor.FACE;
        if (s.contains("backpack") || s.contains("back")) return EquipmentAnchor.BACKPACK;
        if (s.contains("glove")) return EquipmentAnchor.GLOVES;
        if (s.contains("vest") || s.contains("rig")) return EquipmentAnchor.VEST;
        if (s.contains("pants") || s.contains("leg")) return EquipmentAnchor.PANTS;
        if (s.contains("boot") || s.contains("feet") || s.contains("shoe")) return EquipmentAnchor.BOOTS;
        if (s.contains("chest") || s.contains("armor") || s.contains("jacket") || s.contains("upper")) {
            return EquipmentAnchor.UPPER;
        }
        return null;
    }

    private static int[] anchorPosition(@NotNull EquipmentAnchor anchor) {
        return switch (anchor) {
            case HEAD -> new int[]{InventoryLayoutConstants.EQUIP_LEFT_X, InventoryLayoutConstants.EQUIP_HEAD_Y};
            case FACE -> new int[]{InventoryLayoutConstants.EQUIP_LEFT_X, InventoryLayoutConstants.EQUIP_FACE_Y};
            case UPPER -> new int[]{InventoryLayoutConstants.EQUIP_LEFT_X, InventoryLayoutConstants.EQUIP_UPPER_Y};
            case VEST -> new int[]{InventoryLayoutConstants.EQUIP_LEFT_X, InventoryLayoutConstants.EQUIP_VEST_Y};
            case BACKPACK -> new int[]{InventoryLayoutConstants.EQUIP_RIGHT_X, InventoryLayoutConstants.EQUIP_BACK_Y};
            case GLOVES -> new int[]{InventoryLayoutConstants.EQUIP_RIGHT_X, InventoryLayoutConstants.EQUIP_GLOVES_Y};
            case PANTS -> new int[]{InventoryLayoutConstants.EQUIP_LEFT_X, InventoryLayoutConstants.EQUIP_PANTS_Y};
            case BOOTS -> new int[]{InventoryLayoutConstants.EQUIP_LEFT_X, InventoryLayoutConstants.EQUIP_BOOTS_Y};
        };
    }

    public enum EquipmentAnchor {
        HEAD,
        FACE,
        UPPER,
        VEST,
        BACKPACK,
        GLOVES,
        PANTS,
        BOOTS
    }

    public record SlotPosition(int x, int y) {
    }

    public record Layout(@NotNull Map<String, SlotPosition> positions,
                         @NotNull Map<EquipmentAnchor, String> anchors) {
        @Nullable
        public String equipmentSlotId(@NotNull EquipmentAnchor anchor) {
            return anchors.get(anchor);
        }
    }
}

