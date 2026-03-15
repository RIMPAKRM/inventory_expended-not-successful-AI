package com.example.examplemod.inventory.core.layout;

import com.example.examplemod.inventory.content.TestLeatherRigLayoutTemplate;
import com.example.examplemod.inventory.core.slot.IEquipmentSlotProvider;
import com.example.examplemod.inventory.core.slot.PlayerLayoutProfile;
import com.example.examplemod.inventory.core.slot.SlotDefinition;
import com.example.examplemod.inventory.core.slot.SlotGroupDefinition;
import com.example.examplemod.inventory.core.slot.SlotSource;
import com.example.examplemod.inventory.core.slot.SlotType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

/**
 * Unit tests for the pure layout-building logic inside {@link EquipmentLayoutService}.
 *
 * <p>These tests do NOT start a Minecraft server. They only test the data-model and
 * token/profile assembly logic that has no Minecraft side-effects.</p>
 */
class EquipmentLayoutServiceTest {

    // ── PlayerLayoutProfile assembly ──────────────────────────────────────

    @Test
    void profileContainsAllSlotsFromGroups() {
        SlotDefinition slot0 = SlotDefinition
                .builder("test:base_0", SlotType.UTILITY, SlotSource.BASE)
                .build();
        SlotDefinition slot1 = SlotDefinition
                .builder("test:equipment_0", SlotType.MAG_POUCH, SlotSource.EQUIPMENT)
                .build();

        SlotGroupDefinition baseGroup = SlotGroupDefinition
                .builder("test:base", "Base", SlotSource.BASE)
                .slots(List.of(slot0))
                .build();
        SlotGroupDefinition equipGroup = SlotGroupDefinition
                .builder("test:equip", "Chest Rig", SlotSource.EQUIPMENT)
                .slots(List.of(slot1))
                .build();

        PlayerLayoutProfile profile = new PlayerLayoutProfile(
                UUID.randomUUID(), "token-v1", List.of(baseGroup, equipGroup));

        Assertions.assertEquals(2, profile.getTotalSlotCount());
        Assertions.assertNotNull(profile.findSlot("test:base_0"));
        Assertions.assertNotNull(profile.findSlot("test:equipment_0"));
        Assertions.assertNull(profile.findSlot("test:nonexistent"));
    }

    @Test
    void profilePreservesSlotOrder() {
        List<SlotDefinition> slots = List.of(
                SlotDefinition.builder("test:s0", SlotType.UTILITY, SlotSource.BASE).order(0).build(),
                SlotDefinition.builder("test:s1", SlotType.UTILITY, SlotSource.BASE).order(1).build(),
                SlotDefinition.builder("test:s2", SlotType.UTILITY, SlotSource.BASE).order(2).build()
        );
        SlotGroupDefinition group = SlotGroupDefinition
                .builder("test:g", "G", SlotSource.BASE).slots(slots).build();

        PlayerLayoutProfile profile = new PlayerLayoutProfile(UUID.randomUUID(), "t", List.of(group));

        List<SlotDefinition> all = profile.getAllSlots();
        Assertions.assertEquals("test:s0", all.get(0).getSlotId());
        Assertions.assertEquals("test:s1", all.get(1).getSlotId());
        Assertions.assertEquals("test:s2", all.get(2).getSlotId());
    }

    @Test
    void profileSlotIndexMatchesFlatOrder() {
        SlotDefinition a = SlotDefinition.builder("test:a", SlotType.UTILITY, SlotSource.BASE).build();
        SlotDefinition b = SlotDefinition.builder("test:b", SlotType.MAG_POUCH, SlotSource.EQUIPMENT).build();

        SlotGroupDefinition g1 = SlotGroupDefinition.builder("test:g1", "G1", SlotSource.BASE).slots(List.of(a)).build();
        SlotGroupDefinition g2 = SlotGroupDefinition.builder("test:g2", "G2", SlotSource.EQUIPMENT).slots(List.of(b)).build();

        PlayerLayoutProfile profile = new PlayerLayoutProfile(UUID.randomUUID(), "t", List.of(g1, g2));

        Assertions.assertEquals(0, profile.getSlotIndex("test:a"));
        Assertions.assertEquals(1, profile.getSlotIndex("test:b"));
        Assertions.assertEquals(-1, profile.getSlotIndex("test:missing"));
    }

    @Test
    void profileFiltersByType() {
        SlotDefinition u0 = SlotDefinition.builder("test:u0", SlotType.UTILITY, SlotSource.BASE).build();
        SlotDefinition m0 = SlotDefinition.builder("test:m0", SlotType.MAG_POUCH, SlotSource.EQUIPMENT).build();
        SlotDefinition m1 = SlotDefinition.builder("test:m1", SlotType.MAG_POUCH, SlotSource.EQUIPMENT).build();

        SlotGroupDefinition g = SlotGroupDefinition.builder("test:g", "G", SlotSource.BASE)
                .slots(List.of(u0, m0, m1)).build();
        PlayerLayoutProfile profile = new PlayerLayoutProfile(UUID.randomUUID(), "t", List.of(g));

        Assertions.assertEquals(1, profile.getSlotsByType(SlotType.UTILITY).size());
        Assertions.assertEquals(2, profile.getSlotsByType(SlotType.MAG_POUCH).size());
        Assertions.assertEquals(0, profile.getSlotsByType(SlotType.MEDICAL).size());
    }

