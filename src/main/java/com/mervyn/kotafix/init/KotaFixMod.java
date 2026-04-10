package com.mervyn.kotafix.init;

import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KotaFixMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("kota_fix");
    private final List<PendingXpConversion> pendingConversions = new ArrayList<>();
    
    // Cached Enchantment
    private static Enchantment KNOWLEDGE_ENCHANTMENT = null;
    private static boolean HAS_SEARCHED_ENCHANTMENT = false;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing KOTA Fix mod (V4 - Safe Area Scanner Restored)...");
        LivingEntityEvents.DROPS.register(this::onDrops);

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            synchronized (pendingConversions) {
                pendingConversions.removeIf(PendingXpConversion::tick);
            }
        });
    }

    private boolean onDrops(LivingEntity target, DamageSource source, Collection<ItemEntity> drops, int lootingLevel,
            boolean recentlyHit) {
        if (target instanceof PlayerEntity || target.getWorld().isClient()) {
            return false;
        }

        Entity attacker;
        if (source != null && (attacker = source.getAttacker()) instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) attacker;
            
            if (KNOWLEDGE_ENCHANTMENT == null && !HAS_SEARCHED_ENCHANTMENT) {
                HAS_SEARCHED_ENCHANTMENT = true;
                KNOWLEDGE_ENCHANTMENT = Registries.ENCHANTMENT.get(new Identifier("zenith", "knowledge"));
                if (KNOWLEDGE_ENCHANTMENT == null) {
                    KNOWLEDGE_ENCHANTMENT = Registries.ENCHANTMENT.get(new Identifier("apotheosis", "knowledge"));
                }
            }

            if (KNOWLEDGE_ENCHANTMENT != null) {
                int knowledge = EnchantmentHelper.getLevel(KNOWLEDGE_ENCHANTMENT, player.getMainHandStack());
                if (knowledge == 0) {
                    knowledge = EnchantmentHelper.getLevel(KNOWLEDGE_ENCHANTMENT, player.getOffHandStack());
                }

                if (knowledge > 0) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("[KOTA] Detected kill: level {} on {}. Triggering area scan fix...",
                                knowledge, target.getType().getTranslationKey());
                    }

                    int immediateCount = 0;
                    if (drops != null && !drops.isEmpty()) {
                        for (ItemEntity item : drops) {
                            immediateCount += item.getStack().getCount();
                        }
                        drops.clear();
                    }

                    synchronized (pendingConversions) {
                        pendingConversions.add(new PendingXpConversion(
                                (ServerWorld) target.getWorld(),
                                target.getX(), target.getY(), target.getZ(),
                                knowledge, immediateCount));
                    }
                }
            }
        }
        return true;
    }

    private static class PendingXpConversion {
        private final ServerWorld world;
        private final double x, y, z;
        private final int knowledge;
        private int totalItems;
        private int ticksLeft = 4;

        public PendingXpConversion(ServerWorld world, double x, double y, double z, int knowledge, int initialCount) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.knowledge = knowledge;
            this.totalItems = initialCount;
        }

        public boolean tick() {
            if (--this.ticksLeft > 0) {
                return false;
            }
            
            if (world == null) return true;

            Box area = new Box(x - 2.0, y - 2.0, z - 2.0, x + 2.0, y + 2.0, z + 2.0);
            
            // Safe filter removed: Zenith likely sets the owner of the drops it spawns, causing the filters to reject the items.
            // Reverting to the raw area scan that was proven to work.
            List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class, area, e -> !e.isRemoved());

            for (ItemEntity item : items) {
                totalItems += item.getStack().getCount();
                item.discard();
            }

            if (totalItems > 0) {
                int xp = totalItems * knowledge * 25;
                ExperienceOrbEntity.spawn(world, new Vec3d(x, y, z), xp);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("[KOTA] Fixed V4: Converted {} items into {} XP orbs.", totalItems, xp);
                }
            }
            return true;
        }
    }
}
