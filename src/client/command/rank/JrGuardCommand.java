/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

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

import client.CommandInformation;
import client.MapleCharacter;
import client.MapleClient;
import client.anticheat.CheatTracker;
import client.anticheat.CheaterData;
import client.command.CommandAbstract;
import client.command.CommandInterface;
import client.command.GMRank;
import constants.ServerConstants;
import net.server.Server;
import net.server.channel.Channel;
import net.server.world.World;
import server.maps.MapleMap;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.List;

public class JrGuardCommand extends CommandAbstract implements CommandInterface
{
    @Override
    public boolean execute(MapleClient c, String[] splitted, char heading) throws SQLException, RemoteException
    {
        MapleCharacter player = c.getPlayer();
        Channel cserv = c.getChannelServer();
        Server srv = Server.getInstance();
        
        switch (splitted[0].toLowerCase()) {
            /*case "smega":
            {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[2]);
                if(player.gmLevel() >= victim.gmLevel()) {
                    if (splitted[1].equals("on")) {
                        victim.setCanSmega(true);
                        player.dropMessage("You have enabled " + victim.getName() + "'s megaphone privilages");
                        if (!(c.getPlayer().getName().equals(victim.getName()))) {
                            victim.dropMessage("Your megaphone privilages have been enabled by a GM. Please refrain from spamming.");
                        } else {
                            player.dropMessage("You can't turn your own smega on!");
                        }
                    } else if (splitted[1].equals("off")) {
                        victim.setCanSmega(false);
                        player.dropMessage("You have disabled " + victim.getName() + "'s megaphone privilages");
                        if (!(c.getPlayer().getName().equals(victim.getName()))) {
                            victim.dropMessage("Your megaphone privilages have been disabled by a GM. If you continue to spam you will be temp banned.");
                        } else {
                            player.dropMessage("You can't turn your own smega off!");
                        }
                    }
                } else {
                    player.dropMessage("You can't change the smega status of a GM with an equal or higher GM level!");
                    victim.dropMessage(player.getName() + " tried to !smega " + splitted[1] + " you.");
                }
                
                break;
            }*/
            case "gmchat":
            {
                String message = joinStringFrom(splitted, 1);
                Server.getInstance().gmChat("[GM Chat] " + player.getName() + " : " + message, null);
                break;
            }
            case "jail":
            {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if(victim.gmLevel() >= player.gmLevel()) {
                    player.dropMessage("You can't jail a GM with a higher GM level than you!");
                } else {
                    victim.Jail();
                }
                
                break;
            }
            case "unjail":
            {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                victim.Unjail();
                break;
            }
            case "charinfo":
            {
                StringBuilder builder = new StringBuilder();
                MapleCharacter other = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                builder.append(other.getName());
                builder.append(" at ");
                builder.append(other.getPosition().x);
                builder.append("/");
                builder.append(other.getPosition().y);
                builder.append(" ");
                builder.append(other.getHp());
                builder.append("/");
                builder.append(other.getCurrentMaxHp());
                builder.append("hp ");
                builder.append(other.getMp());
                builder.append("/");
                builder.append(other.getCurrentMaxMp());
                builder.append("mp ");
                builder.append(other.getExp());
                builder.append("exp hasParty: ");
                builder.append(other.getParty() != null);
                builder.append(" hasTrade: ");
                builder.append(other.getTrade() != null);
                builder.append(" remoteAddress: ");
                builder.append(other.getClient().getSession().getRemoteAddress());
                builder.append(" macAddress: ");
                builder.append(other.getClient().getMacsString());
                builder.append(" mapId: ");
                builder.append(String.valueOf(other.getMapId()));
                c.getPlayer().dropMessage(builder.toString());
                
                break;
            }
            case "tele":
            {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim != null) {
                    if (splitted.length == 2) {
                        MapleMap target = victim.getMap();
                        c.getPlayer().changeMap(target, target.findClosestSpawnpoint(victim.getPosition()));
                    } else {
                        player.dropMessage("Please use the following syntax: !tele [name]");
                    }
                }
                
                break;
            }
            case "kill":
            {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if(victim.gmLevel() >= player.gmLevel()) {
                    player.dropMessage("You can't kill a GM with an equal or higher GM level!");
                    victim.dropMessage(player.getName() + " tried to !kill you.");
                } else {
                    victim.setHpMp(0);
                }
                
                break;
            }
            case "mute":
            {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (player.gmLevel() >= victim.gmLevel()) {
                    victim.canTalk(false, true);
                    
                    if(victim.isMuted()) {
                    victim.dropMessage("You have been muted by a GM!");
                    player.dropMessage(victim.getName() + " has been muted.");
                    }
                }
                
                break;
            }
            case "unmute":
            {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if(victim.isMuted()) {
                    victim.dropMessage("You have been unmuted by a GM, please don't repeat the reason why you were muted in the future!");
                    player.dropMessage(victim.getName() + " has been unmuted.");
                    victim.canTalk(true, true);
                } else {
                    player.dropMessage(victim.getName() + " is not muted.");
                }
                
                break;
            }
            case "warn":
            {
                if (splitted.length >= 3) {
                    
                    MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                    /*if(victim.gmLevel() >= player.gmLevel()) {
                        player.dropMessage("You can't warn a GM with an equal or higher GM level!");
                        victim.dropMessage(player.getName() + " tried to !warn you.");
                        break;
                    }*/

                    String reason = joinStringFrom(splitted, 2);
                    victim.warn(reason);
                    player.dropMessage("The user has been warned.");
                } else {
                    player.dropMessage("Please use the following syntax: !warn [name] [reason].");
                }
                
                break;
            }
            case "kick":
            {
                if (splitted.length >= 3) {
                    
                    MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                    if(victim.gmLevel() >= player.gmLevel()) {
                        player.dropMessage("You can't kick a GM with an equal or higher GM level!");
                        victim.dropMessage(player.getName() + " tried to !kick you.");
                        return true;
                    }
                    
                    String reason = joinStringFrom(splitted, 2);
                    victim.kick(reason);
                    player.dropMessage("The user will receive your message message and will be kicked after a while.");
                } else {
                    player.dropMessage("Please use the following syntax: !kick [name] [reason].");
                }
                
                break;
            }
            case "message":
            {
                if (splitted.length >= 3) {
                    
                    MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                    if(victim.gmLevel() >= player.gmLevel()) {
                        player.dropMessage("You can't message a GM with an equal or higher GM level!");
                        victim.dropMessage(player.getName() + " tried to !message you.");
                        return true;
                    }
                    
                    String text = joinStringFrom(splitted, 2);
                    victim.showMessage(text);
                    player.dropMessage("The message has been shown.");
                } else {
                    player.dropMessage("Please use the following syntax: !message [name] [text].");
                }
                
                break;
            }
            case "online":
            {
                for (Channel ch : srv.getChannelsFromWorld(player.getWorld())) {
                    String s = "Characters online (Channel " + ch.getId() + " Online: " + ch.getPlayerStorage().getAllCharacters().size() + ") : ";
                    if (ch.getPlayerStorage().getAllCharacters().size() < 50) {
                        for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                            s += MapleCharacter.makeMapleReadable(chr.getName()) + ", ";
                        }
                        player.dropMessage(s.substring(0, s.length() - 2));
                    }
                }
                
                break;
            }
            case "cheaters":
            {
                try {
                    World world = Server.getInstance().getWorld(player.getWorld());
                    List<CheaterData> cheaters = CheatTracker.getCheaters(world);
                    for (int x = cheaters.size() - 1; x >= 0; x--) {
                        CheaterData cheater = cheaters.get(x);
                        player.dropMessage(cheater.getInfo());
                    }
                    if (cheaters.isEmpty()) {
                        player.dropMessage("No cheaters! Hurrah!");
                    }
                } catch (Exception e) {
                    if (player.getGMRank() == GMRank.ADMIN) {
                        player.dropMessage("Exception: " + e.getMessage());
                    }
                }
                break;
            }
            case "invisible":
            {
                if (splitted[1].equals("on") || splitted[1].equals("off")) {
                    if (splitted[1].equals("on")) {
                        player.setInvisible(true);
                    } else {
                        player.setInvisible(false);
                    }
                }
                player.dropMessage("Done.");
                break;
            }
            case "searchcommand":
            case "searchcommands":
            {
                if (splitted.length >= 2) {
                    String search = joinStringFrom(splitted, 1);

                    int resultCount = 0;

                    for (GMRank rank : GMRank.values()) {
                        boolean showResultLine = true;
                        for (CommandInformation item : CommandInformation.getCommandInformations(rank.getId())) {
                            if (item.getCommand().contains(search) || item.getShortDesc().contains(search) || item.getLongDesc().contains(search)) {
                                
                                if (showResultLine) {
                                    player.dropMessage("");
                                    player.dropMessage("-- Commands for rank: '" + rank.getName() + "' --");
                                    showResultLine = false;
                                }
                                
                                player.dropMessage(item.getCommand() + " - " + item.getShortDesc());
                                resultCount++;
                            }
                        }
                    }
                    

                    if (resultCount == 0) {
                        player.dropMessage("-- No commands found. --");
                    } else {
                        player.dropMessage("");
                        player.dropMessage("-- " + resultCount + " commands were found. --");
                    }

                } else {
                    player.dropMessage("Please use the following syntax: @searchcommand [search].");
                }

                break;
            }
            case "command":
            case "commands":
            {
                if (splitted.length > 1) {
                    int gmLevel = Integer.valueOf(splitted[1]);
                    
                    GMRank gmRank = GMRank.getById(gmLevel);
                    
                    if (gmRank == null) {
                        player.dropMessage("No GM Rank exists with ID: " + gmLevel);
                    }
                    
                    player.dropMessage("-- " + ServerConstants.SERVER_NAME + " commands for '" + gmRank.getName() + "' --");
                    for (CommandInformation item : CommandInformation.getCommandInformations(gmLevel)) {
                        player.dropMessage(item.getCommand() + " - " + item.getShortDesc());
                    }
                } else {
                    player.dropMessage("Please use the following syntax: !commands [rankid]");
                    player.dropMessage("The following GM Rank IDs are available:");
                    for (GMRank gmRank : GMRank.values()) {
                        if (gmRank == player.getGMRank()) {
                            player.dropMessage(gmRank.getId() + " - " + gmRank.getName() + "*");
                        } else {
                            player.dropMessage(gmRank.getId() + " - " + gmRank.getName());
                        }
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

