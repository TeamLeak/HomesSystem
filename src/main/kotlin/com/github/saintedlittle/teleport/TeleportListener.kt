package com.github.saintedlittle.teleport

import com.github.saintedlittle.HomesSystem
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerMoveEvent
import java.util.UUID
import kotlin.math.max

class TeleportListener(
    private val plugin: HomesSystem,
    private val teleports: TeleportManager
) : Listener {

    // Сохраняем стартовую локацию только для тех случаев,
    // когда ТП создаётся извне (подстраховка). Основной источник – TeleportManager.
    private val startLocations = mutableMapOf<UUID, org.bukkit.Location>()

    fun register() {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    /** Пометить точку старта для игрока (вызывается из TeleportManager). */
    fun markStart(player: Player) {
        startLocations[player.uniqueId] = player.location.clone()
    }

    /** Снять метку старта. */
    fun unmark(player: Player) {
        startLocations.remove(player.uniqueId)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDamage(e: EntityDamageEvent) {
        if (!plugin.configModel.cancelOnDamage) return
        val p = e.entity as? Player ?: return
        if (!teleports.isPending(p)) return
        teleports.cancel(p, "teleport.cancelled.damage")
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onMove(e: PlayerMoveEvent) {
        if (!plugin.configModel.cancelOnMove) return
        val p = e.player
        if (!teleports.isPending(p)) return

        val to = e.to ?: return
        val start = teleports.getStartLocation(p) ?: startLocations[p.uniqueId] ?: return

        // Если ушли в другой мир — считаем как движение
        if (to.world != start.world) {
            teleports.cancel(p, "teleport.cancelled.move")
            return
        }

        // Допуск движения (квантование координат / лаги)
        val tol = max(0.0, plugin.configModel.moveTolerance)
        if (tol <= 0.0) {
            // строгая проверка XYZ
            if (to.x != start.x || to.y != start.y || to.z != start.z) {
                teleports.cancel(p, "teleport.cancelled.move")
            }
        } else {
            // мягкая проверка по расстоянию
            if (to.distanceSquared(start) > tol * tol) {
                teleports.cancel(p, "teleport.cancelled.move")
            }
        }
    }
}
