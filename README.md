<p align="center">
  <img src="banner.svg" alt="SpeedDial Banner" width="100%"/>
</p>

# SpeedDial

A minimalist Android speed dial app for one-tap calling of your favorite contacts.

## Features

- **One-Tap Calling** - Instantly call contacts with a single tap
- **Custom Contact Photos** - Add personal photos for each contact
- **Import/Export** - Backup and restore contacts as ZIP archives
- **Room Database** - Local storage, no cloud dependency
- **Material 3 Design** - Modern, clean interface

## Screenshots

| Contact List | Add Contact |
|--------------|-------------|
| Quick access to all speed dial contacts | Easy contact creation with photo |

## How It Works

1. Tap **+** to add a new contact
2. Enter name, phone number, and optionally add a photo
3. Tap the green **Call** button to instantly dial
4. Tap **Edit** icon to modify or delete contacts
5. Use the menu to export/import your contacts

## Permissions

- `CALL_PHONE` - Required to make phone calls directly

## Tech Stack

- **Kotlin** - 100% Kotlin codebase
- **Jetpack Compose** - Modern declarative UI
- **Room Database** - Local SQLite storage
- **Coil** - Image loading
- **Material 3** - Latest Material Design components
- **Navigation Compose** - In-app navigation

## Requirements

- Android 9.0 (API 28) or higher

## Installation

### From Release
1. Download the latest APK from [Releases](../../releases)
2. Enable "Install from unknown sources" if prompted
3. Install and grant phone permission when asked

### Build from Source
```bash
git clone https://github.com/sunil-dhaka/SpeedDial.git
cd SpeedDial
./gradlew assembleDebug
```

## Project Structure

```
app/src/main/java/com/example/speeddial/
    MainActivity.kt              # Entry point, navigation setup
    data/
        Contact.kt               # Contact entity
        ContactDao.kt            # Database access object
        ContactDatabase.kt       # Room database
        ContactRepository.kt     # Data repository
        ContactTransferManager.kt # Import/export logic
    ui/
        ContactViewModel.kt      # State management
        screens/
            ContactListScreen.kt # Main contact list
            EditContactScreen.kt # Add/edit contact
        theme/                   # Material 3 theming
```

## License

MIT License - feel free to use, modify, and distribute.

## Author

Built with Jetpack Compose by [sunil-dhaka](https://github.com/sunil-dhaka)
