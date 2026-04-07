package com.mervyn.kotafix.mixin;

import com.mervyn.kotafix.init.KotaFixMod;
import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Function;

/**
 * Mixin to fix a design flaw in Porting Lib's event system.
 * The original implementation stops executing event listeners as soon as one returns true.
 * This Mixin redirects the event creation to provide an invoker that executes ALL listeners.
 * 
 * Target is set using a string to prevent premature classloading during Mixin preparation.
 */
@Mixin(targets = "io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents", remap = false, priority = 10000)
public class LivingEntityEventsMixin {

    @SuppressWarnings("unchecked")
    @Redirect(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/fabricmc/fabric/api/event/EventFactory;createArrayBacked(Ljava/lang/Class;Ljava/util/function/Function;)Lnet/fabricmc/fabric/api/event/Event;"
            )
    )
    private static <T> Event<T> redirectCreateArrayBacked(Class<T> type, Function<T[], T> originalFactory) {
        String typeName = type.getName();
        KotaFixMod.LOGGER.info("[KOTA FIX] Redirecting EventFactory.createArrayBacked for type: " + typeName);
        
        // We use string match to avoid direct inner class literal if possible
        if (typeName.endsWith("LivingEntityEvents$Drops")) {
            KotaFixMod.LOGGER.info("[KOTA FIX] Successfully matched DROPS event for redirection.");
            
            Function<LivingEntityEvents.Drops[], LivingEntityEvents.Drops> invokerFactory = callbacks -> (target, source, drops, lootingLevel, recentlyHit) -> {
                boolean result = false;
                
                // Detailed logging for execution
                KotaFixMod.LOGGER.info("[KOTA FIX] Executing DROPS event invoker. Callbacks count: " + callbacks.length);
                if (target != null) {
                    KotaFixMod.LOGGER.info("[KOTA FIX] Target: " + target.getType().toString() + " at " + target.getPos());
                }
                
                for (int i = 0; i < callbacks.length; i++) {
                    LivingEntityEvents.Drops callback = callbacks[i];
                    String callbackName = callback.getClass().getName();
                    
                    int beforeCount = drops != null ? drops.size() : -1;
                    boolean callbackResult = callback.onLivingEntityDrops(target, source, drops, lootingLevel, recentlyHit);
                    int afterCount = drops != null ? drops.size() : -1;
                    
                    KotaFixMod.LOGGER.info("[KOTA FIX] Callback [" + i + "] (" + callbackName + ") returned: " + callbackResult + ". Drops size change: " + beforeCount + " -> " + afterCount);
                    
                    if (callbackResult) {
                        result = true;
                        // In the original buggy code, it would return true here.
                        // We continue to ensure all mods (like Zenith) get their turn.
                    }
                }
                return result;
            };
            return (Event<T>) EventFactory.createArrayBacked(LivingEntityEvents.Drops.class, invokerFactory);
        }
        
        return EventFactory.createArrayBacked(type, originalFactory);
    }
}