package io.github.evaogbe.diswantin.task.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import io.github.evaogbe.diswantin.data.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LocalTagRepository @Inject constructor(
    private val tagDao: TagDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TagRepository {
    override val tagPagingData = Pager(PagingConfig(pageSize = 20)) {
        tagDao.getTagPagingSource()
    }.flow

    override val hasTagsStream = tagDao.hasTags().flowOn(ioDispatcher)

    override fun getById(id: Long) = tagDao.getById(id).flowOn(ioDispatcher)

    override fun getTagsByTaskId(taskId: Long, size: Int) =
        tagDao.getTagsByTaskId(taskId, size).flowOn(ioDispatcher)

    override fun search(query: String, size: Int) =
        tagDao.search(escapeSql("$query*"), size).flowOn(ioDispatcher)

    private fun escapeSql(str: String) = str.replace("'", "''").replace("\"", "\"\"")

    override suspend fun create(form: NewTagForm) {
        withContext(ioDispatcher) {
            tagDao.insert(form.newTag)
        }
    }

    override suspend fun update(form: EditTagForm) {
        withContext(ioDispatcher) {
            tagDao.update(form.updatedTag)
        }
    }

    override suspend fun delete(tag: Tag) {
        withContext(ioDispatcher) {
            tagDao.delete(tag)
        }
    }
}
