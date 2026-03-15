# M6 Iteration Notes - Admin Policy and Audit Layer

## Changelog (кратко)

- Добавлен policy-слой M6 поверх `AccessMode`:
  - `InventoryAccessPolicyService`
  - проверка `modId` по allow-list
  - проверка admin-permission для cross-player доступа (actor != target)
- API переведен на actor/target контракт:
  - `IExtendedInventoryApi` теперь принимает `actor` и `target`
  - доступ к инвентарю другого игрока возможен только администратору
- Добавлен audit слой для API-мутаций:
  - `InventoryAuditEvent`
  - `InventoryAuditService`
  - фиксируются: кто/кого/что/когда/результат
- Сохранены M4/M5 гарантии:
  - серверно-авторитетные операции
  - только транзакционные мутации через `InventoryTransactionService`
  - partial sync только после commit
  - анти-флуд лимиты на тик остаются активными
- Добавлен интеграционный тест потока:
  - external mod API call -> transaction -> sync packet decision.

## Проверочные шаги

1. Запустить M6 тесты:
   - `./gradlew test --tests "*InventoryAccessPolicyServiceTest"`
   - `./gradlew test --tests "*InventoryAuditServiceTest"`
   - `./gradlew test --tests "*InventoryApiMutationFlowIntegrationTest"`
2. Проверить policy:
   - non-admin actor не может читать/менять `target != actor`;
   - admin actor может.
3. Проверить аудит:
   - после API-мутации в `InventoryAuditService.recent()` появляется событие.
4. Проверить sync-поведение:
   - при commit генерируется partial decision;
   - при конфликте revision/layout запрашивается full decision.

## Конфиг M6

- `apiAdminPermissionLevel` - минимальный permission-level для доступа к чужому инвентарю.
- `apiAllowedModIds` - allow-list модов для внешнего API (пусто = разрешены все).

