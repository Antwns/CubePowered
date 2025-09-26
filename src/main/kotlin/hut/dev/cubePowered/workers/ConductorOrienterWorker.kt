package hut.dev.cubePowered.workers

import hut.dev.cubePowered.library.ConductorDisplayPair
import hut.dev.cubePowered.library.ConductorInstance
import hut.dev.cubePowered.library.Lists
import hut.dev.cubePowered.library.PowerNode
import hut.dev.cubePowered.library.attachFace
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.ItemDisplay
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Transformation
import org.bukkit.Location
import org.bukkit.plugin.Plugin
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.UUID

object ConductorOrienterWorker
{
    private fun Block.center(): Location = Location(world, x + 0.5, y + 0.5, z + 0.5)

    private fun rotX(rad: Float) = Quaternionf().rotateX(rad)
    private fun rotY(rad: Float) = Quaternionf().rotateY(rad)
    private fun rotZ(rad: Float) = Quaternionf().rotateZ(rad)

    private data class Pose(val tx: Float, val ty: Float, val tz: Float, val rot: Quaternionf)

    // block = 16 units. knob = 4 (half = 2/16). connector length = 6 (half = 3/16).
    // We want the connector to run from 2/16 → 8/16 along the axis.
    private const val UNIT       = 1f / 16f
    private const val HALF       = 0.5f
    private const val KNOB_HALF  = 2 * UNIT      // 0.125
    private const val CONN_HALF  = 3 * UNIT      // 0.1875
    private const val ARM_OFFSET = KNOB_HALF + CONN_HALF // 5/16 = 0.3125

    /**
     * Assumes the connector model is authored along +Z (south) axis (north–south).
     * Rotates to the requested face and translates so the inner end touches the 4x4x4 knob.
     */
    fun spawnArmAndKnob(
        block: Block,
        connector: ItemStack,
        knob: ItemStack,
        face: BlockFace,
        half: Float = HALF,
        eps: Float = 0.001f
    ): Pair<ItemDisplay, ItemDisplay>
    {
        // Rotate +Z to the target face, and translate by ARM_OFFSET toward that face.
        val armPose = when (face)
        {
            BlockFace.SOUTH -> Pose(0f, 0f, +ARM_OFFSET, rotY(0f))                    // +Z stays +Z
            BlockFace.NORTH -> Pose(0f, 0f, -ARM_OFFSET, rotY(Math.PI.toFloat()))     // +Z -> -Z
            BlockFace.EAST  -> Pose(+ARM_OFFSET, 0f, 0f,  rotY(-Math.PI.toFloat() / 2f)) // +Z -> +X
            BlockFace.WEST  -> Pose(-ARM_OFFSET, 0f, 0f,  rotY( Math.PI.toFloat() / 2f)) // +Z -> -X
            BlockFace.UP    -> Pose(0f, +ARM_OFFSET, 0f,  rotX(-Math.PI.toFloat() / 2f)) // +Z -> +Y
            BlockFace.DOWN  -> Pose(0f, -ARM_OFFSET, 0f,  rotX( Math.PI.toFloat() / 2f)) // +Z -> -Y
            else            -> Pose(0f, 0f, +ARM_OFFSET, rotY(0f))
        }

        // End knob sits flush on the face (±half) with a tiny inward bias to avoid z-fighting.
        val knobPose = when (face)
        {
            BlockFace.SOUTH -> Pose(0f, 0f,  half - eps, rotY(0f))
            BlockFace.NORTH -> Pose(0f, 0f, -half + eps, rotY(0f))
            BlockFace.EAST  -> Pose( half - eps, 0f, 0f,  rotY(0f))
            BlockFace.WEST  -> Pose(-half + eps, 0f, 0f,  rotY(0f))
            BlockFace.UP    -> Pose(0f,  half - eps, 0f,  rotY(0f))
            BlockFace.DOWN  -> Pose(0f, -half + eps, 0f,  rotY(0f))
            else            -> Pose(0f, 0f,  half - eps, rotY(0f))
        }

        val origin = block.center()

        val arm = block.world.spawn(origin, ItemDisplay::class.java)
        { d ->
            d.setItemStack(connector)
            d.setPersistent(true)
            d.setTransformation(
                Transformation(
                    Vector3f(armPose.tx, armPose.ty, armPose.tz),
                    Quaternionf().identity(),
                    Vector3f(1f, 1f, 1f),       // keep model’s native 2x2x6 size
                    armPose.rot
                )
            )
        }

        val knobDisp = block.world.spawn(origin, ItemDisplay::class.java)
        { d ->
            d.setItemStack(knob)
            d.setPersistent(true)
            d.setTransformation(
                Transformation(
                    Vector3f(knobPose.tx, knobPose.ty, knobPose.tz),
                    Quaternionf().identity(),
                    Vector3f(1f, 1f, 1f),
                    knobPose.rot
                )
            )
        }

        return arm to knobDisp
    }

