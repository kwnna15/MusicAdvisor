package advisor;

import java.io.IOException;

public class ProcessManager {

    public void start(String SpotifyServerPath, String apiServerPath, int pageSize) {
        boolean keepProcessing = true;
        boolean authorizedUser = false;
        RequestProcessor request = new RequestProcessor(authorizedUser, SpotifyServerPath, apiServerPath,pageSize);
        while (keepProcessing) {
            request.getUserRequest();
            try {
                keepProcessing = request.processRequest();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            authorizedUser = request.getAuthorized();
        }
    }
}