package com.sl.party.command.impl;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sl.party.cache.PartyCache;
import com.sl.party.messages.MessagesConfig;
import com.sl.party.model.Party;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class PartyPublicSubCommand extends AbstractAsyncCommand {

    private final PartyCache partyCache;

    public PartyPublicSubCommand(PartyCache partyCache) {
        super("public", "Change your party to public or private");
        this.partyCache = partyCache;
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

            if (!partyCache.hasParty(playerRef.getUuid())) {
                player.sendMessage(MessagesConfig.NOT_IN_A_PARTY);
                return;
            }

            final Party party = partyCache.getByOwner(playerRef.getUuid());
            if (party == null) {
                player.sendMessage(MessagesConfig.ONLY_OWNER_CAN_DISBAND);
                return;
            }

            party.setPublish(!party.isPublish());
            partyCache.save();  // Persist change

            final String status = party.isPublish() ? "PUBLIC" : "PRIVATE";
            party.sendMessage(Message.raw("Party is now " + status).color(Color.YELLOW));
        });

        return CompletableFuture.completedFuture(null);
    }
}
