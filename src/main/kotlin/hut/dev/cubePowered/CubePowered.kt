package hut.dev.cubePowered

import hut.dev.cubePowered.listeners.InventoryListener
import hut.dev.cubePowered.listeners.ItemsAdderListener
import org.bukkit.plugin.java.JavaPlugin
import hut.dev.cubePowered.workers.ConfigWorker

class CubePowered : JavaPlugin()
{

    override fun onEnable()
    {
        logger.info("enabled")
        server.pluginManager.registerEvents(ItemsAdderListener(this), this)
        logger.info("Reading Machine instances from configs...")
        ConfigWorker.loadData(dataFolder);
        server.pluginManager.registerEvents(InventoryListener(this), this)
    }

    override fun onDisable()
    {
        // Plugin shutdown logic
    }
}