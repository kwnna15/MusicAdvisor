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
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class RequestProcessor {
    private String request;
    private boolean authorized = false;
    private final String uriBasePath;

    private final SpotifyRemoteService spotifyRemoteService;

    RequestProcessor(boolean authorized, String SpotifyServerPath) {
        this.authorized = authorized;
        if (SpotifyServerPath.isEmpty()) {
            uriBasePath = "https://accounts.spotify.com";
        } else {
            uriBasePath = SpotifyServerPath;
        }
        spotifyRemoteService = new SpotifyRemoteService(uriBasePath);
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
                System.out.println("---FEATURED---\n" +
                                   "Mellow Morning\n" +
                                   "Wake Up and Smell the Coffee\n" +
                                   "Monday Motivation\n" +
                                   "Songs to Sing in the Shower");

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
                    List<String> categories = parseCategoriesJson(spotifyRemoteService.getCategories());
                    for (String i : categories) {
                        System.out.println(i);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                break;
            case "playlists Mood":
                System.out.println("---MOOD PLAYLISTS---\n" +
                                   "Walk Like A Badass  \n" +
                                   "Rage Beats  \n" +
                                   "Arab Mood Booster  \n" +
                                   "Sunday Stroll");
                break;
            case "auth":

                server.bind(new InetSocketAddress(8080), 0);
                server.createContext("/", new MusicAdvisorHttpServer(spotifyRemoteService, accessCodeQueue));

                server.start();

                System.out.println(uriBasePath + "/authorize?client_id=c4d2c4750f884f688109440ead8ceda8&redirect_uri=http://localhost:8080&response_type=code");


                while (!authorized) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }


                server.stop(1);


                authorized = true;
                System.out.println("---SUCCESS---");
                break;
            case "exit":
                System.out.println("---GOODBYE!---");
                return false;
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

    private List<String> parseNewReleasesJson(String json){
        JsonObject jo = JsonParser.parseString(json).getAsJsonObject();
        JsonObject albumsObj = jo.getAsJsonObject("albums");
        List<String> newReleases = new ArrayList<>();
        for (JsonElement item : albumsObj.getAsJsonArray("items")) {
            List<String> artistsNameList= new ArrayList<>();
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
            /*List<String> artistsNameList= new ArrayList<>();
            for (JsonElement artist : item.getAsJsonObject().getAsJsonArray("artists")) {
                String artistName = artist.getAsJsonObject().get("name").toString();
                artistsNameList.add(artistName);
            }*/
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
                String accessCode = exchange.getRequestURI().getQuery().split("code=")[1];
                accessCodeQueue.add(accessCode);

                String positiveResponse = "Got the code. Return back to your program.";
                exchange.sendResponseHeaders(200, positiveResponse.length());
                exchange.getResponseBody().write(positiveResponse.getBytes());
                exchange.getResponseBody().close();

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