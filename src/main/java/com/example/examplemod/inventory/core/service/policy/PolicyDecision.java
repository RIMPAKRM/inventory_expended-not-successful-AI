package com.example.examplemod.inventory.core.service.policy;

import org.jetbrains.annotations.NotNull;

public record PolicyDecision(boolean allowed, @NotNull String reason) {

    @NotNull
    public static PolicyDecision allow() {
        return new PolicyDecision(true, "ok");
    }

    @NotNull
    public static PolicyDecision deny(@NotNull String reason) {
        return new PolicyDecision(false, reason);
    }
}

