package hut.dev.cubePowered.workers

import hut.dev.cubePowered.library.MachineInstance
import org.bukkit.plugin.Plugin

internal object PlacedMachineHelperWorker
{
    fun upsertPowerNodeOnMachinePlacement(machine: MachineInstance, plugin: Plugin)
    {
        NodeWorker.upsertPowerNodeOnMachinePlacement(machine, plugin)
    }

    fun updatePowerNodesOnMachineBreak(machineKey: String, plugin: Plugin)
    {
        NodeWorker.updatePowerNodesOnMachineBreak(machineKey, plugin)
    }
}
