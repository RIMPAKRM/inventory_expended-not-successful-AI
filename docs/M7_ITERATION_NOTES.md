# M7 Iteration Notes - UI Replacement Foundation

## Changelog (кратко)

- Добавлен M7 rollout-каркас замены ванильного inventory UI:
  - `InventoryUiFeatureFlags`
  - `InventoryScreenRouter`
  - `InventoryClientSetup`
  - `ExtendedInventoryMenu`
  - `ExtendedInventoryScreen`
  - `C2SOpenExtendedInventoryPacket`
- Добавлены feature flags и kill-switch:
  - `replaceVanillaInventoryUi`
  - `inventoryUiKillSwitch`
- Добавлена тестовая экипировка на базе кожаной брони:
  - `TestLeatherRigItem`
  - `ModItems`
  - `EquipmentSlotProviderAttachmentEvents`
  - `TestLeatherRigLayoutTemplate`
- Test Leather Rig даёт 4 дополнительных слота через существующую dynamic-layout архитектуру:
  - 2 `MAG_POUCH`
  - 2 `UTILITY`
- Новый UI опирается только на серверный sync state (`ClientInventorySyncState`) и не пишет состояние локально.
- Открытие нового экрана идёт через серверный `SimpleMenuProvider`, чтобы сервер оставался источником истины.
- Добавлен anti-spam cooldown на клиентский open-request.
- Добавлен следующий шаг M7 для dynamic slots:
  - `DynamicSlotInteractionModel`
  - кликабельные слоты в `ExtendedInventoryScreen`
  - `LMB` = select/move intent
  - `RMB` = extract intent
- Все клики экрана отправляют только `C2SInventoryActionPacket`, без локальной мутации клиентского состояния.
- Добавлены unit-тесты на hit-test и click-resolution для dynamic slots.
- Добавлен richer action-contract для M7 dynamic slots:
  - `swap`
  - `split`
  - `quick-move`
- `ExtendedInventoryScreen` теперь маппит:
  - `LMB` по занятому target при выбранном source -> `swap`
  - `Shift+LMB` -> `quick-move`
  - `MMB` -> `split`
- `C2SInventoryActionPacket` расширен новыми server-authoritative intent-типами.
- `InventoryTransactionService` расширен транзакционными операциями `swapItems`, `splitStack`, `quickMoveItem`.
- Добавлено отображение секций ванильного инвентаря игрока (main + hotbar) внутри `ExtendedInventoryScreen`.
- Добавлен `VanillaInventoryGridModel` для hit-test vanilla слотов.
- Добавлен новый intent `QUICK_MOVE_VANILLA` в `C2SInventoryActionPacket`.
- Добавлена транзакционная операция `quickMoveFromVanillaSlot(...)` в `InventoryTransactionService`.
- Shift+LMB по ванильному слоту в новом UI отправляет server-authoritative quick-move в extended slots.

## Проверочные шаги

1. Запустить unit-тесты M7:
   - `./gradlew test --tests "*InventoryUiFeatureFlagsTest"`
   - `./gradlew test --tests "*InventoryScreenRouterTest"`
   - `./gradlew test --tests "*DynamicSlotInteractionModelTest"`
   - `./gradlew test --tests "*VanillaInventoryGridModelTest"`
   - `./gradlew test --tests "*EquipmentLayoutServiceTest"`
2. Запустить клиент/сервер dev-среду.
3. Выдать предмет `inventory:test_leather_rig` и надеть его в слот нагрудника.
4. Нажать `E`:
   - должен открыться `ExtendedInventoryScreen`, а не vanilla inventory,
   - при kill-switch должен вернуться vanilla fallback.
5. Проверить dynamic layout:
   - после надевания предмета должен обновиться layout token,
   - должен пройти `LAYOUT_CHANGED` full sync,
   - на экране должны появиться дополнительные слоты test rig.
6. Проверить клики по dynamic slots:
   - `LMB` по заполненному слоту выделяет его;
   - `LMB` по пустому target отправляет server-side `MOVE`;
   - `LMB` по занятому target отправляет server-side `SWAP`;
   - `Shift+LMB` по заполненному dynamic слоту отправляет server-side `QUICK_MOVE`;
   - `MMB` по заполненному слоту отправляет server-side `SPLIT`;
   - `RMB` по заполненному слоту отправляет server-side `EXTRACT`;
   - после sync экран отражает обновлённое состояние.
7. Проверить vanilla -> extended quick-move:
   - положить предмет в ванильный инвентарь (main/hotbar);
   - в `ExtendedInventoryScreen` сделать `Shift+LMB` по этому ванильному слоту;
   - убедиться, что предмет серверно переносится в совместимый extended слот.

## Ограничения текущего среза M7

- Добавлен server-authoritative путь vanilla -> extended (`Shift+LMB`), но обратный extended -> vanilla path пока базовый и требует отдельного parity-шага.
