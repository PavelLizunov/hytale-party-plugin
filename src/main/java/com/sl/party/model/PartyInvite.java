package com.sl.party.model;

import java.util.UUID;

/**
 * Represents a pending party invitation
 */
public class PartyInvite {

    private final UUID partyId;
    private final UUID inviterId;
    private final String inviterName;
    private final UUID inviteeId;
    private final long createdAt;

    // Invite expires after 60 seconds
    private static final long INVITE_EXPIRY_MS = 60_000;

    public PartyInvite(UUID partyId, UUID inviterId, String inviterName, UUID inviteeId) {
        this.partyId = partyId;
        this.inviterId = inviterId;
        this.inviterName = inviterName;
        this.inviteeId = inviteeId;
        this.createdAt = System.currentTimeMillis();
    }

    public UUID getPartyId() {
        return partyId;
    }

    public UUID getInviterId() {
        return inviterId;
    }

    public String getInviterName() {
        return inviterName;
    }

    public UUID getInviteeId() {
        return inviteeId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt > INVITE_EXPIRY_MS;
    }
}
