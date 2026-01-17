package com.sl.party.model;

import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.util.NotificationUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a party of players
 */
public class Party {

    private final UUID id;
    private final UUID ownerId;
    private final Set<UUID> members;
    private final Set<UUID> invites;
    private boolean publish;

    public Party(UUID ownerId) {
        this(UUID.randomUUID(), ownerId);
    }

    /**
     * Constructor for loading saved parties
     */
    public Party(UUID id, UUID ownerId) {
        this.id = id;
        this.ownerId = ownerId;
        this.members = new HashSet<>();
        this.members.add(ownerId);
        this.invites = new HashSet<>();
        this.publish = false;
    }

    public UUID getLeaderId() {
        return ownerId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public Set<UUID> getInvites() {
        return invites;
    }

    public boolean isPublish() {
        return publish;
    }

    public void setPublish(boolean publish) {
        this.publish = publish;
    }

    public boolean isLeader(UUID playerID) {
        if (playerID == null) return false;
        return ownerId.equals(playerID);
    }

    public boolean isMember(UUID playerId) {
        if (playerId == null) return false;
        return members.contains(playerId);
    }

    public boolean isInvited(UUID playerId) {
        if (playerId == null) return false;
        return invites.contains(playerId);
    }

    public void addMember(UUID playerId) {
        if (playerId != null) {
            members.add(playerId);
        }
    }

    public void addInvite(UUID playerId) {
        if (playerId != null) {
            invites.add(playerId);
        }
    }

    public void removeMember(UUID playerId) {
        if (playerId != null) {
            members.remove(playerId);
        }
    }

    public void removeInvite(UUID playerId) {
        if (playerId != null) {
            invites.remove(playerId);
        }
    }

    public void sendMessage(Message message) {
        getPlayers().forEach(playerRef -> playerRef.sendMessage(message));
    }

    public void sendNotification(Message title, Message subTitle, NotificationStyle notificationStyle) {
        getPlayers().forEach(playerRef ->
            NotificationUtil.sendNotification(
                playerRef.getPacketHandler(),
                title,
                subTitle,
                "",
                new ItemStack("copper_shield").toPacket(),
                notificationStyle
            )
        );
    }

    public void sendSound(int index, SoundCategory soundCategory) {
        getPlayers().forEach(playerRef ->
            SoundUtil.playSoundEvent2dToPlayer(playerRef, index, soundCategory, 1f, 1f)
        );
    }

    public List<PlayerRef> getPlayers() {
        return members.stream()
            .map(playerId -> Universe.get().getPlayer(playerId))
            .filter(playerRef -> playerRef != null && playerRef.isValid())
            .toList();
    }
}
