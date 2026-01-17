package com.sl.party;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.world.events.AddWorldEvent;
import com.sl.party.cache.PartyCache;
import com.sl.party.command.PartyCommand;
import com.sl.party.compass.PartyMemberMarkerProvider;
import com.sl.party.storage.PartyStorage;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.logging.Level;

/**
 * SLParty - Party system plugin for Hytale
 * Allows players to create groups and see teammates on the world map
 */
public class SLPartyPlugin extends JavaPlugin {

    private static SLPartyPlugin INSTANCE;
    private PartyCache partyCache;

    public SLPartyPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        INSTANCE = this;
    }

    public static SLPartyPlugin getInstance() {
        return INSTANCE;
    }

    @Override
    protected void setup() {
        getLogger().at(Level.INFO).log("SLParty is setting up...");

        // Initialize party cache with storage
        this.partyCache = new PartyCache();

        // Setup persistence - save to mods/SLParty folder
        Path dataFolder = Path.of("mods", "SLParty");
        PartyStorage storage = new PartyStorage(dataFolder, java.util.logging.Logger.getLogger("SLParty"));
        partyCache.setStorage(storage);

        // Load saved parties
        partyCache.load();

        // Register commands
        getCommandRegistry().registerCommand(new PartyCommand(partyCache));

        // Register player disconnect listener
        getEventRegistry().register(
            PlayerDisconnectEvent.class,
            this::onPlayerDisconnect
        );

        // Register marker provider when world is added
        getEventRegistry().registerGlobal(
            AddWorldEvent.class,
            this::onWorldAdded
        );

        getLogger().at(Level.INFO).log("SLParty setup complete!");
    }

    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        var playerRef = event.getPlayerRef();
        if (playerRef == null) {
            return;
        }
        partyCache.leaveParty(playerRef.getUuid());
    }

    private void onWorldAdded(AddWorldEvent event) {
        event.getWorld().getWorldMapManager()
            .addMarkerProvider("partyMembers", PartyMemberMarkerProvider.INSTANCE);
    }

    @Override
    protected void start() {
        getLogger().at(Level.INFO).log("SLParty started successfully!");
    }

    @Override
    protected void shutdown() {
        getLogger().at(Level.INFO).log("SLParty shutting down...");
        if (partyCache != null) {
            partyCache.clear();
        }
    }

    public PartyCache getPartyCache() {
        return partyCache;
    }
}
