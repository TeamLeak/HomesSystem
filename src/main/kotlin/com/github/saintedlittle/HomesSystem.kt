package com.github.saintedlittle


import com.github.saintedlittle.commands.HomeCommands
import com.github.saintedlittle.config.PluginConfig
import com.github.saintedlittle.db.Database
import com.github.saintedlittle.i18n.Messages
import com.github.saintedlittle.repository.HomeRepository
import com.github.saintedlittle.service.HomeService
import com.github.saintedlittle.teleport.TeleportListener
import com.github.saintedlittle.teleport.TeleportManager
import com.github.saintedlittle.util.Registry
import org.bukkit.plugin.java.JavaPlugin


class HomesSystem : JavaPlugin() {
    lateinit var configModel: PluginConfig; private set
    lateinit var messages: Messages; private set
    lateinit var db: Database; private set
    lateinit var repo: HomeRepository; private set
    lateinit var service: HomeService; private set
    lateinit var teleports: TeleportManager; private set
    lateinit var teleportListener: TeleportListener; private set


    val registries = Registry.Root()


    override fun onEnable() {
        saveDefaultConfig()
        saveResource("messages_en.yml", false)
        saveResource("messages_ru.yml", false)


        this.configModel = PluginConfig(this)
        this.messages = Messages(this)
        this.db = Database(this, configModel)
        this.repo = HomeRepository(db)
        this.service = HomeService(this, repo, configModel)
        this.teleports = TeleportManager(this, messages, configModel)
        this.teleportListener = TeleportListener(this, teleports)
        teleportListener.register()

// Commands
        HomeCommands(this).register()


        logger.info("HomesSystem enabled. DB: ${db.sqlitePath}")
    }


    override fun onDisable() {
        teleports.shutdown()
        db.close()
    }
}