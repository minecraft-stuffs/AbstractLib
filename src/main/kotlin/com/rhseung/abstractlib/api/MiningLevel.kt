package com.rhseung.abstractlib.api

data class MiningLevel(
    private val level: Int
) {

    fun toInt(): Int = level

    companion object {
        val WOOD = MiningLevel(0)
        val STONE = MiningLevel(1)
        val IRON = MiningLevel(2)
        val DIAMOND = MiningLevel(3)
        val NETHERITE = MiningLevel(4)
    }

    operator fun compareTo(other: MiningLevel): Int {
        return level.compareTo(other.level)
    }
}