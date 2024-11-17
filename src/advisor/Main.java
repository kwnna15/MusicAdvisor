package advisor;

public class Main {
    public static void main(String[] args) {
        String SpotifyServerPath="";
        String apiServerPath= "";
        int pageSize=5;
        if (args.length > 0) {
            if (args[0].equals("-access"))
                SpotifyServerPath = args[1];
            if (args.length>2){
                if (args[2].equals("-resource")){
                    apiServerPath = args[3];
                }
            }
            if (args.length>4){
                if (args[4].equals("-page")){
                    pageSize = Integer.parseInt(args[5]);
                }
            }
        }
        ProcessManager processing = new ProcessManager();
        processing.start(SpotifyServerPath, apiServerPath,pageSize);
    }
}