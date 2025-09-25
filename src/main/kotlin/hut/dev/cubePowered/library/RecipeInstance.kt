package hut.dev.cubePowered.library

import org.bukkit.inventory.ItemStack

data class RecipeInstance (
    val assignedMachineInstance: MachineInstance,
    val recipeId : String,
    val recipeInputItems: MutableMap<Int ,ItemStack>,
    val recipeOutputItems: MutableMap<Int ,ItemStack>,
    val recipeProcessingTime: Int,
    val recipeEnergyConsumption: Int
)