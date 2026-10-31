package com.onegoal.mcq.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.onegoal.mcq.R
import com.onegoal.mcq.ui.theme.McqColors

/** Round app logo: the admin-panel logo when set, otherwise the launcher artwork. */
@Composable
fun AppLogo(logoUrl: String?, modifier: Modifier = Modifier) {
    val shaped = modifier
        .clip(CircleShape)
        .background(Color.White)
    if (logoUrl.isNullOrBlank()) {
        Image(painter = painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = shaped)
    } else {
        AsyncImage(model = logoUrl, contentDescription = null, modifier = shaped)
    }
}

/** "Dear Students Please Note:" explanation shown the first time the last-read button is pressed. */
@Composable
fun LastReadInfoDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(2.dp),
        containerColor = Color.White,
        title = { Text(title, fontSize = 22.sp, fontWeight = FontWeight.Medium) },
        text = { Text(message, fontSize = 18.sp, color = Color(0xFF424242)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok), color = McqColors.DialogAction, fontSize = 17.sp, letterSpacing = 1.sp)
            }
        },
    )
}

/** "Show Your Support !!" dialog shown when leaving the app from the home screen. */
@Composable
fun SupportDialog(
    logoUrl: String?,
    title: String,
    message: String,
    onQuit: () -> Unit,
    onRate: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color.White,
            border = BorderStroke(8.dp, McqColors.DialogBlue),
            shape = RoundedCornerShape(2.dp),
        ) {
            Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppLogo(logoUrl, Modifier.size(48.dp))
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .border(1.5.dp, Color(0xFF00695C), CircleShape),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close), tint = Color(0xFF00695C))
                    }
                }
                Spacer(Modifier.size(12.dp))
                Text(
                    text = message,
                    fontSize = 17.sp,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.size(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onQuit) {
                        Text(stringResource(R.string.support_quit), color = McqColors.QuitRed, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    TextButton(onClick = onRate) {
                        Text(stringResource(R.string.support_rate), color = McqColors.RateGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

/** "Jump To Page" dialog with a numeric field. Pages are 1-based for the user. */
@Composable
fun JumpToPageDialog(currentPage: Int, pageCount: Int, onJump: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(currentPage.toString()) }
    val page = text.toIntOrNull()
    val valid = page != null && page in 1..pageCount
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(2.dp),
        containerColor = Color.White,
        title = { Text(stringResource(R.string.jump_to_page), fontSize = 22.sp, fontWeight = FontWeight.Medium) },
        text = {
            Column {
                TextField(
                    value = text,
                    onValueChange = { value -> text = value.filter(Char::isDigit).take(5) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 20.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (valid) onJump(page!!) }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color(0xFF3F51B5),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
                if (!valid) {
                    Text(
                        stringResource(R.string.invalid_page, pageCount),
                        color = McqColors.IndexRed,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = McqColors.DialogAction, fontSize = 17.sp, letterSpacing = 1.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = { if (valid) onJump(page!!) }, enabled = valid) {
                Text(stringResource(R.string.ok), color = McqColors.DialogAction, fontSize = 17.sp, letterSpacing = 1.sp)
            }
        },
    )
}

