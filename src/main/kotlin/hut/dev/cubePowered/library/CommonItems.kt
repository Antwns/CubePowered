package hut.dev.cubePowered.library

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.GuiItem
import hut.dev.cubePowered.workers.AutoProcessToggleWorker
import hut.dev.cubePowered.workers.ItemWorker
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin

object CommonItems {

    /** Resolve ItemStack from a namespaced id (vanilla or ItemsAdder). */
    fun idToItemStack(id: String): ItemStack =
        ItemWorker.getItemStackFromString(id)

    /** Locked-slot filler: click-cancelled, explicitly NO display name. */
    fun filler(id: String): GuiItem
    {
        val s = idToItemStack(id).clone()
        s.itemMeta = s.itemMeta.apply { displayName(null) } // ensure nameless
        return ItemBuilder.from(s).asGuiItem { it.isCancelled = true }
    }

    /** Build ON/OFF toggle stacks, applying labels from YAML (null => no override). */
    fun autoProcessOnStack(itemId: String, label: String?): ItemStack {
        val currentOnStack = idToItemStack(itemId).clone()
        if (label != null) {
            currentOnStack.itemMeta = currentOnStack.itemMeta.apply { displayName(Component.text(label).color(NamedTextColor.GREEN)) }
        }
        return currentOnStack
    }

    fun autoProcessOffStack(itemId: String, label: String?): ItemStack {
        val currentOffStack = idToItemStack(itemId).clone()
        if (label != null) {
            currentOffStack.itemMeta = currentOffStack.itemMeta.apply { displayName(Component.text(label).color(NamedTextColor.RED)) }
        }
        return currentOffStack
    }

    /** Wrap any stack as a click-cancelled GuiItem. */
    fun itemStackToDefaultGuiItem(stack: ItemStack, cancel: Boolean = true, isButton: Boolean = true, currentMachineInstance : MachineInstance, plugin: Plugin): GuiItem {
        if(cancel == false && isButton == true)
        {
            return ItemBuilder.from(stack).asGuiItem { e -> AutoProcessToggleWorker.toggleAutoProcessMode(plugin, e, currentMachineInstance, currentMachineInstance.guiToggleItemSlot)}
        }
        else if(cancel == true && isButton == true)
        {
            return ItemBuilder.from(stack).asGuiItem { e -> e.isCancelled = true }
        }
        else
        {
            return ItemBuilder.from(stack).asGuiItem()
        }
    }
}
