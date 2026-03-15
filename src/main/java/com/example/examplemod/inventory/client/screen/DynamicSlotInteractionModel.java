package com.example.examplemod.inventory.client.screen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Pure M7 interaction model for rendered dynamic slots.
 */
public final class DynamicSlotInteractionModel {

    public static final int LEFT_MOUSE_BUTTON = 0;
    public static final int RIGHT_MOUSE_BUTTON = 1;
    public static final int MIDDLE_MOUSE_BUTTON = 2;
    public static final int SLOT_RENDER_SIZE = 16;

    private DynamicSlotInteractionModel() {
    }

    @Nullable
    public static String findSlotAt(double mouseX,
                                    double mouseY,
                                    int startX,
                                    int startY,
                                    int columns,
                                    int slotSpacing,
                                    @NotNull List<String> slotOrder) {
        if (columns <= 0 || slotSpacing <= 0) {
            return null;
        }

        for (int i = 0; i < slotOrder.size(); i++) {
            int column = i % columns;
            int row = i / columns;
            int x = startX + column * slotSpacing;
            int y = startY + row * slotSpacing;
            if (mouseX >= x && mouseX < x + SLOT_RENDER_SIZE && mouseY >= y && mouseY < y + SLOT_RENDER_SIZE) {
                return slotOrder.get(i);
            }
        }
        return null;
    }

    @NotNull
    public static ClickOutcome resolveClick(@Nullable String selectedSlotId,
                                            @Nullable String clickedSlotId,
                                            boolean clickedHasItem,
                                            int mouseButton,
                                            boolean shiftDown) {
        if (clickedSlotId == null || mouseButton < 0) {
            return ClickOutcome.noop(selectedSlotId);
        }

        if (mouseButton == MIDDLE_MOUSE_BUTTON) {
            return clickedHasItem ? ClickOutcome.split(clickedSlotId, selectedSlotId)
                    : ClickOutcome.noop(selectedSlotId);
        }

        if (shiftDown && mouseButton == LEFT_MOUSE_BUTTON) {
            return clickedHasItem ? ClickOutcome.quickMove(clickedSlotId, selectedSlotId)
                    : ClickOutcome.noop(selectedSlotId);
        }

        if (mouseButton == RIGHT_MOUSE_BUTTON) {
            return clickedHasItem
                    ? ClickOutcome.moveToVanilla(clickedSlotId, selectedSlotId)
                    : ClickOutcome.noop(selectedSlotId);
        }

        if (mouseButton != LEFT_MOUSE_BUTTON) {
            return ClickOutcome.noop(selectedSlotId);
        }

        if (selectedSlotId == null || selectedSlotId.isBlank()) {
            return clickedHasItem
                    ? ClickOutcome.select(clickedSlotId)
                    : ClickOutcome.noop(null);
        }

        if (selectedSlotId.equals(clickedSlotId)) {
            return ClickOutcome.clear();
        }

        return clickedHasItem
                ? ClickOutcome.swap(selectedSlotId, clickedSlotId)
                : ClickOutcome.move(selectedSlotId, clickedSlotId);
    }

    public enum Action {
        NONE,
        SELECT,
        CLEAR_SELECTION,
        SEND_MOVE,
        SEND_SWAP,
        SEND_SPLIT,
        SEND_QUICK_MOVE,
        SEND_MOVE_TO_VANILLA
    }

    public record ClickOutcome(@NotNull Action action,
                               @Nullable String sourceSlotId,
                               @Nullable String targetSlotId,
                               @Nullable String nextSelectedSlotId) {

        @NotNull
        public static ClickOutcome noop(@Nullable String selectedSlotId) {
            return new ClickOutcome(Action.NONE, null, null, selectedSlotId);
        }

        @NotNull
        public static ClickOutcome select(@NotNull String slotId) {
            return new ClickOutcome(Action.SELECT, null, null, slotId);
        }

        @NotNull
        public static ClickOutcome clear() {
            return new ClickOutcome(Action.CLEAR_SELECTION, null, null, null);
        }

        @NotNull
        public static ClickOutcome move(@NotNull String fromSlotId, @NotNull String toSlotId) {
            return new ClickOutcome(Action.SEND_MOVE, fromSlotId, toSlotId, null);
        }

        @NotNull
        public static ClickOutcome swap(@NotNull String fromSlotId, @NotNull String toSlotId) {
            return new ClickOutcome(Action.SEND_SWAP, fromSlotId, toSlotId, null);
        }

        @NotNull
        public static ClickOutcome split(@NotNull String slotId, @Nullable String selectedSlotId) {
            return new ClickOutcome(Action.SEND_SPLIT, slotId, null, selectedSlotId);
        }

        @NotNull
        public static ClickOutcome quickMove(@NotNull String slotId, @Nullable String selectedSlotId) {
            return new ClickOutcome(Action.SEND_QUICK_MOVE, slotId, null, selectedSlotId);
        }

        @NotNull
        public static ClickOutcome moveToVanilla(@NotNull String slotId, @Nullable String selectedSlotId) {
            return new ClickOutcome(Action.SEND_MOVE_TO_VANILLA, slotId, null, selectedSlotId);
        }
    }
}

