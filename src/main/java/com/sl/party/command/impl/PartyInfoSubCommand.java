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

            // Get leader name (try online first, then stored name)
            PlayerRef leaderRef = Universe.get().getPlayer(party.getOwnerId());
            String leaderName;
            if (leaderRef != null && leaderRef.isValid()) {
                leaderName = leaderRef.getUsername();
                // Update stored name if online
                party.setMemberName(party.getOwnerId(), leaderName);
            } else {
                leaderName = party.getMemberName(party.getOwnerId());
                if (leaderName == null) leaderName = "Unknown";
            }

            playerRef.sendMessage(Message.raw("=== Party Info ===").color(Color.YELLOW));
            playerRef.sendMessage(Message.raw("Leader: " + leaderName).color(Color.WHITE));
            playerRef.sendMessage(Message.raw("Members (" + party.getMembers().size() + "):").color(Color.WHITE));

            boolean needsSave = false;
            for (UUID memberId : party.getMembers()) {
                PlayerRef memberRef = Universe.get().getPlayer(memberId);
                if (memberRef != null && memberRef.isValid()) {
                    // Update stored name for online players (keeps names fresh)
                    String currentName = memberRef.getUsername();
                    if (!currentName.equals(party.getMemberName(memberId))) {
                        party.setMemberName(memberId, currentName);
                        needsSave = true;
                    }
                    String status = memberId.equals(party.getOwnerId()) ? " [Leader]" : "";
                    playerRef.sendMessage(Message.raw("  - " + currentName + status).color(Color.GREEN));
                } else {
                    // Use stored name for offline players
                    String offlineName = party.getMemberName(memberId);
                    if (offlineName == null) offlineName = memberId.toString().substring(0, 8);
                    String status = memberId.equals(party.getOwnerId()) ? " [Leader]" : "";
                    playerRef.sendMessage(Message.raw("  - " + offlineName + status + " (offline)").color(Color.GRAY));
                }
            }
            if (needsSave) {
                partyCache.save();
            }

            String publicStatus = party.isPublish() ? "Public" : "Private";
            playerRef.sendMessage(Message.raw("Status: " + publicStatus).color(Color.WHITE));
        });

        return CompletableFuture.completedFuture(null);
    }
}
