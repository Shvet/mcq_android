package com.onegoal.mcq.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.onegoal.mcq.R
import com.onegoal.mcq.model.ContentItem
import com.onegoal.mcq.ui.list.ListUiState

/**
 * Shared list body for the home screen and chapter lists.
 * [trailingLabel] decides the red text on each row (index number or item label).
 */
@Composable
fun ContentList(
    state: ListUiState,
    onRetry: () -> Unit,
    onItemClick: (ContentItem) -> Unit,
    trailingLabel: (index: Int, item: ContentItem) -> String?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = 4.dp),
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            ListUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            ListUiState.Error -> MessageState(
                message = stringResource(R.string.error_loading),
                onRetry = onRetry,
                modifier = Modifier.align(Alignment.Center),
            )
            is ListUiState.Loaded -> if (state.items.isEmpty()) {
                MessageState(stringResource(R.string.empty_list), Modifier.align(Alignment.Center))
            } else {
                LazyColumn(contentPadding = contentPadding) {
                    itemsIndexed(state.items, key = { _, item -> item.documentKey }) { index, item ->
                        PdfListRow(
                            title = item.title,
                            trailingLabel = trailingLabel(index, item),
                            onClick = { onItemClick(item) },
                        )
                    }
                }
            }
        }
    }
}
