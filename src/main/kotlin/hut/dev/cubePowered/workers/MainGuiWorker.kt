package hut.dev.cubePowered.workers

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.StorageGui
import hut.dev.cubePowered.library.CommonItems
import hut.dev.cubePowered.library.CommonItems.idToItemStack
import hut.dev.cubePowered.library.MachineInstance
import hut.dev.cubePowered.workers.IntegrityWorker.getItemNamespace
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

object MainGuiWorker {

    fun openMachineGuiForPlayer(plugin: Plugin, currentPlayer: Player, currentPlacedMachineInstance: MachineInstance, currentPlacedMachineGuiInstance : StorageGui) {

        //mark the machine as occupied
        currentPlacedMachineInstance.inUse = true
        currentPlacedMachineInstance.currentPlayerUUID = currentPlayer.uniqueId
        //populate base GUI
        //create the common items
        val currentMachineGuiOnStack = ItemBuilder.from(CommonItems.autoProcessOnStack(currentPlacedMachineInstance.guiOnItemModel, currentPlacedMachineInstance.guiOnLabel)).asGuiItem() { e -> AutoProcessToggleWorker.toggleAutoProcessMode(plugin, e, currentPlacedMachineInstance, currentPlacedMachineInstance.guiToggleItemSlot)}
        val currentMachineGuiOffStack = ItemBuilder.from(CommonItems.autoProcessOffStack(currentPlacedMachineInstance.guiOffItemModel, currentPlacedMachineInstance.guiOnLabel)).asGuiItem() { e -> AutoProcessToggleWorker.toggleAutoProcessMode(plugin, e, currentPlacedMachineInstance, currentPlacedMachineInstance.guiToggleItemSlot)}

        for (currentSlot in 0 until (currentPlacedMachineGuiInstance.rows * 9)) {
            if (currentPlacedMachineInstance.guiLockedSlots.contains(currentSlot)) {
                currentPlacedMachineGuiInstance.setItem(currentSlot, CommonItems.filler(currentPlacedMachineInstance.lockedSlotsMaterial))
                plugin.logger.warning("Attempting to populate slot " + currentSlot + " with " + currentPlacedMachineInstance.lockedSlotsMaterial)
            }
            if (currentPlacedMachineInstance.guiToggleItemSlot != null && currentPlacedMachineInstance.guiToggleItemSlot == currentSlot)
            {
                if(currentPlacedMachineInstance.machinePowerMode == true) {
                    currentPlacedMachineGuiInstance.setItem(currentSlot,  currentMachineGuiOnStack)
                }
                else
                {
                    currentPlacedMachineGuiInstance.setItem(currentSlot,  currentMachineGuiOffStack)
                }
                plugin.logger.warning("Attempting to populate slot " + currentSlot + " with " + getItemNamespace(idToItemStack(currentPlacedMachineInstance.guiOnItemModel)))
            }
            if (currentPlacedMachineInstance.guiLockedSlots.contains(currentSlot) == false && currentPlacedMachineInstance.guiToggleItemSlot != currentSlot)
            {
                    MachineRuntimeInventoryWorker.loadSlotData(plugin, currentPlacedMachineGuiInstance, currentSlot, currentPlacedMachineInstance.machineKey)
            }
        }
        currentPlacedMachineGuiInstance.open(currentPlayer)
    }
}