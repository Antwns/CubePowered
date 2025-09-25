package hut.dev.cubePowered.workers

import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.GuiItem
import dev.triumphteam.gui.guis.StorageGui
import hut.dev.cubePowered.library.Lists
import hut.dev.cubePowered.library.MachineInstance
import hut.dev.cubePowered.workers.IntegrityWorker.getItemNamespace
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin

object MachineRuntimeInventoryWorker {

    /**
     * Capture the *current* content of a single GUI slot into PlacedMachineRuntime.slots.
     * Runs next tick so we read the post-click state.
     */
    fun saveSlotData(plugin: Plugin, currentGui: Gui, currentSlot: Int) {
        plugin.logger.info("Attempting to save saved data for slot " + currentSlot)
        plugin.server.scheduler.runTask(plugin, Runnable {
            currentGui.updateItem(currentSlot, IntegrityWorker.getItemStackFromSlot(currentGui, currentSlot))
        })
    }

    fun saveAllSlotData(plugin: Plugin, currentGui: StorageGui, guiKey: String) {
        plugin.server.scheduler.runTask(plugin, Runnable {

            plugin.logger.info("Code to save to long term storage will be put here")
        })
    }

    /**
     * Load the saved ItemStack for a given slot from PlacedMachineRuntime.
     * Returns a CLONE or null if empty/not present.
     */
    fun loadSlotData(plugin: Plugin, currentGui: StorageGui, currentSlot: Int, guiKey : String) {
        plugin.server.scheduler.runTask(plugin, Runnable {
            plugin.logger.info("Code to load a specific slot from long term storage will be put here")
        })
    }

    /**
     * Snapshot ALL editable slots (non-locked, non-toggle) into runtime.
     * Use after shift-clicks, drags, or on inventory close to ensure consistency.
     */
}