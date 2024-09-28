package advisor;

public class Main {
    public static void main(String[] args) {
        String SpotifyServerPath="";
        if (args.length > 0) {
            if (args[0].equals("-access"))
                SpotifyServerPath = args[1];
        }
        ProcessManager processing = new ProcessManager();
        processing.start(SpotifyServerPath);
    }
}