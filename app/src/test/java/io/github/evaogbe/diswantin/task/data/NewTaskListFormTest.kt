package io.github.evaogbe.diswantin.task.data

import assertk.assertThat
import assertk.assertions.containsExactly
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import org.junit.Test

class NewTaskListFormTest {
    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun `taskPaths returns the paths connecting the whole list`() {
        val form = NewTaskListForm(name = loremFaker.lorem.words(), tasks = List(3) {
            Task(
                id = it + 1L,
                createdAt = faker.random.randomPastDate().toInstant(),
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            )
        })

        assertThat(form.taskPaths).containsExactly(
            TaskPath(ancestor = 1L, descendant = 2L, depth = 1),
            TaskPath(ancestor = 1L, descendant = 3L, depth = 2),
            TaskPath(ancestor = 2L, descendant = 3L, depth = 1),
        )
    }
}
