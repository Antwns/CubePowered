package hut.dev.cubePowered.library

data class PowerNodeInstance (
    val powerNodeKey: String?,
    val powerNodeIncome: Int,
    val powerNodeExpenditure: Int,
    val powerNodePlacedConductors: MutableList<ConductorInstance> = mutableListOf(),
    val powerNodePlacedMachines: MutableList<MachineInstance> = mutableListOf(),
    val powerNodePlacedGenerators: MutableList<MachineInstance> = mutableListOf()
    )