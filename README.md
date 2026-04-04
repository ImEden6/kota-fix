# Kota Fix (Zenith x Create Compatibility)

**Kota Fix** is a lightweight Fabric 1.20.1 compatibility patch designed to restore the functionality of **Zenith's Knowledge Enchantment** when the **Create** mod is present.

## The Core Issue

Both Zenith and Create rely on a shared library called `porting_lib`. This library's `DROPS` event system contains a critical design flaw: it stops processing handlers as soon as any handler returns `true`.

In many modpacks, Create's event handlers (which manage things like Deployer kills or Crushing Wheel drops) are registered earlier or run first. Even if a regular player performs the kill, logic within these handlers or other mods using the same library can trigger an early return, preventing Zenith's **Knowledge** handler from ever executing. This effectively "breaks" the Knowledge enchantment, making it impossible to gain the 25x XP bonus from Mob drops.

## Technical Implementation (The "Area Scanner" Fix)

Instead of attempting a fragile Mixin patch on the embedded `porting_lib` code, **Kota Fix** implements a robust "post-processing" solution:

1. **Event Interception**: The mod registers its own high-priority handler on the `DROPS` event.
2. **Kill Detection**: When a player with the **Knowledge** enchantment kills a mob, the mod identifies the event and calculates the expected XP multiplier based on the enchantment level.
3. **Buffered Conversion**:
   - The mod immediately clears items from the `drops` collection to prevent baseline looting.
   - It registers a **2-tick delayed Area Scan** around the death location.
4. **The Scan**: After 2 ticks (ensuring all other mods have finished their loot-spawning logic), the mod scans a 1x1x1 radius for any orphaned `ItemEntity` objects.
5. **XP Transformation**: All found items are discarded and converted into `ExperienceOrbEntity` objects using the standard Zenith formula (`totalItems * knowledge * 25`).

## Compatibility

- **Minecraft**: 1.20.1
- **Mod Loader**: Fabric
- **Required Mods**: Zenith (or Apotheosis), Create (specifically versions using `porting_lib`).
- **Logic**: This mod acts as a bridge and is safe to use in any modpack where Zenith Knowledge is failing to trigger.

## License

This mod is available under the **CC0-1.0** license. Feel free to include it in any modpack or learn from its implementation.
