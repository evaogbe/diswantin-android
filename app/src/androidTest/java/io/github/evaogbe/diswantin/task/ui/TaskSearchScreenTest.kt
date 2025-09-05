package io.github.evaogbe.diswantin.task.ui

import androidx.compose.runtime.getValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import assertk.assertThat
import assertk.assertions.isNull
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.task.data.TaskSearchCriteria
import io.github.evaogbe.diswantin.task.data.TaskSummary
import io.github.evaogbe.diswantin.testing.FakeDatabase
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.stringResource
import io.github.evaogbe.diswantin.ui.loadstate.PendingLayoutTestTag
import io.github.evaogbe.diswantin.ui.snackbar.SnackbarState
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import io.mockk.every
import io.mockk.spyk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import java.time.Clock

@OptIn(ExperimentalTestApi::class)
class TaskSearchScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun displaysSearchResults_withSearchResults() {
        val query = loremFaker.verbs.base()
        val queryFlow = MutableStateFlow(query)
        val tasks = List(3) {
            val createdAt = faker.random.randomPastDate().toInstant()
            Task(
                id = it + 1L,
                createdAt = createdAt,
                name = "$query ${loremFaker.lorem.unique.words()}",
                updatedAt = createdAt,
            )
        }
        val viewModel = createTaskSearchViewModel({ db ->
            tasks.forEach(db::insertTask)
        })

