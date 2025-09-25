package hut.dev.cubePowered.listeners

import dev.lone.itemsadder.api.Events.CustomBlockBreakEvent
import dev.lone.itemsadder.api.Events.CustomBlockInteractEvent
import dev.lone.itemsadder.api.Events.CustomBlockPlaceEvent
import hut.dev.cubePowered.library.Lists
import hut.dev.cubePowered.workers.MainGuiWorker
import hut.dev.cubePowered.workers.PlacedMachineWorker
import hut.dev.cubePowered.workers.RecipeWorker
import org.bukkit.event.EventHandler
import org.bukkit.plugin.Plugin
import org.bukkit.event.Listener

class ItemsAdderListener(private val plugin: Plugin) : Listener
{
    @EventHandler(ignoreCancelled = true)
    fun onCustomBlockPlace(e: CustomBlockPlaceEvent)
    {
        val blockID = e.namespacedID
        val blockLocation = e.block.location
        val blockWorld = e.block.world.name
        plugin.logger.info("CustomBlock " + blockID + " placed at " + blockWorld + " " + blockLocation.x + " " + blockLocation.y + " " + blockLocation.z)
        if (Lists.machineInstances.any{ it.model == blockID } == true)
        {
            PlacedMachineWorker.handleNewPlacedMachine(e, plugin)
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onCustomBlockBreak(e: CustomBlockBreakEvent)
    {
        val blockID = e.namespacedID
        val blockLocation = e.block.location
        val blockWorld = e.block.world.name
        plugin.logger.info("CustomBlock " + blockID + " broken at " + blockWorld + " " + blockLocation.x + " " + blockLocation.y + " " + blockLocation.z)
        PlacedMachineWorker.handleBrokenMachine(e, plugin)
    }

    @EventHandler(ignoreCancelled = true)
    fun onCustomBlockInteract(e: CustomBlockInteractEvent)
    {
        val blockID = e.namespacedID
        val blockLocation = e.blockClicked.location
        val blockWorld = e.blockClicked.world.name
        plugin.logger.info("CustomBlock " + blockID + " interacted at " + blockWorld + " " + blockLocation.x + " " + blockLocation.y + " " + blockLocation.z)
        for (placedMachineToCheck in Lists.placedMachinesInstances)
        {
            if (placedMachineToCheck.machineKey == (blockWorld + "," + blockLocation.blockX.toString() + "," + blockLocation.blockY.toString() + "," + blockLocation.blockZ.toString()))
            {
                plugin.logger.info("Valid machine was interacted with at " + blockID + " interacted at " + blockWorld + " " + blockLocation.x + " " + blockLocation.y + " " + blockLocation.z + " with machineKey" + placedMachineToCheck.machineKey)
                if(placedMachineToCheck.inUse == false && Lists.placedMachinesGuiInstances[placedMachineToCheck.machineKey] != null) {
                    plugin.logger.info("Opening placed machine GUI with ID " + blockID + " at " + blockWorld + " " + blockLocation.x + " " + blockLocation.y + " " + blockLocation.z + " with machineKey" + placedMachineToCheck.machineKey)
                    MainGuiWorker.openMachineGuiForPlayer(plugin, e.player, placedMachineToCheck, Lists.placedMachinesGuiInstances[placedMachineToCheck.machineKey]!!)
                    break
                }
                else
                {
                    e.player.sendMessage("This " + placedMachineToCheck.machineTitle + " is currently in use!")
                    break
                }
            }
            else
            {
                plugin.logger.info("Invalid machine was interacted with at " + blockID + " interacted at " + blockWorld + " " + blockLocation.x + " " + blockLocation.y + " " + blockLocation.z + " with machineKey" + placedMachineToCheck.machineKey)
            }
        }
    }
}