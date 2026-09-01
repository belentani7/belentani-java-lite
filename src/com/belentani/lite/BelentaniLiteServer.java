package com.belentani.lite;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/**
 * Belentani Lite Server
 * 
 * Servidor HTTP ligero que unifica y sirve todos los HTMLs locales.
 * Sin dependencias externas - usa solo JDK.
 * 
 * Características:
 * - Sirve todos los HTMLs de Descargas
 * - Página índice con diseño cyberpunk
 * - Soporte para archivos estáticos (CSS, JS, imágenes)
 * - Búsqueda de archivos
 * - Navegación por carpetas
 * 
 * Uso: java com.belentani.lite.BelentaniLiteServer [puerto]
 * 
 * @author Pedro Belentani
 * @version 1.0.0
 */
public class BelentaniLiteServer {

    private static final int DEFAULT_PORT = 8080;
    private static final String DOWNLOADS_PATH = System.getProperty("user.home") + "\\Downloads";
    private static final Map<String, String> MIME_TYPES = new HashMap<>();

    static {
        MIME_TYPES.put("html", "text/html; charset=UTF-8");
        MIME_TYPES.put("htm", "text/html; charset=UTF-8");
        MIME_TYPES.put("css", "text/css; charset=UTF-8");
        MIME_TYPES.put("js", "application/javascript; charset=UTF-8");
        MIME_TYPES.put("json", "application/json; charset=UTF-8");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("gif", "image/gif");
        MIME_TYPES.put("svg", "image/svg+xml");
        MIME_TYPES.put("ico", "image/x-icon");
        MIME_TYPES.put("mp3", "audio/mpeg");
        MIME_TYPES.put("wav", "audio/wav");
        MIME_TYPES.put("mp4", "video/mp4");
        MIME_TYPES.put("webm", "video/webm");
        MIME_TYPES.put("pdf", "application/pdf");
        MIME_TYPES.put("zip", "application/zip");
        MIME_TYPES.put("txt", "text/plain; charset=UTF-8");
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        server.createContext("/", new RootHandler());
        server.createContext("/index", new IndexHandler());
        server.createContext("/search", new SearchHandler());
        server.createContext("/files", new FilesHandler());
        server.createContext("/api/list", new ApiListHandler());
        
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("\n╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║                                                           ║");
        System.out.println("║   BELENTANI LITE SERVER v1.0.0                           ║");
        System.out.println("║                                                           ║");
        System.out.println("║   Servidor iniciado en:                                   ║");
        System.out.println("║   http://localhost:" + port + "                              ║");
        System.out.println("║                                                           ║");
        System.out.println("║   Directorio base:                                        ║");
        System.out.println("║   " + DOWNLOADS_PATH);
        System.out.println("║                                                           ║");
        System.out.println("║   Presiona Ctrl+C para detener                           ║");
        System.out.println("║                                                           ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝\n");
    }

