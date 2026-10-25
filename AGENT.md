# AGENT.md — My Music Video

Guía para agentes (y personas) que trabajan en este repositorio. `CLAUDE.md` importa este archivo.

## Objetivo del proyecto

Aplicación Android personal para **reproducir videos de YouTube como si fuera un reproductor de música**:

- El usuario inicia sesión con Google y guarda sus videos favoritos en Firebase, **organizados por categorías** (listas de reproducción propias).
- Un **reproductor propio** a pantalla completa reproduce la lista en orden, aleatorio o repitiendo uno, y **sigue sonando en segundo plano**: con la pantalla apagada, en Picture-in-Picture y controlado desde la notificación, la pantalla de bloqueo o los audífonos Bluetooth.
- Los videos se pueden encontrar navegando YouTube dentro de la app o con el buscador (YouTube Data API v3), y también se pueden enviar a un **Chromecast**.

Es de uso personal: el módulo del reproductor salta anuncios y permite reproducir en segundo plano, lo que va contra los Términos de Servicio de YouTube. **No está pensada para publicarse en Google Play.**

## Stack

| Área | Tecnología |
|---|---|
| Lenguaje | Java 8 (`app`), Kotlin (`youtube-player-core`) |
| Build | Gradle 9.6 (wrapper), AGP 9.4.1, Kotlin DSL en `app` y Groovy en el módulo core |
| SDK | `minSdk 24`, `targetSdk 36`, `compileSdk 37` |
| UI | Activities + Fragments, Navigation Component (drawer), ViewBinding, Material, ViewModel/LiveData |
| Backend | Firebase Auth (Google Sign-In), Cloud Firestore, Crashlytics |
| Reproductor | Módulo local `:youtube-player-core` (fork de *android-youtube-player*, WebView + IFrame API) |
| Cast | `chromecast-sender:0.32` + Play Services Cast Framework, MediaRouter |
| Medios | `androidx.media` (MediaSessionCompat / MediaStyle), Glide para miniaturas |

## Estructura del repositorio

```
My-Music-Video/
├── AGENT.md / CLAUDE.md / README.md
├── build.gradle.kts            # plugins de nivel raíz (AGP, google-services, crashlytics)
├── settings.gradle.kts         # módulos: :app y :youtube-player-core
├── local.properties            # sdk.dir y YOUTUBE_API_KEY (no se versiona)
├── app/                        # aplicación (paquete com.youtube.musica)
│   ├── build.gradle.kts
│   ├── google-services.json    # credenciales de Firebase (no se versiona, ver Configuración)
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/youtube/musica/
│       │   ├── LoginActivity.java            # pantalla inicial: Google Sign-In → Firebase Auth
│       │   ├── MainActivity.java             # drawer + NavHost con los fragments; cerrar sesión
│       │   ├── PlayerFullscreenActivity.java # reproductor principal (ver sección Reproductor)
│       │   ├── CastOptionsProvider.java      # configuración de Cast (receiver app id C5CBE8CA)
│       │   ├── ui/
│       │   │   ├── home/      HomeFragment          # grid de categorías; tocar una = reproducirla
│       │   │   ├── music/     MusicFragment         # "Mi música": categorías + lista con mini-reproductores
│       │   │   ├── recent/    RecentFragment        # "Últimas agregadas": todas las canciones, por fecha desc
│       │   │   ├── youtube/   YouTubeFragment       # WebView de youtube.com; FAB = guardar video actual
│       │   │   │              YoutubeSearchFragment # búsqueda con YouTube Data API v3
│       │   │   └── ctg/       CtgFragment           # CRUD de categorías
│       │   │   (cada carpeta tiene su *ViewModel con MutableLiveData; RecentViewModel tiene alcance de MainActivity)
│       │   ├── adapter/       MusicAdapter (un YouTubePlayerView por item: Mi música y Últimas agregadas),
│       │   │                  VideoAdapter (lista de Inicio), CtgAdapter / CtgAdapterHorizontal,
│       │   │                  YoutubePlaylistAdapter
│       │   ├── dialog/        AddMusic (guardar/editar video), AddCtg, CtgSelectionDialog,
│       │   │                  msgInfo (mensajes), progress (loader)
│       │   ├── firebase/      Firebase (base con FirebaseFirestore), Music, Category
│       │   ├── models/        MusicCollection, CategoryCollection (Serializable), YoutubePlaylist
│       │   ├── interfaces/    PlayerListener, DbMusicListener, MusicListener, CtgListener
│       │   ├── services/      NotificationHelper, PlayerEventBroadcaster, YouTubeBackgroundService
│       │   └── utils/         ChromecastUtil, YouTubePictureInPicture, Constants,
│       │                      idurl/YouTubeHelper + YouTubeHelperTest (extraen el id de una URL)
│       └── res/               layouts, drawables (ic_play, ic_pause, ic_shuffle…), navigation, menus
└── youtube-player-core/       # fork local y modificado de android-youtube-player (Kotlin)
    └── src/main/
        ├── java/com/pierfrancescosoffritti/androidyoutubeplayer/core/player/
        │   ├── views/    YouTubePlayerView, LegacyYouTubePlayerView, WebViewYouTubePlayer
        │   ├── listeners/, options/IFramePlayerOptions, utils/ (PlaybackResumer, NetworkObserver…)
        └── res/raw/ayp_youtube_player.html  # página del IFrame + JS inyectado (salto de anuncios, etc.)
```

