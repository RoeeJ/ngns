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
import client.MapleStat;
import client.sexbot.Muriel;
import net.AbstractMaplePacketHandler;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class GiveFameHandler extends AbstractMaplePacketHandler {
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c, int header) {
        MapleCharacter target = (MapleCharacter) c.getPlayer().getMap().getMapObject(slea.readInt());
        int mode = slea.readByte();
        int famechange = 2 * mode - 1;
        MapleCharacter player = c.getPlayer();
        if (c.getChannelServer().getMuriel() != null && Muriel.getCharacter(c.getChannelServer().getMuriel()).getId() == target.getId()) {
            Muriel.getCharacter(c.getChannelServer().getMuriel()).getMap().broadcastMessage(MaplePacketCreator.getChatText(Muriel.getCharacter(c.getChannelServer().getMuriel()).getId(), "noty " + player.getName(), false, 1));
            return;
        }
        if ((target == player || player.getLevel() < 15)) {
            player.announce(MaplePacketCreator.enableActions());
            return;
        }
        if(famechange > 1 || famechange < -1){
            player.dropMessage("No fame hacking, asshole!"); //TODO: add to megatron's logs
            return;
        }
        if(c.getPlayer() != null) c.getPlayer().updateLastActive();
        switch (player.canGiveFame(target)) {
            case OK:
                if (Math.abs(target.getFame() + famechange) < 30001) {
                    target.addFame(famechange);
                    target.updateSingleStat(MapleStat.FAME, target.getFame());
                }
                if (!player.isGM()) {
                    player.hasGivenFame(target);
                }
                c.announce(MaplePacketCreator.giveFameResponse(mode, target.getName(), target.getFame()));
                target.getClient().announce(MaplePacketCreator.receiveFame(mode, player.getName()));
                break;
            case NOT_TODAY:
                c.announce(MaplePacketCreator.giveFameErrorResponse(3));
                break;
            case NOT_THIS_MONTH:
                c.announce(MaplePacketCreator.giveFameErrorResponse(4));
                break;
        }
    }
}
