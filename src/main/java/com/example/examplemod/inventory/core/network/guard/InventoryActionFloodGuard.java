package com.example.examplemod.inventory.core.network.guard;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player, per-channel action limiter used to avoid packet/API flood.
 */
public final class InventoryActionFloodGuard {

    private static final InventoryActionFloodGuard INSTANCE = new InventoryActionFloodGuard();

    private final Map<Key, CounterWindow> windows = new ConcurrentHashMap<>();

    private InventoryActionFloodGuard() {
    }

    @NotNull
    public static InventoryActionFloodGuard getInstance() {
        return INSTANCE;
    }

    public boolean tryAcquire(@NotNull UUID playerId,
                              @NotNull String channel,
                              long tick,
                              int maxPerTick) {
        if (maxPerTick <= 0) {
            return false;
        }

        Key key = new Key(playerId, channel);
        CounterWindow window = windows.computeIfAbsent(key, ignored -> new CounterWindow(tick));

        synchronized (window) {
            if (window.tick != tick) {
                window.tick = tick;
                window.used = 0;
            }
            if (window.used >= maxPerTick) {
                return false;
            }
            window.used++;
            return true;
        }
    }

    public void clearPlayer(@NotNull UUID playerId) {
        windows.keySet().removeIf(key -> key.playerId.equals(playerId));
    }

    private record Key(UUID playerId, String channel) {}

    private static final class CounterWindow {
        long tick;
        int used;

        CounterWindow(long tick) {
            this.tick = tick;
            this.used = 0;
        }
    }
}

