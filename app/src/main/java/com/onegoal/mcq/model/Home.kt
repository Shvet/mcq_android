package com.onegoal.mcq.model

/** How a home section draws its items; mirrors the admin builder's "Layout" choice. */
enum class HomeLayout {
    HORIZONTAL, GRID, LIST, SLIDER;

    companion object {
        fun fromApi(value: String?): HomeLayout = when (value?.lowercase()) {
            "grid" -> GRID
            "list" -> LIST
            "slider" -> SLIDER
            else -> HORIZONTAL
        }
    }
}

/** One block of the home screen built in admin (Home Screen Sections) for this app. */
data class HomeSection(
    val id: Long,
    val title: String,
    val layout: HomeLayout,
    val items: List<HomeItem>,
    /** Where the section's "See all" goes; null hides it (banners). */
    val seeAll: HomeSeeAll? = null,
)

/** Full list behind a section's "See all". */
sealed interface HomeSeeAll {
    /** Every PDF category of this app. */
    data object AllPdfs : HomeSeeAll

    /** Every MCQ category of this app. */
    data object AllQuizzes : HomeSeeAll

    /** Children of one PDF folder. */
    data class PdfFolder(val item: ContentItem) : HomeSeeAll

    /** Children of one MCQ folder. */
    data class QuizFolder(val node: QuizNode) : HomeSeeAll
}

/** What a tap on a home item opens. */
sealed interface HomeTarget {
    /** A PDF folder (opens its list) or a PDF document. */
    data class Pdf(val item: ContentItem) : HomeTarget

    /** An MCQ folder (opens its list) or a test. */
    data class Quiz(val node: QuizNode) : HomeTarget

    /** Banners only show an image. */
    data object None : HomeTarget
}

/** Small grey line under an item's name; same rules as the admin preview's `meta()`. */
sealed interface HomeMeta {
    data class Items(val count: Int) : HomeMeta
    data object TapToOpen : HomeMeta
    data class Questions(val count: Int) : HomeMeta
    data class Media(val pdf: Boolean, val audio: Boolean, val video: Boolean) : HomeMeta
}

data class HomeItem(
    val id: Long,
    val name: String,
    val imageUrl: String?,
    val isQuiz: Boolean,
    val meta: HomeMeta,
    val target: HomeTarget,
) {
    /** Opens a list rather than a PDF or a test. */
    val isFolder: Boolean get() = when (target) {
        is HomeTarget.Pdf -> target.item.hasChildren
        is HomeTarget.Quiz -> target.node.hasChildren
        HomeTarget.None -> false
    }

    val key: String get() = when (target) {
        is HomeTarget.Pdf -> "pdf:${target.item.documentKey}"
        is HomeTarget.Quiz -> "quiz:${target.node.key}"
        HomeTarget.None -> "banner:$id"
    }
}
