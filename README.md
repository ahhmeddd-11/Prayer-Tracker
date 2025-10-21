# Prayer Tracker 🕌

A modern Android application built with Kotlin and Jetpack Compose to help Muslims track their daily prayers and maintain prayer consistency through an intelligent streak system.

## Features ✨

### 📿 Prayer Tracking
- Track all 5 mandatory prayers (Fajr, Dhuhr, Asr, Maghrib, Isha)
- Optional Tahajjud prayer tracking
- Simple Yes/No marking system
- Real-time prayer status updates

### 🔥 Smart Streak System
- **Infinite streak counting** - track your prayer consistency over time
- **Automatic reset** when any required prayer is marked as "No"
- **Real-time updates** - see your streak change immediately
- **Motivational display** showing current active streak

### ⏰ Prayer Time Management
- **Custom prayer times** - set your local prayer times
- **Intuitive time input** - separate hour/minute fields with validation
- **Smart alarm system** - notifications 5 minutes before each prayer
- **Flexible reminders** - choose between notifications or loud alarms

### 🔊 Advanced Sound System
- **Complete system sound library** - access all alarm sounds on your device
- **Sound preview** - hear sounds before selecting them
- **Custom sounds** - use your own audio files from device storage
- **Categorized sounds** - [Alarm], [Ringtone], [Notification] options

### 🚀 Smart Notifications
- **Intelligent scheduling** - prevents old/outdated alarms from firing
- **Wake-up protection** - no spam notifications after device sleep
- **Precise timing** - exact alarm scheduling with fallback handling
- **Boot persistence** - maintains alarms across device restarts

### 🎨 Modern UI/UX
- **Material 3 Design** - clean, modern interface
- **Dark/Light themes** - automatic theme switching
- **Navigation drawer** - easy access to all features
- **Bottom navigation** - quick home access from any screen
- **Responsive design** - optimized for all screen sizes

## Screenshots 📱

*Screenshots can be added here showing the main interface, prayer tracking, settings, etc.*

## Installation 📲

### Prerequisites
- Android device running Android 7.0 (API level 24) or higher
- Android Studio for development

### Building from Source
1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/Prayer-Tracker.git
   cd Prayer-Tracker
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned folder

3. **Build the project**
   - Build → Build Bundle(s) / APK(s) → Build APK(s)
   - APK will be generated in `app/build/outputs/apk/debug/`

4. **Install on device**
   - Transfer the APK to your Android device
   - Enable "Unknown Sources" in device settings
   - Install the APK

## Usage Guide 📖

### Initial Setup
1. **Open the app** and grant necessary permissions
2. **Set prayer times** - Go to Prayer Times screen and enter your local times
3. **Configure notifications** - Enable alarm sounds in App Settings
4. **Start tracking** - Mark your prayers as completed throughout the day

### Daily Usage
1. **Mark prayers** - Use Yes/No buttons to track each prayer
2. **View streak** - See your current prayer consistency streak
3. **Get reminders** - Receive notifications 5 minutes before each prayer
4. **Monitor progress** - Check daily completion status (X/5 prayers)

### Advanced Features
- **Sound customization** - Choose from system sounds or upload custom audio
- **Theme switching** - Toggle between light and dark modes
- **Prayer scheduling** - Set personalized prayer times for your location

## Technical Details 🛠️

### Architecture
- **MVVM Pattern** with Jetpack Compose
- **Reactive UI** with `mutableStateOf` and `derivedStateOf`
- **Persistent storage** using SharedPreferences with JSON serialization
- **Modular design** with separated concerns

### Key Technologies
- **Kotlin** - Modern, concise programming language
- **Jetpack Compose** - Declarative UI framework
- **Material 3** - Latest Material Design components
- **AlarmManager** - Precise notification scheduling
- **RingtoneManager** - System sound integration
- **MediaPlayer** - Custom sound playback

### Permissions Required
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

## Project Structure 📁

```
app/src/main/
├── java/com/example/prayertracker/
│   ├── MainActivity.kt          # Main app logic and UI
│   ├── AlarmActivity.kt         # Full-screen alarm interface
│   ├── NotificationReceiver.kt  # Handles prayer notifications
│   └── BootReceiver.kt         # Restores alarms after boot
├── res/
│   ├── values/
│   │   ├── strings.xml         # App strings
│   │   └── themes.xml          # App themes
│   └── drawable/
│       └── app_icon.xml        # App icon
└── AndroidManifest.xml         # App configuration
```

## Contributing 🤝

1. **Fork the repository**
2. **Create a feature branch** (`git checkout -b feature/amazing-feature`)
3. **Commit changes** (`git commit -m 'Add amazing feature'`)
4. **Push to branch** (`git push origin feature/amazing-feature`)
5. **Open a Pull Request**

## Development Setup 💻

### Requirements
- Android Studio Arctic Fox or newer
- Kotlin 1.9.25+
- Android SDK 35
- Gradle 8.5+

### Building
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test
```

## Roadmap 🗺️

- [ ] **Qibla Direction** - Compass pointing to Mecca
- [ ] **Prayer Statistics** - Monthly/yearly prayer completion analytics
- [ ] **Multiple Locations** - Support for travel and multiple cities
- [ ] **Widget Support** - Home screen widget for quick access
- [ ] **Backup & Sync** - Cloud backup for prayer data
- [ ] **Habit Tracking** - Additional Islamic habits tracking
- [ ] **Community Features** - Share achievements with friends

## License 📄

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments 🙏

- **Islamic Calendar** - For accurate prayer time calculations
- **Material Design** - For beautiful UI components
- **Jetpack Compose** - For modern Android UI development
- **Muslim Community** - For inspiration and feedback

## Support 💬

If you find this app helpful, please:
- ⭐ **Star this repository**
- 🐛 **Report bugs** by opening an issue
- 💡 **Suggest features** in the discussions
- 🔄 **Share with fellow Muslims**

## Contact 📧

For questions, suggestions, or support:
- **GitHub Issues**: [Open an issue](https://github.com/your-username/Prayer-Tracker/issues)
- **Email**: syedahmed4957@gmail.com

---

**Made with ❤️ for the Muslim Ummah**

*May Allah accept our prayers and grant us consistency in worship.*
