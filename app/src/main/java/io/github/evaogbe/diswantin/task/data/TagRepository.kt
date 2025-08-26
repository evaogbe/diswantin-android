package io.github.evaogbe.diswantin.task.data

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    val tagPagingData: Flow<PagingData<Tag>>

    val hasTagsStream: Flow<Boolean>

    fun getById(id: Long): Flow<Tag?>

    fun getTagsByTaskId(taskId: Long, size: Int): Flow<List<Tag>>

    fun search(query: String, size: Int): Flow<List<Tag>>

    suspend fun create(form: NewTagForm)

    suspend fun update(form: EditTagForm)

    suspend fun delete(tag: Tag)
}
