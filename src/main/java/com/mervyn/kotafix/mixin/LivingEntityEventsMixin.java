package com.mervyn.kotafix.mixin;

import com.mervyn.kotafix.init.KotaFixMod;
import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

/**
 * Mixin to fix Porting Lib's event loop bugs using the "Shadow" approach.
 * Uses Object casting to match the simplified compilation stub.
 */
@Mixin(targets = "io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents", remap = false, priority = 10000)
public class LivingEntityEventsMixin {

    public static final ThreadLocal<Boolean> DROPS_FIRED = ThreadLocal.withInitial(() -> false);
    public static final ThreadLocal<Boolean> EXPERIENCE_FIRED = ThreadLocal.withInitial(() -> false);

    @Shadow @Final @Mutable
    public static Event<LivingEntityEvents.Drops> DROPS;

    @Shadow @Final @Mutable
    public static Event<LivingEntityEvents.ExperienceDrop> EXPERIENCE_DROP;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void kotafix$replaceEventInvokers(CallbackInfo ci) {
        // ===== FIX 1: DROPS =====
        DROPS = EventFactory.createArrayBacked(LivingEntityEvents.Drops.class, callbacks -> 
            (target, source, drops, lootingLevel, recentlyHit) -> {
                DROPS_FIRED.set(true);
                boolean result = false;
                
                int i = 0;
                for (LivingEntityEvents.Drops callback : callbacks) {
                    int sizeBefore = ((Collection<?>)drops).size();
                    try {
                        boolean cbResult = callback.onLivingEntityDrops(target, source, (Collection<?>)drops, lootingLevel, recentlyHit);
                        if (cbResult) result = true;
                        
                        if (KotaFixMod.LOGGER.isDebugEnabled() || sizeBefore != ((Collection<?>)drops).size()) {
                            KotaFixMod.LOGGER.info("[KOTA FIX] DROPS Callback [{}] ({}) returned: {}. Drops size: {} -> {}", i, callback.getClass().getName(), cbResult, sizeBefore, ((Collection<?>)drops).size());
                        }
                    } catch (Throwable t) {
                        KotaFixMod.LOGGER.error("[KOTA FIX] Exception in DROPS callback [{}] ({})", i, callback.getClass().getName(), t);
                    }
                    i++;
                }
                return result;
            }
        );

        // ===== FIX 2: EXPERIENCE_DROP =====
        EXPERIENCE_DROP = EventFactory.createArrayBacked(LivingEntityEvents.ExperienceDrop.class, callbacks -> 
            (xp, attackingPlayer, entity) -> {
                EXPERIENCE_FIRED.set(true);
                int currentXp = xp;
                
                int i = 0;
                for (LivingEntityEvents.ExperienceDrop callback : callbacks) {
                    try {
                        int oldXp = currentXp;
                        currentXp = callback.onLivingEntityExperienceDrop(currentXp, attackingPlayer, entity);
                        
                        if (KotaFixMod.LOGGER.isDebugEnabled() || oldXp != currentXp) {
                            KotaFixMod.LOGGER.info("[KOTA FIX] XP Callback [{}] ({}) returned: {} -> {}", i, callback.getClass().getName(), oldXp, currentXp);
                        }
                    } catch (Throwable t) {
                        KotaFixMod.LOGGER.error("[KOTA FIX] Exception in EXPERIENCE_DROP callback [{}] ({})", i, callback.getClass().getName(), t);
                    }
                    i++;
                }
                return currentXp;
            }
        );

        KotaFixMod.LOGGER.info("[KOTA FIX] Replaced DROPS and EXPERIENCE_DROP invokers.");
    }
}