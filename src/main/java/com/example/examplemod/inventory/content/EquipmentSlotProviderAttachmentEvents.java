package com.example.examplemod.inventory.content;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.inventory.core.layout.EquipmentLayoutService;
import com.example.examplemod.inventory.core.slot.IEquipmentSlotProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EquipmentSlotProviderAttachmentEvents {

    private static final ResourceLocation PROVIDER_ID = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "equipment_slot_provider");

    private EquipmentSlotProviderAttachmentEvents() {
    }

    @SubscribeEvent
    public static void onAttachItemStackCapabilities(AttachCapabilitiesEvent<ItemStack> event) {
        if (!(event.getObject().getItem() instanceof IEquipmentSlotProvider provider)) {
            return;
        }

        event.addCapability(PROVIDER_ID, new ICapabilityProvider() {
            private final LazyOptional<IEquipmentSlotProvider> optional = LazyOptional.of(() -> provider);

            @Override
            public <T> @NotNull LazyOptional<T> getCapability(@NotNull net.minecraftforge.common.capabilities.Capability<T> cap,
                                                              net.minecraft.core.Direction side) {
                if (cap == EquipmentLayoutService.SLOT_PROVIDER_CAP) {
                    return optional.cast();
                }
                return LazyOptional.empty();
            }
        });
    }
}

