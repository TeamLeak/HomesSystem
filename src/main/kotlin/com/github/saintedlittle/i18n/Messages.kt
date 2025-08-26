package com.github.saintedlittle.i18n

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.io.File
import java.util.*


class Messages(private val plugin: Plugin) {
    private val mm = MiniMessage.miniMessage()
    private val bundles: MutableMap<String, org.bukkit.configuration.file.YamlConfiguration> = HashMap()


    init {
// Preload default locales
        load("en"); load("ru")
    }


    private fun load(lang: String) {
        val fileName = if (lang.equals("en", true)) "messages_en.yml" else "messages_${lang}.yml"
        val file = File(plugin.dataFolder, fileName)
        if (!file.exists()) plugin.saveResource(fileName, false)
        val yml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file)
        bundles[lang.lowercase(Locale.ROOT)] = yml
    }


    private fun raw(lang: String, path: String, def: String = path): String {
        val l = lang.lowercase(Locale.ROOT)
        val yml = bundles[l] ?: run { load(l); bundles[l] } ?: bundles["en"]!!
        return yml.getString(path) ?: bundles["en"]!!.getString(path, def)!!
    }


    fun prefixed(lang: String, path: String, placeholders: Map<String, String> = emptyMap()): String {
        val prefix = raw(lang, "prefix", "")
        return colorize(prefix + format(raw(lang, path), placeholders))
    }


    fun plain(lang: String, path: String, placeholders: Map<String, String> = emptyMap()): String {
        return colorize(format(raw(lang, path), placeholders))
    }


    private fun format(input: String, placeholders: Map<String, String>): String {
        var s = input
        placeholders.forEach { (k, v) -> s = s.replace("{$k}", v) }
        return s
    }


    fun send(sender: CommandSender, path: String, placeholders: Map<String, String> = emptyMap(), prefixed: Boolean = true) {
        val lang = (sender as? Player)?.locale()?.language ?: plugin.config.getString("locale", "en")!!
        val msg = if (prefixed) prefixed(lang, path, placeholders) else plain(lang, path, placeholders)
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg))
    }

    private fun colorize(input: String): String {
        return ChatColor.translateAlternateColorCodes('&', input)
    }
}