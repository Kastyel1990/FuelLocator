# Крым-Топливо: Android-приложение мониторинга наличия топлива на АЗС Крыма

## Контекст

Водителям в Крыму сложно найти заправку с нужным видом топлива — информация разрозненна по нескольким сайтам (toplivo.rk.gov.ru, td-tes.com, fuel-status.atan.ru), данные не всегда актуальны. Приложение решает эту проблему, объединяя все АЗС на одной карте с возможностью краудсорсингового обновления наличия топлива.

---

## Технологический стек

| Компонент | Технология |
|-----------|-----------|
| Язык | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Карты | osmdroid (OpenStreetMap) |
| Бэкенд | Firebase Firestore (real-time sync) |
| Авторизация | Firebase Auth (email/телефон) |
| Архитектура | MVVM + Clean Architecture |
| DI | Hilt |
| Навигация | Jetpack Navigation Compose |
| Локальный кэш | Room (SQLite) для оффлайн-режима |
| Min SDK | 24 (Android 7.0) |

---

## Структура проекта

```
com.crimeafuel.app/
├── data/
│   ├── local/
│   │   ├── dao/          # Room DAO
│   │   ├── entity/       # Room Entity
│   │   └── CrimeFuelDatabase.kt
│   ├── remote/
│   │   ├── FirestoreDataSource.kt
│   │   └── dto/          # Data Transfer Objects
│   ├── repository/
│   │   ├── StationRepositoryImpl.kt
│   │   └── AuthRepositoryImpl.kt
│   └── seed/
│       └── InitialStationsData.kt  # 65 АЗС ТЭС + другие
├── domain/
│   ├── model/
│   │   ├── Station.kt
│   │   ├── FuelType.kt
│   │   ├── FuelStatus.kt
│   │   ├── PaymentMethod.kt
│   │   └── Region.kt
│   ├── repository/
│   │   ├── StationRepository.kt
│   │   └── AuthRepository.kt
│   └── usecase/
│       ├── GetStationsUseCase.kt
│       ├── FilterStationsUseCase.kt
│       ├── UpdateFuelStatusUseCase.kt
│       └── GetNearestStationUseCase.kt
├── presentation/
│   ├── map/
│   │   ├── MapScreen.kt           # Главный экран с картой
│   │   ├── MapViewModel.kt
│   │   ├── StationMarker.kt       # Кастомный маркер
│   │   └── StationInfoSheet.kt    # Bottom sheet с инфо
│   ├── filter/
│   │   ├── FilterPanel.kt         # Панель фильтров
│   │   └── FilterViewModel.kt
│   ├── edit/
│   │   ├── EditFuelScreen.kt      # Экран редактирования
│   │   └── EditFuelViewModel.kt
│   ├── auth/
│   │   ├── LoginScreen.kt
│   │   └── AuthViewModel.kt
│   ├── components/
│   │   ├── FuelChip.kt
│   │   ├── RegionSelector.kt
│   │   └── PaymentMethodChip.kt
│   └── theme/
│       ├── Theme.kt
│       ├── Color.kt
│       └── Type.kt
├── di/
│   ├── AppModule.kt
│   ├── DatabaseModule.kt
│   └── FirebaseModule.kt
├── util/
│   ├── LocationHelper.kt
│   └── DateUtils.kt
└── CrimeFuelApp.kt
```

---

## Модели данных

### Station (АЗС)
```kotlin
data class Station(
    val id: String,                    // Уникальный ID (например "tes_52")
    val number: String,                // Номер АЗС в сети (например "52")
    val network: String,               // Сеть: "ТЭС", "АТАН", "Лукойл", etc.
    val address: String,               // Полный адрес
    val region: Region,                // Район/город
    val latitude: Double,              // Координаты
    val longitude: Double,
    val fuelStatuses: List<FuelStatus>, // Наличие по каждому виду топлива
    val lastUpdated: Timestamp,        // Когда обновлено
    val lastUpdatedBy: String?,        // Кем обновлено (user ID)
    val isVerified: Boolean            // Из официального источника?
)
```

