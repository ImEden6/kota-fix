package com.mervyn.kotafix.mixin;

import com.mervyn.kotafix.init.KotaFixMod;
import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents;
import io.github.fabricators_of_create.porting_lib.entity.extensions.EntityExtensions;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

/**
 * The "Double-Shot" fix. 
 * This mixin manually fires Porting Lib events if the original Porting Lib mixins 
 * are being bypassed by another mod.
 */
@Mixin(value = LivingEntity.class, priority = 11000)
public abstract class KotaLivingEntityMixin {

    @Shadow protected int playerHitTimer;
    @Shadow protected PlayerEntity attackingPlayer;

    @Inject(method = "dropLoot", at = @At("HEAD"))
    private void kotafix$resetMarkers(DamageSource source, boolean causedByPlayer, CallbackInfo ci) {
        LivingEntityEventsMixin.DROPS_FIRED.set(false);
    }

    @Inject(method = "dropLoot", at = @At("RETURN"))
    private void kotafix$forceFireDrops(DamageSource source, boolean causedByPlayer, CallbackInfo ci) {
        if (!LivingEntityEventsMixin.DROPS_FIRED.get()) {
            LivingEntity entity = (LivingEntity) (Object) this;
            
            // Access captured drops from Porting Lib's extensions
            Collection<ItemEntity> drops = ((EntityExtensions) entity).captureDrops(null);
            
            if (drops != null && !drops.isEmpty()) {
                KotaFixMod.LOGGER.info("[KOTA FIX] Manual DROPS firing triggered for {}. Original hook was bypassed.", entity.getType().toString());
                
                // Call the invoker using Object casts as per the simplified stub
                @SuppressWarnings("unchecked")
                boolean cancelled = LivingEntityEvents.DROPS.invoker().onLivingEntityDrops(
                        (Object)entity, (Object)source, (Collection<ItemEntity>)drops, 0, this.playerHitTimer > 0
                );
                
                if (!cancelled) {
                    World world = entity.getWorld();
                    drops.forEach(e -> world.spawnEntity(e));
                }
            }
        }
    }
}
