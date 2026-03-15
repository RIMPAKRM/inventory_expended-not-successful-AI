# M8+ Iteration Notes - Equipment UX and Embedded Craft Panel

## Changelog (кратко)

- [M8+ forge-slot cutover] Все слоты инвентаря переведены на реальные Forge `Slot`:
  - `hotbar` и `main` (только creative) остаются ванильными `Slot` из `Inventory`,
  - экипировка/одежда и dynamic-слоты теперь `SlotItemHandler` в `ExtendedInventoryMenu`,
  - удалены клиентские hitbox-клики по mod-слотам: клики/drag-and-drop обрабатывает `AbstractContainerMenu`.
- [M8+ open-screen sync] При открытии меню сервер передает клиенту `slotOrder` через `FriendlyByteBuf`, чтобы набор и индексы Forge-слотов совпадали на обеих сторонах.
- [M8+ sync hardening] `ExtendedInventoryMenu.broadcastChanges()` отслеживает изменения mod-слотов и шлет только partial-sync по реально измененным `slotId` (без packet flood).

- [M8 hardening] Исправлена интерактивность mod-слотов без обхода server-authoritative контракта:
  - `LMB` по пустому mod-слоту в survival отправляет `MOVE_VANILLA_TO_MOD` из выбранного hotbar-слота,
  - `RMB` по занятому mod-слоту отправляет `MOVE_MOD_TO_VANILLA` (снятие экипировки без удаления предмета).
- [M8 hardening] Добавлены серверные транзакции `moveVanillaToExtendedSlot(...)` и `moveExtendedToVanilla(...)`:
  - полный rollback capability + vanilla inventory при ошибке,
  - соблюдение режима слотов (`hotbar-only` вне creative).
- [M8 hardening] `ExtendedInventoryScreen` переведен на атомарный `ClientInventorySyncState.snapshot()`:
  - меньше копирований `slots/slotOrder` в одном кадре,
  - ниже риск GC-spike и деградации FPS при длительной сессии.
- [M8 hardening] Исправлен preview персонажа (`renderEntityInInventoryFollowsMouse`):
  - нормализован offset взгляда по мыши,
  - увеличен масштаб и выровнена позиция в центральном блоке.

- Переведен routing открытия инвентаря на M8-правило режимов:
  - в `creative` ванильный экран не перехватывается,
  - в `survival/adventure` открывается `ExtendedInventoryScreen`.
- Усилена server-authoritative проверка `QUICK_MOVE_VANILLA`:
  - в `creative` разрешены ванильные слоты `0..35`,
  - вне `creative` разрешен только hotbar `0..8`.
- `ExtendedInventoryScreen` переработан в M8-layout:
  - центральный блок персонажа,
  - слоты экипировки вокруг силуэта,
  - отдельная зона dynamic slots,
  - правая встроенная панель крафта (UI-заглушка для визуального контракта).
- Для `survival/adventure` добавлен режим `main.locked`:
  - `main 3x9` визуально заблокирован,
  - доступен только ванильный hotbar.
- Добавлены unit-тесты на:
  - режимный роутинг inventory UI,
  - hotbar-only hit-test для ванильной сетки,
  - серверную политику vanilla quick-move слотов.
- Добавлен минимальный server-authoritative backend для craft-панели (M8 vertical slice):
  - `C2SCraftCreateIntentPacket` (intent `Create`),
  - серверная валидация recipe/revision/layout,
  - транзакционный путь `craftRecipe(...)` в `InventoryTransactionService`,
  - `S2CCraftCreateResultPacket` + `ClientCraftPanelState` для UI-статуса результата.
- Добавлен минимальный реестр рецептов M8:
  - recipe id `test_leather_rig`,
  - требования: `minecraft:leather x4`, `minecraft:string x2`,
  - результат: `inventory:test_leather_rig`.
- Кнопка `Создать` в `ExtendedInventoryScreen` теперь отправляет только server intent и отображает последний серверный статус (успех/ошибка).

## Цель итерации M8+

Собрать production-версию игрового экрана инвентаря с центральным блоком персонажа/экипировки и встроенной панелью крафта справа, сохранив server-authoritative контракт из M7.

## Scope M8+

Входит в M8+:
- Новый layout `ExtendedInventoryScreen`:
  - центр: силуэт игрока (в стиле ванильного интерфейса),
  - вокруг силуэта: слоты экипировки,
  - справа: встроенная панель крафта.
