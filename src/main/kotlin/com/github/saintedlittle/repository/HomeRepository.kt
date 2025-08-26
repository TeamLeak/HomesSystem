package com.github.saintedlittle.repository


import com.github.saintedlittle.db.Database
import com.github.saintedlittle.model.Home
import java.sql.ResultSet
import java.util.*

class HomeRepository(private val db: Database) {


    fun upsert(home: Home): Pair<Home, Boolean /* created? */> {
        val now = System.currentTimeMillis()
        db.connection().use { c ->
// Try insert, on conflict update
            c.prepareStatement(
                """
                    INSERT INTO homes(owner_uuid, name, world, x, y, z, yaw, pitch, created_at, updated_at)
                    VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(owner_uuid, name) DO UPDATE SET
                    world=excluded.world, x=excluded.x, y=excluded.y, z=excluded.z,
                    yaw=excluded.yaw, pitch=excluded.pitch, updated_at=excluded.updated_at
                    """.trimIndent()
            ).use { ps ->
                ps.setString(1, home.owner.toString())
                ps.setString(2, home.name)
                ps.setString(3, home.world)
                ps.setDouble(4, home.x)
                ps.setDouble(5, home.y)
                ps.setDouble(6, home.z)
                ps.setFloat(7, home.yaw)
                ps.setFloat(8, home.pitch)
                ps.setLong(9, home.createdAt)
                ps.setLong(10, now)
                val affected = ps.executeUpdate()
                val created = affected == 1 // insert path returns 1 row
                val saved = find(home.owner, home.name)!!
                return saved to created
            }
        }
    }


    fun find(owner: UUID, name: String): Home? {
        db.connection().use { c ->
            c.prepareStatement(
                "SELECT * FROM homes WHERE owner_uuid=? AND name=? LIMIT 1"
            ).use { ps ->
                ps.setString(1, owner.toString())
                ps.setString(2, name)
                ps.executeQuery().use { rs ->
                    return if (rs.next()) map(rs) else null
                }
            }
        }
    }


    fun list(owner: UUID): List<Home> {
        db.connection().use { c ->
            c.prepareStatement("SELECT * FROM homes WHERE owner_uuid=? ORDER BY name ASC").use { ps ->
                ps.setString(1, owner.toString())
                ps.executeQuery().use { rs ->
                    val out = mutableListOf<Home>()
                    while (rs.next()) out += map(rs)
                    return out
                }
            }
        }
    }


    fun count(owner: UUID): Int {
        db.connection().use { c ->
            c.prepareStatement("SELECT COUNT(*) FROM homes WHERE owner_uuid=?").use { ps ->
                ps.setString(1, owner.toString())
                ps.executeQuery().use { rs ->
                    return if (rs.next()) rs.getInt(1) else 0
                }
            }
        }
    }


    fun delete(owner: UUID, name: String): Boolean {
        db.connection().use { c ->
            c.prepareStatement("DELETE FROM homes WHERE owner_uuid=? AND name=?").use { ps ->
                ps.setString(1, owner.toString())
                ps.setString(2, name)
                return ps.executeUpdate() > 0
            }
        }
    }


    private fun map(rs: ResultSet): Home = Home(
        id = rs.getLong("id"),
        owner = UUID.fromString(rs.getString("owner_uuid")),
        name = rs.getString("name"),
        world = rs.getString("world"),
        x = rs.getDouble("x"),
        y = rs.getDouble("y"),
        z = rs.getDouble("z"),
        yaw = rs.getFloat("yaw"),
        pitch = rs.getFloat("pitch"),
        createdAt = rs.getLong("created_at"),
        updatedAt = rs.getLong("updated_at")
    )
}