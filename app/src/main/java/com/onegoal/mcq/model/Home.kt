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
)

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
    val key: String get() = when (target) {
        is HomeTarget.Pdf -> "pdf:${target.item.documentKey}"
        is HomeTarget.Quiz -> "quiz:${target.node.key}"
        HomeTarget.None -> "banner:$id"
    }
}
