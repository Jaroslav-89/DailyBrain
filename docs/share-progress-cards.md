# Share progress cards

The "Ваша серия" calendar screen includes a local progress-card sharing flow:
- choose the Classic or Premium 4:5 card style;
- preview the current streak, best streak, weekly progress and monthly streak progress;
- share a PNG through the Android system Sharesheet.

Cards are rendered on-device and use the active local profile display name. The feature works offline and does not require Google/Firebase authentication. Generated PNG files are stored temporarily under the app cache and exposed only through AndroidX FileProvider.