## Flujo de la aplicación

1. **`MainActivity`** es el launcher y se puede usar como **invitado**. "Mi música", "Últimas agregadas" y "Categorías" piden iniciar sesión (`AuthUtils.requireLogin` abre `LoginActivity`). Cerrar sesión **no** cierra `MainActivity`: vuelve a Inicio.
2. **`MainActivity`** tiene un drawer con estos destinos (`res/navigation/mobile_navigation.xml`):
   - **Inicio** (`HomeFragment`): las categorías del usuario. Al tocar una se cargan sus videos (`Music.LoadByCtg`) y se abre el reproductor.
   - **Mi música** (`MusicFragment`): elige una categoría y ve sus videos en una lista con reproductores embebidos. El FAB abre el reproductor completo desde la posición y el segundo actuales (`startActivityForResult`).
   - **Últimas agregadas** (`RecentFragment`): las últimas `Constants.RECENT_MUSIC_LIMIT` (50) canciones de todas las categorías, de la más reciente a la más antigua. Los datos vienen de `RecentViewModel` (ver Datos). Se reproducen igual que en "Mi música": en línea, o con el FAB que abre el reproductor completo (segundo plano).
   - **YouTube** (`YouTubeFragment`): navega youtube.com en un WebView. El FAB toma el id del video de la URL actual y abre `AddMusic` para guardarlo en una categoría.
   - **Categorías** (`CtgFragment`): crear, editar y eliminar categorías (antes de borrar comprueba si tienen videos).
   - **Búsqueda YouTube** (`YoutubeSearchFragment`): toque corto = reproducir el resultado; toque largo = guardarlo con `AddMusic`.
3. **`PlayerFullscreenActivity`** (`singleTask`, con soporte PiP) recibe un `Bundle` con:
   - `"list"`: `ArrayList<MusicCollection>`, la lista de reproducción
   - `"position"`: `int`, índice inicial
   - `"minuto"`: `float`, segundo inicial
   - `"categorias"`: `ArrayList<CategoryCollection>`, para cambiar de categoría desde el reproductor

   Al salir con *back* devuelve `minuto` y `position` como resultado.

## Datos (Cloud Firestore)

Todas las consultas filtran por `idUser = FirebaseAuth.getCurrentUser().getUid()`, así que cada usuario solo ve sus propios datos.

| Colección (`Constants`) | Modelo | Campos |
|---|---|---|
| `musics` | `MusicCollection` | `idvideo`, `url`, `name`, `details`, `ctgCode`, `date`, `idUser` (`code` = id del documento) |
| `categories` | `CategoryCollection` | `name`, `description`, `date`, `idUser` (`code` = id del documento) |

Las consultas ordenadas necesitan **índices compuestos** en la colección `musics`:
- `Music.LoadByCtg`: `ctgCode` + `idUser` + `date DESC`
- `Music.listenRecent`: `idUser ASC` + `date DESC`

Si falta un índice, Firestore falla con `FAILED_PRECONDITION`, y el mensaje que sale en Logcat trae el enlace para crearlo.

**Caché en `RecentViewModel`.** `RecentFragment` lo obtiene con `ViewModelProvider(requireActivity())`. Por eso la lista sobrevive a los cambios de pantalla y a la rotación, y Firestore no se vuelve a consultar cada vez que se entra.
- Usa *snapshot listeners* (`Music.listenRecent`, `Category.listenCtg`) en vez de `get()`. Así, cuando se agrega, edita o borra una canción desde cualquier pantalla, la lista se actualiza sola.
- Escucha `FirebaseAuth.AuthStateListener`: si cambia el usuario o se cierra sesión, suelta los listeners y los datos del anterior, porque `MainActivity` sigue viva en modo invitado. Al quedar como invitado entrega una lista vacía. Todo se libera en `onCleared()`.
- Si un listener falla, se libera y se vuelve a intentar la próxima vez que se abre la pantalla. Los resultados vuelven por callbacks (`DbMusicListener`, `CtgListener`), no por LiveData directa.

## Reproductor y reproducción en segundo plano

**`PlayerFullscreenActivity`**
- Usa `YouTubePlayerView` con `enableBackgroundPlayback(true)` y registrado como observer del lifecycle.
- **Modos de reproducción** (se guardan en `SharedPreferences "MusicAppPrefs"`, clave `playback_mode`):
  - `MODE_SEQUENTIAL`
  - `MODE_SHUFFLE`, con historial `shuffleHistory` para que "Anterior" funcione
  - `MODE_REPEAT_ONE`
