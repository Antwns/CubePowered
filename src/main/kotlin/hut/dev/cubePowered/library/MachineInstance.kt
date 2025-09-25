package hut.dev.cubePowered.library

import  java.util.UUID
import org.bukkit.inventory.ItemStack

data class MachineInstance
    (
    val machineId: String,
    val model: String,
    val machineTitle: String,
    val inventorySize: Int,
    val lockedSlotsMaterial: String,
    val energyEnabled: Boolean,
    val energyPerTick: Int,
    val energyBuffer: Int,
    var machinePowerMode: Boolean,
    val processingInterval: Int,
    val processingToggleable: Boolean,
    val behaviorOnFull: String,
    val behaviourPauseIfNoEnergy: Boolean,
    val guiRows: Int?,
    val guiToggleItemSlot: Int,
    val guiOutputSlots: MutableList<Int>,
    val guiInputSlots: MutableList<Int>,
    val guiLockedSlots: List<Int>,
    val guiOnItemModel: String,
    val guiOffItemModel: String,
    val guiOnLabel: String,
    val guiOffLabel: String,
    val machineContents: MutableMap<Int,ItemStack> = mutableMapOf(),
    var progressTicks: Int = 0,
    var tickAccumulator: Int = 0,
    var machineX: Int = 0,
    var machineY: Int = 0,
    var machineZ: Int = 0,
    var machineWorld: String = "",
    var machineKey: String = machineWorld + "," + machineX + "," +  machineY + "," + machineZ,
    var inUse: Boolean,
    var currentPlayerUUID: UUID?
)


