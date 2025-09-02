package io.github.evaogbe.diswantin.advice

import io.github.evaogbe.diswantin.ui.navigation.BottomBarRoute
import kotlinx.serialization.Serializable

@Serializable
object AdviceRoute : BottomBarRoute {
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
