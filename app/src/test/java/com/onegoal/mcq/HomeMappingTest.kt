package com.onegoal.mcq

import com.onegoal.mcq.data.remote.HomeItemDto
import com.onegoal.mcq.data.remote.HomeSectionDto
import com.onegoal.mcq.data.remote.ListResponse
import com.onegoal.mcq.data.fallbackHomeSections
import com.onegoal.mcq.data.toHomeSections
import com.onegoal.mcq.model.ContentItem
import com.onegoal.mcq.model.HomeSeeAll
import com.onegoal.mcq.model.QuizNode
import com.onegoal.mcq.model.HomeLayout
import com.onegoal.mcq.model.HomeMeta
import com.onegoal.mcq.model.HomeTarget
import com.onegoal.mcq.model.NodeLevel
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeMappingTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false }

    private fun map(vararg sections: HomeSectionDto) = sections.toList().toHomeSections("QUESTIONS", "ANSWERS")

    @Test
    fun decodesTheBackendResponse() {
        val body = """
            {"status":true,"success":true,"data":[{"id":3,"title":"Chapters","source_type":"package","item_type":"category",
              "layout":"grid","items":[{"id":9,"type":"category","name":"Atoms","has_children":false,"image":null,
              "pdf":"https://x/q.pdf","answer_pdf":"https://x/a.pdf","answer_label":"Key","question_count":0,"children":[]}]}]}
        """.trimIndent()
        val response = json.decodeFromString(ListResponse.serializer(HomeSectionDto.serializer()), body)
        val section = map(*response.data.toTypedArray()).single()

        assertEquals(HomeLayout.GRID, section.layout)
        val target = section.items.single().target as HomeTarget.Pdf
        assertTrue(target.item.isPdf)
        assertEquals(listOf("QUESTIONS", "Key"), target.item.sections.map { it.label })
        assertEquals(HomeMeta.Media(pdf = true, audio = false, video = false), section.items.single().meta)
    }

    @Test
    fun foldersShowChildCountOrTapToOpen() {
        val child = HomeItemDto(id = 2, type = "sub_category", name = "Child")
        val items = map(
            HomeSectionDto(
                id = 1, layout = "list",
                items = listOf(
                    HomeItemDto(id = 1, type = "category", name = "With", hasChildren = true, children = listOf(child, child)),
                    HomeItemDto(id = 2, type = "category", name = "Without", hasChildren = true),
                ),
            ),
        ).single().items

        assertEquals(HomeMeta.Items(2), items[0].meta)
        assertEquals(HomeMeta.TapToOpen, items[1].meta)
        assertTrue((items[0].target as HomeTarget.Pdf).item.hasChildren)
    }

    @Test
    fun mcqItemsOpenTheQuizTree() {
        val item = map(
            HomeSectionDto(
                id = 1,
                items = listOf(HomeItemDto(id = 5, type = "quiz_sub_category", name = "Cells", parentName = "Biology", questionCount = 12)),
            ),
        ).single().items.single()

        val node = (item.target as HomeTarget.Quiz).node
        assertEquals(NodeLevel.SUB_CATEGORY, node.level)
        assertEquals("Biology", node.parentTitle)
        assertTrue(node.isTest)
        assertTrue(item.isQuiz)
        assertEquals(HomeMeta.Questions(12), item.meta)
    }

    @Test
    fun bannersOpenNothingAndUnknownTypesAreDropped() {
        val sections = map(
            HomeSectionDto(id = 1, layout = "slider", items = listOf(HomeItemDto(id = 4, type = "banner", image = "https://x/b.png"))),
            HomeSectionDto(id = 2, items = listOf(HomeItemDto(id = 5, type = "something_new"))),
        )

        assertEquals(1, sections.size)
        assertEquals(HomeLayout.SLIDER, sections.single().layout)
        assertEquals(HomeTarget.None, sections.single().items.single().target)
        assertFalse(sections.single().items.single().isQuiz)
    }

    @Test
    fun seeAllPointsAtTheListTheSectionCameFrom() {
        val item = HomeItemDto(id = 1, type = "category", name = "A")
        fun seeAll(type: String, id: String) =
            map(HomeSectionDto(id = 1, title = "Physics", sourceType = type, sourceId = id, items = listOf(item))).single().seeAll

        assertEquals(HomeSeeAll.AllPdfs, seeAll("package", "com.a"))
        assertEquals(HomeSeeAll.AllQuizzes, seeAll("quiz_package", "com.a"))
        val folder = (seeAll("category", "7") as HomeSeeAll.PdfFolder).item
        assertEquals(7L, folder.id)
        assertEquals("Physics", folder.title)
        assertTrue(folder.hasChildren)
        assertEquals(NodeLevel.SUB_CATEGORY, (seeAll("quiz_sub_category", "9") as HomeSeeAll.QuizFolder).node.level)
        assertEquals(null, seeAll("banner", "banners"))
    }

    @Test
    fun fallbackHomeShowsPdfAndMcqCategoriesWhenAdminHasNoSections() {
        val pdfs = (1L..8L).map { ContentItem(id = it, level = NodeLevel.CATEGORY, title = "Chapter $it", hasChildren = true) }
        val quizzes = listOf(QuizNode(id = 3, level = NodeLevel.CATEGORY, title = "Test", questionCount = 20))

        val sections = fallbackHomeSections("PDF Notes", pdfs, "MCQ Tests", quizzes)

        assertEquals(listOf("PDF Notes", "MCQ Tests"), sections.map { it.title })
        assertEquals(6, sections[0].items.size)
        assertEquals(HomeSeeAll.AllPdfs, sections[0].seeAll)
        assertEquals(HomeMeta.Questions(20), sections[1].items.single().meta)
        assertTrue(fallbackHomeSections("P", emptyList(), "Q", emptyList()).isEmpty())
    }

    @Test
    fun unknownLayoutFallsBackToHorizontal() {
        assertEquals(HomeLayout.HORIZONTAL, HomeLayout.fromApi("carousel"))
        assertEquals(HomeLayout.HORIZONTAL, HomeLayout.fromApi(null))
    }
}
