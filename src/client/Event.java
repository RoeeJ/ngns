/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import java.util.List;
import java.util.ArrayList;
/**
 *
 * @author Menno
 */
public class Event {
    private static Event[] events = new Event[3];
    private List<MapleCharacter> participants = new ArrayList<MapleCharacter>();
    private static boolean isstartup = true;
    private boolean started;
    private int map = -1;
    private String starttime;
    private MapleCharacter starter;
    private int starterCharacterId;
    private String type;
    private MapleCharacter winner;
    
    public Event() {
        started = false;
    }
            
    
    public Event(MapleCharacter starter, String type, String starttime, int map) {
        started = true;
        this.starttime = starttime;
        this.starter = starter;
        this.starterCharacterId = starter.getId();
        this.type = type;
        this.map = map;
    }
    
    public static int start(MapleCharacter starter, String type, String starttime, int map) {
        if(isstartup)
        {
            events[0] = null;
            events[1] = null;
            events[2] = null;
            isstartup = false;
        }
        for (int i = 0; i < events.length; i++) {
            if (events[i] == null) {
                events[i] = new Event(starter, type, starttime, map);
                return i;
            }
        }
        return 500;
    }
    
    public static void stop(Event event) {
        for (int i = 0; i < events.length; i++) {
            
            if (events[i] != null) {
                if (event.equals(events[i])) {
                    events[i] = null;
                    return;
                }
            }
        }
        
        System.out.println("Can not cancel event, no such event exists.");
    }
    
    public static void clear(boolean forced) {
        for (int i = 0; i < events.length; i++) {
            
            if (events[i] != null) {
                
                if (forced == false) {
                    if (!events[i].hasStarter()) {
                        events[i] = null;
                    }
                } else {
                    events[i] = null;
                }
            }
        }
    }
    
    public void stop() {
        started = false;
        starter = null;
    }
    
    public void stop(MapleCharacter winner) {
        stop();
        this.winner = winner;
    }
    
    public static int getMaxGmEvents() {
        int maxgmevents = 3;
        return maxgmevents;
    }
    
    public static int getMaxPlayerEvents() {
        int maxplayerevents = 10;
        return maxplayerevents;
    }
    
    public List<MapleCharacter> getParticipants() {
        return participants;
    }
    
    public boolean isParticipant(MapleCharacter isparticipant) {
        if(participants.contains(isparticipant))
            return true;
        return false;
    }
    
    public boolean isParticipant(String name) {
        for(MapleCharacter a : participants) {
            if(a.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }
    
    public MapleCharacter getParticipant(String name) {
        for(MapleCharacter a : participants) {
            if(a.getName().equals(name)) {
                return a;
            }
        }
        return null;
    }
    
    public int getStarterCharacterId()
    {
        return starterCharacterId;
    }
    
    public boolean isStarter(MapleCharacter isstarter) {
        
        if (starter == null) {
            if (isstarter.getId() == getStarterCharacterId()) {
                starter = isstarter;
                return true;
            }
            
            return false;
        }
        
        return starter.equals(isstarter);
    }
    
    public boolean isStarter(String name) {
        return starter.getName().equals(name);
    }
    
    public boolean hasStarter() {
        return starter != null;
    }
    
    public boolean isGmEvent() {
        return starter.isGM();
    }
    
    public int getMap() {
        return map;
    }
    
    public void addParticipant(MapleCharacter newparticipant) {
        participants.add(newparticipant);
    }
    
    public void removeParticipant(MapleCharacter removeparticipant) {
        for(int i = 0; i < participants.size(); i++)
        {
            if(participants.contains(removeparticipant))
            {
                participants.remove(removeparticipant);
            }
        }
    }
    
    public void removeParticipant(String removeparticipant) {
        for(int i = 0; i < participants.size(); i++)
        {
            
            if(isParticipant(removeparticipant))
            {
                participants.remove(getParticipant(removeparticipant));
            }
        }
    }
    
    public int totalParticipants() {
        return participants.size();
    }
    
    public void nullType() {
        type = null;
    }
    
    public Boolean isStarted() {
        return started;
    }
    
    public String getStarttime() {
        return starttime;
    }
    
    public String getStarter(boolean displayname) {
        if(displayname)
            return starter.getDisplayName();
        else
            return starter.getName();
    }
    
    public String getType() {
        return type;
    }
    
    public String getWinner() {
        return winner.getName();
    }
    
    public static Event[] getEvents() {
        return events;
    }
    
    public static Event getEvent(int i) {
        return events[i];
    }
    
    public static int startedEvents() {
        int startedevents = 0;
        for (int i = 0; i < events.length; i++) { 
            if(events[i].isStarted())
            {
                startedevents++;
            }
        }
        return startedevents;
    }
}
