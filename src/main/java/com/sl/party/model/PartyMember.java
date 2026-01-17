package com.sl.party.model;

import java.util.UUID;

/**
 * Represents a member of a party
 */
public class PartyMember {

    private final UUID playerId;
    private final String playerName;
    private final boolean isLeader;

    public PartyMember(UUID playerId, String playerName, boolean isLeader) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.isLeader = isLeader;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public boolean isLeader() {
        return isLeader;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PartyMember that = (PartyMember) obj;
        return playerId.equals(that.playerId);
    }

    @Override
    public int hashCode() {
        return playerId.hashCode();
    }
}
