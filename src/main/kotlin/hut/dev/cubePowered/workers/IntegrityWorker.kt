package hut.dev.cubePowered.workers

import dev.triumphteam.gui.guis.Gui
import hut.dev.cubePowered.library.Lists
import net.kyori.adventure.text.logger.slf4j.ComponentLogger.logger
import org.bukkit.inventory.ItemStack

object IntegrityWorker {

    /**
     * Checks if input/output/fuel slots are within bounds for the given inventory type + size,
     * and ensures no overlapping slots between groups.
     *
     * Logs errors if invalid, but never throws.
     *
     * @return true if all slots are valid and non-overlapping, false otherwise.
     */
    fun verifySlotIntegrity(
        machineId: String,
        inventorySize: Int,
        inputSlots: List<Int>,
        outputSlots: List<Int>,
        fuelSlots: List<Int>
    ): Boolean {
        val effectiveInventorySize = when ("chest") {
            "chest" -> inventorySize
            else -> inventorySize.coerceAtLeast(0)
        }

        fun List<Int>.isInBounds(): Boolean = this.all { it in 0 until effectiveInventorySize }

        if (!inputSlots.isInBounds()) {
            logger().error("Machine '$machineId': input slot index out of bounds (size=$effectiveInventorySize). slots=$inputSlots")
            return false
        }
        if (!outputSlots.isInBounds()) {
            logger().error("Machine '$machineId': output slot index out of bounds (size=$effectiveInventorySize). slots=$outputSlots")
            return false
        }
        if (!fuelSlots.isInBounds()) {
            logger().error("Machine '$machineId': fuel slot index out of bounds (size=$effectiveInventorySize). slots=$fuelSlots")
            return false
        }

        // ---- NEW: Overlap check ----
        val inputSet = inputSlots.toSet()
        val outputSet = outputSlots.toSet()
        val fuelSet = fuelSlots.toSet()

        val overlaps = mutableListOf<String>()

        if (inputSet.intersect(outputSet).isNotEmpty()) {
            overlaps.add("input/output: ${inputSet.intersect(outputSet)}")
        }
        if (inputSet.intersect(fuelSet).isNotEmpty()) {
            overlaps.add("input/fuel: ${inputSet.intersect(fuelSet)}")
        }
        if (outputSet.intersect(fuelSet).isNotEmpty()) {
            overlaps.add("output/fuel: ${outputSet.intersect(fuelSet)}")
        }

        if (overlaps.isNotEmpty()) {
            logger().error("Machine '$machineId': slot overlap detected → ${overlaps.joinToString("; ")}")
            return false
        }
        // ---- END overlap check ----

        return true
    }
    /** Returns CHEST rows for bounds-checking; null for fixed-size GUIs. */
    fun getRowsFromInfo(inventoryType: String, inventorySize: Int): Int? {
        if (inventoryType == "CHEST") {
            require(inventorySize in setOf(9, 18, 27, 36, 45, 54)) { "Invalid chest size: " + inventorySize + " (allowed: 9,18,27,36,45,54)" }
            return inventorySize / 9
        }
        else if (inventoryType == "DISPENSER" || inventoryType == "DROPPER") {
            return 3
        }
        else if (inventoryType == "HOPPER") {
            return 1
        }
        return 1// hopper/dispenser/furnace/brewing/etc. are fixed-size
    }

    /** Compute locked slots = all slots minus input & output (common pattern). */
    fun getLockedSlotsFromInfo(
        inventorySize: Int,
        inputSlots: List<Int>,
        outputSlots: List<Int>
    ): List<Int> {
        val unlocked = (inputSlots + outputSlots).toSet()
        return (0 until inventorySize).filter { it !in unlocked }
    }

    fun getItemNamespace(stack: ItemStack?): String {
        if (stack != null) {
            // 1) Try ItemsAdder: if this stack is a custom item, return its namespaced id (e.g., "mypack:dark_glass")
            try {
                val cs = dev.lone.itemsadder.api.CustomStack.byItemStack(stack)
                if (cs != null) return cs.namespacedID
            } catch (_: Throwable) {
                // ItemsAdder not present or API version differs — fall back to vanilla path
            }

            // 2) Vanilla: material’s namespaced key (e.g., "minecraft:black_stained_glass_pane")
            return try {
                val key = stack.type.key  // org.bukkit.NamespacedKey
                "${key.namespace}:${key.key}"
            } catch (_: Throwable) {
                // Older APIs without Material.key(): best-effort fallback
                "minecraft:" + stack?.type?.name?.lowercase()
            }
        } else {
            return "minecraft:air"
        }
    }

    fun getItemStackFromSlot(currentGui: Gui, currentSlot: Int) : ItemStack
    {
        return currentGui.guiItems.get(currentSlot)!!.itemStack

    }
}