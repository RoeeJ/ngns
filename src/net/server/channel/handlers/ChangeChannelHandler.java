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

import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.inventory.MapleInventoryType;
import net.AbstractMaplePacketHandler;
import net.server.Server;
import server.MapleTrade;
import server.maps.FieldLimit;
import server.maps.HiredMerchant;
import tools.FilePrinter;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

import java.io.IOException;
import java.net.InetAddress;

/**
 *
 * @author Matze
 */
public final class ChangeChannelHandler extends AbstractMaplePacketHandler {

    @Override
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c, int header) {
        MapleCharacter chr = c.getPlayer();
        if(c.getPlayer() != null) c.getPlayer().updateLastActive();
        int channel = slea.readByte() + 1;
        int curchannel = chr.getClient().getChannel();
        Server server = Server.getInstance();
        if (channel == 6 && (c.getAge() < 20 && !c.isGM())) {
            if (c.getLoginState() == MapleClient.LOGIN_LOGGEDIN) {
                c.getPlayer().dropMessage(String.format("Channel %d is for players older than 20.", channel));
                c.announce(MaplePacketCreator.enableActions());
                return;
            }
        }
        if (chr.isBanned()) {
            c.disconnect(false, false);
            return;
        }
        if (!chr.isAlive() || FieldLimit.CHANGECHANNEL.check(chr.getMap().getFieldLimit())) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        String[] socket = Server.getInstance().getIP(c.getWorld(), channel).split(":");
        if (chr.getTrade() != null) {
            MapleTrade.cancelTrade(c.getPlayer());
        }

        HiredMerchant merchant = chr.getHiredMerchant();
        if (merchant != null) {
            if (merchant.isOwner(c.getPlayer())) {
                merchant.setOpen(true);
            } else {
                merchant.removeVisitor(c.getPlayer());
            }
        }
        server.getPlayerBuffStorage().addBuffsToStorage(chr.getId(), chr.getAllBuffs());
        chr.cancelBuffEffects();
        chr.cancelMagicDoor();
        chr.saveCooldowns();
        //Canceling mounts? Noty
        if (chr.getBuffedValue(MapleBuffStat.PUPPET) != null) {
            chr.cancelEffectFromBuffStat(MapleBuffStat.PUPPET);
        }
        if (chr.getBuffedValue(MapleBuffStat.COMBO) != null) {
            chr.cancelEffectFromBuffStat(MapleBuffStat.COMBO);
        }
        chr.getInventory(MapleInventoryType.EQUIPPED).checked(false); //test
        chr.getMap().removePlayer(chr);
        chr.getClient().getChannelServer().removePlayer(chr);
        chr.saveToDB();
        chr.getClient().updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
        FilePrinter.print("channels.txt", String.format("%s->%d(%d)", chr.getName(), channel, curchannel));
        try {
            c.announce(MaplePacketCreator.getChannelChange(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1])));
        } catch (IOException ignored) {
        }
    }
}