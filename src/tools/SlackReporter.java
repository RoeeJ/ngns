package tools;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

/**
 * Created by Thor on 27-Feb-15.
 */
public class SlackReporter {

    private static SlackReporter instance = null;
    private String message;
    private String name;
    private SlackReporter() {
    }

    public static SlackReporter getInstance() { //works together with disable()
        if (instance == null)
            instance = new SlackReporter();
        return instance;
    }

    public String generateTime() {
        return new Date().toString(); //deprecated class ftw
    }

    private boolean containsIllegal(String check) {
        String[] illegal = {"what", "the", "if", "is", "he", "she", "why", "when", "how", "because"}; //very limited, I know...
        for (int i = 0; i < illegal.length; i++) {
            if (check.equalsIgnoreCase(illegal[i]) || check.length() < 4) { //has to be a 4 letter word+
                return true;
            }
        }
        return false;
    }
    public void log(String name, String channel, String message){
        if(true) return;
        try {
            String targetURL = "https://hooks.slack.com/services/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";
            String urlParameters = String.format("{\"text\": \"%s\", \"channel\": \"%s\", \"username\": \"%s\"}", message, channel, name);
            URL url = new URL(targetURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
            connection.setDoInput(true);
            connection.setDoOutput(true);
            DataOutputStream dataoutput = new DataOutputStream(connection.getOutputStream());
            dataoutput.write(urlParameters.getBytes("UTF-8"));
            dataoutput.flush();
            BufferedReader bufferResponse = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = bufferResponse.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            bufferResponse.close();
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }
}
