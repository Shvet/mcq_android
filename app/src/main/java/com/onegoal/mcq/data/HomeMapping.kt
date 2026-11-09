package com.onegoal.mcq.data

import com.onegoal.mcq.data.remote.HomeItemDto
import com.onegoal.mcq.data.remote.HomeSectionDto
import com.onegoal.mcq.model.ContentItem
import com.onegoal.mcq.model.HomeItem
import com.onegoal.mcq.model.HomeLayout
import com.onegoal.mcq.model.HomeMeta
import com.onegoal.mcq.model.HomeSection
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
    pdfPassword: String? = null,
    answerPdfPassword: String? = null,
): List<PdfSection> = buildList {
    if (!pdf.isNullOrBlank()) {
        add(PdfSection(questionLabel?.takeIf { it.isNotBlank() } ?: defaultQuestionLabel, pdf, pdfPassword?.takeIf { it.isNotEmpty() }))
    }
    if (!answerPdf.isNullOrBlank()) {
        add(PdfSection(answerLabel?.takeIf { it.isNotBlank() } ?: defaultAnswerLabel, answerPdf, answerPdfPassword?.takeIf { it.isNotEmpty() }))
    }
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
        )
    }

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
                sections = pdfSections(
                    pdf, answerPdf, questionLabel, answerLabel, defaultQuestionLabel, defaultAnswerLabel,
                    pdfPassword, answerPdfPassword,
                ),
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
