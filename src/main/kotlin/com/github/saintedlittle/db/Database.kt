package com.github.saintedlittle.db
import com.github.saintedlittle.config.PluginConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.plugin.Plugin
import java.io.File
import java.sql.Connection


class Database(private val plugin: Plugin, private val cfg: PluginConfig) {
    val sqlitePath: String
    private val ds: HikariDataSource


    init {
        if (!plugin.dataFolder.exists()) plugin.dataFolder.mkdirs()
        val dbFile = File(plugin.dataFolder, cfg.sqliteFile)
        sqlitePath = dbFile.absolutePath


        val hc = HikariConfig().apply {
            jdbcUrl = "jdbc:sqlite:$sqlitePath"
            maximumPoolSize = 8
            minimumIdle = 1
            isAutoCommit = true
            poolName = "HomesSystem-SQLite"
            addDataSourceProperty("journal_mode", cfg.pragmaJournalMode)
            addDataSourceProperty("busy_timeout", cfg.pragmaBusyTimeoutMs)
        }
        ds = HikariDataSource(hc)
        migrate()
        applyPragmas()
    }


    private fun applyPragmas() {
        connection().use { c ->
            c.createStatement().use { st ->
                st.execute("PRAGMA journal_mode=${cfg.pragmaJournalMode}")
                st.execute("PRAGMA synchronous=${cfg.pragmaSynchronous}")
                st.execute("PRAGMA busy_timeout=${cfg.pragmaBusyTimeoutMs}")
                st.execute("PRAGMA cache_size=${cfg.pragmaCacheSizeKb}")
                st.execute("PRAGMA foreign_keys=ON")
            }
        }
    }


    private fun migrate() {
        connection().use { c ->
            c.createStatement().use { st ->
                st.execute(
                    """
                            CREATE TABLE IF NOT EXISTS homes (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            owner_uuid TEXT NOT NULL,
                            name TEXT NOT NULL,
                            world TEXT NOT NULL,
                            x REAL NOT NULL,
                            y REAL NOT NULL,
                            z REAL NOT NULL,
                            yaw REAL NOT NULL,
                            pitch REAL NOT NULL,
                            created_at INTEGER NOT NULL,
                            updated_at INTEGER NOT NULL,
                            UNIQUE(owner_uuid, name)
                            );
                            """.trimIndent()
                )
                st.execute("CREATE INDEX IF NOT EXISTS idx_homes_owner ON homes(owner_uuid);")
                st.execute("CREATE INDEX IF NOT EXISTS idx_homes_name ON homes(name);")
            }
        }
    }


    fun connection(): Connection = ds.connection


    fun close() = ds.close()
}