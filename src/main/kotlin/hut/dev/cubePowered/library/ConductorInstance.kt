package hut.dev.cubePowered.library

data class ConductorInstance(
    val conductorId : String,
    val conductorModel: String,
    val conductorKey: String?,
    val conductorWorld: String?,
    val conductorX: Int?,
    val conductorY: Int?,
    val conductorZ: Int?,
    val conductorMaxPower: Int,
    var conductorCurrentPower: Int = 0,
    val conductorExplodeOnOverload : Boolean,
    val conductorExplodePower: Int,
    val conductorVanishOnOverload: Boolean
)