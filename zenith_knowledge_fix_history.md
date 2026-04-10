# Zenith Knowledge - Bug Fix History & Post-Mortem

This document serves as a historical record of the failed approaches attempted while fixing the Zenith Knowledge enchantment in the KOTA modpack environment.

## The Core Problem
The **Zenith/Apotheosis Knowledge Enchantment** works by intercepting mob item drops, deleting them, and spawning Experience Orbs calculated by the item count. In the KOTA modpack, this mechanic completely breaks: items drop normally, and zero bonus XP is granted.

## ❌ Attempt 1: Relying on `LivingEntityEvents.DROPS` (Zenith's Native Method)
**The Approach:**
Zenith uses Fabric's (Porting Lib's) `LivingEntityEvents.DROPS` to iterate through the `drops` list, calculate the XP, and clear the list. Initially, we tried to verify and patch this event hook.

**Why it failed:** 
Another mod in the pack (or a deep Mixin conflict) completely circumvents the standard `LivingEntity.dropLoot()` collection pipeline. Instead of items passing through the `drops` list, they are instantiated and hard-spawned directly into the physical world. 
Because the `drops` collection arrives at the event completely **empty**, Zenith (and any attempt to piggyback off the event) loops over 0 items, shrugs, and spawns 0 XP.

## ❌ Attempt 2: Removing the Area Scanner (The V3 Illusion)
**The Approach:**
After writing a successful low-level Mixin (`LivingEntityEventsMixin`) that hacked the Event Factory initialization, we wrongly theorized that the `drops` collection was being populated but bypassed. We deleted the bulky "Area Scanner" and tried iterating over the `drops` collection directly.

**Why it failed:**
The Mixin successfully ensured the event callbacks fired, but **the underlying collection remained empty**. As proven in Attempt 1, the items just do not enter this pipeline. The mod returned to its original broken state.

## ❌ Attempt 3: The "Safe" Area Scanner with Filters (The V4 Trap)
**The Approach:**
Realizing the physical Area Scanner (waiting 4 ticks and scanning a 4x4x4 box around the corpse) was the absolute only way to intercept the items, we brought it back. However, to prevent accidentally deleting items dropped *by the player* or stray items on the floor, we added two strict safety filters:
1. `getItemAge() < 10`: Only grab brand newly spawned items.
2. `getOwner() == null`: Only grab items with no explicit thrower/owner (Vanilla mobs drop ownerless loot).

**Why it failed:**
The items spawned successfully on the ground, but absolutely zero XP was generated. 
**The cause:** Some unknown core mod in the pack (likely a loot-instancing or magnet mod) explicitly stamps the killer's Player UUID into the `Owner` property of the `ItemEntity` instantly upon generation. This is completely non-vanilla behavior. Because every single genuine mob drop was signed with the player's name, our `getOwner() == null` safety filter actively rejected the actual loot.

---

## ✅ Final Working Solution (V4.1)
**The Approach:**
The pure, unfiltered Area Scanner.
1. Capture the `LivingEntityEvents.DROPS` event just to know *when* and *where* a mob died, and to cache the player's Zenith Knowledge level.
2. Schedule an asynchronous action 4 ticks later.
3. Generate a `Box` around the death coordinates and scoop up every single `ItemEntity` that is not removed (`!e.isRemoved()`), completely ignoring who owns it or how old it is.
4. Calculate the total counts and spawn XP.

> [!WARNING]
> **Why we can't change this:** 
> Modifying this raw scanner risks catastrophic failure. You cannot rely on conventional Fabric drop events, and you cannot rely on Vanilla NBT tags like `Owner` or `Age` due to how aggressively other mods manipulate the physical item spawning pipeline.