### FuelType (Вид топлива)
```kotlin
enum class FuelType(val displayName: String, val shortName: String) {
    AI_92("АИ-92", "92"),
    AI_92_PLUS("АИ-92+", "92+"),
    AI_95("АИ-95", "95"),
    AI_95_PLUS("АИ-95+", "95+"),
    AI_100("АИ-100", "100"),
    DT("Дизельное топливо", "ДТ"),
    DT_PLUS("ДТ Премиум", "ДТ+"),
    PROPANE("Пропан (LPG)", "Пропан"),
    METHANE("Метан (CNG)", "Метан")
}
```

### FuelStatus (Статус наличия)
```kotlin
data class FuelStatus(
    val fuelType: FuelType,
    val availability: Availability
)

enum class Availability(val displayName: String, val emoji: String) {
    FREE_SALE("Свободная продажа", "✅"),     // СП - есть для всех
    CARDS_ONLY("Талоны/Топл. карты", "🟡"),   // ТК - только по картам
    NOT_AVAILABLE("Нет в наличии", "❌"),       // Нет
    UNKNOWN("Нет данных", "❓")                // Неизвестно
}
```

### PaymentMethod (Вид отпуска)
```kotlin
enum class PaymentMethod(val displayName: String) {
    CARDS("Талоны / Топливные карты"),
    CASHLESS("Безнал"),
    CASH("Наличка")
}
```

### Region (Район/город)
```kotlin
enum class Region(val displayName: String) {
    SIMFEROPOL("г. Симферополь"),
    SIMFEROPOL_DISTRICT("Симферопольский район"),
    SEVASTOPOL("г. Севастополь"),
    YALTA("г. Ялта"),
    YALTA_DISTRICT("г.о. Ялта"),
    ALUSHTA("г. Алушта"),
    ALUSHTA_DISTRICT("г.о. Алушта"),
    EVPATORIA("г. Евпатория"),
    KERCH("г. Керчь"),
    FEODOSIA("г. Феодосия"),
    DZHANKOY("г. Джанкой"),
    BAKHCHISARAY("г. Бахчисарай"),
    BAKHCHISARAY_DISTRICT("Бахчисарайский район"),
    BELOGORSK_DISTRICT("Белогорский район"),
    KIROVSKY_DISTRICT("Кировский район"),
    KRASNOGVARDEYSKY_DISTRICT("Красногвардейский район"),
    LENINSKY_DISTRICT("Ленинский район"),
    NIZHNEGORSKY_DISTRICT("Нижнегорский район"),
    RAZDOLNENSKY_DISTRICT("Раздольненский район"),
    SAKSKY_DISTRICT("Сакский район"),
    SAKI("г. Саки"),
    CHERNOMORSKY_DISTRICT("Черноморский район"),
    ARMYANSK("г. Армянск"),
    KRASNOPEREKOPSK("г. Красноперекопск"),
    SUDAK("г. Судак"),
    // ... остальные
}
```

---

## Firestore — Структура коллекций

```
stations/                          # Коллекция АЗС
  ├── {stationId}/                 # Документ: одна АЗС
  │     ├── id: string
  │     ├── number: string
  │     ├── network: string
  │     ├── address: string
  │     ├── region: string
  │     ├── lat: number
  │     ├── lng: number
  │     ├── paymentMethods: string[]
  │     ├── fuelStatuses: map
  │     │     ├── AI_92: "FREE_SALE" | "CARDS_ONLY" | "NOT_AVAILABLE" | "UNKNOWN"
  │     │     ├── AI_95: ...
  │     │     ├── DT: ...
  │     │     └── ...
  │     ├── lastUpdated: timestamp
  │     ├── lastUpdatedBy: string
  │     └── isVerified: boolean

updates/                           # История обновлений
  ├── {updateId}/
  │     ├── stationId: string
  │     ├── userId: string
  │     ├── timestamp: timestamp
  │     ├── previousStatus: map
  │     └── newStatus: map
```

---

## Экраны и UI Flow