- Фиксированные слоты экипировки вокруг персонажа:
  - голова (`helmet/mask`),
  - лицо (`gas mask/face mask`),
  - верх (`jacket/armor`),
  - жилет (`rig/vest`),
  - рюкзак (`backpack`),
  - перчатки (`gloves`),
  - штаны (`pants`),
  - ботинки (`boots`).
- Режимы отображения инвентаря:
  - `creative`: отображается полный ванильный инвентарь (`main 3x9` + `hotbar 1x9`),
  - `survival/adventure`: отображается модовый инвентарь (`ExtendedInventoryScreen`), где из ванильных слотов доступен только `hotbar 1x9`.
- Дополнительные слоты (например, карманы разгрузки, utility-слоты рюкзака) появляются рядом с основным инвентарем в том же окне и визуально выглядят как обычные ванильные слоты.
- Встроенная панель крафта:
  - ванильный стиль рамок/фона/сеточной подачи,
  - внутри панели: список категорий слева и сетка рецептов справа,
  - доступные рецепты подсвечены, недоступные затемнены,
  - `tooltip` показывает название, требования и текущие ресурсы,
  - кнопка `Создать` активна только при наличии всех материалов.

Не входит в M8+:
- новая экономика рецептов,
- автокрафт и крафт по очереди,
- изменение базовой transaction-модели и sync-протокола beyond текущего action-contract.

## UX-контракт M8+

- Экран открывается по `E` через существующий routing/feature-flag механизм (без локального bypass сервера).
- В `creative` игрок видит полный ванильный инвентарь; в `survival/adventure` игрок видит модовый экран с hotbar и слотами экипировки/дополнительными слотами.
- Все пользовательские действия (экипировка, перенос, крафт) отправляют intent на сервер; клиент не делает финальные локальные мутации.
- Режим отображения (`creative` vs `survival/adventure`) и доступность слотов определяются серверным state и обновляются через sync.
- Правая крафт-панель использует актуальный серверный snapshot инвентаря/слотов для расчета доступности рецептов.
- При server-ответе UI обновляется через существующие full/partial sync механизмы.

## Поведение dynamic slots при надевании экипировки

- При надевании/снятии предмета экипировки сервер пересчитывает layout и обновляет `layoutToken`.
- При изменении layout отправляется `LAYOUT_CHANGED` full sync.
- Новые зависимые слоты появляются только после серверного подтверждения состояния.
- Если слот исчезает из-за снятия экипировки, применяется существующая транзакционная политика переноса/rollback (`InventoryTransactionService`) без потери предметов.

## Acceptance Criteria

- `ExtendedInventoryScreen` отображает центральный силуэт и 8 базовых слотов экипировки вокруг него.
- В `creative` отображается полный ванильный инвентарь (`main 3x9` + `hotbar 1x9`).
- В `survival/adventure` из ванильных слотов доступен только `hotbar 1x9`; дополнительные слоты даются экипировкой и корректно появляются/исчезают после server sync.
- Крафт-панель справа показывает категории и рецепты с корректной доступностью, затемнением и `tooltip`.
- Кнопка `Создать` корректно блокируется при нехватке ресурсов и активируется при полном наборе материалов.
- Нет client-server рассинхрона и packet flood при сериях кликов/крафта.

## Проверочные шаги

1. Запустить M8 unit-тесты:
   - `./gradlew test --tests "*InventoryScreenRouterTest"`
   - `./gradlew test --tests "*VanillaInventoryGridModelTest"`
   - `./gradlew test --tests "*C2SInventoryActionPacketTest"`
   - `./gradlew test --tests "*CraftRecipeServiceTest"`
   - `./gradlew test --tests "*C2SCraftCreateIntentPacketTest"`
   - `./gradlew test --tests "*ClientCraftPanelStateTest"`
2. В режиме `creative` открыть инвентарь (`E`) и проверить отображение полного ванильного инвентаря.
3. Переключиться в `survival` или `adventure` и открыть инвентарь (`E`):
   - должен отображаться модовый экран,
   - из ванильных слотов доступен только hotbar,
   - `main 3x9` должен отображаться как заблокированный.
