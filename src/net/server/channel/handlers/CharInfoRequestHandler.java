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
package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import client.sexbot.Muriel;
import net.AbstractMaplePacketHandler;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class CharInfoRequestHandler extends AbstractMaplePacketHandler {
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c, int header) {
        slea.readInt();
        int cid = slea.readInt();
        MapleCharacter player = (MapleCharacter) c.getPlayer().getMap().getMapObject(cid);
        if (c.getChannelServer().getMuriel() != null && Muriel.getCharacter(c.getChannelServer().getMuriel()).getId() == player.getId()) {
            MapleCharacter sb = Muriel.getCharacter(c.getChannelServer().getMuriel());
            sb.getMap().broadcastMessage(MaplePacketCreator.getChatText(sb.getId(), "Dont touch me, perv!", false, 1));
            c.getPlayer().setHp(c.getPlayer().getHp() - c.getPlayer().getMaxHp() / 10);
            c.getPlayer().dropMessage(String.format("%s slaps you and hits your for 10%% of your hitpoints!", sb.getName()));
            return;
        }
        if (player.isGM() && !c.getPlayer().isGM()) {
            c.announce(MaplePacketCreator.charInfo(c.getPlayer()));
        } else {
            c.announce(MaplePacketCreator.charInfo(player));
        }
    }
}
