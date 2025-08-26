package com.github.saintedlittle.config


import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.Plugin


class PluginConfig(private val plugin: Plugin) {
    val locale: String
    val defaultMaxHomes: Int
    val allowBypassLimit: Boolean
    val delaySeconds: Int
    val cancelOnMove: Boolean
    val moveTolerance: Double
    val cancelOnDamage: Boolean
    val defaultName: String
    val minName: Int
    val maxName: Int
    val pattern: Regex
    val forceLowercase: Boolean
    val sqliteFile: String
    val pragmaJournalMode: String
    val pragmaSynchronous: String
    val pragmaBusyTimeoutMs: Int
    val pragmaCacheSizeKb: Int
    val prefix: String
    val debug: Boolean


    init {
        plugin.reloadConfig()
        val cfg: FileConfiguration = plugin.config
        locale = cfg.getString("locale", "en")!!
        defaultMaxHomes = cfg.getInt("limits.default_max_homes", 1)
        allowBypassLimit = cfg.getBoolean("limits.allow_bypass_via_permission", true)
        delaySeconds = cfg.getInt("teleport.delay_seconds", 3)
        cancelOnMove = cfg.getBoolean("teleport.cancel_on_move", true)
        moveTolerance = cfg.getDouble("teleport.move_tolerance", 0.12)
        cancelOnDamage = cfg.getBoolean("teleport.cancel_on_damage", true)
        defaultName = cfg.getString("names.default_name", "home")!!
        minName = cfg.getInt("names.min_length", 1)
        maxName = cfg.getInt("names.max_length", 16)
        val patternStr = cfg.getString("names.pattern", "^[a-z0-9_\\-]+$")!!
        pattern = Regex(patternStr)
        forceLowercase = cfg.getBoolean("names.force_lowercase", true)
        sqliteFile = cfg.getString("sqlite.file", "homes.db")!!
        pragmaJournalMode = cfg.getString("sqlite.pragmas.journal_mode", "WAL")!!
        pragmaSynchronous = cfg.getString("sqlite.pragmas.synchronous", "NORMAL")!!
        pragmaBusyTimeoutMs = cfg.getInt("sqlite.pragmas.busy_timeout_ms", 5000)
        pragmaCacheSizeKb = cfg.getInt("sqlite.pragmas.cache_size_kb", -8192)
        prefix = cfg.getString("messages.prefix", "&8[&aHomes&8]&r ")!!
        debug = cfg.getBoolean("debug", false)
    }
}