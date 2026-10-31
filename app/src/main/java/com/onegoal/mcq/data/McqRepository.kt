package com.onegoal.mcq.data

import com.onegoal.mcq.data.local.BookmarkDao
import com.onegoal.mcq.data.local.BookmarkEntity
import com.onegoal.mcq.data.prefs.AppPreferences
import com.onegoal.mcq.data.remote.AppConfigDto
import com.onegoal.mcq.data.remote.McqApi
import com.onegoal.mcq.model.AppConfig
import com.onegoal.mcq.model.ContentItem
import com.onegoal.mcq.model.NodeLevel
import com.onegoal.mcq.model.PdfSection
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import retrofit2.HttpException

class McqRepository(
    private val api: McqApi,
    private val bookmarkDao: BookmarkDao,
    private val preferences: AppPreferences,
    private val json: Json,
    private val packageName: String,
    private val defaultQuestionLabel: String,
    private val defaultAnswerLabel: String,
) {
    val bookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.observeAll()
    val bookmarkCount: Flow<Int> = bookmarkDao.observeCount()

    /** Top level list on the home screen (categories of this app's package). */
    suspend fun homeItems(): List<ContentItem> = emptyOn404 {
        api.categories(packageName).data.map { dto ->
            ContentItem(
                id = dto.id,
                level = NodeLevel.CATEGORY,
                title = dto.name,
                itemLabel = dto.itemLabel,
                hasChildren = dto.hasChildren == 1,
                sections = sections(dto.pdf, dto.answerPdf, dto.questionLabel, dto.answerLabel),
            )
        }
    }

    /** Children of a folder row. */
    suspend fun children(parent: ContentItem): List<ContentItem> = emptyOn404 {
        when (parent.level) {
            NodeLevel.CATEGORY -> api.subCategories(parent.id).data.map { dto ->
                ContentItem(
                    id = dto.id,
                    level = NodeLevel.SUB_CATEGORY,
                    title = dto.name,
                    parentTitle = dto.categoryName ?: parent.title,
                    itemLabel = dto.itemLabel ?: parent.itemLabel,
                    hasChildren = dto.hasChildren == 1,
                    sections = sections(dto.pdf, dto.answerPdf, dto.questionLabel, dto.answerLabel),
                )
            }

            NodeLevel.SUB_CATEGORY -> api.subSubCategories(parent.id).data.map { dto ->
                ContentItem(
                    id = dto.id,
                    level = NodeLevel.SUB_SUB_CATEGORY,
                    title = dto.name,
                    parentTitle = parent.parentTitle ?: dto.categoryName,
                    itemLabel = dto.itemLabel ?: parent.itemLabel,
                    hasChildren = false,
                    sections = sections(dto.pdf, dto.answerPdf, dto.questionLabel, dto.answerLabel),
                )
            }

            NodeLevel.SUB_SUB_CATEGORY -> emptyList()
        }
    }

    /** Fetches the admin-panel settings for this app and caches them for offline starts. */
    suspend fun refreshAppConfig(): AppConfig? = runCatching {
        api.appConfig(packageName).data?.toModel()
    }.getOrNull()?.also { preferences.saveAppConfig(it) }

    fun bookmarksForSection(item: ContentItem, sectionIndex: Int): Flow<List<BookmarkEntity>> =
        bookmarkDao.observeForSection(item.documentKey, sectionIndex)

    suspend fun addBookmark(item: ContentItem, sectionIndex: Int, page: Int) {
        bookmarkDao.insert(
            BookmarkEntity(
                documentKey = item.documentKey,
                sectionIndex = sectionIndex,
                page = page,
                title = item.title,
                parentTitle = item.parentTitle,
                sectionLabel = item.sections.getOrNull(sectionIndex)?.label.orEmpty(),
                itemJson = json.encodeToString(ContentItem.serializer(), item),
            ),
        )
    }

    suspend fun removeBookmark(item: ContentItem, sectionIndex: Int, page: Int) =
        bookmarkDao.deletePage(item.documentKey, sectionIndex, page)

    suspend fun deleteBookmark(id: Long) = bookmarkDao.delete(id)

    fun decodeItem(itemJson: String): ContentItem? =
        runCatching { json.decodeFromString(ContentItem.serializer(), itemJson) }.getOrNull()

    private fun sections(pdf: String?, answerPdf: String?, questionLabel: String?, answerLabel: String?): List<PdfSection> =
        buildList {
            if (!pdf.isNullOrBlank()) add(PdfSection(questionLabel?.takeIf { it.isNotBlank() } ?: defaultQuestionLabel, pdf))
            if (!answerPdf.isNullOrBlank()) add(PdfSection(answerLabel?.takeIf { it.isNotBlank() } ?: defaultAnswerLabel, answerPdf))
        }

    /** The backend answers 404 with an empty list when a level has no rows yet. */
    private suspend fun <T> emptyOn404(block: suspend () -> List<T>): List<T> = try {
        block()
    } catch (e: HttpException) {
        if (e.code() == 404) emptyList() else throw e
    }

    private fun AppConfigDto.toModel() = AppConfig(
        toolbarTitle = toolbarTitle,
        splashTitle = splashTitle ?: appName,
        logoUrl = logo,
        splashImageUrl = splashImage,
        feedbackEmail = feedbackEmail,
        privacyPolicyUrl = privacyPolicyUrl,
        moreAppsUrl = moreAppsUrl,
        shareMessage = shareMessage,
        supportTitle = supportTitle,
        supportMessage = supportMessage,
        lastReadTitle = lastReadTitle,
        lastReadMessage = lastReadMessage,
        adsEnabled = adsEnabled,
        bannerAdUnitId = admobBannerId?.takeIf { it.isNotBlank() },
        interstitialAdUnitId = admobInterstitialId?.takeIf { it.isNotBlank() },
        interstitialEvery = interstitialEvery,
        minVersionCode = minVersionCode,
    )
}
