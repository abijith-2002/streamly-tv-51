package app.claro.tv.views

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * PUBLIC_INTERFACE
 * A compact Android TV top navigation bar built with Jetpack Compose.
 * - Horizontally centered, positioned 18.dp from the top by its parent.
 * - Container uses exactly: Modifier.padding(0.dp).width(579.5.dp).height(32.dp)
 *   .background(Color(0xFF28292F), RoundedCornerShape(17.dp))
 * - Items in order: Search icon, "Inicio", "Peliculas", "Series", "TV en vivo", "Kids", "Mis Contenidos"
 * - Even spacing within 579.5.dp width, D-pad focusable with visible focus feedback.
 *
 * Params:
 * - onItemClick: stub click handler for each item, index based (0 = search)
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TopNavBar(
    modifier: Modifier = Modifier,
    onItemClick: (index: Int) -> Unit = {}
) {
    // Items including search as first logical element
    val labels = listOf(
        "SEARCH_ICON",
        "Inicio", "Peliculas", "Series", "TV en vivo", "Kids", "Mis Contenidos"
    )

    // Container: EXACT modifier chain as requested
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
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // We lay them evenly by distributing space between items
            // Create 7 focusable items (icon + 6 text items)
            labels.forEachIndexed { index, label ->
                if (index == 0) {
                    // Search Icon item
                    FocusablePill(
                        isIcon = true,
                        label = null,
                        onClick = { onItemClick(0) }
                    )
                } else {
                    FocusablePill(
                        isIcon = false,
                        label = label,
                        onClick = { onItemClick(index) }
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
 * Provides visible focus feedback (border + slight scale) on TV.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FocusablePill(
    isIcon: Boolean,
    label: String?,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }
    val focusBorderColor = if (focused) Color.White else Color.Transparent

    val shape = RoundedCornerShape(12.dp)

    val focusableModifier = Modifier
        .border(width = 1.dp, color = focusBorderColor, shape = shape)
        .focusable(interactionSource = interactionSource)
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
        modifier = focusableModifier
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .onFocusChanged { state -> focused = state.hasFocus }
        ) {
            if (isIcon) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
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
}


