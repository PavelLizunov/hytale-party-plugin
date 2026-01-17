package com.sl.party.command.impl;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sl.party.cache.PartyCache;
import com.sl.party.listener.PartyMapFilterListener;
import com.sl.party.messages.MessagesConfig;
import com.sl.party.model.Party;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PartyKickSubCommand extends AbstractAsyncCommand {

    private final PartyCache partyCache;

    public PartyKickSubCommand(PartyCache partyCache) {
        super("kick", "Kick a player from your party");
        this.partyCache = partyCache;

        setAllowsExtraArguments(true);
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext commandContext) {
        if (!commandContext.isPlayer()) {
            commandContext.sendMessage(MessagesConfig.ONLY_PLAYER);
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

            final Party party = partyCache.getParty(playerRef.getUuid());
            if (party == null) {
                player.sendMessage(MessagesConfig.NOT_IN_A_PARTY);
                return;
            }

            if (!party.isLeader(playerRef.getUuid())) {
                player.sendMessage(Message.raw("Only the party leader can kick players!").color(Color.RED));
                return;
            }

            // Parse player name from input: "/party kick PlayerName"
            final String[] parts = commandContext.getInputString().split(" ");
            if (parts.length < 3) {
                playerRef.sendMessage(Message.raw("Usage: /party kick <player>").color(Color.RED));
                return;
            }
            final String targetName = parts[2];

            // Find member by name (case-insensitive)
            UUID targetUuid = null;
            String actualName = null;
            for (Map.Entry<UUID, String> entry : party.getMemberNames().entrySet()) {
                if (entry.getValue().equalsIgnoreCase(targetName)) {
                    targetUuid = entry.getKey();
                    actualName = entry.getValue();
                    break;
                }
            }

            // If not found by name, try to find by UUID (partial match)
            if (targetUuid == null) {
                for (UUID memberId : party.getMembers()) {
                    if (memberId.toString().toLowerCase().startsWith(targetName.toLowerCase())) {
                        targetUuid = memberId;
                        actualName = party.getMemberName(memberId);
                        if (actualName == null) {
                            actualName = memberId.toString().substring(0, 8);
                        }
                        break;
                    }
                }
            }

            if (targetUuid == null) {
                playerRef.sendMessage(Message.raw("Player '" + targetName + "' not found in your party!").color(Color.RED));
                return;
            }

            if (targetUuid.equals(playerRef.getUuid())) {
                playerRef.sendMessage(Message.raw("You cannot kick yourself!").color(Color.RED));
                return;
            }

            if (!party.isMember(targetUuid)) {
                playerRef.sendMessage(Message.raw(actualName + " is not in your party!").color(Color.RED));
                return;
            }

            // Remove the player from party
            party.removeMember(targetUuid);
            partyCache.save();

            // Check if target is online
            PlayerRef targetRef = Universe.get().getPlayer(targetUuid);

            // Update map filter for kicked player if online
            if (targetRef != null && targetRef.isValid()) {
                for (Player p : world.getPlayers()) {
                    if (p != null && targetUuid.equals(p.getUuid())) {
                        PartyMapFilterListener.updateFilter(p);
                        break;
                    }
                }
                // Notify kicked player if online
                targetRef.sendMessage(Message.raw("You have been kicked from the party").color(Color.RED));
            }

            // Update filters for remaining party members
            PartyMapFilterListener.updatePartyFilters(party, world);

            // Notify party
            party.sendMessage(Message.raw(actualName + " was kicked from the party").color(Color.YELLOW));
        });

        return CompletableFuture.completedFuture(null);
    }
}
