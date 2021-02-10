import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.MapMeta
import org.bukkit.map.MapRenderer
import org.bukkit.map.MapView
import org.bukkit.plugin.java.JavaPlugin

class Loremaster : JavaPlugin(), CommandExecutor {

    val MAX_LENGTH = config["MAX_LENGTH"] as Int
    val MAX_LINE_LENGTH = config["MAX_LINE_LENGTH"] as Int
    val XP_COST = config["XP_COST"] as Int
    val LORE_OVERWRITE_ALLOWED = config["LORE_OVERWRITE_ALLOWED"] as Boolean
    val LORE_STACKABLES_ALLOWED = config["LORE_STACKABLES_ALLOWED"] as Boolean

    override fun onEnable() {
        this.saveDefaultConfig()
        getCommand("loremaster")?.setExecutor(this)
        getCommand("loremaster")?.setTabCompleter { _, _, _, _ -> listOf<String>() }
        getCommand("loreclear")?.setExecutor(ClearCommand())
        getCommand("loreclear")?.setTabCompleter { _, _, _, _ -> listOf<String>() }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        val player = Bukkit.getPlayer(sender.name) ?: return false
        val item = player.itemInHand
        val meta = item.itemMeta ?: return false

        if (args.isEmpty()) return false
        if (item.maxStackSize > 1 && !LORE_STACKABLES_ALLOWED) {
            Messager.msg(player, "§6You can only write lore to non-stackable items")
            return true
        }
        if (meta.hasLore() && !LORE_OVERWRITE_ALLOWED) {
            Messager.msg(player, "§6This item already has lore")
            return true
        }
        if (player.level < XP_COST) {
            Messager.msg(player, "§6You need ${XP_COST - player.level} more levels in order to write lore")
            return true
        }

        val lore = mutableListOf<String>()
        var line = ""
        var charsOnLine = 0
        var totalLength = 0
        val colorCodes = mutableListOf<Char>()

        fun addWord(it: String) {
            if (it.split("\\n").any { it.length > MAX_LINE_LENGTH }) {
                Messager.msg(player, "§6A word was too long and does not fit on a single line, please split it into smaller parts")
                throw IllegalArgumentException()
            }

            // find and store color codes:
            it.forEachIndexed { index, c ->
                if (c == '&') {
                    if (index + 1 < it.length) {
                        if (it[index + 1] == 'r') colorCodes.clear()
                        else colorCodes.add(it[index + 1])
                    }
                }
            }

            if (it.contains("\\n")) {
                line += it.substringBefore("\\n")
                lore.add(line.replace('&', '§'))
                val remainder = it.substringAfter("\\n")
                // start a new line, append color codes
                line = ""
                colorCodes.forEach { line += "&$it" }
                charsOnLine = 0
                addWord(remainder)

            } else {
                if (charsOnLine + it.length > MAX_LINE_LENGTH) {
                    lore.add(line.replace('&', '§'))
                    // start a new line, append color codes
                    line = ""
                    colorCodes.forEach { line += "&$it" }
                    charsOnLine = 0
                }
                line += "$it "
                charsOnLine += it.length + 1
                totalLength += it.length + 1
            }
        }

        args.forEach {
            try {
                addWord(it)
            } catch (iae: IllegalArgumentException) { return true }
        }
        lore.add(line.replace('&', '§'))


        if (totalLength > MAX_LENGTH) {
            Messager.msg(player, "§6Lore was too long ($totalLength), maximum length is $MAX_LENGTH")
            return true
        }

        meta.lore = lore
        item.itemMeta = meta
        player.level = player.level - XP_COST

        Messager.msg(player, "§aSuccess")
        return true
    }
}

class ClearCommand: CommandExecutor {
    override fun onCommand(sender: CommandSender, p1: Command, p2: String, p3: Array<out String>): Boolean {
        val player = Bukkit.getPlayer(sender.name) ?: return false
        val item = player.itemInHand
        val meta = item.itemMeta ?: return false
        if (!meta.hasLore()) {
            Messager.msg(player, "§6This item does not have lore")
            return true
        }
        meta.lore = null
        item.itemMeta = meta

        Messager.msg(player, "§aSuccess")
        return true
    }
}

object Messager {
    fun msg(player: Player, msg: String) {
        player.sendMessage("[Loremaster] $msg")
    }
}