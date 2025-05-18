![alt text](https://github.com/Singularity-Coder/Instant-SQLite/blob/main/assets/logo192.png)
# Play Book
Listen to your e-books! [Download APK](https://github.com/Singularity-Coder/Play-Book/blob/main/assets/app-debug.apk)

## Screenshots
![alt text](https://github.com/Singularity-Coder/Instant-SQLite/blob/main/assets/sc1.png)
![alt text](https://github.com/Singularity-Coder/Instant-SQLite/blob/main/assets/sc2.png)
![alt text](https://github.com/Singularity-Coder/Instant-SQLite/blob/main/assets/sc3.png)
![alt text](https://github.com/Singularity-Coder/Instant-SQLite/blob/main/assets/sc4.png)

## Tech stack & Open-source libraries
- Minimum SDK level 31
-  [Kotlin](https://kotlinlang.org/) based, [Coroutines](https://github.com/Kotlin/kotlinx.coroutines) + [Flow](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/) for asynchronous.
- Jetpack
  - Lifecycle: Observe Android lifecycles and handle UI states upon the lifecycle changes.
  - ViewModel: Manages UI-related data holder and lifecycle aware. Allows data to survive configuration changes such as screen rotations.
  - DataBinding: Binds UI components in your layouts to data sources in your app using a declarative format rather than programmatically.
  - Room: Constructs Database by providing an abstraction layer over SQLite to allow fluent database access.
  - [Hilt](https://dagger.dev/hilt/): for dependency injection.
  - WorkManager: WorkManager allows you to schedule work to run one-time or repeatedly using flexible scheduling windows.
- Architecture
  - MVVM Architecture (View - DataBinding - ViewModel - Model)
- [Retrofit2 & OkHttp3](https://github.com/square/retrofit): Construct the REST APIs and paging network data.
- [gson](https://github.com/google/gson): A Java serialization/deserialization library to convert Java Objects into JSON and back.
- [Material-Components](https://github.com/material-components/material-components-android): Material design components for building ripple animation, and CardView.
- [Coil](https://github.com/coil-kt/coil): Image loading for Android and Compose Multiplatform.
- [Browser](https://developer.android.com/jetpack/androidx/releases/browser): Custom Chrome Tab.
- [iTextPdf](https://github.com/itext/itextpdf): Convert PDF to plain text.

## Architecture
![alt text](https://github.com/Singularity-Coder/Instant-SQLite/blob/main/assets/arch.png)

This App is based on the MVVM architecture and the Repository pattern, which follows the [Google's official architecture guidance](https://developer.android.com/topic/architecture).

The overall architecture of this App is composed of two layers; the UI layer and the data layer. Each layer has dedicated components and they have each different responsibilities.