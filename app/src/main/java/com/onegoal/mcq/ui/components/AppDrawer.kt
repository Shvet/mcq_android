package com.onegoal.mcq.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onegoal.mcq.R
import com.onegoal.mcq.ui.theme.McqColors

enum class DrawerDestination { Home, Bookmarks, Share, Rate, Feedback }

@Composable
fun AppDrawer(
    title: String,
    logoUrl: String?,
    bookmarkCount: Int,
    selected: DrawerDestination,
    onSelect: (DrawerDestination) -> Unit,
) {
    ModalDrawerSheet(drawerContainerColor = Color.White, drawerShape = androidx.compose.ui.graphics.RectangleShape) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(McqColors.Primary)
                .statusBarsPadding()
                .padding(start = 22.dp, end = 16.dp, top = 22.dp, bottom = 20.dp),
        ) {
            AppLogo(logoUrl, Modifier.size(84.dp))
            Spacer(Modifier.height(14.dp))
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        DrawerItem(Icons.Outlined.Home, stringResource(R.string.menu_home), selected == DrawerDestination.Home) {
            onSelect(DrawerDestination.Home)
        }
        DrawerItem(
            icon = Icons.Filled.CollectionsBookmark,
            label = stringResource(R.string.menu_bookmarks),
            selected = selected == DrawerDestination.Bookmarks,
            badge = bookmarkCount,
        ) { onSelect(DrawerDestination.Bookmarks) }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        DrawerItem(Icons.Outlined.Share, stringResource(R.string.menu_share), false) { onSelect(DrawerDestination.Share) }
        DrawerItem(Icons.Outlined.StarBorder, stringResource(R.string.menu_rate), false) { onSelect(DrawerDestination.Rate) }
        DrawerItem(Icons.Outlined.Email, stringResource(R.string.menu_feedback), false) { onSelect(DrawerDestination.Feedback) }
    }
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    badge: Int? = null,
    onClick: () -> Unit,
) {
    val tint = if (selected) McqColors.Primary else Color.Black
    NavigationDrawerItem(
        label = { Text(label, fontSize = 18.sp, color = tint) },
        icon = { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp)) },
        selected = false,
        onClick = onClick,
        badge = badge?.let {
            {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(McqColors.BadgeYellow, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(it.toString(), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                }
            }
        },
        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}
