package com.example.examplemod.inventory.core.network.client;

import org.jetbrains.annotations.NotNull;

/**
 * Client mirror for last craft action result.
 */
public final class ClientCraftPanelState {

    private static boolean lastSuccess;
    private static String lastRecipeId = "";
    private static String lastMessageKey = "";

    private ClientCraftPanelState() {
    }

    public static synchronized void applyCreateResult(boolean success,
                                                      @NotNull String recipeId,
                                                      @NotNull String messageKey) {
        lastSuccess = success;
        lastRecipeId = recipeId;
        lastMessageKey = messageKey;
    }

    public static synchronized boolean lastSuccess() {
        return lastSuccess;
    }

    @NotNull
    public static synchronized String lastRecipeId() {
        return lastRecipeId;
    }

    @NotNull
    public static synchronized String lastMessageKey() {
        return lastMessageKey;
    }

    public static synchronized void resetForTests() {
        lastSuccess = false;
        lastRecipeId = "";
        lastMessageKey = "";
    }
}
