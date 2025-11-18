package app.claro.tv

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
 * - Initial focus on first action button
 * - DPAD_UP from actions returns to metadata section (via focus properties)
 * - DPAD_DOWN uses default focus behavior (not consumed)
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
    val pillIconFocused = Color(0xFF282828) // icon/text on focused background
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
                .focusGroup(), // Parent is a focus group only; it is not focusable and does not intercept keys
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

                FocusAwarePill(
                    modifier = Modifier.semantics { contentDescription = it.label },
                    icon = rememberVectorPainter(it.icon),
                    text = it.label, // label included; small pill width may clip text which is acceptable per minimal design
                    onClick = {
                        // DPAD_CENTER behavior: action-specific handling could be added here
                    },
                    focusColor = pillBgFocused,
                    unfocusColor = pillBgUnfocused,
                    focusRequester = actionRequesters[index],
                    leftRequester = actionRequesters[leftIndex],
                    rightRequester = actionRequesters[rightIndex],
                    upRequester = metadataFocusRequester,
                    sizeW = 52.dp,
                    sizeH = 40.dp,
                    corner = 50.dp,
                    iconSize = 20.dp,
                    focusedIconColor = pillIconFocused
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
    val icon: ImageVector,
    val label: String
)

/**
 * A minimal, focus-driven pill that owns focus and draws its own visuals inside a single node.
 * - Uses a Surface that is both the focus target and the visual background.
 * - Visual state computed only from focus state.
 * - No interactionSource/pressed/selected logic. Only DPAD_CENTER/ENTER triggers onClick.
 * - Directional DPAD keys return false to allow system focus navigation.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FocusAwarePill(
    modifier: Modifier = Modifier,
    icon: Painter,
    text: String,
    onClick: () -> Unit,
    focusColor: Color,
    unfocusColor: Color,
    focusRequester: FocusRequester,
    leftRequester: FocusRequester,
    rightRequester: FocusRequester,
    upRequester: FocusRequester,
    sizeW: Dp,
    sizeH: Dp,
    corner: Dp,
    iconSize: Dp,
    focusedIconColor: Color
) {
    // Focus-driven state exclusively in this node
    var isFocused by remember { mutableStateOf(false) }

    val bgColor = if (isFocused) focusColor else unfocusColor
    // Acceptance criteria:
    // - focused -> background = focusColor, icon = #282828
    // - unfocused -> background = unfocusColor, icon = focusColor
    val iconTint = if (isFocused) focusedIconColor else focusColor
    val textTint = iconTint

    val shape = RoundedCornerShape(corner)

    Surface(
        color = bgColor,
        contentColor = iconTint,
        shape = shape,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
            .requiredWidth(sizeW)
            .requiredHeight(sizeH)
            .focusRequester(focusRequester)
            // Focus target and focusable are applied to this same node that also draws the background
            .focusTarget()
            .focusProperties {
                left = leftRequester
                right = rightRequester
                up = upRequester
                // Do not override "down" to keep default navigation and avoid consuming DPAD_DOWN
            }
            .onFocusChanged { state ->
                val nowFocused = state.isFocused
                if (isFocused != nowFocused) {
                    isFocused = nowFocused
                    Log.d(
                        "ContentInfoFocus",
                        "FocusAwarePill onFocusChanged: \"$text\" isFocused=$isFocused"
                    )
                }
            }
            .focusable()
            // Only handle DPAD_CENTER/ENTER for click; do not consume DPAD directional keys
            .onKeyEvent { key ->
                val code = key.nativeKeyEvent.keyCode
                val actionUp = key.nativeKeyEvent.action == KeyEvent.ACTION_UP
                when (code) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        if (actionUp) onClick()
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT,
                    KeyEvent.KEYCODE_DPAD_RIGHT,
                    KeyEvent.KEYCODE_DPAD_UP,
                    KeyEvent.KEYCODE_DPAD_DOWN -> false
                    else -> false
                }
            }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = text,
                tint = iconTint,
                modifier = Modifier.size(iconSize)
            )
            if (text.isNotBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = text,
                    color = textTint,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
