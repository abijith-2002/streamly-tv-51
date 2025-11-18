package app.claro.tv

import android.os.Bundle
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity

/**
 * PUBLIC_INTERFACE
 * ContentInfoActivity
 * A native Android TV Content Info screen implemented with Jetpack Compose that matches @figma35.
 *
 * Elements:
 * - "242 TNT" network label (16sp)
 * - Title (30sp)
 * - Metadata row (12.5sp): title | duration | genre | +16 anos badge
 * - "mas tarde" badge (bg 0xFF3F9321, 11sp)
 * - Time range | rewind icon | record icon (12.5sp)
 * - Description (13sp)
 * - Actions row: 6 pill buttons (52dp x 40dp, corner radius 50dp) with outlined 20dp icons
 *   order: clock, rewind, record, heart, lock, subtitle
 *
 * Focus behavior:
 * - Focused button background: 0xFFF4F4F4 with icon color #282828
 * - Unfocused background: 0x26F4F4F4 with icon color 0xFFF4F4F4
 * - Ensure TV DPAD focus outline behavior
 * - Initial focus on first action button
 * - DPAD_UP from actions returns to metadata section
 * - DPAD_DOWN stays within actions row (consumed)
 * - DPAD_CENTER events preserved
 *
 * Intent extras:
 * - EXTRA_TITLE (String)
 * - EXTRA_SYNOPSIS (String)
 * - EXTRA_ID (String, optional)
 * - EXTRA_THUMBNAIL_URL (String, optional - not shown on this screen)
 */
class ContentInfoActivity : FragmentActivity() {

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_SYNOPSIS = "extra_synopsis"
        const val EXTRA_ID = "extra_id"
        const val EXTRA_THUMBNAIL_URL = "extra_thumbnail_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Título de Ejemplo"
        val synopsis = intent.getStringExtra(EXTRA_SYNOPSIS) ?: "Sinopsis no disponible en este momento. Intenta nuevamente más tarde."
        // val id = intent.getStringExtra(EXTRA_ID)
        // val thumb = intent.getStringExtra(EXTRA_THUMBNAIL_URL)

        val composeView = ComposeView(this).apply {
            setContent {
                MaterialTheme {
                    ContentInfoScreen(
                        networkLabel = "242 TNT",
                        titleText = title,
                        // The metadata row shows: title | duration | genre | +16 anos
                        metaTitle = "Título",
                        durationText = "1h 52m",
                        genreText = "Drama",
                        ratingBadge = "+16 anos",
                        laterBadge = "mas tarde",
                        timeRange = "10:00 – 12:30",
                        description = synopsis
                    )
                }
            }
        }
        setContentView(composeView)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                finish()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ContentInfoScreen(
    networkLabel: String,
    titleText: String,
    metaTitle: String,
    durationText: String,
    genreText: String,
    ratingBadge: String,
    laterBadge: String,
    timeRange: String,
    description: String
) {
    // Colors
    val screenBg = Color(0xFF121212)
    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFCCCCCC)
    val metaTextColor = Color(0xFFEEEEEE)
    val badgeTextColor = Color(0xFFFFFFFF)
    val badgeBgLater = Color(0xFF3F9321)
    val badgeBgAge = Color(0x33FFFFFF) // subtle translucent badge bg for +16 anos

    // Actions row colors
    val pillBgFocused = Color(0xFFF4F4F4)
    val pillIconFocused = Color(0xFF282828)
    val pillBgUnfocused = Color(0x26F4F4F4)
    val pillIconUnfocused = Color(0xFFF4F4F4)

    // Focus anchors
    val metadataFocusRequester = remember { FocusRequester() }
    val actionRequesters = remember { List(6) { FocusRequester() } }

