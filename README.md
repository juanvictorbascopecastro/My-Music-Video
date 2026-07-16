# 🎥 App de Videos Favoritos

Es una aplicación que te permite reproducir videos de YouTube y guardar tus videos preferidos en una base de datos, organizados por categoría. Además, cuenta con un reproductor propio que te permite reproducir los videos en segundo plano. ¿Cansado de pausar la reproducción al cambiar de aplicación? ¡Con nuestro reproductor propio, puedes reproducir tus videos favoritos en segundo plano mientras accedes a otras aplicaciones!

<img src="https://github.com/juanvictorbascopecastro/My-Music-Video/assets/43118668/22ca399c-f556-4214-bafa-a3fc65a84a09" width="290">
<img src="https://github.com/juanvictorbascopecastro/My-Music-Video/assets/43118668/a7d722c5-ec1c-4411-b7a4-19b05aa121d8" width="290">
<img src="https://github.com/juanvictorbascopecastro/My-Music-Video/assets/43118668/04d85da4-e741-42ca-8ea5-9d028d0a363e" width="290">
<img src="https://github.com/juanvictorbascopecastro/My-Music-Video/assets/43118668/b168e3d4-6aa2-4f91-87e7-bd6d6813146d" width="290">

SHA64
./gradlew assembleDebug

## ⚙️ Configuración Necesaria para Funcionar

Para que este proyecto compile y funcione correctamente en un entorno local, necesitas configurar lo siguiente:

### 1. Firebase (Google Sign-In)
La aplicación utiliza Google Sign-In mediante Firebase Authentication. Necesitas proveer tus propias credenciales:
- Debes crear un proyecto en Firebase y registrar la aplicación Android (`com.youtube.musica`).
- Descarga el archivo **`google-services.json`** y colócalo en la carpeta `/app/`.
- En tu archivo `/app/src/main/res/values/strings.xml`, debes agregar el Web Client ID generado por Firebase para que el login funcione:
  ```xml
  <string name="default_web_client_id">TU_WEB_CLIENT_ID.apps.googleusercontent.com</string>
  ```
- No olvides habilitar el método de inicio de sesión con Google en la consola de Firebase y añadir el SHA-1/SHA-256 de tu clave de firma (Debug/Release).

### 2. YouTube Data API v3
Para cargar listas de reproducción y buscar videos (como en el apartado "Mis Videos"), la app usa la **YouTube Data API v3**:
- Debes ir a [Google Cloud Console](https://console.cloud.google.com/), habilitar la *YouTube Data API v3*.
- Generar una Clave de API (API Key) y restringirla para que solo se use con esta API.
- Abre tu archivo `local.properties` en la raíz del proyecto y agrega tu llave como una variable de entorno:
  ```properties
  YOUTUBE_API_KEY="TU_API_KEY_AQUI"
  ```
*(Nota: Para acceder a listas privadas `mine=true`, se requiere implementar autenticación OAuth2 y proveer un Access Token en la petición).*

### 3. Reproductor Local y Workarounds (Bloqueo de Anuncios)
El reproductor de YouTube no se extrae de internet, sino que es **un módulo local** (`:youtube-player-core`) incluido en este repositorio.
Este módulo modificado contiene *Workarounds* en el archivo `ayp_youtube_player.html` mediante inyección de JavaScript puro para:
- Bloquear y saltar anuncios automáticamente.
- Ocultar títulos e interfaces intrusivas de la pantalla.
- Compartir sesiones de visualización.

Dado que es una modificación a nivel de WebView para uso personal, **no se recomienda** subir esta versión a Google Play Store para evitar infracciones a los Términos de Servicio de YouTube.
