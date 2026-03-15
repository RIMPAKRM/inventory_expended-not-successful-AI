# M1 Data Core (Forge 1.20.1)

Реализован фундамент хранения расширенного инвентаря:

- серверно-авторитетное состояние (runtime) через `Player Capability`;
- persistent-слой через `SavedData` по `UUID`;
- NBT-кодек с `schemaVersion`, `revision`, `dirty`;
- lifecycle-события: login/logout/clone/respawn/dimension change.

## Где смотреть

- `src/main/java/com/example/examplemod/inventory/core/capability`
- `src/main/java/com/example/examplemod/inventory/core/storage`
- `src/main/java/com/example/examplemod/inventory/core/lifecycle`
- `src/main/java/com/example/examplemod/inventory/core/model`

## Запуск тестов

```powershell
.\gradlew.bat test
```

## Ограничения текущей фазы

- Нет M2+ (динамический layout, транзакции, sync-пакеты, внешний API, админ GUI).
- Базовый размер runtime-слотов задается в конфиге `baseRuntimeSlots`; это временный M1-фолбэк до динамических слотов M2.

