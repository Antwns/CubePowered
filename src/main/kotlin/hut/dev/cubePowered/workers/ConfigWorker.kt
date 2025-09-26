package hut.dev.cubePowered.workers

import hut.dev.cubePowered.library.ConductorInstance
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
    internal var loadedConductorsTotal = 0
    /** Load/ all data from plugins/CubePowered/*/.yml */*/
    fun loadData(dataFolder: File)
    {
        FileWorker().ensureExampleMachineFile(dataFolder)
        FileWorker().ensureExampleRecipeFile(dataFolder)
        FileWorker().ensureConductorFile(dataFolder)

        val machinesDir = File(dataFolder, "machines")
        val recipesDir = File(dataFolder, "recipes")
        val conductorsDir = File(dataFolder, "conductors")
        if (!machinesDir.exists()) machinesDir.mkdirs()
        if (!recipesDir.exists()) recipesDir.mkdirs()
        if (!conductorsDir.exists()) conductorsDir.mkdirs()

        Lists.machineInstances.clear()//may remove in the future

        loadedMachineGuisTotal = 0
        loadedMachinesTotal = 0
        loadedRecipesTotal = 0
        loadedConductorsTotal = 0

        val machineFiles = machinesDir.listFiles { f ->
            f.isFile && f.name.endsWith(".yml", ignoreCase = true)
        } ?: emptyArray()

        val recipeFiles = recipesDir.listFiles { f ->
            f.isFile && f.name.endsWith(".yml", ignoreCase = true)
        } ?: emptyArray()

        val conductorFiles = conductorsDir.listFiles { f ->
            f.isFile && f.name.endsWith(".yml", ignoreCase = true)
        }

        logger().info("Found " + machineFiles.size + " machine file(s) in " + machinesDir.absolutePath)
        logger().info("Found " + recipeFiles.size + " recipe file(s) in " + recipesDir.absolutePath)
        logger().info("Found " + conductorFiles.size + " conductor file(s) in " + conductorsDir.absolutePath)

        machineFiles.forEach { machineDefinitionFile ->
            loadOneMachineFile(machineDefinitionFile)
        }

        logger().info("Loaded " + loadedMachinesTotal + " machine definition(s).")
        logger().info("Loaded " + loadedMachineGuisTotal + " gui definition(s).")

        logger().info("Loading recipes...")

        recipeFiles.forEach { recipeDefinitionFile ->
            loadOneRecipeFile(recipeDefinitionFile)
        }

        logger().info("Loaded " + loadedRecipesTotal + " recipe definitions" )

        conductorFiles.forEach { conductorDefinitionFile ->
            loadOneConductorFile(conductorDefinitionFile)
        }

        logger().info("Loaded " + loadedConductorsTotal + " conductor definitions")
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

            if (!verifySlotIntegrity(id, inventorySize, inputSlots, outputSlots)) {
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

    private fun loadOneConductorFile(file: File)
    {
        logger().info("Loading conductor file " + file.name + "...")
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

        for (cid in ids)
        {
            val sec = cfg.getConfigurationSection(cid) ?: continue

            val knobModel = sec.getString("knob_model") ?: run {
                logger().error("Conductor '" + cid + "': missing 'knob_model' -> skipping.")
                continue
            }

            val connectorModel = sec.getString("connector_model") ?: run {
                logger().error("Conductor '" + cid + "': missing 'connector_model' -> skipping.")
                continue
            }

            val maxPower = sec.getInt("max_power", 0)
            if (maxPower <= 0) {
                logger().error("Conductor '" + cid + "': 'max_power' must be > 0 -> skipping.")
                continue
            }

            // Read optional behavior flags (not stored in ConductorInstance yet)
            val explodeOnOverload = sec.getBoolean("explode_on_overload", false)
            val explodePower      = sec.getInt("explode_power", 0)
            val vanishOnOverload  = sec.getBoolean("vanishOnOverload", false)

            val conductor = ConductorInstance(
                conductorId = cid,
                conductorKnobModel = knobModel,
                conductorConnectorModel = connectorModel,
                conductorKey = null,            // filled at runtime on placement
                conductorWorld = null,          // filled at runtime on placement
                conductorX = null,              // filled at runtime on placement
                conductorY = null,              // filled at runtime on placement
                conductorZ = null,              // filled at runtime on placement
                conductorMaxPower = maxPower,
                conductorCurrentPower = 0,
                conductorExplodeOnOverload = explodeOnOverload,
                conductorExplodePower = explodePower,
                conductorVanishOnOverload = vanishOnOverload
            )

            Lists.conductorInstances.add(conductor)
            loadedConductorsTotal++

            logger().info("Loaded conductor " + cid + " knob_model=" + knobModel + " connector_model=" + connectorModel + " max_power=" + maxPower)
        }
    }
}
