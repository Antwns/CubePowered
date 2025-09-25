package hut.dev.cubePowered.workers

import dev.lone.itemsadder.api.Events.CustomBlockPlaceEvent
import hut.dev.cubePowered.library.ConductorInstance
import hut.dev.cubePowered.library.Lists
import org.bukkit.plugin.Plugin

object PlacedConductorWorker {
    fun handleNewPlacedConductor(e: CustomBlockPlaceEvent, plugin: Plugin) {
        val conductorInstanceType = Lists.conductorInstances.first{it.conductorModel == e.namespacedID}
        val placedConductorInstanceToAdd = ConductorInstance (
            conductorId = conductorInstanceType.conductorId,
            conductorX = e.block.x,
            conductorY = e.block.y,
            conductorZ = e.block.z,
            conductorWorld = e.block.world.name,
            conductorKey = e.block.world.name + "," + e.block.x + "," + e.block.y + "," + e.block.z,
            conductorMaxPower = conductorInstanceType.conductorMaxPower,
            conductorCurrentPower = 0,
            conductorExplodePower = conductorInstanceType.conductorExplodePower,
            conductorExplodeOnOverload = conductorInstanceType.conductorExplodeOnOverload,
            conductorVanishOnOverload = conductorInstanceType.conductorVanishOnOverload,
            conductorModel = conductorInstanceType.conductorModel
                )
        Lists.placedConductorInstances[placedConductorInstanceToAdd.conductorKey] == placedConductorInstanceToAdd
        plugin.logger.info("Placed valid conductor with key: " + placedConductorInstanceToAdd.conductorKey)
    }
}