package hut.dev.cubePowered.workers

import java.io.File

class FileWorker
{
    internal  fun ensureExampleMachineFile(dataFolder: File) {
        val machinesDir = File(dataFolder, "machines")
        if (!machinesDir.exists()) machinesDir.mkdirs()

        val example = File(machinesDir, "my_machine_1.yml")
        if (example.exists()) return

        val yaml = """
        my_machine_1:
          model: "generators:basic_generator"
          inventory:
            title: "Basic Generator"
            type: "chest"      # chest | hopper | dispenser
            size: 27           # valid: 9,18,27,36,45,54 (0-based slots)
          slots:
            input:  [6, 7]
            output: [8, 9]
            fuel:   []
          energy:
            enabled: true
            per_tick: -10      # consume 10 per tick
            buffer: 10000
          processing:
            auto: true
            interval_ticks: 200
            toggleable: true
          behavior:
            on_output_full: "block"
            pause_if_no_energy: true
          gui:
            toggle:
              enabled: true
              slot: 4
              on_item:  "minecraft:lime_dye"
              off_item: "minecraft:red_dye"
              on_label:  "Auto: ON"
              off_label: "Auto: OFF"
    """.trimIndent()

        example.writeText(yaml, Charsets.UTF_8)
    }
}