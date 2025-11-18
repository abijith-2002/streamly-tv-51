# Content Info Screen

This activity (ContentInfoActivity) implements the content details scene based on the 35-4077-14472 Figma assets. It is a native TV layout (no WebView) and matches the pixel grid using dp equivalents:
- Overscan-safe padding: 88dp left/right, 36dp top, 48dp bottom
- Title: 24sp, white
- Synopsis: 18sp, #b0b0b0
- Primary action pill (Reproducir) and secondary (Volver): 32dp height, rounded/focus background using #DE1717 when focused and #28292F unfocused

Focus and navigation:
- Initial focus goes to the first action button. A slight delay is used to ensure the composition is attached before requesting focus, preventing touch-mode traps.
- DPAD_LEFT and DPAD_RIGHT move focus between action buttons without requiring DPAD_CENTER, wired via FocusRequester and focusProperties.
- DPAD_UP from the actions row returns focus to the metadata section.
- DPAD_DOWN uses default focus behavior (not consumed).
- DPAD_BACK finishes the activity.
- DPAD_CENTER/ENTER on a focused action triggers its handler.
- No container (Row/LazyRow) is focusable or intercepts DPAD_LEFT/RIGHT.

Focus visuals (per diagnostics and spec):
- Focused button background: 0xFFF4F4F4 with icon color #282828
- Unfocused background: 0x26F4F4F4 with icon color 0xFFF4F4F4
- Visuals update immediately on focus via onFocusChanged; no reliance on click/pressed/selected states.

Implementation notes:
- The actions row is a Row with Modifier.focusGroup(); the row itself is not focusable.
- Each pill is a single focus-owning node (Surface) that draws its background and icon/text with:
  - Modifier.focusRequester(...)
  - Modifier.focusTarget()
  - Modifier.focusProperties { left = ..., right = ..., up = ... }
  - Modifier.onFocusChanged { isFocused -> ... }
  - Modifier.focusable()
  - No key handler consumes LEFT/RIGHT; only DPAD_CENTER/ENTER is handled for click. Directional keys return false to allow platform focus navigation.
- The metadata section is a non-intercepting focus anchor to support DPAD_UP from actions.

Maintenance notes:
- Do not add onKeyEvent handlers on parent containers that return true for DPAD_LEFT or DPAD_RIGHT. Doing so will break system focus navigation.
- Keep the focusTarget + focusable modifiers on the same node that draws the background/icon so focus state and visuals are in sync.
- Maintain explicit left/right FocusRequester wiring between sibling pills for deterministic navigation.
- If you observe initial focus not being taken, ensure the LaunchedEffect with a short delay is intact, or request focus again after composition settles.

Intent extras:
- EXTRA_TITLE, EXTRA_SYNOPSIS, EXTRA_ID, EXTRA_THUMBNAIL_URL