### 1. Главный экран (MapScreen)
```
┌──────────────────────────────────────┐
│  🔍 Поиск АЗС...            [👤]   │  ← Поиск + профиль
├──────────────────────────────────────┤
│  [92] [95] [ДТ] [Газ] [Все]         │  ← Фильтр топлива (chips)
│  [📍Район▼] [💳Отпуск▼]             │  ← Фильтр район + отпуск
├──────────────────────────────────────┤
│                                      │
│         ┌─ Маркер АЗС              │
│    🗺️   │  Цвет = наличие          │
│  КАРТА  │  ✅ зелёный = есть       │
│  КРЫМА  │  🟡 жёлтый = по картам   │
│         │  ❌ красный = нет         │
│         │  ❓ серый = нет данных    │
│    📍 ← Мое местоположение          │
│                                      │
│                         [◎]          │  ← Кнопка "мое место"
├──────────────────────────────────────┤
│  ⬆ Bottom Sheet (при нажатии):      │
│  ┌──────────────────────────────────┐│
│  │ ТЭС №52                    [✏️] ││  ← Название + кнопка "Ред."
│  │ с. Вилино, ул. Кулиняка, 18     ││
│  │ Обновлено: 2 часа назад         ││
│  │──────────────────────────────────││
│  │ 92  🟡 Талоны   │ 95  🟡 Талоны ││
│  │ 95+ 🟡 Талоны   │ ДТ  🟡 Талоны ││
│  │ LPG 🟡 Талоны   │ 100 ❌ Нет    ││
│  │──────────────────────────────────││
│  │ 💳 Талоны/карты  💵 Наличка     ││
│  │ 📍 2.3 км от вас                ││
│  │ [🧭 Маршрут]  [✏️ Обновить]     ││
│  └──────────────────────────────────┘│
└──────────────────────────────────────┘
```

### 2. Экран редактирования (EditFuelScreen)
```
┌──────────────────────────────────────┐
│  ← Назад    Обновить наличие        │
├──────────────────────────────────────┤
│  ТЭС №52                            │
│  с. Вилино, ул. Кулиняка, 18        │
├──────────────────────────────────────┤
│                                      │
│  Выберите наличие для каждого вида:  │
│                                      │
│  АИ-92   [✅ Есть] [🟡 ТК] [❌ Нет]│
│  АИ-92+  [✅ Есть] [🟡 ТК] [❌ Нет]│
│  АИ-95   [✅ Есть] [🟡 ТК] [❌ Нет]│
│  АИ-95+  [✅ Есть] [🟡 ТК] [❌ Нет]│
│  АИ-100  [✅ Есть] [🟡 ТК] [❌ Нет]│
│  ДТ      [✅ Есть] [🟡 ТК] [❌ Нет]│
│  ДТ+     [✅ Есть] [🟡 ТК] [❌ Нет]│
│  Пропан  [✅ Есть] [🟡 ТК] [❌ Нет]│
│  Метан   [✅ Есть] [🟡 ТК] [❌ Нет]│
│                                      │
│  Виды отпуска:                       │
│  [x] Талоны/Топливные карты          │
│  [x] Безнал                          │
│  [ ] Наличка                         │
│                                      │
│  📝 Комментарий (необязательно):     │
│  ┌──────────────────────────────────┐│
│  │ Очередь ~30 минут               ││
│  └──────────────────────────────────┘│
│                                      │
│  [        💾 СОХРАНИТЬ              ]│
│                                      │
└──────────────────────────────────────┘
```

### 3. Экран авторизации (LoginScreen)
```
┌──────────────────────────────────────┐
│          Крым-Топливо                │
│          ⛽                          │
│                                      │
│  Войдите, чтобы обновлять данные     │
│                                      │
│  📧 Email                            │
│  ┌──────────────────────────────────┐│
│  │ user@example.com                 ││
│  └──────────────────────────────────┘│
│  🔑 Пароль                           │
│  ┌──────────────────────────────────┐│
│  │ ••••••••                         ││
│  └──────────────────────────────────┘│
│                                      │
│  [         🚀 ВОЙТИ                 ]│
│  [       📝 РЕГИСТРАЦИЯ             ]│
│                                      │
│  ────── или ──────                   │
│                                      │
│  [📱 Войти по номеру телефона       ]│
│                                      │
│  [Пропустить (только просмотр) →    ]│
│                                      │
└──────────────────────────────────────┘
```

