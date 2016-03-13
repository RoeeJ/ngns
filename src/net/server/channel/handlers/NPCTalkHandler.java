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

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import scripting.npc.NPCScriptManager;
import server.life.MapleNPC;
import server.maps.MapleMapObject;
import server.maps.PlayerNPCs;
import tools.FilePrinter;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class NPCTalkHandler extends AbstractMaplePacketHandler {
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c, int header) {
        if (!c.getPlayer().isAlive() || (c.getPlayer().getLastSpokeToNpc() + 2000 > System.currentTimeMillis())) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        if(c.getPlayer() != null) c.getPlayer().updateLastActive();
        int oid = slea.readInt();
        MapleMapObject obj = c.getPlayer().getMap().getMapObject(oid);
        if (obj instanceof MapleNPC) {
            MapleNPC npc = (MapleNPC) obj;
            if(c.getPlayer().isGM()) {
                c.getPlayer().dropMessage("NPC ID:"+npc.getId());
            }
            FilePrinter.print("npcs.txt", String.format("%s(%d)->%d(%s)", c.getPlayer().getName(), c.getPlayer().getId(), npc.getId(), npc.getName()));
            if (npc.getId() == 9010009) {
                c.announce(MaplePacketCreator.sendDuey((byte) 8, DueyHandler.loadItems(c.getPlayer())));
            } else if (npc.hasShop()) {
                if (c.getPlayer().getShop() != null) {
                    return;
                }
                npc.sendShop(c);
            } else {
                if (c.getCM() != null || c.getQM() != null) {
                    c.announce(MaplePacketCreator.enableActions());
                    return;
                }
                NPCScriptManager.getInstance().start(c, npc.getId(), null, null);
                c.getPlayer().updateLastSpokeToNpc();
            }
        } else if (obj instanceof PlayerNPCs) {
            NPCScriptManager.getInstance().start(c, ((PlayerNPCs) obj).getId(), null, null);
        }
    }
}