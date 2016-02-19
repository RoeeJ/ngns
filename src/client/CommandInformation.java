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
public class CommandInformation {
    
    protected int id;
    protected String command;
    protected String shortDesc;
    protected String longDesc;
    protected int gmLevel;
    
    protected static boolean commandsInformationLoaded = false;
    protected static List<CommandInformation> commandsInformation = new ArrayList<>();
    
    public static void loadCommandInformations(boolean forced) {
        if (commandsInformationLoaded == false || forced == true) {
            
            commandsInformation.clear();
            
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("SELECT * FROM `command_information` ORDER BY `order` ASC;");
                ResultSet rs = ps.executeQuery();
                
                while (rs.next()) {
                    CommandInformation item = new CommandInformation();
                    item.setId(rs.getInt("id"));
                    item.setCommand(rs.getString("command"));
                    item.setShortDesc(rs.getString("short_desc"));
                    item.setLongDesc(rs.getString("long_desc"));
                    item.setGmLevel(rs.getInt("gm_level"));
                    
                    commandsInformation.add(item);
                }
                
                commandsInformationLoaded = true;
            } catch (Exception e) {
                FilePrinter.printError(FilePrinter.CUSTOM, e, "Error loading available commandsinformation");
            }
        }
    }
    
    public static boolean commandInformationExists(String command) {
        
        CommandInformation.loadCommandInformations(false);
        
        for (CommandInformation item : commandsInformation) {
            if (item.getCommand().equalsIgnoreCase(command)) {
                return true;
            }
        }
        
        return false;
    }
    
    public static CommandInformation getCommandInformationByCommand(String command) {
        for (CommandInformation item : commandsInformation) {
            if (item.getCommand().equals(command)) {
                return item;
            }
        }
        
        return null;
    }
    
    public static CommandInformation getCommandInformationById(int id) {
        for (CommandInformation item : commandsInformation) {
            if (item.getId() == id) {
                return item;
            }
        }
        
        return null;
    }
    
    public static List<CommandInformation> getCommandInformations() {
        CommandInformation.loadCommandInformations(false);        
        return commandsInformation;
    }
    
    public static List<CommandInformation> getCommandInformations(int gmLevel) {
        CommandInformation.loadCommandInformations(false);
        
        List<CommandInformation> returnValues = new ArrayList<>();
        for (CommandInformation commandInfo : commandsInformation) {
            
            if (commandInfo.getGmLevel() == gmLevel) {
                returnValues.add(commandInfo);
            }
        }
        return returnValues;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getId() {
        return id;
    }
    
    public void setCommand(String command) {
        this.command = command;
    }
    
    public String getCommand() {
        return command;
    }
    
    public void setShortDesc(String shortDesc) {
        this.shortDesc = shortDesc;
    }
    
    public String getShortDesc() {
        return shortDesc;
    }
    
    public void setLongDesc(String longDesc) {
        this.longDesc = longDesc;
    }
    
    public String getLongDesc() {
        return longDesc;
    }
    
    public void setGmLevel(int gmLevel) {
        this.gmLevel = gmLevel;
    }
    
    public int getGmLevel() {
        return gmLevel;
    }
}
