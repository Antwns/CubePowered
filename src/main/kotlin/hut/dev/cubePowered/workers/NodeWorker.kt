package hut.dev.cubePowered.workers

import hut.dev.cubePowered.library.*
import org.bukkit.block.BlockFace
import org.bukkit.plugin.Plugin
import kotlin.random.Random

internal object NodeWorker
{
    // ---------- Basics ----------
    fun nodeSize(n: PowerNodeInstance): Int
    {
        return n.powerNodePlacedConductors.size +
                n.powerNodePlacedMachines.size +
                n.powerNodePlacedGenerators.size
    }

    private fun facesSix() = listOf(
        BlockFace.NORTH, BlockFace.SOUTH,
        BlockFace.EAST,  BlockFace.WEST,
        BlockFace.UP,    BlockFace.DOWN
    )

    private fun keyOf(world: String, x: Int, y: Int, z: Int) = world + "," + x + "," + y + "," + z

    private fun neighborKeysOf(key: String): List<String>
    {
        // key = "world,x,y,z"
        val p = key.split(',')
        if (p.size != 4) return emptyList()
        val w = p[0]
        val x = p[1].toIntOrNull() ?: return emptyList()
        val y = p[2].toIntOrNull() ?: return emptyList()
        val z = p[3].toIntOrNull() ?: return emptyList()

        return listOf(
            keyOf(w, x + 1, y, z),
            keyOf(w, x - 1, y, z),
            keyOf(w, x, y + 1, z),
            keyOf(w, x, y - 1, z),
            keyOf(w, x, y, z + 1),
            keyOf(w, x, y, z - 1)
        )
    }

    // ---------- List merge helpers (dedupe by key) ----------
    private inline fun <T> mergeByKey(
        dst: MutableList<T>,
        src: MutableList<T>,
        keyOf: (T) -> String?
    )
    {
        val seen = dst.mapNotNull(keyOf).toHashSet()
        for (item in src)
        {
            val k = keyOf(item)
            if (k == null)
            {
                if (!dst.contains(item)) dst.add(item)
            }
            else if (!seen.contains(k))
            {
                dst.add(item)
                seen.add(k)
            }
        }
    }

    fun mergeNodeIntoWinner(winner: PowerNodeInstance, loser: PowerNodeInstance, plugin: Plugin)
    {
        mergeByKey(winner.powerNodePlacedConductors, loser.powerNodePlacedConductors) { it.conductorKey }
        mergeByKey(winner.powerNodePlacedMachines,   loser.powerNodePlacedMachines)   { it.machineKey }
        mergeByKey(winner.powerNodePlacedGenerators, loser.powerNodePlacedGenerators) { it.machineKey }

        Lists.placedPowerNodes.remove(loser)

        plugin.logger.info(
            "[PowerNode] Merged node " + (loser.powerNodeKey ?: "unknown") +
                    " into " + (winner.powerNodeKey ?: "unknown") +
                    " (winnerSize=" + nodeSize(winner) + ")"
        )
    }

    // ---------- Graph/splitting ----------
    private fun buildAdjacency(allKeys: Set<String>): Map<String, MutableSet<String>>
    {
        val adj = HashMap<String, MutableSet<String>>(allKeys.size)
        allKeys.forEach { adj[it] = HashSet() }
        for (k in allKeys)
        {
            for (n in neighborKeysOf(k))
            {
                if (n in allKeys)
                {
                    adj[k]!!.add(n)
                    adj[n]!!.add(k)
                }
            }
        }
        return adj
    }

    private fun connectedComponents(allKeys: Set<String>, adj: Map<String, MutableSet<String>>): List<Set<String>>
    {
        val remaining = allKeys.toMutableSet()
        val comps = ArrayList<Set<String>>()
        val q: ArrayDeque<String> = ArrayDeque()

        while (remaining.isNotEmpty())
        {
            val start = remaining.first()
            val comp = HashSet<String>()
            q.add(start)
            remaining.remove(start)

            while (q.isNotEmpty())
            {
                val v = q.removeFirst()
                comp.add(v)
                for (n in adj[v].orEmpty())
                {
                    if (n in remaining)
                    {
                        remaining.remove(n)
                        q.add(n)
                    }
                }
            }
            comps.add(comp)
        }
        return comps
    }

    private inline fun <T> moveByKeys(
        from: MutableList<T>,
        to: MutableList<T>,
        keyOf: (T) -> String?,
        keys: Set<String>
    )
    {
        val it = from.listIterator()
        while (it.hasNext())
        {
            val item = it.next()
            val k = keyOf(item)
            if (k != null && keys.contains(k))
            {
                it.remove()
                to.add(item)
            }
        }
    }

