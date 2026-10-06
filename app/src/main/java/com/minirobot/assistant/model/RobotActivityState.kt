package com.minirobot.assistant.model

/**
 * Visual and operational states for the Robot companion.
 * Represents all emotional, mechanical, and AI interactive states.
 */
enum class RobotActivityState {
    IDLE,
    WALKING,
    LOOKING_AROUND,
    TAPPED,
    LISTENING,
    THINKING,
    SPEAKING,
    SUCCESS,
    ERROR
}

/**
 * Horizontal orientation the robot is facing.
 */
enum class RobotFacing {
    LEFT,
    RIGHT
}

/**
 * Emotional mood cues.
 */
enum class RobotMood {
    HAPPY,
    CURIOUS,
    PLAYFUL,
    THINKING,
    PROUD,
    CONCERNED
}
