# План промптов — Notes & Tasks (тестовое задание Android trainee, осень 2026)

Документ для последовательного скармливания AI-агенту (Claude Code / Cursor / любому другому). Каждый промт — отдельный шаг, зависящий от предыдущих. После каждого шага — сборка проекта и ревью диффа перед переходом дальше. Это же и есть «промпт-план», который ТЗ прямо предлагает приложить к решению.

---

## 1. Архитектурные решения и почему так

| Область | Решение | Почему |
|---|---|---|
| Модульность | multi-module (core/* + feature/*) | явное требование; разделение по слоям снижает связность и ускоряет сборку |
| Архитектура | Clean Architecture, слой presentation — MVVM с однонаправленным потоком данных (единый `UiState` на экран, события через функции/`sealed` intents) | ТЗ разрешает MVVM или MVI — беру MVVM+UDF как компромисс: проще читать, но по сути тот же принцип, что и MVI |
| DI | Hilt | не обязателен по ТЗ, но у тебя уже есть опыт (RPG Map Pet), ускоряет и снижает риск ошибок в графе зависимостей |
| БД | Room | лучше всего ложится на Flow/Compose, есть встроенный полнотекстовый LIKE-поиск без сторонних либ |
| Настройки | Preferences DataStore | штатное решение для примитивных настроек, реактивный Flow "из коробки" |
| Сеть | Retrofit + OkHttp + kotlinx.serialization | указано в стеке первым пунктом; kotlinx.serialization — официальная и не требует reflection |
| Картинки | Photo Picker (`PickVisualMedia`) + Camera (`TakePicture` + FileProvider) + Coil | Photo Picker не требует runtime-разрешения на API 33+, это сильный сигнал понимания Android SDK на собеседовании |
| GigaChat / SaluteSpeech | отдельные core-модули, OAuth2 client credentials, TrustManager под сертификат Минцифры | **у тебя уже есть рабочее решение этой части в gigachatpet (обмен токена, dual-host Retrofit, SSL) — переносим его, а не переписываем заново** |

Проверено по актуальной документации Sber Developers: баланс токенов отдаётся методом `GET /balance` на хосте `gigachat.devices.sberbank.ru/api/v1`, требует Bearer-токена и не работает на pay-as-you-go тарифе (403) — это стоит учесть как отдельное состояние ошибки, а не только "нет сети".

### Дерево модулей

```
app/                         единственный модуль с com.android.application
core/
 ├─ common/                  Result-обёртка, DispatcherProvider, base UseCase<P,R>
 ├─ ui/                      Material3-тема, палитра акцентных цветов, общие composable
 ├─ navigation/              sealed/serializable Route-контракты для трёх графов
 ├─ database/                Room: AppDatabase, NoteEntity, TaskEntity, DAO
 ├─ datastore/                Preferences DataStore: тема, цветовая схема
 ├─ network/                  OkHttp/Retrofit фабрики, общий ApiResult, интерцепторы
 ├─ permissions/              Compose-обёртка над ActivityResultContracts
 ├─ media/                    Photo Picker / Camera / Coil
 ├─ voice/                    запись аудио + клиент SaluteSpeech
 └─ gigachat/                 OAuth2, get-balance, chat-completion (порт из gigachatpet)
feature/
 ├─ notes/                    data+domain+presentation одним модулем (пакетами)
 ├─ tasks/
 └─ settings/
```

Осознанный компромисс: внутри `feature/*` — обычное разделение на пакеты `data/domain/presentation`, а не doubly-nested `:api`/`:impl` подмодули. Это даёт видимую модульность без избыточного времени на бойлерплейт Gradle — если останется время, можно углубить.

Если по времени не успеваешь — сначала можно объединить `permissions` в `media`, а `navigation` — прямо в `app`; остальное трогать не стоит, т.к. это ломает границы data/domain.

### Правила «без хардкода» (напоминать агенту в каждом промте, если он начнёт съезжать)

- Все строки — в `strings.xml`, ни одного текста прямо в `Text("...")`.
- Отступы/размеры — через токены темы (`Spacing` object в `core:ui`), не голые `.dp` в фичах.
- Цвета — только через `MaterialTheme.colorScheme.*`, ни одного `Color(0xFF...)` в feature-модулях.
- Маршруты навигации — типизированные объекты/классы (`@Serializable`), не строки.
- Секреты (client_id/secret GigaChat, ключ SaluteSpeech) — в `local.properties` → `BuildConfig` через Gradle, `local.properties` в `.gitignore`. Ничего похожего на ключ не коммитить.
- Статусы (тема, фильтр задач, состояние записи голоса) — `enum class`/`sealed interface`, не строки/int-флаги.

---

## 2. Как использовать план

Скармливай промты по одному, жди завершения, собирай проект (`./gradlew assembleDebug`), смотри диф. Если агент — Claude Code, стоит сначала попросить его создать `CLAUDE.md` в корне репозитория с содержимым раздела 1 (архитектура + правила против хардкода) — тогда эти правила будут подхватываться автоматически в каждой сессии, и их не придётся повторять в каждом промте руками.

---

## 3. Промты по шагам

### Шаг 0 — репозиторий и Gradle-скелет
```
Инициализируй Android-проект: Kotlin, Jetpack Compose, minSdk 26 / target latest stable,
Gradle version catalog (libs.versions.toml). Создай пустые модули согласно дереву:
app, core:common, core:ui, core:navigation, core:database, core:datastore, core:network,
core:permissions, core:media, core:voice, core:gigachat, feature:notes, feature:tasks,
feature:settings. Подключи их в settings.gradle.kts. В app пока оставь пустой MainActivity
с Hello World на Compose. Настрой .gitignore так, чтобы local.properties и любые файлы
с секретами не попадали в git. Добавь convention-плагин или общий build.gradle.kts для
единых compileSdk/kotlinOptions на все модули, чтобы не дублировать в каждом build.gradle.kts.
```
**Проверить:** проект собирается, модули видны в Project Structure, `local.properties` в `.gitignore`.

### Шаг 1 — core:common
```
В модуле core:common создай: sealed-обёртку для результата операций (Success/Error/Loading
не нужен на уровне domain — Loading живёт во ViewModel), DispatcherProvider (io/default/main)
как интерфейс с default-реализацией через Hilt, базовый интерфейс UseCase<in P, out R> с
suspend operator fun invoke, и набор extension-функций для Flow (например, комбинирование
поиска+сортировки). Никакой Android-зависимости в этом модуле быть не должно — это чистый
Kotlin JVM модуль (java-library, не android-library).
```
**Проверить:** модуль без Android SDK импортов, компилируется как `kotlin(jvm)`.

### Шаг 2 — core:ui (дизайн-система)
```
В core:ui сделай Material3-тему: Theme.kt с поддержкой system/light/dark и функцией применения
акцентного цвета. Определи 5 предустановленных акцентных цветов (seed-цвета) как enum
AccentColor с человекочитаемыми именами и построй для каждого полную светлую и тёмную
ColorScheme (можно вручную через Material Theme Builder экспорт, либо через генерацию
tonal-палитры из одного seed-цвета — выбери сам и обоснуй в комментарии). Добавь Spacing
object с токенами отступов (не хардкодь dp в фичах). Сделай переиспользуемые composable:
LoadingIndicator, ErrorView(message, onRetry), EmptyStateView(text, icon), ConfirmDialog,
и SearchTopBar — поле поиска, которое НЕ фильтрует на каждый символ, а только по нажатию
иконки поиска или клавиши Search на клавиатуре (onSearch callback). Дизайн — стандартные
компоненты Material3, без кастомной графики.
```
**Проверить:** превью в Compose Preview для всех состояний тем и акцентов; SearchTopBar реально не дёргает колбэк на каждый ввод.

### Шаг 3 — core:database
```
В core:database создай Room: NoteEntity (id, title, text, imagePath: String?, createdAt: Long),
TaskEntity (id, title, isCompleted: Boolean, createdAt: Long). DAO для каждой сущности с Flow-
запросами: getAll с параметрами поиска (LIKE по title) и направления сортировки по createdAt.
Для задач — отдельный метод, который в SQL уже кладёт completed ниже active (ORDER BY
isCompleted ASC, потом по createdAt с нужным направлением), чтобы не пересортировывать в
памяти на каждый чих. AppDatabase с двумя DAO, версия 1, без миграций (fallbackToDestructive
на dev-этапе, но зафиксируй TODO с комментарием почему).
```
**Проверить:** DAO-запросы реально не сериализуют сортировку в Kotlin-код, а делают её в SQL; Flow из DAO эмитит новое значение после insert/update/delete.

### Шаг 4 — core:datastore
```
В core:datastore на Preferences DataStore заведи UserSettingsRepository с полями:
themeMode (SYSTEM/LIGHT/DARK), accentColor (ключ из AccentColor из core:ui — но core:ui не
должен зависеть от datastore, поэтому здесь используй строковый ключ + маппинг в feature/app
слое, либо вынеси enum в core:common). Экспортируй Flow<UserSettings> и suspend-методы
setThemeMode/setAccentColor/reset(), где reset возвращает оба поля к дефолтным значениям.
Дефолты не хардкодь в нескольких местах — один source of truth (companion object).
```
**Проверить:** после reset() Flow эмитит именно дефолтные значения, а не старые.

### Шаг 5 — core:network
```
В core:network сделай фабрику OkHttpClient (таймауты, HttpLoggingInterceptor только для
debug-сборки через BuildConfig.DEBUG) и фабрику Retrofit с kotlinx.serialization
converter-factory. Добавь generic ApiResult<T> (Success/HttpError(code)/NetworkError/
UnknownError) и функцию-обёртку safeApiCall, которая ловит IOException/HttpException/
SerializationException и мапит их в ApiResult, а не пробрасывает наверх как есть.
```
**Проверить:** отключение сети действительно даёт NetworkError, а не краш; логи видны только в debug build type.

### Шаг 6 — core:permissions
```
В core:permissions сделай Compose-friendly обёртку над ActivityResultContracts для
CAMERA и RECORD_AUDIO (для медиатеки на API 33+ разрешение не нужно — используется Photo
Picker, учти это отдельной веткой для старых API, где нужен READ_MEDIA_IMAGES /
READ_EXTERNAL_STORAGE). Экспортируй PermissionState: Granted / Denied / PermanentlyDenied.
Для PermanentlyDenied предусмотри composable-баннер с кнопкой перехода в системные настройки
приложения (Settings.ACTION_APPLICATION_DETAILS_SETTINGS) — это явно требуется в ТЗ
("обрабатывать ситуацию, когда пользователь не предоставил разрешение").
```
**Проверить:** повторный явный отказ (без "не спрашивать снова") даёт Denied с возможностью повторного запроса, а не тишину.

### Шаг 7 — core:media
```
В core:media реализуй выбор изображения через PickVisualMedia (без разрешения на API 33+)
и съёмку через TakePicture + FileProvider (создай file_paths.xml и провайдер в манифесте
app-модуля, но саму логику держи здесь). Полученный Uri скопируй в files dir приложения
(internal storage), чтобы он не протух после закрытия picker'а — верни стабильный путь для
сохранения в БД. Добавь Coil и composable NotePreviewImage(path: String?, placeholder),
который рисует заглушку, если path == null или файл не найден.
```
**Проверить:** после выбора фото и перезапуска приложения превью не пропадает (файл реально скопирован, а не временный Uri).

### Шаг 8 — core:voice
```
В core:voice сделай VoiceRecorder на MediaRecorder: start()/stop(), Flow<RecordingState>
(Idle/Recording/Error), сохранение в temp-файл в cacheDir. Отдельно — клиент SaluteSpeech:
сначала изучи страницу https://developers.sber.ru/docs/ru/salutespeech/overview на предмет
точной схемы авторизации (вероятно тот же OAuth2 client credentials, что и у GigaChat, но
проверь scope и хост) и на предмет требуемого формата аудио для синхронного распознавания
короткой речи — не придумывай формат, посмотри в доке. Экспортируй suspend-функцию
recognize(file): ApiResult<String>.
```
**Проверить:** ошибка распознавания и ошибка сети различимы в UI (не одно общее "что-то пошло не так").

### Шаг 9 — core:gigachat
```
Перенеси из pet-проекта gigachatpet рабочее решение для GigaChat: обмен client_id/secret
(Authorization key) на access-токен через OAuth2 client credentials на хосте
ngw.devices.sberbank.ru, кэширование и обновление токена, и конфигурацию TrustManager под
цепочку сертификатов Минцифры (dual-host: отдельно auth-хост и api-хост
gigachat.devices.sberbank.ru). Не переписывай эту часть с нуля — адаптируй под Hilt/
core:network. Добавь новый метод getBalance(): ApiResult<List<BalanceEntry>> (GET /balance) —
учти, что этот метод вернёт 403 на pay-as-you-go тарифе, это отдельная ветка ApiResult, а
не общий сетевой сбой. Добавь метод refineTaskText(rawText: String): ApiResult<String> для
голосового создания задач через chat completion.
```
**Проверить:** секреты не захардкожены (BuildConfig из local.properties), 403 от /balance показывает пользователю осмысленное сообщение, а не "ошибка сети".

### Шаг 10 — core:navigation
```
В core:navigation опиши типизированные (kotlinx.serialization @Serializable) маршруты:
NotesList, NoteEditor(noteId: Long?), TasksList, SettingsScreen, плюс три top-level route
для Bottom Navigation (NotesTab/TasksTab/SettingsTab). Никаких строковых route-путей руками.
Feature-модули не должны знать друг о друге — каждый feature-модуль экспортирует одну
composable-функцию-граф (например NotesFeatureGraph(navController)), а app-модуль их
объединяет под общим NavHost и Scaffold с NavigationBar.
```
**Проверить:** feature:notes и feature:tasks не имеют gradle-зависимости друг на друга.

### Шаг 11 — feature:notes
```
Реализуй feature:notes целиком: domain (Note, NotesRepository интерфейс, GetNotesUseCase
с параметрами query+sortOrder, SaveNoteUseCase с валидацией непустого заголовка,
DeleteNoteUseCase), data (NotesRepositoryImpl поверх core:database DAO + мапперы),
presentation — два экрана:
1) NotesListScreen: FAB создания, LazyColumn с превью/заглушкой/названием/датой,
   SearchTopBar, кнопка сортировки (DropdownMenu: "сначала новые"/"сначала старые"),
   переключатель режима удаления — в этом режиме на айтемах появляется кнопка удаления,
   а обычный тап по айтему должен игнорироваться (не открывать редактор).
2) NoteEditorScreen: поле заголовка (обязательное) и текста (необязательное), добавление
   фото из галереи или камеры (core:media + core:permissions), удаление выбранного фото,
   кнопка голосового ввода (core:voice) — распознанный текст ДОБАВЛЯЕТСЯ в поле текста,
   не заменяет его. Кнопка "Сохранить" валидирует заголовок и возвращает на список. Состояния
   записи/обработки/ошибки голосового ввода отобрази на самом экране редактора.
Используй один UiState data class на ViewModel, без разрозненных boolean-полей.
```
**Проверить:** режим удаления реально блокирует переход в редактор; пустой заголовок не даёт сохранить с понятной ошибкой; список обновляется сразу после сохранения (через Flow, без ручного refresh).

### Шаг 12 — feature:tasks
```
Реализуй feature:tasks: domain (Task, TasksRepository, GetTasksUseCase с query+filter+sort,
ToggleTaskStatusUseCase, CreateTaskUseCase, CreateTaskFromVoiceUseCase — вызывает
core:voice.recognize, затем core:gigachat.refineTaskText, затем создаёт задачу),
data (TasksRepositoryImpl), presentation — TasksScreen с App Bar, SearchTopBar, фильтром
статуса (Все/Активные/Выполненные — сегменты, не Dropdown), списком и FAB. По нажатию на
FAB покажи два компактных действия (текст/голос) — не полноэкранный диалог и не Bottom
Sheet (в ТЗ это прямо запрещено для текстового создания): например расходящийся FAB-меню
на два мини-FAB. Текстовый режим: прямо в списке появляется новая строка с активным полем
ввода и автофокусом, по завершении (IME Done) — валидация непустого текста, сохранение,
возврат строки в обычный вид. Голосовой режим: показывает состояния запись/обработка/ошибка
поверх списка, во время обработки — индикатор загрузки. Выполненные задачи — уменьшенная
прозрachность и зачёркнутый текст, отображаются ниже активных при любой сортировке.
Предусмотри состояние пустого списка отдельно от состояния ошибки.
```
**Проверить:** все состояния из ТЗ реально присутствуют (список / пусто / текстовый ввод / запись / обработка / ошибка); чекбокс обновляет и сохраняет статус мгновенно; completed всегда ниже active независимо от направления сортировки.

### Шаг 13 — feature:settings
```
Реализуй feature:settings: SettingsViewModel комбинирует Flow настроек (core:datastore) и
состояние баланса GigaChat (Idle/Loading/Success(list)/Error) из core:gigachat. Экран:
блок баланса токенов с тремя состояниями, выбор темы (радиокнопки система/светлая/тёмная) —
применяется сразу без пересоздания Activity, выбор акцентного цвета (свотчи из core:ui
AccentColor), кнопка "Сбросить настройки" вызывающая reset() у репозитория настроек.
```
**Проверить:** смена темы/цвета отражается на других вкладках без перезапуска приложения; после Reset тема/цвет реально возвращаются к дефолту, а не только на экране настроек.

### Шаг 14 — app: сборка приложения
```
В app подключи Hilt (@HiltAndroidApp, MainActivity с @AndroidEntryPoint), собери Scaffold с
NavigationBar на три вкладки (Notes/Tasks/Settings) и общий NavHost, объединяющий графы
feature-модулей из core:navigation. Оберни содержимое в тему из core:ui, подписанную на
Flow настроек из core:datastore (через ViewModel уровня app), чтобы смена темы/цвета
применялась глобально сразу.
```
**Проверить:** холодный запуск восстанавливает последнюю выбранную тему/цвет из DataStore; переключение вкладок не теряет состояние списков (сохраняется при возврате).

### Шаг 15 — ревью-проход на соответствие ТЗ и чистоту кода
```
Пройдись по всему проекту и проверь: 1) нет ли хардкод-строк вне strings.xml, хардкод-цветов
вне MaterialTheme.colorScheme, хардкод-dp вне Spacing; 2) у каждого сетевого/БД вызова есть
обработка ошибки, показываемая пользователю, а не проглатываемая; 3) permission-denied
сценарии не крашат экран; 4) при повороте экрана / process death черновик заметки в редакторе
не теряется (используй SavedStateHandle или rememberSaveable для полей ввода); 5) индикаторы
загрузки показываются на всех долгих операциях (сеть, распознавание речи, GigaChat).
Исправь найденное.
```
**Проверить:** пройтись по чек-листу раздела 4 ниже вручную, отметить что не покрыто.

### Шаг 16 (опционально, если есть время) — unit-тесты
```
Добавь unit-тесты для ключевых use case (валидация непустого заголовка/текста задачи,
сортировка с completed ниже active) и для одного ViewModel на fake-репозитории
(MockK + Turbine для Flow). Не покрывай тестами Compose UI — фокус на domain/presentation
логике.
```

### Шаг 17 — README
```
Составь README.md: краткое описание архитектуры (ссылка на дерево модулей), что из ТЗ
реализовано полностью, что частично, что не реализовано, что бы сделал с дополнительным
временем, и явно укажи, что этот проект написан с помощью AI-агента по прилагаемому
prompt-плану (ai-prompts-plan.md).
```

---

## 4. Чек-лист покрытия ТЗ

| Требование ТЗ | Где реализовано |
|---|---|
| Bottom Navigation, 3 вкладки | Шаг 10, 14 |
| Список заметок: FAB, поиск, сортировка, режим удаления | Шаг 11 |
| Заметка: заголовок обязателен, картинка из галереи/камеры, голосовой ввод | Шаг 11, 7, 8 |
| Задачи: поиск, фильтр статуса, чекбокс, completed ниже active | Шаг 12 |
| Создание задачи текстом инлайн без экрана/шита | Шаг 12 |
| Голосовое создание задачи через GigaChat | Шаг 8, 9, 12 |
| Настройки: тема без перезапуска, цветовая схема, сброс | Шаг 13 |
| Баланс GigaChat: загрузка/успех/ошибка | Шаг 9, 13 |
| Разрешения камера/файлы/микрофон + обработка отказа | Шаг 6, 7, 8 |
| Данные переживают перезапуск (Room + DataStore) | Шаг 3, 4 |
| Navigation Compose | Шаг 10, 14 |
| Поиск по кнопке, а не по каждому символу | Шаг 2 (SearchTopBar) |

## 5. На что смотрит проверяющий → где это видно

- **Корректность сценариев / edge cases** — Шаг 15, чек-листы после каждого шага.
- **Качество и читаемость кода** — единый `UiState`, `UseCase`-слой, отсутствие хардкода (раздел 1).
- **Архитектура** — модульная структура (раздел 1), явные границы data/domain/presentation.
- **UI state** — Шаг 12 (явные состояния списка задач), Шаг 2 (единый подход к Loading/Error/Empty).
- **Асинхронность** — Flow из Room везде, coroutines в use case, safeApiCall (Шаг 5).
- **Ошибки** — ApiResult, отдельная ветка для 403 у get-balance (Шаг 9), permission denied (Шаг 6).
- **Локальное хранилище** — Room + DataStore, реактивность (Шаг 3, 4).
- **Интеграция с внешними API** — перенос уже проверенного решения для GigaChat (Шаг 9), это стоит явно проговорить на собеседовании как обоснованное решение, а не "заново написал".
- **Обоснованность решений** — таблица в разделе 1 и README (Шаг 17).

## 6. Опциональные улучшения (если останется время, по приоритету)

1. Подтверждение перед удалением заметки — дешевле всего, один `ConfirmDialog` уже есть с Шага 2.
2. Splash Screen — `androidx.core.splashscreen`, 15 минут работы.
3. Список/сетка для заметок — переключатель `LazyColumn`/`LazyVerticalGrid` на общем `UiState`.
4. Поделиться заметкой через `Intent.ACTION_SEND`.
5. Стилизация текста (жирный/курсив/подчёркнутый) — самое дорогое по времени, делать последним.
6. Пагинация — только если заметок предполагается действительно много; иначе явно избыточно для тестового.

Не пытайся закрыть все пункты — ТЗ прямо говорит, что качество важнее полноты бонусов.
