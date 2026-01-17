package com.sl.party.command.impl;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sl.party.cache.PartyCache;
import com.sl.party.listener.PartyMapFilterListener;
import com.sl.party.messages.MessagesConfig;
import com.sl.party.model.Party;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class PartyKickSubCommand extends AbstractAsyncCommand {

    private final PartyCache partyCache;
    private RequiredArg<PlayerRef> targetRefArg;

    public PartyKickSubCommand(PartyCache partyCache) {
        super("kick", "Kick a player from your party");
        this.partyCache = partyCache;

        this.targetRefArg = this.withRequiredArg("player", "Player to kick", ArgTypes.PLAYER_REF);
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

            final PlayerRef targetRef = commandContext.get(targetRefArg);
            if (targetRef == null) {
                playerRef.sendMessage(MessagesConfig.PLAYER_NOT_FOUND);
                return;
            }

            if (targetRef.getUuid().equals(playerRef.getUuid())) {
                playerRef.sendMessage(Message.raw("You cannot kick yourself!").color(Color.RED));
                return;
            }

            if (!party.isMember(targetRef.getUuid())) {
                playerRef.sendMessage(Message.raw(targetRef.getUsername() + " is not in your party!").color(Color.RED));
                return;
            }

            // Remove the player from party
            party.removeMember(targetRef.getUuid());
            partyCache.save();

            // Update map filter for kicked player
            if (targetRef.isValid()) {
                for (Player p : world.getPlayers()) {
                    if (p != null && targetRef.getUuid().equals(p.getUuid())) {
                        PartyMapFilterListener.updateFilter(p);
                        break;
                    }
                }
            }

            // Update filters for remaining party members
            PartyMapFilterListener.updatePartyFilters(party, world);

            // Notify
            party.sendMessage(Message.raw(targetRef.getUsername() + " was kicked from the party").color(Color.YELLOW));
            targetRef.sendMessage(Message.raw("You have been kicked from the party").color(Color.RED));
        });

        return CompletableFuture.completedFuture(null);
    }
}
