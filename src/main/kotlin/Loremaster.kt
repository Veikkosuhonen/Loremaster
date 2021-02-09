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
        getCommand("loremaster")?.setTabCompleter { _, _, _, _ -> listOf<String>() }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        val player = Bukkit.getPlayer(sender.name) ?: return false
        val item = player.itemInHand
        val meta = item.itemMeta ?: return false

        if (args.isEmpty()) return false
        if (item.maxStackSize > 1 && !LORE_STACKABLES_ALLOWED) {
            msg(player, "§6You can only write lore to non-stackable items")
            return true
        }
        if (meta.hasLore() && !LORE_OVERWRITE_ALLOWED) {
            msg(player, "§6This item already has lore")
            return true
        }
        if (player.level < XP_COST) {
            msg(player, "§6You need ${XP_COST - player.level} more levels in order to write lore")
            return true
        }

        val lore = mutableListOf<String>()
        var line = ""
        var charsOnLine = 0
        var totalLength = 0
        args.forEach {
            if (it == "\\n") {
                lore.add(line.replace('&', '§'))
                line = ""
                charsOnLine = 0
            } else {
                if (charsOnLine + it.length > MAX_LINE_LENGTH) {
                    lore.add(line.replace('&', '§'))
                    line = ""
                    charsOnLine = 0
                }
                line += "$it "
                charsOnLine += it.length + 1
                totalLength += it.length + 1
            }
        }
        lore.add(line.replace('&', '§'))

        if (totalLength > MAX_LENGTH) {
            msg(player, "§6Lore was too long ($totalLength), maximum length is $MAX_LENGTH")
            return true
        }

        player.level = player.level - XP_COST

        meta.lore = lore
        item.itemMeta = meta

        msg(player, "§aSuccess")

        return true
    }

    private fun msg(player: Player, msg: String) {
        player.sendMessage("[Loremaster] $msg")
    }
}