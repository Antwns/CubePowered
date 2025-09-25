package hut.dev.cubePowered.workers

import hut.dev.cubePowered.library.MachineInstance
import hut.dev.cubePowered.library.Lists
import hut.dev.cubePowered.library.RecipeInstance
import hut.dev.cubePowered.workers.IntegrityWorker.verifySlotIntegrity
import net.kyori.adventure.text.logger.slf4j.ComponentLogger.logger
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import java.io.File

object ConfigWorker {
    internal var loadedMachinesTotal = 0
    internal var loadedMachineGuisTotal = 0
    internal var loadedRecipesTotal = 0
    /** Load/refresh all machine instances from plugins/CubePowered/machines/.yml */
    fun loadData(dataFolder: File)
    {
        FileWorker().ensureExampleMachineFile(dataFolder)

        val machinesDir = File(dataFolder, "machines")
        val recipesDir = File(dataFolder, "recipes")
        if (!machinesDir.exists()) machinesDir.mkdirs()
        if (!recipesDir.exists()) recipesDir.mkdirs()

        Lists.machineInstances.clear()
        loadedMachineGuisTotal = 0
        loadedMachinesTotal = 0
        loadedRecipesTotal = 0

        val machineFiles = machinesDir.listFiles { f ->
            f.isFile && f.name.endsWith(".yml", ignoreCase = true)
        } ?: emptyArray()

        val recipeFiles = recipesDir.listFiles { f ->
            f.isFile && f.name.endsWith(".yml", ignoreCase = true)
        } ?: emptyArray()

        logger().info("Found " + machineFiles.size + " machine file(s) in " + machinesDir.absolutePath)

        machineFiles.forEach { machineDefinitionFile ->
            loadOneMachineFile(machineDefinitionFile)
        }

        logger().info("Loaded " + loadedMachinesTotal + " machine definition(s).")
        logger().info("Loaded " + loadedMachineGuisTotal + " gui definition(s).")

        logger().info("Loading recipes...")

        recipeFiles.forEach { recipeDefinitionFile ->
            loadOneRecipeFile(recipeDefinitionFile)
        }

        logger().info("Loaded " + loadedRecipesTotal + " recipe defintions" )
    }

