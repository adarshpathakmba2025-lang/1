package com.bits.app.games

import kotlin.random.Random

data class Pipe(val x: Float, val gapTop: Float)

data class FlappyState(
    val birdY: Float,
    val velocity: Float,
    val pipes: List<Pipe>,
    val score: Int,
    val dead: Boolean,
    val started: Boolean,
) {
    companion object {
        // A unitless 0..1 play area, so the same physics work at any screen size.
        const val GRAVITY = 0.0016f
        const val FLAP = -0.028f
        const val PIPE_SPEED = 0.006f
        const val GAP = 0.30f
        const val BIRD_X = 0.28f
        const val BIRD_SIZE = 0.055f
        const val PIPE_WIDTH = 0.16f
    }
}

object FlappyBird {
    fun newGame(): FlappyState = FlappyState(
        birdY = 0.45f,
        velocity = 0f,
        pipes = emptyList(),
        score = 0,
        dead = false,
        started = false,
    )

    fun flap(state: FlappyState): FlappyState =
        if (state.dead) state
        else state.copy(velocity = FlappyState.FLAP, started = true)

    fun step(state: FlappyState, random: Random = Random.Default): FlappyState {
        if (state.dead || !state.started) return state

        val velocity = state.velocity + FlappyState.GRAVITY
        val birdY = state.birdY + velocity

        val moved = state.pipes.map { it.copy(x = it.x - FlappyState.PIPE_SPEED) }
            .filter { it.x + FlappyState.PIPE_WIDTH > -0.05f }
        val needsPipe = moved.isEmpty() || moved.maxOf { it.x } < 0.55f
        val pipes = if (needsPipe) {
            moved + Pipe(x = 1.05f, gapTop = 0.12f + random.nextFloat() * (0.88f - FlappyState.GAP - 0.12f))
        } else {
            moved
        }

        // A pipe counts as passed once its right edge is fully behind the bird.
        val passed = pipes.count { it.x + FlappyState.PIPE_WIDTH < FlappyState.BIRD_X }
        val previouslyPassed = state.pipes.count { it.x + FlappyState.PIPE_WIDTH < FlappyState.BIRD_X }
        val score = state.score + (passed - previouslyPassed).coerceAtLeast(0)

        val hitGround = birdY + FlappyState.BIRD_SIZE >= 1f || birdY <= 0f
        val hitPipe = pipes.any { pipe ->
            val overlapsX = FlappyState.BIRD_X + FlappyState.BIRD_SIZE > pipe.x &&
                FlappyState.BIRD_X < pipe.x + FlappyState.PIPE_WIDTH
            val insideGap = birdY > pipe.gapTop && birdY + FlappyState.BIRD_SIZE < pipe.gapTop + FlappyState.GAP
            overlapsX && !insideGap
        }

        return state.copy(
            birdY = birdY.coerceIn(0f, 1f),
            velocity = velocity,
            pipes = pipes,
            score = score,
            dead = hitGround || hitPipe,
        )
    }
}
