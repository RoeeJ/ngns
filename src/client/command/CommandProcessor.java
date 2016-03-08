/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation version 3 as published by
 the Free Software Foundation. You may not use, modify or distribute
 this program under any other version of the GNU Affero General Public
 License.te

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package client.command;

import client.MapleClient;
import net.server.Server;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.life.MapleLifeFactory;
import tools.DatabaseConnection;
import tools.FilePrinter;
import tools.Pair;
import tools.StringUtil;

import java.io.File;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CommandProcessor {

    public static boolean processCommand(MapleClient c, String s, char heading) throws RemoteException, SQLException
    {
        String[] sp = s.split(" ");
        sp[0] = sp[0].toLowerCase().substring(1);
        GMRank gmRank = c.getPlayer().isUnderCover() ? GMRank.ADMIN : c.getPlayer().getGMRank();
        
        if (heading == '@') {
            CommandInterface commandProcessor = GMRank.PLAYER.getCommandProcessor();
            
            if (commandProcessor.execute(c, sp, heading)) {
                CommandProcessor.logCommand(c, s);
                return true;
            }
            
            c.getPlayer().yellowMessage("Command " + heading + sp[0] + " does not exist.");
            return true;
            
        } else if ((heading == '!' || heading == '/') && gmRank.getId() >= GMRank.JRGUARD.getId()) {
            
            CommandInterface commandProcessor = gmRank.getCommandProcessor();
            if (commandProcessor.execute(c, sp, heading)) {
                return true;
            }
            
            c.getPlayer().yellowMessage("Command " + heading + sp[0] + " does not exist.");
            return true;
        }
        return false;
    }
    
    protected static void logCommand(MapleClient c, String s)
    {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            ps = con.prepareStatement("INSERT INTO logs_command (account_id, character_id, command) VALUES (?, ?, ?)");
            ps.setInt(1, c.getAccID());
            ps.setInt(2, c.getPlayer().getId());
            ps.setString(3, s);
            ps.executeUpdate();
        } catch (SQLException ex) {
            FilePrinter.printError(FilePrinter.LOGGER + CommandProcessor.class.getName() + ".txt", ex);
//            Logger.getLogger(MapleCharacter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static ArrayList<Pair<Integer, String>> getMobsIDsFromName(String search)
    {
            MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File("wz/String.wz"));
            ArrayList<Pair<Integer, String>> retMobs = new ArrayList<Pair<Integer, String>>();
            MapleData data = dataProvider.getData("Mob.img");
            List<Pair<Integer, String>> mobPairList = new LinkedList<Pair<Integer, String>>();
            for (MapleData mobIdData : data.getChildren()) {
                int mobIdFromData = Integer.parseInt(mobIdData.getName());
                String mobNameFromData = MapleDataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME");
                mobPairList.add(new Pair<Integer, String>(mobIdFromData, mobNameFromData));
            }
            for (Pair<Integer, String> mobPair : mobPairList) {
                if (mobPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                    retMobs.add(mobPair);
                }
            }
            return retMobs;
    }
    
    public static String getMobNameFromID(int id)
    {
        try
        {
            return MapleLifeFactory.getMonster(id).getName();
        } catch (Exception e)
        {
            return null; //nonexistant mob
        }
    }
    public String joinAfterString(String splitted[], String str) {
        for (int i = 1; i < splitted.length; i++) {
            if (splitted[i].equalsIgnoreCase(str) && i + 1 < splitted.length) {
                return StringUtil.joinStringFrom(splitted, i + 1);
            }
        }
        return null;
    }

    public int getOptionalIntArg(String splitted[], int position, int def) {
        if (splitted.length > position) {
            try {
                return Integer.parseInt(splitted[position]);
            } catch (NumberFormatException nfe) {
                return def;
            }
        }
        return def;
    }

    public static String getNamedArg(String splitted[], int startpos, String name) {
        for (int i = startpos; i < splitted.length; i++) {
            if (splitted[i].equalsIgnoreCase(name) && i + 1 < splitted.length) {
                return splitted[i + 1];
            }
        }
        return null;
    }

    public static Integer getNamedIntArg(String splitted[], int startpos, String name) {
        String arg = getNamedArg(splitted, startpos, name);
        if (arg != null) {
            try {
                return Integer.parseInt(arg);
            } catch (NumberFormatException nfe) {
                // swallow - we don't really care
            }
        }
        return null;
    }

    public static int getNamedIntArg(String splitted[], int startpos, String name, int def) {
        Integer ret = getNamedIntArg(splitted, startpos, name);
        if (ret == null) {
            return def;
        }
        return ret;
    }

    public static Double getNamedDoubleArg(String splitted[], int startpos, String name) {
        String arg = getNamedArg(splitted, startpos, name);
        if (arg != null) {
            try {
                return Double.parseDouble(arg);
            } catch (NumberFormatException nfe) {
                // swallow - we don't really care
            }
        }
        return null;
    }
}