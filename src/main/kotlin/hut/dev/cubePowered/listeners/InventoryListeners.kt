package hut.dev.cubePowered.listeners

import dev.triumphteam.gui.guis.StorageGui
import hut.dev.cubePowered.library.Lists
import hut.dev.cubePowered.library.MachineInstance
import hut.dev.cubePowered.workers.IntegrityWorker
import hut.dev.cubePowered.workers.MachineRuntimeInventoryWorker
import hut.dev.cubePowered.workers.RecipeWorker
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import java.util.UUID

class InventoryListeners(private val plugin: Plugin): Listener
{
    @EventHandler
    fun onInventoryClick(e: InventoryClickEvent) {//something is returning null, must fix
        plugin.server.scheduler.runTask(plugin, Runnable {
            if (Lists.placedMachinesInstances.count() != 0) {
                if (e.clickedInventory == e.view.topInventory && Lists.placedMachinesInstances.first().currentPlayerUUID == e.whoClicked.uniqueId) {
                    // --- debug: basic click info
                    plugin.logger.info("[InvClick] start: rawSlot=" + e.rawSlot + " slot=" + e.slot + " clickedTop=" + (e.clickedInventory === e.view.topInventory) + " topSize=" + e.view.topInventory.size + " cancelled=" + e.isCancelled)

                    val currentPlayerUUID = e.whoClicked.uniqueId //only players can open Guis anyway
                    val currentMaterial: ItemStack? = e.currentItem
                    plugin.logger.warning(
                        "[InvClick] clicked slot=" + e.slot + " " + IntegrityWorker.getItemNamespace(
                            currentMaterial
                        )
                    )
                    // --- debug: basic click info end
                    var currentPlacedMachineInstance: MachineInstance
                    if (Lists.placedMachinesInstances.first { it.currentPlayerUUID == currentPlayerUUID } != null) {
                        currentPlacedMachineInstance =
                            Lists.placedMachinesInstances.first { it.currentPlayerUUID == currentPlayerUUID }
                        //only handle stuff that happens to the top inventory and ensure the correctly mapped GUI exists
                        if (Lists.placedMachinesGuiInstances[currentPlacedMachineInstance.machineKey] != null) {
                            if (e.slot != currentPlacedMachineInstance.guiToggleItemSlot) {
                                plugin.logger.warning("[InvClick] slot " + e.slot + " is available for manipulation")
                                var currentGui: StorageGui = Lists.placedMachinesGuiInstances[currentPlacedMachineInstance.machineKey]!!
                                if(currentPlacedMachineInstance.machinePowerMode == true) {
                                    val currentGui = Lists.placedMachinesGuiInstances[currentPlacedMachineInstance.machineKey]
                                    for (currentRecipe in Lists.recipeInstances) {
                                        plugin.logger.info("Attempting to process recipe " + currentRecipe.recipeId)
                                        RecipeWorker().processRecipe(currentRecipe, currentPlacedMachineInstance, currentGui!!, plugin)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        })
    }

    @EventHandler
    fun onItemInserted(e: InventoryDragEvent)
    {
        val currentMachine = Lists.placedMachinesInstances.first {it.currentPlayerUUID == e.whoClicked.uniqueId}
        if(currentMachine.machinePowerMode == true) {
            val currentGui = Lists.placedMachinesGuiInstances[currentMachine.machineKey]
            for (currentRecipe in Lists.recipeInstances) {
                plugin.logger.info("Attempting to process recipe " + currentRecipe.recipeId)
                RecipeWorker().processRecipe(currentRecipe, currentMachine, currentGui!!, plugin)
            }
        }
    }

    @EventHandler
    fun onInventoryClose(e: InventoryCloseEvent) {
        plugin.server.scheduler.runTask(plugin, Runnable {
            var currentPlayerUUID: UUID?
            try {
                currentPlayerUUID = Lists.placedMachinesInstances.first { it.currentPlayerUUID == e.player.uniqueId }.currentPlayerUUID
                if (Lists.placedMachinesInstances.count() != 0 && e.inventory == e.view.topInventory && Lists.placedMachinesInstances.any{ it.currentPlayerUUID == currentPlayerUUID}) {
                    val currentPlacedMachineInstance = Lists.placedMachinesInstances.first{ it.currentPlayerUUID == currentPlayerUUID}
                    plugin.logger.info("[InvClose] " + e.player + " closed inventory with machineKey:" + currentPlacedMachineInstance.machineKey)
                    MachineRuntimeInventoryWorker.saveAllSlotData(
                        plugin,
                        Lists.placedMachinesGuiInstances[currentPlacedMachineInstance.machineKey]!!,
                        currentPlacedMachineInstance.machineKey
                    )
                    //update temporary storage
                    Lists.placedMachinesInstances.first { it.currentPlayerUUID == e.player.uniqueId }.inUse = false
                    Lists.placedMachinesInstances.first { it.currentPlayerUUID == e.player.uniqueId }.currentPlayerUUID = null
                    //sava data to persistent storage
                    plugin.logger.info("[InvClose] updated machine with key " + currentPlacedMachineInstance.machineKey)
                    //save gui data one last time here
                }
            }
            catch (e: Exception)
            {
                //plugin.logger.info("[InvClose] couldn't update machine inventory due to " + e.printStackTrace())
            }
        })
    }
}