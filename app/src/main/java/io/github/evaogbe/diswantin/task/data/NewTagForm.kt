package io.github.evaogbe.diswantin.task.data

data class NewTagForm(private val name: String) {
    init {
        require(name.isNotBlank()) { "Name must be present" }
    }

    val newTag = Tag(name = name.trim())
}
