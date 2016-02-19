package client;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Soulfist
 */

public class ChatLog {
    
    BufferedWriter w = null;
    BufferedReader b = null;
    private static ChatLog instance = null;
    final List<String> chat = new ArrayList<String>();
    
    private ChatLog() {
        try {
            w = new BufferedWriter(new FileWriter("chatlog.txt", true));
            b = new BufferedReader(new FileReader("chatlog.txt"));
            w.append("\n\n\n\n");
        } catch (IOException i) {
        //    i.printStackTrace(System.out);
        }
    }
    
   public static ChatLog getInstance() { //works together with disable()
        if (instance == null)
            instance = new ChatLog();
            return instance;
    }
    
    public List<String> getChat() {
        return chat;
    }
    
    public String generateTime() {
        return new Date().toString(); //deprecated class ftw
    }
    
    public void disable() {
        try {
            if (w != null) w.close();
            if (b != null) b.close();
            instance = null;
        } catch (IOException io) {
            //io.printStackTrace(System.out);
        }
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
    
    public void makeLog() {
        synchronized (w) {
            try {
                for (int i = 0; i < chat.size(); i++) {
                    w.newLine();
                    w.append(chat.get(i));
                }
            } catch (IOException io) {
                //io.printStackTrace(System.out);
            }
            disable();
        }
        chat.clear();
    }
    
    public synchronized void add(String a) { //constantly adding
        chat.add(a);
    }
}  