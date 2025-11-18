# Content Info Screen

This activity (ContentInfoActivity) implements the content details scene based on the 35-4077-14472 Figma assets. It is a native TV layout (no WebView) and matches the pixel grid using dp equivalents:
- Overscan-safe padding: 88dp left/right, 36dp top, 48dp bottom
- Title: 24sp, white
- Synopsis: 18sp, #b0b0b0
- Primary action pill (Reproducir) and secondary (Volver): 32dp height, rounded/focus background using #DE1717 when focused and #28292F unfocused

Focus and navigation:
- Initial focus goes to the primary action pill.
- DPAD_BACK finishes the activity.
- DPAD_DOWN from top spacer moves to the action row.
- DPAD_CENTER/ENTER on any rail item (Home) launches this activity passing metadata via Intent extras.

Intent extras:
- EXTRA_TITLE, EXTRA_SYNOPSIS, EXTRA_ID, EXTRA_THUMBNAIL_URL