    fun attachNearbyConductorsToMachine(machineBlock: Block, plugin: Plugin)
    {
        var made = 0

        for (face in facesSix())
        {
            val nb = machineBlock.getRelative(face)
            val neighbor = findPlacedConductor(nb.world.name, nb.x, nb.y, nb.z) ?: continue

            // Conductor should face back toward the machine
            val conductorFace = opposite(face)

            // Skip if already connected on that face
            if (neighbor.faceDisplays.containsKey(conductorFace)) continue

            // Use the neighbor CONDUCTOR's own models
            val nbConnector = ItemWorker.getItemStackFromString(neighbor.conductorConnectorModel)
            val nbKnob = ItemWorker.getItemStackFromString(neighbor.conductorKnobModel)
            if (nbConnector == null || nbKnob == null)
            {
                plugin.logger.warning("[Conductor] Missing models for neighbor @ " + neighbor.conductorKey)
                continue
            }

            // Spawn on the CONDUCTOR block, facing toward the machine
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

        if (made > 0)
        {
            plugin.logger.info("[Conductor] Connected " + made + " conductor face(s) to new machine @ " + keyOf(machineBlock.world.name, machineBlock.x, machineBlock.y, machineBlock.z))
        }
    }

    private fun facesSix(): List<BlockFace>
    {
        return listOf(
            BlockFace.NORTH, BlockFace.SOUTH,
            BlockFace.EAST,  BlockFace.WEST,
            BlockFace.UP,    BlockFace.DOWN
        )
    }

    fun opposite(face: BlockFace): BlockFace
    {
        return when (face)
        {
            BlockFace.NORTH -> BlockFace.SOUTH
            BlockFace.SOUTH -> BlockFace.NORTH
            BlockFace.EAST  -> BlockFace.WEST
            BlockFace.WEST  -> BlockFace.EAST
            BlockFace.UP    -> BlockFace.DOWN
            BlockFace.DOWN  -> BlockFace.UP
            else -> BlockFace.SELF
        }
    }

    private fun keyOf(world: String, x: Int, y: Int, z: Int): String
    {
        return world + "," + x + "," + y + "," + z
    }

    private fun findPlacedConductor(world: String, x: Int, y: Int, z: Int): ConductorInstance?
    {
        val k = keyOf(world, x, y, z)
        return Lists.placedConductorInstances.firstOrNull { it.conductorKey == k }
    }

    private fun adjacentKeys(world: String?, x: Int?, y: Int?, z: Int?): Set<String>
    {
        if (world == null || x == null || y == null || z == null) return emptySet()
        val out = HashSet<String>(6)
        for (f in facesSix())
        {
            out.add(keyOf(world, x + f.modX, y + f.modY, z + f.modZ))
        }
        return out
    }

    /** Find the first PowerNode that already has a conductor adjacent to 'c'. */
    fun findAdjacentPowerNodeForConductor(c: ConductorInstance): PowerNode?
    {
        val adj = adjacentKeys(c.conductorWorld, c.conductorX, c.conductorY, c.conductorZ)
        if (adj.isEmpty()) return null

        return Lists.placedPowerNodes.firstOrNull { node ->
            node.powerNodePlacedConductors.values.any { pc ->
                val k = pc.conductorKey
                k != null && adj.contains(k)
            }
        }
    }

    /** Create a new node seeded with this conductor (LIST variant). */
    fun createPowerNodeFor(conductor: ConductorInstance): PowerNode
    {
        val nodeKey = "node:" + (conductor.conductorKey ?: UUID.randomUUID().toString())
        val node = PowerNode(
            powerNodeKey = nodeKey,
            powerNodeIncome = 0,
            powerNodeExpenditure = 0,
            powerNodeStoredPower = 0
        )
        val ck = conductor.conductorKey
        if (ck != null)
        {
            node.powerNodePlacedConductors[ck] = conductor
        }
        return node
    }

    /** Attach a conductor to an existing node. */
    fun attachConductorToNode(node: PowerNode, conductor: ConductorInstance, plugin: Plugin)
    {
        val ck = conductor.conductorKey ?: return
        node.powerNodePlacedConductors[ck] = conductor
        plugin.logger.info("[PowerNode] Added conductor " + ck + " to node " + node.powerNodeKey)
    }

    /** To be called right after we add the placed ConductorInstance to Lists.placedConductorInstances. */
    fun upsertPowerNodeOnConductorPlacement(conductor: ConductorInstance, plugin: Plugin)
    {
        val matches = Lists.placedPowerNodes.filter { node ->
            node.powerNodePlacedConductors.values.any { pc ->
                val k = pc.conductorKey
                val adj = adjacentKeys(conductor.conductorWorld, conductor.conductorX, conductor.conductorY, conductor.conductorZ)
                k != null && adj.contains(k)
            }
        }

        if (matches.isEmpty())
        {
            val newNode = createPowerNodeFor(conductor)
            Lists.placedPowerNodes.add(newNode)
            plugin.logger.info("[PowerNode] Created node " + newNode.powerNodeKey + " with seed conductor " + (conductor.conductorKey ?: "unknown"))
        }
        else
        {
            // attach to the first matching node (simple, functional baseline)
            attachConductorToNode(matches.first(), conductor, plugin)

            // (Optional) If matches.size > 1, you might want to merge nodes later.
        }
    }
}