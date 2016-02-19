/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.command;

import client.command.rank.*;

/**
 *
 * @author Menno
 */
public enum GMRank
{
    PLAYER(0, "Player", new PlayerCommand()),
    JRGUARD(4, "Jr. Guard", new JrGuardCommand()),
    GUARD(5, "Guard", new GuardCommand()),
    JRGM(6, "Jr. GM", new JrGMCommand()),
    GM(7, "GM", new GMCommand()),
    ADMIN(8, "Admin", new AdminCommand()),
    OWNER(99, "Owner", new OwnerCommand()),
    DEVELOPER(127, "Developer", new DeveloperCommand());
    
    final int id;
    final String name;
    final CommandInterface commandProcessor;

    private GMRank(int id, String name, CommandInterface commandProcessor) {
        this.id = id;
        this.name = name;
        this.commandProcessor = commandProcessor;
    }

    public static GMRank getById(int id) {
        for (GMRank l : GMRank.values()) {
            if (l.getId() == id) {
                return l;
            }
        }
        return null;
    }
    
    public int getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }

    public CommandInterface getCommandProcessor () {
        return commandProcessor;
    }
}
