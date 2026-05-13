# Документация проекта TCD_RPKB

## Оглавление
1. [Архитектура проекта](#архитектура-проекта)
2. [Функциональные модули](#функциональные-модули)
   - [Модуль 1: Авторизация](#модуль-1-авторизация)
   - [Модуль 2: Главный экран (Список перемещений)](#модуль-2-главный-экран-список-перемещений)
   - [Модуль 3: Работа с документами перемещения и прихода](#модуль-3-работа-с-документами-перемещения-и-прихода)
   - [Модуль 4: Работа с сериями товаров и ЗНП](#модуль-4-работа-с-сериями-товаров-и-знп)
   - [Модуль 5: Настройки приложения](#модуль-5-настройки-приложения)
3. [Общая инфраструктура](#общая-инфраструктура)

---

## Архитектура проекта

Проект построен по принципам **Clean Architecture** и разделен на 3 основных слоя:

### 1. Domain Layer (Доменный слой)
**Расположение:** `domain/src/main/java/`

**Ответственность:**
- Бизнес-логика приложения
- Модели предметной области
- Интерфейсы репозиториев
- Use Cases (сценарии использования)

**Независимость:** Не зависит от Android Framework и внешних библиотек

### 2. Data Layer (Слой данных)
**Расположение:** `data/src/main/java/`

**Ответственность:**
- Реализация репозиториев
- Работа с сетью (API)
- Локальное хранилище данных
- Маппинг данных между DTO и Domain моделями

**Зависимости:** Зависит от Domain слоя

### 3. App Layer (Презентационный слой)
**Расположение:** `app/src/main/java/`

**Ответственность:**
- UI компоненты (Activities, Fragments)
- ViewModels
- Адаптеры для списков
- Android-специфичная логика

**Зависимости:** Зависит от Domain и Data слоев

---

## Функциональные модули


## Модуль 1: Авторизация

### Описание
Модуль отвечает за авторизацию пользователей в системе двумя способами:
1. **QR-авторизация** - сканирование QR-кода пользователя
2. **Ручная авторизация** - ввод логина и пароля

### Основные сценарии использования:
- Получение информации о пользователе по GUID из QR-кода
- Проверка существования пользователя по логину
- Проверка пароля пользователя
- Сохранение учетных данных
- Выход из системы

### Компоненты модуля

#### Domain Layer

**Models:**
- `User` - модель пользователя
  - `fullName` - полное имя
  - `role` - роль в системе
  - `userGuid` - уникальный идентификатор

- `Credentials` - учетные данные
  - `username` - логин
  - `password` - пароль

- `AuthenticationResult` - результат авторизации
  - `success` - флаг успешности
  - `errorMessage` - сообщение об ошибке
  - `user` - данные пользователя

- `UserInfoResponse` - информация о пользователе из QR
  - `name` - логин пользователя
  - `fullName` - полное имя
  - `userGuid` - GUID из QR-кода

**Use Cases:**
- `AuthenticateUserUseCase` - основная логика авторизации
- `GetUserInfoByGuidUseCase` - получение информации по QR-коду
- `IsUserAuthenticatedUseCase` - проверка авторизации
- `GetUserUseCase` - получение текущего пользователя

**Repository Interfaces:**
- `AuthRepository` - работа с авторизацией
- `UserRepository` - хранение данных пользователя
- `UserSettingsRepository` - настройки пользователя

#### Data Layer

**DTOs:**
- `UserInfoResponseDto` - DTO ответа сервера с информацией о пользователе

**Mappers:**
- `UserInfoMapper` - преобразование между DTO и Domain моделями

**Network - API Services:**
- `AuthApiService` - работа с QR (использует фиксированную авторизацию)
  - `getUserInfoByGuid()` - получение информации по GUID
  - `checkUserLogin()` - проверка существования логина

- `UserAuthApiService` - проверка пароля (использует пользовательские учетные данные)
  - `checkUserPassword()` - проверка пароля

**Network - Interceptors:**
- `AuthorizationInterceptor` - добавляет фиксированную авторизацию для системных запросов
- `UserAuthInterceptor` - добавляет пользовательскую авторизацию для проверки пароля

**Repository Implementations:**
- `AuthRepositoryImpl` - реализация авторизации через API
- `UserRepositoryImpl` - хранение пользователя в SharedPreferences
- `UserSettingsRepositoryImpl` - управление настройками через SharedPreferences

**Dependency Injection:**
- `AuthModule` - Hilt модуль для инъекции зависимостей авторизации

#### App Layer
- `LoginActivity` / `LoginFragment` - экран авторизации
- `QrScannerActivity` / `QrScannerFragment` - сканирование QR-кода
- `LoginViewModel` - логика презентационного слоя

### Поток данных (Авторизация по QR-коду):
1. Пользователь сканирует QR-код → получаем GUID
2. `GetUserInfoByGuidUseCase` → `AuthRepository` → `AuthApiService.getUserInfoByGuid()`
3. Получаем `UserInfoResponse` с данными пользователя
4. Пользователь вводит пароль
5. `AuthenticateUserUseCase` → проверка пароля через `UserAuthApiService`
6. При успехе: сохранение `User` и `Credentials`
7. Переход на главный экран

---

## Модуль 2: Главный экран (Список перемещений)

### 📋 Обзор модуля

**Назначение:** Главный экран приложения для управления списками перемещений товаров между складами. Обеспечивает полный цикл работы: от просмотра перемещений до изменения их статусов и перехода к детальной обработке.

**Ключевые возможности:**
- 🗂️ Просмотр перемещений по статусам
- 🔍 Многокритериальная фильтрация с сохранением настроек для каждого пользователя
- ✅ Множественный выбор и массовое изменение статусов
- 🔄 Работа в онлайн и оффлайн(нужна была на начальных этапах тестирования, сейчас практически не поддерживатеся) режимах
- 💾 Автоматическое сохранение состояния фильтров

---

### 🎯 Основные сценарии использования

#### 1. Просмотр списка перемещений
Пользователь видит перемещения, разделенные на три вкладки:
- **"Сформирован"** 
- **"Комплектуется"** 
- **"Подготовлен"** 

#### 2. Фильтрация перемещений
Доступные фильтры:
- **По складу-отправителю** (текстовый поиск)
- **По номеру перемещения** (текстовый поиск)
- **По складу-получателю** (текстовый поиск)
- **По комплектовщику** (текстовый поиск)
- **По приоритету** (выпадающий список: Все/Неотложный/Высокий/Средний/Низкий)
- **По ответственному за получение** (текстовый поиск)
- **По ЦПС** (checkbox: показывать только ЦПС перемещения)
- **По доступности** (checkbox: фильтровать по доступности для текущего пользователя)
- **По серии** (текстовый поиск, требует перезагрузки данных с сервера)
- **По номенклатуре** (текстовый поиск, требует перезагрузки данных с сервера)

**Особенности применения фильтров:**
- Фильтры применяются **не в реальном времени** при вводе текста
- Применяются при нажатии клавиши **Enter** в любом текстовом поле фильтра
- Применяются при нажатии кнопки **"Применить фильтры"**
- Фильтры по серии, номенклатуре и доступности требуют нового запроса к серверу для получения актуальных данных

#### 3. Работа с фильтрами по умолчанию
- **Сохранение фильтров по умолчанию** для текущего пользователя
- **Загрузка сохраненных фильтров** при входе пользователя
- **Сброс фильтров** к начальным значениям


#### 4. Множественный выбор и смена статусов
- **Выбор нескольких перемещений** через checkbox
- **Массовое изменение статусов:**
  - Из "Сформирован" → "Комплектуется" ("Отпр. в "Комплектуется"")
  - Из "Комплектуется" → "Сформирован" ("Отпр. в "Сформировано"")
  - Из "Комплектуется" → "Подготовлен" ("Отпр. в "Подготовлено"")


#### 5. Переход к детальному просмотру
- **Клик по элементу** → запрос на сервер о составе перемещения + проверка на наличие не пустого поля "УИДСтрокиТовары" → переход к детальному просмотру 
- **Обработка ошибок:** уведомление при пустом составе документа или если есть пустые поля "УИДСтрокиТовары"

#### 6. Фоновое обновление данных
- **Автоматическая проверка новых данных** при возврате на экран
- **Индикация наличия обновлений** (кнопка обновления)
- **Применение обновлений** при нажатии на кнопку

---

### 🏗️ Архитектура модуля

Модуль с разделением ответственности между 3 слоями: **Data**, **Domain**, **App**.

```
┌─────────────────────────────────────────────────────────────┐
│                     PRESENTATION LAYER                      │
│                  (UI - Android Specific)                    │
├─────────────────────────────────────────────────────────────┤
│  MoveListActivity (Main Container)                          │
│    ├─ ViewPager2 (Swipe между вкладками)                    │
│    ├─ ViewPagerAdapter (Управление фрагментами)             │
│    ├─ MoveListFragment x3 (по одному на статус)             │
│    │   └─ MoveListAdapter → RecyclerView                    │
│    ├─ DrawerLayout (Боковая панель фильтров)                │
│    └─ SelectionPanel (Панель множественного выбора)         │
│                           ↓↑                                │
│  MoveListViewModel (Управление состоянием и бизнес-логикой) │
│    ├─ LiveData для данных (original + filtered списки)      │
│    ├─ LiveData для UI событий (navigation, toast, dialogs)  │
│    ├─ Логика фильтрации (единые фильтры для всех вкладок)   │
│    ├─ Логика смены статусов (одиночная и массовая)          │
│    └─ Управление фоновым обновлением                        │
└─────────────────────────────────────────────────────────────┘
                              ↓↑
┌─────────────────────────────────────────────────────────────┐
│                      DOMAIN LAYER                           │
│                  (Pure Business Logic)                      │
├─────────────────────────────────────────────────────────────┤
│  Models:                                                    │
│    ├─ MoveResponse (список перемещений)                     │
│    └─ MoveItem (элемент перемещения)                        │
│                                                             │
│  Use Cases:                                                 │
│    ├─ ChangeMoveStatusUseCase                               │
│    ├─ GetOnlineModeUseCase                                  │
│    └─ GetUserUseCase                                        │
│                                                             │
│  Repository Interfaces:                                     │
│    └─ MoveRepository                                        │
└─────────────────────────────────────────────────────────────┘
                              ↓↑
┌─────────────────────────────────────────────────────────────┐
│                       DATA LAYER                            │
│              (Data Management & Mapping)                    │
├─────────────────────────────────────────────────────────────┤
│  Repository Implementation:                                 │
│    └─ MoveRepositoryImpl (выбор источника данных)           │
│                           ↓↑                                │
│  Data Sources:                                              │
│    ├─ RemoteMoveDataSource → MoveApiService → 1C Server     │
│    └─ LocalMoveDataSource → assets/moves.json               │
│                           ↓↑                                │
│  DTOs:                                                      │
│    ├─ MoveResponseDto                                       │
│    └─ MoveItemDto                                           │
│                           ↓↑                                │
│  Mappers:                                                   │
│    ├─ MoveResponseMapper (DTO ↔ Domain)                     │
│    └─ MoveItemMapper (DTO ↔ Domain)                         │
└─────────────────────────────────────────────────────────────┘
```

---

### 📦 Компоненты модуля

#### 🎨 Presentation Layer (App)

##### **1. MoveListActivity**
**Файл:** `app/src/main/java/com/step/tcd_rpkb/UI/movelist/activity/MoveListActivity.java`

- Главный контейнер для всего UI списка перемещений
- Управление ViewPager2 для переключения между вкладками
- Управление боковой панелью фильтров (DrawerLayout)
- Управление панелью множественного выбора
- Обработка UI событий от ViewModel

**Ключевые компоненты UI:**

1. **ViewPager2** (`viewPager`) - контейнер для свайпов между фрагментами
   ```java
   // Настройка ViewPager с тремя фрагментами
   viewPagerAdapter = new ViewPagerAdapter(this);
   viewPagerAdapter.addFragment(formirovanFragment);
   viewPagerAdapter.addFragment(komplektuetsaFragment);
   viewPagerAdapter.addFragment(podgotovlenFragment);
   viewPager.setAdapter(viewPagerAdapter);
   ```

2. **DrawerLayout** (`drawerLayout`) - боковая панель с фильтрами
   - Открывается кнопкой `navMenuButton`
   - Содержит все фильтры в `NavigationView`
   - Индикатор активных фильтров (`filterIndicator`)
   - Кнопка "Применить фильтры" (`btn_apply_filters`) - применяет текущие значения фильтров
   - Кнопка "Сохранить фильтры по умолчанию" (`btn_set_default_filters`)
   - Кнопка "Сбросить фильтры" (`btn_reset_filters`)

3. **Сегментированное управление вкладками** (Toolbar)
   ```java
   segmentFormirovano    // Кнопка вкладки "Сформирован"
   segmentKomplektuetsa  // Кнопка вкладки "Комплектуется"
   segmentPodgotovlen    // Кнопка вкладки "Подготовлен"
   ```
   - Синхронизированы с ViewPager
   - Визуальная индикация текущей вкладки (selected state)

4. **Панель множественного выбора** (`actionButtonsPanel`)
   ```java
   btnMoveToWork      // (Сформирован → Комплектуется)
   btnMoveFromWork    // (Комплектуется → Сформирован)
   btnFinishWork      // (Комплектуется → Подготовлен)
   btnCancelSelection // "Отмена" выбора
   selectedItemsCount // Счетчик выбранных элементов
   ```
   - Появляется при выборе элементов в RecyclerView с помощью checkbox


5. **Кнопка фонового обновления** (`refreshButton`)
   - `FloatingActionButton`
   - Появляется при наличии новых данных с сервера

6. **Карточка пользователя** (в NavigationView Header)
   ```java
   userFullNameTextView // ФИО пользователя
   userRoleTextView     // Должность пользователя
   userAvatarImageView  // Аватар с инициалами
   ```
   - Анимация появления при открытии панели

  

**Методы жизненного цикла:**

```java
onCreate() {
    // 1. Инициализация ViewModel
    moveListViewModel = new ViewModelProvider(this).get(MoveListViewModel.class);
    
    // 2. Инициализация UI компонентов
    initFilterComponents();
    setupNavigationView();
    setupSegmentedControl();
    
    // 3. Создание фрагментов для каждого из статусов
    formirovanFragment = MoveListFragment.newInstance(STATUS_FORMIROVAN);
    komplektuetsaFragment = MoveListFragment.newInstance(STATUS_KOMPLEKTUETSA);
    podgotovlenFragment = MoveListFragment.newInstance(STATUS_PODGOTOVLEN);
    
    // 4. Настройка ViewPager
    setupViewPager();
    
    // 5. Настройка панели выбора, которая появляется при выборе контейнеров для смены статуса
    setupSelectionPanel();
    
    // 6. Подписка на LiveData
    observeViewModel();
    
    // 7. Загрузка фильтров по умолчанию для текущего пользователя
    moveListViewModel.loadDefaultFiltersForCurrentUser();
    
    // 8. Загрузка данных
    moveListViewModel.loadMoveData();
}

onResume() {
    // Запуск фоновой проверки наличия новых данных
    moveListViewModel.startBackgroundDataCheck();
}

onPause() {
    // Остановка фоновой проверки
    moveListViewModel.stopBackgroundDataCheck();
}
```

**Подписка на LiveData:**

```java
observeViewModel() {
    // Индикатор загрузки
    moveListViewModel.isLoading.observe(this, isLoading -> {
        if (isLoading) loadingDialog.show();
        else loadingDialog.dismiss();
    });
    
    // Сообщения об ошибках
    moveListViewModel.errorMessage.observe(this, error -> {
        if (error != null) Toast.makeText(this, error, Toast.LENGTH_LONG).show();
    });
    
    // Событие навигации к ProductsActivity(внутрь самого перемещения)
    moveListViewModel.navigateToPrixodEvent.observe(this, event -> {
        String moveUuid = event.getContentIfNotHandled();
        if (moveUuid != null) {
            Intent intent = new Intent(this, ProductsActivity.class);
            intent.putExtra("moveUuid", moveUuid);
            startActivity(intent);
        }
    });
    
    // Событие показа диалога ошибки
    moveListViewModel.showErrorDialogEvent.observe(this, event -> {
        ErrorDialogData data = event.getContentIfNotHandled();
        if (data != null) showErrorDialog(data);
    });
    
    // Событие успешной смены статуса
    moveListViewModel.showSuccessStatusChangeEvent.observe(this, event -> {
        SuccessStatusChangeEvent data = event.getContentIfNotHandled();
        if (data != null) {
            Toast.makeText(this, data.message, Toast.LENGTH_SHORT).show();
        }
    });
    
    // Кнопка обновления данных (если есть новые данные с сервера)
    moveListViewModel.showRefreshButton.observe(this, show -> {
        if (show) {
            refreshButton.setVisibility(View.VISIBLE);
            // Анимация появления
            refreshButton.setScaleX(0f);
            refreshButton.setScaleY(0f);
            refreshButton.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .start();
        } else {
            refreshButton.setVisibility(View.GONE);
        }
    });
    
    // Индикатор активных фильтров
    moveListViewModel.isAnyFilterActiveLive.observe(this, isActive -> {
        filterIndicator.setVisibility(isActive ? View.VISIBLE : View.INVISIBLE);
    });
}
```

---

##### **2. MoveListFragment**
**Файл:** `app/src/main/java/com/step/tcd_rpkb/UI/movelist/fragments/MoveListFragment.java`

**Ответственность:**
- Отображение списка перемещений для одного статуса
- Управление RecyclerView с адаптером
- Обработка кликов по элементам
- Передача событий выбора в Activity

**Создание экземпляра:**
```java
public static MoveListFragment newInstance(String status) {
    MoveListFragment fragment = new MoveListFragment();
    Bundle args = new Bundle();
    args.putString("status", status);
    fragment.setArguments(args);
    return fragment;
}
```

**Настройка RecyclerView:**
```java
setupRecyclerView() {
    // LinearLayoutManager с оптимизацией
    LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
    layoutManager.setItemPrefetchEnabled(true) //предзагрузка ViewHolder’ов за пределами экрана для более плавной прокрутки.
    layoutManager.setInitialPrefetchItemCount(15); // количество предзагруженных контейнеров
    
    // Адаптер
    adapter = new MoveListAdapter(getContext());
    adapter.setSelectionChangeListener(this);
    adapter.setOnMoveItemClickListener(this);
    
    // RecyclerView с оптимизацией производительности
    recyclerView.setHasFixedSize(true);
    recyclerView.setItemViewCacheSize(30);
    recyclerView.setDrawingCacheEnabled(true);
    recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
    recyclerView.setLayoutManager(layoutManager);
    recyclerView.setItemAnimator(null); // Отключение анимации для производительности
    recyclerView.setAdapter(adapter);
}
```

**Подписка на данные:**
```java
observeViewModel() {
    if (STATUS_FORMIROVAN.equals(status)) {
        moveListViewModel.filteredFormirovanList.observe(getViewLifecycleOwner(), newList -> {
            updateAdapterData(newList);
        });
    } else if (STATUS_KOMPLEKTUETSA.equals(status)) {
        moveListViewModel.filteredKomplektuetsaList.observe(getViewLifecycleOwner(), newList -> {
            updateAdapterData(newList);
        });
    } else if (STATUS_PODGOTOVLEN.equals(status)) {
        moveListViewModel.filteredPodgotovlenList.observe(getViewLifecycleOwner(), newList -> {
            updateAdapterData(newList);
        });
    }
}

updateAdapterData(List<MoveItem> newList) {
    if (adapter == null) return;
    // Передаем копию
    adapter.submitList(newList != null ? new ArrayList<>(newList) : new ArrayList<>());
}
```

**Обработка кликов:**
```java
@Override
public void onItemClicked(MoveItem moveItem) {
    if (moveItem != null && moveListViewModel != null) {
        moveListViewModel.processMoveItemClick(moveItem);
    }
}

@Override
public void onSelectionChanged(int count) {
    if (getActivity() instanceof MoveListActivity) {
        ((MoveListActivity) getActivity()).updateSelectionPanel(count);
    }
}
```

---

##### **3. MoveListAdapter**
**Файл:** `app/src/main/java/com/step/tcd_rpkb/UI/movelist/adapters/MoveListAdapter.java`

**Ответственность:**
- Отображение элементов списка перемещений в RecyclerView
- Управление выбором элементов (checkbox)
- Визуализация приоритетов цветом (сбоку каждого контейнера)
- Эффективное обновление через DiffUtil

**Наследование:**
```java
public class MoveListAdapter extends ListAdapter<MoveItem, MoveListAdapter.MoveViewHolder>
```
- `ListAdapter` - встроенная поддержка DiffUtil для эффективных обновлений
- `MoveItem` - тип данных
- `MoveViewHolder` - ViewHolder

**Использование DiffUtil:**
```java
public MoveListAdapter(Context context) {
    super(new MoveDiffCallback());  // Автоматическое вычисление изменений
    setHasStableIds(true);  // Для оптимизации
}

@Override
public long getItemId(int position) {
    MoveItem item = getItem(position);
    return item != null ? item.hashCode() : RecyclerView.NO_ID;
}
```

**Форматирование даты:**
```java
// ThreadLocal для безопасности потоков
private static final ThreadLocal<SimpleDateFormat> inputDateFormat = 
    ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()));
private static final ThreadLocal<SimpleDateFormat> outputDateFormat = 
    ThreadLocal.withInitial(() -> new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()));

// Использование в onBindViewHolder
try {
    Date date = inputDateFormat.get().parse(moveItem.getDate());
    if (date != null) {
        holder.tvDate.setText(outputDateFormat.get().format(date));
    }
} catch (ParseException e) {
    holder.tvDate.setText(moveItem.getDate());
}
```

**Цвета приоритетов:**
```java
private static final SparseArray<Integer> priorityColors = new SparseArray<>(4);

static {
    priorityColors.put(0, Color.parseColor("#8B0000")); // Неотложный - темно-красный
    priorityColors.put(1, Color.parseColor("#FF6347")); // Высокий - томатный
    priorityColors.put(2, Color.YELLOW);                // Средний - желтый
    priorityColors.put(3, Color.GREEN);                 // Низкий - зеленый
}

setPriorityColor(MoveViewHolder holder, String priority) {
    if ("Неотложный".equals(priority)) {
        holder.priorityIndicator.setBackgroundColor(priorityColors.get(0));
    } else if ("Высокий".equals(priority)) {
        holder.priorityIndicator.setBackgroundColor(priorityColors.get(1));
    } else if ("Средний".equals(priority)) {
        holder.priorityIndicator.setBackgroundColor(priorityColors.get(2));
    } else {
        holder.priorityIndicator.setBackgroundColor(priorityColors.get(3));
    }
}
```

**Отображение статуса "Проведен":**
```java
setCompletedCheckbox(MoveViewHolder holder, boolean isCompleted, String priority) {
    holder.checkboxCompleted.setChecked(isCompleted);
    
    // Цвет фона по приоритету
    int priorityColor = getPriorityColor(priority);
    GradientDrawable backgroundDrawable = new GradientDrawable();
    backgroundDrawable.setShape(GradientDrawable.RECTANGLE);
    backgroundDrawable.setCornerRadius(4f);
    backgroundDrawable.setColor(priorityColor);
    holder.checkboxCompleted.setBackground(backgroundDrawable);
    
    // Если проведено - зеленая галочка
    if (isCompleted) {
        holder.checkboxCompleted.setText("✓");
        holder.checkboxCompleted.setTextColor(Color.parseColor("#277d1d")); // Ярко-зеленый
        holder.checkboxCompleted.setTextSize(22f);
        holder.checkboxCompleted.setTypeface(null, Typeface.BOLD);
        holder.checkboxCompleted.setShadowLayer(4f, 0f, 0f, Color.parseColor("#00FF00"));
    } else {
        holder.checkboxCompleted.setText("");
        holder.checkboxCompleted.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT);
    }
}
```

**Управление выбором:**
```java
private Set<String> selectedItems = new HashSet<>();

// В onBindViewHolder
holder.checkboxMove.setOnCheckedChangeListener(null);  // Отключаем слушатель
holder.checkboxMove.setChecked(selectedItems.contains(itemId));  // Устанавливаем состояние
holder.checkboxMove.setOnCheckedChangeListener((checkBox, isChecked) -> {
    if (isChecked) {
        selectedItems.add(itemId);
    } else {
        selectedItems.remove(itemId);
    }
    notifySelectionChangeListener();
});

public List<MoveItem> getSelectedItems() {
    List<MoveItem> selected = new ArrayList<>();
    List<MoveItem> currentList = getCurrentList();
    for (MoveItem item : currentList) {
        if (item != null && selectedItems.contains(item.getMovementId())) {
            selected.add(item);
        }
    }
    return selected;
}

public void clearSelection() {
    if (selectedItems.isEmpty()) return;
    
    List<Integer> positionsToUpdate = new ArrayList<>();
    List<MoveItem> currentList = getCurrentList();
    for (int i = 0; i < currentList.size(); i++) {
        MoveItem item = currentList.get(i);
        if (item != null && selectedItems.contains(item.getMovementId())) {
            positionsToUpdate.add(i);
        }
    }
    
    selectedItems.clear();
    for (int position : positionsToUpdate) {
        notifyItemChanged(position);
    }
    notifySelectionChangeListener();
}
```

**ViewHolder:**
```java
static class MoveViewHolder extends RecyclerView.ViewHolder {
    View priorityIndicator;       // Цветная полоса приоритета
    TextView tvDate;              // Дата перемещения
    TextView tvNumber;            // Номер документа
    TextView tvFromWarehouse;     // Склад отправитель
    TextView tvToWarehouse;       // Склад получатель
    TextView tvProduct;           // Наименование товара
    TextView tvPositionCount;     // Количество позиций
    TextView tvItemsCount;        // Количество штук
    TextView tvAssembler;         // Комплектовщик
    TextView tvResponsible;       // Ответственный
    CustomCheckBox checkboxMove;  // Checkbox выбора при смене статуса
    CheckBox checkboxCompleted;   // Индикатор "Проведен"
    
    public MoveViewHolder(@NonNull View itemView) {
        super(itemView);
        // Инициализация всех view из layout
    }
}
```

---

##### **4. MoveDiffCallback**
**Файл:** `app/src/main/java/com/step/tcd_rpkb/utils/MoveDiffCallback.java`

**Ответственность:**
- Эффективное вычисление изменений в списке перемещений
- Минимизация перерисовок RecyclerView

```java
public class MoveDiffCallback extends DiffUtil.ItemCallback<MoveItem> {
    
    @Override
    public boolean areItemsTheSame(@NonNull MoveItem oldItem, @NonNull MoveItem newItem) {
        // Проверяем, это тот же элемент (по GUID)
        if (oldItem.getMovementId() == null || newItem.getMovementId() == null) {
            return oldItem == newItem;
        }
        return oldItem.getMovementId().equals(newItem.getMovementId());
    }

    @Override
    public boolean areContentsTheSame(@NonNull MoveItem oldItem, @NonNull MoveItem newItem) {
        // Проверяем, изменилось ли содержимое
        // Сравниваем только поля, влияющие на отображение
        if (!compareStrings(oldItem.getDate(), newItem.getDate())) return false;
        if (!compareStrings(oldItem.getNumber(), newItem.getNumber())) return false;
        if (!compareStrings(oldItem.getSourceWarehouseName(), newItem.getSourceWarehouseName())) return false;
        if (!compareStrings(oldItem.getDestinationWarehouseName(), newItem.getDestinationWarehouseName())) return false;
        if (!compareStrings(oldItem.getProductName(), newItem.getProductName())) return false;
        if (!compareStrings(oldItem.getPriority(), newItem.getPriority())) return false;
        if (!compareStrings(oldItem.getSigningStatus(), newItem.getSigningStatus())) return false;
        if (oldItem.getPositionsCount() != newItem.getPositionsCount()) return false;
        if (Double.compare(oldItem.getItemsCount(), newItem.getItemsCount()) != 0) return false;
        if (oldItem.isCps() != newItem.isCps()) return false;
        if (oldItem.isCompleted() != newItem.isCompleted()) return false;
        
        return true;
    }
}
```

---

##### **5. MoveListViewModel**
**Файл:** `app/src/main/java/com/step/tcd_rpkb/UI/movelist/viewmodel/MoveListViewModel.java`

**Ответственность:**
- Центральная бизнес-логика модуля списка перемещений
- Управление состоянием UI через LiveData
- Загрузка и фильтрация данных
- Смена статусов перемещений (одиночная и массовая)
- Сохранение/загрузка фильтров по умолчанию
- Фоновое обновление данных

**Константы статусов:**
```java
public static final String STATUS_FORMIROVAN = "Сформирован";
public static final String STATUS_KOMPLEKTUETSA = "Комплектуется";
public static final String STATUS_PODGOTOVLEN = "Подготовлен";

public static final String PRIORITY_URGENT = "Неотложный";
public static final String PRIORITY_HIGH = "Высокий";
public static final String PRIORITY_MEDIUM = "Средний";
public static final String PRIORITY_LOW = "Низкий";
```

**LiveData для данных:**
```java
// Оригинальные (нефильтрованные) списки
private final MutableLiveData<List<MoveItem>> _originalFormirovanList = new MutableLiveData<>(new ArrayList<>());
public LiveData<List<MoveItem>> originalFormirovanList = _originalFormirovanList;

private final MutableLiveData<List<MoveItem>> _originalKomplektuetsaList = new MutableLiveData<>(new ArrayList<>());
public LiveData<List<MoveItem>> originalKomplektuetsaList = _originalKomplektuetsaList;

private final MutableLiveData<List<MoveItem>> _originalPodgotovlenList = new MutableLiveData<>(new ArrayList<>());
public LiveData<List<MoveItem>> originalPodgotovlenList = _originalPodgotovlenList;

// Отфильтрованные списки
private final MutableLiveData<List<MoveItem>> _filteredFormirovanList = new MutableLiveData<>(new ArrayList<>());
public LiveData<List<MoveItem>> filteredFormirovanList = _filteredFormirovanList;

private final MutableLiveData<List<MoveItem>> _filteredKomplektuetsaList = new MutableLiveData<>(new ArrayList<>());
public LiveData<List<MoveItem>> filteredKomplektuetsaList = _filteredKomplektuetsaList;

private final MutableLiveData<List<MoveItem>> _filteredPodgotovlenList = new MutableLiveData<>(new ArrayList<>());
public LiveData<List<MoveItem>> filteredPodgotovlenList = _filteredPodgotovlenList;
```

**LiveData для состояния фильтров (SavedStateHandle):**
```java
// Сохраняются автоматически при смерти процесса
public LiveData<String> senderFilter;
public LiveData<String> movementNumberFilter;
public LiveData<String> recipientFilter;
public LiveData<String> assemblerFilter;
public LiveData<String> priorityFilter;
public LiveData<String> receiverFilter;
public LiveData<String> seriesFilter;
public LiveData<String> nomenclatureFilter;
public LiveData<Boolean> cpsChecked;
public LiveData<Boolean> availabilityChecked;
public LiveData<Integer> currentTabPosition;
```

**LiveData для UI событий:**
```java
// Индикаторы состояния
private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
public LiveData<Boolean> isLoading = _isLoading;

private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
public LiveData<String> errorMessage = _errorMessage;

private final MutableLiveData<Boolean> _isAnyFilterActiveLive = new MutableLiveData<>(false);
public LiveData<Boolean> isAnyFilterActiveLive = _isAnyFilterActiveLive;

private final MutableLiveData<Boolean> _showRefreshButton = new MutableLiveData<>(false);
public LiveData<Boolean> showRefreshButton = _showRefreshButton;

// События навигации
private final MutableLiveData<SingleEvent<String>> _navigateToPrixodEvent = new MutableLiveData<>();
public LiveData<SingleEvent<String>> navigateToPrixodEvent = _navigateToPrixodEvent;

// События уведомлений
private final MutableLiveData<SingleEvent<String>> _showToastEvent = new MutableLiveData<>();
public LiveData<SingleEvent<String>> showToastEvent = _showToastEvent;

private final MutableLiveData<SingleEvent<ErrorDialogData>> _showErrorDialogEvent = new MutableLiveData<>();
public LiveData<SingleEvent<ErrorDialogData>> showErrorDialogEvent = _showErrorDialogEvent;

private final MutableLiveData<SingleEvent<SuccessStatusChangeEvent>> _showSuccessStatusChangeEvent = new MutableLiveData<>();
public LiveData<SingleEvent<SuccessStatusChangeEvent>> showSuccessStatusChangeEvent = _showSuccessStatusChangeEvent;

private final MutableLiveData<SingleEvent<String>> _showSnackbarEvent = new MutableLiveData<>();
public LiveData<SingleEvent<String>> showSnackbarEvent = _showSnackbarEvent;
```

**Зависимости (Hilt Injection):**
```java
@Inject
public MoveListViewModel(
    Application application,
    SavedStateHandle savedStateHandle,
    MoveRepository moveRepository,
    GetOnlineModeUseCase getOnlineModeUseCase,
    GetUserUseCase getUserUseCase,
    ChangeMoveStatusUseCase changeMoveStatusUseCase,
    @ApplicationContext Context appContext
) {
    super(application);
    this.savedStateHandle = savedStateHandle;
    this.moveRepository = moveRepository;
    this.getOnlineModeUseCase = getOnlineModeUseCase;
    this.getUserUseCase = getUserUseCase;
    this.changeMoveStatusUseCase = changeMoveStatusUseCase;
    this.defaultFiltersManager = new DefaultFiltersManager(appContext);
    this.productsDataManager = new ProductsDataManager(appContext);
    this.executorService = Executors.newSingleThreadExecutor();
    
    // Инициализация LiveData из SavedStateHandle
    currentTabPosition = savedStateHandle.getLiveData(KEY_CURRENT_TAB_POSITION, 0);
    senderFilter = savedStateHandle.getLiveData(KEY_SENDER, "");
    movementNumberFilter = savedStateHandle.getLiveData(KEY_MOVEMENT_NUMBER, "");
}
```

**Ключевые методы:**

```java
/**
 * Загружает данные о перемещениях с сервера или из локального источника
 */
public void loadMoveData() {
    _isLoading.setValue(true);
    _errorMessage.setValue(null);
    
    // Параметры запроса
    String status = STATUS_FORMIROVAN + "|" + STATUS_KOMPLEKTUETSA + "|" + STATUS_PODGOTOVLEN;
    String userGuid = getCurrentUserGuid();
    Boolean availabilityFilterValue = availabilityChecked.getValue();
    boolean useFilter = availabilityFilterValue != null ? availabilityFilterValue : true;
    
    moveRepository.getMoveList(status, null, null, userGuid, useFilter, new RepositoryCallback<MoveResponse>() {
        @Override
        public void onSuccess(MoveResponse response) {
            if (response != null && response.getItems() != null) {
                // Разделение по статусам
                List<MoveItem> formirovanList = new ArrayList<>();
                List<MoveItem> komplektuetsaList = new ArrayList<>();
                List<MoveItem> podgotovlenList = new ArrayList<>();
                
                for (MoveItem item : response.getItems()) {
                    if (STATUS_FORMIROVAN.equals(item.getSigningStatus())) {
                        formirovanList.add(item);
                    } else if (STATUS_KOMPLEKTUETSA.equals(item.getSigningStatus())) {
                        komplektuetsaList.add(item);
                    } else if (STATUS_PODGOTOVLEN.equals(item.getSigningStatus())) {
                        podgotovlenList.add(item);
                    }
                }
                
                // Сортировка по приоритету
                sortByPriority(formirovanList);
                sortByPriority(komplektuetsaList);
                sortByPriority(podgotovlenList);
                
                // Обновление данных
                _originalFormirovanList.setValue(formirovanList);
                _originalKomplektuetsaList.setValue(komplektuetsaList);
                _originalPodgotovlenList.setValue(podgotovlenList);
                
                // Применение фильтров
                triggerFilterRecalculation();
            }
            _isLoading.postValue(false);
        }
        
        @Override
        public void onError(Exception exception) {
            // Очистка списков при ошибке
            _originalFormirovanList.setValue(new ArrayList<>());
            _originalKomplektuetsaList.setValue(new ArrayList<>());
            _originalPodgotovlenList.setValue(new ArrayList<>());
            
            triggerFilterRecalculation();
            
            // Обработка типов ошибок
            if (exception instanceof ServerErrorWithTypeException) {
                ServerErrorWithTypeException serverError = (ServerErrorWithTypeException) exception;
                _showErrorDialogEvent.postValue(new SingleEvent<>(
                    new ErrorDialogData(serverError.getDialogTitle(), exception.getMessage())
                ));
            } else {
                _errorMessage.setValue("Ошибка загрузки списка перемещений: " + exception.getMessage());
            }
            _isLoading.setValue(false);
        }
    });
}

/**
 * Применяет фильтры ко всем спискам (единые фильтры для всех вкладок)
 */
private void triggerFilterRecalculation() {
    executorService.execute(() -> {
        // Фильтрация всех трех списков
        List<MoveItem> filteredFormirovan = applyFiltersToList(
            _originalFormirovanList.getValue(),
            senderFilter.getValue(),
            movementNumberFilter.getValue(),
            // ... остальные фильтры
        );
        _filteredFormirovanList.postValue(filteredFormirovan);
        
        // Аналогично для komplektuetsa и podgotovlen
        
        updateIsAnyFilterActive();
    });
}

/**
 * Применяет все фильтры к списку
 */
private List<MoveItem> applyFiltersToList(
    List<MoveItem> originalList,
    String senderFilter,
    String movementNumberFilter,
    String recipientFilter,
    String assemblerFilter,
    String priorityFilter,
    String receiverFilter,
    Boolean cpsFilterEnabled,
    Boolean availabilityFilterEnabled
) {
    List<MoveItem> filteredList = new ArrayList<>();
    if (originalList == null) return filteredList;
    
    for (MoveItem item : originalList) {
        boolean matches = true;
        
        // Фильтр по ЦПС
        if (Boolean.TRUE.equals(cpsFilterEnabled) && !item.isCps()) {
            matches = false;
        }
        
        // Фильтр по отправителю
        if (matches && senderFilter != null && !senderFilter.isEmpty()) {
            if (item.getSourceWarehouseName() == null || 
                !item.getSourceWarehouseName().toLowerCase().contains(senderFilter.toLowerCase())) {
                matches = false;
            }
        }
        
        // Фильтр по номеру перемещения
        if (matches && movementNumberFilter != null && !movementNumberFilter.isEmpty()) {
            if (item.getNumber() == null || 
                !item.getNumber().toLowerCase().contains(movementNumberFilter.toLowerCase())) {
                matches = false;
            }
        }
        
        // ... остальные фильтры аналогично
        
        if (matches) {
            filteredList.add(item);
        }
    }
    return filteredList;
}

/**
 * Обрабатывает клик по элементу перемещения
 * Проверяет наличие товаров перед навигацией
 */
public void processMoveItemClick(MoveItem moveItem) {
    String moveUuid = moveItem.getMovementId();
    
    // Устанавливаем индикатор обработки
    _isProcessingItemClick.setValue(true);
    
    // Загружаем документ для проверки наличия товаров
    moveRepository.getDocumentMove(moveUuid, new RepositoryCallback<Invoice>() {
        @Override
        public void onSuccess(Invoice invoice) {
            _isProcessingItemClick.postValue(false);
            
            if (invoice != null && invoice.getProducts() != null && !invoice.getProducts().isEmpty()) {
                // Проверка УИДСтрокиТовары
                List<Product> products = invoice.getProducts();
                boolean allProductsHaveLineId = true;
                
                for (Product product : products) {
                    if (product.getProductLineId() == null || product.getProductLineId().isEmpty()) {
                        allProductsHaveLineId = false;
                        break;
                    }
                }
                
                if (!allProductsHaveLineId) {
                    _showEmptyProductLineIdErrorEvent.postValue(
                        new SingleEvent<>("Ошибка: некоторые товары не имеют УИДСтрокиТовары")
                    );
                    return;
                }
                
                // Навигация к детальному просмотру
                _navigateToPrixodEvent.postValue(new SingleEvent<>(moveUuid));
            } else {
                _showEmptyMovementErrorEvent.postValue(
                    new SingleEvent<>("Перемещение " + moveItem.getNumber() + " не содержит товаров")
                );
            }
        }
        
        @Override
        public void onError(Exception exception) {
            _isProcessingItemClick.postValue(false);
            String errorMessage = "Ошибка загрузки документа: " + exception.getMessage();
            
            if (exception instanceof ServerErrorWithTypeException) {
                ServerErrorWithTypeException serverError = (ServerErrorWithTypeException) exception;
                _showErrorDialogEvent.postValue(new SingleEvent<>(
                    new ErrorDialogData(serverError.getDialogTitle(), errorMessage)
                ));
            } else {
                _showToastEvent.postValue(new SingleEvent<>(errorMessage));
            }
        }
    });
}

/**
 * Перемещает элементы между статусами (одиночное или массовое)
 */
public void moveItems(List<MoveItem> itemsToMove, String currentFragmentState, String targetState) {
    if (itemsToMove == null || itemsToMove.isEmpty()) {
        _showToastEvent.setValue(new SingleEvent<>("Нет элементов для перемещения."));
        return;
    }
    
    boolean isOnline = getOnlineModeUseCase.execute();
    
    if (isOnline) {
        if (itemsToMove.size() > 1) {
            // Массовая обработка - последовательная смена статуса для каждого элемента
            startMultiMoveProcess(itemsToMove, currentFragmentState, targetState);
        } else {
            // Одиночная обработка
            processSingleMove(itemsToMove.get(0), currentFragmentState, targetState);
        }
    } else {
        // Оффлайн режим - локальное перемещение (устаревший функционал)
        performLocalMove(itemsToMove, currentFragmentState, targetState);
    }
}

/**
 * Обработка одиночной смены статуса
 */
private void processSingleMove(MoveItem item, String currentState, String targetState) {
    String guid = item.getMovementId();
    String userGuid = getCurrentUserGuid();
    
    _isLoading.postValue(true);
    
    // Проверяем нужна ли синхронизация данных с 1С
    boolean needDataSync = STATUS_KOMPLEKTUETSA.equals(currentState) && 
                          STATUS_PODGOTOVLEN.equals(targetState);
    
    if (needDataSync) {
        // Загружаем продукты для сохранения
        List<Product> products = productsDataManager.loadProductsData(guid);
        
        // Смена статуса с сохранением данных
        changeMoveStatusUseCase.executeWithDataSave(guid, targetState, userGuid, products, 
            new RepositoryCallback<ChangeMoveStatusResult>() {
            @Override
            public void onSuccess(ChangeMoveStatusResult result) {
                if (result.isResult()) {
                    handleSuccessfulStatusChange(guid, item.getNumber(), currentState, targetState);
                } else {
                    _showErrorDialogEvent.postValue(new SingleEvent<>(
                        new ErrorDialogData("Ошибка", result.getErrorText())
                    ));
                }
                _isLoading.postValue(false);
            }
            
            @Override
            public void onError(Exception exception) {
                _showErrorDialogEvent.postValue(new SingleEvent<>(
                    new ErrorDialogData("Ошибка", exception.getMessage())
                ));
                _isLoading.postValue(false);
            }
        });
    } else {
        // Простая смена статуса без синхронизации
        changeMoveStatusUseCase.execute(guid, targetState, userGuid, 
            new RepositoryCallback<ChangeMoveStatusResult>() {
            @Override
            public void onSuccess(ChangeMoveStatusResult result) {
                if (result.isResult()) {
                    handleSuccessfulStatusChange(guid, item.getNumber(), currentState, targetState);
                } else {
                    _showErrorDialogEvent.postValue(new SingleEvent<>(
                        new ErrorDialogData("Ошибка", result.getErrorText())
                    ));
                }
                _isLoading.postValue(false);
            }
            
            @Override
            public void onError(Exception exception) {
                _showErrorDialogEvent.postValue(new SingleEvent<>(
                    new ErrorDialogData("Ошибка", exception.getMessage())
                ));
                _isLoading.postValue(false);
            }
        });
    }
}

/**
 * Обработка успешной смены статуса
 */
private void handleSuccessfulStatusChange(String guid, String moveNumber, String sourceState, String targetState) {
    // Удаляем из исходного списка
    removeItemFromList(guid, sourceState);
    
    // Перезагружаем целевой список с сервера
    reloadTargetList(targetState);
    
    // Удаляем сохраненные данные
    deleteAllDataForMovement(guid);
    
    // Показываем успешное сообщение
    String successMessage = "Статус для '" + moveNumber + "' успешно изменён на '" + targetState + "'.";
    _showSuccessStatusChangeEvent.postValue(new SingleEvent<>(
        new SuccessStatusChangeEvent(successMessage, targetState, guid)
    ));
}

/**
 * Работа с фильтрами по умолчанию
 */
public void loadDefaultFiltersForCurrentUser() {
    User currentUser = getUserUseCase.execute();
    if (currentUser != null && currentUser.getUserGuid() != null) {
        DefaultFiltersData filtersData = defaultFiltersManager.loadDefaultFiltersForUser(
            currentUser.getUserGuid()
        );
        
        if (filtersData != null) {
            // Применяем загруженные фильтры
            setSenderFilter(filtersData.getSender());
            setMovementNumberFilter(filtersData.getMovementNumber());
            setRecipientFilter(filtersData.getRecipient());
            setAssemblerFilter(filtersData.getAssembler());
            setPriorityFilter(filtersData.getPriority());
            setReceiverFilter(filtersData.getReceiver());
            setCpsChecked(filtersData.isCpsChecked());
            setAvailabilityChecked(filtersData.isAvailabilityChecked());
        }
    }
}

public void saveDefaultFiltersForCurrentUser() {
    User currentUser = getUserUseCase.execute();
    if (currentUser != null && currentUser.getUserGuid() != null) {
        DefaultFiltersData filtersData = new DefaultFiltersData(
            currentUser.getUserGuid(),
            currentUser.getFullName()
        );
        
        // Сохраняем текущие значения фильтров
        filtersData.setSender(senderFilter.getValue() != null ? senderFilter.getValue() : "");
        filtersData.setMovementNumber(movementNumberFilter.getValue() != null ? movementNumberFilter.getValue() : "");
        // ... остальные фильтры
        
        boolean success = defaultFiltersManager.saveDefaultFilters(
            currentUser.getUserGuid(),
            currentUser.getFullName(),
            filtersData
        );
        
        if (success) {
            _showDefaultFiltersMessageEvent.postValue(
                new SingleEvent<>("Фильтры по умолчанию сохранены")
            );
        } else {
            _showDefaultFiltersMessageEvent.postValue(
                new SingleEvent<>("Ошибка сохранения фильтров")
            );
        }
    }
}

/**
 * Фоновое обновление данных
 */
public void checkForDataUpdates(boolean showLoading) {
    // Запуск фоновой проверки новых данных
    // При наличии изменений - показ кнопки обновления
}
 // Если данные есть и нажата кнопка их обновления вызыватся метод
public void applyPendingUpdates() {
        _isLoading.setValue(true);
        boolean updated = false;
        if (hasFreshFormirovanData && freshFormirovanData != null) {
            _originalFormirovanList.setValue(new ArrayList<>(freshFormirovanData)); // создаем копию
            hasFreshFormirovanData = false;
            freshFormirovanData = null;
            updated = true;
        }
        if (hasFreshKomplektuetsaData && freshKomplektuetsaData != null) {
            _originalKomplektuetsaList.setValue(new ArrayList<>(freshKomplektuetsaData)); // создаем копию
            hasFreshKomplektuetsaData = false;
            freshKomplektuetsaData = null;
            updated = true;
        }
        if (hasFreshPodgotovlenData && freshPodgotovlenData != null) {
            _originalPodgotovlenList.setValue(new ArrayList<>(freshPodgotovlenData)); // создаем копию
            hasFreshPodgotovlenData = false;
            freshPodgotovlenData = null;
            updated = true;
        }

        if (updated) {
            triggerFilterRecalculation();
            _showToastEvent.postValue(new SingleEvent<>("Данные обновлены."));
        } else {
            _showToastEvent.postValue(new SingleEvent<>("Нет доступных обновлений."));
        }
        _showRefreshButton.setValue(false);
        _isLoading.setValue(false);
    }   
```

---

#### 🧩 Domain Layer

Модуль использует следующие компоненты из доменного слоя:

##### **Models:**

**`MoveResponse`** - ответ со списком перемещений

Поля:
- `errorText: String` - текст ошибки от сервера
- `result: boolean` - результат операции (true = успех)
- `state: String` - состояние фильтра (может содержать несколько статусов через "|")
- `startDate: String` - дата начала периода (формат YYYYMMDD)
- `endDate: String` - дата окончания периода (формат YYYYMMDD)
- `statusList: List<String>` - список доступных статусов
- `items: List<MoveItem>` - список элементов перемещений



---

**`MoveItem`** - элемент перемещения

Поля :
- `movementId: String` - UUID перемещения (уникальный идентификатор)
- `movementDisplayText: String` - представление перемещения для отображения кртауой информации
- `isCps: boolean` - флаг ЦПС 
- `date: String` - дата перемещения 
- `number: String` - номер документа перемещения
- `isCompleted: boolean` - проведен ли документ
- `comment: String` - комментарий к перемещению
- `productName: String` - НоменклатураНаименование
- `responsiblePersonName: String` - ответственный за получение
- `color: String` - цвет (не используется никак)
- `priority: String` - приоритет ("Неотложный", "Высокий", "Средний", "Низкий")
- `assemblerName: String` - комплектовщик
- `signingStatus: String` - статус подписания ("Сформирован", "Комплектуется", "Подготовлен")
- `sourceWarehouseName: String` - склад отправитель
- `destinationWarehouseName: String` - склад получатель
- `itemsCount: double` - количество штук
- `positionsCount: int` - количество позиций

---

##### **Use Cases:**

**`ChangeMoveStatusUseCase`**

Изменяет статус перемещения с опциональным сохранением данных в 1С.

Зависимости:
- `MoveRepository`
- `SaveMoveDataBeforeStatusChangeUseCase`

Методы:
```java
public void execute(String guid, String targetState, String userGuid, 
                   RepositoryCallback<ChangeMoveStatusResult> callback)

public void executeWithDataSave(String guid, String targetState, String userGuid, 
                               List<Product> products, 
                               RepositoryCallback<ChangeMoveStatusResult> callback)
```

**`GetOnlineModeUseCase`** и **`GetUserUseCase`**

Используются для получения настроек и текущего пользователя.

---

##### **Repository Interface:**

**`MoveRepository`**

Методы, используемые модулем:

```java
// Получение списка перемещений
void getMoveList(String state, String startDate, String endDate, 
                String userGuid, boolean useFilter, 
                RepositoryCallback<MoveResponse> callback);

// Получение детального документа
void getDocumentMove(String guid, RepositoryCallback<Invoice> callback);

// Смена статуса перемещения
void changeMoveStatus(String guid, String targetState, String userGuid, 
                     RepositoryCallback<ChangeMoveStatusResult> callback);

```

---

#### 💾 Data Layer

##### **DTOs:**

**`MoveResponseDto`**

Структура JSON ответа от сервера:
```json
{
  "ТекстОшибки": "",
  "Результат": true,
  "state": "Сформирован|Комплектуется|Подготовлен",
  "ДатаНачала": "20250101",
  "ДатаОкончания": "20250131",
  "МассивСостоянийСтрокой": ["Сформирован", "Комплектуется", "Подготовлен"],
  "Данные": [
    // массив MoveItemDto
  ]
}
```

Поля с @SerializedName:
```java
@SerializedName("ТекстОшибки") private String errorText;
@SerializedName("Результат") private boolean result;
@SerializedName("state") private String state;
@SerializedName("ДатаНачала") private String startDate;
@SerializedName("ДатаОкончания") private String endDate;
@SerializedName("МассивСостоянийСтрокой") private List<String> statusList;
@SerializedName("Данные") private List<MoveItemDto> items;
```

---

**`MoveItemDto`**

Структура JSON элемента перемещения:
```json
{
  "ПеремещениеГУИД": "2d7f7704-f268-11ee-bba5-001dd8b71c23",
  "ПеремещениеПредставление": "Перемещение 00000001234 от 24.01.2025",
  "ЦПС": true,
  "Дата": "2025-01-24T10:30:00",
  "Номер": "00000001234",
  "Проведен": false,
  "Комментарий": "Срочное перемещение",
  "НоменклатураНаименование": "Товар А",
  "ОтветственныйЗаПолучениеНаименование": "Иванов И.И.",
  "Цвет": "#FF0000",
  "Приоритет": "Неотложный",
  "КомплектовщикНаименование": "Петров П.П.",
  "СтатусПодписания": "Сформирован",
  "СкладОтправительНаименование": "Склад А",
  "СкладПолучательНаименование": "Склад Б",
  "КолвоШтук": 150.5,
  "КолвоПозиций": 25
}
```

---

##### **Mappers:**

**`MoveResponseMapper`**

Преобразует DTO в domain модели и обратно.


Методы:
```java
public MoveResponse mapToDomain(MoveResponseDto dto) {
    List<MoveItem> items = moveItemMapper.mapToDomainList(dto.getItems());
    return new MoveResponse(
        dto.getErrorText(),
        dto.isResult(),
        dto.getState(),
        dto.getStartDate(),
        dto.getEndDate(),
        dto.getStatusList(),
        items
    );
}

public MoveResponseDto mapToDto(MoveResponse domain) { /* ... */ }
public List<MoveResponse> mapToDomainList(List<MoveResponseDto> dtoList) { /* ... */ }
public List<MoveResponseDto> mapToDtoList(List<MoveResponse> domainList) { /* ... */ }
```

**`MoveItemMapper`**

Преобразует отдельные элементы перемещений.

Методы:
```java
public MoveItem mapToDomain(MoveItemDto dto) {
    return new MoveItem(
        dto.getMovementId(),
        dto.getMovementDisplayText(),
        dto.isCps(),
        dto.getDate(),
        dto.getNumber(),
        dto.isCompleted(),
        dto.getComment(),
        dto.getProductName(),
        dto.getResponsiblePersonName(),
        dto.getColor(),
        dto.getPriority(),
        dto.getAssemblerName(),
        dto.getSigningStatus(),
        dto.getSourceWarehouseName(),
        dto.getDestinationWarehouseName(),
        dto.getItemsCount(),
        dto.getPositionsCount()
    );
}

public MoveItemDto mapToDto(MoveItem domain) { /* аналогично */ }
```

---

##### **Data Sources:**

**`LocalMoveDataSource`**

Читает данные из локального файла `moves.json` в `assets`.

Зависимости:
- `Context` (для доступа к assets)
- `Gson` (для парсинга JSON)
- `MoveResponseMapper` (для преобразования)

Метод:
```java
public MoveResponse getMoveList() throws IOException, ServerErrorWithTypeException {
    // 1. Загрузка JSON из assets
    String jsonContent = loadJSONFromAssets("moves.json");
    
    // 2. Парсинг в DTO
    MoveResponseDto dto = gson.fromJson(jsonContent, MoveResponseDto.class);
    
    // 3. Проверка результата
    if (!dto.isResult()) {
        throw new ServerErrorWithTypeException(dto.getErrorText(), ErrorType.MOVE_LIST);
    }
    
    // 4. Маппинг в domain
    return moveResponseMapper.mapToDomain(dto);
}
```

Файл `assets/moves.json`:
- Содержит тестовые данные для оффлайн режима
- Структура соответствует формату сервера

---

**`RemoteMoveDataSource`**

Получает данные с сервера через Retrofit API.

Зависимости:
- `MoveApiService` (Retrofit interface)
- `MoveResponseMapper` (для маппинга)

Метод:
```java
public void getMoveList(String state, String startDate, String endDate, 
                       String userGuid, boolean useFilter, 
                       DataSourceCallback<MoveResponse> callback) {
    
    Call<MoveResponseDto> call = moveApiService.getMoveList(
        state, startDate, endDate, userGuid, useFilter
    );
    
    call.enqueue(new Callback<MoveResponseDto>() {
        @Override
        public void onResponse(Call<MoveResponseDto> call, Response<MoveResponseDto> response) {
            if (response.isSuccessful() && response.body() != null) {
                processResponseDto(response.body(), callback);
            } else {
                // Попытка парсинга errorBody
                tryParseErrorBody(response, callback);
            }
        }
        
        @Override
        public void onFailure(Call<MoveResponseDto> call, Throwable t) {
            callback.onError(new IOException("Ошибка сети: " + t.getMessage(), t));
        }
    });
}

private void processResponseDto(MoveResponseDto dto, DataSourceCallback<MoveResponse> callback) {
    if (dto.isResult()) {
        MoveResponse domainResponse = moveResponseMapper.mapToDomain(dto);
        callback.onSuccess(domainResponse);
    } else {
        callback.onError(new ServerErrorWithTypeException(
            dto.getErrorText(),
            ErrorType.MOVE_LIST
        ));
    }
}
```

---

##### **API Service:**

**`MoveApiService`** (Retrofit Interface)

```java
public interface MoveApiService {
    /**
     * Получает список перемещений
     * 
     * Пример запроса:
     * GET /hs/jsontsd/movelist?state=Сформирован|Комплектуется&userguid=...&usefilter=true
     */
    @GET("movelist")
    Call<MoveResponseDto> getMoveList(
        @Query("state") String state,
        @Query("startdate") String startDate,
        @Query("enddate") String endDate,
        @Query("userguid") String userGuid,
        @Query("usefilter") boolean useFilter
    );
}
```

**HTTP запрос:**
```
GET https://rdc1c-upp/upp82/ru_RU/hs/jsontsd/movelist?state=Сформирован|Комплектуется|Подготовлен&userguid=550e8400-e29b-41d4-a716-446655440000&usefilter=true
Authorization: Basic dXNlcm5hbWU6cGFzc3dvcmQ=
```

**Параметры:**
- `state` - фильтр по статусам (через "|" для нескольких)
- `startdate` - начальная дата (YYYYMMDD), может быть null
- `enddate` - конечная дата (YYYYMMDD), может быть null
- `userguid` - GUID пользователя для фильтрации доступности
- `usefilter` - применять ли фильтр по доступности

---

##### **Repository Implementation:**

**`MoveRepositoryImpl`**

Выбирает источник данных (локальный или удаленный) на основе настроек.

Зависимости:
- `LocalMoveDataSource`
- `RemoteMoveDataSource`
- `UserSettingsRepository` (для проверки онлайн режима)
- `ConnectivityChecker` (для проверки сети)

Метод:
```java
@Override
public void getMoveList(String state, String startDate, String endDate, 
                       String userGuid, boolean useFilter, 
                       RepositoryCallback<MoveResponse> callback) {
    
    // Проверка режима работы
    boolean isOnlineMode = userSettingsRepository.isOnlineMode();
    boolean isNetworkAvailable = connectivityChecker.isNetworkAvailable();
    
    if (isOnlineMode && isNetworkAvailable) {
        // Онлайн режим
        remoteDataSource.getMoveList(state, startDate, endDate, userGuid, useFilter, 
            new DataSourceCallback<MoveResponse>() {
                @Override
                public void onSuccess(MoveResponse data) {
                    callback.onSuccess(data);
                }
                
                @Override
                public void onError(Exception exception) {
                    callback.onError(exception);
                }
            });
    } else {
        // Оффлайн режим
        try {
            MoveResponse response = localDataSource.getMoveList();
            callback.onSuccess(response);
        } catch (Exception e) {
            callback.onError(e);
        }
    }
}
```

---

#### 📊 Детальные потоки данных

##### **Сценарий 1: Загрузка списка перемещений (Онлайн)**

```
[User] Открывает MoveListActivity
   ↓
[Activity] onCreate()
   ├─ Инициализация ViewModel
   ├─ Настройка UI компонентов
   ├─ Создание 3 фрагментов (по одному на статус)
   ├─ Подписка на LiveData
   └─ moveListViewModel.loadMoveData()
      ↓
[ViewModel] loadMoveData()
   ├─ _isLoading.setValue(true)  →  [Activity] показывает LoadingDialog
   ├─ Получение userGuid через GetUserUseCase
   ├─ Получение availabilityChecked из SavedStateHandle
   └─ moveRepository.getMoveList(state, null, null, userGuid, useFilter, callback)
      ↓
[Repository] MoveRepositoryImpl.getMoveList()
   ├─ Проверка isOnlineMode() → true
   ├─ Проверка isNetworkAvailable() → true
   └─ remoteDataSource.getMoveList(...)
      ↓
[DataSource] RemoteMoveDataSource.getMoveList()
   └─ moveApiService.getMoveList(state, startDate, endDate, userGuid, useFilter)
      ↓
[Retrofit] Создание HTTP запроса
   └─ OkHttpClient
      ├─ DynamicBaseUrlInterceptor → добавляет BASE_URL
      ├─ BasicAuthInterceptor → добавляет Authorization header
      ├─ ConnectionRetryInterceptor → настройка повторных попыток
      └─ FullBodyLoggingInterceptor → логирование
         ↓
[Network] HTTP GET запрос к 1C сервер
```
GET /hs/jsontsd/movelist?state=Сформирован|Комплектуется|Подготовлен&userguid=...&usefilter=true
```
         ↓
[1C Server] Обработка запроса
   └─ Отправка response в формате JSON
      ↓
[Retrofit] onResponse(Response<MoveResponseDto>)
   ├─ Проверка response.isSuccessful()
   ├─ Проверка response.body() != null
   └─ → processResponseDto(dto, callback)
      ↓
[DataSource] processResponseDto()
   ├─ Проверка dto.isResult() == true?
   │  ├─ false → ServerErrorWithTypeException(dto.getErrorText(), MOVE_LIST)
   │  └─ true → продолжение
   ├─ moveResponseMapper.mapToDomain(dto)
   │  ├─ Маппинг MoveResponseDto → MoveResponse
   │  └─ moveItemMapper.mapToDomainList(dto.getItems())
   │     └─ Для каждого MoveItemDto:
   │        └─ mapToDomain() → создание MoveItem
   └─ callback.onSuccess(MoveResponse)
      ↓
[Repository] callback.onSuccess(response)
   └─ RepositoryCallback.onSuccess(response)
      ↓
[ViewModel] onSuccess(MoveResponse response)
   ├─ Разделение items по статусам:
   │  ├─ formirovanList = []
   │  ├─ komplektuetsaList = []
   │  └─ podgotovlenList = []
   │  └─ for (item : response.getItems()):
   │     └─ switch (item.getSigningStatus()):
   │        ├─ "Сформирован" → formirovanList.add(item)
   │        ├─ "Комплектуется" → komplektuetsaList.add(item)
   │        └─ "Подготовлен" → podgotovlenList.add(item)
   │
   ├─ Сортировка каждого списка по приоритету:
   │  └─ sortByPriority(list)
   │     └─ Collections.sort(list, priorityComparator)
   │        └─ Порядок: Неотложный(1) < Высокий(2) < Средний(3) < Низкий(4)
   │
   ├─ Обновление оригинальных списков:
   │  ├─ _originalFormirovanList.setValue(formirovanList)
   │  ├─ _originalKomplektuetsaList.setValue(komplektuetsaList)
   │  └─ _originalPodgotovlenList.setValue(podgotovlenList)
   │
   ├─ triggerFilterRecalculation()
   │  └─ executorService.execute(() → {
   │     ├─ applyFiltersToList(originalFormirovanList, filters...) → filteredFormirovan
   │     ├─ applyFiltersToList(originalKomplektuetsaList, filters...) → filteredKomplektuetsa
   │     ├─ applyFiltersToList(originalPodgotovlenList, filters...) → filteredPodgotovlen
   │     ├─ _filteredFormirovanList.postValue(filteredFormirovan)
   │     ├─ _filteredKomplektuetsaList.postValue(filteredKomplektuetsa)
   │     ├─ _filteredPodgotovlenList.postValue(filteredPodgotovlen)
   │     └─ updateIsAnyFilterActive()
   │  })
   │
   └─ _isLoading.postValue(false)  →  [Activity] скрывает LoadingDialog
      ↓
[LiveData] filteredFormirovanList.postValue()
   ↓
[Fragment] formirovanFragment observeViewModel()
   └─ filteredFormirovanList.observe(owner, newList → {
      └─ updateAdapterData(newList)
         └─ adapter.submitList(new ArrayList<>(newList))
            ↓
[Adapter] MoveListAdapter.submitList()
   ├─ DiffUtil вычисляет изменения
   │  └─ MoveDiffCallback:
   │     ├─ areItemsTheSame() - сравнение по movementId
   │     └─ areContentsTheSame() - сравнение всех полей
   │
   └─ Обновление RecyclerView (только измененные элементы)
      ↓
[RecyclerView] Отображение списка перемещений
   └─ Для каждого элемента:
      ├─ onBindViewHolder()
      ├─ Форматирование даты
      ├─ Установка цвета приоритета
      ├─ Отображение статуса "Проведен"
      └─ Настройка checkbox выбора
```

**Аналогично** для фрагментов `komplektuetsaFragment` и `podgotovlenFragment`.

---

##### **Сценарий 2: Фильтрация перемещений**

```
[User] Открывает боковую панель фильтров
   └─ Нажимает navMenuButton
      ↓
[Activity] drawerLayout.openDrawer(GravityCompat.START)
   └─ Открывается NavigationView с фильтрами
      ↓
[User] Вводит текст в filterSender (например, "Склад А")
   ↓
[Activity] Текст сохраняется в EditText, но НЕ применяется автоматически
      ↓
[User] Нажимает Enter ИЛИ кнопку "Применить фильтры"
   ↓
[Activity] applyFilters()
   ├─ Считывает текущие значения всех фильтров из UI компонентов
   ├─ moveListViewModel.setSenderFilter(filterSender.getText().toString())
   ├─ moveListViewModel.setNomenculatureFilter(...)
   ├─ moveListViewModel.setSeriesFilter(...)
   ├─ moveListViewModel.setAvailabilityChecked(...)
   └─ ... (все остальные фильтры)
      ↓
[ViewModel] setSenderFilter("Склад А")
   ├─ savedStateHandle.set(KEY_SENDER, "Склад А")  // Сохранение в SavedState
   └─ НЕ вызывает triggerFilterRecalculation() (будет вызван после всех фильтров)
      ↓
[ViewModel] После установки всех фильтров
   └─ Проверка изменений критичных фильтров (серия, номенклатура, доступность)
      ├─ Если изменились → перезагрузка данных с сервера (loadMoveData)
      └─ Если не изменились → triggerFilterRecalculation()
      ↓
[ViewModel] triggerFilterRecalculation()
   └─ executorService.execute(() → {
      // Фоновый поток
      ├─ Для каждого из 3 списков (formirovan, komplektuetsa, podgotovlen):
      │  └─ List<MoveItem> filtered = applyFiltersToList(
      │        originalList,
      │        senderFilter = "Склад А",
      │        movementNumberFilter = "",
      │        recipientFilter = "",
      │        assemblerFilter = "",
      │        priorityFilter = "",
      │        receiverFilter = "",
      │        seriesFilter = "",
      │        nomenclatureFilter = "",
      │        cpsChecked = true
      │     )
      │     ↓
      │  applyFiltersToList():
      │     └─ for (MoveItem item : originalList):
      │        ├─ boolean matches = true
      │        │
      │        ├─ // Фильтр по ЦПС
      │        ├─ if (cpsChecked && !item.isCps()):
      │        │     matches = false
      │        │
      │        ├─ // Фильтр по отправителю
      │        ├─ if (matches && senderFilter != "" ):
      │        │     if (item.getSourceWarehouseName() == null ||
      │        │         !item.getSourceWarehouseName().toLowerCase()
      │        │              .contains("склад а")):
      │        │        matches = false
      │        │
      │        ├─ // Остальные фильтры аналогично...
      │        │
      │        └─ if (matches):
      │              filteredList.add(item)
      │
      ├─ _filteredFormirovanList.postValue(filteredFormirovan)
      ├─ _filteredKomplektuetsaList.postValue(filteredKomplektuetsa)
      ├─ _filteredPodgotovlenList.postValue(filteredPodgotovlen)
      │
      └─ updateIsAnyFilterActive()
         ├─ boolean isActive = isFilterApplied("Склад А") ||  // true!
         │                     isFilterApplied("") ||           // false
         │                     isFilterPriorityApplied("") ||   // false
         │                     ...
         │                     cpsChecked == false              // false
         └─ _isAnyFilterActiveLive.postValue(true)
   })
      ↓
[LiveData] filteredFormirovanList изменился
   ↓
[Fragment] observeViewModel() получает новый список
   └─ updateAdapterData(filtered)
      └─ adapter.submitList(filtered)
         ↓
[DiffUtil] Вычисляет разницу между старым и новым списком
   └─ Удаляет элементы не из "Склад А"
   └─ Оставляет только элементы с sourceWarehouseName содержащим "склад а"
      ↓
[RecyclerView] Плавно обновляется (удаляются несоответствующие элементы)
   ↓
[Activity] isAnyFilterActiveLive.observe() → получает true
   └─ filterIndicator.setVisibility(View.VISIBLE)  // Показать индикатор
```

---

##### **Сценарий 3: Массовое изменение статуса (Онлайн)**

```
[User] Выбирает 3 перемещения через checkbox
   ↓
[Adapter] checkboxMove.setOnCheckedChangeListener()
   ├─ selectedItems.add(itemId1)
   ├─ selectedItems.add(itemId2)
   ├─ selectedItems.add(itemId3)
   └─ notifySelectionChangeListener()
      ↓
[Fragment] onSelectionChanged(count=3)
   └─ ((MoveListActivity) getActivity()).updateSelectionPanel(3)
      ↓
[Activity] updateSelectionPanel(count=3)
   ├─ actionButtonsPanel.setVisibility(View.VISIBLE)  // Показать панель
   ├─ selectedItemsCount.setText("Выбрано: 3")
   ├─ isSelectionMode = true
   └─ Настройка видимости кнопок в зависимости от текущей вкладки:
      └─ if (currentTab == "Сформирован"):
         ├─ btnMoveToWork.setVisibility(View.VISIBLE)    // "Взять в работу"
         ├─ btnMoveFromWork.setVisibility(View.GONE)
         └─ btnFinishWork.setVisibility(View.GONE)
            ↓
[User] Нажимает кнопку "Взять в работу"
   ↓
[Activity] btnMoveToWork.setOnClickListener()
   └─ Получение выбранных элементов из текущего фрагмента:
      └─ List<MoveItem> selectedItems = currentFragment.getSelectedItems()
         └─ [MoveItem1, MoveItem2, MoveItem3]
            ↓
   └─ moveListViewModel.moveItems(
         selectedItems,
         currentFragmentState = "Сформирован",
         targetState = "Комплектуется"
      )
         ↓
[ViewModel] moveItems(items, "Сформирован", "Комплектуется")
   ├─ Проверка isOnline = getOnlineModeUseCase.execute() → true
   ├─ items.size() > 1 → true (3 элемента)
   └─ startMultiMoveProcess()
      ├─ multiMoveQueue = [MoveItem1, MoveItem2, MoveItem3]
      ├─ multiMoveIndex = 0
      ├─ multiMoveSuccessGuids = []
      ├─ multiMoveTargetState = "Комплектуется"
      ├─ multiMoveSourceState = "Сформирован"
      ├─ multiMoveActive = true
      ├─ multiMoveHadSuccess = false
      ├─ multiMoveHadError = false
      ├─ multiMoveErrorCount = 0
      └─ processSingleMoveInMulti()
         ↓
[ViewModel] processSingleMoveInMulti() - обработка первого элемента
   ├─ MoveItem currentItem = multiMoveQueue.get(multiMoveIndex)  // MoveItem1
   ├─ String guid = currentItem.getMovementId()
   ├─ String userGuid = getCurrentUserGuid()
   └─ changeMoveStatusUseCase.execute(guid, "Комплектуется", userGuid, callback)
      ↓
[UseCase] ChangeMoveStatusUseCase.execute()
   └─ moveRepository.changeMoveStatus(guid, "Комплектуется", userGuid, callback)
      ↓
[Repository] MoveRepositoryImpl.changeMoveStatus()
   └─ remoteDataSource.changeMoveStatus(guid, "Комплектуется", userGuid, callback)
      ↓
[DataSource] RemoteMoveDataSource.changeMoveStatus()
   └─ moveApiService.changeMoveStatus(guid, "Комплектуется", userGuid)
      ↓
[Retrofit] HTTP GET запрос
```
GET /hs/jsontsd/documentmove?guid=...&state=Комплектуется&userguid=...
```
      ↓
[1C Server] Смена статуса документа
   ├─ Формирование ответа
   └─ Отправка ChangeMoveStatusResponseDto
      ↓
[Retrofit] onResponse(Response<ChangeMoveStatusResponseDto>)
   └─ processChangeMoveStatusResponseDto(dto, callback)
      ├─ Проверка dto.isResult() == true
      ├─ Маппинг dto → ChangeMoveStatusResult
      └─ callback.onSuccess(result)
         ↓
[ViewModel] onSuccess(ChangeMoveStatusResult) - первый элемент
   ├─ result.isResult() == true → success
   ├─ multiMoveSuccessGuids.add(guid)  // [guid1]
   ├─ multiMoveHadSuccess = true
   ├─ multiMoveIndex++  // = 1
   ├─ Проверка: multiMoveIndex < multiMoveQueue.size()? → true (1 < 3)
   └─ processSingleMoveInMulti()  // Обработка второго элемента
      ↓
   ... (аналогично для MoveItem2)
      ↓
   ... (аналогично для MoveItem3)
      ↓
[ViewModel] После обработки всех 3 элементов
   ├─ multiMoveIndex == multiMoveQueue.size()  // 3 == 3
   └─ finishMultiMoveProcess()
      ├─ Подготовка сообщения:
      │  └─ message = "Успешно изменено 3 из 3 перемещений"
      │
      ├─ Удаление элементов из исходного списка:
      │  └─ for (guid : multiMoveSuccessGuids):
      │     └─ removeItemFromList(guid, "Сформирован")
      │        └─ List<MoveItem> currentList = _originalFormirovanList.getValue()
      │           └─ newList = currentList без элементов с guid
      │              └─ _originalFormirovanList.setValue(newList)
      │
      ├─ Перезагрузка целевого списка:
      │  └─ reloadTargetList("Комплектуется")
      │     └─ moveRepository.getMoveList("Комплектуется", ..., callback)
      │        └─ (полный цикл загрузки данных для статуса "Комплектуется")
      │
      ├─ Очистка multiMove переменных:
      │  ├─ multiMoveActive = false
      │  ├─ multiMoveQueue = null
      │  └─ ...
      │
      ↓
[RecyclerView] Обновление списков
   ├─ filteredFormirovanList уменьшился на 3 элемента
   └─ filteredKomplektuetsaList увеличился на 3 элемента
      ↓
[Activity] Очистка выбора
   └─ currentFragment.clearSelection()
      └─ adapter.clearSelection()
         └─ selectedItems.clear()
            └─ notifyItemChanged() для каждого элемента
               ↓
[Activity] Скрытие панели выбора
   └─ actionButtonsPanel.setVisibility(View.GONE)
```

---

##### **Сценарий 4: Клик по элементу и переход к деталям**

```
[User] Нажимает на элемент перемещения в списке
   ↓
[Adapter] itemView.setOnClickListener()
   └─ itemClickListener.onItemClicked(moveItem)
      ↓
[Fragment] onItemClicked(moveItem)
   └─ moveListViewModel.processMoveItemClick(moveItem)
      ↓
[ViewModel] processMoveItemClick(MoveItem moveItem)
   ├─ String moveUuid = moveItem.getMovementId()
   ├─ _isProcessingItemClick.setValue(true)  // Показать индикатор загрузки
   └─ moveRepository.getDocumentMove(moveUuid, callback)
      ↓
[Repository] MoveRepositoryImpl.getDocumentMove()
   └─ (онлайн/оффлайн проверка и получение документа)
      ↓
[Repository] callback.onSuccess(Invoice invoice)
   ↓
[ViewModel] onSuccess(Invoice invoice)
   ├─ _isProcessingItemClick.postValue(false)  // Скрыть индикатор
   │
   ├─ Проверка наличия товаров:
   │  └─ if (invoice == null || products == null || products.isEmpty()):
   │     └─ _showEmptyMovementErrorEvent.postValue(
   │           new SingleEvent<>("Перемещение " + number + " не содержит товаров")
   │        )
   │        [STOP]
   │
   ├─ Проверка УИДСтрокиТовары:
   │  └─ for (Product product : products):
   │     └─ if (product.getProductLineId() == null || isEmpty()):
   │        └─ allProductsHaveLineId = false
   │           break
   │
   │  └─ if (!allProductsHaveLineId):
   │     └─ _showEmptyProductLineIdErrorEvent.postValue(
   │           new SingleEvent<>("Ошибка: некоторые товары не имеют УИДСтрокиТовары")
   │        )
   │        [STOP]
   │
   └─ Все проверки пройдены:
      └─ _navigateToPrixodEvent.postValue(new SingleEvent<>(moveUuid))
         ↓
[Activity] navigateToPrixodEvent.observe()
   └─ String moveUuid = event.getContentIfNotHandled()
      └─ if (moveUuid != null):
         └─ Intent intent = new Intent(this, ProductsActivity.class)
            ├─ intent.putExtra("moveUuid", moveUuid)
            └─ startActivity(intent)
               ↓
[ProductsActivity] Открывается экран детального просмотра
```

---

##### **Сценарий 5: Сохранение и применение фильтров по умолчанию**

```
[User] Настраивает фильтры по своему вкусу
   ├─ Вводит "Склад А" в filterSender
   ├─ Выбирает "Высокий" в filterPriority
   └─ Снимает галочку с filterCps
      ↓
[User] Нажимает Enter ИЛИ кнопку "Применить фильтры"
   ↓
[Activity] applyFilters()
   └─ Все значения фильтров сохраняются в SavedStateHandle через ViewModel
   └─ Фильтры применяются к спискам
      ↓
[User] Нажимает кнопку "Сохранить фильтры по умолчанию"
   ↓
[Activity] btnSetDefaultFilters.setOnClickListener()
   └─ moveListViewModel.saveDefaultFiltersForCurrentUser()
      ↓
[ViewModel] saveDefaultFiltersForCurrentUser()
   ├─ User currentUser = getUserUseCase.execute()
   ├─ String userGuid = currentUser.getUserGuid()
   ├─ String userName = currentUser.getFullName()
   │
   ├─ Создание объекта фильтров:
   │  └─ DefaultFiltersData filtersData = new DefaultFiltersData(userGuid, userName)
   │     ├─ filtersData.setSender("Склад А")
   │     ├─ filtersData.setMovementNumber("")
   │     ├─ filtersData.setRecipient("")
   │     ├─ filtersData.setAssembler("")
   │     ├─ filtersData.setPriority("Высокий")
   │     ├─ filtersData.setReceiver("")
   │     ├─ filtersData.setSeries("")
   │     ├─ filtersData.setNomenclature("")
   │     ├─ filtersData.setCpsChecked(false)
   │     └─ filtersData.setAvailabilityChecked(true)
   │
   └─ defaultFiltersManager.saveDefaultFilters(userGuid, userName, filtersData)
      ↓
[Manager] DefaultFiltersManager.saveDefaultFilters()
   ├─ Загрузка существующих фильтров всех пользователей:
   │  └─ List<DefaultFiltersData> allFilters = loadAllDefaultFilters()
   │     └─ Чтение из файла: context.getFilesDir() + "/default_filters.json"
   │        └─ gson.fromJson(jsonContent, TypeToken<List<DefaultFiltersData>>)
   │
   ├─ Удаление старой записи для текущего пользователя (если есть):
   │  └─ Iterator<DefaultFiltersData> iterator = allFilters.iterator()
   │     └─ while (iterator.hasNext()):
   │        └─ if (existingData.getUserGuid().equals(userGuid)):
   │           └─ iterator.remove()
   │              break
   │
   ├─ Добавление новой записи:
   │  └─ allFilters.add(filtersData)
   │
   └─ Сохранение в файл:
      └─ String json = gson.toJson(allFilters)
         └─ FileOutputStream → write(json)
            └─ /data/data/com.step.tcd_rpkb/files/default_filters.json
               ↓
[Manager] return true
   ↓
[ViewModel] Результат сохранения
   └─ if (success):
      └─ _showDefaultFiltersMessageEvent.postValue(
            new SingleEvent<>("Фильтры по умолчанию сохранены")
         )
            ↓
[Activity] showDefaultFiltersMessageEvent.observe()
   └─ Toast.makeText(this, "Фильтры по умолчанию сохранены", Toast.LENGTH_SHORT).show()
```

**Загрузка фильтров при следующем запуске:**

```
[User] Повторно открывает MoveListActivity
   ↓
[Activity] onCreate()
   └─ moveListViewModel.loadDefaultFiltersForCurrentUser()
      ↓
[ViewModel] loadDefaultFiltersForCurrentUser()
   ├─ User currentUser = getUserUseCase.execute()
   ├─ String userGuid = currentUser.getUserGuid()
   └─ DefaultFiltersData filtersData = 
         defaultFiltersManager.loadDefaultFiltersForUser(userGuid)
         ↓
[Manager] loadDefaultFiltersForUser(userGuid)
   ├─ List<DefaultFiltersData> allFilters = loadAllDefaultFilters()
   └─ for (DefaultFiltersData data : allFilters):
      └─ if (data.getUserGuid().equals(userGuid)):
         └─ return data
            ↓
[ViewModel] Применение загруженных фильтров
   ├─ setSenderFilter(filtersData.getSender())            // "Склад А"
   ├─ setMovementNumberFilter(filtersData.getMovementNumber())  // ""
   ├─ setRecipientFilter(filtersData.getRecipient())      // ""
   ├─ setAssemblerFilter(filtersData.getAssembler())      // ""
   ├─ setPriorityFilter(filtersData.getPriority())        // "Высокий"
   ├─ setReceiverFilter(filtersData.getReceiver())        // ""
   ├─ setSeriesFilter(filtersData.getSeries())            // ""
   ├─ setNomenclatureFilter(filtersData.getNomenclature())  // ""
   ├─ setCpsChecked(filtersData.isCpsChecked())           // false
   └─ setAvailabilityChecked(filtersData.isAvailabilityChecked())  // true
      ↓
[SavedStateHandle] Все значения сохранены
   ↓
[ViewModel] triggerFilterRecalculation()
   └─ Применение фильтров к загруженным данным
      ↓
[Activity] Фильтры в UI обновляются (через LiveData binding)
   ├─ filterSender.setText("Склад А")
   ├─ filterPriority.setSelection(position("Высокий"))
   ├─ filterSeries.setText("")
   ├─ filterNomenclature.setText("")
   ├─ filterCps.setChecked(false)
   └─ filterIndicator.setVisibility(View.VISIBLE)
```

---

### ⚙️ Особенности реализации

#### 1. **SavedStateHandle для сохранения состояния**

ViewModel использует `SavedStateHandle` для автоматического сохранения состояния фильтров и текущей вкладки при смерти процесса:

```java
// Инициализация LiveData из SavedStateHandle
senderFilter = savedStateHandle.getLiveData(KEY_SENDER, "");
currentTabPosition = savedStateHandle.getLiveData(KEY_CURRENT_TAB_POSITION, 0);

// При изменении - автоматическое сохранение
public void setSenderFilter(String filter) {
    savedStateHandle.set(KEY_SENDER, filter);  // Автоматически сохраняется
    triggerFilterRecalculation();
}
```

**Преимущества:**
- Восстановление состояния после смерти процесса
- Восстановление после поворота экрана
- Автоматическое управление жизненным циклом

#### 2. **Единые фильтры для всех вкладок**

В отличие от раздельных фильтров для каждой вкладки, реализованы единые фильтры:

```java
// Один набор фильтров применяется ко всем трем спискам
triggerFilterRecalculation() {
    executorService.execute(() -> {
        List<MoveItem> filteredFormirovan = applyFiltersToList(
            _originalFormirovanList.getValue(), filters...
        );
        List<MoveItem> filteredKomplektuetsa = applyFiltersToList(
            _originalKomplektuetsaList.getValue(), filters...
        );
        List<MoveItem> filteredPodgotovlen = applyFiltersToList(
            _originalPodgotovlenList.getValue(), filters...
        );
        
        // Обновление всех трех списков одновременно
        _filteredFormirovanList.postValue(filteredFormirovan);
        _filteredKomplektuetsaList.postValue(filteredKomplektuetsa);
        _filteredPodgotovlenList.postValue(filteredPodgotovlen);
    });
}
```


#### 3. **Применение фильтров по требованию**

Фильтры применяются только при явном действии пользователя:

```java
// Обработка нажатия Enter в текстовых полях фильтров
filterSender.setOnKeyListener((v, keyCode, event) -> {
    if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
        applyFilters();  // Применение всех фильтров
        return true;
    }
    return false;
});

// Обработка нажатия кнопки "Применить фильтры"
btnApplyFilters.setOnClickListener(v -> applyFilters());

// Метод применения всех фильтров
private void applyFilters() {
    moveListViewModel.setSenderFilter(filterSender.getText().toString());
    moveListViewModel.setMovementNumberFilter(filterMovementNumber.getText().toString());
    // ... установка всех остальных фильтров
    
    // Если изменились критичные фильтры (серия, номенклатура, доступность)
    // - будет вызван loadMoveData() для перезагрузки с сервера
    // Иначе - triggerFilterRecalculation() для локальной фильтрации
}
```




#### 4. **Фоновая фильтрация через ExecutorService**

Фильтрация выполняется в фоновом потоке для предотвращения блокировки UI:

```java
private final ExecutorService executorService = Executors.newSingleThreadExecutor();

triggerFilterRecalculation() {
    executorService.execute(() -> {
        // Работа в фоновом потоке
        List<MoveItem> filtered = applyFiltersToList(...);
        
        // Обновление LiveData (postValue - thread-safe)
        _filteredFormirovanList.postValue(filtered);
    });
}
```

**Преимущества:**
- UI остается отзывчивым даже при больших списках

---

## Модуль 3: Работа с документами перемещения и прихода

### Описание
Модуль объединяет работу с документами перемещения товаров и документами прихода. Позволяет просматривать состав документа, редактировать количество товаров, менять статус документа.

### Основные сценарии использования:
- Просмотр состава документа перемещения
- Просмотр документа прихода
- Редактирование количества товаров
- Смена статуса перемещения
- Сохранение изменений перед сменой статуса

### Компоненты модуля

#### Domain Layer

**Models:**
- `Invoice` - документ (перемещение/приход)
  - `errorText` - текст ошибки
  - `result` - результат операции
  - `uuid` - UUID документа
  - `products` - список товаров

- `Product` - товар в документе
  - `productLineId` - UUID строки товара
  - `parentProductLineId` - UUID родительской строки (для копий)
  - `nomenclatureUuid` - UUID номенклатуры
  - `nomenclatureName` - наименование
  - `seriesName` - серия
  - `seriesUuid` - UUID серии
  - `quantity` - количество
  - `unitName` - единица измерения
  - `senderStorageName` - место хранения отправителя
  - `receiverStorageName` - место хранения получателя
  - `responsibleReceiverName` - ответственный
  - `reserveDocumentName` - документ резерва (ЗНП)
  - `freeBalance` - свободный остаток
  - `taken` - взято
  - `exists` - существует (для отметки удаленных)

- `ChangeMoveStatusResult` - результат смены статуса
  - `errorText` - текст ошибки
  - `result` - результат
  - `status` - новый статус
  - `user` - пользователь
  - `data` - дополнительные данные
  - `moveGuid` - UUID перемещения

**Use Cases:**
- `ChangeMoveStatusUseCase` - смена статуса перемещения
- `SaveMoveDataBeforeStatusChangeUseCase` - сохранение данных перед сменой статуса
- `GetPrixodDocumentUseCase` - получение документа прихода

**Repository Interfaces:**
- `MoveRepository` - работа с перемещениями
- `PrixodRepository` - работа с приходами

#### Data Layer

**DTOs:**
- `InvoiceDto` - DTO документа
- `ProductDto` - DTO товара
- `ChangeMoveStatusResponseDto` - DTO ответа смены статуса
- `SaveMoveDataRequestDto` - DTO запроса сохранения
- `SaveMoveDataItemDto` - DTO элемента для сохранения

**Mappers:**
- `InvoiceMapper` - маппинг документа
- `ProductMapper` - маппинг товара
- `ChangeMoveStatusMapper` - маппинг результата смены статуса
- `SaveMoveDataMapper` - маппинг данных для сохранения

**Data Sources:**
- `LocalMoveDataSource.getDocumentMove()` - локальные данные документа
- `RemoteMoveDataSource.getDocumentMove()` - документ с сервера
- `RemoteMoveDataSource.changeMoveStatus()` - смена статуса

**API Services:**
- `MoveApiService.getDocumentMove()` - получение документа
- `MoveApiService.changeMoveStatus()` - смена статуса
- `MoveApiService.saveMoveData()` - сохранение данных в 1С

**Repository Implementations:**
- `MoveRepositoryImpl` - управление перемещениями
- `PrixodRepositoryImpl` - делегирует в MoveRepository

#### App Layer
- `MoveDetailActivity` / `MoveDetailFragment` - экран детального просмотра
- `MoveDetailViewModel` - логика управления
- `ProductListAdapter` - адаптер списка товаров
- `ProductItemViewHolder` - ViewHolder товара

### Поток данных (Просмотр документа):
1. Пользователь открывает перемещение
2. `MoveDetailViewModel` → `MoveRepository.getDocumentMove(guid)`
3. **Онлайн:** API запрос → `InvoiceDto`
4. **Оффлайн:** Чтение из assets (`1move.json`, `2move.json`)
5. Маппинг → `Invoice` с `List<Product>`
6. Отображение в UI

### Поток данных (Смена статуса):
1. Пользователь нажимает кнопку смены статуса
2. `ChangeMoveStatusUseCase.executeWithDataSave()`
3. **Шаг 1:** `SaveMoveDataBeforeStatusChangeUseCase` → сохранение в 1С
4. **Шаг 2:** `MoveRepository.changeMoveStatus()` → смена статуса
5. Получение `ChangeMoveStatusResult`
6. Обновление UI

---

## Модуль 4: Работа с сериями товаров и ЗНП

### Описание
Модуль объединяет функционал работы с сериями товаров. Включает подбор и замену серий для товаров, а также просмотр и резервирование ЗНП для конкретных серий.

### Основные сценарии использования:
- Просмотр доступных серий для товара
- Распределение количества по сериям
- Замена серии товара на другую
- Сохранение распределения серий
- Просмотр ЗНП для серии
- Резервирование количества по ЗНП
- Сохранение резервирования ЗНП

### Компоненты модуля

#### Domain Layer

**Models:**
- `SeriesItem` - элемент серии
  - `seriesUuid` - UUID серии
  - `seriesName` - наименование серии
  - `expiryDate` - срок годности
  - `freeBalance` - свободный остаток
  - `reservedByOthers` - зарезервировано другими
  - `documentQuantity` - количество документа
  - `allocatedQuantity` - распределенное количество

- `SeriesChangeItem` - элемент для замены серии (расширенная версия Product)
  - Содержит все поля `Product`
  - `isSelected` - выбран для замены
  - Методы: `fromProduct()`, `toProduct()`

- `ZnpSeriesData` - данные ЗНП для серии
  - `seriesUuid` - UUID серии
  - `warehouse` - склад
  - `unitOfMeasurement` - единица измерения
  - `freeBalance` - свободный остаток
  - `znpOrders` - список заказов

- `ZnpOrderItem` - элемент заказа ЗНП
  - `znpNumber` - номер ЗНП
  - `fromDate` - дата от
  - `quantityToProcure` - к обеспечению
  - `reserve` - резерв
  - `reserveByOthers` - резерв других
  - `reservedQuantity` - зарезервированное количество

**Use Cases:**
- `GetSeriesForNomenclatureUseCase` - получение серий для товара
- `SaveSeriesAllocationUseCase` - сохранение распределения серий
- `GetZnpDataForSeriesUseCase` - получение данных ЗНП для серии (используется напрямую через репозиторий)
- `SaveZnpReservationUseCase` - сохранение резервирования ЗНП (используется напрямую через репозиторий)

**Repository Interfaces:**
- `SeriesRepository` - работа с сериями
- `ZnpRepository` - работа с ЗНП

#### Data Layer

**DTOs:**
- `ProductSeriesDto` - DTO серии товара
- `ProductSeriesResponseDto` - DTO ответа со списком серий
- `ZnpSeriesDataDto` - DTO данных ЗНП серии
- `ZnpOrderDto` - DTO заказа ЗНП

**Mappers:**
- `ProductSeriesMapper` - маппинг серий
- `ZnpDataMapper` - маппинг данных ЗНП

**Data Sources:**
- `RemoteSeriesDataSource` - получение серий через API
- Локальные данные из assets для серий (в SeriesRepositoryImpl)
- `ZnpLocalDataSource` - локальные данные ЗНП из assets
  - Файлы: `znp_data_series1.json`, `znp_data_series2.json`, `znp_data_series3.json`

**API Services:**
- `MoveApiService.getProductSeries()` - получение серий для продукта

**Repository Implementations:**
- `SeriesRepositoryImpl` - управление онлайн/оффлайн режимами для серий
- `ZnpRepositoryImpl` - работа только в оффлайн режиме для ЗНП

**Dependency Injection:**
- `SeriesModule` - Hilt модуль для инъекции зависимостей серий
- `ZnpModule` - Hilt модуль для инъекции зависимостей ЗНП

#### App Layer
- `SeriesSelectionActivity` / `SeriesSelectionFragment` - экран подбора серий
- `SeriesChangeActivity` / `SeriesChangeFragment` - экран замены серий
- `ZnpActivity` / `ZnpFragment` - экран работы с ЗНП
- `SeriesSelectionViewModel` - логика подбора
- `SeriesChangeViewModel` - логика замены
- `ZnpViewModel` - логика управления ЗНП
- `SeriesListAdapter` - адаптер списка серий
- `SeriesItemViewHolder` - ViewHolder серии
- `ZnpOrdersAdapter` - адаптер списка заказов ЗНП

### Поток данных (Подбор серии):
1. Пользователь открывает товар для подбора серии
2. `GetSeriesForNomenclatureUseCase` → `SeriesRepository`
3. **Онлайн:** `RemoteSeriesDataSource` → `MoveApiService.getProductSeries()`
4. **Оффлайн:** Чтение из локального хранилища
5. Маппинг `ProductSeriesDto` → `SeriesItem`
6. Пользователь распределяет количество по сериям
7. `SaveSeriesAllocationUseCase` → сохранение

### Поток данных (Замена серии):
1. Пользователь выбирает товар для замены серии
2. `SeriesChangeItem.fromProduct()` - создание элемента из Product
3. Получение доступных серий
4. Пользователь выбирает новую серию
5. `SeriesChangeItem.toProduct()` - конвертация обратно в Product
6. Сохранение изменений

### Поток данных (ЗНП):
1. Пользователь открывает ЗНП для серии
2. `ZnpRepository.getZnpDataForSeries(seriesUuid)`
3. `ZnpLocalDataSource` → определение файла по UUID серии
4. Чтение JSON из assets (`znp_data_series1.json`, `znp_data_series2.json`, `znp_data_series3.json`)
5. Маппинг `ZnpSeriesDataDto` → `ZnpSeriesData`
6. Пользователь распределяет количество по ЗНП
7. `ZnpRepository.saveZnpReservation()` - сохранение резервирования

---

## Модуль 5: Настройки приложения(этот модуль нужен только для легкого переключения между базами данных, режимами работы, в релизной вверсии он будет скрыт)

### Описание
Модуль управления настройками приложения: режим работы (онлайн/оффлайн), выбор базы данных.

### Основные сценарии использования:
- Переключение между онлайн/оффлайн режимами
- Выбор базы данных (основная/тестовая)
- Просмотр сохраненных учетных данных

### Компоненты модуля

#### Domain Layer

**Models:**
- `Credentials` - учетные данные (переиспользуется из модуля авторизации)

**Use Cases:**
- `GetCredentialsUseCase` / `SaveCredentialsUseCase`
- `GetOnlineModeUseCase` / `SetOnlineModeUseCase`
- `GetMainDatabaseUseCase` / `SetMainDatabaseUseCase`

**Repository Interfaces:**
- `UserSettingsRepository` - управление настройками

#### Data Layer

**Repository Implementations:**
- `UserSettingsRepositoryImpl` - хранение в SharedPreferences
  - Ключи: `username`, `password`, `online_mode`, `main_database`
  - URL баз данных

#### App Layer
- `SettingsActivity` / `SettingsFragment` - экран настроек
- `SettingsViewModel` - логика управления

### Поток данных:
1. Пользователь открывает настройки
2. Загрузка текущих настроек через UseCase'ы
3. Изменение настройки
4. `SetOnlineModeUseCase` / `SetMainDatabaseUseCase`
5. Сохранение в SharedPreferences
6. Обновление UI

---

## Общая инфраструктура

### Компоненты, используемые во всех модулях

#### Domain Layer

**Utilities:**
- `RepositoryCallback<T>` - интерфейс callback для асинхронных операций
  - `onSuccess(T data)` - успешный результат
  - `onError(Exception exception)` - ошибка

- `ServerAvailabilityCallback` - callback проверки сервера
  - `onResult(boolean isAvailable)` - результат проверки

- `ConnectivityChecker` - интерфейс проверки подключения
  - `isNetworkAvailable()` - наличие сети

**Use Cases:**
- `CheckServerAvailabilityUseCase` - проверка доступности сервера

**Repository Interfaces:**
- `ServerAvailabilityRepository` - проверка сервера

#### Data Layer

**Network - Interceptors:**
- `DynamicBaseUrlInterceptor` - динамическое изменение базового URL
- `ConnectionRetryInterceptor` - автоматические повторные попытки при ошибках
- `FullBodyLoggingInterceptor` - подробное логирование запросов/ответов
- `BasicAuthInterceptor` - базовая HTTP авторизация

**Exceptions:**
- `ServerErrorException` - базовое исключение для серверных ошибок
- `ServerErrorWithTypeException` - типизированные серверные ошибки
  - `ErrorType`: MOVE_LIST, DOCUMENT_MOVE, CHANGE_MOVE_STATUS, PRODUCT_SERIES, USER_INFO

**Utilities:**
- `ConnectivityCheckerImpl` - реализация проверки подключения (Android)

**Repository Implementations:**
- `ServerAvailabilityRepositoryImpl` - проверка доступности сервера

---


