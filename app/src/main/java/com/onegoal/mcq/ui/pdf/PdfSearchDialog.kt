package com.onegoal.mcq.ui.pdf

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onegoal.mcq.R
import com.onegoal.mcq.ui.theme.McqColors

/** Search box plus a list of matching pages; tapping a result jumps to it. */
@Composable
fun PdfSearchDialog(
    state: SearchState,
    onSearch: (String) -> Unit,
    onResultClick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf((state as? SearchState.Results)?.query.orEmpty()) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(2.dp),
        containerColor = Color.White,
        title = { Text(stringResource(R.string.search_title), fontSize = 22.sp) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { if (query.isNotBlank()) onSearch(query) }),
                    trailingIcon = {
                        IconButton(onClick = { if (query.isNotBlank()) onSearch(query) }) {
                            Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
                when (state) {
                    SearchState.Idle -> Unit
                    SearchState.Searching -> Row(
                        modifier = Modifier.padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(stringResource(R.string.searching), modifier = Modifier.padding(start = 12.dp))
                    }
                    is SearchState.Results -> if (state.pages.isEmpty()) {
                        Text(stringResource(R.string.search_no_results), modifier = Modifier.padding(top = 16.dp))
                    } else {
                        Text(
                            stringResource(R.string.search_results, state.pages.size),
                            color = McqColors.SectionGreen,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                        )
                        LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                            items(state.pages) { page ->
                                Text(
                                    text = stringResource(R.string.search_page_result, page + 1),
                                    fontSize = 17.sp,
                                    color = McqColors.ChapterBlue,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onResultClick(page) }
                                        .padding(vertical = 12.dp),
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close), color = McqColors.DialogAction) }
        },
    )
}
