package com.bits.app.games

import kotlin.random.Random

data class Point(val x: Int, val y: Int)

data class SnakeState(
    val body: List<Point>,
    /** The direction actually in effect until the next step consumes any pending turn. */
    val direction: Direction,
    /**
     * At most one queued turn, waiting for the next tick. Buffering exactly one input
     * this way, rather than mutating [direction] the instant a swipe arrives, is what
     * stops two quick swipes from composing into a reversal the player never saw happen:
     * every turn is always validated against the direction the snake is truly moving in,
     * never against another turn that hasn't been consumed by a tick yet.
     */
    val pendingDirection: Direction?,
    val food: Point,
    val score: Int,
    val dead: Boolean,
) {
    val head: Point get() = body.first()
}

object Snake {
    const val GRID = 12

    fun newGame(random: Random = Random.Default): SnakeState {
        val start = listOf(Point(5, 6), Point(4, 6), Point(3, 6))
        return SnakeState(
            body = start,
            direction = Direction.RIGHT,
            pendingDirection = null,
            food = spawnFood(start, random),
            score = 0,
            dead = false,
        )
    }

    private fun opposite(a: Direction, b: Direction): Boolean = when (a) {
        Direction.UP -> b == Direction.DOWN
        Direction.DOWN -> b == Direction.UP
        Direction.LEFT -> b == Direction.RIGHT
        Direction.RIGHT -> b == Direction.LEFT
    }

    /**
     * Queues a turn for the next tick. Always validated against [SnakeState.direction] —
     * the snake's true, still-in-effect heading — never against a turn that arrived a
     * moment earlier and hasn't been consumed yet. That is what makes two rapid swipes
     * safe: each is judged against where the snake is actually going, so there is no
     * sequence of individually-legal turns that can add up to a reversal.
     */
    fun turn(state: SnakeState, direction: Direction): SnakeState = when {
        state.dead -> state
        opposite(state.direction, direction) -> state
        else -> state.copy(pendingDirection = direction)
    }

    fun step(state: SnakeState, random: Random = Random.Default): SnakeState {
        if (state.dead) return state
        // Exactly one queued turn is adopted per tick, then cleared.
        val direction = state.pendingDirection ?: state.direction
        val head = state.head
        val next = when (direction) {
            Direction.UP -> Point(head.x, head.y - 1)
            Direction.DOWN -> Point(head.x, head.y + 1)
            Direction.LEFT -> Point(head.x - 1, head.y)
            Direction.RIGHT -> Point(head.x + 1, head.y)
        }

        val hitWall = next.x < 0 || next.y < 0 || next.x >= GRID || next.y >= GRID
        // The tail tip moves away this tick, so stepping onto it is allowed.
        val hitSelf = next in state.body.dropLast(1)
        if (hitWall || hitSelf) return state.copy(direction = direction, pendingDirection = null, dead = true)

        val ate = next == state.food
        val body = if (ate) listOf(next) + state.body else listOf(next) + state.body.dropLast(1)
        return state.copy(
            body = body,
            direction = direction,
            pendingDirection = null,
            food = if (ate) spawnFood(body, random) else state.food,
            score = if (ate) state.score + 1 else state.score,
        )
    }

    private fun spawnFood(body: List<Point>, random: Random): Point {
        val free = (0 until GRID).flatMap { y -> (0 until GRID).map { x -> Point(x, y) } }
            .filterNot { it in body }
        if (free.isEmpty()) return body.first()
        return free[random.nextInt(free.size)]
    }
}
