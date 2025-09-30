package io.github.evaogbe.diswantin.advice

import kotlinx.serialization.Serializable

@Serializable
object AdviceRoute {
    @Serializable
    object BodySensation

    @Serializable
    object Hungry

    @Serializable
    object Tired

    @Serializable
    object Pain

    @Serializable
    object DistressLevel

    @Serializable
    object LowDistress

    @Serializable
    object CheckTheFacts

    @Serializable
    object HighDistress

    @Serializable
    object ExtremeDistress
}
