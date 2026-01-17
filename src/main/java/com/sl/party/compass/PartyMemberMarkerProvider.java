package com.sl.party.compass;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.packets.worldmap.MapMarker;
import com.hypixel.hytale.server.core.asset.type.gameplay.GameplayConfig;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldMapTracker;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager;
import com.hypixel.hytale.server.core.util.PositionUtil;
import com.sl.party.SLPartyPlugin;
import com.sl.party.cache.PartyCache;
import com.sl.party.model.Party;

public class PartyMemberMarkerProvider implements WorldMapManager.MarkerProvider {

    public static final PartyMemberMarkerProvider INSTANCE = new PartyMemberMarkerProvider();

    // Use player icon for party members
    private static final String MARKER_ICON = "Player.png";

    private PartyMemberMarkerProvider() {
    }

    @Override
    public void update(World world, GameplayConfig gameplayConfig, WorldMapTracker worldMapTracker, int viewRadius, int playerChunkX, int playerChunkZ) {
        final Player player = worldMapTracker.getPlayer();
        if (player == null || player.getReference() == null) return;

        final PartyCache partyCache = SLPartyPlugin.getInstance().getPartyCache();
        if (partyCache == null) return;

        // Get viewer's UUID from player
        final PlayerRef viewerRef = player.getPlayerRef();
        if (viewerRef == null) return;
        final java.util.UUID viewerUuid = viewerRef.getUuid();

        final Party party = partyCache.getParty(viewerUuid);
        if (party == null || party.getMembers().size() <= 1) return;

        // Iterate over all party members
        for (PlayerRef partyMember : party.getPlayers()) {
            // Skip showing marker for yourself
            if (partyMember.getUuid().equals(viewerUuid)) {
                continue;
            }

            // Skip invalid members
            if (!partyMember.isValid()) {
                continue;
            }

            final Transform transform = partyMember.getTransform();
            if (transform == null) continue;

            // Add party indicator to name for visual distinction
            final String memberName = "[Party] " + partyMember.getUsername();
            final String markerId = "party-" + partyMember.getUuid().toString();

            // Create MapMarker directly (like PlayerCompass does)
            MapMarker marker = new MapMarker(
                    markerId,
                    memberName,
                    MARKER_ICON,
                    PositionUtil.toTransformPacket(transform),
                    null  // no context menu
            );

            // KEY: Use viewRadius = -1 to show on compass!
            worldMapTracker.trySendMarker(
                    -1,  // -1 = show on compass
                    playerChunkX,
                    playerChunkZ,
                    marker
            );
        }
    }
}
