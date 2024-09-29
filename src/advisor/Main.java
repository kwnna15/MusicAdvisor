package advisor;

public class Main {
    public static void main(String[] args) {
        String SpotifyServerPath="";
        String apiServerPath= "";
        if (args.length > 0) {
            if (args[0].equals("-access"))
                SpotifyServerPath = args[1];
            if (args.length>2){
                if (args[2].equals("-resource")){
                    apiServerPath = args[3];
                }
            }
        }
        ProcessManager processing = new ProcessManager();
        processing.start(SpotifyServerPath, apiServerPath);
    }
}