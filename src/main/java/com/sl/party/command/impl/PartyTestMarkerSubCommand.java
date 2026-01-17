package com.sl.party.command.impl;

import com.hypixel.hytale.builtin.adventure.objectives.ObjectivePlugin;
import com.hypixel.hytale.builtin.adventure.objectives.markers.reachlocation.ReachLocationMarker;
import com.hypixel.hytale.builtin.adventure.objectives.markers.reachlocation.ReachLocationMarkerAsset;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Test command to check if ReachLocationMarker shows on compass
 * Usage: /party testmarker
 */
public class PartyTestMarkerSubCommand extends AbstractAsyncCommand {

    public PartyTestMarkerSubCommand() {
        super("testmarker", "Test if location markers show on compass");
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext commandContext) {
        if (!commandContext.isPlayer()) {
            commandContext.sendMessage(Message.raw("Only players can use this command").color(Color.RED));
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

            // Get player position
            final TransformComponent playerTransform = store.getComponent(reference, TransformComponent.getComponentType());
            if (playerTransform == null) return;

            final Transform transform = playerTransform.getTransform();
            final Vector3d position = transform.getPosition();

            // Find existing ReachLocationMarkerAsset
            Map<String, ReachLocationMarkerAsset> assetMap = ReachLocationMarkerAsset.getAssetMap().getAssetMap();

            if (assetMap.isEmpty()) {
                playerRef.sendMessage(Message.raw("No ReachLocationMarkerAsset found in game!").color(Color.RED));
                return;
            }

            // Get first available asset
            String markerId = assetMap.keySet().iterator().next();
            playerRef.sendMessage(Message.raw("Using marker asset: " + markerId).color(Color.YELLOW));

            // Target position: 100 blocks north of player
            double targetX = position.getX();
            double targetY = position.getY();
            double targetZ = position.getZ() - 100;

            try {
                // Create entity holder
                Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

                // Add ReachLocationMarker component
                ReachLocationMarker marker = new ReachLocationMarker(markerId);
                marker.getPlayers().add(playerRef.getUuid()); // Add this player to see the marker
                holder.addComponent(ReachLocationMarker.getComponentType(), marker);

                // Add model component
                Model model = ObjectivePlugin.get().getObjectiveLocationMarkerModel();
                if (model != null) {
                    holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
                    holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(model.toReference()));
                }

                // Add nameplate
                holder.addComponent(Nameplate.getComponentType(), new Nameplate("Party Test Marker"));

                // Add transform at target position
                Vector3d targetPosition = new Vector3d(targetX, targetY, targetZ);
                Vector3f targetRotation = new Vector3f(0, 0, 0);
                holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(targetPosition, targetRotation));

                // Spawn the entity
                store.addEntity(holder, AddReason.SPAWN);

                playerRef.sendMessage(Message.raw("Marker entity spawned at: " + targetX + ", " + targetY + ", " + targetZ).color(Color.GREEN));
                playerRef.sendMessage(Message.raw("Check compass for direction indicator!").color(Color.GREEN));

            } catch (Exception e) {
                playerRef.sendMessage(Message.raw("Error: " + e.getMessage()).color(Color.RED));
                e.printStackTrace();
            }
        });

        return CompletableFuture.completedFuture(null);
    }
}
