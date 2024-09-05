package io.github.evaogbe.diswantin.data

sealed interface Result<out T> {
    fun getOrNull() = (this as? Success)?.value

    data class Success<T>(val value: T) : Result<T>

    data object Failure : Result<Nothing>
}
