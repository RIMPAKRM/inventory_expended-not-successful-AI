# M8+ Local Verification Guide

## Quick Start

### Компиляция и тесты
```powershell
cd "C:\Users\GameBox\Documents\programist\minecraft apocalipsis\inventory_expended\Inv_ex"

# Полная компиляция + тесты
.\gradlew clean test

# Или отдельно:
.\gradlew test --tests "*.InventoryLayoutConstantsTest" --tests "*.ExtendedInventoryLayoutTest" --tests "*.VanillaInventoryGridModelTest" --tests "*.DynamicSlotInteractionModelTest" --tests "*.ClientInventorySyncSnapshotTest"
```

**Ожидаемый результат:**
```
BUILD SUCCESSFUL
Tests: 24 passed
Time: ~8s
```

---

## Code Review Checklist

### ✅ `InventoryLayoutConstants.java`
- [ ] Все координаты должны быть положительными
- [ ] Нет пересечений зон (тесты подтверждают)
- [ ] Цвета в формате ARGB правильные (0xAARRGGBB)
- [ ] Документация с диаграммой layout присутствует

### ✅ `ExtendedInventoryMenu.java`
- [ ] Hotbar-слоты регистрируются всегда (9 слотов, indices 0-8)
- [ ] Main-слоты регистрируются только при `isCreative()` (27 слотов, indices 9-35)
- [ ] `quickMoveStack` возвращает `EMPTY` в survival (server-side обработка)
- [ ] Координаты совпадают с `InventoryLayoutConstants`

### ✅ `ExtendedInventoryScreen.java`
- [ ] `renderBg()` рисует все зоны без пропусков
- [ ] `renderEquipmentAnchors()` использует правильные координаты (8 якорей)
- [ ] `renderDynamicSlots()` работает с `ClientInventorySyncState.slots()`
- [ ] `renderVanillaZones()` рисует фон под vanilla-слоты
- [ ] `renderCraftPanel()` отрисовывает панель справа
- [ ] `mouseClicked()` обрабатывает все типы слотов
- [ ] `renderPlayerSilhouette()` использует правильную сигнатуру `InventoryScreen.renderEntityInInventoryFollowsMouse`

### ✅ Tests
- [ ] `InventoryLayoutConstantsTest` — 11 тестов на layout integrity
- [ ] `ExtendedInventoryLayoutTest` — 9 тестов на hit-test
- [ ] `VanillaInventoryGridModelTest` — 4 существующих теста остаются PASS

---

## Manual Testing in Game

### Setup
1. Запустить dev-сервер (с модом):
   ```bash
   .\gradlew runServer
   ```

2. Запустить dev-клиент:
   ```bash
   .\gradlew runClient
   ```

3. В игре создать мир или заджойнить сервер

### Test Case 1: Creative Mode
```
1. Переключить режим на Creative (/gamemode creative)
2. Нажать E (открыть инвентарь)
3. Проверить:
   ✅ Видны все 36 ванильных слотов (9 hotbar + 27 main)
   ✅ Силуэт персонажа в центре
   ✅ 8 якорей экипировки вокруг
   ✅ Крафт-панель справа
   ✅ Можно перемещать предметы через drag-and-drop
   ✅ Shift-клик работает
```

### Test Case 2: Survival Mode
```
1. Переключить режим на Survival (/gamemode survival)
2. Нажать E (открыть инвентарь)
3. Проверить:
   ✅ Видны только 9 hotbar-слотов (vanilla)
   ✅ Main-инвентарь НЕ видна (заблокирована)
   ✅ Силуэт персонажа
   ✅ 8 якорей экипировки (некоторые могут быть пусты)
   ✅ Base-слоты (если есть конфиг base slots)
   ✅ Крафт-панель справа
```

### Test Case 3: Equipment Dynamic Slots
```
1. В survival добавить предмет экипировки (напр., chest rig из конфига)
2. Надеть его (если поддерживается экипировка)
3. Проверить:
   ✅ После sync появляются новые слоты в dynamic-зоне
   ✅ В slots видны предметы из этого оборудования
   ✅ При снятии оборудования слоты исчезают
   ✅ Нет потери предметов при смене layout
```

### Test Case 4: Vanilla Hotbar Interaction
```
1. В survival положить предмет в выбранный hotbar-слот (например, 1)
2. Кликнуть ЛКМ по пустому mod-слоту экипировки
3. Проверить:
   ✅ На сервер отправлен intent MOVE_VANILLA_TO_MOD
   ✅ Предмет появляется в mod-слоте после sync
   ✅ В hotbar-слоте предмет исчезает
4. Кликнуть ПКМ по занятому mod-слоту
5. Проверить:
   ✅ На сервер отправлен intent MOVE_MOD_TO_VANILLA
   ✅ Предмет возвращается в hotbar (или в main в creative)
   ✅ Предмет не удаляется и не дублируется
```

