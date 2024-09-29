//https://gitlab.com/pavelperc-hyperskill/music_advisor/-/blob/master/Music%20Advisor/stage3/src/advisor/Main.java?ref_type=heads

package advisor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

class SpotifyRemoteService {

    private static final String client_id = "c4d2c4750f884f688109440ead8ceda8";
    private static final String client_secret = "22176e4a95e249849ccf8084deb20373";
    private static final String redirect_uri = "http://localhost:8080";

    private final String tokenUri;
    private final String authorizeUri;
    private String SpotifyToken;
    private final String APIServerPath;

    public SpotifyRemoteService(String spotifySite, String apiServerPath) {
        tokenUri = spotifySite + "/api/token";
        authorizeUri = spotifySite + "/authorize?client_id="
                       + client_id + "&response_type=code&redirect_uri=" + redirect_uri;
        APIServerPath = apiServerPath;
    }

    void askAuthorization() {
        System.out.println("use this link to request the access code:\n" + authorizeUri);
    }

    void requestToken(String authorizationCode) throws IOException, InterruptedException {
        String body = "grant_type=authorization_code"
                      + "&code=" + authorizationCode
                      + "&redirect_uri=" + redirect_uri
                      + "&client_id=" + client_id
                      + "&client_secret=" + client_secret;

        HttpClient client = HttpClient.newBuilder().build();
        System.out.println("Making http request for access_token...");
        HttpRequest request = HttpRequest.newBuilder()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .uri(URI.create(tokenUri))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        getAccessToken(response.body());



    }

    String getCategories() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .header("Authorization", "Bearer " + SpotifyToken)
                .uri(URI.create(APIServerPath+"/v1/browse/categories"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        //System.out.println("response:");
        //System.out.println(response.body());
        return response.body();
    }

    String getPlaylists(String playlistId) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .header("Authorization", "Bearer " + SpotifyToken)
                .uri(URI.create(APIServerPath+"/v1/browse/categories/"+playlistId+"/playlists"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("playlists:");
        System.out.println(response.body());
        System.out.println(response.statusCode());
        if (response.statusCode() == 200) {
            return response.body();
        }
        return null;
    }

    String getNewReleases() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .header("Authorization", "Bearer " + SpotifyToken)
                .uri(URI.create(APIServerPath+"/v1/browse/new-releases"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    String getFeatured() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .header("Authorization", "Bearer " + SpotifyToken)
                .uri(URI.create(APIServerPath+"/v1/browse/featured-playlists"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        //System.out.println(response.body());
        return response.body();
    }

    public void getAccessToken(String _response) {
        JsonObject jsonObject = JsonParser.parseString(_response).getAsJsonObject();
        SpotifyToken = jsonObject.get("access_token").getAsString();
        System.out.println("Success!");
    }
}
