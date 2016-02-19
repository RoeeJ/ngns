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

import client.MapleCharacter;
import client.MapleClient;
import client.command.CommandInterface;
import net.server.Server;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleNPC;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import tools.DatabaseConnection;
import tools.MaplePacketCreator;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class DeveloperCommand extends AdminCommand implements CommandInterface
{
    @Override
    public boolean execute(MapleClient c, String[] splitted, char heading) throws SQLException, RemoteException {
        
        if (super.execute(c, splitted, heading)) {
            return true;
        }
        
        MapleCharacter player = c.getPlayer();
        
        switch (splitted[0].toLowerCase()) {
            case "monsterdebug":
            {
                List<MapleMapObject> monsters = player.getMap().getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER));
                for (MapleMapObject monstermo : monsters) {
                    MapleMonster monster = (MapleMonster) monstermo;
                    player.message("Monster ID: " + monster.getId() +  " - Object ID: " + monster.getObjectId());
                }
                break;
            }
            case "setgmlevel": {
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                victim.setGM(Integer.parseInt(splitted[2]));
                victim.getClient().getSession().close(true);
                player.message("Done.");
                break;
            }
            case "mesofeg": {
                Server.getInstance().shutdown();
                break;
            }
            case "playernpc": {
                player.playerNPC(c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]), Integer.parseInt(splitted[2]));
                break;
            }
            case "removenpc": {
                MapleNPC remnpc = MapleLifeFactory.getNPC(Integer.parseInt(splitted[1]));
                if (remnpc != null) {
                    remnpc.setPosition(player.getPosition());
                    remnpc.setCy(player.getPosition().y);
                    remnpc.setRx0(player.getPosition().x + 50);
                    remnpc.setRx1(player.getPosition().x - 50);
                    remnpc.setFh(player.getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId());
                    player.getMap().removeMapObject(remnpc);
                }
                break;
            }
            case "exprate": {
                c.getWorldServer().setExpRate(Integer.parseInt(splitted[1]));
                c.getWorldServer().getPlayerStorage().getAllCharacters().forEach(client.MapleCharacter::setRates);
                break;
            }
            case "pnpc": {
                int npcId = Integer.parseInt(splitted[1]);
                MapleNPC npc = MapleLifeFactory.getNPC(npcId);
                int xpos = player.getPosition().x;
                int ypos = player.getPosition().y;
                int fh = player.getMap().getFootholds().findBelow(player.getPosition()).getId();
                if (npc != null && !npc.getName().equals("MISSINGNO")) {
                    npc.setPosition(player.getPosition());
                    npc.setCy(ypos);
                    npc.setRx0(xpos + 50);
                    npc.setRx1(xpos - 50);
                    npc.setFh(fh);
                    try {
                        Connection con = DatabaseConnection.getConnection();
                        PreparedStatement ps = con.prepareStatement("INSERT INTO spawns ( idd, f, fh, cy, rx0, rx1, type, x, y, mid ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )");
                        ps.setInt(1, npcId);
                        ps.setInt(2, 0);
                        ps.setInt(3, fh);
                        ps.setInt(4, ypos);
                        ps.setInt(5, xpos + 50);
                        ps.setInt(6, xpos - 50);
                        ps.setString(7, "n");
                        ps.setInt(8, xpos);
                        ps.setInt(9, ypos);
                        ps.setInt(10, player.getMapId());
                        ps.executeUpdate();
                    } catch (SQLException e) {
                        player.dropMessage("Failed to save NPC to the database");
                    }
                    player.getMap().addMapObject(npc);
                    player.getMap().broadcastMessage(MaplePacketCreator.spawnNPC(npc));
                } else {
                    player.dropMessage("You have entered an invalid Npc-Id");
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

