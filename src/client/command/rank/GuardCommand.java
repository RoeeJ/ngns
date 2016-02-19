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

import client.MapleCharacter;
import client.MapleClient;
import client.command.CommandInterface;
import net.server.channel.Channel;
import tools.MaplePacketCreator;
import tools.StringUtil;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Objects;

public class GuardCommand extends JrGuardCommand implements CommandInterface
{
    @Override
    public boolean execute(MapleClient c, String[] splitted, char heading) throws SQLException, RemoteException {
        
        if (super.execute(c, splitted, heading)) {
            return true;
        }
        
        MapleCharacter player = c.getPlayer();
        Channel cserv = c.getChannelServer();
        
        switch (splitted[0].toLowerCase()) {
            case "cancelbuffs":
            {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim.gmLevel() > player.gmLevel()) {
                    if (victim.gmLevel() == 5) {
                        
                    } else {
                        player.dropMessage("You can't cancelbuffs a GM with an equal or higher GM level!");
                    }
                } else {
                    victim.cancelAllBuffs(false);
                    player.dropMessage("Done.");
                }   victim.cancelAllBuffs(false);
                break;
            }
            case "gtfo":
            case "ban":
            {
                if (splitted.length < 3) {
                    player.dropMessage("Syntax for !ban: !ban [user] [reason]");
                    return true;
                }
                
                String originalReason = StringUtil.joinStringFrom(splitted, 2);
                String reason = c.getPlayer().getName() + " banned " + splitted[1] + ": " + originalReason;
                MapleCharacter target = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (target != null) {
                    if (target.gmLevel() <= 3) {
                        if (Objects.equals(splitted[0], "gtfo") && player.gmLevel() >= 127) {
                            target.getClient().banMacs();
                        }
                        target.ban(reason);
                        //target.sendPolice("You have been banned for: " + originalReason);
                        target.announce(MaplePacketCreator.openUI((byte) 33));
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
