package com.github.saintedlittle.commands

import com.github.saintedlittle.HomesSystem
import com.github.saintedlittle.service.HomeService
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.*

class HomeCommands(private val plugin: HomesSystem) : CommandExecutor, TabCompleter {

    fun register() {
        plugin.getCommand("sethome")!!.setExecutor(this)
        plugin.getCommand("home")!!.setExecutor(this)
        plugin.getCommand("delhome")!!.setExecutor(this)
        plugin.getCommand("sethome")!!.tabCompleter = this
        plugin.getCommand("home")!!.tabCompleter = this
        plugin.getCommand("delhome")!!.tabCompleter = this

        // NEW: /homes
        plugin.getCommand("homes")!!.setExecutor(this)
        plugin.getCommand("homes")!!.tabCompleter = this
    }

    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<out String>): Boolean {
        val p = sender as? Player

        when (cmd.name.lowercase()) {
            "sethome" -> {
                val player = p ?: run { plugin.messages.send(sender, "error.players_only"); return true }
                val raw = args.getOrNull(0)
                val name = plugin.service.normalizedName(raw)
                if (name == null) {
                    plugin.messages.send(player, "error.invalid_name")
                    if (raw == null) plugin.messages.send(player, "error.usage_sethome", prefixed = false)
                    return true
                }
                val loc = player.location.clone()
                Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                    try {
                        val (saved, created) = plugin.service.setHome(player, name, loc)
                        val count = plugin.repo.count(player.uniqueId)
                        val limit = plugin.service.maxHomesOf(player)
                        Bukkit.getScheduler().runTask(plugin, Runnable {
                            val key = if (created) "set.created" else "set.updated"
                            plugin.messages.send(
                                player, key,
                                mapOf(
                                    "NAME" to name,
                                    "COUNT" to count.toString(),
                                    "LIMIT" to (if (limit == Int.MAX_VALUE) "∞" else limit.toString())
                                )
                            )
                        })
                    } catch (ex: HomeService.MaxHomesReached) {
                        Bukkit.getScheduler().runTask(plugin, Runnable {
                            plugin.messages.send(player, "error.max_reached", mapOf("LIMIT" to ex.limit.toString()))
                        })
                    }
                })
                return true
            }

            "home" -> {
                val player = p ?: run { plugin.messages.send(sender, "error.players_only"); return true }
                val raw = args.getOrNull(0)
                val name = plugin.service.normalizedName(raw)
                if (name == null || raw == null) {
                    plugin.messages.send(player, "error.usage_home")
                    return true
                }
                Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                    val home = plugin.service.getHome(player, name)
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        if (home == null) {
                            plugin.messages.send(player, "error.not_found", mapOf("NAME" to name))
                        } else {
                            val world = Bukkit.getWorld(home.world)
                            if (world == null) {
                                plugin.messages.send(player, "error.not_found", mapOf("NAME" to name))
                                return@Runnable
                            }
                            val loc = org.bukkit.Location(world, home.x, home.y, home.z, home.yaw, home.pitch)
                            plugin.teleports.scheduleTeleport(player, name, loc)
                        }
                    })
                })
                return true
            }

            "delhome" -> {
                val player = p ?: run { plugin.messages.send(sender, "error.players_only"); return true }
                val raw = args.getOrNull(0)
                val name = plugin.service.normalizedName(raw)
                if (name == null || raw == null) {
                    plugin.messages.send(player, "error.usage_delhome")
                    return true
                }
                Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                    val ok = plugin.service.deleteHome(player, name)
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        if (ok) plugin.messages.send(player, "delete.ok", mapOf("NAME" to name))
                        else plugin.messages.send(player, "error.not_found", mapOf("NAME" to name))
                    })
                })
                return true
            }

            // NEW: /homes and /homes <player>
            "homes" -> {
                // /homes (self)
                if (args.isEmpty()) {
                    val player = p ?: run { plugin.messages.send(sender, "error.players_only"); return true }
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                        val homes = plugin.service.listHomes(player)
                        Bukkit.getScheduler().runTask(plugin, Runnable {
                            sendHomesList(sender, homes, header = "§aYour homes (§f${homes.size}§a):")
                        })
                    })
                    return true
                }

                // /homes <player> (others) — console allowed, requires permission
                if (!sender.hasPermission("homessystem.homes.others")) {
                    plugin.messages.send(sender, "no_permission")
                    return true
                }

                val targetName = args[0]
                val targetUuid = resolveUuid(targetName)
                if (targetUuid == null) {
                    // Use your messages system if you already have a key; fallback to a simple line otherwise
                    plugin.messages.send(sender, "error.player_not_found", mapOf("NAME" to targetName))
                    return true
                }

                Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                    val homes = plugin.service.listHomesOf(targetUuid)
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        val header = "§aHomes of §e$targetName §a(§f${homes.size}§a):"
                        sendHomesList(sender, homes, header)
                    })
                })
                return true
            }
        }
        return false
    }

    override fun onTabComplete(sender: CommandSender, cmd: Command, alias: String, args: Array<out String>): MutableList<String> {
        val p = sender as? Player

        return when (cmd.name.lowercase()) {
            "sethome" -> mutableListOf()

            "home", "delhome" -> {
                val player = p ?: return mutableListOf()
                if (args.size == 1) {
                    val prefix = args[0].lowercase()
                    plugin.service.listHomes(player)
                        .map { it.name }
                        .filter { it.startsWith(prefix) }
                        .toMutableList()
                } else mutableListOf()
            }

            // NEW: suggest player names for /homes <player> if sender can view others
            "homes" -> {
                if (args.size == 1 && sender.hasPermission("homessystem.homes.others")) {
                    val pref = args[0].lowercase()
                    val online = Bukkit.getOnlinePlayers().map { it.name }
                    online.filter { it.lowercase().startsWith(pref) }.toMutableList()
                } else mutableListOf()
            }

            else -> mutableListOf()
        }
    }

    private fun sendHomesList(target: CommandSender, homes: List<com.github.saintedlittle.model.Home>, header: String) {
        target.sendMessage(header)
        if (homes.isEmpty()) {
            target.sendMessage(" §7(no homes)")
            return
        }
        for (h in homes) {
            val xi = h.x.toInt(); val yi = h.y.toInt(); val zi = h.z.toInt()
            target.sendMessage(" §f• §e${h.name} §7— §b${h.world} §7@ §f$xi§7,§f$yi§7,§f$zi")
        }
        target.sendMessage(" §8Tip: use §a/home <name> §8to teleport.")
    }

    /**
     * Resolve UUID by player name:
     * - Online: exact match
     * - Offline (cached): use Bukkit.getOfflinePlayerIfCached if available
     * - Fallback: Bukkit.getOfflinePlayer(name) but require hasPlayedBefore() to avoid fake profiles
     */
    private fun resolveUuid(name: String): UUID? {
        Bukkit.getPlayerExact(name)?.let { return it.uniqueId }

        // Paper has getOfflinePlayerIfCached; on Spigot it may be absent — guarded via reflection-like call
        try {
            val m = Bukkit::class.java.methods.firstOrNull { it.name == "getOfflinePlayerIfCached" && it.parameterCount == 1 }
            if (m != null) {
                val off = m.invoke(null, name) as? OfflinePlayer
                if (off != null) return off.uniqueId
            }
        } catch (_: Throwable) { /* ignore */ }

        val off = Bukkit.getOfflinePlayer(name)
        return if (off.hasPlayedBefore() || off.isOnline) off.uniqueId else null
    }
}
