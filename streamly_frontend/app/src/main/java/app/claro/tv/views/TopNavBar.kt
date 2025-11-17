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
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.claro.tv.R
import kotlinx.coroutines.delay

/**
 * PUBLIC_INTERFACE
 * A compact Android TV top navigation bar built with Jetpack Compose.
 * - Horizontally centered, positioned by parent (18dp from top).
 * - Container uses: Modifier.padding(0.dp).width(579.5.dp).height(32.dp)
 *   .background(Color(0xFF28292F), RoundedCornerShape(17.dp))
 * - Items: Search icon, "Inicio", "Peliculas", "Series", "TV en vivo", "Kids", "Mis Contenidos"
 * - Visible focus: pill-shaped background (#DE1717), radius=18.5dp, height=26.5dp.
 * - Each item is independently focusable with explicit DPAD left/right chaining.
 * - Exposes runnables via host view tags to request search focus and last-focused nav item.
 *
 * Params:
 * - onItemClick: stub click handler for item activation.
 * - requestInitialFocus: when true, focuses the Search icon initially.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TopNavBar(
    modifier: Modifier = Modifier,
    onItemClick: (index: Int) -> Unit = {},
    requestInitialFocus: Boolean = true
) {
    // Keep the textual/structural contents unchanged
    val labels = listOf(
        "SEARCH_ICON",
        "Inicio", "Peliculas", "Series", "TV en vivo", "Kids", "Mis Contenidos"
    )

    // Focus requesters: one per nav item, first is dedicated for search
    val searchFocusRequester = remember { FocusRequester() }
    val otherRequesters = remember { List(labels.size - 1) { FocusRequester() } }
    val requesters = remember(searchFocusRequester, otherRequesters) {
        listOf(searchFocusRequester) + otherRequesters
    }

    // Track the last-focused nav item index to restore focus when moving up from hero
    var lastFocusedIndex by remember { mutableIntStateOf(0) }

    // Expose runnables on the host view to allow non-Compose code to change focus
    val hostView = LocalView.current
    hostView.setTag(
        R.id.tag_request_search_focus,
        Runnable {
            try {
                searchFocusRequester.requestFocus()
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
                searchFocusRequester.requestFocus()
            } catch (_: IllegalStateException) {
            }
        }
    }

    // Container with specified dimensions and background
    val containerShape = RoundedCornerShape(size = 17.dp)
    Box(
        modifier = modifier
            .padding(0.dp)
            .width(579.5.dp)
            .height(32.dp)
            .background(color = Color(0xFF28292F), shape = containerShape),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .focusGroup(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEachIndexed { index, label ->
                val isIcon = index == 0
                val leftRequester = requesters[(index - 1).coerceAtLeast(0)]
                val rightRequester = requesters[(index + 1).coerceAtMost(requesters.lastIndex)]

                FocusablePill(
                    isIcon = isIcon,
                    label = if (isIcon) null else label,
                    onClick = { onItemClick(index) },
                    focusRequester = requesters[index],
                    leftRequester = leftRequester,
                    rightRequester = rightRequester,
                    onFocusedChanged = { hasFocus ->
                        if (hasFocus) {
                            lastFocusedIndex = index
                        }
                    }
                )

                if (index < labels.lastIndex) {
                    Spacer(modifier = Modifier.width(8.dp))
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
    isIcon: Boolean,
    label: String?,
    onClick: () -> Unit,
    focusRequester: FocusRequester,
    leftRequester: FocusRequester,
    rightRequester: FocusRequester,
    onFocusedChanged: (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }

    val pillShape = RoundedCornerShape(18.5.dp)
    val hostView = LocalView.current

    val contentDesc = if (isIcon) "Search" else label.orEmpty()

    val baseModifier = Modifier
        .wrapContentWidth()
        .height(26.5.dp)
        .focusRequester(focusRequester)
        .semantics { this.contentDescription = contentDesc }
        .focusTarget()
        .focusProperties {
            left = leftRequester
            right = rightRequester
            // Up/Down are handled via onKeyEvent to ensure correct routing
        }
        .focusable(interactionSource = interactionSource)
        .onFocusChanged { state ->
            focused = state.hasFocus
            onFocusedChanged(state.hasFocus)
        }
        // Use Compose drawBehind for the pill background tied to focus state,
        // satisfying visible focus requirement without altering nav contents.
        .drawBehind {
            if (focused) {
                // Draw a rounded rect "pill" red background under the item
                val corner = 18.5.dp.toPx()
                drawRoundRect(
                    color = Color(0xFFDE1717),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner)
                )
            }
        }
        .padding(horizontal = 12.dp)
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
                    // Remain within nav bar when pressing UP
                    true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    // Move focus to hero container via host runnable
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
        if (isIcon) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Text(
                text = label.orEmpty(),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }
    }
}
