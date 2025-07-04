import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.net.InetSocketAddress;
import java.net.URLDecoder;

public class URLShortener
{
    public static final Map<String, String> shortCodeToUrl = new HashMap<>();
    public static final Map<String, String> urlToShortCode = new HashMap<>();
    public static final String filePath = "paths";

    public static void main(String[] args) 
    {
        loadUrlsFromFile(filePath);
        try 
        {
            HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
            server.createContext("/", new RedirectHandler());
            server.createContext("/add", new EntryHandler());
            server.setExecutor(null);
            server.start();
            System.out.println("Server is running on port 8000");
        } 
        catch (IOException e) 
        {
            System.out.println("Error starting the server: " + e.getMessage());
        }
    }

    static class RedirectHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException 
        {
            String path = exchange.getRequestURI().getPath();
            System.out.println("Path: " + path);
            String code = "";
            if (path.length() > 1)
            {
                code = path.substring(1);
                System.out.println("Short code: " + code);
                String longUrl = shortCodeToUrl.get(code);
                System.out.println("Long URL: " + longUrl);

                if (longUrl != null) 
                {
                    System.out.println("Redirecting '" + code + "' to: '" + longUrl + "'");
                    exchange.getResponseHeaders().set("Location", longUrl);
                    exchange.sendResponseHeaders(302, -1);
                } 
                else 
                {
                    System.out.println("Short code '" + code + "' not found.");
                    String response = "Short URL not found: " + code;
                    exchange.sendResponseHeaders(404, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
                exchange.close();
            }
        }
    }

    static class EntryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException 
        {
            String response;
            String query = exchange.getRequestURI().getQuery();
            int statusCode = 400;

            if (query != null && query.startsWith("url=")) 
            {
                String encodedUrl = query.substring(4);
                String originalUrl = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.name());

                String existingShortCode = urlToShortCode.get(originalUrl);
                if (existingShortCode != null) 
                {
                    response = "URL already exists with short code: " + existingShortCode;
                    statusCode = 200;
                }
                else 
                {
                    String newShortCode;
                    do 
                    {
                        newShortCode = urlGenerator();
                    }
                    while (shortCodeToUrl.containsKey(newShortCode));

                    shortCodeToUrl.put(newShortCode, originalUrl);
                    urlToShortCode.put(originalUrl, newShortCode);

                    try 
                    {
                        String lineToAppend = newShortCode + " " + originalUrl;
                        Files.write(Paths.get(filePath), (lineToAppend + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        response = "New URL added. Short code: " + newShortCode;
                        statusCode = 200;
                    } 
                    catch (IOException e) 
                    {
                        response = "Error writing URL to file: " + e.getMessage();
                        statusCode = 500;
                        e.printStackTrace();
                    }
                }
            }
            else 
            {
                response = "Missing 'url' parameter. Usage: /add?url=<YOUR_URL>";
            }

            exchange.sendResponseHeaders(statusCode, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        }
    }

    static void loadUrlsFromFile(String filePath) 
    {
        if (!Files.exists(Paths.get(filePath))) 
        {
            System.out.println("URLs file does not exist. Starting fresh.");
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath), StandardCharsets.UTF_8)) 
        {
            String line;
            while ((line = reader.readLine()) != null) 
            {
                String[] parts = line.split(" ", 2);
                if (parts.length == 2) 
                {
                    String shortCode = parts[0];
                    String originalUrl = parts[1];
                    shortCodeToUrl.put(shortCode, originalUrl);
                    urlToShortCode.put(originalUrl, shortCode);
                }
            }
        } 
        catch (IOException e) 
        {
            System.err.println("Error loading URLs from file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    static String urlGenerator()
    {
        String alphanum = "abcdefghijklmnopqrstuvwyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random rnd = new Random();
        while (sb.length() < 7) 
        {
            int index = rnd.nextInt(alphanum.length() + 1);
            sb.append(alphanum.charAt(index));
        }
        String result = sb.toString();
        return result;
    }
}