package io.github.fabricators_of_create.porting_lib.entity.extensions;

import net.minecraft.entity.ItemEntity;
import java.util.Collection;

/**
 * Compile-time stub for Porting Lib's EntityExtensions.
 * 
 * Signature MUST match the production JAR exactly.
 */
public interface EntityExtensions {
    /**
     * Stubs the captureDrops method used by Porting Lib to manage death loot.
     */
    Collection<ItemEntity> captureDrops(Collection<ItemEntity> drops);
}
