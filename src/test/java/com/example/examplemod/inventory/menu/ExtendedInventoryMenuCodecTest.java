package com.example.examplemod.inventory.menu;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtendedInventoryMenuCodecTest {

    @Test
    void readModSlotIds_handlesNullBuffer() {
        assertTrue(ExtendedInventoryMenu.readModSlotIds(null).isEmpty());
    }

    @Test
    void readModSlotIds_readsEncodedIdsInOrder() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        List<String> expected = List.of("examplemod:head", "examplemod:vest", "examplemod:backpack");

        buf.writeVarInt(expected.size());
        for (String id : expected) {
            buf.writeUtf(id, 128);
        }

        assertEquals(expected, ExtendedInventoryMenu.readModSlotIds(buf));
    }
}

