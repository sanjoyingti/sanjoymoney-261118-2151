package com.jaiho.sss.in.model

/** The four aimable points. The order here is the order AIM mode walks through them. */
enum class TapPoint { SPIN, JACKPOT, BET_UP, BET_DOWN }

val TAP_POINTS: List<TapPoint> = listOf(TapPoint.SPIN, TapPoint.JACKPOT, TapPoint.BET_UP, TapPoint.BET_DOWN)

fun TapPoint.label(): String = when (this) {
    TapPoint.SPIN -> "SPIN"
    TapPoint.JACKPOT -> "JACKPOT"
    TapPoint.BET_UP -> "BET UP"
    TapPoint.BET_DOWN -> "BET DOWN"
}

fun TapPoint.shortLabel(): String = when (this) {
    TapPoint.SPIN -> "SPIN"
    TapPoint.JACKPOT -> "JP"
    TapPoint.BET_UP -> "BET+"
    TapPoint.BET_DOWN -> "BET-"
}

/** Base delays for the six speeds, in milliseconds. */
val SPEED_INTERVALS: LongArray = longArrayOf(3400L, 2600L, 2000L, 1500L, 1000L, 400L)

fun intervalFor(speed: Int): Long = SPEED_INTERVALS[(speed - 1).coerceIn(0, SPEED_INTERVALS.lastIndex)]

/** Base interval plus or minus 20 percent, so the pattern never looks mechanical. */
fun jitteredInterval(speed: Int): Long {
    val base = intervalFor(speed)
    val factor = 0.8 + Math.random() * 0.4
    return (base * factor).toLong().coerceAtLeast(120L)
}

const val MISSING = -1f

data class BotSettings(
    val spinX: Float = MISSING,
    val spinY: Float = MISSING,
    val jackpotX: Float = MISSING,
    val jackpotY: Float = MISSING,
    val betUpX: Float = MISSING,
    val betUpY: Float = MISSING,
    val betDownX: Float = MISSING,
    val betDownY: Float = MISSING,
    val speed: Int = 3,
    val opacity: Float = 0.9f,
    val panelX: Int = -1,
    val panelY: Int = -1,
    val collapsed: Boolean = false,
    val panelHidden: Boolean = false,
    val alwaysWin: Boolean = false,
    val jackpot: Boolean = false,
    val netHook: Boolean = false,
    val multiplier: Float = 5f,
    val targetPackage: String = "com.jaiho.sss.in"
) {
    /** Normalised 0..1 position of a point, or null when it has not been aimed yet. */
    fun point(kind: TapPoint): Pair<Float, Float>? {
        val (x, y) = when (kind) {
            TapPoint.SPIN -> spinX to spinY
            TapPoint.JACKPOT -> jackpotX to jackpotY
            TapPoint.BET_UP -> betUpX to betUpY
            TapPoint.BET_DOWN -> betDownX to betDownY
        }
        return if (x >= 0f && y >= 0f) x to y else null
    }

    fun has(kind: TapPoint): Boolean = point(kind) != null

    fun withPoint(kind: TapPoint, x: Float, y: Float): BotSettings = when (kind) {
        TapPoint.SPIN -> copy(spinX = x, spinY = y)
        TapPoint.JACKPOT -> copy(jackpotX = x, jackpotY = y)
        TapPoint.BET_UP -> copy(betUpX = x, betUpY = y)
        TapPoint.BET_DOWN -> copy(betDownX = x, betDownY = y)
    }

    fun changingSpeed(delta: Int): BotSettings = copy(speed = (speed + delta).coerceIn(1, 6))

    companion object {
        fun clearedMarkers(from: BotSettings): BotSettings = from.copy(
            spinX = MISSING, spinY = MISSING,
            jackpotX = MISSING, jackpotY = MISSING,
            betUpX = MISSING, betUpY = MISSING,
            betDownX = MISSING, betDownY = MISSING
        )
    }
}

data class SessionStats(
    val spins: Int = 0,
    val streak: Int = 0,
    val misses: Int = 0,
    val startedAt: Long = System.currentTimeMillis()
)

data class NetStats(
    val bytes: Long = 0L,
    val frames: Long = 0L
)
