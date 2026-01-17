package com.sl.party.command.impl;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.Message;
import com.sl.party.cache.PartyCache;
import com.sl.party.messages.MessagesConfig;
import com.sl.party.model.Party;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class PartyInviteSubCommand extends AbstractAsyncCommand {

    private final PartyCache partyCache;
    private RequiredArg<PlayerRef> targetRefArg;

    public PartyInviteSubCommand(PartyCache partyCache) {
        super("invite", "Invite some player for your party");
        this.partyCache = partyCache;

        this.targetRefArg = this.withRequiredArg("player", "target player", ArgTypes.PLAYER_REF);
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
                player.sendMessage(MessagesConfig.ONLY_OWNER_CAN_DISBAND);
                return;
            }

            final PlayerRef targetRef = commandContext.get(targetRefArg);
            if (targetRef == null) {
                playerRef.sendMessage(MessagesConfig.PLAYER_NOT_FOUND);
                return;
            }

            party.addInvite(targetRef.getUuid());

            targetRef.sendMessage(Message.raw("You received a party invite from " + playerRef.getUsername() + "! Use /party join " + playerRef.getUsername()).color(Color.CYAN));
            playerRef.sendMessage(Message.raw("Invite sent to " + targetRef.getUsername()).color(Color.GREEN));
        });

        return CompletableFuture.completedFuture(null);
    }
}
