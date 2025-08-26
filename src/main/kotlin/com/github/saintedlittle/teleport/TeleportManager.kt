package com.github.saintedlittle.teleport

import com.github.saintedlittle.HomesSystem
import com.github.saintedlittle.config.PluginConfig
import com.github.saintedlittle.i18n.Messages
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class TeleportManager(
    private val plugin: HomesSystem,
    private val messages: Messages,
    private val cfg: PluginConfig
) {

    private data class Pending(
        val target: Location,
        val task: BukkitTask,
        val startPos: Location
    )

    // playerId -> pending
    private val pending = ConcurrentHashMap<UUID, Pending>()

    /**
     * Планирует телепорт игрока с задержкой.
     * Отменяется, если игрок двигается или получает урон (в зависимости от настроек).
     */
    fun scheduleTeleport(p: Player, name: String, target: Location) {
        // БЕЗ задержки / байпас
        if (p.hasPermission("homessystem.bypass.delay") || cfg.delaySeconds <= 0) {
            p.teleportAsync(target)
            messages.send(p, "teleport.success", mapOf("NAME" to name))
            return
        }

        // Уже телепортируется
        if (pending.containsKey(p.uniqueId)) {
            messages.send(p, "error.already_teleporting")
            return
        }

        messages.send(
            p,
            "teleport.scheduled",
            mapOf("NAME" to name, "SECONDS" to cfg.delaySeconds.toString())
        )

        val start = p.location.clone()

        // помечаем старт для листенера
        plugin.teleportListener.markStart(p)

        val task = Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            // если к моменту выполнения уже отменили — выходим
            val pend = pending.remove(p.uniqueId) ?: run {
                plugin.teleportListener.unmark(p)
                return@Runnable
            }

            // Снять метку (больше не следим за движением)
            plugin.teleportListener.unmark(p)

            if (!p.isOnline) return@Runnable

            // телепорт
            p.teleportAsync(pend.target)
            messages.send(p, "teleport.success", mapOf("NAME" to name))
        }, cfg.delaySeconds * 20L)

        pending[p.uniqueId] = Pending(target, task, start)
    }

    /** Явная отмена с отправкой сообщения (используется слушателем). */
    fun cancel(p: Player, reasonKey: String) {
        val pend = pending.remove(p.uniqueId) ?: return
        pend.task.cancel()
        plugin.teleportListener.unmark(p)
        messages.send(p, reasonKey)
    }

    /** Внутренняя отмена без сообщения (если нужно). */
    fun cancelSilent(p: Player) {
        val pend = pending.remove(p.uniqueId) ?: return
        pend.task.cancel()
        plugin.teleportListener.unmark(p)
    }

    fun isPending(p: Player): Boolean = pending.containsKey(p.uniqueId)

    fun getStartLocation(p: Player): Location? = pending[p.uniqueId]?.startPos

    fun shutdown() {
        pending.values.forEach { it.task.cancel() }
        pending.clear()
    }
}
