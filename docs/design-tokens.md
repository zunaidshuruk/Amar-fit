# Colors in screens: theme tokens only (dark-only app)

The app is dark-only, so screens use `MaterialTheme.colorScheme` (and `AccentTokens`) directly: no `Emerald*`, `Slate*`, `Orange*`, `Red500`, `TextPrimary`, hex
`Color(0x...)` literals, and no `if (isDark) ... else ...` light-mode branches. `DesignTokenGuardTest` fails the build if they come back.

* Exceptions: camera overlays and image scrims (`ScannerScreen.kt`, `QrScannerScreen.kt`, `UniversalAssistantScreen.kt`) may use plain `Color.White` / `Color.Black`.
* `pendingCleanup` in the test lists files still to be cleaned; it is empty now (Sprints 23 and 24 finished the cleanup). Never grow it; a cleaned file left in the list also fails.
* The badge medal colors (bronze, silver, gold) are named colors in `ui/theme/Color.kt`, the one place plain colors belong.
* The Edit tiles screen (Sprint 23) is now three section cards (Your tiles, Large tiles, Small tiles) in the Nutrition-screen style: 20 dp surfaceVariant cards with inset rows.
* Roles: primary (lime) = accent and "do this" buttons (primary container with onPrimary text); secondary (Iron) / onSurfaceVariant = muted text and inactive icons;
  tertiary (amber) = warnings and "over target"; error = the one destructive or missed state; surfaceVariant = card background.
* The red "Watch" buttons (Markdown video blocks, workout video button, tutorial icon) now use the lime accent, like the Recipe screen.
* Sprint 24 (2026-10-15): the Today screen, bottom bar (unselected items were still an old blue-grey), Metric detail, Health, Food log and others lost their dead light-mode branches.
