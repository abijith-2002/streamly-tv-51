package app.claro.tv.views

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.claro.tv.R
import kotlinx.coroutines.delay

// Local sealed hierarchy must be top-level (not local) to compile correctly in Kotlin.
private sealed class NavVisual {
    data class IconItem(val icon: ImageVector, val contentDesc: String) : NavVisual()
    data class LabelItem(val text: String) : NavVisual()
}

/**
 * PUBLIC_INTERFACE
 * A compact Android TV top navigation bar built with Jetpack Compose.
 * - Container uses exact spec:
 *   Modifier.padding(0.dp).width(579.5.dp).height(32.dp)
 *     .background(Color(0xFF28292F), RoundedCornerShape(17.dp))
 * - Items: Search icon, "Inicio", "Peliculas", "Series", "TV en vivo", "Kids", "Mis Contenidos", Avatar
 * - Focus: each item is focusable with DPAD LEFT/RIGHT chaining; focused item shows red pill (#DE1717) with 18.5dp radius
 * - Label text size: 14.5sp
 * - Exposes runnables via host view tags to:
 *   - focus Search (R.id.tag_request_search_focus)
 *   - restore last-focused nav item (R.id.tag_request_last_nav_focus)
 *   - move DOWN to hero (consumed via FocusablePill on DPAD_DOWN using R.id.tag_request_focus_hero)
 *
 * Params:
 * - onItemClick: invoked when an item is "clicked" (DPAD_CENTER/ENTER)
 * - requestInitialFocus: when true, focuses the Search icon initially
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TopNavBar(
    modifier: Modifier = Modifier,
    onItemClick: (index: Int) -> Unit = {},
    requestInitialFocus: Boolean = true
) {
    // Keep the textual/structural contents; append Avatar per requirement
    val navItems: List<NavVisual> = listOf(
        NavVisual.IconItem(Icons.Filled.Search, "Search"),
        NavVisual.LabelItem("Inicio"),
        NavVisual.LabelItem("Peliculas"),
        NavVisual.LabelItem("Series"),
        NavVisual.LabelItem("TV en vivo"),
        NavVisual.LabelItem("Kids"),
        NavVisual.LabelItem("Mis Contenidos"),
        NavVisual.IconItem(Icons.Filled.AccountCircle, "Avatar")
    )

    // Focus requesters: one per nav item, first is dedicated for search
    val requesters = remember { List(navItems.size) { FocusRequester() } }

    // Track the last-focused nav item index to restore focus when moving up from hero
    var lastFocusedIndex by remember { mutableIntStateOf(0) }

    // Expose runnables on the host view to allow non-Compose code to change focus
    val hostView = LocalView.current
    hostView.setTag(
        R.id.tag_request_search_focus,
        Runnable {
            try {
                requesters.firstOrNull()?.requestFocus()
            } catch (_: IllegalStateException) {
            }
        }
    )
    hostView.setTag(
        R.id.tag_request_last_nav_focus,
        Runnable {
            try {
                requesters[lastFocusedIndex.coerceIn(0, requesters.lastIndex)].requestFocus()
            } catch (_: IllegalStateException) {
            }
        }
    )

    // Optionally request initial focus on the Search icon
    LaunchedEffect(requestInitialFocus) {
        if (requestInitialFocus) {
            try {
                // slight delay to ensure composition is attached to a window
                delay(60)
                requesters.firstOrNull()?.requestFocus()
            } catch (_: IllegalStateException) {
            }
        }
    }

    // Container with specified dimensions and background
    val containerShape = RoundedCornerShape(size = 17.dp)
    Box(
        modifier = modifier
            .padding(0.dp)
            // Increase width to prevent label wrapping for "Mis Contenidos"
            // Keep height and radius per spec while ensuring overscan-safe area usage.
            .width(660.dp)
            .height(32.dp)
            .background(color = Color(0xFF28292F), shape = containerShape),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                // Slightly reduce horizontal padding to reclaim space for labels
                .padding(horizontal = 10.dp)
                .focusGroup(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            navItems.forEachIndexed { index, item ->
                val leftRequester = requesters[(index - 1).coerceAtLeast(0)]
                val rightRequester = requesters[(index + 1).coerceAtMost(requesters.lastIndex)]

                when (item) {
                    is NavVisual.IconItem -> {
                        FocusablePill(
                            icon = item.icon,
                            label = null,
                            contentDescription = item.contentDesc,
                            onClick = { onItemClick(index) },
                            focusRequester = requesters[index],
                            leftRequester = leftRequester,
                            rightRequester = rightRequester
                        ) { hasFocus ->
                            if (hasFocus) lastFocusedIndex = index
                        }
                    }

                    is NavVisual.LabelItem -> {
                        FocusablePill(
                            icon = null,
                            label = item.text,
                            contentDescription = item.text,
                            onClick = { onItemClick(index) },
                            focusRequester = requesters[index],
                            leftRequester = leftRequester,
                            rightRequester = rightRequester
                        ) { hasFocus ->
                            if (hasFocus) lastFocusedIndex = index
                        }
                    }
                }

                if (index < navItems.lastIndex) {
                    // Slightly reduce inter-item gap to fit long labels safely
                    Spacer(modifier = Modifier.width(6.dp))
                }
            }
        }
    }
}

/**
 * A focusable nav item that draws a pill background when focused and provides DPAD routing.
 * Pill specs: #DE1717 color, 18.5dp radius, 26.5dp height.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FocusablePill(
    icon: ImageVector?,
    label: String?,
    contentDescription: String,
    onClick: () -> Unit,
    focusRequester: FocusRequester,
    leftRequester: FocusRequester,
    rightRequester: FocusRequester,
    onFocusedChanged: (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }
    val pillRadius = 18.5.dp

    val hostView = LocalView.current

    val baseModifier = Modifier
        .wrapContentWidth()
        .height(26.5.dp)
        .focusRequester(focusRequester)
        .semantics { this.contentDescription = contentDescription }
        .focusTarget()
        .focusProperties {
            left = leftRequester
            right = rightRequester
        }
        .focusable(interactionSource = interactionSource)
        .onFocusChanged { state ->
            focused = state.hasFocus
            onFocusedChanged(state.hasFocus)
        }
        // Visible focus pill background
        .drawBehind {
            if (focused) {
                val corner = pillRadius.toPx()
                drawRoundRect(
                    color = Color(0xFFDE1717),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner)
                )
            }
        }
        .padding(horizontal = 10.dp)
        .onKeyEvent { keyEvent ->
            val code = keyEvent.nativeKeyEvent.keyCode
            val actionDown = keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN
            val actionUp = keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_UP
            when (code) {
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    if (actionUp) onClick()
                    true
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    // Stay within nav bar on UP
                    true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    // Move focus to hero via host runnable
                    if (actionDown) {
                        (hostView.getTag(R.id.tag_request_focus_hero) as? Runnable)?.run()
                    }
                    true
                }
                else -> false
            }
        }

    Box(
        modifier = baseModifier,
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Text(
                text = label.orEmpty(),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                fontSize = 14.5.sp
            )
        }
    }
}
