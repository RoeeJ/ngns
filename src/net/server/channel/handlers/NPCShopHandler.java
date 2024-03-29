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
import com.google.common.collect.ImmutableMap;
import net.AbstractMaplePacketHandler;
import org.bson.Document;
import server.MapleItemInformationProvider;
import tools.FilePrinter;
import tools.MongoReporter;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * 
 * @author Matze
 */
public final class NPCShopHandler extends AbstractMaplePacketHandler {
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c, int header) {
        Document doc = new Document("action","NPC_SHOP");
        byte bmode = slea.readByte();
        if (bmode == 0) { // mode 0 = buy :)
            doc.put("type","BUY");
            short slot = slea.readShort();// slot
            int itemId = slea.readInt();
            short quantity = slea.readShort();
            doc.put("item",new Document(ImmutableMap.of("id",itemId,"quantity",quantity)));
            if (0 > quantity || quantity > 20000) {
                //c.getPlayer().dropMessage("nowai bruh");
                return;
            }
            c.getPlayer().getShop().buy(c, slot, itemId, quantity);
        } else if (bmode == 1) { // sell ;)
            doc.put("type","SELL");
            short slot = slea.readShort();
            int itemId = slea.readInt();
            short quantity = slea.readShort();
            if (0 > quantity || quantity > 20000) {
                //c.getPlayer().dropMessage("nowai bruh");
                return;
            }
            c.getPlayer().getShop().sell(c, MapleItemInformationProvider.getInstance().getInventoryType(itemId), slot, quantity);
        } else if (bmode == 2) { // recharge ;)
            byte slot = (byte) slea.readShort();
            c.getPlayer().getShop().recharge(c, slot);
            doc.put("type","RECHARGE");
        } else if (bmode == 3) // leaving :(
        {
            c.getPlayer().setShop(null);
        }
        MongoReporter.INSTANCE.insertReport(doc);
    }
}
