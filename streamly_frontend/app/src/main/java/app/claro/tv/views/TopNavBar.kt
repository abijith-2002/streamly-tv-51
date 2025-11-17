package app.claro.tv.views

import android.view.KeyEvent
import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.border
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.delay
import app.claro.tv.R

/**
 * PUBLIC_INTERFACE
 * A compact Android TV top navigation bar built with Jetpack Compose.
 * - Horizontally centered, positioned 18.dp from the top by its parent.
 * - Container uses exactly: Modifier.padding(0.dp).width(579.5.dp).height(32.dp)
 *   .background(Color(0xFF28292F), RoundedCornerShape(17.dp))
 * - Items in order: Search icon, "Inicio", "Peliculas", "Series", "TV en vivo", "Kids", "Mis Contenidos"
 * - Even spacing within 579.5.dp width, D-pad focusable with visible focus feedback.
 * - Focus state: pill-shaped background (#9B0F0F), height 26.5dp, width wrapping content, corner radius 18.5dp
 * - Initial focus targets the search icon
 *
 * Params:
 * - onItemClick: stub click handler for each item, index based (0 = search)
 * - requestInitialFocus: when true, requests focus on the search icon
 *
 * Additional:
 * - Exposes requestSearchFocus() through a View lambda so non-Compose code can move focus
 *   (e.g., DPAD_RIGHT override from hero/initial area).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TopNavBar(
    modifier: Modifier = Modifier,
    onItemClick: (index: Int) -> Unit = {},
    requestInitialFocus: Boolean = true
) {
    val labels = listOf(
        "SEARCH_ICON",
        "Inicio", "Peliculas", "Series", "TV en vivo", "Kids", "Mis Contenidos"
    )

    val searchFocusRequester = remember { FocusRequester() }

    // Provide a bridge: attach a callback on the hosting Android View to request search focus
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

    LaunchedEffect(requestInitialFocus) {
        if (requestInitialFocus) {
            try {
                delay(60)
                searchFocusRequester.requestFocus()
            } catch (_: IllegalStateException) {
            }
        }
    }

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
                if (index == 0) {
                    FocusablePill(
                        isIcon = true,
                        label = null,
                        onClick = { onItemClick(0) },
                        focusRequester = searchFocusRequester
                    )
                } else {
                    FocusablePill(
                        isIcon = false,
                        label = label,
                        onClick = { onItemClick(index) },
                        focusRequester = null
                    )
                }
                if (index < labels.lastIndex) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    }
}

/**
 * Small focusable pill for each nav item.
 * Provides visible focus feedback with pill-shaped background on TV.
 * Focus styling: #9B0F0F background, height 26.5dp, width wraps content, corner radius 18.5dp
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FocusablePill(
    isIcon: Boolean,
    label: String?,
    onClick: () -> Unit,
    focusRequester: FocusRequester?
) {
    val interactionSource = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }

    val pillShape = RoundedCornerShape(18.5.dp)
    var focusableModifier = Modifier
        .wrapContentWidth()
        .height(26.5.dp)
        .background(color = if (focused) Color(0xFFDE1717) else Color.Transparent, shape = pillShape)
        .padding(horizontal = 12.dp)

    if (focusRequester != null) {
        focusableModifier = focusableModifier.focusRequester(focusRequester)
    }

    // Add semantic content description for accessibility
    val contentDescription = if (isIcon) "Search" else label.orEmpty()
    
    focusableModifier = focusableModifier
        .semantics { this.contentDescription = contentDescription }
        .focusTarget()
        .focusProperties { canFocus = true }
        .focusable(interactionSource = interactionSource)
        .onFocusChanged { state -> focused = state.hasFocus }
        .onKeyEvent { keyEvent ->
            if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER
            ) {
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                    onClick()
                }
                true
            } else {
                false
            }
        }

    Box(
        modifier = focusableModifier,
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
