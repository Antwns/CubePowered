package hut.dev.cubePowered.cables

import dev.lone.itemsadder.api.CustomBlock
import org.bukkit.block.BlockFace
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.MultipleFacing
import org.bukkit.plugin.Plugin
import java.util.EnumSet

object CableFaces {

    // 1) Read the current faces from BlockData (returns null if not a MultipleFacing block)
    fun readFaces(block: Block): EnumSet<BlockFace>? {
        val mf = block.blockData as? MultipleFacing ?: return null
        val faces = EnumSet.noneOf(BlockFace::class.java)
        for (f in SIX_FACES) if (mf.hasFace(f)) faces.add(f)
        return faces
    }

    // 2) Apply faces back to the block (updates the model immediately)
    fun applyFaces(block: Block, faces: Set<BlockFace>) {
        val mf = block.blockData as? MultipleFacing ?: return
        for (f in SIX_FACES) mf.setFace(f, faces.contains(f))
        block.blockData = mf
        // no need for TileState.update(); mushroom block isn’t a TileState
    }

    // 3) Decide which neighboring blocks we connect to
    fun computeNeighborConnections(block: Block, isConnector: (Block) -> Boolean): EnumSet<BlockFace> {
        val out = EnumSet.noneOf(BlockFace::class.java)
        for (f in SIX_FACES) {
            val nb = block.getRelative(f)
            if (isConnector(nb)) out.add(f)
        }
        return out
    }

    // Helper: treat IA cables and (optionally) your powered machines as connectors
    fun defaultIsConnector(plugin: Plugin): (Block) -> Boolean = { b ->
        // IA cable?
        val ia = try { CustomBlock.byAlreadyPlaced(b.location.block) } catch (_: Throwable) { null }
        if (ia != null && ia.namespacedID.endsWith("_cable")) true
        // (Optional) also connect to your machine blocks:
        // else Lists.placedMachines.any { it.machineWorld == b.world.name && it.machineX == b.x && it.machineY == b.y && it.machineZ == b.z }
        else false
    }

    // 4) One-call update for a cable block: recompute faces from neighbors and apply
    fun refreshCable(block: Block, plugin: Plugin) {
        if (block.type != Material.BROWN_MUSHROOM_BLOCK) return  // your cable base
        val faces = computeNeighborConnections(block, defaultIsConnector(plugin))
        applyFaces(block, faces)
    }

    // 5) Update this cable and tell adjacent cables to update too (call on place/break)
    fun refreshCableAndNeighbors(block: Block, plugin: Plugin) {
        refreshCable(block, plugin)
        for (f in SIX_FACES) refreshCable(block.getRelative(f), plugin)
    }

    // Optional: turn faces into a compact key or mask (useful for debugging/mapping)
    fun facesMask(block: Block): Int {
        val mf = block.blockData as? MultipleFacing ?: return 0
        var mask = 0
        if (mf.hasFace(BlockFace.NORTH)) mask = mask or 1
        if (mf.hasFace(BlockFace.EAST )) mask = mask or 2
        if (mf.hasFace(BlockFace.SOUTH)) mask = mask or 4
        if (mf.hasFace(BlockFace.WEST )) mask = mask or 8
        if (mf.hasFace(BlockFace.UP   )) mask = mask or 16
        if (mf.hasFace(BlockFace.DOWN )) mask = mask or 32
        return mask
    }

    private val SIX_FACES = arrayOf(
        BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
    )
}