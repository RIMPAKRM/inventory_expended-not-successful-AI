# M8 UI Layout Sketch - ExtendedInventoryScreen

## Цель документа

Зафиксировать практичную схему компоновки `ExtendedInventoryScreen` для M8+:
- центральный блок персонажа и экипировки,
- встроенная панель крафта справа,
- `creative`: полный ванильный инвентарь,
- `survival/adventure`: доступен только ванильный hotbar + модовые слоты от экипировки.

Документ задает UX-ориентиры и псевдокоординаты для реализации/ревью. Это не pixel-perfect арт-спецификация.

## Принципы M8 layout

- Server-authoritative: клиент рендерит состояние из sync snapshot и отправляет только intent-действия.
- Vanilla-first визуально: слоты и сетки выглядят как ванильные.
- Нулевая регрессия по режимам: в `creative` показывается полный vanilla inventory; в `survival/adventure` из ванильных слотов доступен только `hotbar 1x9`.
- Прогрессивное расширение: dynamic slots от экипировки добавляются рядом с основным инвентарем без перекрытия ключевых зон.

## Каркас экрана

Базовый контейнер:
- `imageWidth = 320`
- `imageHeight = 196`
- опорные координаты: `leftPos`, `topPos`

Зоны:
- `A`: центральный блок персонажа и слоты экипировки (слева/центр).
- `B`: зона dynamic slots (слева от vanilla main либо между A и vanilla main).
- `C`: vanilla main inventory `3x9` (доступен только в `creative`).
- `D`: vanilla hotbar `1x9` (всегда доступен).
- `E`: встроенная панель крафта справа.

## Псевдокоординаты (относительно leftPos/topPos)

### A. Центр - персонаж и экипировка

- `playerPreviewRect`: `x=56, y=24, w=64, h=96`
- Рекомендуемый якорь слотов экипировки (`16x16`, шаг `18`):
  - `HEAD`: `x=78, y=6`
  - `FACE`: `x=78, y=24`
  - `UPPER`: `x=78, y=42`
  - `VEST`: `x=60, y=42`
  - `BACKPACK`: `x=96, y=42`
  - `GLOVES`: `x=42, y=60`
  - `PANTS`: `x=78, y=78`
  - `BOOTS`: `x=78, y=96`

Примечание: `VEST/BACKPACK/GLOVES` размещены асимметрично специально, чтобы оставить читаемый силуэт центра.

### B. Dynamic slots рядом с main

- Базовый якорь: `x=14, y=24`
- Сетка: `columns=2..4` (динамически), `slotSpacing=18`, `slotSize=16`
- Максимальная высота зоны до начала vanilla main: `maxY = 102`
- Если слотов больше видимой высоты:
  - приоритет 1: увеличить число колонок до 4,
  - приоритет 2: показать paging/scroll индикатор (опционально в M8+, обязательно в M9 при overflow).

### C. Vanilla main (creative-only)

- `mainStartX=14`
- `mainStartY=124`
- Размер зоны при активном состоянии: `9x3`, `slotSpacing=18`, `slotSize=16`
- Правило доступности:
  - в `creative` блок `main 3x9` полностью доступен,
  - в `survival/adventure` блок `main 3x9` не доступен для взаимодействия,
  - переключение состояния выполняется только после server sync.

### D. Vanilla hotbar (без изменения поведения)

- `hotbarStartX=14`
- `hotbarStartY=178`
- Размер: `9x1`, `slotSpacing=18`, `slotSize=16`

### E. Craft panel справа

- `craftPanelRect`: `x=176, y=18, w=130, h=164`

Внутри панели:
- `categoryListRect`: `x=180, y=24, w=36, h=116`
- `recipeGridRect`: `x=220, y=24, w=82, h=116`
  - сетка рецептов: `3x4`, `slotSpacing=18`
- `resultPreviewRect`: `x=220, y=144, w=36, h=18`
- `createButtonRect`: `x=258, y=144, w=44, h=18`

## Визуальные состояния

- `recipe.available`: обычная яркость + акцентная рамка.
- `recipe.unavailable`: затемнение (overlay) + muted border.
- `recipe.hover`: `tooltip` с названием, требованиями, текущими ресурсами.
- `createButton.enabled`: активный стиль.
- `createButton.disabled`: приглушенный стиль, клик игнорируется.
- `layout.updating`: краткий индикатор синхронизации при `LAYOUT_CHANGED`.
- `main.locked`: состояние для `survival/adventure`, когда `main 3x9` отключен, а активен только hotbar.

## Приоритеты hit-test

Порядок обработки клика в M8:
1. Элементы правой крафт-панели (`category`, `recipe`, `createButton`).
2. Слоты экипировки вокруг силуэта.
3. Dynamic slots (`DynamicSlotInteractionModel.findSlotAt`).
4. Vanilla hit-test:
   - в `creative`: `main + hotbar` через `VanillaInventoryGridModel.findPlayerInventorySlot`,
   - в `survival/adventure`: только hotbar-слоты (`0..8`).
5. Остальная область экрана.

Правило: верхние UI-слои (крафт/кнопки) всегда перекрывают слот-сетки ниже по Z-order.

## Поведение при экипировке и reflow

- Надевание/снятие экипировки меняет layout на сервере.
- Клиент до sync не делает финальный reflow локально.
- После `LAYOUT_CHANGED` full sync:
  - обновляется порядок и количество dynamic slots,
  - invalid selection очищается,
  - hit-test переходит на новую геометрию.

## Адаптация под разрешение

- Минимальная целевая ширина UI-контейнера: `320px`.
- Если окно слишком узкое:
  - шаг 1: уменьшить горизонтальные отступы между зонами на `2..4px`,
  - шаг 2: снизить ширину category list до `30px`,
  - шаг 3: сократить видимую высоту `recipeGridRect` (с paging).
- Если окно широкое:
  - центрировать контейнер,
  - не растягивать slot-size (оставить vanilla `16x16`),
  - свободное место отдавать полям/декору, а не масштабу сеток.

## QA checklist (M8 UI)

1. `E` открывает новый экран без регрессии vanilla fallback через feature flag.
2. Все 8 слотов экипировки видимы и кликабельны без пересечений hit-box.
3. Dynamic slots появляются/исчезают после server sync при смене жилета/рюкзака.
4. Поведение по режимам:
   - в `creative` доступен полный ванильный инвентарь (`main 3x9` + `hotbar 1x9`),
   - в `survival/adventure` доступен только hotbar + модовые слоты от экипировки,
   - quick-move не ломает M7-контракт при переключении режимов.
5. Крафт-панель:
   - категории переключаются,
   - рецепты корректно показывают availability,
   - `tooltip` корректно показывает требования/ресурсы,
   - `Создать` активируется только при полном наборе материалов.
6. При серии быстрых кликов нет рассинхрона, packet flood или потери предметов.

## Связанные документы

- `docs/M8_ITERATION_NOTES.md`
- `docs/ROADMAP.md`
- `src/main/java/com/example/examplemod/inventory/client/screen/ExtendedInventoryScreen.java`
- `src/main/java/com/example/examplemod/inventory/client/screen/DynamicSlotInteractionModel.java`
- `src/main/java/com/example/examplemod/inventory/client/screen/VanillaInventoryGridModel.java`
