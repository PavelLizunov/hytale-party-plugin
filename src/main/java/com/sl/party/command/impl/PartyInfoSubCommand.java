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
import com.sl.party.messages.MessagesConfig;
import com.sl.party.model.Party;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PartyInfoSubCommand extends AbstractAsyncCommand {

    private final PartyCache partyCache;

    public PartyInfoSubCommand(PartyCache partyCache) {
        super("info", "Show party information");
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

            final Party party = partyCache.getParty(playerRef.getUuid());
            if (party == null) {
                playerRef.sendMessage(MessagesConfig.NOT_IN_A_PARTY);
                return;
            }

            // Get leader name
            PlayerRef leaderRef = Universe.get().getPlayer(party.getOwnerId());
            String leaderName = leaderRef != null ? leaderRef.getUsername() : "Unknown";

            playerRef.sendMessage(Message.raw("=== Party Info ===").color(Color.YELLOW));
            playerRef.sendMessage(Message.raw("Leader: " + leaderName).color(Color.WHITE));
            playerRef.sendMessage(Message.raw("Members (" + party.getMembers().size() + "):").color(Color.WHITE));

            for (UUID memberId : party.getMembers()) {
                PlayerRef memberRef = Universe.get().getPlayer(memberId);
                if (memberRef != null && memberRef.isValid()) {
                    String status = memberId.equals(party.getOwnerId()) ? " [Leader]" : "";
                    playerRef.sendMessage(Message.raw("  - " + memberRef.getUsername() + status).color(Color.GREEN));
                } else {
                    playerRef.sendMessage(Message.raw("  - (offline)").color(Color.GRAY));
                }
            }

            String publicStatus = party.isPublish() ? "Public" : "Private";
            playerRef.sendMessage(Message.raw("Status: " + publicStatus).color(Color.WHITE));
        });

        return CompletableFuture.completedFuture(null);
    }
}
