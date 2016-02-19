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
import client.inventory.PetDataFactory;
import net.AbstractMaplePacketHandler;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class PetChatHandler extends AbstractMaplePacketHandler {

    @Override
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        long petId = slea.readLong();
        int petIndex = c.getPlayer().getPetIndex(petId);
        if (petIndex < 0) {
            return;
        }
        int nType = slea.readByte();
        int nAction = slea.readByte();
        String sChat = slea.readMapleAsciiString();
        if (nAction < 1 || PetDataFactory.IsValidPetAction(petId, nAction)) {
            c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.petChat(c.getPlayer().getId(), petIndex, nType, nAction, sChat, false));
        }
    }
}