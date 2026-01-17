# Hytale Server-Side Modding Guide

A practical guide to creating server-side plugins for Hytale based on hands-on development experience.

## Table of Contents

1. [Project Setup](#project-setup)
2. [Plugin Structure](#plugin-structure)
3. [Commands](#commands)
4. [Events](#events)
5. [Working with Players](#working-with-players)
6. [Map and Compass Markers](#map-and-compass-markers)
7. [Useful APIs](#useful-apis)
8. [Tips and Pitfalls](#tips-and-pitfalls)

---

## Project Setup

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

    // Gson for JSON (already included in Hytale)
    compileOnly("com.google.code.gson:gson:2.10.1")
}

tasks.jar {
    archiveBaseName.set("MyPlugin")
}
```

### Folder Structure

```
my-plugin/
├── libs/
│   └── HytaleServer.jar      # Copy from server
├── src/main/
│   ├── java/com/example/myplugin/
│   │   └── MyPlugin.java
│   └── resources/
│       └── manifest.json
├── build.gradle.kts
└── settings.gradle.kts
```

---

## Plugin Structure

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

### Main Plugin Class

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
        // Called when plugin is loading
        getLogger().at(Level.INFO).log("Plugin setup...");

        // Register commands
        getCommandRegistry().registerCommand(new MyCommand());

        // Register events
        getEventRegistry().register(SomeEvent.class, this::onEvent);
    }

    @Override
    protected void start() {
        // Called after all plugins are set up
        getLogger().at(Level.INFO).log("Plugin started!");
    }

    @Override
    protected void shutdown() {
        // Called when server is stopping
        getLogger().at(Level.INFO).log("Plugin shutdown...");
    }
}
```

---

## Commands

### Synchronous Command

```java
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.Message;
import java.awt.Color;

public class MyCommand extends CommandBase {

    public MyCommand() {
        super("mycommand", "Description of command");

        // Add subcommands
        addSubCommand(new MySubCommand());
    }

    @Override
    protected void executeSync(CommandContext ctx) {
        ctx.sendMessage(Message.raw("Hello!").color(Color.GREEN));
    }

    @Override
    protected boolean canGeneratePermission() {
        return false; // false = everyone can use
    }
}
```

### Asynchronous Command with Arguments

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

        // Execute in world thread (thread-safe)
        Ref<EntityStore> ref = player.getReference();
        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        world.execute(() -> {
            // Code here runs in world thread
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            // ... logic
        });

        return CompletableFuture.completedFuture(null);
    }
}
```

---

## Events

### Registering Events

```java
// In plugin setup():

// Event for specific world/context
getEventRegistry().register(PlayerDisconnectEvent.class, this::onPlayerDisconnect);

// Global event
getEventRegistry().registerGlobal(AddWorldEvent.class, this::onWorldAdded);
```

### Event Handlers

```java
private void onPlayerDisconnect(PlayerDisconnectEvent event) {
    PlayerRef playerRef = event.getPlayerRef();
    if (playerRef == null) return;

    UUID uuid = playerRef.getUuid();
    // Handle player disconnect
}

private void onWorldAdded(AddWorldEvent event) {
    World world = event.getWorld();
    // Register providers for new world
    world.getWorldMapManager().addMarkerProvider("myMarkers", MyMarkerProvider.INSTANCE);
}
```

---

## Working with Players

### Getting Player Data

```java
// From CommandContext
Player player = (Player) ctx.sender();

// Player UUID (deprecated but works)
UUID uuid = player.getUuid();

// PlayerRef - main way to work with players
PlayerRef playerRef = player.getPlayerRef(); // deprecated
// Or through store:
PlayerRef playerRef = store.getComponent(reference, PlayerRef.getComponentType());

// Player name
String name = playerRef.getUsername();

// Position
Transform transform = playerRef.getTransform();
Vector3d position = transform.getPosition();

// Send message
playerRef.sendMessage(Message.raw("Hello!").color(Color.GREEN));
```

### Finding Player by UUID

```java
import com.hypixel.hytale.server.core.universe.Universe;

PlayerRef playerRef = Universe.get().getPlayer(uuid);
if (playerRef != null && playerRef.isValid()) {
    // Player is online
}
```

### All Players in World

```java
// All PlayerRefs in world
Collection<PlayerRef> playerRefs = world.getPlayerRefs();

// All Player objects (deprecated)
List<Player> players = world.getPlayers();
```

---

## Map and Compass Markers

### MarkerProvider for Compass

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

        // Create marker
        MapMarker marker = new MapMarker(
            "unique-marker-id",           // Marker ID
            "Marker Name",                // Display name
            "Player.png",                 // Icon (Player.png, Spawn.png, Death.png, Home.png, Warp.png)
            PositionUtil.toTransformPacket(transform),  // Position
            null                          // Context menu (null = none)
        );

        // IMPORTANT: viewRadius = -1 to show on COMPASS
        tracker.trySendMarker(
            -1,              // -1 = show on compass, >0 = map only within radius
            playerChunkX,
            playerChunkZ,
            marker
        );
    }
}
```

### Registering Provider

```java
// When world is added
private void onWorldAdded(AddWorldEvent event) {
    event.getWorld().getWorldMapManager()
        .addMarkerProvider("myPlugin:markers", MyMarkerProvider.INSTANCE);
}
```

### Player Map Filter

```java
// Exclude certain players from default map icons
player.getWorldMapTracker().setPlayerMapFilter(playerRef -> {
    // return true = hide this player
    // return false = show
    return shouldHide(playerRef.getUuid());
});

// Reset filter
player.getWorldMapTracker().setPlayerMapFilter(null);
```

---

## Useful APIs

### Sending Notifications

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

### Playing Sounds

```java
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.protocol.SoundCategory;

SoundUtil.playSoundEvent2dToPlayer(
    playerRef,
    soundIndex,           // Sound index
    SoundCategory.UI,     // Category
    1.0f,                 // Volume
    1.0f                  // Pitch
);
```

### Working with World

```java
// Get world from Player
World world = player.getWorld();

// Execute code in world thread (thread-safe)
world.execute(() -> {
    // Safe code here
});

// World name
String worldName = world.getName();
```

---

## Tips and Pitfalls

### Thread Safety

```java
// WRONG - may cause race condition
public void onCommand(CommandContext ctx) {
    Player player = (Player) ctx.sender();
    PlayerRef ref = player.getPlayerRef();
    ref.sendMessage(...); // May be unsafe
}

// CORRECT - execute in world thread
public void onCommand(CommandContext ctx) {
    Player player = (Player) ctx.sender();
    Ref<EntityStore> reference = player.getReference();
    Store<EntityStore> store = reference.getStore();
    World world = store.getExternalData().getWorld();

    world.execute(() -> {
        PlayerRef playerRef = store.getComponent(reference, PlayerRef.getComponentType());
        playerRef.sendMessage(...); // Safe
    });
}
```

### Deprecated Methods

Many methods are marked deprecated but still work:
- `player.getUuid()` → works
- `player.getPlayerRef()` → works
- `world.getPlayers()` → works

### Available Marker Icons

Built-in icons (from client):
- `Player.png` - player icon
- `Spawn.png` - spawn point
- `Death.png` - death location
- `Home.png` - home/respawn
- `Warp.png` - warp/teleport

Icon color cannot be changed without client-side modifications.

### CustomUIHud

**DOES NOT WORK** without client assets! Requires `.ui` files on client.

```java
// This will crash the client:
builder.append("body", "<div>Test</div>");

// CustomUIHud only works with existing client .ui templates
```

### Data Persistence

Hytale doesn't provide a built-in API for plugin data storage. Use JSON:

```java
// Save
Path file = Path.of("mods", "MyPlugin", "data.json");
Files.createDirectories(file.getParent());
try (Writer w = new FileWriter(file.toFile())) {
    new Gson().toJson(data, w);
}

// Load
try (Reader r = new FileReader(file.toFile())) {
    data = new Gson().fromJson(r, DataClass.class);
}
```

---

## Useful Classes

| Class | Description |
|-------|-------------|
| `JavaPlugin` | Base plugin class |
| `CommandBase` | Synchronous command |
| `AbstractAsyncCommand` | Asynchronous command |
| `Player` | Player entity |
| `PlayerRef` | Player reference (main API) |
| `World` | Game world |
| `WorldMapTracker` | Player's map tracker |
| `WorldMapManager` | World map manager |
| `MapMarker` | Map/compass marker |
| `Universe` | Global server access |
| `Message` | Create formatted messages |

---

## Build and Deploy

```bash
# Build
./gradlew build

# JAR will be in build/libs/MyPlugin-1.0.0.jar

# Copy to server
scp build/libs/MyPlugin-1.0.0.jar user@server:/opt/hytale-data/mods/

# Restart server
docker restart hytale
```

---

*Guide based on SLParty plugin development. Hytale API may change in future versions.*
