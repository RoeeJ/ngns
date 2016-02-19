/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import tools.DatabaseConnection;
import tools.FilePrinter;

/**
 *
 * @author Menno
 */
public class UnlockItem {
    
    protected int id;
    protected String code;
    protected String name;
    protected String description;
    protected int cost;
    protected boolean available;
    protected boolean enabled;
    protected List<UnlockItem> dependencies = new ArrayList<>();
    
    protected static boolean availableUnlocksLoaded = false;
    protected static List<UnlockItem> availableUnlocks = new ArrayList<>();
    
    public static void loadAvailableUnlocks(boolean forced) {
        if (availableUnlocksLoaded == false || forced == true) {
            
            availableUnlocks.clear();
            
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("SELECT * FROM `unlock_item` ORDER BY `order` ASC;");
                ResultSet rs = ps.executeQuery();
                
                while (rs.next()) {
                    UnlockItem item = new UnlockItem();
                    item.setId(rs.getInt("id"));
                    item.setCode(rs.getString("code"));
                    item.setName(rs.getString("name"));
                    item.setDescription(rs.getString("description"));
                    item.setCost(rs.getInt("cost"));
                    item.setAvailable(rs.getBoolean("available"));
                    item.setEnabled(rs.getBoolean("enabled"));
                    
                    String dependency = rs.getString("dependency");
                    
                    if (dependency != null && !dependency.isEmpty()) {                        
                        String[] dependencies = dependency.split(",");
                        
                        for (String dep : dependencies) {
                            UnlockItem depUnlockItem = UnlockItem.getAvailableUnlockByCode(dep);

                            if (depUnlockItem != null) {
                                item.dependencies.add(depUnlockItem);
                            } else {
                                item.setAvailable(false);
                                item.setEnabled(false);
                                FilePrinter.printError(FilePrinter.CUSTOM, "Missing dependency '" + dep + "' for UnlockItem: " + item.getCode());
                            }
                        }
                    }
                    
                    availableUnlocks.add(item);
                }
                
                availableUnlocksLoaded = true;
            } catch (Exception e) {
                FilePrinter.printError(FilePrinter.CUSTOM, e, "Error loading available unlocks");
            }
        }
    }
    
    public static boolean codeExists(String code) {
        
        UnlockItem.loadAvailableUnlocks(false);
        
        for (UnlockItem item : availableUnlocks) {
            if (item.getCode().equalsIgnoreCase(code)) {
                return true;
            }
        }
        
        return false;
    }
    
    public static UnlockItem getAvailableUnlockByCode(String code) {
        for (UnlockItem item : availableUnlocks) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }
        
        return null;
    }
    
    public static UnlockItem getAvailableUnlockById(int id) {
        for (UnlockItem item : availableUnlocks) {
            if (item.getId() == id) {
                return item;
            }
        }
        
        return null;
    }
    
    public static List<UnlockItem> getAvailableUnlocks() {
        return availableUnlocks;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getId() {
        return id;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setCost(int cost) {
        this.cost = cost;
    }
    
    public int getCost() {
        return cost;
    }
    
    public void setAvailable(boolean available) {
        this.available = available;
    }
    
    public boolean getAvailable() {
        return available;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean getEnabled() {
        return enabled;
    }
    
    public List<UnlockItem> getDependencies() {
        return dependencies;
    }
}
