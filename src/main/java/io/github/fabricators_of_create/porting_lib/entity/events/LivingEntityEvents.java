package io.github.fabricators_of_create.porting_lib.entity.events;

import net.fabricmc.fabric.api.event.Event;
import java.util.Collection;

/**
 * Compile-time stub for Porting Lib's LivingEntityEvents.
 * Used to avoid heavyweight dependencies during compilation.
 * Uses Object for Minecraft classes to avoid mapping-specific compilation errors.
 */
public abstract class LivingEntityEvents {
    public static Event<Drops> DROPS;
    public static Event<ExperienceDrop> EXPERIENCE_DROP;

    public interface Drops {
        boolean onLivingEntityDrops(Object target, Object source, Collection<?> drops, int lootingLevel, boolean recentlyHit);
    }

    public interface ExperienceDrop {
        int onLivingEntityExperienceDrop(int i, Object attackingPlayer, Object entity);
    }
}