    private fun spawnNodeFromComponent(source: PowerNodeInstance, component: Set<String>): PowerNodeInstance
    {
        val newKey = "node:" + (component.firstOrNull() ?: java.util.UUID.randomUUID().toString())
        val node = PowerNodeInstance(
            powerNodeKey = newKey,
            powerNodeIncome = 0,
            powerNodeExpenditure = 0,
            powerNodePlacedConductors = mutableListOf(),
            powerNodePlacedMachines = mutableListOf(),
            powerNodePlacedGenerators = mutableListOf()
        )
        moveByKeys(source.powerNodePlacedConductors, node.powerNodePlacedConductors, { it.conductorKey }, component)
        moveByKeys(source.powerNodePlacedMachines,   node.powerNodePlacedMachines,   { it.machineKey },   component)
        moveByKeys(source.powerNodePlacedGenerators, node.powerNodePlacedGenerators, { it.machineKey },   component)
        return node
    }

    private fun splitPowerNodeIfDisconnected(node: PowerNodeInstance, plugin: Plugin)
    {
        val allKeys = buildSet {
            addAll(node.powerNodePlacedConductors.mapNotNull { it.conductorKey })
            addAll(node.powerNodePlacedMachines.mapNotNull   { it.machineKey })
            addAll(node.powerNodePlacedGenerators.mapNotNull { it.machineKey })
        }
        if (allKeys.isEmpty())
        {
            Lists.placedPowerNodes.remove(node)
            plugin.logger.info("[PowerNode] Removed empty node " + (node.powerNodeKey ?: "unknown"))
            return
        }

        val adj = buildAdjacency(allKeys)
        val comps = connectedComponents(allKeys, adj)
        if (comps.size <= 1) return

        // winner = largest; tie -> random among tied
        val maxSize = comps.maxOf { it.size }
        val winners = comps.filter { it.size == maxSize }
        val winnerComp = if (winners.size == 1) winners.first() else winners[Random.nextInt(winners.size)]

        for (comp in comps)
        {
            if (comp === winnerComp) continue
            val newNode = spawnNodeFromComponent(node, comp)
            Lists.placedPowerNodes.add(newNode)
            plugin.logger.info(
                "[PowerNode] Split: spawned node " + (newNode.powerNodeKey ?: "unknown") +
                        " (size=" + nodeSize(newNode) + ") from " + (node.powerNodeKey ?: "unknown")
            )
        }

        plugin.logger.info(
            "[PowerNode] Split result for " + (node.powerNodeKey ?: "unknown") +
                    " winnerSize=" + nodeSize(node) + " parts=" + comps.size
        )
    }

    // ---------- Conductor placement/break ----------
    fun upsertPowerNodeOnConductorPlacement(conductor: ConductorInstance, plugin: Plugin)
    {
        val ck = conductor.conductorKey ?: return
        val neighbors = neighborKeysOf(ck).toSet()

        val matches = Lists.placedPowerNodes.filter { node ->
            node.powerNodePlacedConductors.any { it.conductorKey != null && neighbors.contains(it.conductorKey) } ||
                    node.powerNodePlacedMachines.any   { it.machineKey    != null && neighbors.contains(it.machineKey) }   ||
                    node.powerNodePlacedGenerators.any { it.machineKey    != null && neighbors.contains(it.machineKey) }
        }.toMutableList()

        when
        {
            matches.isEmpty() -> {
                val node = PowerNodeInstance(
                    powerNodeKey = "node:" + ck,
                    powerNodeIncome = 0,
                    powerNodeExpenditure = 0,
                    powerNodePlacedConductors = mutableListOf(conductor),
                    powerNodePlacedMachines = mutableListOf(),
                    powerNodePlacedGenerators = mutableListOf()
                )
                Lists.placedPowerNodes.add(node)
                plugin.logger.info("[PowerNode] Created node " + (node.powerNodeKey ?: "unknown") + " with seed conductor " + ck)
            }

            matches.size == 1 -> {
                val winner = matches.first()
                if (winner.powerNodePlacedConductors.none { it.conductorKey == ck })
                    winner.powerNodePlacedConductors.add(conductor)
                plugin.logger.info("[PowerNode] Added conductor " + ck + " to node " + (winner.powerNodeKey ?: "unknown"))
            }

            else -> {
                val sizes = matches.associateWith { nodeSize(it) }
                val maxSize = sizes.values.maxOrNull() ?: 0
                val tied = matches.filter { sizes[it] == maxSize }
                val winner = if (tied.size == 1) tied.first() else tied[Random.nextInt(tied.size)]
                for (n in matches) if (n !== winner) mergeNodeIntoWinner(winner, n, plugin)

                if (winner.powerNodePlacedConductors.none { it.conductorKey == ck })
                    winner.powerNodePlacedConductors.add(conductor)
                plugin.logger.info("[PowerNode] After merge, added conductor " + ck + " to node " + (winner.powerNodeKey ?: "unknown"))
            }
        }
    }

