package com.bits.app.games

import kotlin.random.Random

data class Point(val x: Int, val y: Int)

data class SnakeState(
    val body: List<Point>,
    val direction: Direction,
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

    /** Ignores reversals into the snake's own neck, which would be an instant self-collision. */
    fun turn(state: SnakeState, direction: Direction): SnakeState =
        if (opposite(state.direction, direction)) state else state.copy(direction = direction)

    fun step(state: SnakeState, random: Random = Random.Default): SnakeState {
        if (state.dead) return state
        val head = state.head
        val next = when (state.direction) {
            Direction.UP -> Point(head.x, head.y - 1)
            Direction.DOWN -> Point(head.x, head.y + 1)
            Direction.LEFT -> Point(head.x - 1, head.y)
            Direction.RIGHT -> Point(head.x + 1, head.y)
        }

        val hitWall = next.x < 0 || next.y < 0 || next.x >= GRID || next.y >= GRID
        // The tail tip moves away this tick, so stepping onto it is allowed.
        val hitSelf = next in state.body.dropLast(1)
        if (hitWall || hitSelf) return state.copy(dead = true)

        val ate = next == state.food
        val body = if (ate) listOf(next) + state.body else listOf(next) + state.body.dropLast(1)
        return state.copy(
            body = body,
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
