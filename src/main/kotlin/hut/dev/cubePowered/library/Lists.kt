package hut.dev.cubePowered.library

import dev.triumphteam.gui.guis.StorageGui
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask

object Lists
{
    val machineInstances: MutableList<MachineInstance> = mutableListOf()

    val placedMachinesInstances: MutableList<MachineInstance> = mutableListOf()

    val placedMachinesGuiInstances: MutableMap<String, StorageGui> = mutableMapOf()

    val recipeInstances: MutableList<RecipeInstance> = mutableListOf()

    val currentRecipeTasks: MutableMap<String, BukkitTask> = mutableMapOf()

    val conductorInstances: MutableList<ConductorInstance> = mutableListOf()

    val placedConductorInstances: MutableMap<String, ConductorInstance> = mutableMapOf()

    val placedPowerNodes: MutableMap<String, PowerNode> = mutableMapOf()
}