    fun updatePowerNodesOnConductorBreak(brokenConductorKey: String, plugin: Plugin)
    {
        val affected = Lists.placedPowerNodes.filter { node ->
            node.powerNodePlacedConductors.any { it.conductorKey == brokenConductorKey }
        }.toList()
        if (affected.isEmpty()) return

        for (node in affected)
        {
            node.powerNodePlacedConductors.removeIf { it.conductorKey == brokenConductorKey }

            if (nodeSize(node) == 0)
            {
                Lists.placedPowerNodes.remove(node)
                plugin.logger.info("[PowerNode] Removed empty node " + (node.powerNodeKey ?: "unknown") + " after conductor break")
            }
            else
            {
                splitPowerNodeIfDisconnected(node, plugin)
            }
        }
    }

    // ---------- Machine placement/break ----------
    fun upsertPowerNodeOnMachinePlacement(machine: MachineInstance, plugin: Plugin)
    {
        val mKey = machine.machineKey ?: return
        val neighbors = neighborKeysOf(mKey).toSet()

        val matches = Lists.placedPowerNodes.filter { node ->
            node.powerNodePlacedConductors.any { it.conductorKey != null && neighbors.contains(it.conductorKey) } ||
                    node.powerNodePlacedMachines.any   { it.machineKey    != null && neighbors.contains(it.machineKey) }   ||
                    node.powerNodePlacedGenerators.any { it.machineKey    != null && neighbors.contains(it.machineKey) }
        }.toMutableList()

        when
        {
            matches.isEmpty() -> {
                val node = PowerNodeInstance(
                    powerNodeKey = "node:" + mKey,
                    powerNodeIncome = 0,
                    powerNodeExpenditure = 0,
                    powerNodePlacedConductors = mutableListOf(),
                    powerNodePlacedMachines = mutableListOf(machine),
                    powerNodePlacedGenerators = mutableListOf()
                )
                Lists.placedPowerNodes.add(node)
                plugin.logger.info("[PowerNode] Created node " + (node.powerNodeKey ?: "unknown") + " with seed machine " + mKey)
            }

            matches.size == 1 -> {
                val winner = matches.first()
                if (winner.powerNodePlacedMachines.none { it.machineKey == mKey })
                    winner.powerNodePlacedMachines.add(machine)
                plugin.logger.info("[PowerNode] Added machine " + mKey + " to node " + (winner.powerNodeKey ?: "unknown"))
            }

            else -> {
                val sizes = matches.associateWith { nodeSize(it) }
                val maxSize = sizes.values.maxOrNull() ?: 0
                val tied = matches.filter { sizes[it] == maxSize }
                val winner = if (tied.size == 1) tied.first() else tied[Random.nextInt(tied.size)]
                for (n in matches) if (n !== winner) mergeNodeIntoWinner(winner, n, plugin)

                if (winner.powerNodePlacedMachines.none { it.machineKey == mKey })
                    winner.powerNodePlacedMachines.add(machine)
                plugin.logger.info("[PowerNode] After merge, added machine " + mKey + " to node " + (winner.powerNodeKey ?: "unknown"))
            }
        }
    }

    fun updatePowerNodesOnMachineBreak(machineKey: String, plugin: Plugin)
    {
        val affected = Lists.placedPowerNodes.filter { node ->
            node.powerNodePlacedMachines.any   { it.machineKey == machineKey } ||
                    node.powerNodePlacedGenerators.any { it.machineKey == machineKey }
        }.toList()
        if (affected.isEmpty()) return

        for (node in affected)
        {
            var changed = false
            changed = node.powerNodePlacedMachines.removeIf   { it.machineKey == machineKey } || changed
            changed = node.powerNodePlacedGenerators.removeIf { it.machineKey == machineKey } || changed
            if (!changed) continue

            if (nodeSize(node) == 0)
            {
                Lists.placedPowerNodes.remove(node)
                plugin.logger.info("[PowerNode] Removed empty node " + (node.powerNodeKey ?: "unknown") + " after machine break")
            }
            else
            {
                splitPowerNodeIfDisconnected(node, plugin)
            }
        }
    }
}