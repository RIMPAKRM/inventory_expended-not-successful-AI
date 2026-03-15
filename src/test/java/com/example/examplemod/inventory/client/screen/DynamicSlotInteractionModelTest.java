package com.example.examplemod.inventory.client.screen;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DynamicSlotInteractionModelTest {

    @Test
    void findSlotAt_returnsMatchingSlotInsideGrid() {
        String slotId = DynamicSlotInteractionModel.findSlotAt(
                20,
                30,
                14,
                28,
                6,
                18,
                List.of("slot_a", "slot_b"));

        assertEquals("slot_a", slotId);
    }

    @Test
    void findSlotAt_returnsNullOutsideGrid() {
        String slotId = DynamicSlotInteractionModel.findSlotAt(
                200,
                200,
                14,
                28,
                6,
                18,
                List.of("slot_a", "slot_b"));

        assertNull(slotId);
    }

    @Test
    void resolveClick_leftClickSelectsFilledSlotAndThenMoves() {
        DynamicSlotInteractionModel.ClickOutcome first = DynamicSlotInteractionModel.resolveClick(
                null,
                "slot_a",
                true,
                DynamicSlotInteractionModel.LEFT_MOUSE_BUTTON,
                false);
        assertEquals(DynamicSlotInteractionModel.Action.SELECT, first.action());
        assertEquals("slot_a", first.nextSelectedSlotId());

        DynamicSlotInteractionModel.ClickOutcome second = DynamicSlotInteractionModel.resolveClick(
                first.nextSelectedSlotId(),
                "slot_b",
                false,
                DynamicSlotInteractionModel.LEFT_MOUSE_BUTTON,
                false);
        assertEquals(DynamicSlotInteractionModel.Action.SEND_MOVE, second.action());
        assertEquals("slot_a", second.sourceSlotId());
        assertEquals("slot_b", second.targetSlotId());
        assertNull(second.nextSelectedSlotId());
    }

    @Test
    void resolveClick_leftClickOnOccupiedTargetSendsSwap() {
        DynamicSlotInteractionModel.ClickOutcome outcome = DynamicSlotInteractionModel.resolveClick(
                "slot_a",
                "slot_b",
                true,
                DynamicSlotInteractionModel.LEFT_MOUSE_BUTTON,
                false);

        assertEquals(DynamicSlotInteractionModel.Action.SEND_SWAP, outcome.action());
        assertEquals("slot_a", outcome.sourceSlotId());
        assertEquals("slot_b", outcome.targetSlotId());
    }

    @Test
    void resolveClick_shiftLeftClickQuickMovesFilledSlot() {
        DynamicSlotInteractionModel.ClickOutcome outcome = DynamicSlotInteractionModel.resolveClick(
                null,
                "slot_a",
                true,
                DynamicSlotInteractionModel.LEFT_MOUSE_BUTTON,
                true);

        assertEquals(DynamicSlotInteractionModel.Action.SEND_QUICK_MOVE, outcome.action());
        assertEquals("slot_a", outcome.sourceSlotId());
    }

    @Test
    void resolveClick_middleClickSplitsFilledSlot() {
        DynamicSlotInteractionModel.ClickOutcome outcome = DynamicSlotInteractionModel.resolveClick(
                null,
                "slot_a",
                true,
                DynamicSlotInteractionModel.MIDDLE_MOUSE_BUTTON,
                false);

        assertEquals(DynamicSlotInteractionModel.Action.SEND_SPLIT, outcome.action());
        assertEquals("slot_a", outcome.sourceSlotId());
    }

    @Test
    void resolveClick_rightClickMovesFilledSlotToVanilla() {
        DynamicSlotInteractionModel.ClickOutcome outcome = DynamicSlotInteractionModel.resolveClick(
                null,
                "slot_a",
                true,
                DynamicSlotInteractionModel.RIGHT_MOUSE_BUTTON,
                false);

        assertEquals(DynamicSlotInteractionModel.Action.SEND_MOVE_TO_VANILLA, outcome.action());
        assertEquals("slot_a", outcome.sourceSlotId());
    }

    @Test
    void resolveClick_clickOutsideOrEmptyDoesNothing() {
        DynamicSlotInteractionModel.ClickOutcome outcome = DynamicSlotInteractionModel.resolveClick(
                null,
                null,
                false,
                DynamicSlotInteractionModel.LEFT_MOUSE_BUTTON,
                false);
        assertEquals(DynamicSlotInteractionModel.Action.NONE, outcome.action());

        DynamicSlotInteractionModel.ClickOutcome empty = DynamicSlotInteractionModel.resolveClick(
                null,
                "slot_a",
                false,
                DynamicSlotInteractionModel.LEFT_MOUSE_BUTTON,
                false);
        assertEquals(DynamicSlotInteractionModel.Action.NONE, empty.action());
    }
}
