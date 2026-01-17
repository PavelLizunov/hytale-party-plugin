package com.sl.party.command.impl;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sl.party.hud.TestHud;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Command to test CustomUIHud with different selectors
 * Usage: /party testhud [selector_index]
 */
public class PartyTestHudSubCommand extends AbstractAsyncCommand {

    // Store active test HUDs per player
    private static final Map<UUID, TestHud> activeHuds = new HashMap<>();
    // Store current selector index per player
    private static final Map<UUID, Integer> selectorIndices = new HashMap<>();

    public PartyTestHudSubCommand() {
        super("testhud", "Test CustomUIHud with different selectors");
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext commandContext) {
        if (!commandContext.isPlayer()) {
            commandContext.sendMessage(Message.raw("Only players can use this command").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        final Player player = (Player) commandContext.sender();
        final Ref<EntityStore> reference = player.getReference();
        if (reference == null) return CompletableFuture.completedFuture(null);

        final Store<EntityStore> store = reference.getStore();
        final World world = store.getExternalData().getWorld();

        world.execute(() -> {
            final PlayerRef playerRef = store.getComponent(reference, PlayerRef.getComponentType());
            if (playerRef == null) return;

            UUID playerId = playerRef.getUuid();

            // Get current index and increment for next call
            int currentIndex = selectorIndices.getOrDefault(playerId, 0);
            int nextIndex = (currentIndex + 1) % 11;
            selectorIndices.put(playerId, nextIndex);

            // Check if HUD already exists
            TestHud existingHud = activeHuds.get(playerId);

            if (existingHud != null) {
                // Update selector and rebuild
                existingHud.setSelectorIndex(currentIndex);
                existingHud.show();
                playerRef.sendMessage(Message.raw("HUD updated with selector [" + currentIndex + "]: " + existingHud.getCurrentSelector()).color(Color.YELLOW));
            } else {
                // Create new HUD
                TestHud testHud = new TestHud(playerRef);
                testHud.setSelectorIndex(currentIndex);
                testHud.show();
                activeHuds.put(playerId, testHud);
                playerRef.sendMessage(Message.raw("HUD created with selector [" + currentIndex + "]: " + testHud.getCurrentSelector()).color(Color.GREEN));
            }

            playerRef.sendMessage(Message.raw("Run /party testhud again to try next selector").color(Color.GRAY));
        });

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Remove HUD for a player (called on disconnect)
     */
    public static void removeHud(UUID playerId) {
        activeHuds.remove(playerId);
        selectorIndices.remove(playerId);
    }
}
