# BELENTANI LITE SERVER

Servidor HTTP ligero en Java puro que unifica y sirve todos los HTMLs locales.

## Características

- ✅ **Sin dependencias externas** - Solo JDK
- ✅ **Servidor HTTP completo** - Usa `com.sun.net.httpserver`
- ✅ **Unifica HTMLs** - Sirve todos los archivos HTML de Descargas
- ✅ **Búsqueda integrada** - Busca archivos por nombre
- ✅ **Navegación de carpetas** - Explora el sistema de archivos
- ✅ **Diseño cyberpunk** - Interfaz moderna con tema rojo/negro
- ✅ **API JSON** - Endpoint para listar archivos programáticamente
- ✅ **Soporte multimedia** - HTML, CSS, JS, imágenes, audio, video

## Requisitos

- Java 8 o superior
- No requiere Maven, Gradle ni dependencias externas

## Instalación

1. Asegúrate de tener Java instalado:
   ```bash
   java -version
   ```

2. Compila el proyecto (si no está compilado):
   ```bash
   javac -encoding UTF-8 -d out src/com/belentani/lite/BelentaniLiteServer.java
   ```

## Uso

### Opción 1: Script de inicio (Windows)
```bash
start.bat
```

### Opción 2: Comando directo
```bash
java -cp out com.belentani.lite.BelentaniLiteServer
```

### Opción 3: Puerto personalizado
```bash
java -cp out com.belentani.lite.BelentaniLiteServer 9090
```

El servidor se iniciará en `http://localhost:8080` (o el puerto que especifiques).

## Endpoints

### `/` o `/index`
Página principal con todos los HTMLs encontrados en Descargas.

### `/files/[ruta]`
Navega por el sistema de archivos.
- `/files/` - Lista el directorio raíz (Descargas)
- `/files/subcarpeta/` - Lista una subcarpeta

### `/search?q=[término]`
Busca archivos por nombre.
- Ejemplo: `/search?q=judas`

### `/api/list`
API JSON que lista todos los archivos con metadata:
```json
[
  {
    "name": "archivo.html",
    "path": "/archivo.html",
    "isDirectory": false,
    "size": 12345,
    "lastModified": 1704067200000
  }
]
```

### `/[ruta-relativa]`
Sirve cualquier archivo estático directamente.
- Ejemplo: `/02_mascara.html`

## Estructura del Proyecto

```
belentani-java-lite/
├── src/
│   └── com/
│       └── belentani/
│           └── lite/
│               └── BelentaniLiteServer.java  (código fuente)
├── out/
│   └── com/
│       └── belentani/
│           └── lite/
│               └── BelentaniLiteServer.class  (compilado)
├── start.bat  (script de inicio Windows)
└── README.md
```

## Tipos de Archivo Soportados

- **Documentos**: HTML, HTM, PDF, TXT, MD
- **Estilos**: CSS
- **Scripts**: JS, JSON
- **Imágenes**: PNG, JPG, JPEG, GIF, SVG, ICO
- **Audio**: MP3, WAV
- **Video**: MP4, WEBM
- **Comprimidos**: ZIP, RAR, 7Z

## Características Técnicas

### Servidor HTTP
- Usa `com.sun.net.httpserver.HttpServer` (incluido en JDK)
- Thread pool de 10 hilos concurrentes
- Soporte para GET requests
- Manejo de MIME types automático

### Seguridad
- Solo sirve archivos del directorio Descargas
- No permite acceso fuera del directorio base
- Validación de rutas

### Rendimiento
- Lectura eficiente de archivos
- Cache de MIME types
- Búsqueda recursiva optimizada

## Personalización

### Cambiar directorio base
Edita `BelentaniLiteServer.java` línea 20:
```java
private static final String DOWNLOADS_PATH = "C:\\tu\\ruta\\personalizada";
```

### Cambiar puerto por defecto
Edita línea 19:
```java
private static final int DEFAULT_PORT = 9090; // tu puerto preferido
```

### Agregar más MIME types
Edita el bloque `static` (líneas 22-40):
```java
MIME_TYPES.put("nuevo_tipo", "mime/type");
```

## Ejemplos de Uso

### Abrir el servidor
```bash
start.bat
```
Luego abre: http://localhost:8080

### Buscar archivos
```
http://localhost:8080/search?q=manos
```

### Navegar carpetas
```
http://localhost:8080/files/
```

### Abrir HTML específico
```
http://localhost:8080/02_mascara.html
```

### Obtener lista JSON
```bash
curl http://localhost:8080/api/list
```

## Solución de Problemas

### Error: "No se puede encontrar o cargar el archivo de clase"
Asegúrate de compilar primero:
```bash
javac -encoding UTF-8 -d out src/com/belentani/lite/BelentaniLiteServer.java
```

### Error: "Puerto ya en uso"
Cambia el puerto:
```bash
java -cp out com.belentani.lite.BelentaniLiteServer 9090
```

### Error: "Permiso denegado"
Ejecuta como administrador o cambia el directorio base.

### Los caracteres especiales no se muestran
Asegúrate de compilar con `-encoding UTF-8`.

## Comparación con Versión Completa

| Característica | LITE | Completa (Spring Boot) |
|----------------|------|------------------------|
| Dependencias | ❌ Ninguna | ✅ Spring Boot, JPA, Security |
| Base de datos | ❌ No | ✅ PostgreSQL/H2 |
| Autenticación | ❌ No | ✅ JWT, Roles |
| API REST | ✅ Básica | ✅ Completa |
| Caché | ❌ No | ✅ Redis/Caffeine |
| Tamaño JAR | ~50 KB | ~50 MB |
| Tiempo inicio | <1s | ~5s |
| Uso memoria | ~20 MB | ~200 MB |

## Licencia

MIT License - Úsalo libremente.

## Autor

**Pedro Belentani** - 2026

Parte del ecosistema Belentani.
