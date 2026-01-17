# SLParty

Party system plugin for Hytale servers. Allows players to create groups and see teammates on the compass and world map.

## Features

- **Party Management** - Create, join, leave, and disband parties
- **Compass Tracking** - See party members on the compass with `[Party]` prefix
- **Map Markers** - Party members appear on the world map
- **Invitations** - Invite players or make party public for anyone to join
- **Party Chat** - Send messages to all party members
- **Persistence** - Parties are saved to disk and persist across server restarts and player reconnects

## Commands

| Command | Description |
|---------|-------------|
| `/party` | Show help and available commands |
| `/party create` | Create a new party |
| `/party info` | Show information about your current party |
| `/party invite <player>` | Invite a player to your party |
| `/party join <leader>` | Join a player's party |
| `/party leave` | Leave your current party |
| `/party disband` | Disband your party (leader only) |
| `/party public` | Toggle party between public/private |
| `/party chat <message>` | Send a message to all party members |

## Requirements

- Java 21+
- Hytale Server
- Gradle 8.x+

## Building

1. Place the Hytale server JAR in the `libs/` folder:
   ```bash
   cp /path/to/HytaleServer.jar libs/
   ```

2. Build the plugin:
   ```bash
   ./gradlew build
   ```

3. Find the JAR in `build/libs/SLParty-1.0.0.jar`

## Installation

Copy the built JAR to your Hytale server's `mods/` directory:

```bash
cp build/libs/SLParty-1.0.0.jar /path/to/hytale-server/mods/
```

Restart the server.

## Configuration

### Party Storage

Parties are automatically saved to `mods/SLParty/parties.json`. The file is created automatically when the first party is created.

Example structure:
```json
[
  {
    "id": "uuid-of-party",
    "leaderId": "uuid-of-leader",
    "members": ["uuid-1", "uuid-2"],
    "isPublic": false
  }
]
```

## Project Structure

```
SLParty/
├── src/main/java/com/sl/party/
│   ├── SLPartyPlugin.java              # Main plugin class
│   ├── cache/
│   │   └── PartyCache.java             # In-memory party cache with persistence
│   ├── command/
│   │   ├── PartyCommand.java           # Main command handler
│   │   └── impl/                       # Subcommand implementations
│   │       ├── PartyCreateSubCommand.java
│   │       ├── PartyJoinSubCommand.java
│   │       ├── PartyLeaveSubCommand.java
│   │       ├── PartyDisbandSubCommand.java
│   │       ├── PartyInviteSubCommand.java
│   │       ├── PartyInfoSubCommand.java
│   │       ├── PartyPublicSubCommand.java
│   │       └── PartyChatSubCommand.java
│   ├── compass/
│   │   └── PartyMemberMarkerProvider.java  # Compass/map marker provider
│   ├── listener/
│   │   └── PartyMapFilterListener.java     # Map filter for deduplication
│   ├── messages/
│   │   └── MessagesConfig.java             # Message constants
│   ├── model/
│   │   └── Party.java                      # Party data model
│   └── storage/
│       └── PartyStorage.java               # JSON persistence
├── src/main/resources/
│   └── manifest.json                       # Plugin manifest
├── docs/
│   └── HYTALE_SERVER_MODDING_GUIDE.md      # Server modding guide
├── libs/                                   # Hytale server JAR (not committed)
├── build.gradle.kts
└── settings.gradle.kts
```

## Technical Details

### Compass Markers

Party members are displayed on the compass using the `WorldMapManager.MarkerProvider` interface:

- Shows all party members with `[Party]` prefix
- Uses `viewRadius = -1` to ensure markers appear on compass
- Filters out party members from default player icons to prevent duplication on map

### Persistence

- Parties persist across server restarts
- Players remain in their party after disconnecting and reconnecting
- Data is saved automatically on any party change

## Documentation

See [Hytale Server-Side Modding Guide](docs/HYTALE_SERVER_MODDING_GUIDE.md) for a comprehensive guide on creating Hytale server plugins.

## Known Limitations

- Marker icon color cannot be changed without client-side assets (uses default `Player.png`)

## License

MIT

## Author

SL
