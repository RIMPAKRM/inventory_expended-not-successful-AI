# M5 Iteration Notes - Public API Beta

## Changelog (кратко)

- Добавлен публичный серверный API:
  - `inventory/api/IExtendedInventoryApi`
  - `inventory/api/ExtendedInventoryApi`
  - `inventory/api/InventoryAccessRequest`
  - `inventory/api/InventoryMutationResult`
  - `inventory/api/InventorySlotView`
  - `inventory/api/AccessMode`
- Реализован серверный фасад `ExtendedInventoryApiService`:
  - мутации только через `InventoryTransactionService`,
  - обязательная проверка `revision/layoutVersion`,
  - partial sync только после успешного commit.
- Добавлен anti-flood guard `InventoryActionFloodGuard`:
  - отдельные каналы лимитов для `c2s` и `api`,
  - лимиты на тик игрока через конфиг.
- Усилен `C2SInventoryActionPacket`:
  - добавлен `actionId`,
  - добавлен server-side rate-limit до транзакции,
  - сохранена server-authoritative проверка `revision + layoutToken`.
- Обновлен сетевой протокол `InventoryNetworkHandler` до `m5-api-v1`.
- Добавлены M5-конфиги:
  - `c2sActionsPerTickLimit`,
  - `apiMutationsPerTickLimit`.

## Проверочные шаги

1. Запустить unit-тесты M5:
   - `./gradlew test --tests "*InventoryActionFloodGuardTest"`
   - `./gradlew test --tests "*ExtendedInventoryApiServiceStateTest"`
   - `./gradlew test --tests "*InventoryMutationResultTest"`
   - `./gradlew test --tests "*InventoryAccessRequestTest"`
2. В dev-среде открыть сервер/клиент и выполнить несколько C2S-инвентарных действий за один тик:
   - при превышении `c2sActionsPerTickLimit` действия сверх лимита отклоняются без применения мутации.
3. Проверить API-мутации внешним вызовом:
   - при `READ_ONLY` мутация возвращает `FORBIDDEN`;
   - при mismatch `revision/layoutVersion` возвращается `CONFLICT`;
   - при commit меняется `revision` и ставится partial sync.
4. Проверить anti-flood API:
   - сделать burst мутаций в один тик выше `apiMutationsPerTickLimit`;
   - ожидать `RATE_LIMITED` для лишних операций.

## Ограничения текущей итерации

- Контракт API beta: допускается расширение полей в M6/M7 без гарантии полной бинарной совместимости.
- Политика access-control на уровне `modid`/whitelist пока не включена, только `AccessMode` в request.