4. Надеть/снять жилет или рюкзак и убедиться, что связанные дополнительные слоты появляются/исчезают после sync.
5. Проверить крафт-панель справа:
   - переключение категорий,
   - подсветку доступных и затемнение недоступных рецептов,
   - `tooltip` с названием, требованиями и текущими ресурсами,
   - активность/неактивность кнопки `Создать`.
6. Проверить craft create vertical slice:
   - подготовить ингредиенты (`leather x4`, `string x2`) в доступных слотах,
   - нажать `Создать` на правой панели,
   - убедиться, что операция проходит через сервер (результат появляется после sync, а UI показывает статус);
   - при нехватке ингредиентов/места убедиться, что приходит server-side отказ без локальной мутации.
7. Выполнить серию быстрых кликов по слотам и крафту; убедиться в отсутствии потери предметов и зависаний состояния.

## Риски M8+

- Перегрузка экрана при большом количестве дополнительных слотов.
- UX-регрессии относительно ожидаемого ванильного флоу.
- Ошибки доступности рецептов при устаревшем клиентском snapshot.
- Избыточные full sync при частой смене экипировки.

## Статус итерации

- Статус (2026-03-12): `completed — GUI rebuild`.

## Связанный layout sketch

- Детальная схема зон/якорей/псевдокоординат: `docs/M8_UI_LAYOUT_SKETCH.md`.

---

## Changelog M8+ GUI Rebuild (2026-03-12)

### Исправленные проблемы

1. **Наслоение слотов** — полностью переработан layout. Все зоны разведены по непересекающимся координатам:
   - Левая экип-колонка: x=8, y=8..98 (HEAD/FACE/UPPER/VEST/PANTS/BOOTS, шаг 18px)
   - Силуэт игрока: x=36..94, y=8..116
   - Правая экип-колонка: x=160, y=8..26 (BACKPACK/GLOVES)
   - Dynamic slots: x=8, y=120, макс. 9 колонок
   - Hotbar: x=8, y=196
   - Craft panel: x=182..250, y=8..196

2. **Hotbar не был связан с ванильным инвентарём** — `ExtendedInventoryMenu` теперь регистрирует Forge `Slot`-объекты для hotbar (0–8) и, в Creative, main-инвентаря (9–35). Drag-and-drop и Shift-клик работают через стандартный Forge-механизм.

3. **В survival отображался ванильный main-инвентарь** — в `survival/adventure` main-слоты (9–35) НЕ добавляются в меню. Из ванильных слотов доступен только hotbar. Дополнительные слоты добавляются исключительно через server-sync при надевании экипировки.

4. **Предметы не отображались в слотах** — слоты экипировки и dynamic-слоты теперь рендерятся из `ClientInventorySyncState.slots()` с правильными абсолютными координатами. Vanilla-слоты рендерятся Forge автоматически через `AbstractContainerScreen.renderSlot`.

5. **Силуэт персонажа** — добавлен живой рендер через `InventoryScreen.renderEntityInInventoryFollowsMouse` с правильными параметрами для MC 1.20.1.

### Новые файлы

- `InventoryLayoutConstants.java` — единый источник координат и цветов GUI.
- `InventoryLayoutConstantsTest.java` — 11 тестов на отсутствие перекрытий зон.
- `ExtendedInventoryLayoutTest.java` — 9 тестов hit-test и layout.

### Обновлённые файлы

- `ExtendedInventoryMenu.java` — Forge-слоты для hotbar + creative main.
- `ExtendedInventoryScreen.java` — полный rebuild layout, рендер, click-handling.
- `VanillaInventoryGridModel.java` — использует константы из `InventoryLayoutConstants`.
- `en_us.json` — новые translation keys (`survival_hint`, `craft_title`, `inv.group.base`).

### Проверочные шаги

```bash
./gradlew test --tests "*.InventoryLayoutConstantsTest"
./gradlew test --tests "*.ExtendedInventoryLayoutTest"
./gradlew test --tests "*.VanillaInventoryGridModelTest"
```

Все 24 теста: **PASS**.

### Оставшиеся риски / следующий этап

- При большом числе dynamic-слотов (>4 строки) они начнут перекрывать hotbar — необходима scroll-зона или pagination.
- Крафт-панель — UI-заглушка; полная интеграция категорий рецептов и tooltips в следующей итерации.
- `quickMove` в survival возвращает `EMPTY` — quick-move из vanilla-hotbar в mod-слоты обрабатывается server-side через `C2SInventoryActionPacket.quickMove`.
