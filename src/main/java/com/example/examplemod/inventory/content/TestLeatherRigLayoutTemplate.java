package com.example.examplemod.inventory.content;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.inventory.core.slot.SlotDefinition;
import com.example.examplemod.inventory.core.slot.SlotGroupDefinition;
import com.example.examplemod.inventory.core.slot.SlotSource;
import com.example.examplemod.inventory.core.slot.SlotType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure M7 test equipment template: leather chest rig with extra capacity.
 */
public final class TestLeatherRigLayoutTemplate {

    public static final String ITEM_NAME = "test_leather_rig";
    public static final int MAG_SLOTS = 2;
    public static final int UTILITY_SLOTS = 2;

    private TestLeatherRigLayoutTemplate() {
    }

    @NotNull
    public static List<SlotGroupDefinition> createGroups() {
        List<SlotDefinition> slots = new ArrayList<>(MAG_SLOTS + UTILITY_SLOTS);
        for (int i = 0; i < MAG_SLOTS; i++) {
            slots.add(SlotDefinition.builder(slotId("mag_", i), SlotType.MAG_POUCH, SlotSource.EQUIPMENT)
                    .displayGroup("inv.group.test_rig")
                    .order(i)
                    .uiAnchor("right_panel")
                    .uiPriority(200)
                    .build());
        }
        for (int i = 0; i < UTILITY_SLOTS; i++) {
            slots.add(SlotDefinition.builder(slotId("utility_", i), SlotType.UTILITY, SlotSource.EQUIPMENT)
                    .displayGroup("inv.group.test_rig")
                    .order(MAG_SLOTS + i)
                    .uiAnchor("right_panel")
                    .uiPriority(200)
                    .build());
        }

        return List.of(SlotGroupDefinition.builder(groupId(), "inv.group.test_rig", SlotSource.EQUIPMENT)
                .slots(slots)
                .uiAnchor("right_panel")
                .uiPriority(200)
                .build());
    }

    @NotNull
    public static String layoutToken() {
        return ITEM_NAME + ":" + MAG_SLOTS + ":" + UTILITY_SLOTS;
    }

    @NotNull
    private static String groupId() {
        return ExampleMod.MODID + ":" + ITEM_NAME;
    }

    @NotNull
    private static String slotId(@NotNull String prefix, int index) {
        return ExampleMod.MODID + ":" + ITEM_NAME + "_" + prefix + index;
    }
}

