package com.example.examplemod;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue BASE_RUNTIME_SLOTS = BUILDER
            .comment("Base slot count for M1 runtime inventory capability. M2 will replace this with dynamic layout.")
            .defineInRange("baseRuntimeSlots", 12, 1, 256);

    private static final ForgeConfigSpec.BooleanValue DEBUG_LIFECYCLE_LOGS = BUILDER
            .comment("Enable verbose lifecycle logs for the inventory data core.")
            .define("debugLifecycleLogs", false);

    private static final ForgeConfigSpec.IntValue PARTIAL_SYNC_SLOT_LIMIT = BUILDER
            .comment("If dirty slot count exceeds this value in one batch, fallback to full sync.")
            .defineInRange("partialSyncSlotLimit", 24, 1, 256);

    private static final ForgeConfigSpec.IntValue C2S_ACTIONS_PER_TICK_LIMIT = BUILDER
            .comment("Maximum accepted C2S inventory actions per player per server tick.")
            .defineInRange("c2sActionsPerTickLimit", 12, 1, 256);

    private static final ForgeConfigSpec.IntValue API_MUTATIONS_PER_TICK_LIMIT = BUILDER
            .comment("Maximum accepted API inventory mutations per player per server tick.")
            .defineInRange("apiMutationsPerTickLimit", 32, 1, 512);

    private static final ForgeConfigSpec.IntValue API_ADMIN_PERMISSION_LEVEL = BUILDER
            .comment("Minimum permission level required to access another player's inventory via API.")
            .defineInRange("apiAdminPermissionLevel", 2, 0, 4);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> API_ALLOWED_MOD_IDS = BUILDER
            .comment("Allowed external mod ids for inventory API. Empty list means allow all.")
            .defineListAllowEmpty("apiAllowedModIds", List.of(), it -> it instanceof String);

    private static final ForgeConfigSpec.BooleanValue REPLACE_VANILLA_INVENTORY_UI = BUILDER
            .comment("Enable M7 replacement of the vanilla inventory screen with the extended inventory UI.")
            .define("replaceVanillaInventoryUi", true);

    private static final ForgeConfigSpec.BooleanValue INVENTORY_UI_KILL_SWITCH = BUILDER
            .comment("Emergency kill-switch that forces fallback to vanilla inventory UI.")
            .define("inventoryUiKillSwitch", false);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int baseRuntimeSlots = 12;
    public static boolean debugLifecycleLogs = false;
    public static int partialSyncSlotLimit = 24;
    public static int c2sActionsPerTickLimit = 12;
    public static int apiMutationsPerTickLimit = 32;
    public static int apiAdminPermissionLevel = 2;
    public static List<String> apiAllowedModIds = List.of();
    public static boolean replaceVanillaInventoryUi = true;
    public static boolean inventoryUiKillSwitch = false;

    public static int getBaseRuntimeSlots() {
        return Math.max(1, baseRuntimeSlots);
    }

    public static int getPartialSyncSlotLimit() {
        return Math.max(1, partialSyncSlotLimit);
    }

    public static int getC2sActionsPerTickLimit() {
        return Math.max(1, c2sActionsPerTickLimit);
    }

    public static int getApiMutationsPerTickLimit() {
        return Math.max(1, apiMutationsPerTickLimit);
    }

    public static int getApiAdminPermissionLevel() {
        return Math.max(0, Math.min(4, apiAdminPermissionLevel));
    }

    public static List<String> getApiAllowedModIds() {
        return List.copyOf(apiAllowedModIds);
    }

    public static boolean isReplaceVanillaInventoryUiEnabled() {
        return replaceVanillaInventoryUi;
    }

    public static boolean isInventoryUiKillSwitchEnabled() {
        return inventoryUiKillSwitch;
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        baseRuntimeSlots = BASE_RUNTIME_SLOTS.get();
        debugLifecycleLogs = DEBUG_LIFECYCLE_LOGS.get();
        partialSyncSlotLimit = PARTIAL_SYNC_SLOT_LIMIT.get();
        c2sActionsPerTickLimit = C2S_ACTIONS_PER_TICK_LIMIT.get();
        apiMutationsPerTickLimit = API_MUTATIONS_PER_TICK_LIMIT.get();
        apiAdminPermissionLevel = API_ADMIN_PERMISSION_LEVEL.get();
        replaceVanillaInventoryUi = REPLACE_VANILLA_INVENTORY_UI.get();
        inventoryUiKillSwitch = INVENTORY_UI_KILL_SWITCH.get();

        List<String> normalized = new ArrayList<>();
        for (Object value : API_ALLOWED_MOD_IDS.get()) {
            String s = String.valueOf(value).trim();
            if (!s.isBlank()) {
                normalized.add(s);
            }
        }
        apiAllowedModIds = List.copyOf(normalized);
    }
}
