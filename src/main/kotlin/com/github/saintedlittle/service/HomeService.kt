package com.github.saintedlittle.service


import com.github.saintedlittle.HomesSystem
import com.github.saintedlittle.config.PluginConfig
import com.github.saintedlittle.model.Home
import com.github.saintedlittle.repository.HomeRepository
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.*


class HomeService(
    private val plugin: HomesSystem,
    private val repo: HomeRepository,
    private val cfg: PluginConfig,
) {
    private val limitRegex = Regex("^homessystem\\.lvl(\\d+)\\.homes$", RegexOption.IGNORE_CASE)


    fun normalizedName(input: String?): String? {
        val name = when {
            input.isNullOrBlank() -> cfg.defaultName
            else -> input.trim()
        }
        val n = if (cfg.forceLowercase) name.lowercase(Locale.ROOT) else name
        if (n.length !in cfg.minName..cfg.maxName) return null
        if (!cfg.pattern.matches(n)) return null
        return n
    }


    fun maxHomesOf(player: Player): Int {
        if (cfg.allowBypassLimit && player.hasPermission("homessystem.bypass.limit")) return Int.MAX_VALUE
        var max = cfg.defaultMaxHomes
        for (perm in player.effectivePermissions) {
            val m = limitRegex.matchEntire(perm.permission) ?: continue
            if (!perm.value) continue
            val n = m.groupValues[1].toIntOrNull() ?: continue
            if (n > max) max = n
        }
        return max
    }


    fun setHome(player: Player, name: String, loc: Location): Pair<Home, Boolean> {
        val count = repo.count(player.uniqueId)
        val limit = maxHomesOf(player)
        if (repo.find(player.uniqueId, name) == null && count >= limit) {
            throw MaxHomesReached(limit)
        }
        val now = System.currentTimeMillis()
        val home = Home(
            owner = player.uniqueId,
            name = name,
            world = loc.world!!.name,
            x = loc.x, y = loc.y, z = loc.z,
            yaw = loc.yaw, pitch = loc.pitch,
            createdAt = now,
            updatedAt = now,
        )
        return repo.upsert(home)
    }


    fun getHome(player: Player, name: String): Home? = repo.find(player.uniqueId, name)
    fun listHomes(player: Player) = repo.list(player.uniqueId)
    fun deleteHome(player: Player, name: String): Boolean = repo.delete(player.uniqueId, name)


    class MaxHomesReached(val limit: Int) : RuntimeException()
}