package com.sl.party.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sl.party.model.Party;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles persistence of parties to JSON file
 */
public class PartyStorage {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path storageFile;
    private final Logger logger;

    public PartyStorage(Path dataFolder, Logger logger) {
        this.storageFile = dataFolder.resolve("parties.json");
        this.logger = logger;
    }

    /**
     * Save all parties to file
     */
    public void save(Collection<Party> parties) {
        try {
            // Convert parties to serializable format
            List<PartyData> partyDataList = new ArrayList<>();
            for (Party party : parties) {
                PartyData data = new PartyData();
                data.id = party.getId().toString();
                data.leaderId = party.getLeaderId().toString();
                data.members = new ArrayList<>();
                for (UUID member : party.getMembers()) {
                    data.members.add(member.toString());
                }
                data.isPublic = party.isPublish();
                partyDataList.add(data);
            }

            // Ensure parent directory exists
            Files.createDirectories(storageFile.getParent());

            // Write to file
            try (Writer writer = new FileWriter(storageFile.toFile())) {
                GSON.toJson(partyDataList, writer);
            }

            logger.log(Level.INFO, "Saved " + parties.size() + " parties to file");
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to save parties: " + e.getMessage());
        }
    }

    /**
     * Load all parties from file
     */
    public List<Party> load() {
        List<Party> parties = new ArrayList<>();

        if (!Files.exists(storageFile)) {
            logger.log(Level.INFO, "No parties file found, starting fresh");
            return parties;
        }

        try (Reader reader = new FileReader(storageFile.toFile())) {
            Type listType = new TypeToken<List<PartyData>>(){}.getType();
            List<PartyData> partyDataList = GSON.fromJson(reader, listType);

            if (partyDataList != null) {
                for (PartyData data : partyDataList) {
                    try {
                        UUID id = UUID.fromString(data.id);
                        UUID leaderId = UUID.fromString(data.leaderId);

                        Party party = new Party(id, leaderId);
                        party.setPublish(data.isPublic);

                        // Add members (excluding leader who is already added)
                        for (String memberStr : data.members) {
                            UUID memberId = UUID.fromString(memberStr);
                            if (!memberId.equals(leaderId)) {
                                party.addMember(memberId);
                            }
                        }

                        parties.add(party);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Failed to load party: " + e.getMessage());
                    }
                }
            }

            logger.log(Level.INFO, "Loaded " + parties.size() + " parties from file");
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to load parties: " + e.getMessage());
        }

        return parties;
    }

    /**
     * Internal data class for JSON serialization
     */
    private static class PartyData {
        String id;
        String leaderId;
        List<String> members;
        boolean isPublic;
    }
}
