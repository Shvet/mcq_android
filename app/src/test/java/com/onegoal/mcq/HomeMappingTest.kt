package com.onegoal.mcq

import com.onegoal.mcq.data.remote.HomeItemDto
import com.onegoal.mcq.data.remote.HomeSectionDto
import com.onegoal.mcq.data.remote.ListResponse
import com.onegoal.mcq.data.toHomeSections
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
    fun unknownLayoutFallsBackToHorizontal() {
        assertEquals(HomeLayout.HORIZONTAL, HomeLayout.fromApi("carousel"))
        assertEquals(HomeLayout.HORIZONTAL, HomeLayout.fromApi(null))
    }
}
