package com.mervyn.kotafix.init;

import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import java.util.Collection;

public class AreaScannerHandler {

    private static Enchantment KNOWLEDGE_ENCHANTMENT = null;

    public static void init() {
        LivingEntityEvents.DROPS.register((target, source, drops, lootingLevel, recentlyHit) -> 
            onDrops((LivingEntity) target, (DamageSource) source, (Collection<ItemEntity>) drops, lootingLevel, recentlyHit)
        );
        KotaFixMod.LOGGER.info("[KOTA FIX] Area Scanner Handler Initialized.");
    }

    private static boolean onDrops(LivingEntity target, DamageSource source, Collection<ItemEntity> drops, int lootingLevel, boolean recentlyHit) {
        // STRICT PLAYER GUARD: Never convert player items to XP
        if (target instanceof PlayerEntity) {
            return false;
        }

        Entity attacker = source.getAttacker();
        if (!(attacker instanceof PlayerEntity player)) {
            return false;
        }

        if (drops == null || drops.isEmpty()) {
            return false;
        }

        int knowledgeLevel = getKnowledgeLevel(player);
        if (knowledgeLevel <= 0) {
            return false;
        }

        // Calculate total items
        long totalItems = 0;
        for (ItemEntity itemEntity : drops) {
            ItemStack stack = itemEntity.getStack();
            totalItems += stack.getCount();
        }

        if (totalItems <= 0) {
            return false;
        }

        // Clear the drops so they don't spawn as items
        drops.clear();

        // Calculate XP: knowledgeLevel * 25 per item (Zenith formula)
        long totalXp = totalItems * knowledgeLevel * 25;

        // Spawn XP orbs
        World world = target.getWorld();
        while (totalXp > 0) {
            int orbValue = getNextOrbSize((int) Math.min(totalXp, Integer.MAX_VALUE));
            totalXp -= orbValue;
            world.spawnEntity(new ExperienceOrbEntity(world, target.getX(), target.getY(), target.getZ(), orbValue));
        }

        return true;
    }

    /**
     * Helper to determine the size of the next XP orb, mirroring vanilla logic.
     */
    private static int getNextOrbSize(int value) {
        if (value >= 2477) return 2477;
        if (value >= 1237) return 1237;
        if (value >= 617) return 617;
        if (value >= 307) return 307;
        if (value >= 149) return 149;
        if (value >= 73) return 73;
        if (value >= 37) return 37;
        if (value >= 17) return 17;
        if (value >= 7) return 7;
        if (value >= 3) return 3;
        return 1;
    }

    private static int getKnowledgeLevel(PlayerEntity player) {
        if (KNOWLEDGE_ENCHANTMENT == null) {
            // Try to find the enchantment in the registry
            KNOWLEDGE_ENCHANTMENT = Registries.ENCHANTMENT.get(new Identifier("zenith", "knowledge"));
            if (KNOWLEDGE_ENCHANTMENT == null) {
                KNOWLEDGE_ENCHANTMENT = Registries.ENCHANTMENT.get(new Identifier("apotheosis", "knowledge"));
            }
        }

        if (KNOWLEDGE_ENCHANTMENT == null) return 0;

        return EnchantmentHelper.getLevel(KNOWLEDGE_ENCHANTMENT, player.getMainHandStack());
    }
}