    /** Load one file with top-level machine ids. */
    private fun loadOneMachineFile(file: File)
    {
        logger().info("Loading file " + file.name + "...")
        val cfg = YamlConfiguration()
        try {
            cfg.load(file) // throws on invalid YAML
        } catch (e: Exception) {
            logger().error("Failed to parse YAML '" + file.name + "': " + e.message)
            e.printStackTrace()
            return
        }
        val ids = cfg.getKeys(false)
        if (ids.isEmpty()) return

        for (id in ids) {
            val sec = cfg.getConfigurationSection(id) ?: continue

            val model = sec.getString("model") ?: ""

            val invSec = sec.getConfigurationSection("inventory")
            val inventoryTitle = invSec?.getString("title") ?: id
            val inventorySize = invSec?.getInt("size", 27) ?: 27
            val inventoryRows = invSec?.getInt("rows", 1)?: 1

            val inputSlots = sec.getIntegerList("slots.input")
            val outputSlots = sec.getIntegerList("slots.output")
            val fuelSlots = sec.getIntegerList("slots.fuel")
            val lockedSlotsMaterial = sec.getString("slots.locked_slots_material") ?: "minecraft:black_stained_glass_pane"

            val energySec = sec.getConfigurationSection("energy")
            val energyEnabled = energySec?.getBoolean("enabled", false) ?: false
            val energyPerTick = energySec?.getInt("per_tick", 0) ?: 0
            val energyBuffer = energySec?.getInt("buffer", 0) ?: 0

            val procSec = sec.getConfigurationSection("processing")
            val autoProcessingMode = procSec?.getBoolean("auto", false) ?: false
            val processingInterval = procSec?.getInt("interval_ticks", 200) ?: 200
            val processingToggleable = procSec?.getBoolean("toggleable", false) ?: false

            val behSec = sec.getConfigurationSection("behavior")
            val behaviorOnFull = behSec?.getString("on_output_full") ?: "block"
            val behaviourPauseIfNoEnergy = behSec?.getBoolean("pause_if_no_energy", true) ?: true

            val guiSec = sec.getConfigurationSection("gui")
            val guiToggleItemSlot = guiSec?.getInt("slot", 4) ?: 4
            val guiOnItemModel = guiSec?.getString("on_item") ?: "minecraft:lime_dye"
            val guiOffItemModel = guiSec?.getString("off_item") ?: "minecraft:red_dye"
            val guiOnLabel = guiSec?.getString("on_label") ?: "Auto: ON"
            val guiOffLabel = guiSec?.getString("off_label") ?: "Auto: OFF"

            if (!verifySlotIntegrity(id, inventorySize, inputSlots, outputSlots, fuelSlots)) {
                logger().error("Skipping machine with id " + id + " due to invalid slot configuration.")
                continue
            }

            val machineInstanceToAdd = MachineInstance(
                machineId = id,
                machineTitle = inventoryTitle,
                model = model,
                inventorySize = inventorySize,
                guiInputSlots = inputSlots,
                guiLockedSlots = IntegrityWorker.getLockedSlotsFromInfo(inventorySize, inputSlots, outputSlots),
                guiOutputSlots = outputSlots,
                fuelTypes = fuelSlots,
                lockedSlotsMaterial = lockedSlotsMaterial,
                energyEnabled = energyEnabled,
                energyPerTick = energyPerTick,
                energyBuffer = energyBuffer,
                machinePowerMode = autoProcessingMode,
                processingInterval = processingInterval,
                processingToggleable = processingToggleable,
                behaviorOnFull = behaviorOnFull,
                behaviourPauseIfNoEnergy = behaviourPauseIfNoEnergy,
                guiRows = inventoryRows,
                guiToggleItemSlot = guiToggleItemSlot,
                guiOnItemModel = guiOnItemModel,
                guiOffItemModel = guiOffItemModel,
                guiOnLabel = guiOnLabel,
                guiOffLabel = guiOffLabel,
                inUse = false, //Fresh machines shouldn't be in use upon placement
                currentPlayerUUID = null
            )
            Lists.machineInstances.add(machineInstanceToAdd)

            loadedMachinesTotal++

            logger().info("Loaded machine " + id + " with model " + model)
            loadedMachineGuisTotal++
        }
    }
    private fun loadOneRecipeFile(file: File)
    {
        logger().info("Loading recipe file " + file.name + "...")
        val cfg = YamlConfiguration()
        try {
            cfg.load(file)
        } catch (e: Exception) {
            logger().error("Failed to parse YAML '" + file.name + "': " + e.message)
            e.printStackTrace()
            return
        }

        val ids = cfg.getKeys(false)
        if (ids.isEmpty()) return

        for (rid in ids)
        {
            val sec = cfg.getConfigurationSection(rid) ?: continue

// Read via absolute path to avoid any section/path ambiguity
            val assignedMachineId = cfg.getString(rid + ".assigned_machine") ?: run {
                // quick debug to see what Bukkit actually parsed:
                logger().warn("Recipe '" + rid + "' keys: " + sec.getKeys(false).joinToString(","))
                val raw = sec.get("assigned_machine")
                logger().warn("Recipe '" + rid + "' assigned_machine raw=" + raw + " type=" + (raw?.javaClass?.name ?: "null"))

                logger().error("Recipe '" + rid + "': missing 'assigned_machine' -> skipping.")
                continue
            }


            val machine = Lists.machineInstances.firstOrNull { it.machineId == assignedMachineId } ?: run {
                logger().error("Recipe '" + rid + "': machine '" + assignedMachineId + "' not found -> skipping.")
                continue
            }

            val timeTicks  = sec.getInt("time_ticks", 200)
            val energyCost = sec.getInt("energy_cost", 0)

            val inputs: MutableMap<Int, ItemStack>  = mutableMapOf()
            val outputs: MutableMap<Int, ItemStack> = mutableMapOf()

            // input:
            sec.getMapList("input").forEach { raw ->
                val m = raw as? Map<*, *> ?: return@forEach
                val slot   = (m["slot"] as? Number)?.toInt() ?: return@forEach
                val itemId = m["item"] as? String ?: return@forEach
                val amount = (m["amount"] as? Number)?.toInt() ?: 1

                val stack = ItemWorker.getItemStackFromString(itemId).apply { this.amount = amount }
                inputs[slot] = stack
            }

            // output:
            sec.getMapList("output").forEach { raw ->
                val m = raw as? Map<*, *> ?: return@forEach
                val slot   = (m["slot"] as? Number)?.toInt() ?: return@forEach
                val itemId = m["item"] as? String ?: return@forEach
                val amount = (m["amount"] as? Number)?.toInt() ?: 1

                val stack = ItemWorker.getItemStackFromString(itemId).apply { this.amount = amount }
                outputs[slot] = stack
            }

            val recipe = RecipeInstance(
                assignedMachineInstance = machine,
                recipeId = rid,
                recipeInputItems = inputs,
                recipeOutputItems = outputs,
                recipeProcessingTime = timeTicks,
                recipeEnergyConsumption = energyCost,
            )

            Lists.recipeInstances.add(recipe)
            loadedRecipesTotal++

            logger().info(
                "Loaded recipe " + rid + " for machine " + assignedMachineId + " (inputs=" + inputs.size + ", outputs=" + outputs.size + ", time=" + timeTicks + ", energy=" + energyCost + ")"
            )
        }
    }
}
