package io.github.evaogbe.diswantin.data

sealed interface Result<out T> {
    fun getOrNull() = (this as? Success)?.value

    fun <R> map(transform: (T) -> R) = when (this) {
        is Success -> Success(transform(value))
        is Failure -> Failure
    }

    data class Success<T>(val value: T) : Result<T>

    data object Failure : Result<Nothing>
}

fun <R, T : R> Result<T>.getOrDefault(default: R) = getOrNull() ?: default