- Al terminar un video (`ENDED`) o si da error, pasa al siguiente.
- **Reanudación automática**: si el sistema pausa el video con la pantalla apagada o en PiP, se vuelve a dar `play()`. También hay un reintento a los 4 s si se queda en `BUFFERING`. Las dos cosas respetan el flag `isUserIntentionallyPaused`: si el usuario pausó, no se reanuda.
- **PiP**: botón propio, entrada automática al pulsar Home (Android 12+ con `setAutoEnterEnabled`; en 8–11 desde `onUserLeaveHint`), y acciones anterior/play-pausa/siguiente (`YouTubePictureInPicture`).
- **Chromecast** (`ChromecastUtil`): al conectarse pausa el reproductor local y carga el video en el TV. Los controles se redirigen al Cast mientras `isCasting()` sea true.

**`services/`**
- `NotificationHelper` crea la notificación `MediaStyle` y la `MediaSessionCompat`, que da los controles de la pantalla de bloqueo, Bluetooth y relojes. Carga la miniatura con Glide y arranca o actualiza el foreground service.
- `PlayerEventBroadcaster` es un **singleton** con un único `PlayerListener` activo (`register` / `unregisterListener`). Traduce `ACTION_PLAY`, `ACTION_PAUSE`, `ACTION_NEXT`, `ACTION_PREVIOUS` y `ACTION_PLAYBACK_MODE`. Cuando otro reproductor se registra, el anterior recibe `onLostActiveFocus()` y se pausa. Lo implementan `PlayerFullscreenActivity` y el reproductor de `HomeFragment`.
- `YouTubeBackgroundService` es un foreground service de tipo `mediaPlayback`. Retiene un `WakeLock` parcial y un `WifiLock` para que Android no corte la reproducción.

> **Importante:** play y pausa son **acciones separadas a propósito**, no un toggle. El sistema y los dispositivos Bluetooth mandan "pausa" aunque el video ya esté pausado. Con un toggle, esa pausa extra reanudaba el video a los pocos segundos. Cualquier control nuevo (notificación, PiP, MediaSession, widget) debe enviar la acción concreta.

## Módulo `youtube-player-core`

Es un fork local de [android-youtube-player](https://github.com/PierfrancescoSoffritti/android-youtube-player) que se incluye con `implementation(project(":youtube-player-core"))`. La dependencia de Cast excluye el `core` publicado para que no haya clases duplicadas.

- `WebViewYouTubePlayer` ignora `onWindowVisibilityChanged` cuando el background playback está activo, para que el WebView no se pause al salir de la app.
- `LegacyYouTubePlayerView.onStop()` sigue llamando a `pause()` en `ON_STOP`. La activity lo compensa con la reanudación automática descrita arriba.
- `ayp_youtube_player.html` inyecta JS que salta y oculta anuncios, oculta el título y el overlay de pausa.

Si se cambia el comportamiento del reproductor, revisar primero este módulo y no asumir que se comporta como la librería oficial.

## Configuración y compilación

Requisitos (detalle en `README.md`):
1. **Firebase**: poner `app/google-services.json` (paquete `com.youtube.musica`) y `default_web_client_id` en `res/values/strings.xml`. Sin ese archivo **Gradle falla en `processDebugGoogleServices`** y no compila nada.
2. **YouTube Data API v3**: `YOUTUBE_API_KEY="..."` en `local.properties`. Se expone como `BuildConfig.YOUTUBE_API_KEY`.
3. **JDK**: usar el JBR de Android Studio si no hay otro JDK en el sistema.

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug
```

No hay tests reales: `app/src/test` y `app/src/androidTest` solo tienen las plantillas.

## Convenciones del código

- Java 8 con ViewBinding. Comentarios, Javadoc y textos de la UI están en **español**; mantener ese idioma.
- La comunicación entre capas usa **interfaces listener** (`interfaces/`) en vez de LiveData o eventos.
- Los datos pasan entre pantallas como `Serializable` dentro de un `Bundle` (`MusicCollection`, `CategoryCollection`).
- Algunos nombres de métodos están en PascalCase y en español (`NextVideo`, `LoadCtg`, `ShowButton`). Respetar el estilo del archivo que se edite.
- Los iconos de controles siguen el patrón `ic_<acción>` / `ic_<acción>_disabled`.

## Notas y problemas conocidos

- `YouTubeHelperTest` **no es un test**: está en `main` y es la clase que se usa para sacar el id de video de una URL de YouTube.
- `MusicFragment.onActivityResult` lee `"minuto"` con `getIntExtra`, pero el reproductor lo envía como `float`. Por eso siempre recibe `0`.
- El texto de la notificación siempre dice "Reproduciendo", incluso en pausa.
- `MusicAdapter` crea un `YouTubePlayerView` (un WebView) por item, así que las listas largas consumen bastante memoria.
- No se versionan `google-services.json`, `local.properties` ni los `*.jks`.
