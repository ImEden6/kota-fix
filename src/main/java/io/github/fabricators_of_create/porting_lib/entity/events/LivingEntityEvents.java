package io.github.fabricators_of_create.porting_lib.entity.events;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ItemEntity;
import java.util.Collection;
import net.fabricmc.fabric.api.event.Event;

public abstract class LivingEntityEvents {
    public static final Event<Drops> DROPS = null;

    public interface Drops {
        boolean onLivingEntityDrops(LivingEntity target, DamageSource source, Collection<ItemEntity> drops, int lootingLevel, boolean recentlyHit);
    }
}
