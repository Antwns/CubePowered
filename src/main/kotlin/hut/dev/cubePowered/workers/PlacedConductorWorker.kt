package hut.dev.cubePowered.workers

import dev.lone.itemsadder.api.CustomBlock
import dev.lone.itemsadder.api.Events.CustomBlockBreakEvent
import dev.lone.itemsadder.api.Events.CustomBlockPlaceEvent
import hut.dev.cubePowered.library.ConductorDisplayPair
import hut.dev.cubePowered.library.ConductorInstance
import hut.dev.cubePowered.library.Lists
import hut.dev.cubePowered.library.attachFace
import hut.dev.cubePowered.library.detachFace
import org.bukkit.Bukkit
import org.bukkit.block.Block
import org.bukkit.block.TileState
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

object PlacedConductorWorker
{
    fun handleNewPlacedConductor(e: CustomBlockPlaceEvent, plugin: Plugin)
    {
        val conductorInstanceType = Lists.conductorInstances.first { it.conductorKnobModel == e.namespacedID } // using the knob as the base
        val placedConductorInstanceToAdd = ConductorInstance(
            conductorId = conductorInstanceType.conductorId,
            conductorX = e.block.x,
            conductorY = e.block.y,
            conductorZ = e.block.z,
            conductorWorld = e.block.world.name,
            conductorKey = keyOf(e.block.world.name, e.block.x, e.block.y, e.block.z),
            conductorMaxPower = conductorInstanceType.conductorMaxPower,
            conductorCurrentPower = 0,
            conductorExplodePower = conductorInstanceType.conductorExplodePower,
            conductorExplodeOnOverload = conductorInstanceType.conductorExplodeOnOverload,
            conductorVanishOnOverload = conductorInstanceType.conductorVanishOnOverload,
            conductorKnobModel = conductorInstanceType.conductorKnobModel,
            conductorConnectorModel = conductorInstanceType.conductorConnectorModel
        )

        Lists.placedConductorInstances.add(placedConductorInstanceToAdd)
        plugin.logger.info("Placed valid conductor with key: " + placedConductorInstanceToAdd.conductorKey)

        // Build model stacks (ItemsAdder/vanilla)
        val connectorStack = ItemWorker.getItemStackFromString(placedConductorInstanceToAdd.conductorConnectorModel)
        val knobStack = ItemWorker.getItemStackFromString(placedConductorInstanceToAdd.conductorKnobModel)
        if (connectorStack == null || knobStack == null) {
            plugin.logger.warning("Conductor models missing for key: " + placedConductorInstanceToAdd.conductorKey)
            return
        }

        val block = e.block
        var connections = 0

        // connect to neighbors (conductors or machines)
        for (face in facesSix()) {
            val nb = block.getRelative(face)

            val neighbor = findPlacedConductor(nb.world.name, nb.x, nb.y, nb.z)
            val attachToConductor = (neighbor != null)
            val attachToMachine = (!attachToConductor && isAttachableMachine(nb))

            if (attachToConductor || attachToMachine) {
                val (arm, endKnob) = ConductorOrienterWorker.spawnArmAndKnob(
                    block = block,
                    connector = connectorStack,
                    knob = knobStack,
                    face = face,
                    half = 0.5f,
                    eps = 0.001f
                )
                placedConductorInstanceToAdd.attachFace(face, ConductorDisplayPair(arm.uniqueId, endKnob.uniqueId))
                connections++

                if (neighbor != null) {
                    val nbConnector = ItemWorker.getItemStackFromString(neighbor.conductorConnectorModel)
                    val nbKnob = ItemWorker.getItemStackFromString(neighbor.conductorKnobModel)
                    if (nbConnector != null && nbKnob != null) {
                        val opp = opposite(face)
                        if (!neighbor.faceDisplays.containsKey(opp)) {
                            val (nArm, nKnob) = ConductorOrienterWorker.spawnArmAndKnob(
                                block = nb,
                                connector = nbConnector,
                                knob = nbKnob,
                                face = opp,
                                half = 0.5f,
                                eps = 0.001f
                            )
                            neighbor.attachFace(opp, ConductorDisplayPair(nArm.uniqueId, nKnob.uniqueId))
                        }
                    }
                }
            }
        }

        if (connections == 0) {
            plugin.logger.info("Conductor " + placedConductorInstanceToAdd.conductorId + " has no neighbors; stays as center knob only.")
        } else {
            plugin.logger.info("Conductor " + placedConductorInstanceToAdd.conductorId + " connected on " + connections + " face(s).")
        }

        // PowerNode placement upsert
        NodeWorker.upsertPowerNodeOnConductorPlacement(placedConductorInstanceToAdd, plugin)
    }

