# App themes (Settings > App theme)

The user picks one of six predefined dark color sets; the whole app changes at once. The choice is saved on the profile (`UserProfile.themeId`, database version 53, migration 52 to 53) so it syncs to the user's other phones like the rest of the profile.

## The six palettes (`ui/theme/ThemePalettes.kt`)
Chartreuse (the original look and the default), Ocean, Violet, Rose, Mint, Graphite. Each palette defines: background, surface, card color (`surfaceVariant`), accent, text on the accent, muted text and outlines (`secondary`), the warning color (`alert`, always a warm amber so it never drifts towards the accent) and the main text color.
**Contrast is enforced by `ThemePalettesTest`:** accent on background and text on accent at least 7:1, accent on cards at least 4.5:1, main text at least 7:1 and muted text at least 4.5:1 on all three layers, the warning at least 4.5:1, a hue gap of at least 25 degrees between warning and accent (colour-blind safety), layers getting lighter from background to cards.

## How it works
* `MyApplicationTheme(palette)` builds the whole Material color scheme from the palette, including dialogs, sheets and dividers (`surfaceContainer*`, `outlineVariant`).
* `AppPalette.current` is the shared palette that `AccentTokens` (the colors of the tiles) and the badge gallery read; `MyApplicationTheme` keeps it in step.
* `ThemeCache` remembers the palette id on the phone, and `MainActivity` sets the palette before the first frame, so the app never flashes the default colors while the profile loads.
* `AppearanceScreen` shows a preview card per palette; tapping one calls `ShasthoViewModel.setTheme`.
* An unknown or missing id (for example from a newer app version) falls back to the default.

## Rules for new code
Use `MaterialTheme.colorScheme` (or `AppPalette.current` where there is no composable). Never use the default palette's constants (`SignatureAccent`, `GunMetal`, ...) outside `ui/theme`: `DesignTokenGuardTest` fails the build. To add a palette, add one line to `ThemePalettes.all`; the contrast tests tell you at once if it is not readable.

## Changes to the default look (small, on purpose)
Dialogs, bottom sheets and divider lines now take their colors from the palette (they used to be neutral grey-purple), so they match the app in every theme, including the default.

## Not built
A light theme (the app is dark-only), following the system theme, and custom colors picked freely by the user.
