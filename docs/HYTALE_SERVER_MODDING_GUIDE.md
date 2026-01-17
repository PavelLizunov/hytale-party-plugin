# Hytale Server-Side Modding Guide

Краткий гайд по созданию серверных плагинов для Hytale на основе практического опыта.

## Содержание

1. [Настройка проекта](#настройка-проекта)
2. [Структура плагина](#структура-плагина)
3. [Команды](#команды)
4. [События](#события)
5. [Работа с игроками](#работа-с-игроками)
6. [Маркеры на карте и компасе](#маркеры-на-карте-и-компасе)
7. [Полезные API](#полезные-api)
8. [Советы и подводные камни](#советы-и-подводные-камни)

---

## Настройка проекта

### build.gradle.kts

```kotlin
plugins {
    java
}

group = "com.example"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(files("libs/HytaleServer.jar"))

    // Gson для JSON (уже есть в Hytale)
    compileOnly("com.google.code.gson:gson:2.10.1")
}

tasks.jar {
    archiveBaseName.set("MyPlugin")
}
```

### Структура папок

```
my-plugin/
├── libs/
│   └── HytaleServer.jar      # Скопировать с сервера
├── src/main/
│   ├── java/com/example/myplugin/
│   │   └── MyPlugin.java
│   └── resources/
│       └── manifest.json
├── build.gradle.kts
└── settings.gradle.kts
```

---

## Структура плагина

### manifest.json

```json
{
  "formatVersion": 1,
  "id": "com.example:MyPlugin",
  "name": "MyPlugin",
  "version": "1.0.0",
  "entrypoints": {
    "main": "com.example.myplugin.MyPlugin"
  },
  "dependencies": []
}
```

### Главный класс плагина

```java
package com.example.myplugin;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import javax.annotation.Nonnull;
import java.util.logging.Level;

public class MyPlugin extends JavaPlugin {

    private static MyPlugin INSTANCE;

    public MyPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        INSTANCE = this;
    }

    public static MyPlugin getInstance() {
        return INSTANCE;
    }

    @Override
    protected void setup() {
        // Вызывается при загрузке плагина
        getLogger().at(Level.INFO).log("Plugin setup...");

        // Регистрация команд
        getCommandRegistry().registerCommand(new MyCommand());

        // Регистрация событий
        getEventRegistry().register(SomeEvent.class, this::onEvent);
    }

    @Override
    protected void start() {
        // Вызывается после setup всех плагинов
        getLogger().at(Level.INFO).log("Plugin started!");
    }

    @Override
    protected void shutdown() {
        // Вызывается при остановке сервера
        getLogger().at(Level.INFO).log("Plugin shutdown...");
    }
}
```

---

## Команды

### Синхронная команда

```java
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.Message;
import java.awt.Color;

public class MyCommand extends CommandBase {

    public MyCommand() {
        super("mycommand", "Description of command");

        // Добавление подкоманд
        addSubCommand(new MySubCommand());
    }

    @Override
    protected void executeSync(CommandContext ctx) {
        ctx.sendMessage(Message.raw("Hello!").color(Color.GREEN));
    }

    @Override
    protected boolean canGeneratePermission() {
        return false; // false = все могут использовать
    }
}
```

### Асинхронная команда с аргументами

```java
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class MyAsyncCommand extends AbstractAsyncCommand {

    private final RequiredArg<PlayerRef> targetArg;

    public MyAsyncCommand() {
        super("invite", "Invite a player");
        this.targetArg = withRequiredArg("player", "Target player", ArgTypes.PLAYER_REF);
    }

    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("Only players!").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        Player player = (Player) ctx.sender();
        PlayerRef target = ctx.get(targetArg);

        if (target == null) {
            ctx.sendMessage(Message.raw("Player not found!").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        // Выполнение в мире (thread-safe)
        Ref<EntityStore> ref = player.getReference();
        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        world.execute(() -> {
            // Код здесь выполняется в потоке мира
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            // ... логика
        });

        return CompletableFuture.completedFuture(null);
    }
}
```

---

## События

### Регистрация событий

```java
// В setup() плагина:

// Событие для конкретного мира/контекста
getEventRegistry().register(PlayerDisconnectEvent.class, this::onPlayerDisconnect);

// Глобальное событие
getEventRegistry().registerGlobal(AddWorldEvent.class, this::onWorldAdded);
```

### Обработчики

```java
private void onPlayerDisconnect(PlayerDisconnectEvent event) {
    PlayerRef playerRef = event.getPlayerRef();
    if (playerRef == null) return;

    UUID uuid = playerRef.getUuid();
    // Обработка выхода игрока
}

private void onWorldAdded(AddWorldEvent event) {
    World world = event.getWorld();
    // Регистрация провайдеров для нового мира
    world.getWorldMapManager().addMarkerProvider("myMarkers", MyMarkerProvider.INSTANCE);
}
```

---

## Работа с игроками

### Получение данных игрока

```java
// Из CommandContext
Player player = (Player) ctx.sender();

// UUID игрока (deprecated но работает)
UUID uuid = player.getUuid();

// PlayerRef - основной способ работы с игроком
PlayerRef playerRef = player.getPlayerRef(); // deprecated
// Или через store:
PlayerRef playerRef = store.getComponent(reference, PlayerRef.getComponentType());

// Имя игрока
String name = playerRef.getUsername();

// Позиция
Transform transform = playerRef.getTransform();
Vector3d position = transform.getPosition();

// Отправка сообщения
playerRef.sendMessage(Message.raw("Hello!").color(Color.GREEN));
```

### Поиск игрока по UUID

```java
import com.hypixel.hytale.server.core.universe.Universe;

PlayerRef playerRef = Universe.get().getPlayer(uuid);
if (playerRef != null && playerRef.isValid()) {
    // Игрок онлайн
}
```

### Все игроки в мире

```java
// Все PlayerRef в мире
Collection<PlayerRef> playerRefs = world.getPlayerRefs();

// Все Player объекты (deprecated)
List<Player> players = world.getPlayers();
```

---

## Маркеры на карте и компасе

### MarkerProvider для компаса

```java
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager;
import com.hypixel.hytale.protocol.packets.worldmap.MapMarker;

public class MyMarkerProvider implements WorldMapManager.MarkerProvider {

    public static final MyMarkerProvider INSTANCE = new MyMarkerProvider();

    @Override
    public void update(World world, GameplayConfig config,
                       WorldMapTracker tracker, int viewRadius,
                       int playerChunkX, int playerChunkZ) {

        Player viewer = tracker.getPlayer();
        if (viewer == null) return;

        // Создание маркера
        MapMarker marker = new MapMarker(
            "unique-marker-id",           // ID маркера
            "Marker Name",                // Отображаемое имя
            "Player.png",                 // Иконка (Player.png, Spawn.png, Death.png, Home.png, Warp.png)
            PositionUtil.toTransformPacket(transform),  // Позиция
            null                          // Context menu (null = нет)
        );

        // ВАЖНО: viewRadius = -1 для отображения на КОМПАСЕ
        tracker.trySendMarker(
            -1,              // -1 = показывать на компасе, >0 = только на карте в радиусе
            playerChunkX,
            playerChunkZ,
            marker
        );
    }
}
```

### Регистрация провайдера

```java
// При добавлении мира
private void onWorldAdded(AddWorldEvent event) {
    event.getWorld().getWorldMapManager()
        .addMarkerProvider("myPlugin:markers", MyMarkerProvider.INSTANCE);
}
```

### Фильтр игроков на карте

```java
// Исключить определённых игроков из стандартных иконок на карте
player.getWorldMapTracker().setPlayerMapFilter(playerRef -> {
    // return true = скрыть этого игрока
    // return false = показать
    return shouldHide(playerRef.getUuid());
});

// Сбросить фильтр
player.getWorldMapTracker().setPlayerMapFilter(null);
```

---

## Полезные API

### Отправка уведомлений

```java
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;

NotificationUtil.sendNotification(
    playerRef.getPacketHandler(),
    Message.raw("Title"),
    Message.raw("Subtitle"),
    "",  // Icon path (optional)
    null, // Item stack (optional)
    NotificationStyle.Success  // Success, Warning, Error
);
```

### Воспроизведение звука

```java
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.protocol.SoundCategory;

SoundUtil.playSoundEvent2dToPlayer(
    playerRef,
    soundIndex,           // Индекс звука
    SoundCategory.UI,     // Категория
    1.0f,                 // Volume
    1.0f                  // Pitch
);
```

### Работа с миром

```java
// Получить мир из Player
World world = player.getWorld();

// Выполнить код в потоке мира (thread-safe)
world.execute(() -> {
    // Безопасный код
});

// Имя мира
String worldName = world.getName();
```

---

## Советы и подводные камни

### Thread Safety

```java
// НЕПРАВИЛЬНО - может вызвать race condition
public void onCommand(CommandContext ctx) {
    Player player = (Player) ctx.sender();
    PlayerRef ref = player.getPlayerRef();
    ref.sendMessage(...); // Может быть небезопасно
}

// ПРАВИЛЬНО - выполнение в потоке мира
public void onCommand(CommandContext ctx) {
    Player player = (Player) ctx.sender();
    Ref<EntityStore> reference = player.getReference();
    Store<EntityStore> store = reference.getStore();
    World world = store.getExternalData().getWorld();

    world.execute(() -> {
        PlayerRef playerRef = store.getComponent(reference, PlayerRef.getComponentType());
        playerRef.sendMessage(...); // Безопасно
    });
}
```

### Deprecated методы

Многие методы помечены как deprecated, но работают. Основные:
- `player.getUuid()` → работает
- `player.getPlayerRef()` → работает
- `world.getPlayers()` → работает

### Доступные иконки маркеров

Встроенные иконки (из клиента):
- `Player.png` - иконка игрока
- `Spawn.png` - точка спавна
- `Death.png` - место смерти
- `Home.png` - дом/респавн
- `Warp.png` - варп/телепорт

Цвет иконки изменить нельзя без клиентских модификаций.

### CustomUIHud

**НЕ РАБОТАЕТ** без клиентских ассетов! Требует `.ui` файлы в клиенте.

```java
// Это вызовет краш клиента:
builder.append("body", "<div>Test</div>");

// CustomUIHud работает только с существующими .ui шаблонами клиента
```

### Персистентность данных

Hytale не предоставляет встроенного API для сохранения данных плагина. Используйте JSON:

```java
// Сохранение
Path file = Path.of("mods", "MyPlugin", "data.json");
Files.createDirectories(file.getParent());
try (Writer w = new FileWriter(file.toFile())) {
    new Gson().toJson(data, w);
}

// Загрузка
try (Reader r = new FileReader(file.toFile())) {
    data = new Gson().fromJson(r, DataClass.class);
}
```

---

## Полезные классы

| Класс | Описание |
|-------|----------|
| `JavaPlugin` | Базовый класс плагина |
| `CommandBase` | Синхронная команда |
| `AbstractAsyncCommand` | Асинхронная команда |
| `Player` | Сущность игрока |
| `PlayerRef` | Ссылка на игрока (основной API) |
| `World` | Игровой мир |
| `WorldMapTracker` | Трекер карты игрока |
| `WorldMapManager` | Менеджер карты мира |
| `MapMarker` | Маркер на карте/компасе |
| `Universe` | Глобальный доступ к серверу |
| `Message` | Создание сообщений с форматированием |

---

## Сборка и деплой

```bash
# Сборка
./gradlew build

# JAR будет в build/libs/MyPlugin-1.0.0.jar

# Копирование на сервер
scp build/libs/MyPlugin-1.0.0.jar user@server:/opt/hytale-data/mods/

# Перезапуск сервера
docker restart hytale
```

---

*Гайд основан на разработке SLParty плагина. Hytale API может измениться в будущих версиях.*
