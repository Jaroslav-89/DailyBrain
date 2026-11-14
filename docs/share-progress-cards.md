# Share progress cards

The "Ваша серия" calendar screen includes a local progress-card sharing flow:
- choose the Classic or Premium 4:5 card style;
- preview the current streak, best streak, weekly progress and monthly streak progress;
- share a PNG through the Android system Sharesheet.

Cards are rendered on-device and use the active local profile display name. The feature works offline and does not require Google/Firebase authentication. Generated PNG files are stored temporarily under the app cache and exposed only through AndroidX FileProvider.

## Artwork

The two card backgrounds are drawn locally by `ProgressShareCardRenderer`, rather than loaded from a remote host. This keeps the feature available offline and avoids adding network requests to the sharing flow. The visual direction was checked against these freely usable Unsplash references:

- Classic direction: [Abstract mint green background with subtle wave pattern](https://unsplash.com/photos/abstract-mint-green-background-with-subtle-wave-pattern-zbRB5CDyTQ8)
- Premium direction: [A dark green background with gold lines](https://unsplash.com/photos/a-dark-green-background-with-gold-lines-bKKyecJoslc)

The referenced photographs are not bundled in the app; the rendered cards use original local vector artwork.
