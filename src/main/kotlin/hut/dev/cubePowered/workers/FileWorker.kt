package hut.dev.cubePowered.workers

import java.io.File

class FileWorker {
    internal fun ensureExampleMachineFile(dataFolder: File) {
        val machinesDir = File(dataFolder, "machines")
        if (!machinesDir.exists()) machinesDir.mkdirs()
        if (machinesDir.listFiles().count() != 0) {
            return
        }
        val example = File(machinesDir, "my_machine_1.yml")
        if (example.exists()) return

        val yaml = """
my_machine_1:
  model: "generators:basic_generator"
  inventory:
    title: "Basic Generator"
    size: 27           # valid: 9,18,27,36,45,54 (0-based slots)
    rows: 3           # valid 1-6
  slots:
    input:  [10, 13]
    output: [15, 16]
  energy:
    enabled: true     # wether or not the machine will use the energy mechanic
    per_tick: 10      # "10": output 10 per tick, "-10" consume 10 per tick, can be set to 0 if you want to make a battery or energy holder
    buffer: 10000      # max energy storage, can be used to create batteres, energy holders etc
  processing:
    auto: true # if left true the machine will begin ticking and processing data immediately upon placement, should always be true if the machine has no toggle button
    interval_ticks: 200 # processing time
    toggleable: true # wether the machine can be toggle on and off
  behavior:
    on_output_full: "block" # block extra processes once the output slots are full DON'T CHANGE IT FOR NOW
    pause_if_no_energy: true # pause the machine if it has no energy left, recommended to be set to true
  gui: 
    slot: 26 # position of the on and off item
    on_item:  "minecraft:lime_dye" #supports itemsadder item via namespace:item_id
    off_item: "minecraft:red_dye" #supports itemsadder item via namespace:item_id
    on_label:  "ON" # text that will be displayed when the machine is on on that button
    off_label: "OFF" # text that will be displayed when the machine is off on that button
    """.trimIndent()

        example.writeText(yaml, Charsets.UTF_8)
    }

    internal fun ensureExampleRecipeFile(dataFolder: File) {
        val recipesDir = File(dataFolder, "recipes")
        if (!recipesDir.exists()) recipesDir.mkdirs()
        if (recipesDir.listFiles().count() != 0) {
            return
        }
        val example = File(recipesDir, "my_machine_1.yml")
        if (example.exists()) return

        val yaml = """
my_recipe_1:
  assigned_machine: "my_machine_1"
  input:
    - slot: 10
      item: "minecraft:raw_iron"
      amount: 2
    - slot: 13
      item: "minecraft:coal"
      amount: 4
  output:
    - slot: 15
      item: "minecraft:iron_ingot"
      amount: 1
    - slot: 16
      item: "minecraft:diamond"
      amount: 1
  time_ticks: 200
  energy_cost: 1000   # extra energy consumption per craft

my_recipe_2:
  assigned_machine: "my_machine_1"
  input:
    - slot: 10
      item: "minecraft:gunpowder"
      amount: 4
    - slot: 13
      item: "minecraft:paper"
      amount: 1
  output:
    - slot: 15 
      item: "guns:bullet" #supports itemsadder item via namespace:item_id
      amount: 16
  time_ticks: 120
  energy_cost: 400
    """.trimIndent()

        example.writeText(yaml, Charsets.UTF_8)
    }

    internal fun ensureConductorFile(dataFolder: File) {
        val conductorsDir = File(dataFolder, "conductors")
        if (!conductorsDir.exists()) conductorsDir.mkdirs()
        if (conductorsDir.listFiles().count() != 0) {
            return
        }
        val example = File(conductorsDir, "my_conductor_1.yml")
        if (example.exists()) return

        val yaml = """
my_conductor_1:
  model: "cables:copper_cable" #the model of the conductor
  max_power: 10 # the maximum power a conductor of this instance can transmit
  explode_on_overload: true # if a conductor has more energy than it can handle, it can explode
  explode_power: 1 # how strong is the explosion, set only if explode_on_overload is true
  vanishOnOverload: false # if explode_on_overload is true, leave this false, if you enable this the cable will simply vanish instead of exploding
    """.trimIndent()

        example.writeText(yaml, Charsets.UTF_8)
    }
}