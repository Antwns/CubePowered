package hut.dev.cubePowered.library

import org.bukkit.block.BlockFace

data class ConductorInstance(
    val conductorId : String,
    val conductorKnobModel: String,
    val conductorConnectorModel: String,
    val conductorKey: String?,
    val conductorWorld: String?,
    val conductorX: Int?,
    val conductorY: Int?,
    val conductorZ: Int?,
    val conductorMaxPower: Int,
    var conductorCurrentPower: Int = 0,
    val conductorExplodeOnOverload : Boolean,
    val conductorExplodePower: Int,
    val conductorVanishOnOverload: Boolean,

    val faceDisplays: MutableMap<BlockFace, ConductorDisplayPair> = mutableMapOf()
)

fun ConductorInstance.attachFace(face: BlockFace, pair: ConductorDisplayPair)
{
    faceDisplays[face] = pair
}

fun ConductorInstance.detachFace(face: BlockFace): ConductorDisplayPair?
{
    return faceDisplays.remove(face)
}

fun ConductorInstance.clearFaces(): Collection<ConductorDisplayPair>
{
    val all = faceDisplays.values.toList()
    faceDisplays.clear()
    return all
}