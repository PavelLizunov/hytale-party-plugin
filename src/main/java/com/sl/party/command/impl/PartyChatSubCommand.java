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
import com.sl.party.cache.PartyCache;
import com.sl.party.messages.MessagesConfig;
import com.sl.party.model.Party;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class PartyChatSubCommand extends AbstractAsyncCommand {

    private final PartyCache partyCache;

    public PartyChatSubCommand(PartyCache partyCache) {
        super("chat", "Chat with party members");
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
                playerRef.sendMessage(MessagesConfig.NOT_IN_A_PARTY);
                return;
            }

            final String[] parts = commandContext.getInputString().split(" ");
            final StringBuilder messageBuilder = new StringBuilder();
            for (int i = 2; i < parts.length; i++) {
                messageBuilder.append(parts[i]);
                if (i < parts.length - 1) {
                    messageBuilder.append(" ");
                }
            }

            final String message = messageBuilder.toString();
            party.sendMessage(Message.raw("[Party]").color(Color.MAGENTA).insert(" " + playerRef.getUsername() + ": " + message).color(Color.WHITE));
        });

        return CompletableFuture.completedFuture(null);
    }
}