    fun handleBrokenConductor(e: CustomBlockBreakEvent, plugin: Plugin)
    {
        val block = e.block
        val state = block.state
        val blockKey = keyOf(block.world.name, block.x, block.y, block.z)

        // PDC debug (unchanged)
        if (state is TileState) {
            val pdc = state.persistentDataContainer
            if (pdc.keys.isEmpty()) {
                plugin.logger.info("[PDC] No keys on tile state " + block.type)
            } else {
                pdc.keys.forEach { key ->
                    val s = pdc.get(key, PersistentDataType.STRING)
                    val i = pdc.get(key, PersistentDataType.INTEGER)
                    val l = pdc.get(key, PersistentDataType.LONG)
                    plugin.logger.info("[PDC] " + key + " -> str=" + s + " int=" + i + " long=" + l)
                }
            }
        } else {
            plugin.logger.info("[PDC] " + block.type + " is not a TileState; no PDC available.")
        }

        val placed = Lists.placedConductorInstances.firstOrNull { it.conductorKey == blockKey }
        if (placed == null) {
            plugin.logger.warning("[Conductor] Break @ " + blockKey + " but no placed instance found.")
            return
        }
        Lists.placedConductorInstances.remove(placed)
        plugin.logger.info("Broken valid conductor with key: " + placed.conductorKey)

        // remove all displays owned by this conductor
        var removedSelf = 0
        val ownPairs = placed.faceDisplays.values.toList()
        placed.faceDisplays.clear()
        ownPairs.forEach { pair ->
            Bukkit.getEntity(pair.connectorId)?.remove()
            Bukkit.getEntity(pair.knobId)?.remove()
            removedSelf++
        }

        // remove reciprocal displays from neighbor conductors
        var removedNeighbors = 0
        for (face in facesSix()) {
            val nb = block.getRelative(face)
            val neighbor = findPlacedConductor(nb.world.name, nb.x, nb.y, nb.z) ?: continue
            val neighborFaceTowardUs = opposite(face)

            neighbor.detachFace(neighborFaceTowardUs)?.let { pair ->
                Bukkit.getEntity(pair.connectorId)?.remove()
                Bukkit.getEntity(pair.knobId)?.remove()
                removedNeighbors++
            }
        }

        plugin.logger.info("[Conductor] Cleanup @ " + blockKey + " removedSelf=" + removedSelf + " removedNeighbors=" + removedNeighbors)

        plugin.logger.info(
            "Conductor info: " + block.blockData.toString() + " || " + block.toString() +
                    "||" + block.state + "||" + block.temperature + "||" +
                    CustomBlock.byAlreadyPlaced((block))!!.baseBlockData
        )

        // PowerNode break maintenance
        NodeWorker.updatePowerNodesOnConductorBreak(blockKey, plugin)
    }

    fun facesSix() = listOf(
        org.bukkit.block.BlockFace.NORTH, org.bukkit.block.BlockFace.SOUTH,
        org.bukkit.block.BlockFace.EAST,  org.bukkit.block.BlockFace.WEST,
        org.bukkit.block.BlockFace.UP,    org.bukkit.block.BlockFace.DOWN
    )

    fun opposite(face: org.bukkit.block.BlockFace) = when (face) {
        org.bukkit.block.BlockFace.NORTH -> org.bukkit.block.BlockFace.SOUTH
        org.bukkit.block.BlockFace.SOUTH -> org.bukkit.block.BlockFace.NORTH
        org.bukkit.block.BlockFace.EAST  -> org.bukkit.block.BlockFace.WEST
        org.bukkit.block.BlockFace.WEST  -> org.bukkit.block.BlockFace.EAST
        org.bukkit.block.BlockFace.UP    -> org.bukkit.block.BlockFace.DOWN
        org.bukkit.block.BlockFace.DOWN  -> org.bukkit.block.BlockFace.UP
        else -> org.bukkit.block.BlockFace.SELF
    }

    fun keyOf(world: String, x: Int, y: Int, z: Int) = world + "," + x + "," + y + "," + z

    fun findPlacedConductor(world: String, x: Int, y: Int, z: Int): ConductorInstance?
    {
        val k = keyOf(world, x, y, z)
        return Lists.placedConductorInstances.firstOrNull { it.conductorKey == k }
    }

    fun isAttachableMachine(b: Block): Boolean
    {
        val key = b.world.name + "," + b.x + "," + b.y + "," + b.z
        return Lists.placedMachinesInstances.any { it.machineKey == key }
    }

    fun attachNearbyConductorsToMachine(machineBlock: Block, plugin: Plugin)
    {
        var made = 0

        for (face in facesSix()) {
            val nb = machineBlock.getRelative(face)
            val neighbor = findPlacedConductor(nb.world.name, nb.x, nb.y, nb.z) ?: continue
            val conductorFace = opposite(face)
            if (neighbor.faceDisplays.containsKey(conductorFace)) continue

            val nbConnector = ItemWorker.getItemStackFromString(neighbor.conductorConnectorModel)
            val nbKnob = ItemWorker.getItemStackFromString(neighbor.conductorKnobModel)
            if (nbConnector == null || nbKnob == null) {
                plugin.logger.warning("[Conductor] Missing models for neighbor @ " + neighbor.conductorKey)
                continue
            }

            val (arm, endKnob) = ConductorOrienterWorker.spawnArmAndKnob(
                block = nb,
                connector = nbConnector,
                knob = nbKnob,
                face = conductorFace,
                half = 0.5f,
                eps = 0.001f
            )
            neighbor.attachFace(conductorFace, ConductorDisplayPair(arm.uniqueId, endKnob.uniqueId))
            made++
        }

        if (made > 0) {
            plugin.logger.info(
                "[Conductor] Connected " + made + " conductor face(s) to new machine @ " +
                        keyOf(machineBlock.world.name, machineBlock.x, machineBlock.y, machineBlock.z)
            )
        }
    }

    fun detachNearbyConductorsFromMachine(machineBlock: Block, plugin: Plugin)
    {
        var removed = 0

        for (face in facesSix()) {
            val nb = machineBlock.getRelative(face)
            val neighbor = findPlacedConductor(nb.world.name, nb.x, nb.y, nb.z) ?: continue
            val conductorFace = opposite(face)

            neighbor.detachFace(conductorFace)?.let { pair ->
                Bukkit.getEntity(pair.connectorId)?.remove()
                Bukkit.getEntity(pair.knobId)?.remove()
                removed++
            }
        }

        if (removed > 0) {
            plugin.logger.info(
                "[Conductor] Detached " + removed + " conductor face(s) from broken machine @ " +
                        machineBlock.world.name + "," + machineBlock.x + "," + machineBlock.y + "," + machineBlock.z
            )
        }
    }
}
