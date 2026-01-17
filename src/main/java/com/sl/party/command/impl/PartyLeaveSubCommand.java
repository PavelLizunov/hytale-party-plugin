package com.sl.party.command.impl;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.sl.party.cache.PartyCache;
import com.sl.party.listener.PartyMapFilterListener;
import com.sl.party.messages.MessagesConfig;
import com.sl.party.model.Party;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class PartyLeaveSubCommand extends AbstractAsyncCommand {

    private final PartyCache partyCache;

    public PartyLeaveSubCommand(PartyCache partyCache) {
        super("leave", "Leave your current party.");
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
            if(party == null) {
                player.sendMessage(MessagesConfig.NOT_IN_A_PARTY);
                return;
            }

            if(party.isLeader(playerRef.getUuid())) {
                player.sendMessage(MessagesConfig.OWNER_CANT_LEAVE_PARTY);
                return;
            }

            party.removeMember(playerRef.getUuid());
            partyCache.save();  // Persist change

            // Clear filter for leaving player and update remaining members
            PartyMapFilterListener.updateFilter(player);
            PartyMapFilterListener.updatePartyFilters(party, world);

            party.sendMessage(Message.raw(playerRef.getUsername() + " left the party").color(Color.YELLOW));
            playerRef.sendMessage(Message.raw("You left the party").color(Color.YELLOW));
        });

        return CompletableFuture.completedFuture(null);
    }
}
