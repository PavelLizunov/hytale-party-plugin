package com.sl.party.cache;

import com.sl.party.model.Party;
import com.sl.party.storage.PartyStorage;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cache for storing and managing parties
 */
public class PartyCache {

    private final Map<UUID, Party> cache = new HashMap<>();
    private PartyStorage storage;

    public void setStorage(PartyStorage storage) {
        this.storage = storage;
    }

    public void add(Party party) {
        cache.put(party.getId(), party);
        saveAsync();
    }

    public void remove(Party party) {
        cache.remove(party.getId());
        saveAsync();
    }

    /**
     * Load parties from storage
     */
    public void load() {
        if (storage != null) {
            List<Party> parties = storage.load();
            for (Party party : parties) {
                cache.put(party.getId(), party);
            }
        }
    }

    /**
     * Save parties to storage asynchronously
     */
    private void saveAsync() {
        if (storage != null) {
            storage.save(cache.values());
        }
    }

    public Party getParty(UUID playerId) {
        if (playerId == null) return null;
        return cache.values().stream()
            .filter(party -> party.isMember(playerId))
            .findFirst()
            .orElse(null);
    }

    public Party getByOwner(UUID ownerId) {
        if (ownerId == null) return null;
        return cache.values().stream()
            .filter(party -> party.isLeader(ownerId))
            .findFirst()
            .orElse(null);
    }

    public boolean hasParty(UUID playerId) {
        if (playerId == null) return false;
        return cache.values().stream()
            .anyMatch(party -> party.isMember(playerId));
    }

    public Collection<Party> getParties() {
        return cache.values();
    }

    /**
     * Removes a player from their party when they disconnect
     * Note: Does NOT disband party when leader leaves - party persists for reconnection
     */
    public void leaveParty(UUID playerId) {
        if (playerId == null) return;

        // Don't remove from party on disconnect - party persists
        // Player will rejoin their party when they reconnect
    }

    /**
     * Call this after modifying party membership to persist changes
     */
    public void save() {
        saveAsync();
    }

    /**
     * Clears all parties (used on shutdown)
     */
    public void clear() {
        cache.clear();
    }
}