---

## Начальные данные (Seed)

### Источник 1: ТЭС (td-tes.com) — 65 АЗС
Полные данные получены: номер, сеть, адрес, координаты, район, наличие по всем видам топлива. Будут вшиты в `InitialStationsData.kt` и загружены в Firestore при первом развёртывании.

### Источник 2: АТАН (fuel-status.atan.ru) — ~30 АЗС
Данные с сайта не удалось извлечь автоматически (динамическая загрузка через JS). **Для seed-данных**: вручную добавим известные АЗС АТАН с координатами из открытых источников (OpenStreetMap), наличие = UNKNOWN (❓).

### Источник 3: toplivo.rk.gov.ru — правительственный портал
Аналогично — динамическая загрузка. Данные частично совпадают с ТЭС. Дополнительные АЗС добавим из OSM.

### Источник 4: Остальные АЗС из OSM/2GIS
Добавим все оставшиеся АЗС Крыма из OpenStreetMap (по тегу `amenity=fuel` в пределах bounding box Крыма). Наличие = UNKNOWN (❓). Это обеспечит полноту покрытия.

**Итого в seed: ~120-150 АЗС** (65 ТЭС с реальными данными + остальные с ❓)

---

## Поэтапный план реализации

### Этап 1: Инициализация проекта
1. Создать Android-проект (Kotlin, Jetpack Compose, Gradle KTS)
2. Настроить зависимости (osmdroid, Firebase, Hilt, Room, Navigation)
3. Настроить Firebase проект (Firestore + Auth)
4. Создать структуру пакетов

### Этап 2: Domain-слой
5. Определить модели данных (Station, FuelType, FuelStatus, Region, PaymentMethod)
6. Определить интерфейсы репозиториев
7. Написать UseCase'ы (GetStations, FilterStations, UpdateFuelStatus, GetNearest)

### Этап 3: Data-слой
8. Настроить Room (Entity, DAO, Database) для оффлайн-кэша
9. Настроить Firebase Firestore data source
10. Реализовать репозитории (с кэшированием: Firestore → Room)
11. Подготовить seed-данные (65 АЗС ТЭС + заглушки для остальных)

### Этап 4: Presentation — Карта
12. Реализовать MapScreen с osmdroid (центр = Крым, zoom 8-15)
13. Отрисовать маркеры АЗС (цвет по наличию)
14. Реализовать определение геолокации пользователя
15. Добавить нажатие на маркер → Bottom Sheet с информацией

### Этап 5: Presentation — Фильтры
16. Реализовать FilterPanel (chips по видам топлива)
17. Добавить выбор региона (dropdown)
18. Добавить фильтр по виду отпуска (chips)
19. Связать фильтры с ViewModel → обновление маркеров

### Этап 6: Presentation — Редактирование
20. Реализовать LoginScreen (Firebase Auth email + телефон)
21. Реализовать EditFuelScreen
22. Связать с Firestore (сохранение + history)

### Этап 7: Финальная доработка
23. Тёмная тема
24. Оффлайн-режим (Room кэш)
25. Кнопка "Построить маршрут" (intent → Яндекс.Навигатор или Google Maps)
26. Индикатор "давности" данных (2 часа назад, 1 день назад)

---

## Верификация

1. **Сборка**: `./gradlew assembleDebug` — проект должен компилироваться без ошибок
2. **Seed-данные**: Проверить что все 65 АЗС ТЭС отображаются на карте в правильных местах
3. **Фильтры**: Выбрать "АИ-92" → показать только АЗС где 92 есть (СП или ТК)
4. **Редактирование**: Авторизоваться → нажать маркер → "Редактировать" → изменить наличие → сохранить → проверить что данные обновились
5. **Геолокация**: Проверить что метка пользователя отображается на карте
6. **Оффлайн**: Включить авиарежим → приложение должно показывать кэшированные данные
