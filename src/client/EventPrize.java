/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import java.util.List;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import tools.DatabaseConnection;

/**
 *
 * @author Menno
 */
public class EventPrize {
    private static List<EventPrize> eventPrizes = new ArrayList<EventPrize>();
    private static boolean eventPrizesLoaded = false;
    private static int eventPrizeCount = 0;
    
    private int id;
    private int itemid;
    private int qty;
    private int cost;
    
    public EventPrize() {
        
    }
    
    public static List<EventPrize> getEventPrizes() {
        
        loadEventPrizes(false);
        return eventPrizes;
    }
    
    public static int getCount()
    {
        loadEventPrizes(false);
        return eventPrizeCount;
    }
    
    public static EventPrize getEventPrize(int index)
    {
        loadEventPrizes(false);
        return eventPrizes.get(index);
    }
    
    public static EventPrize getEventPrizeById(int id)
    {
        loadEventPrizes(false);
        
        for (EventPrize eventprize : eventPrizes)
        {
            if (eventprize.getId() == id) {
                return eventprize;
            }
        }
        
        return null;
    }
    
    public static void loadEventPrizes(boolean forced)
    {
        if (eventPrizesLoaded == false || forced == true) {
            try {
                eventPrizeCount = 0;
                eventPrizes.clear();
                
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("SELECT * FROM `event_prizes` ORDER BY `order` ASC;");
                ResultSet rs = ps.executeQuery();
                
                while (rs.next()) {
                    
                    EventPrize eventprize = new EventPrize();                    
                    eventprize.setId(rs.getInt("id"));
                    eventprize.setItemId(rs.getInt("item_id"));
                    eventprize.setQty(rs.getInt("qty"));
                    eventprize.setCost(rs.getInt("cost"));
                    
                    eventPrizes.add(eventprize);
                    eventPrizeCount++;
                }
                
                rs.close();
                ps.close();
            } catch (SQLException ex) {
                System.out.println("EventPrize error: " + ex.getMessage());
            }
            
            eventPrizesLoaded = true;
        }
    }
    
    public void setId(int id)
    {
        this.id = id;
    }
    
    public int getId()
    {
        return this.id;
    }
    
    public void setItemId(int itemid)
    {
        this.itemid = itemid;
    }
    
    public int getItemId()
    {
        return this.itemid;
    }
    
    public void setQty(int qty)
    {
        this.qty = qty;
    }
    
    public int getQty()
    {
        return this.qty;
    }
    
    public void setCost(int cost)
    {
        this.cost = cost;
    }
    
    public int getCost()
    {
        return this.cost;
    }
}
