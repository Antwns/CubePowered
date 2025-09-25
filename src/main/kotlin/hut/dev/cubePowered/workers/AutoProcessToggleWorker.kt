package hut.dev.cubePowered.workers

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.GuiItem
import hut.dev.cubePowered.library.CommonItems
import hut.dev.cubePowered.library.Lists
import hut.dev.cubePowered.library.MachineInstance
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.plugin.Plugin

object AutoProcessToggleWorker
{
    fun toggleAutoProcessMode(plugin: Plugin, e: InventoryClickEvent, currentPlacedMachineInstance: MachineInstance, toggleSlot: Int)
    {
        //create the common items //EXTRA: I MAY NEED TO INCLUDE THOSE IN A LIBRARY INSTEAD OF RE-CREATING HERE
        val currentMachineGuiOnStack = ItemBuilder.from(CommonItems.autoProcessOnStack(currentPlacedMachineInstance.guiOnItemModel, currentPlacedMachineInstance.guiOnLabel)).asGuiItem() { e -> toggleAutoProcessMode(plugin, e, currentPlacedMachineInstance, currentPlacedMachineInstance.guiToggleItemSlot)}
        val currentMachineGuiOffStack = ItemBuilder.from(CommonItems.autoProcessOffStack(currentPlacedMachineInstance.guiOffItemModel, currentPlacedMachineInstance.guiOffLabel)).asGuiItem() { e -> toggleAutoProcessMode(plugin, e, currentPlacedMachineInstance, currentPlacedMachineInstance.guiToggleItemSlot)}
        // This is our toggle -> cancel and flip state
        val toggleItemToDisplay: GuiItem
        e.isCancelled = true
        if(currentPlacedMachineInstance.machinePowerMode == true) {
            currentPlacedMachineInstance.machinePowerMode = false
            toggleItemToDisplay = currentMachineGuiOffStack
        }
        else {
            currentPlacedMachineInstance.machinePowerMode = true
            toggleItemToDisplay = currentMachineGuiOnStack
        }
        plugin.server.scheduler.runTask(plugin, Runnable {
            Lists.placedMachinesGuiInstances[currentPlacedMachineInstance.machineKey]!!.updateItem(toggleSlot, toggleItemToDisplay)
        })

        plugin.logger.warning("[InvClick] auto-processing mode slot " + toggleSlot + " to " + currentPlacedMachineInstance.machinePowerMode)
    }
}