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

import java.rmi.RemoteException;
import java.sql.SQLException;
import server.life.MapleMonster;
import client.MapleCharacter;
import client.MapleClient;
import client.command.CommandInterface;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import net.server.Server;
import net.server.channel.Channel;
import server.TimerManager;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;

public class OwnerCommand extends AdminCommand implements CommandInterface
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
            case "shutdown":
            {
                int time = 1000;
                time *= Integer.parseInt(splitted[1]);

                Collection<Channel> cservs = Server.getInstance().getAllChannels();
                for (Channel curCserv : cservs) {
                    for (MapleCharacter mch : curCserv.getPlayerStorage().getAllCharacters()) {
                        if (true || !mch.equals(player)) {
                            player.showMessage("Server will be shut down in " + Integer.parseInt(splitted[1]) + " seconds.");
                            player.dropMessage("Server will be shut down in " + Integer.parseInt(splitted[1]) + " seconds.");
                        }
                    }
                }

                TimerManager.getInstance().schedule(Server.getInstance().shutdown(false), time);
                break;
            }
            case "shutdownnow":
            {
                player.dropMessage("Server will shut down now.");

                TimerManager.getInstance().schedule(Server.getInstance().shutdown(false), 1);
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

