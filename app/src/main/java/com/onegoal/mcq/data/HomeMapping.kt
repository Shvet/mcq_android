package com.onegoal.mcq.data

import com.onegoal.mcq.data.remote.HomeItemDto
import com.onegoal.mcq.data.remote.HomeSectionDto
import com.onegoal.mcq.model.ContentItem
import com.onegoal.mcq.model.HomeItem
import com.onegoal.mcq.model.HomeLayout
import com.onegoal.mcq.model.HomeMeta
import com.onegoal.mcq.model.HomeSection
import com.onegoal.mcq.model.HomeSeeAll
import com.onegoal.mcq.model.HomeTarget
import com.onegoal.mcq.model.NodeLevel
import com.onegoal.mcq.model.PdfSection
import com.onegoal.mcq.model.QuizNode

/** Question / answer PDF tabs of an item; blank labels fall back to the app's defaults. */
internal fun pdfSections(
    pdf: String?,
    answerPdf: String?,
    questionLabel: String?,
    answerLabel: String?,
    defaultQuestionLabel: String,
    defaultAnswerLabel: String,
): List<PdfSection> = buildList {
    if (!pdf.isNullOrBlank()) add(PdfSection(questionLabel?.takeIf { it.isNotBlank() } ?: defaultQuestionLabel, pdf))
    if (!answerPdf.isNullOrBlank()) add(PdfSection(answerLabel?.takeIf { it.isNotBlank() } ?: defaultAnswerLabel, answerPdf))
}

/** Turns /api/home into home sections, dropping sections whose items are all unknown types. */
internal fun List<HomeSectionDto>.toHomeSections(defaultQuestionLabel: String, defaultAnswerLabel: String): List<HomeSection> =
    mapNotNull { section ->
        val items = section.items.mapNotNull { it.toHomeItem(defaultQuestionLabel, defaultAnswerLabel) }
        if (items.isEmpty()) return@mapNotNull null
        HomeSection(
            id = section.id,
            title = section.title,
            layout = HomeLayout.fromApi(section.layout),
            items = items,
            seeAll = section.seeAll(),
        )
    }

/** "See all" of a section: the whole package, or the folder the section lists. */
internal fun HomeSectionDto.seeAll(): HomeSeeAll? {
    val folderId = sourceId?.toLongOrNull()
    return when (sourceType) {
        "package" -> HomeSeeAll.AllPdfs
        "quiz_package" -> HomeSeeAll.AllQuizzes
        "category", "sub_category" -> folderId?.let {
            val level = if (sourceType == "category") NodeLevel.CATEGORY else NodeLevel.SUB_CATEGORY
            HomeSeeAll.PdfFolder(ContentItem(id = it, level = level, title = title, hasChildren = true))
        }
        "quiz_category", "quiz_sub_category" -> folderId?.let {
            val level = if (sourceType == "quiz_category") NodeLevel.CATEGORY else NodeLevel.SUB_CATEGORY
            HomeSeeAll.QuizFolder(QuizNode(id = it, level = level, title = title, hasChildren = true))
        }
        else -> null
    }
}

/** Built-in home for apps whose admin has no sections yet: their PDF and MCQ categories. */
internal fun fallbackHomeSections(
    pdfTitle: String,
    pdfs: List<ContentItem>,
    quizTitle: String,
    quizzes: List<QuizNode>,
): List<HomeSection> = buildList {
    if (pdfs.isNotEmpty()) add(
        HomeSection(
            id = -1,
            title = pdfTitle,
            layout = HomeLayout.GRID,
            items = pdfs.take(6).map { it.toHomeItem() },
            seeAll = HomeSeeAll.AllPdfs,
        ),
    )
    if (quizzes.isNotEmpty()) add(
        HomeSection(
            id = -2,
            title = quizTitle,
            layout = HomeLayout.LIST,
            items = quizzes.take(5).map { it.toHomeItem() },
            seeAll = HomeSeeAll.AllQuizzes,
        ),
    )
}

internal fun ContentItem.toHomeItem() = HomeItem(
    id = id,
    name = title,
    imageUrl = null,
    isQuiz = false,
    meta = if (hasChildren) HomeMeta.TapToOpen else HomeMeta.Media(pdf = sections.isNotEmpty(), audio = false, video = false),
    target = HomeTarget.Pdf(this),
)

internal fun QuizNode.toHomeItem() = HomeItem(
    id = id,
    name = title,
    imageUrl = imageUrl,
    isQuiz = true,
    meta = if (hasChildren) HomeMeta.TapToOpen else HomeMeta.Questions(questionCount),
    target = HomeTarget.Quiz(this),
)

internal fun HomeItemDto.toHomeItem(defaultQuestionLabel: String, defaultAnswerLabel: String): HomeItem? {
    val image = image?.takeIf { it.isNotBlank() }
    val isQuiz = type.startsWith("quiz_")
    val level = when (type.removePrefix("quiz_")) {
        "category" -> NodeLevel.CATEGORY
        "sub_category" -> NodeLevel.SUB_CATEGORY
        "sub_sub_category" -> NodeLevel.SUB_SUB_CATEGORY
        "banner" -> null
        else -> return null
    }
    // Sub sub categories are always leaves in both trees.
    val folder = hasChildren && level != NodeLevel.SUB_SUB_CATEGORY

    val target = when {
        level == null -> HomeTarget.None
        isQuiz -> HomeTarget.Quiz(
            QuizNode(
                id = id,
                level = level,
                title = name,
                parentTitle = parentName,
                imageUrl = image,
                hasChildren = folder,
                questionCount = questionCount,
            ),
        )
        else -> HomeTarget.Pdf(
            ContentItem(
                id = id,
                level = level,
                title = name,
                parentTitle = parentName,
                itemLabel = itemLabel,
                hasChildren = folder,
                sections = pdfSections(pdf, answerPdf, questionLabel, answerLabel, defaultQuestionLabel, defaultAnswerLabel),
            ),
        )
    }

    val meta = when {
        folder -> if (children.isNotEmpty()) HomeMeta.Items(children.size) else HomeMeta.TapToOpen
        isQuiz -> HomeMeta.Questions(questionCount)
        else -> HomeMeta.Media(
            pdf = !pdf.isNullOrBlank() || !answerPdf.isNullOrBlank(),
            audio = !audio.isNullOrBlank(),
            video = !video.isNullOrBlank(),
        )
    }

    return HomeItem(id = id, name = name, imageUrl = image, isQuiz = isQuiz, meta = meta, target = target)
}
