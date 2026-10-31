package com.onegoal.mcq.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.onegoal.mcq.R
import com.onegoal.mcq.ui.theme.McqColors

data class OverflowAction(val label: String, val onClick: () -> Unit)

/** Blue app bar used by the list screens: menu or back on the left, optional home and overflow on the right. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McqTopBar(
    title: String,
    onNavigation: () -> Unit,
    showBack: Boolean,
    onHome: (() -> Unit)? = null,
    overflow: List<OverflowAction> = emptyList(),
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontSize = 21.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.5.sp,
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigation) {
                if (showBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                } else {
                    Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.open_menu))
                }
            }
        },
        actions = {
            if (onHome != null) {
                IconButton(onClick = onHome) { Icon(Icons.Outlined.Home, contentDescription = stringResource(R.string.menu_home)) }
            }
            if (overflow.isNotEmpty()) {
                OverflowMenu(overflow)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = McqColors.Primary,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White,
        ),
    )
}

@Composable
fun OverflowMenu(actions: List<OverflowAction>) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.label) },
                    onClick = {
                        expanded = false
                        action.onClick()
                    },
                )
            }
        }
    }
}