        composeTestRule.setContent {
            val queryState by queryFlow.collectAsStateWithLifecycle()

            DiswantinTheme {
                TaskSearchScreen(
                    query = queryState,
                    topBarAction = null,
                    topBarActionHandled = {},
                    showSnackbar = {},
                    onAddTask = {},
                    onSelectSearchResult = {},
                    taskSearchViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(tasks[0].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(tasks[1].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(tasks[2].name).assertIsDisplayed()

        queryFlow.value = tasks[0].name

        composeTestRule.waitUntilDoesNotExist(hasText(tasks[1].name))
        composeTestRule.onNodeWithText(tasks[0].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(tasks[1].name).assertDoesNotExist()
    }

    @Test
    fun displaysErrorMessage_whenReloadFails() {
        var snackbarState: SnackbarState? = null
        val query = loremFaker.verbs.base()
        val queryFlow = MutableStateFlow(query)
        val tasks = List(3) {
            val createdAt = faker.random.randomPastDate().toInstant()
            Task(
                id = it + 1L,
                createdAt = createdAt,
                name = "$query ${loremFaker.lorem.unique.words()}",
                updatedAt = createdAt,
            )
        }
        val viewModel = createTaskSearchViewModel(
            initDatabase = { db -> tasks.forEach(db::insertTask) },
            initTaskRepositorySpy = { repository ->
                every {
                    repository.searchTaskSummaries(TaskSearchCriteria(query))
                } returns flowOf(
                    PagingData.from(
                        tasks.map { it.toTaskSummary() },
                        LoadStates(
                            refresh = LoadState.NotLoading(endOfPaginationReached = true),
                            prepend = LoadState.NotLoading(endOfPaginationReached = true),
                            append = LoadState.NotLoading(endOfPaginationReached = true),
                        ),
                    )
                )
                every {
                    repository.searchTaskSummaries(TaskSearchCriteria(tasks[0].name))
                } returns flowOf(
                    PagingData.from(
                        listOf(tasks[0].toTaskSummary()),
                        LoadStates(
                            refresh = LoadState.Error(RuntimeException("Test")),
                            prepend = LoadState.NotLoading(endOfPaginationReached = false),
                            append = LoadState.NotLoading(endOfPaginationReached = false),
                        ),
                    )
                )
            },
        )

        composeTestRule.setContent {
            val queryState by queryFlow.collectAsStateWithLifecycle()

            DiswantinTheme {
                TaskSearchScreen(
                    query = queryState,
                    topBarAction = TaskSearchTopBarAction.Search,
                    topBarActionHandled = {},
                    showSnackbar = { snackbarState = it },
                    onAddTask = {},
                    onSelectSearchResult = {},
                    taskSearchViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(tasks[0].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(tasks[1].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(tasks[2].name).assertIsDisplayed()

        assertThat(snackbarState).isNull()

        queryFlow.value = tasks[0].name

        composeTestRule.waitUntilDoesNotExist(hasText(tasks[1].name))
        composeTestRule.onNodeWithText(tasks[0].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(tasks[1].name).assertDoesNotExist()

        composeTestRule.waitUntil {
            snackbarState?.matches(
                message = stringResource(R.string.task_search_error),
                actionLabel = stringResource(R.string.retry_button),
            ) == true
        }
    }


    @Test
    fun displaysEmptyMessage_withoutSearchResults() {
        val query = loremFaker.verbs.unique.base()
        val queryFlow = MutableStateFlow(query)
        val viewModel = createTaskSearchViewModel()

        composeTestRule.setContent {
            val queryState by queryFlow.collectAsStateWithLifecycle()

            DiswantinTheme {
                TaskSearchScreen(
                    query = queryState,
                    topBarAction = null,
                    topBarActionHandled = {},
                    showSnackbar = {},
                    onAddTask = {},
                    onSelectSearchResult = {},
                    taskSearchViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.task_search_empty))
            .assertIsDisplayed()

        queryFlow.value = loremFaker.verbs.unique.base()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(stringResource(R.string.task_search_empty))
            .assertIsDisplayed()
    }

    @Test
    fun displaysLoadingSpinner_whenLoadingInitial() {
        val query = loremFaker.verbs.base()
        val viewModel = createTaskSearchViewModel(
            initDatabase = {},
            initTaskRepositorySpy = { repository ->
                every { repository.searchTaskSummaries(any()) } returns flowOf(
                    PagingData.from(
                        emptyList(),
                        LoadStates(
                            refresh = LoadState.Loading,
                            prepend = LoadState.NotLoading(endOfPaginationReached = false),
                            append = LoadState.NotLoading(endOfPaginationReached = false),
                        ),
                    )
                )
            },
        )

        composeTestRule.setContent {
            DiswantinTheme {
                TaskSearchScreen(
                    query = query,
                    topBarAction = TaskSearchTopBarAction.Search,
                    topBarActionHandled = {},
                    showSnackbar = {},
                    onAddTask = {},
                    onSelectSearchResult = {},
                    taskSearchViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithTag(PendingLayoutTestTag).assertIsDisplayed()
    }

    @Test
    fun displaysInitialLayout_withoutSearchCriteria() {
        val viewModel = createTaskSearchViewModel()

        composeTestRule.setContent {
            DiswantinTheme {
                TaskSearchScreen(
                    query = "",
                    topBarAction = TaskSearchTopBarAction.Search,
                    topBarActionHandled = {},
                    showSnackbar = {},
                    onAddTask = {},
                    onSelectSearchResult = {},
                    taskSearchViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithTag(InitialTaskSearchLayoutTestTag).assertIsDisplayed()
    }

    @Test
    fun displayErrorMessage_whenInitialLoadFails() {
        val query = loremFaker.verbs.base()
        val viewModel = createTaskSearchViewModel(
            initDatabase = {},
            initTaskRepositorySpy = { repository ->
                every { repository.searchTaskSummaries(any()) } returns flowOf(
                    PagingData.from(
                        emptyList(),
                        LoadStates(
                            refresh = LoadState.Error(RuntimeException("Test")),
                            prepend = LoadState.NotLoading(endOfPaginationReached = false),
                            append = LoadState.NotLoading(endOfPaginationReached = false),
                        ),
                    )
                )
            },
        )

        composeTestRule.setContent {
            DiswantinTheme {
                TaskSearchScreen(
                    query = query,
                    topBarAction = TaskSearchTopBarAction.Search,
                    topBarActionHandled = {},
                    showSnackbar = {},
                    onAddTask = {},
                    onSelectSearchResult = {},
                    taskSearchViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.task_search_error))
            .assertIsDisplayed()
    }

    private fun Task.toTaskSummary() =
        TaskSummary(id = id, name = name, recurring = false, doneAt = null)

    private fun createClock() = Clock.systemDefaultZone()

    private fun createTaskSearchViewModel(
        initDatabase: (FakeDatabase) -> Unit = {},
        initTaskRepositorySpy: ((TaskRepository) -> Unit)? = null,
    ): TaskSearchViewModel {
        val clock = createClock()
        val db = FakeDatabase().also(initDatabase)
        val taskRepository = if (initTaskRepositorySpy == null) {
            FakeTaskRepository(db, clock)
        } else {
            spyk(FakeTaskRepository(db, clock)).also(initTaskRepositorySpy)
        }
        return TaskSearchViewModel(SavedStateHandle(), taskRepository, clock)
    }
}
