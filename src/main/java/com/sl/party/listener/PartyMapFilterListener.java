package com.sl.party.listener;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.sl.party.SLPartyPlugin;
import com.sl.party.cache.PartyCache;
import com.sl.party.model.Party;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * Filter that excludes party members from default map player icons.
 * This prevents duplication - party members will only show via PartyMemberMarkerProvider.
 */
public class PartyMapFilterListener {

    /**
     * Creates a filter for the given viewer that returns true for party members
     * (causing them to be skipped by PlayerIconMarkerProvider)
     */
    public static Predicate<PlayerRef> createPartyFilter(UUID viewerUuid) {
        return playerRef -> {
            if (playerRef == null) return false;

            final PartyCache partyCache = SLPartyPlugin.getInstance().getPartyCache();
            if (partyCache == null) return false;

            final Party viewerParty = partyCache.getParty(viewerUuid);
            if (viewerParty == null) return false;

            // Return true to SKIP this player from default icons if they're in the same party
            return viewerParty.getMembers().contains(playerRef.getUuid());
        };
    }

    /**
     * Updates the map filter for a player to exclude party members
     */
    public static void updateFilter(Player player) {
        if (player == null) return;

        final UUID playerUuid = player.getUuid();
        if (playerUuid == null) return;

        final var tracker = player.getWorldMapTracker();
        if (tracker == null) return;

        final PartyCache partyCache = SLPartyPlugin.getInstance().getPartyCache();
        if (partyCache == null) return;

        final Party party = partyCache.getParty(playerUuid);
        if (party != null && party.getMembers().size() > 1) {
            // Player is in a party with others - set filter to exclude party members
            tracker.setPlayerMapFilter(createPartyFilter(playerUuid));
        } else {
            // Player not in party or alone - clear filter
            tracker.setPlayerMapFilter(null);
        }
    }

    /**
     * Updates filters for all members of a party using the World to find Player objects
     */
    public static void updatePartyFilters(Party party, World world) {
        if (party == null || world == null) return;

        for (Player player : world.getPlayers()) {
            if (player == null) continue;

            UUID playerUuid = player.getUuid();
            if (playerUuid != null && party.isMember(playerUuid)) {
                updateFilter(player);
            }
        }
    }
}
