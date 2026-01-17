package com.sl.party.command.impl;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
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

import java.util.concurrent.CompletableFuture;

public class PartyCreateSubCommand extends AbstractAsyncCommand {

    private final PartyCache partyCache;

    public PartyCreateSubCommand(PartyCache partyCache) {
        super("create", "Create a party");
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

            if (partyCache.hasParty(playerRef.getUuid())) {
                commandContext.sendMessage(MessagesConfig.ALREADY_IN_A_PARTY);
                return;
            }

            final Party party = new Party(playerRef.getUuid());

            partyCache.add(party);
            playerRef.sendMessage(MessagesConfig.PARTY_CREATED);
        });

        return CompletableFuture.completedFuture(null);
    }
}
