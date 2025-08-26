package com.github.saintedlittle.model


import java.util.*


data class Home(
    val id: Long? = null,
    val owner: UUID,
    val name: String,
    val world: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float,
    val createdAt: Long,
    val updatedAt: Long,
)