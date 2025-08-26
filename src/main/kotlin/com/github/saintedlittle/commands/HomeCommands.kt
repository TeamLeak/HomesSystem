package com.github.saintedlittle.commands

import com.github.saintedlittle.HomesSystem
import com.github.saintedlittle.service.HomeService
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class HomeCommands(private val plugin: HomesSystem) : CommandExecutor, TabCompleter {

    fun register() {
        plugin.getCommand("sethome")!!.setExecutor(this)
        plugin.getCommand("home")!!.setExecutor(this)
        plugin.getCommand("delhome")!!.setExecutor(this)
        plugin.getCommand("sethome")!!.tabCompleter = this
        plugin.getCommand("home")!!.tabCompleter = this
        plugin.getCommand("delhome")!!.tabCompleter = this
    }

    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<out String>): Boolean {
        val p = sender as? Player
        if (p == null) {
            plugin.messages.send(sender, "error.players_only")
            return true
        }

        when (cmd.name.lowercase()) {
            "sethome" -> {
                val raw = args.getOrNull(0)
                val name = plugin.service.normalizedName(raw)
                if (name == null) {
                    plugin.messages.send(p, "error.invalid_name")
                    if (raw == null) plugin.messages.send(p, "error.usage_sethome", prefixed = false)
                    return true
                }
                val loc = p.location.clone()
                Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                    try {
                        val (saved, created) = plugin.service.setHome(p, name, loc)
                        val count = plugin.repo.count(p.uniqueId)
                        val limit = plugin.service.maxHomesOf(p)
                        Bukkit.getScheduler().runTask(plugin, Runnable {
                            val key = if (created) "set.created" else "set.updated"
                            plugin.messages.send(
                                p, key,
                                mapOf(
                                    "NAME" to name,
                                    "COUNT" to count.toString(),
                                    "LIMIT" to (if (limit == Int.MAX_VALUE) "∞" else limit.toString())
                                )
                            )
                        })
                    } catch (ex: HomeService.MaxHomesReached) {
                        Bukkit.getScheduler().runTask(plugin, Runnable {
                            plugin.messages.send(p, "error.max_reached", mapOf("LIMIT" to ex.limit.toString()))
                        })
                    }
                })
                return true
            }

            "home" -> {
                val raw = args.getOrNull(0)
                val name = plugin.service.normalizedName(raw)
                if (name == null || raw == null) {
                    plugin.messages.send(p, "error.usage_home")
                    return true
                }
                Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                    val home = plugin.service.getHome(p, name)
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        if (home == null) {
                            plugin.messages.send(p, "error.not_found", mapOf("NAME" to name))
                        } else {
                            val world = Bukkit.getWorld(home.world)
                            if (world == null) {
                                plugin.messages.send(p, "error.not_found", mapOf("NAME" to name))
                                return@Runnable
                            }
                            val loc = org.bukkit.Location(world, home.x, home.y, home.z, home.yaw, home.pitch)
                            plugin.teleports.scheduleTeleport(p, name, loc)
                        }
                    })
                })
                return true
            }

            "delhome" -> {
                val raw = args.getOrNull(0)
                val name = plugin.service.normalizedName(raw)
                if (name == null || raw == null) {
                    plugin.messages.send(p, "error.usage_delhome")
                    return true
                }
                Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                    val ok = plugin.service.deleteHome(p, name)
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        if (ok) plugin.messages.send(p, "delete.ok", mapOf("NAME" to name))
                        else plugin.messages.send(p, "error.not_found", mapOf("NAME" to name))
                    })
                })
                return true
            }
        }
        return false
    }

    override fun onTabComplete(sender: CommandSender, cmd: Command, alias: String, args: Array<out String>): MutableList<String> {
        val p = sender as? Player ?: return mutableListOf()
        return when (cmd.name.lowercase()) {
            "sethome" -> mutableListOf()
            "home", "delhome" -> {
                if (args.size == 1) {
                    val prefix = args[0].lowercase()
                    plugin.service.listHomes(p)
                        .map { it.name }
                        .filter { it.startsWith(prefix) }
                        .toMutableList()
                } else mutableListOf()
            }
            else -> mutableListOf()
        }
    }
}