    /**
     * Handler raíz - redirige al índice
     */
    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            
            if (path.equals("/")) {
                serveIndex(exchange);
            } else {
                serveFile(exchange, DOWNLOADS_PATH + path);
            }
        }
    }

    /**
     * Handler del índice principal
     */
    static class IndexHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            serveIndex(exchange);
        }
    }

    /**
     * Handler de búsqueda
     */
    static class SearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String searchTerm = "";
            
            if (query != null && query.contains("q=")) {
                searchTerm = URLDecoder.decode(query.split("q=")[1], "UTF-8");
            }
            
            List<File> results = searchFiles(new File(DOWNLOADS_PATH), searchTerm);
            String html = generateSearchResultsHtml(results, searchTerm);
            
            sendResponse(exchange, 200, "text/html", html);
        }
    }

    /**
     * Handler de listado de archivos
     */
    static class FilesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath().replace("/files", "");
            if (path.isEmpty()) path = "/";
            
            File dir = new File(DOWNLOADS_PATH + path);
            
            if (!dir.exists() || !dir.isDirectory()) {
                sendResponse(exchange, 404, "text/html", "<h1>404 - Directorio no encontrado</h1>");
                return;
            }
            
            String html = generateDirectoryHtml(dir, path);
            sendResponse(exchange, 200, "text/html", html);
        }
    }

    /**
     * Handler API - lista archivos en JSON
     */
    static class ApiListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<Map<String, Object>> files = listFiles(new File(DOWNLOADS_PATH));
            String json = toJson(files);
            sendResponse(exchange, 200, "application/json", json);
        }
    }

    /**
     * Sirve el índice principal con todos los HTMLs
     */
    private static void serveIndex(HttpExchange exchange) throws IOException {
        List<File> htmlFiles = findHtmlFiles(new File(DOWNLOADS_PATH));
        String html = generateIndexHtml(htmlFiles);
        sendResponse(exchange, 200, "text/html", html);
    }

    /**
     * Sirve un archivo estático
     */
    private static void serveFile(HttpExchange exchange, String filePath) throws IOException {
        File file = new File(filePath);
        
        if (!file.exists()) {
            sendResponse(exchange, 404, "text/html", "<h1>404 - Archivo no encontrado</h1>");
            return;
        }
        
        if (file.isDirectory()) {
            String html = generateDirectoryHtml(file, exchange.getRequestURI().getPath());
            sendResponse(exchange, 200, "text/html", html);
            return;
        }
        
        String extension = getExtension(file.getName());
        String mimeType = MIME_TYPES.getOrDefault(extension, "application/octet-stream");
        
        byte[] bytes = Files.readAllBytes(file.toPath());
        exchange.getResponseHeaders().set("Content-Type", mimeType);
        exchange.sendResponseHeaders(200, bytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * Busca archivos HTML recursivamente
     */
    private static List<File> findHtmlFiles(File dir) {
        List<File> htmlFiles = new ArrayList<>();
        findHtmlFilesRecursive(dir, htmlFiles);
        return htmlFiles;
    }

    private static void findHtmlFilesRecursive(File dir, List<File> htmlFiles) {
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                findHtmlFilesRecursive(file, htmlFiles);
            } else if (file.getName().toLowerCase().endsWith(".html") || 
                       file.getName().toLowerCase().endsWith(".htm")) {
                htmlFiles.add(file);
            }
        }
    }

    /**
     * Busca archivos por nombre
     */
    private static List<File> searchFiles(File dir, String searchTerm) {
        List<File> results = new ArrayList<>();
        if (searchTerm.isEmpty()) return results;
        
        searchFilesRecursive(dir, searchTerm.toLowerCase(), results);
        return results;
    }

    private static void searchFilesRecursive(File dir, String searchTerm, List<File> results) {
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.getName().toLowerCase().contains(searchTerm)) {
                results.add(file);
            }
            if (file.isDirectory()) {
                searchFilesRecursive(file, searchTerm, results);
            }
        }
    }

    /**
     * Lista archivos de un directorio
     */
    private static List<Map<String, Object>> listFiles(File dir) {
        List<Map<String, Object>> files = new ArrayList<>();
        File[] items = dir.listFiles();
        
        if (items == null) return files;
        
        for (File file : items) {
            Map<String, Object> fileInfo = new HashMap<>();
            fileInfo.put("name", file.getName());
            fileInfo.put("path", file.getAbsolutePath().replace(DOWNLOADS_PATH, ""));
            fileInfo.put("isDirectory", file.isDirectory());
            fileInfo.put("size", file.length());
            fileInfo.put("lastModified", file.lastModified());
            files.add(fileInfo);
        }
        
        return files;
    }

    /**
     * Genera el HTML del índice principal
     */
    private static String generateIndexHtml(List<File> htmlFiles) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang='es'>\n");
        html.append("<head>\n");
        html.append("<meta charset='UTF-8'>\n");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
        html.append("<title>BELENTANI LITE - Servidor de Archivos</title>\n");
        html.append("<style>\n");
        html.append(getCyberpunkCSS());
        html.append("</style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        
        // Header
        html.append("<header>\n");
        html.append("  <div class='brand'>BELENTANI <span>LITE</span></div>\n");
        html.append("  <div class='stats'>").append(htmlFiles.size()).append(" HTMLs encontrados</div>\n");
        html.append("</header>\n");
        
        // Search
        html.append("<div class='search-container'>\n");
        html.append("  <input type='text' id='searchInput' placeholder='Buscar archivos...' onkeyup='searchFiles()'>\n");
        html.append("  <button onclick='searchFiles()'>🔍 Buscar</button>\n");
        html.append("</div>\n");
        
        // File grid
        html.append("<div class='container'>\n");
        html.append("  <h2>📄 Documentos HTML</h2>\n");
        html.append("  <div class='grid'>\n");
        
        for (File file : htmlFiles) {
            String relativePath = file.getAbsolutePath().replace(DOWNLOADS_PATH, "").replace("\\", "/");
            String fileName = file.getName();
            String fileSize = formatFileSize(file.length());
            String lastModified = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(file.lastModified()));
            
            html.append("    <div class='card'>\n");
            html.append("      <div class='card-icon'>📄</div>\n");
            html.append("      <div class='card-title'>").append(fileName).append("</div>\n");
            html.append("      <div class='card-info'>").append(fileSize).append(" | ").append(lastModified).append("</div>\n");
            html.append("      <a href='").append(relativePath).append("' class='card-link' target='_blank'>Abrir</a>\n");
            html.append("    </div>\n");
        }
        
        html.append("  </div>\n");
        
        // Quick links
        html.append("  <h2>🔗 Accesos Rápidos</h2>\n");
        html.append("  <div class='quick-links'>\n");
        html.append("    <a href='/files/' class='quick-link'>📁 Explorar Archivos</a>\n");
        html.append("    <a href='/api/list' class='quick-link'>📊 API JSON</a>\n");
        html.append("    <a href='/search?q=judas' class='quick-link'>🎵 Judas Experience</a>\n");
        html.append("    <a href='/search?q=manos' class='quick-link'>📚 Manos Abiertas</a>\n");
        html.append("  </div>\n");
        
        html.append("</div>\n");
        
        // Footer
        html.append("<footer>\n");
        html.append("  <p>BELENTANI LITE SERVER v1.0.0 | Pedro Belentani | 2026</p>\n");
        html.append("</footer>\n");
        
        // JavaScript
        html.append("<script>\n");
        html.append(getSearchJS());
        html.append("</script>\n");
        
        html.append("</body>\n");
        html.append("</html>\n");
        
        return html.toString();
    }

    /**
     * Genera HTML para listado de directorio
     */
    private static String generateDirectoryHtml(File dir, String path) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang='es'>\n");
        html.append("<head>\n");
        html.append("<meta charset='UTF-8'>\n");
        html.append("<title>BELENTANI LITE - ").append(path).append("</title>\n");
        html.append("<style>\n");
        html.append(getCyberpunkCSS());
        html.append("</style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        
        html.append("<header>\n");
        html.append("  <div class='brand'>BELENTANI <span>LITE</span></div>\n");
        html.append("  <div class='path'>📁 ").append(path).append("</div>\n");
        html.append("</header>\n");
        
        html.append("<div class='container'>\n");
        html.append("  <h2>Contenido del Directorio</h2>\n");
        html.append("  <div class='file-list'>\n");
        
        // Parent directory link
        if (!path.equals("/")) {
            String parentPath = path.substring(0, path.lastIndexOf('/', path.length() - 2) + 1);
            html.append("    <div class='file-item'>\n");
            html.append("      <span class='file-icon'>📁</span>\n");
            html.append("      <a href='/files").append(parentPath).append("'>..</a>\n");
            html.append("    </div>\n");
        }
        
        File[] files = dir.listFiles();
        if (files != null) {
            Arrays.sort(files, (a, b) -> {
                if (a.isDirectory() && !b.isDirectory()) return -1;
                if (!a.isDirectory() && b.isDirectory()) return 1;
                return a.getName().compareToIgnoreCase(b.getName());
            });
            
            for (File file : files) {
                String icon = file.isDirectory() ? "📁" : getFileIcon(file.getName());
                String fileSize = file.isDirectory() ? "-" : formatFileSize(file.length());
                String link = "/files" + path + (path.endsWith("/") ? "" : "/") + file.getName();
                
                html.append("    <div class='file-item'>\n");
                html.append("      <span class='file-icon'>").append(icon).append("</span>\n");
                html.append("      <a href='").append(link).append("'>").append(file.getName()).append("</a>\n");
                html.append("      <span class='file-size'>").append(fileSize).append("</span>\n");
                html.append("    </div>\n");
            }
        }
        
        html.append("  </div>\n");
        html.append("</div>\n");
        
        html.append("<footer>\n");
        html.append("  <a href='/'>← Volver al inicio</a>\n");
        html.append("</footer>\n");
        
        html.append("</body>\n");
        html.append("</html>\n");
        
        return html.toString();
    }

    /**
     * Genera HTML para resultados de búsqueda
     */
    private static String generateSearchResultsHtml(List<File> results, String searchTerm) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang='es'>\n");
        html.append("<head>\n");
        html.append("<meta charset='UTF-8'>\n");
        html.append("<title>BELENTANI LITE - Búsqueda: ").append(searchTerm).append("</title>\n");
        html.append("<style>\n");
        html.append(getCyberpunkCSS());
        html.append("</style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        
        html.append("<header>\n");
        html.append("  <div class='brand'>BELENTANI <span>LITE</span></div>\n");
        html.append("  <div class='stats'>").append(results.size()).append(" resultados para '").append(searchTerm).append("'</div>\n");
        html.append("</header>\n");
        
        html.append("<div class='container'>\n");
        html.append("  <h2>Resultados de Búsqueda</h2>\n");
        
        if (results.isEmpty()) {
            html.append("  <p class='no-results'>No se encontraron archivos.</p>\n");
        } else {
            html.append("  <div class='file-list'>\n");
            for (File file : results) {
                String relativePath = file.getAbsolutePath().replace(DOWNLOADS_PATH, "").replace("\\", "/");
                String icon = file.isDirectory() ? "📁" : getFileIcon(file.getName());
                String fileSize = file.isDirectory() ? "-" : formatFileSize(file.length());
                
                html.append("    <div class='file-item'>\n");
                html.append("      <span class='file-icon'>").append(icon).append("</span>\n");
                html.append("      <a href='").append(relativePath).append("'>").append(file.getName()).append("</a>\n");
                html.append("      <span class='file-size'>").append(fileSize).append("</span>\n");
                html.append("    </div>\n");
            }
            html.append("  </div>\n");
        }
        
        html.append("</div>\n");
        
        html.append("<footer>\n");
        html.append("  <a href='/'>← Volver al inicio</a>\n");
        html.append("</footer>\n");
        
        html.append("</body>\n");
        html.append("</html>\n");
        
        return html.toString();
    }

    /**
     * CSS cyberpunk para el diseño
     */
    private static String getCyberpunkCSS() {
        return "* { margin: 0; padding: 0; box-sizing: border-box; }\n" +
            "body {\n" +
            "  font-family: 'Courier New', monospace;\n" +
            "  background: #0a0a0a;\n" +
            "  color: #e0e0e0;\n" +
            "  min-height: 100vh;\n" +
            "}\n" +
            "header {\n" +
            "  background: linear-gradient(135deg, #1a1a1a 0%, #0a0a0a 100%);\n" +
            "  border-bottom: 2px solid #ff0033;\n" +
            "  padding: 20px 40px;\n" +
            "  display: flex;\n" +
            "  justify-content: space-between;\n" +
            "  align-items: center;\n" +
            "  box-shadow: 0 4px 20px rgba(255, 0, 51, 0.3);\n" +
            "}\n" +
            ".brand {\n" +
            "  font-size: 24px;\n" +
            "  font-weight: bold;\n" +
            "  color: #fff;\n" +
            "  text-shadow: 0 0 10px rgba(255, 0, 51, 0.5);\n" +
            "}\n" +
            ".brand span {\n" +
            "  color: #ff0033;\n" +
            "  font-size: 14px;\n" +
            "  margin-left: 8px;\n" +
            "}\n" +
            ".stats, .path {\n" +
            "  color: #888;\n" +
            "  font-size: 14px;\n" +
            "}\n" +
            ".search-container {\n" +
            "  max-width: 1200px;\n" +
            "  margin: 30px auto;\n" +
            "  padding: 0 40px;\n" +
            "  display: flex;\n" +
            "  gap: 10px;\n" +
            "}\n" +
            "#searchInput {\n" +
            "  flex: 1;\n" +
            "  padding: 12px 20px;\n" +
            "  background: #1a1a1a;\n" +
            "  border: 2px solid #333;\n" +
            "  color: #fff;\n" +
            "  font-size: 16px;\n" +
            "  border-radius: 4px;\n" +
            "}\n" +
            "#searchInput:focus {\n" +
            "  outline: none;\n" +
            "  border-color: #ff0033;\n" +
            "  box-shadow: 0 0 10px rgba(255, 0, 51, 0.3);\n" +
            "}\n" +
            "button {\n" +
            "  padding: 12px 24px;\n" +
            "  background: #ff0033;\n" +
            "  border: none;\n" +
            "  color: #fff;\n" +
            "  font-size: 16px;\n" +
            "  cursor: pointer;\n" +
            "  border-radius: 4px;\n" +
            "  transition: all 0.3s;\n" +
            "}\n" +
            "button:hover {\n" +
            "  background: #cc0029;\n" +
            "  box-shadow: 0 0 20px rgba(255, 0, 51, 0.5);\n" +
            "}\n" +
            ".container {\n" +
            "  max-width: 1200px;\n" +
            "  margin: 0 auto;\n" +
            "  padding: 40px;\n" +
            "}\n" +
            "h2 {\n" +
            "  color: #ff0033;\n" +
            "  margin-bottom: 20px;\n" +
            "  font-size: 20px;\n" +
            "  text-transform: uppercase;\n" +
            "  letter-spacing: 2px;\n" +
            "}\n" +
            ".grid {\n" +
            "  display: grid;\n" +
            "  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));\n" +
            "  gap: 20px;\n" +
            "  margin-bottom: 40px;\n" +
            "}\n" +
            ".card {\n" +
            "  background: #1a1a1a;\n" +
            "  border: 1px solid #333;\n" +
            "  border-radius: 8px;\n" +
            "  padding: 20px;\n" +
            "  transition: all 0.3s;\n" +
            "}\n" +
            ".card:hover {\n" +
            "  border-color: #ff0033;\n" +
            "  transform: translateY(-5px);\n" +
            "  box-shadow: 0 10px 30px rgba(255, 0, 51, 0.2);\n" +
            "}\n" +
            ".card-icon {\n" +
            "  font-size: 48px;\n" +
            "  margin-bottom: 10px;\n" +
            "}\n" +
            ".card-title {\n" +
            "  font-size: 16px;\n" +
            "  font-weight: bold;\n" +
            "  color: #fff;\n" +
            "  margin-bottom: 8px;\n" +
            "  word-break: break-word;\n" +
            "}\n" +
            ".card-info {\n" +
            "  font-size: 12px;\n" +
            "  color: #888;\n" +
            "  margin-bottom: 15px;\n" +
            "}\n" +
            ".card-link {\n" +
            "  display: inline-block;\n" +
            "  padding: 8px 16px;\n" +
            "  background: #ff0033;\n" +
            "  color: #fff;\n" +
            "  text-decoration: none;\n" +
            "  border-radius: 4px;\n" +
            "  font-size: 14px;\n" +
            "  transition: all 0.3s;\n" +
            "}\n" +
            ".card-link:hover {\n" +
            "  background: #cc0029;\n" +
            "}\n" +
            ".quick-links {\n" +
            "  display: flex;\n" +
            "  gap: 15px;\n" +
            "  flex-wrap: wrap;\n" +
            "  margin-bottom: 40px;\n" +
            "}\n" +
            ".quick-link {\n" +
            "  padding: 12px 24px;\n" +
            "  background: #1a1a1a;\n" +
            "  border: 1px solid #333;\n" +
            "  color: #fff;\n" +
            "  text-decoration: none;\n" +
            "  border-radius: 4px;\n" +
            "  transition: all 0.3s;\n" +
            "}\n" +
            ".quick-link:hover {\n" +
            "  border-color: #ff0033;\n" +
            "  background: #2a2a2a;\n" +
            "}\n" +
            ".file-list {\n" +
            "  background: #1a1a1a;\n" +
            "  border: 1px solid #333;\n" +
            "  border-radius: 8px;\n" +
            "  overflow: hidden;\n" +
            "}\n" +
            ".file-item {\n" +
            "  display: flex;\n" +
            "  align-items: center;\n" +
            "  padding: 15px 20px;\n" +
            "  border-bottom: 1px solid #333;\n" +
            "  transition: all 0.3s;\n" +
            "}\n" +
            ".file-item:last-child {\n" +
            "  border-bottom: none;\n" +
            "}\n" +
            ".file-item:hover {\n" +
            "  background: #2a2a2a;\n" +
            "}\n" +
            ".file-icon {\n" +
            "  font-size: 24px;\n" +
            "  margin-right: 15px;\n" +
            "}\n" +
            ".file-item a {\n" +
            "  flex: 1;\n" +
            "  color: #fff;\n" +
            "  text-decoration: none;\n" +
            "  font-size: 16px;\n" +
            "}\n" +
            ".file-item a:hover {\n" +
            "  color: #ff0033;\n" +
            "}\n" +
            ".file-size {\n" +
            "  color: #888;\n" +
            "  font-size: 14px;\n" +
            "  margin-left: 20px;\n" +
            "}\n" +
            "footer {\n" +
            "  text-align: center;\n" +
            "  padding: 40px;\n" +
            "  color: #888;\n" +
            "  border-top: 1px solid #333;\n" +
            "  margin-top: 40px;\n" +
            "}\n" +
            "footer a {\n" +
            "  color: #ff0033;\n" +
            "  text-decoration: none;\n" +
            "}\n" +
            ".no-results {\n" +
            "  text-align: center;\n" +
            "  padding: 40px;\n" +
            "  color: #888;\n" +
            "  font-size: 18px;\n" +
            "}\n";
    }

    /**
     * JavaScript para búsqueda
     */
    private static String getSearchJS() {
        return "function searchFiles() {\n" +
            "  const searchTerm = document.getElementById('searchInput').value;\n" +
            "  if (searchTerm.length > 0) {\n" +
            "    window.location.href = '/search?q=' + encodeURIComponent(searchTerm);\n" +
            "  }\n" +
            "}\n";
    }

    /**
     * Envía una respuesta HTTP
     */
    private static void sendResponse(HttpExchange exchange, int statusCode, String contentType, String content) throws IOException {
        byte[] bytes = content.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * Obtiene la extensión de un archivo
     */
    private static String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1).toLowerCase() : "";
    }

    /**
     * Obtiene el icono según el tipo de archivo
     */
    private static String getFileIcon(String filename) {
        String ext = getExtension(filename);
        switch (ext) {
            case "html": case "htm": return "📄";
            case "css": return "🎨";
            case "js": return "⚡";
            case "json": return "📋";
            case "png": case "jpg": case "jpeg": case "gif": case "svg": return "🖼️";
            case "mp3": case "wav": return "🎵";
            case "mp4": case "webm": return "🎬";
            case "pdf": return "📕";
            case "zip": case "rar": case "7z": return "📦";
            case "exe": case "msi": return "⚙️";
            case "txt": case "md": return "📝";
            default: return "📎";
        }
    }

    /**
     * Formatea el tamaño del archivo
     */
    private static String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    /**
     * Convierte lista de mapas a JSON
     */
    private static String toJson(List<Map<String, Object>> list) {
        StringBuilder json = new StringBuilder("[\n");
        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> map = list.get(i);
            json.append("  {\n");
            int j = 0;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                json.append("    \"").append(entry.getKey()).append("\": ");
                Object value = entry.getValue();
                if (value instanceof String) {
                    json.append("\"").append(value.toString().replace("\"", "\\\"")).append("\"");
                } else if (value instanceof Boolean) {
                    json.append(value.toString());
                } else {
                    json.append(value.toString());
                }
                if (j < map.size() - 1) json.append(",");
                json.append("\n");
                j++;
            }
            json.append("  }");
            if (i < list.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("]");
        return json.toString();
    }
}
