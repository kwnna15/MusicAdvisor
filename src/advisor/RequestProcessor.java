package advisor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class RequestProcessor {
    private String request;
    private boolean authorized = false;
    private final String uriBasePath;
    private final String APIserverPath;
    private final SpotifyRemoteService spotifyRemoteService;
    private HashMap<String, String> categoryIds;

    RequestProcessor(boolean authorized, String SpotifyServerPath, String apiServerPath) {
        this.authorized = authorized;
        if (SpotifyServerPath.isEmpty()) {
            uriBasePath = "https://accounts.spotify.com";
        } else {
            uriBasePath = SpotifyServerPath;
        }

        if (apiServerPath.isEmpty()) {
            APIserverPath = "https://api.spotify.com";
        } else {
            APIserverPath = apiServerPath;
        }
        spotifyRemoteService = new SpotifyRemoteService(uriBasePath, APIserverPath);
        categoryIds = new HashMap<>();
    }

    protected boolean getAuthorized() {
        return authorized;
    }

    public void getUserRequest() {
        Scanner scanner = new Scanner(System.in);
        request = scanner.nextLine();
    }

    public boolean processRequest() throws IOException {
        if (!request.equals("auth") && !request.equals("exit")) {
            if (!isAuthorized()) {
                return true;
            }

        }
        HttpServer server = HttpServer.create();
        BlockingDeque<String> accessCodeQueue = new LinkedBlockingDeque<>();

        if (request.startsWith("playlists")) {
            String categoryName = request.substring(10);
            System.out.println(categoryName);

            try {
                String categoriesOutput = spotifyRemoteService.getCategories();
                categoryIds = parseCategoryIDsJson(categoriesOutput);
                if (categoryIds.containsKey(categoryName)) {
                    String categoryId = categoryIds.get(categoryName);
                    List<String> featured = parsePlaylistsJson(spotifyRemoteService.getPlaylists(categoryId));
                    for (String i : featured) {
                        System.out.println(i);
                    }
                } else {
                    System.out.println("Unknown category name.");
                }

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return true;
        }

        switch (request) {
            case "new":
                try {
                    List<String> newReleases = parseNewReleasesJson(spotifyRemoteService.getNewReleases());
                    for (String i : newReleases) {
                        System.out.println(i);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                break;
            case "featured":
                try {
                    List<String> featured = parseFeaturedJson(spotifyRemoteService.getFeatured());
                    for (String i : featured) {
                        System.out.println(i);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                break;
            case "categories":
                try {
                    String categoriesOutput = spotifyRemoteService.getCategories();
                    List<String> categories = parseCategoriesJson(categoriesOutput);
                    for (String i : categories) {
                        System.out.println(i);
                    }
                    categoryIds = parseCategoryIDsJson(categoriesOutput);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                break;

            case "auth":

                server.bind(new InetSocketAddress(8080), 0);
                server.createContext("/", new MusicAdvisorHttpServer(spotifyRemoteService, accessCodeQueue));

                server.start();
                System.out.println("use this link to request the access code:");
                System.out.println(uriBasePath + "/authorize?client_id=c4d2c4750f884f688109440ead8ceda8&redirect_uri=http://localhost:8080&response_type=code");


                while (!authorized) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                server.stop(1);

                break;
            case "exit":
                return false;
            default:
                throw new IllegalStateException("Unexpected value: " + request);
        }
        return true;
    }

    private boolean isAuthorized() {
        if (authorized) {
            return true;
        }
        System.out.println("Please, provide access for application.");
        return false;
    }

    private List<String> parseCategoriesJson(String json) {
        JsonObject jo = JsonParser.parseString(json).getAsJsonObject();
        JsonObject categoriesObj = jo.getAsJsonObject("categories");
        List<String> categories = new ArrayList<>();
        for (JsonElement item : categoriesObj.getAsJsonArray("items")) {
            categories.add(item.getAsJsonObject().get("name").toString().replaceAll("\"", ""));
        }
        return categories;
    }

    private HashMap<String, String> parseCategoryIDsJson(String json) {
        JsonObject jo = JsonParser.parseString(json).getAsJsonObject();
        JsonObject categoriesObj = jo.getAsJsonObject("categories");
        HashMap<String, String> categoryIds = new HashMap<>();
        for (JsonElement item : categoriesObj.getAsJsonArray("items")) {
            categoryIds.put(item.getAsJsonObject().get("name").toString().replaceAll("\"", ""), item.getAsJsonObject().get("id").toString().replaceAll("\"", ""));
        }
        return categoryIds;
    }

    private List<String> parsePlaylistsJson(String json) {
        if (json == null || JsonParser.parseString(json).getAsJsonObject().has("error")) {
            List<String> errorResponse = new ArrayList<>();
            errorResponse.add("Specified id doesn't exist");
            return errorResponse;
        }

        JsonObject jo = JsonParser.parseString(json).getAsJsonObject();
        JsonObject playlistsObj = jo.getAsJsonObject("playlists");
        List<String> playlists = new ArrayList<>();
        for (JsonElement item : playlistsObj.getAsJsonArray("items")) {
            String playlistName = item.getAsJsonObject().get("name").toString();
            String spotifyUrl = item.getAsJsonObject().get("external_urls").getAsJsonObject().get("spotify").toString();

            playlists.add("%s\n%s\n".formatted(playlistName, spotifyUrl).replaceAll("\"", ""));
        }
        return playlists;
    }

    private List<String> parseNewReleasesJson(String json) {
        JsonObject jo = JsonParser.parseString(json).getAsJsonObject();
        JsonObject albumsObj = jo.getAsJsonObject("albums");
        List<String> newReleases = new ArrayList<>();
        for (JsonElement item : albumsObj.getAsJsonArray("items")) {
            List<String> artistsNameList = new ArrayList<>();
            for (JsonElement artist : item.getAsJsonObject().getAsJsonArray("artists")) {
                String artistName = artist.getAsJsonObject().get("name").toString();
                artistsNameList.add(artistName);
            }
            String songName = item.getAsJsonObject().get("name").toString();
            String spotifyUrl = item.getAsJsonObject().get("external_urls").getAsJsonObject().get("spotify").toString();

            newReleases.add("%s\n%s\n%s\n".formatted(songName, artistsNameList, spotifyUrl).replaceAll("\"", ""));
        }
        return newReleases;
    }


    private List<String> parseFeaturedJson(String json) {
        JsonObject jo = JsonParser.parseString(json).getAsJsonObject();
        JsonObject playlistsObj = jo.getAsJsonObject("playlists");
        List<String> featured = new ArrayList<>();
        for (JsonElement item : playlistsObj.getAsJsonArray("items")) {
            String featuredName = item.getAsJsonObject().get("name").toString();
            String spotifyUrl = item.getAsJsonObject().get("external_urls").getAsJsonObject().get("spotify").toString();

            featured.add("%s\n%s\n".formatted(featuredName, spotifyUrl).replaceAll("\"", ""));
        }
        return featured;
    }

    private class MusicAdvisorHttpServer implements HttpHandler {

        private final SpotifyRemoteService spotifyRemoteService;
        private final BlockingDeque<String> accessCodeQueue;

        private MusicAdvisorHttpServer(SpotifyRemoteService spotifyRemoteService,
                                       BlockingDeque<String> accessCodeQueue) {
            this.spotifyRemoteService = spotifyRemoteService;
            this.accessCodeQueue = accessCodeQueue;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();


            if (query != null && query.startsWith("code")) {
                System.out.println("waiting for code...");
                String accessCode = exchange.getRequestURI().getQuery().split("code=")[1];
                accessCodeQueue.add(accessCode);

                String positiveResponse = "Got the code. Return back to your program.";
                exchange.sendResponseHeaders(200, positiveResponse.length());
                exchange.getResponseBody().write(positiveResponse.getBytes());
                exchange.getResponseBody().close();
                System.out.println("code received");

                // POST
                try {
                    spotifyRemoteService.requestToken(accessCode);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                authorized = true;
            } else {
                String negativeResponse = "Authorization code not found. Try again.";
                exchange.sendResponseHeaders(200, negativeResponse.length());
                exchange.getResponseBody().write(negativeResponse.getBytes());
                exchange.getResponseBody().close();
            }
        }
    }


}