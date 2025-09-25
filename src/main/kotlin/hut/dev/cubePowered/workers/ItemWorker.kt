package hut.dev.cubePowered.workers

object ItemWorker
{
    fun getItemStackFromString(id: String): org.bukkit.inventory.ItemStack =
        if (id.startsWith("minecraft:"))
        {
            val materialToCheck = org.bukkit.Material.matchMaterial(id) ?: org.bukkit.Material.BLACK_STAINED_GLASS_PANE
            org.bukkit.inventory.ItemStack(materialToCheck)
        }
        else
        {
            // ItemsAdder custom item
            dev.lone.itemsadder.api.CustomStack.getInstance(id)?.itemStack ?: org.bukkit.inventory.ItemStack(org.bukkit.Material.BLACK_STAINED_GLASS_PANE)
        }
}