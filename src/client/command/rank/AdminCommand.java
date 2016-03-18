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
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package client.command.rank;

import client.*;
import client.Event;
import client.command.CommandInterface;
import client.inventory.Equip;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.sexbot.Muriel;
import net.server.Server;
import net.server.channel.Channel;
import net.server.world.World;
import scripting.npc.NPCScriptManager;
import scripting.portal.PortalScriptManager;
import scripting.reactor.ReactorScriptManager;
import server.MapleInventoryManipulator;
import server.MapleShopFactory;
import server.events.gm.MapleOxQuiz;
import server.life.MapleLifeFactory;
import server.life.MapleMonsterInformationProvider;
import server.life.MapleNPC;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import tools.DatabaseConnection;
import tools.MaplePacketCreator;
import tools.StringUtil;

import java.awt.*;
import java.rmi.RemoteException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map.Entry;

public class AdminCommand extends GMCommand implements CommandInterface
{
    @Override
    public boolean execute(MapleClient c, String[] splitted, char heading) throws SQLException, RemoteException {
        
        if (super.execute(c, splitted, heading)) {
            return true;
        }
        
        MapleCharacter player = c.getPlayer();
        Channel cserv = c.getChannelServer();
        
        switch(splitted[0].toLowerCase()) {
            case "serverstatus":
            {
                Server server = Server.getInstance();
                
                int totalConnectedClients = 0;
                for (Channel channel : server.getAllChannels()) {
                    totalConnectedClients += channel.getConnectedClients();
                    player.dropMessage("Channel " + channel.getId() + " has " + channel.getConnectedClients() + " players.");
                }
                
                player.dropMessage("Total amount of players: " + totalConnectedClients);
                player.dropMessage("-------------------------------------------------");
                
                int totalMaps = 0;
                for (Channel channel : server.getAllChannels()) {
                    MapleMapFactory mapFactory = channel.getMapFactory();

                    totalMaps += mapFactory.getMaps().size();
                    player.dropMessage("Channel " + channel.getId() + " has " + mapFactory.getMaps().size() + " maps.");
                }
                
                player.dropMessage("Total amount of maps: " + totalMaps);
                player.dropMessage("-------------------------------------------------");

                int totalMapObjects = 0;
                for (Channel channel : server.getAllChannels()) {
                    MapleMapFactory mapFactory = channel.getMapFactory();

                    int currentChannelObjects = 0;
                    for (MapleMap map : mapFactory.getMaps().values()) {
                        currentChannelObjects += map.getMapObjects().size();
                    }

                    totalMapObjects += currentChannelObjects;
                    player.dropMessage("Channel " + channel.getId() + " has " + currentChannelObjects + " map objects.");
                }
                
                player.dropMessage("Total amount of map objects: " + totalMapObjects);
            
                break;
            }
            case "gc":
            {
                System.gc();
                break;
            }
            case "equipslot":
            {
                byte invslot = (byte) getNamedIntArg(splitted,0,"invslot",255);
                if(invslot == 255) {
                    player.dropMessage(String.format("usage: %s%s invslot <invslot> eqpslot <eqpslot>", heading,splitted[0]));
                    break;
                }
                MapleInventory equipinv= player.getInventory(MapleInventoryType.EQUIPPED);
                Equip equip = (Equip) equipinv.getItem(invslot);

                byte eqpslot = (byte) getNamedIntArg(splitted,0,"eqpslot",255);
                if(eqpslot == 255) {
                    player.dropMessage(String.format("usage: %s%s invslot <invslot> eqpslot <eqpslot>", heading,splitted[0]));
                    break;
                }
                MapleInventory equippedinv = player.getInventory(MapleInventoryType.EQUIPPED);
                Equip equipped = (Equip) equippedinv.getItem(eqpslot);

                player.dropMessage(String.format("%d %d", invslot,eqpslot));
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("equip is %snull", equip == null ? "" : "not ")).append('\n');
                sb.append(String.format("equipped is %snull", equipped == null ? "" : "not "));
                player.dropMessage(sb.toString());

                equip.setPosition(invslot);
                equipinv.addFromDB(equipped);
                equippedinv.removeSlot(eqpslot);

                equipped.setPosition(eqpslot);
                equippedinv.addFromDB(equipped);
                equipinv.removeSlot(invslot);
                break;
            }
            case "npc":
            {
                MapleNPC npc = MapleLifeFactory.getNPC(Integer.parseInt(splitted[1]));
                if (npc != null) {
                    npc.setPosition(player.getPosition());
                    npc.setCy(player.getPosition().y);
                    npc.setRx0(player.getPosition().x + 50);
                    npc.setRx1(player.getPosition().x - 50);
                    npc.setFh(player.getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId());
                    player.getMap().addMapObject(npc);
                    player.getMap().broadcastMessage(MaplePacketCreator.spawnNPC(npc));
                }
                break;
            }
            case "print":
            {
                System.out.println(StringUtil.joinStringFrom(splitted, 1));
                break;
            }
            case "speak":
            {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (cserv.getMuriel() != null && Muriel.getCharacter(cserv.getMuriel()).getId() == victim.getId()) {
                    break;
                }
                victim.getMap().broadcastMessage(MaplePacketCreator.getChatText(victim.getId(), StringUtil.joinStringFrom(splitted, 2) , victim.isGM(), 0));
                break;
            }
            case "giftnx":
            {
                cserv.getPlayerStorage().getCharacterByName(splitted[1]).getCashShop().gainCash(4, Integer.parseInt(splitted[2]));
                player.message("Done");
                break;
            }
            case "screamer":
            {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if(victim != null){
                    victim.getClient().getSession().write(MaplePacketCreator.mapEffect("quest/party/scream"));
                    victim.getClient().getSession().write(MaplePacketCreator.mapSound("Party1/Scream"));
                }
                else{
                    player.dropMessage(4,"Victim is offline/invalid");
                }
                break;
            }
            case "mac": {
                player.dropMessage(player.getClient().getMacsString());
                break;
            }
            case "mapscreamer":
            {
                for(MapleCharacter victim : player.getMap().getCharacters()) {
                    if(player.getId() == victim.getId()){continue;}
                    victim.getClient().getSession().write(MaplePacketCreator.mapEffect("quest/party/clear"));
                    victim.getClient().getSession().write(MaplePacketCreator.mapEffect("Party1/Clear"));
                }
                break;
            }
            case "getcharid":{
                player.dropMessage(6, String.valueOf(MapleCharacter.getIdByName(splitted[1])));
                break;
            }
            case "clearcharinv":
            {
                try {
                    Integer id = MapleCharacter.getIdByName(splitted[1]);
                    if(id == -1)
                    {
                        //player.dropMessage(6,"nowai bruh.");
                        break;
                    }
                    try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("DELETE FROM inventoryitems WHERE characterid = ?")) {
                        ps.setString(1, id.toString());
                        int ret = ps.executeUpdate();
                        if(ret != 0){
                            ret = ps.executeUpdate();
                            player.dropMessage(ret == 0 ? "Wiped":String.valueOf(ret));
                        }
                        else{player.dropMessage("Wiped");}
                    }
                } catch (Exception e) {
                    player.dropMessage(e.getMessage());
                }
                break;
            }
            case "ox":
            {
                if (splitted[1].equalsIgnoreCase("on") && player.getMapId() == 109020001) {
                    player.getMap().setOx(new MapleOxQuiz(player.getMap()));
                    player.getMap().getOx().sendQuestion();
                    player.getMap().setOxQuiz(true);
                } else {
                    player.getMap().setOxQuiz(false);
                    player.getMap().setOx(null);
                }
                break;
            }
            case "pinkbean":
            {
                player.getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(8820001), player.getPosition());
                break;
            }
            case "godmode":
            {
                if (splitted.length == 2) {
                    player.setGodMode(splitted[1].equals("on"));
                    player.dropMessage("Godmode " + (splitted[1].equals("on") ? "on" : "off"));
                } else {
                    player.dropMessage("Godmode is currently " + (player.getGodMode() ? "on" : "off"));
                }
                break;
            }
            case "levelperson":
            {
                MapleCharacter mpc = cserv.getPlayerStorage().getCharacterByName(splitted[1]);

                mpc.setLevel(Integer.parseInt(splitted[2]));
                mpc.gainExp(-player.getExp(), false, false);
                mpc.updateSingleStat(MapleStat.LEVEL, player.getLevel());
                mpc.setExp(0);
                mpc.updateSingleStat(MapleStat.EXP, 0);
                player.dropMessage("Done.");
                break;
            }
            case "bgm":
            {
                if(splitted.length == 1) break;
                String path = splitted[1];
                player.getMap().broadcastMessage(MaplePacketCreator.musicChange(path));
                break;
            }
            case "saveall":
            {
                for (World world : Server.getInstance().getWorlds()) {
                    for (MapleCharacter chr : world.getPlayerStorage().getAllCharacters()) {
                        chr.saveToDB();
                    }
                }
                
                player.dropMessage("All characters saved.");
                break;
            }
            case "reloadmapspawns":
            {
                for (Entry<Integer, MapleMap> map : c.getChannelServer().getMapFactory().getMaps().entrySet()) {
                    map.getValue().respawn();
                }
                player.message("Done.");
                break;
            }
            case "openportal":
            {
                player.getMap().getPortal(splitted[1]).setPortalState(true);
                break;
            }
            case "closeportal":
            {
                player.getMap().getPortal(splitted[1]).setPortalState(false);
                break;
            }
            case "zakum":
            {
                player.getMap().spawnFakeMonsterOnGroundBelow(MapleLifeFactory.getMonster(8800000), player.getPosition());
                for (int x = 8800003; x < 8800011; x++) {
                    player.getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(x), player.getPosition());
                }
                break;
            }
            case "disposenpcs":
            {
                NPCScriptManager.getInstance().dispose(c);
                c.getSession().write(MaplePacketCreator.enableActions());
                player.message("Done.");
                break;
            }
            case "clearportalscripts":
            {
                PortalScriptManager.getInstance().reloadPortalScripts();
                player.message("Done.");
                break;
            }
            case "clearmonsterdrops":
            {
                MapleMonsterInformationProvider.getInstance().clearDrops();
                player.message("Done.");
                break;
            }
            case "clearreactordrops":
            {
                ReactorScriptManager.getInstance().clearDrops();
                player.message("Done.");
                break;
            }
            case "clearskills": {
                long timeToTake = System.currentTimeMillis();
                player.dropMessage("Loading Skills");
                SkillFactory.loadAllSkills();
                player.dropMessage("Skills loaded in " + ((System.currentTimeMillis() - timeToTake) / 1000.0) + " seconds");
                break;
            }
            case "clearshops":
            {
                MapleShopFactory.getInstance().reloadShops();
                player.message("Done.");
                break;
            }
            case "clearevents":
            {
                if (splitted.length == 2 && splitted[1].equals("forced")) {
                    Event.clear(true);
                } else {
                    Event.clear(false);
                }

                player.message("Done.");
                break;
            }
            case "cleareventprizes":
            {
                EventPrize.loadEventPrizes(true);
                player.message("Done.");
                break;
            }
            case "clearunlockitems":
            {
                UnlockItem.loadAvailableUnlocks(true);
                player.message("Done.");
                break;
            }
            case "clearcommandinformation":
            {
                CommandInformation.loadCommandInformations(true);
                player.message("Done.");
                break;
            }
            case "dcall":
            {
                Collection<Channel> cservs = Server.getInstance().getAllChannels();

                for (Channel curCserv : cservs) {
                    for (MapleCharacter mch : curCserv.getPlayerStorage().getAllCharacters()) {
                        if (!mch.equals(player)) {
                            mch.saveToDB();
                            mch.getClient().getSession().close(true);
                        }
                    }
                }

                player.message("Done.");
                break;
            }
            case "openui":
            {
                if (splitted.length < 3) break;
                cserv.getPlayerStorage().getCharacterByName(splitted[1]).announce(MaplePacketCreator.openUI(Byte.parseByte(splitted[2])));
                break;
            }
            case "crash": {
                if (splitted.length != 2) {
                    player.dropMessage("Invalid syntax, " + heading + "crash <ign>");
                    break;
                }
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim != null) {
                    victim.announce(MaplePacketCreator.openUI((byte) 33));
                    player.dropMessage(String.format("Player %s has been crashed!", victim.getName()));
                }
                break;
            }
            case "fakeban":
            {
                if (splitted.length < 3) {
                    player.dropMessage("Syntax for !fakeban: !ban [user] [reason]");
                    return true;
                }
                String originalReason = StringUtil.joinStringFrom(splitted, 2);
                String reason = "[TEST]" + c.getPlayer().getName() + " banned " + splitted[1] + ": " + originalReason;
                MapleCharacter target = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (target != null) {
                    if (target.gmLevel() <= 3 || player.getAdmin()) {

                        target.sendPolice("[TEST] You have been banned for: " + originalReason);

                        player.dropMessage("Banned " + target.getName() + " for reason: " + originalReason);
                        player.dropMessage("The user receives a message and will be disconnected within 10 seconds.");
                    } else {
                        player.dropMessage("You can't ban a GM!");
                    }
                } else {
                    if (MapleCharacter.ban(splitted[1], reason, false)) {
                        player.dropMessage("Offline Banned " + splitted[1]);
                    } else {
                        player.dropMessage("Failed to ban " + splitted[1]);
                    }
                }
                break;
            }
            default:
            {
                return false;
            }
        }
        
        return true;
    }
 }

