package io.github.fabricators_of_create.porting_lib.entity.events;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.ItemEntity;
import java.util.Collection;

/**
 * Stub for Porting Lib's LivingEntityEvents.
 * 
 * We use the actual Fabric API Event class to avoid field type mismatches at runtime.
 */
public class LivingEntityEvents {
    // The actual type in Porting Lib is net.fabricmc.fabric.api.event.Event
    public static final Event<Drops> DROPS = null;

    public interface Drops {
        boolean onLivingEntityDrops(LivingEntity target, DamageSource source, Collection<ItemEntity> drops, int lootingLevel, boolean recentlyHit);
    }
}
