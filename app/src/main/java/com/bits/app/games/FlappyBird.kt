package com.bits.app.games

import kotlin.random.Random

// The gap size is fixed the moment a pipe is spawned, and travels with it for its whole
// life. Difficulty still ramps over time, but only for pipes created after the ramp,
// never for one already on screen — otherwise a pipe's safe zone could shrink while a
// player is already committed to flying through it.
data class Pipe(val x: Float, val gapTop: Float, val gap: Float)

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
        //
        // Tuned for control rather than twitchiness: a flap is a small nudge, gravity is
        // gentle, and upward speed is capped so a tap can't fling the bird off the top.
        // Terminal velocity keeps the fall readable, and slower pipes with a wider gap
        // leave room to correct mid-flight.
        const val GRAVITY = 0.00085f
        const val FLAP = -0.0150f
        const val MAX_RISE = -0.0150f
        const val MAX_FALL = 0.0180f
        const val PIPE_SPEED = 0.0042f
        const val GAP = 0.38f
        const val BIRD_X = 0.26f
        const val BIRD_SIZE = 0.050f
        const val PIPE_WIDTH = 0.14f
        const val PIPE_SPACING = 0.62f
    }
}

object FlappyBird {

    /**
     * Difficulty by score. It stays gentle to 20, tightens through the twenties and
     * thirties, then tightens again past 47 while deliberately holding a floor on the gap
     * and a ceiling on speed, so a very high score stays hard rather than impossible.
     */
    fun gapFor(score: Int): Float = when {
        score < 20 -> FlappyState.GAP
        score < 47 -> (FlappyState.GAP - (score - 20) * 0.0028f).coerceAtLeast(0.305f)
        else -> (0.305f - (score - 47) * 0.0016f).coerceAtLeast(0.255f)
    }

    fun speedFor(score: Int): Float = when {
        score < 20 -> FlappyState.PIPE_SPEED
        score < 47 -> (FlappyState.PIPE_SPEED + (score - 20) * 0.00006f).coerceAtMost(0.0058f)
        else -> (0.0058f + (score - 47) * 0.00004f).coerceAtMost(0.0072f)
    }

    fun spacingFor(score: Int): Float = when {
        score < 20 -> FlappyState.PIPE_SPACING
        score < 47 -> (FlappyState.PIPE_SPACING - (score - 20) * 0.004f).coerceAtLeast(0.52f)
        else -> (0.52f - (score - 47) * 0.002f).coerceAtLeast(0.45f)
    }

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
        // Each tap sets a fixed, modest rise rather than adding to whatever speed
        // the bird already had, so repeated taps can't compound into a rocket.
        else state.copy(velocity = FlappyState.FLAP, started = true)

    fun step(state: FlappyState, random: Random = Random.Default): FlappyState {
        if (state.dead || !state.started) return state

        val velocity = (state.velocity + FlappyState.GRAVITY)
            .coerceIn(FlappyState.MAX_RISE, FlappyState.MAX_FALL)
        val birdY = state.birdY + velocity

        // Every existing pipe simply translates left at the current speed; that's uniform
        // across the whole list, so it never disturbs the spacing or gap any pipe was
        // actually spawned with.
        val moved = state.pipes.map { it.copy(x = it.x - speedFor(state.score)) }
            .filter { it.x + FlappyState.PIPE_WIDTH > -0.05f }
        val needsPipe = moved.isEmpty() || moved.maxOf { it.x } < spacingFor(state.score)
        val pipes = if (needsPipe) {
            // The gap for a brand new pipe is decided once, right now, and baked in.
            val newGap = gapFor(state.score)
            moved + Pipe(x = 1.05f, gapTop = 0.10f + random.nextFloat() * (0.90f - newGap - 0.10f), gap = newGap)
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
            // Uses this pipe's own gap, fixed at the moment it spawned, never today's score.
            val insideGap = birdY > pipe.gapTop && birdY + FlappyState.BIRD_SIZE < pipe.gapTop + pipe.gap
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