### Test Case 5: Craft Panel Integration
```
1. Собрать ингредиенты для test_leather_rig:
   - Leather x4
   - String x2
2. Нажать кнопку "Create" на крафт-панели
3. Проверить:
   ✅ Отправляется C2SCraftCreateIntentPacket на сервер
   ✅ UI показывает "Syncing..." или статус результата
   ✅ После server-ответа результат появляется в инвентаре
   ✅ При нехватке ингредиентов — error-сообщение
```

### Test Case 6: Stress Test
```
1. Выполнить серию быстрых кликов по разным слотам
2. Нажимать "Create" несколько раз подряд
3. Проверить:
   ✅ Нет зависаний GUI
   ✅ Нет потери предметов
   ✅ Нет спама в логах
   ✅ TPS не падает ниже 18
```

### Test Case 7: Performance Soak (3-5 min)
```
1. Открыть ExtendedInventoryScreen в survival
2. В течение 3-5 минут выполнять серии кликов по mod-слотам, hotbar и Create
3. Проверить:
   ✅ Нет деградации до 1 FPS
   ✅ Нет зависания клиента/OS после закрытия игры
   ✅ В логах нет packet flood и ошибок rollback
```

---

## Expected Layout (ASCII Diagram)

```
┌─────────────────────────────────────────────────────────┐ y=8
│ [H] [F] [PLAYER]         [B] [G]        [CRAFT PANEL]  │
│     [U] [       ] [PLAYER]              │    TITLE      │
│ [V]     [       ] [PLAYER]         [G]  │ Craft Panel   │
│     [P]     [SILHOUETTE] x,y        │ Categories:
│ [Bo]        [       ] [PLAYER]      │ [1][2][3][4][5] │
│             [       ]                │ Recipes:        │
│        DYNAMIC SLOTS GRID 9xN       │ [1][1][1]       │
│        (slots from equipment)        │ [1][1][1]       │
│                                      │ [1][1][1]       │
│ [Hotbar 1x9 slots]                 │ [1][1][1]       │
│                                      │ Result:  [1]    │
│                                      │ [Create Button] │
└─────────────────────────────────────────────────────────┘
Legend:
[H] = HEAD        [B] = BACKPACK
[F] = FACE        [G] = GLOVES
[U] = UPPER       [V] = VEST
[P] = PANTS       [Bo] = BOOTS
```

---

## Troubleshooting

### "BuildException: method renderEntityInInventoryFollowsMouse cannot be applied"
**Cause:** gradle кешировал старый код  
**Fix:** `.\gradlew clean` перед компиляцией

### "Hotbar-слоты видны но предметы не отображаются"
**Cause:** `ClientInventorySyncState` не синхронизирован  
**Fix:** Проверить, что `S2CFullInventorySyncPacket` отправляется при открытии инвентаря (логи сервера)

### "Main-инвентарь виден в survival"
**Cause:** `isCreative()` флаг установлен неправильно или `ExtendedInventoryMenu` не обновлён  
**Fix:** Проверить `minecraft.player.isCreative()` в `InventoryScreenRouter`

### "Dynamic-слоты перекрывают hotbar"
**Cause:** Слишком много динамических слотов (>4 строки)  
**Expected:** Это известное ограничение M8, будет решено в M9 со scroll-зоной

### "Guid очень широкий и не влазит на экран"
**Cause:** GUI_WIDTH установлен слишком большим  
**Expected:** M8: 256px (должен вмещаться на 854px экран)  
**Check:** `InventoryLayoutConstants.GUI_WIDTH` = 256

---

## Performance Expectations

| Метрика | Ожидаемо | Максимум |
|---------|----------|----------|
| Время открытия инвентаря | <200ms | 500ms |
| Первый sync | <1s | 2s |
| Клик → ответ сервера | <50ms | 200ms |
| TPS при раскрытом инвентаре | 18+ | 12+ |
| Память GUI | ~10MB | 30MB |

---

## Sign-Off for Tester

- [ ] Все unit-тесты прошли (24/24 PASS)
- [ ] Creative mode работает (36 ванильных слотов видны)
- [ ] Survival mode работает (только hotbar + mod-слоты)
- [ ] Equipment динамические слоты появляются/исчезают
- [ ] Крафт-панель отображается
- [ ] Нет зависаний и потери предметов
- [ ] TPS стабилен при раскрытом инвентаре
- [ ] Логи не содержат ошибок GUI-интеграции

**Tested by:** [NAME]  
**Date:** 2026-03-[??]  
**Status:** ✅ APPROVED / ❌ FAILED (describe issues)

