package com.mervyn.kotafix.mixin;

import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Function;

@Mixin(value = LivingEntityEvents.class, remap = false)
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
        if (type == LivingEntityEvents.Drops.class) {
            return EventFactory.createArrayBacked(type, callbacks -> {
                return (T) (LivingEntityEvents.Drops) (target, source, drops, lootingLevel, recentlyHit) -> {
                    boolean handled = false;
                    boolean isPlayer = source != null && source.getAttacker() instanceof PlayerEntity;

                    for (LivingEntityEvents.Drops callback : (LivingEntityEvents.Drops[]) callbacks) {
                        if (callback.onLivingEntityDrops(target, source, drops, lootingLevel, recentlyHit)) {
                            handled = true;
                            if (!isPlayer) {
                                return true;
                            }
                        }
                    }
                    return handled;
                };
            });
        }
        return EventFactory.createArrayBacked(type, originalFactory);
    }
}
    