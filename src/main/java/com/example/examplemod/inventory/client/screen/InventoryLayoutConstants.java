package com.example.examplemod.inventory.client.screen;

/**
 * M8+ GUI layout constants — единственный источник координат для всего экрана.
 *
 * <p>Схема размещения зон (ширина 256, высота 220):
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────┐
 * │ [EQUIPMENT LEFT]  [PLAYER SILHOUETTE]  [EQUIPMENT RIGHT]        │ top=8
 * │  HEAD(8,8)          (88,8) 64x96       BACKPACK(176,8)          │
 * │  FACE(8,26)                             GLOVES(176,26)          │
 * │  UPPER(8,44)        ~~~~~~~~~~          (empty)                 │
 * │  VEST(8,62)                             (empty)                 │
 * │  PANTS(8,98)                            (empty)                 │
 * │  BOOTS(8,116)                           (empty)                 │
 * ├─────────────────────────────────────────────────────────────────┤
 * │ [DYNAMIC SLOTS — карманы/утилити от экипировки] top=140         │
 * │  max 3 cols × N rows, startX=8, spacing=18                      │
 * ├─────────────────────────────────────────────────────────────────┤
 * │ [HOTBAR 1×9]  startX=8, startY=196, spacing=18                  │
 * └─────────────────────────────────────────────────────────────────┘
 * Крафт-панель справа: x=164, y=8, w=88, h=176
 * </pre>
 *
 * <p>В режиме Creative main-инвентарь 3×9 рисуется между динамическими слотами
 * и хотбаром: startX=8, startY=140.
 */
public final class InventoryLayoutConstants {

    // ── Размер окна ────────────────────────────────────────────────────────
    /** Полная ширина GUI-окна в пикселях. */
    public static final int GUI_WIDTH  = 256;
    /** Полная высота GUI-окна в пикселях. */
    public static final int GUI_HEIGHT = 222;

    // ── Слот-геометрия ─────────────────────────────────────────────────────
    /** Визуальный размер содержимого слота (иконка предмета). */
    public static final int SLOT_INNER = 16;
    /** Шаг сетки слотов (рамка + 1 px зазор). */
    public static final int SLOT_STEP  = 18;

    // ── Левая колонка: слоты экипировки ────────────────────────────────────
    public static final int EQUIP_LEFT_X      = 8;
    /** HEAD */
    public static final int EQUIP_HEAD_Y      = 8;
    /** FACE (противогаз) */
    public static final int EQUIP_FACE_Y      = 26;
    /** UPPER (куртка/броня) */
    public static final int EQUIP_UPPER_Y     = 44;
    /** VEST (разгрузка) */
    public static final int EQUIP_VEST_Y      = 62;
    /** PANTS */
    public static final int EQUIP_PANTS_Y     = 80;
    /** BOOTS */
    public static final int EQUIP_BOOTS_Y     = 98;

    // ── Правая колонка: дополнительная экипировка ──────────────────────────
    public static final int EQUIP_RIGHT_X     = 160;
    /** BACKPACK */
    public static final int EQUIP_BACK_Y      = 8;
    /** GLOVES */
    public static final int EQUIP_GLOVES_Y    = 26;

    // ── Силуэт игрока ──────────────────────────────────────────────────────
    public static final int PLAYER_X          = 36;
    public static final int PLAYER_Y          = 8;
    public static final int PLAYER_W          = 58;  // вписывается между колонками экипировки
    public static final int PLAYER_H          = 108;
    /** X-центр для силуэта (используется рендером EntityRenderDispatcher). */
    public static final int PLAYER_CENTER_X   = PLAYER_X + PLAYER_W / 2;
    /** Y-основание персонажа (ноги). */
    public static final int PLAYER_BASE_Y     = PLAYER_Y + PLAYER_H;

    // ── Зона динамических слотов (карманы/утилити от экипировки) ───────────
    /** X старта сетки dynamic-слотов. */
    public static final int DYNAMIC_X         = 8;
    /** Y старта — чуть ниже персонажа и нижних экип-слотов. */
    public static final int DYNAMIC_Y         = 120;
    /** Максимальное число колонок в сетке dynamic-слотов. */
    public static final int DYNAMIC_COLS      = 9;

    // ── Ванильный main-инвентарь 3×9 (только creative) ────────────────────
    public static final int VANILLA_MAIN_X    = 8;
    public static final int VANILLA_MAIN_Y    = 120;

    // ── Хотбар 1×9 ────────────────────────────────────────────────────────
    public static final int HOTBAR_X          = 8;
    public static final int HOTBAR_Y          = 196;

    // ── Крафт-панель (справа) ──────────────────────────────────────────────
    public static final int CRAFT_X           = 182;
    public static final int CRAFT_Y           = 8;
    public static final int CRAFT_W           = 68;
    public static final int CRAFT_H           = 188;

    /** Y кнопки «Создать» относительно CRAFT_Y. */
    public static final int CRAFT_BTN_REL_Y   = 162;
    public static final int CRAFT_BTN_W       = 60;
    public static final int CRAFT_BTN_H       = 16;

    // ── Цвета (ARGB) ──────────────────────────────────────────────────────
    public static final int COLOR_BG_OUTER    = 0xEE111318;
    public static final int COLOR_BG_INNER    = 0xFF1E2230;
    public static final int COLOR_SLOT_BORDER = 0xFF4B5563;
    public static final int COLOR_SLOT_BG     = 0xFF111827;
    public static final int COLOR_SLOT_SEL    = 0xFFEAB308;
    public static final int COLOR_SLOT_SEL_BG = 0xFF3F2A00;
    public static final int COLOR_EQUIP_EMPTY = 0xFF2A3344; // якорь без слота
    public static final int COLOR_LOCKED_BG   = 0xAA1F2937;
    public static final int COLOR_LOCKED_FG   = 0xCC0B1220;

    private InventoryLayoutConstants() {}
}

