package hut.dev.cubePowered.workers

import dev.lone.itemsadder.api.Events.CustomBlockBreakEvent
import dev.lone.itemsadder.api.Events.CustomBlockInteractEvent
import dev.lone.itemsadder.api.Events.CustomBlockPlaceEvent
import dev.triumphteam.gui.guis.Gui
import hut.dev.cubePowered.library.Lists
import hut.dev.cubePowered.library.MachineInstance
import net.kyori.adventure.text.Component
import org.bukkit.plugin.Plugin

internal object PlacedMachineWorker {

    fun handleNewPlacedMachine(e: CustomBlockPlaceEvent, plugin: Plugin) {
        val blockID = e.namespacedID
        val blockX = e.block.x
        val blockY = e.block.y
        val blockZ = e.block.z
        val blockWorld = e.block.world.name

        var foundDuplicate = false
        for (machineLocToCheck in Lists.placedMachinesInstances) {
            if (machineLocToCheck.machineX == blockX && machineLocToCheck.machineY == blockY && machineLocToCheck.machineZ == blockZ && machineLocToCheck.machineWorld == blockWorld) {
                foundDuplicate = true
                plugin.logger.info("Stopped invalid machine registration to avoid duplicates!")
                break
            }
        }
        if (foundDuplicate == false) {
            plugin.logger.info("Placed valid machine instance of " + blockID + " at " + blockX + " " + blockY + " " + blockZ + " in world " + blockWorld)

            val currentMachineInstance = Lists.machineInstances.first { it.model == blockID }//fetch type from ID
            val placedMachineInstanceToAdd = MachineInstance(
                machineId = currentMachineInstance.machineId,
                machineTitle = currentMachineInstance.machineTitle,
                machineX = blockX,
                machineY = blockY,
                machineZ = blockZ,
                machineWorld = blockWorld,
                machineKey = blockWorld + "," + blockX + "," + blockY + "," + blockZ,                 //register machine's runtime
                machineContents = currentMachineInstance.machineContents,
                model = currentMachineInstance.model,
                fuelTypes = currentMachineInstance.fuelTypes,
                guiOnLabel = currentMachineInstance.guiOnLabel,
                guiOffLabel = currentMachineInstance.guiOffLabel,
                energyBuffer = currentMachineInstance.energyBuffer,
                energyEnabled = currentMachineInstance.energyEnabled,
                energyPerTick = currentMachineInstance.energyPerTick,
                inventorySize = currentMachineInstance.inventorySize,
                behaviorOnFull = currentMachineInstance.behaviorOnFull,
                guiRows = currentMachineInstance.guiRows,
                guiOnItemModel = currentMachineInstance.guiOnItemModel,
                guiOffItemModel = currentMachineInstance.guiOffItemModel,
                guiToggleItemSlot = currentMachineInstance.guiToggleItemSlot,
                guiOutputSlots = currentMachineInstance.guiOutputSlots,
                guiInputSlots = currentMachineInstance.guiInputSlots,
                guiLockedSlots = currentMachineInstance.guiLockedSlots,
                machinePowerMode = currentMachineInstance.machinePowerMode,
                processingInterval = currentMachineInstance.processingInterval,
                lockedSlotsMaterial = currentMachineInstance.lockedSlotsMaterial,
                processingToggleable = currentMachineInstance.processingToggleable,
                behaviourPauseIfNoEnergy = currentMachineInstance.behaviourPauseIfNoEnergy,
                inUse = false,
                currentPlayerUUID = null
            )
            Lists.placedMachinesInstances.add(placedMachineInstanceToAdd)
            plugin.logger.info("Successfully registered machine placement with ID: " + placedMachineInstanceToAdd.machineId + " at world " + placedMachineInstanceToAdd.machineWorld + " with coordinates " + placedMachineInstanceToAdd.machineX + " " + placedMachineInstanceToAdd.machineY + " " + placedMachineInstanceToAdd.machineZ + " !")
            plugin.logger.info("Creating GUI instance for machine with ID: " + placedMachineInstanceToAdd.machineId + " and with " + placedMachineInstanceToAdd.guiRows + " rows")

            val guiInstanceToAdd = Gui.storage().title(Component.text(placedMachineInstanceToAdd.machineTitle))
                .rows(placedMachineInstanceToAdd.guiRows!!).create()
            //Potential improvement: DRAW THE BASE UI HERE LIKE THE LOCKED SLOTS AND TOGGLE SLOTS INSTEAD OF RE-DRAWING THEM ON INTERACT
            Lists.placedMachinesGuiInstances[placedMachineInstanceToAdd.machineKey] =
                guiInstanceToAdd//Assign value to key on GUIs mapping
            plugin.logger.info("Successfully created GUI instance for machine with ID: " + placedMachineInstanceToAdd.machineId + " shared key: " + placedMachineInstanceToAdd.machineKey)

            plugin.logger.info("Registering recipe ticker for machine with key " + placedMachineInstanceToAdd.machineKey)
            for (currentRecipe in Lists.recipeInstances) {
                RecipeWorker().processRecipe(currentRecipe, placedMachineInstanceToAdd, guiInstanceToAdd!!, plugin)
                plugin.logger.info("Registered recipe " + currentRecipe.recipeId + " to machine with key " + placedMachineInstanceToAdd.machineKey)
            }
        }
    }

    fun handleBrokenMachine(e: CustomBlockBreakEvent, plugin: Plugin) {
        val blockX = e.block.x
        val blockY = e.block.y
        val blockZ = e.block.z
        val blockWorld = e.block.world.name
        plugin.logger.info("Grabbed broken block relevant info...")
        //find and remove the machine from memory
        val placedMachineToRemove = Lists.placedMachinesInstances.first { it.machineKey == blockWorld + "," + blockX + "," + blockY + "," + blockZ }
        Lists.placedMachinesGuiInstances.remove(placedMachineToRemove.machineKey)
        plugin.logger.info("Removed instance from memory")
        //find and remove machine runtime
        for (recipeTaskToRemove in Lists.currentRecipeTasks) {
            val currentRecipe = Lists.recipeInstances.first { it.assignedMachineInstance == placedMachineToRemove }
            val recipeTaskToRemoveKey = currentRecipe.recipeId + placedMachineToRemove.machineKey
            val recipeTaskToRemove = Lists.currentRecipeTasks[recipeTaskToRemoveKey]
            Lists.currentRecipeTasks.remove(recipeTaskToRemoveKey)!!.cancel()
            plugin.logger.info("Removed recipe task with key " + recipeTaskToRemoveKey)
        }
    }
}