package io.github.evaogbe.diswantin.task.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import javax.inject.Inject

class LocalTagRepository @Inject constructor(private val tagDao: TagDao) : TagRepository {
    override val tagPagingData = Pager(PagingConfig(pageSize = 40)) {
        tagDao.getTagPagingSource()
    }.flow

    override suspend fun hasTags() = tagDao.hasTags()

    override fun getTagById(id: Long) = tagDao.getTagById(id)

    override fun getTagsByTaskId(taskId: Long, size: Int) = tagDao.getTagsByTaskId(taskId, size)

    override fun search(query: String, size: Int) = tagDao.search(escapeSql("$query*"), size)

    private fun escapeSql(str: String) = str.replace("'", "''").replace("\"", "\"\"")

    override suspend fun create(form: NewTagForm) {
        tagDao.insert(form.newTag)
    }

    override suspend fun update(form: EditTagForm) {
        tagDao.update(form.updatedTag)
    }

    override suspend fun delete(tag: Tag) {
        tagDao.delete(tag)
    }
}
