package com.sl.party.system;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.AllLegacyLivingEntityTypesQuery;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sl.party.cache.PartyCache;
import com.sl.party.model.Party;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Prevents party members from damaging each other
 */
public class DamageSystemListener extends DamageEventSystem {

    private final PartyCache partyCache;

    @Nonnull
    private static final Query<EntityStore> QUERY;

    public DamageSystemListener(PartyCache partyCache) {
        this.partyCache = partyCache;
    }

    @Nullable
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Override
    public void handle(int i, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer, @NonNullDecl Damage damage) {
        if (!(damage.getSource() instanceof Damage.EntitySource entitySource)) return;

        final Ref<EntityStore> attackerRef = entitySource.getRef();
        if (!attackerRef.isValid()) return;

        final PlayerRef victim = archetypeChunk.getComponent(i, PlayerRef.getComponentType());
        final PlayerRef attacker = commandBuffer.getComponent(attackerRef, PlayerRef.getComponentType());
        if (victim == null || attacker == null) return;

        final Party attackerParty = partyCache.getParty(attacker.getUuid());
        if (attackerParty == null) return;

        final Party victimParty = partyCache.getParty(victim.getUuid());
        if (victimParty == null) return;

        if (!attackerParty.getId().equals(victimParty.getId())) return;

        // Cancel damage between party members
        damage.setCancelled(true);
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    static {
        QUERY = AllLegacyLivingEntityTypesQuery.INSTANCE;
    }
}
