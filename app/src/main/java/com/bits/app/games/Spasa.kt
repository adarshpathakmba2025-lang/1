package com.bits.app.games

import kotlin.math.abs
import kotlin.random.Random

/**
 * Spasa: a formation of drones sways above you, peels off to dive, and you shoot it down.
 *
 * The rules of a fixed shooter are free for anyone to use; the arcade classic's insect
 * sprites, its name and its exact wave choreography are not, so none of them appear
 * here. The attackers are plain blocks in Bits' own palette, there is no capture-and-
 * rescue trick, and the guns fire themselves so steering is the whole job.
 *
 * Everything lives in a unitless 0..1 by 0..1 play area, so the same numbers work on any
 * screen, which is the same approach the Flappy game takes.
 */

/** A single shot, travelling straight up (player) or straight down (drone). */
data class SpasaShot(val x: Float, val y: Float)

data class SpasaDrone(
    val id: Int,
    val col: Int,
    val row: Int,
    /** 0, 1 or 2. Decides the colour, the points, and how eagerly it dives. */
    val kind: Int,
    val diving: Boolean,
    /** Only meaningful while diving; in formation the position is worked out from col/row. */
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
)

data class SpasaState(
    val shipX: Float,
    val drones: List<SpasaDrone>,
    val shots: List<SpasaShot>,
    val bombs: List<SpasaShot>,
    val swayX: Float,
    val swayDir: Int,
    val wave: Int,
    val lives: Int,
    val score: Int,
    /** Ticks until the next automatic shot. */
    val reload: Int,
    /** Ticks of grace after being hit, during which nothing can hurt the ship. */
    val grace: Int,
    val nextId: Int,
    val dead: Boolean,
    val started: Boolean,
)

object Spasa {

    const val COLS = 6
    const val ROWS = 3

    const val SHIP_Y = 0.90f
    const val SHIP_W = 0.085f
    const val SHIP_H = 0.055f
    const val DRONE_W = 0.085f
    const val DRONE_H = 0.060f
    const val SHOT_W = 0.014f
    const val SHOT_H = 0.045f

    private const val COL_GAP = 0.135f
    private const val ROW_GAP = 0.105f
    private const val ROW_TOP = 0.10f
    private const val SWAY_LIMIT = 0.085f
    private const val SWAY_SPEED = 0.0022f

    private const val SHOT_SPEED = 0.026f
    private const val BOMB_SPEED = 0.011f
    private const val RELOAD_TICKS = 16
    private const val GRACE_TICKS = 70

    private const val STARTING_LIVES = 3
    const val MAX_LIVES = 5

    /** Points per drone, by kind. The ones that dive hardest are worth the most. */
    private val KIND_SCORE = intArrayOf(50, 80, 120)

    /** How eagerly each kind leaves the formation, as a relative weight. */
    private val KIND_DIVE = floatArrayOf(1.0f, 1.4f, 1.9f)

    fun newGame(): SpasaState {
        val drones = formation(wave = 1, startId = 0)
        return SpasaState(
            shipX = 0.5f,
            drones = drones,
            shots = emptyList(),
            bombs = emptyList(),
            swayX = 0f,
            swayDir = 1,
            wave = 1,
            lives = STARTING_LIVES,
            score = 0,
            reload = RELOAD_TICKS,
            grace = 0,
            nextId = drones.size,
            dead = false,
            started = false,
        )
    }

    /** Builds a full formation. Later waves put the tougher kinds nearer the front. */
    private fun formation(wave: Int, startId: Int): List<SpasaDrone> {
        var id = startId
        return buildList {
            for (row in 0 until ROWS) {
                for (col in 0 until COLS) {
                    // Top rows hold the eager ones; a later wave promotes the rest.
                    val kind = when {
                        row == 0 -> 2
                        row == 1 -> if (wave >= 3) 2 else 1
                        else -> if (wave >= 5) 1 else 0
                    }
                    add(
                        SpasaDrone(
                            id = id++,
                            col = col,
                            row = row,
                            kind = kind,
                            diving = false,
                            x = 0f,
                            y = 0f,
                            vx = 0f,
                            vy = 0f,
                        )
                    )
                }
            }
        }
    }

