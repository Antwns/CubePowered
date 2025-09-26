package hut.dev.cubePowered.library

import net.kyori.option.OptionSchema

data class PowerNode (
    val powerNodeKey: String?,
    val powerNodeIncome: Int,
    val powerNodeExpenditure: Int,
    var powerNodeStoredPower: Int = 0,
    val powerNodePlacedConductors: MutableMap<String, ConductorInstance> = mutableMapOf(),
    val powerNodePlacedMachines: MutableMap<String, MachineInstance> = mutableMapOf(),
    val powerNodePlacedGenerators: MutableMap<String, MachineInstance> = mutableMapOf()
    )