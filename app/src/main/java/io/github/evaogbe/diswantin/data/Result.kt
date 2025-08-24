package io.github.evaogbe.diswantin.data

sealed interface Result<out T> {
    val isSuccess get() = this is Success

    val isFailure get() = this is Failure

    fun getOrNull() = (this as? Success)?.value

    fun <R> map(transform: (T) -> R) = when (this) {
        is Success -> Success(transform(value))
        is Failure -> Failure(exception)
    }

    fun <R> andThen(transform: (T) -> Result<R>) = when (this) {
        is Success -> transform(value)
        is Failure -> Failure(exception)
    }

    fun <R> fold(onSuccess: (T) -> R, onFailure: (Throwable) -> R) = when (this) {
        is Success -> onSuccess(value)
        is Failure -> onFailure(exception)
    }

    fun <R> zip(other: Result<R>) = when (this) {
        is Success -> other.map { value to it }
        is Failure -> Failure(exception)
    }

    fun <T2, R> zipWith(other: Result<T2>, transform: (T, T2) -> R) = when (this) {
        is Success -> other.map { transform(value, it) }
        is Failure -> Failure(exception)
    }

    data class Success<T>(val value: T) : Result<T>

    data class Failure(val exception: Throwable) : Result<Nothing>
}

fun <R, T : R> Result<T>.getOrDefault(default: R) = getOrNull() ?: default