    /** Where a drone sits. Divers carry their own position; the rest follow the sway. */
    fun positionOf(state: SpasaState, drone: SpasaDrone): Pair<Float, Float> =
        if (drone.diving) {
            drone.x to drone.y
        } else {
            val left = (1f - (COLS - 1) * COL_GAP) / 2f
            (left + drone.col * COL_GAP + state.swayX) to (ROW_TOP + drone.row * ROW_GAP)
        }

    fun steer(state: SpasaState, x: Float): SpasaState =
        if (state.dead) state
        else state.copy(shipX = x.coerceIn(SHIP_W / 2f, 1f - SHIP_W / 2f), started = true)

    fun nudge(state: SpasaState, dx: Float): SpasaState = steer(state, state.shipX + dx)

    fun start(state: SpasaState): SpasaState = if (state.dead) state else state.copy(started = true)

    fun step(state: SpasaState, random: Random = Random.Default): SpasaState {
        if (state.dead || !state.started) return state

        // --- the formation drifts side to side, reversing at its limits ---
        var swayDir = state.swayDir
        var swayX = state.swayX + swayDir * SWAY_SPEED
        if (swayX > SWAY_LIMIT) {
            swayX = SWAY_LIMIT
            swayDir = -1
        } else if (swayX < -SWAY_LIMIT) {
            swayX = -SWAY_LIMIT
            swayDir = 1
        }
        val swayed = state.copy(swayX = swayX, swayDir = swayDir)

        // --- divers travel; anyone who leaves the bottom rejoins the formation ---
        val drones = swayed.drones.map { drone ->
            if (!drone.diving) return@map drone
            val x = drone.x + drone.vx
            val y = drone.y + drone.vy
            // A diver that runs off the side is turned back rather than lost.
            val vx = if (x < DRONE_W / 2f || x > 1f - DRONE_W / 2f) -drone.vx else drone.vx
            if (y > 1f + DRONE_H) {
                drone.copy(diving = false, x = 0f, y = 0f, vx = 0f, vy = 0f)
            } else {
                drone.copy(x = x.coerceIn(DRONE_W / 2f, 1f - DRONE_W / 2f), y = y, vx = vx)
            }
        }

        // --- now and then one peels off and comes for the ship ---
        val diveChance = 0.008f + swayed.wave * 0.0022f
        val candidates = drones.filter { !it.diving }
        val launched = if (candidates.isNotEmpty() && random.nextFloat() < diveChance) {
            val pick = candidates[random.nextInt(candidates.size)]
            // Weighted so the tougher kinds come down more often.
            if (random.nextFloat() < KIND_DIVE[pick.kind] / (KIND_DIVE.maxOrNull() ?: 1f)) {
                val (px, py) = positionOf(swayed, pick)
                val toward = (swayed.shipX - px).coerceIn(-1f, 1f)
                drones.map {
                    if (it.id == pick.id) {
                        it.copy(
                            diving = true,
                            x = px,
                            y = py,
                            vx = toward * 0.0085f,
                            vy = 0.0075f + pick.kind * 0.0016f + swayed.wave * 0.0004f,
                        )
                    } else {
                        it
                    }
                }
            } else {
                drones
            }
        } else {
            drones
        }

        // --- drones drop bombs; divers are keener than the formation ---
        val newBombs = buildList {
            launched.forEach { drone ->
                val chance = if (drone.diving) 0.010f else 0.0013f
                if (random.nextFloat() < chance) {
                    val (dx, dy) = positionOf(swayed, drone)
                    add(SpasaShot(dx, dy + DRONE_H / 2f))
                }
            }
        }

        // --- the ship's guns run themselves, so steering is the only job ---
        var reload = swayed.reload - 1
        val firedShots = if (reload <= 0) {
            reload = RELOAD_TICKS
            swayed.shots + SpasaShot(swayed.shipX, SHIP_Y - SHIP_H / 2f)
        } else {
            swayed.shots
        }

        val shots = firedShots.map { it.copy(y = it.y - SHOT_SPEED) }.filter { it.y > -SHOT_H }
        val bombs = (swayed.bombs + newBombs).map { it.copy(y = it.y + BOMB_SPEED) }
            .filter { it.y < 1f + SHOT_H }

        // --- shots meeting drones ---
        val hitShots = mutableSetOf<Int>()
        val downed = mutableSetOf<Int>()
        var gained = 0
        shots.forEachIndexed { shotIndex, shot ->
            if (shotIndex in hitShots) return@forEachIndexed
            launched.forEach { drone ->
                if (drone.id in downed) return@forEach
                val (dx, dy) = positionOf(swayed, drone)
                if (overlaps(shot.x, shot.y, SHOT_W, SHOT_H, dx, dy, DRONE_W, DRONE_H)) {
                    hitShots += shotIndex
                    downed += drone.id
                    gained += KIND_SCORE[drone.kind] * if (drone.diving) 2 else 1
                    return@forEachIndexed
                }
            }
        }
        val survivingShots = shots.filterIndexed { index, _ -> index !in hitShots }
        val survivingDrones = launched.filterNot { it.id in downed }

        // --- anything reaching the ship ---
        val grace = (swayed.grace - 1).coerceAtLeast(0)
        val bombHit = grace == 0 && bombs.any {
            overlaps(it.x, it.y, SHOT_W, SHOT_H, swayed.shipX, SHIP_Y, SHIP_W, SHIP_H)
        }
        val ramHit = grace == 0 && survivingDrones.any { drone ->
            if (!drone.diving) return@any false
            overlaps(drone.x, drone.y, DRONE_W, DRONE_H, swayed.shipX, SHIP_Y, SHIP_W, SHIP_H)
        }
        val struck = bombHit || ramHit

        if (struck) {
            val lives = swayed.lives - 1
            return swayed.copy(
                drones = survivingDrones.map {
                    // Everyone returns to the formation, so a fresh life starts calmly.
                    if (it.diving) it.copy(diving = false, x = 0f, y = 0f, vx = 0f, vy = 0f) else it
                },
                shots = emptyList(),
                bombs = emptyList(),
                shipX = 0.5f,
                score = swayed.score + gained,
                lives = lives.coerceAtLeast(0),
                reload = RELOAD_TICKS,
                grace = GRACE_TICKS,
                dead = lives <= 0,
            )
        }

        // --- a cleared sky brings the next, harder wave ---
        if (survivingDrones.isEmpty()) {
            val wave = swayed.wave + 1
            val fresh = formation(wave, swayed.nextId)
            return swayed.copy(
                drones = fresh,
                shots = emptyList(),
                bombs = emptyList(),
                swayX = 0f,
                swayDir = 1,
                wave = wave,
                // A cleared wave is worth a bonus, and occasionally a spare life.
                score = swayed.score + gained + 250 * swayed.wave,
                lives = if (wave % 3 == 0) (swayed.lives + 1).coerceAtMost(MAX_LIVES) else swayed.lives,
                reload = RELOAD_TICKS,
                grace = GRACE_TICKS,
                nextId = swayed.nextId + fresh.size,
            )
        }

        return swayed.copy(
            drones = survivingDrones,
            shots = survivingShots,
            bombs = bombs,
            score = swayed.score + gained,
            reload = reload,
            grace = grace,
        )
    }

    /** Centre-to-centre box overlap, which is how every position here is expressed. */
    private fun overlaps(
        ax: Float, ay: Float, aw: Float, ah: Float,
        bx: Float, by: Float, bw: Float, bh: Float,
    ): Boolean = abs(ax - bx) * 2f < (aw + bw) && abs(ay - by) * 2f < (ah + bh)
}