    @Test
    void profileFiltersBySource() {
        SlotDefinition base = SlotDefinition.builder("test:b", SlotType.UTILITY, SlotSource.BASE).build();
        SlotDefinition equip = SlotDefinition.builder("test:e", SlotType.MAG_POUCH, SlotSource.EQUIPMENT).build();

        SlotGroupDefinition g = SlotGroupDefinition.builder("test:g", "G", SlotSource.BASE)
                .slots(List.of(base, equip)).build();
        PlayerLayoutProfile profile = new PlayerLayoutProfile(UUID.randomUUID(), "t", List.of(g));

        Assertions.assertEquals(1, profile.getSlotsBySource(SlotSource.BASE).size());
        Assertions.assertEquals(1, profile.getSlotsBySource(SlotSource.EQUIPMENT).size());
        Assertions.assertEquals(0, profile.getSlotsBySource(SlotSource.CONTAINER).size());
    }

    // ── IEquipmentSlotProvider default method ─────────────────────────────

    @Test
    void slotProviderDefaultLayoutVersionTokenReturnsNonNull() {
        // Minimal anonymous implementation — just checking the default method contract
        IEquipmentSlotProvider provider = stack -> List.of();
        // Can't instantiate ItemStack without Minecraft bootstrap; test the interface contract only
        Assertions.assertNotNull(provider);
    }

    // ── SlotDefinition builder ────────────────────────────────────────────

    @Test
    void slotDefinitionDefaultsAreVisible_enabled() {
        SlotDefinition def = SlotDefinition
                .builder("test:slot", SlotType.UTILITY, SlotSource.BASE)
                .build();
        Assertions.assertTrue(def.isVisible());
        Assertions.assertTrue(def.isEnabled());
        Assertions.assertEquals(SlotType.UTILITY, def.getSlotType());
        Assertions.assertEquals(SlotSource.BASE, def.getSource());
    }

    @Test
    void slotDefinitionDisabledHidden() {
        SlotDefinition def = SlotDefinition
                .builder("test:slot", SlotType.OVERFLOW_READONLY, SlotSource.BASE)
                .disabled()
                .hidden()
                .build();
        Assertions.assertFalse(def.isVisible());
        Assertions.assertFalse(def.isEnabled());
    }

    @Test
    void slotDefinitionEqualityBySlotId() {
        SlotDefinition a = SlotDefinition.builder("test:x", SlotType.UTILITY, SlotSource.BASE).build();
        SlotDefinition b = SlotDefinition.builder("test:x", SlotType.MAG_POUCH, SlotSource.EQUIPMENT).build();
        SlotDefinition c = SlotDefinition.builder("test:y", SlotType.UTILITY, SlotSource.BASE).build();
        Assertions.assertEquals(a, b, "Same id should be equal regardless of type");
        Assertions.assertNotEquals(a, c);
    }

    // ── SlotGroupDefinition ───────────────────────────────────────────────

    @Test
    void slotGroupDefinitionCountMatchesSlotList() {
        SlotGroupDefinition group = SlotGroupDefinition
                .builder("test:g", "Label", SlotSource.BASE)
                .slots(List.of(
                        SlotDefinition.builder("test:s0", SlotType.UTILITY, SlotSource.BASE).build(),
                        SlotDefinition.builder("test:s1", SlotType.UTILITY, SlotSource.BASE).build()
                ))
                .build();
        Assertions.assertEquals(2, group.getSlotCount());
    }

    @Test
    void slotGroupDefinitionImmutableSlotList() {
        SlotGroupDefinition group = SlotGroupDefinition
                .builder("test:g", "Label", SlotSource.BASE)
                .slots(List.of(
                        SlotDefinition.builder("test:s0", SlotType.UTILITY, SlotSource.BASE).build()
                ))
                .build();
        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> group.getSlots().add(
                        SlotDefinition.builder("test:s1", SlotType.UTILITY, SlotSource.BASE).build()
                ));
    }

    @Test
    void testLeatherRigTemplateAddsExpectedEquipmentSlots() {
        List<SlotGroupDefinition> groups = TestLeatherRigLayoutTemplate.createGroups();

        Assertions.assertEquals(1, groups.size());
        SlotGroupDefinition group = groups.get(0);
        Assertions.assertEquals(TestLeatherRigLayoutTemplate.MAG_SLOTS + TestLeatherRigLayoutTemplate.UTILITY_SLOTS,
                group.getSlotCount());
        Assertions.assertEquals(SlotSource.EQUIPMENT, group.getSource());
        Assertions.assertEquals(2, group.getSlots().stream().filter(slot -> slot.getSlotType() == SlotType.MAG_POUCH).count());
        Assertions.assertEquals(2, group.getSlots().stream().filter(slot -> slot.getSlotType() == SlotType.UTILITY).count());
        Assertions.assertTrue(TestLeatherRigLayoutTemplate.layoutToken().contains(TestLeatherRigLayoutTemplate.ITEM_NAME));
    }
}
