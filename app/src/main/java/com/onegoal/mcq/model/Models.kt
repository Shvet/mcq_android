package com.onegoal.mcq.model

import kotlinx.serialization.Serializable

/** Level of an item in the backend hierarchy; decides which endpoint lists its children. */
@Serializable
enum class NodeLevel { CATEGORY, SUB_CATEGORY, SUB_SUB_CATEGORY }

/** One section (tab) of a PDF item, e.g. the questions PDF or the answers PDF. */
@Serializable
data class PdfSection(
    val label: String,
    val url: String,
)

/** A row in any list screen: either a folder with children or a PDF document. */
@Serializable
data class ContentItem(
    val id: Long,
    val level: NodeLevel,
    val title: String,
    /** Name of the parent shown in bookmarks and on list rows, e.g. "NEET-AIPMT Chapter-wise रसायन विज्ञान". */
    val parentTitle: String? = null,
    /** Small red label on the right of a row, e.g. "Chapter-wise". */
    val itemLabel: String? = null,
    val hasChildren: Boolean = false,
    val sections: List<PdfSection> = emptyList(),
) {
    val isPdf: Boolean get() = !hasChildren && sections.isNotEmpty()

    /** Stable key used for bookmarks and the last-read shortcut. */
    val documentKey: String get() = "${level.name}:$id"
}

/** Where the reader was in a document. */
@Serializable
data class ReadingPosition(
    val item: ContentItem,
    val sectionIndex: Int = 0,
    val page: Int = 0,
)

/** App texts and switches from the admin panel, with built-in fallbacks. */
@Serializable
data class AppConfig(
    val toolbarTitle: String? = null,
    val splashTitle: String? = null,
    val logoUrl: String? = null,
    val splashImageUrl: String? = null,
    val feedbackEmail: String? = null,
    val privacyPolicyUrl: String? = null,
    val moreAppsUrl: String? = null,
    val shareMessage: String? = null,
    val supportTitle: String? = null,
    val supportMessage: String? = null,
    val lastReadTitle: String? = null,
    val lastReadMessage: String? = null,
    val adsEnabled: Boolean = true,
    val bannerAdUnitId: String? = null,
    val interstitialAdUnitId: String? = null,
    val interstitialEvery: Int = 3,
    val minVersionCode: Int = 1,
)
