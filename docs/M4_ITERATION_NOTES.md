# M4 Iteration Notes - Sync Stable

## Changelog (кратко)

- Добавлен сетевой слой M4: `S2CFullInventorySyncPacket`, `S2CPartialInventorySyncPacket`, `C2SInventoryActionPacket`.
- Добавлен `InventoryNetworkHandler` и регистрация пакетов в `ExampleMod#commonSetup`.
- Добавлен `InventorySyncOrchestrator` для batching изменений по игроку за тик.
- Добавлен `InventorySyncService` и Forge-события `InventorySyncEvents` для flush в `ServerTickEvent`.
- Добавлена серверная валидация C2S действий по `revision + layoutToken` перед выполнением транзакции.
- Добавлены full-sync триггеры в lifecycle (`login/clone/respawn/dimension change`) и на layout invalidation (`LAYOUT_CHANGED`).
- Добавлен конфиг `partialSyncSlotLimit` для fallback partial -> full без packet flood.
- Добавлены unit-тесты для оркестратора sync-батчинга.
- Добавлен full-sync trigger `OPEN_MENU` через `PlayerContainerEvent.Open` (для меню нашего мода).
- Добавлены unit-тесты `ClientInventorySyncStateTest` для full/partial/stale/mismatch сценариев.

## Проверочные шаги

1. Запустить unit-тесты:
   - `./gradlew test --tests "*InventorySyncOrchestratorTest"`
   - `./gradlew test --tests "*ClientInventorySyncStateTest"`
2. Запустить сервер/клиент в dev-режиме и открыть мир.
3. Проверить login full sync:
   - войти на сервер;
   - убедиться в логах/дебаге, что отправлен full sync (`LOGIN`).
4. Проверить partial batching:
   - сделать несколько перемещений в пределах одного тика (или быстро подряд);
   - убедиться, что отправляется один partial пакет с объединенным списком dirty slot id.
5. Проверить revision conflict:
   - отправить C2S действие со старой `expectedRevision`;
   - убедиться, что операция не применяется и запрашивается full sync (`REVISION_CONFLICT`).
6. Проверить layout change:
   - надеть/снять предмет, меняющий layout;
   - убедиться, что кэш layout invalidated и отправлен full sync (`LAYOUT_CHANGED`).
7. Проверить OPEN_MENU триггер:
   - открыть меню, используя `PlayerContainerEvent.Open`;
   - убедиться, что отправлен full sync для открываемого меню.

## Ограничения текущей итерации

- Клиентское состояние `ClientInventorySyncState` остается временным зеркалом до полной GUI-интеграции.
