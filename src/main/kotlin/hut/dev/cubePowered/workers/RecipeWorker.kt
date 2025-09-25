package hut.dev.cubePowered.workers

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.GuiItem
import dev.triumphteam.gui.guis.StorageGui
import hut.dev.cubePowered.library.MachineInstance
import hut.dev.cubePowered.library.RecipeInstance
import hut.dev.cubePowered.library.Lists
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin

class RecipeWorker {
    fun processRecipe(currentRecipe: RecipeInstance, currentPlacedMachineInstance: MachineInstance, currentGui: StorageGui, plugin: Plugin ) {
        val currentRecipeTaskKey = currentPlacedMachineInstance.machineKey//assign a task an id based on the machine it's trying to execute in
        if(Lists.currentRecipeTasks[currentRecipeTaskKey] != null)
        {
            plugin.logger.info("Current task with key " + currentRecipeTaskKey + " is already running! New instances are blocked!")
            return//end task early if the task is already running
        }
        val currentRecipeTask = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
                if (currentPlacedMachineInstance.machinePowerMode == true) {
                    plugin.logger.info("Current machine is powered on!, checking recipe details...")
                    // 0) sanity: make sure this recipe belongs to this machine
                    if (currentRecipe.assignedMachineInstance.machineId != currentPlacedMachineInstance.machineId) return@Runnable
                    plugin.logger.info("Properly registered recipe to machine!, iterating recipe details...")
                    val currentGuiInventory = currentGui.inventory

                    // 1) verify inputs exist with enough amount
                    for ((slot, requiredItem) in currentRecipe.recipeInputItems) {
                        val currentItem = currentGuiInventory.getItem(slot)
                        if (currentItem == null || currentItem.type == Material.AIR) return@Runnable
                        plugin.logger.info("Verified item for slot " + slot + "! (1/3)")
                        plugin.logger.info("Found: " + currentItem.amount + "*" + currentItem.type + "|Expected: " + requiredItem.amount + "*" + requiredItem.type + "| Slot: " + slot + "|Detailed data comparison: " + requiredItem.toString() + " |<|>| " + currentItem.toString()  )
                        if (!currentItem.isSimilar (requiredItem)) return@Runnable              // same type/meta (works for IA items if stacks match)
                        plugin.logger.info("Verified item for slot " + slot + "! (2/3)")
                        if (currentItem.amount < requiredItem.amount) return@Runnable              // not enough
                        plugin.logger.info("Verified item for slot " + slot + "! (3/3)")
                    }
                    plugin.logger.info("Input slots verified! Checking output slots...")

                    // 2) verify outputs fit
                    for ((slot, outputSlot) in currentRecipe.recipeOutputItems) {
                        val currentItem = currentGuiInventory.getItem(slot)
                        if (currentItem == null || currentItem.type == Material.AIR) continue
                        if (!currentItem.isSimilar(outputSlot)) return@Runnable                  // different item sitting in output
                        val max = currentItem.maxStackSize
                        if (currentItem.amount + outputSlot.amount > max) return@Runnable        // would overflow
                    }
                    plugin.logger.info("Output slots verified!, consuming input items...")

                    // 3) consume inputs (subtract amounts)
                    for ((slot, requiredItem) in currentRecipe.recipeInputItems) {
                        val currentItem = currentGuiInventory.getItem(slot) ?: continue
                        val newAmount = currentItem.amount - requiredItem.amount
                        if (newAmount <= 0) {
                            currentGuiInventory.clear(slot)
                            currentGui.removeItem(slot)
                        } else {
                            currentItem.amount = newAmount
                            currentGui.inventory.setItem(slot, currentItem)

                        }
                        plugin.logger.info("Consumed item " + requiredItem.type.name)
                    }

                    // 4) place outputs (merge or set)
                    for ((slot, outputSlot) in currentRecipe.recipeOutputItems) {
                        val existingItem = currentGuiInventory.getItem(slot)
                        if (existingItem == null || existingItem.type == Material.AIR) {
                            val put = outputSlot.clone()
                            currentGui.inventory.setItem(slot, put)
                        } else {
                            existingItem.amount =
                                (existingItem.amount + outputSlot.amount).coerceAtMost(existingItem.maxStackSize)
                            currentGui.inventory.setItem(slot, existingItem)
                        }
                        plugin.logger.info("Ejected output item in slot " + slot + "!")
                    }
                }
                else
                {
                    return@Runnable
                }
            }, currentRecipe.recipeProcessingTime/2L, currentRecipe.recipeProcessingTime/2L//tweak processing speed algo at some point!
        )
        Lists.currentRecipeTasks[currentRecipeTaskKey] = currentRecipeTask
        plugin.logger.info("Stored recipe task in memory with key " + currentRecipeTaskKey + "!")
    }
}