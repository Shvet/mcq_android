package com.onegoal.mcq

import com.onegoal.mcq.model.ContentItem
import com.onegoal.mcq.model.NodeLevel
import com.onegoal.mcq.model.PdfSection
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentItemTest {

    private val item = ContentItem(
        id = 7,
        level = NodeLevel.SUB_CATEGORY,
        title = "अध्याय-2 परमाणु की संरचना",
        parentTitle = "NEET-AIPMT Chapter-wise रसायन विज्ञान",
        itemLabel = "Chapter-wise",
        sections = listOf(
            PdfSection("QUESTIONS-SECTION:", "https://example.com/q.pdf"),
            PdfSection("ANSWERS-SECTION:", "https://example.com/a.pdf"),
        ),
    )

    @Test
    fun itemWithSectionsAndNoChildrenIsAPdf() {
        assertTrue(item.isPdf)
        assertFalse(item.copy(hasChildren = true).isPdf)
        assertFalse(item.copy(sections = emptyList()).isPdf)
    }

    @Test
    fun documentKeyIncludesLevelSoIdsDoNotClash() {
        assertEquals("SUB_CATEGORY:7", item.documentKey)
        assertEquals("CATEGORY:7", item.copy(level = NodeLevel.CATEGORY).documentKey)
    }

    @Test
    fun survivesJsonRoundTripUsedForNavigationAndBookmarks() {
        val json = Json { ignoreUnknownKeys = true }
        val encoded = json.encodeToString(ContentItem.serializer(), item)
        assertEquals(item, json.decodeFromString(ContentItem.serializer(), encoded))
    }
}