    // Request initial focus to the first action button
    LaunchedEffect(Unit) {
        try {
            actionRequesters.firstOrNull()?.requestFocus()
        } catch (_: IllegalStateException) {
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .padding(start = 88.dp, top = 36.dp, end = 88.dp, bottom = 48.dp)
    ) {
        // "242 TNT" label - 16sp
        Text(
            text = networkLabel,
            color = textPrimary,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Title - 30sp
        Text(
            text = titleText,
            color = textPrimary,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Metadata section (focusable anchor)
        Box(
            modifier = Modifier
                .focusRequester(metadataFocusRequester)
                .focusTarget()
                .focusable()
        ) {
            Column {
                // Row: metaTitle | duration | genre | +16 anos
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetaText(metaTitle, metaTextColor)
                    MetaDivider()
                    MetaText(durationText, metaTextColor)
                    MetaDivider()
                    MetaText(genreText, metaTextColor)
                    MetaDivider()
                    Badge(
                        text = ratingBadge,
                        bg = badgeBgAge,
                        color = badgeTextColor,
                        textSizeSp = 12.5f.sp,
                        paddingH = 8.dp,
                        paddingV = 3.dp,
                        corner = 4.dp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // "mas tarde" badge - 11sp (bg 0xFF3F9321)
                Badge(
                    text = laterBadge,
                    bg = badgeBgLater,
                    color = badgeTextColor,
                    textSizeSp = 11.sp,
                    paddingH = 10.dp,
                    paddingV = 4.dp,
                    corner = 6.dp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Time range | rewind icon | record icon (icons accompanying)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeRange,
                        color = metaTextColor,
                        fontSize = 12.5f.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))

                    Icon(
                        imageVector = Icons.Outlined.Replay,
                        contentDescription = "Rewind",
                        tint = metaTextColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))

                    Icon(
                        imageVector = Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = "Record",
                        tint = metaTextColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Description - 13sp
        Text(
            text = description,
            color = textSecondary,
            fontSize = 13.sp,
            maxLines = 7,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Actions row with 6 pill buttons (52dp x 40dp, radius 50dp, icons 20dp)
        Row(
            modifier = Modifier
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = listOf(
                ActionItem(Icons.Outlined.AccessTime, "Recordatorio"),
                ActionItem(Icons.Outlined.Replay, "Rebobinar"),
                ActionItem(Icons.Outlined.RadioButtonUnchecked, "Grabar"),
                ActionItem(Icons.Outlined.FavoriteBorder, "Favorito"),
                ActionItem(Icons.Outlined.Lock, "Bloquear"),
                ActionItem(Icons.Outlined.Subtitles, "Subtítulos")
            )

            items.forEachIndexed { index, it ->
                val leftIndex = if (index - 1 < 0) items.lastIndex else index - 1
                val rightIndex = if (index + 1 > items.lastIndex) 0 else index + 1
                ActionPillButton(
                    icon = it.icon,
                    contentDesc = it.label,
                    sizeW = 52.dp,
                    sizeH = 40.dp,
                    iconSize = 20.dp,
                    corner = 50.dp,
                    focusedBg = pillBgFocused,
                    unfocusedBg = pillBgUnfocused,
                    focusedIcon = pillIconFocused,
                    unfocusedIcon = pillIconUnfocused,
                    focusRequester = actionRequesters[index],
                    leftRequester = actionRequesters[leftIndex],
                    rightRequester = actionRequesters[rightIndex],
                    onClick = {
                        // Preserve DPAD_CENTER behavior: action-specific handling could be added here
                        // For now: no-op to keep consistent behavior; in a real app this might start playback or set a reminder, etc.
                    },
                    onDpadUp = {
                        // Move back to metadata section
                        metadataFocusRequester.requestFocus()
                    },
                    onDpadDown = {
                        // Stay within actions row: consume event
                    }
                )
            }
        }
    }
}

@Composable
private fun MetaText(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 12.5f.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun MetaDivider() {
    Text(
        text = " | ",
        color = Color(0xFFAAAAAA),
        fontSize = 12.5f.sp
    )
}

@Composable
private fun Badge(
    text: String,
    bg: Color,
    color: Color,
    textSizeSp: androidx.compose.ui.unit.TextUnit,
    paddingH: Dp,
    paddingV: Dp,
    corner: Dp
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(corner))
            .background(bg)
            .padding(horizontal = paddingH, vertical = paddingV),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = textSizeSp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private data class ActionItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ActionPillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDesc: String,
    sizeW: Dp,
    sizeH: Dp,
    iconSize: Dp,
    corner: Dp,
    focusedBg: Color,
    unfocusedBg: Color,
    focusedIcon: Color,
    unfocusedIcon: Color,
    focusRequester: FocusRequester,
    leftRequester: FocusRequester,
    rightRequester: FocusRequester,
    onClick: () -> Unit,
    onDpadUp: () -> Unit,
    onDpadDown: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }

    val shape = RoundedCornerShape(corner)

    Box(
        modifier = Modifier
            .requiredWidth(sizeW)
            .requiredHeight(sizeH)
            .clip(shape)
            .background(if (focused) focusedBg else unfocusedBg, shape)
            // subtle outline to ensure visible focus ring on TV
            .then(
                if (focused)
                    Modifier.border(width = 1.dp, color = Color(0x33000000), shape = shape)
                else Modifier
            )
            .focusRequester(focusRequester)
            .focusTarget()
            .focusProperties {
                left = leftRequester
                right = rightRequester
            }
            .onFocusChanged { state -> focused = state.hasFocus }
            .focusable()
            .semantics { contentDescription = contentDesc }
            .onKeyEvent { key ->
                val code = key.nativeKeyEvent.keyCode
                val actionDown = key.nativeKeyEvent.action == KeyEvent.ACTION_DOWN
                val actionUp = key.nativeKeyEvent.action == KeyEvent.ACTION_UP
                when (code) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        if (actionUp) onClick()
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        if (actionDown) onDpadUp()
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (actionDown) onDpadDown()
                        true
                    }
                    else -> false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = if (focused) focusedIcon else unfocusedIcon,
            modifier = Modifier.size(iconSize)
        )
    }
}
