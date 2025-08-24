package io.github.evaogbe.diswantin.testing

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import io.github.evaogbe.diswantin.task.data.EditTagForm
import io.github.evaogbe.diswantin.task.data.NewTagForm
import io.github.evaogbe.diswantin.task.data.Tag
import io.github.evaogbe.diswantin.task.data.TagRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class FakeTagRepository(private val db: FakeDatabase = FakeDatabase()) :
    TagRepository {
    val tags
        get() = db.tagTable.value.values

    val taskTags
        get() = db.taskTagTable.value.values

    override val tagPagingData = db.tagTable.map {
        PagingData.from(
            it.values.sortedBy(Tag::name),
            LoadStates(
                refresh = LoadState.NotLoading(endOfPaginationReached = true),
                prepend = LoadState.NotLoading(endOfPaginationReached = true),
                append = LoadState.NotLoading(endOfPaginationReached = true),
            ),
        )
    }

    override val hasTagsStream = db.tagTable.map { it.isNotEmpty() }

    override fun getById(id: Long) = db.tagTable.map { it[id] }

    override fun getTagsByTaskId(taskId: Long) =
        combine(db.tagTable, db.taskTagTable) { tags, taskTags ->
            taskTags.values.filter { it.taskId == taskId }.mapNotNull { tags[it.tagId] }
        }

    override fun search(query: String, size: Int) = db.tagTable.map { tags ->
        tags.values.filter { it.name.contains(query, ignoreCase = true) }.take(size)
    }

    override suspend fun create(form: NewTagForm) {
        db.insertTag(form.newTag, form.newTaskIds)
    }

    override suspend fun update(form: EditTagForm) {
        db.updateTag(
            tag = form.updatedTag,
            taskIdsToInsert = form.taskIdsToInsert,
            taskIdsToRemove = form.taskIdsToRemove,
        )
    }

    override suspend fun delete(tag: Tag) {
        db.deleteTag(tag.id)
    }
}
