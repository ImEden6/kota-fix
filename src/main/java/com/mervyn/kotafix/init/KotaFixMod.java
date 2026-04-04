package com.mervyn.kotafix.init;

import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class KotaFixMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("kota_fix");
    private final List<PendingXpConversion> pendingConversions = new ArrayList<>();

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing KOTA Fix mod (V2 - Area Scanner) for Zenith...");
        LivingEntityEvents.DROPS.register(this::onDrops);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            synchronized (pendingConversions) {
                pendingConversions.removeIf(PendingXpConversion::tick);
            }
        });
    }

    private boolean onDrops(LivingEntity target, DamageSource source, Collection<ItemEntity> drops, int lootingLevel, boolean recentlyHit) {
        if (target instanceof PlayerEntity || target.getWorld().isClient) return false;

        if (source != null && source.getAttacker() instanceof PlayerEntity player) {
            Enchantment knowledgeEnchant = Registries.ENCHANTMENT.get(new Identifier("zenith", "knowledge"));
            if (knowledgeEnchant == null) knowledgeEnchant = Registries.ENCHANTMENT.get(new Identifier("apotheosis", "knowledge"));

            if (knowledgeEnchant != null) {
                int knowledge = EnchantmentHelper.getLevel(knowledgeEnchant, player.getMainHandStack());
                if (knowledge == 0) knowledge = EnchantmentHelper.getLevel(knowledgeEnchant, player.getOffHandStack());

                if (knowledge > 0) {
                    LOGGER.debug("KOTA detected kill: level " + knowledge + " on " + target.getType().getTranslationKey() + ". Triggering area scan fix...");
                    
                    // Immediately convert any items already in the list
                    int immediateCount = 0;
                    if (drops != null && !drops.isEmpty()) {
                        for (ItemEntity item : drops) immediateCount += item.getStack().getCount();
                        drops.clear(); 
                    }

                    // Register a delayed scan for items spawned outside the event by other mods
                    synchronized (pendingConversions) {
                        pendingConversions.add(new PendingXpConversion(
                            (ServerWorld) target.getWorld(), 
                            target.getX(), target.getY(), target.getZ(), 
                            knowledge, immediateCount
                        ));
                    }
                }
            }
        }
        return false;
    }

    private static class PendingXpConversion {
        private final ServerWorld world;
        private final double x, y, z;
        private final int knowledge;
        private int totalItems;
        private int ticksLeft = 2; // Wait 2 ticks to ensure all mods have finished spawning loot

        public PendingXpConversion(ServerWorld world, double x, double y, double z, int knowledge, int initialCount) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.knowledge = knowledge;
            this.totalItems = initialCount;
        }

        public boolean tick() {
            if (--ticksLeft > 0) return false;

            // Scan 1 block around death location for dropped items
            Box area = new Box(x - 1.0, y - 1.0, z - 1.0, x + 1.0, y + 1.0, z + 1.0);
            List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class, area, e -> true);
            
            for (ItemEntity item : items) {
                totalItems += item.getStack().getCount();
                item.discard(); // Remove items to convert to XP
            }

            if (totalItems > 0) {
                int xp = totalItems * knowledge * 25;
                ExperienceOrbEntity.spawn(world, new net.minecraft.util.math.Vec3d(x, y, z), xp);
                LOGGER.debug("KOTA Fix V2: Converted " + totalItems + " items into " + xp + " XP orbs (Zenith Multiplier).");
            }
            return true;
        }
    }
}
