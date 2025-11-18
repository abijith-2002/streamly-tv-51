# Content Info Screen

This activity (ContentInfoActivity) implements the content details scene based on the 35-4077-14472 Figma assets. It is a native TV layout (no WebView) and matches the pixel grid using dp equivalents:
- Overscan-safe padding: 88dp left/right, 36dp top, 48dp bottom
- Title: 24sp, white
- Synopsis: 18sp, #b0b0b0
- Primary action pill (Reproducir) and secondary (Volver): 32dp height, rounded/focus background using #DE1717 when focused and #28292F unfocused

Focus and navigation:
- Initial focus goes to the first action button.
- DPAD_LEFT and DPAD_RIGHT move focus between action buttons without requiring DPAD_CENTER, wired via FocusRequester and focusProperties.
- DPAD_UP from the actions row returns focus to the metadata section.
- DPAD_DOWN within the actions row is consumed (focus stays in the row).
- DPAD_BACK finishes the activity.
- DPAD_CENTER/ENTER on a focused action triggers its handler.
- No container (Row/LazyRow) is focusable or intercepts DPAD_LEFT/RIGHT.

Intent extras:
- EXTRA_TITLE, EXTRA_SYNOPSIS, EXTRA_ID, EXTRA_THUMBNAIL_URL
