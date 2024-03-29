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
import client.MapleRing;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import com.google.common.collect.ImmutableMap;
import constants.GameConstants;
import net.AbstractMaplePacketHandler;
import org.bson.Document;
import server.CashShop;
import server.CashShop.CashItem;
import server.CashShop.CashItemFactory;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import tools.MaplePacketCreator;
import tools.MongoReporter;
import tools.data.input.SeekableLittleEndianAccessor;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class CashOperationHandler extends AbstractMaplePacketHandler {

    private static boolean checkBirthday(MapleClient c, int idate) {
        int year = idate / 10000;
        int month = (idate - year * 10000) / 100;
        int day = idate - year * 10000 - month * 100;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(0);
        cal.set(year, month - 1, day);
        return c.checkBirthDate(cal);
    }

    public static boolean canBuy(CashItem item, int cash) {
        return item != null && item.isOnSale() && item.getPrice() <= cash;
    }

    @Override
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c, int header) {

        MapleCharacter chr = c.getPlayer();
        CashShop cs = chr.getCashShop();
        if (!cs.isOpened()) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        if(c.getPlayer() != null) c.getPlayer().updateLastActive();
        Document doc = new Document("action","CS_OPERATION");
        doc.put("char",chr.toLogFormat());
        final int action = slea.readByte();
        if (action == 0x03 || action == 0x1E) {
            doc.put("operation","PURCHASE");
            slea.readByte();
            final int useNX = slea.readInt();
            final int snCS = slea.readInt();
            CashItem cItem = CashItemFactory.getItem(snCS);
            if (!canBuy(cItem,cs.getCash(useNX))) {
                //c.announce(MaplePacketCreator.serverNotice(1,"nowai bruh."));
                return;
            }
            if (action == 0x03) { // Item
                doc.put("type","ITEM");
                Item item = cItem.toItem();
                doc.put("item", ImmutableMap.of("id",item.getItemId(),"quantity",item.getQuantity()));
                cs.addToInventory(item);
                c.announce(MaplePacketCreator.showBoughtCashItem(item, c.getAccID()));
            } else { // Package
                doc.put("type","PACKAGE");
                List<Item> cashPackage = CashItemFactory.getPackage(cItem.getItemId());
                for (Item item : cashPackage) {
                    cs.addToInventory(item);
                }
                doc.put("item",cashPackage.stream().map(
                        (item)-> ImmutableMap.of("id",item.getItemId(),"quantity",item.getQuantity())
                ).collect(Collectors.toList()));
                c.announce(MaplePacketCreator.showBoughtCashPackage(cashPackage, c.getAccID()));
            }
            cs.gainCash(useNX, -cItem.getPrice());
            c.announce(MaplePacketCreator.showCash(chr));
            doc.put("price",cItem.getPrice());
            MongoReporter.INSTANCE.insertReport(doc);
        } else if (action == 0x04) {//TODO check for gender
            int birthday = slea.readInt();
            CashItem cItem = CashItemFactory.getItem(slea.readInt());
            Map<String, String> recipient = MapleCharacter.getCharacterFromDatabase(slea.readMapleAsciiString());
            String message = slea.readMapleAsciiString();
            doc.putAll(ImmutableMap.of(
                    "operation","GIFT",
                    "message",message,
                    "char",chr.toLogFormat()
            ));
            if (!canBuy(cItem, cs.getCash(4)) || message.length() < 1 || message.length() > 73) {
                //c.announce(MaplePacketCreator.serverNotice(1,"nowai bruh."));
                return;
            }
            if (!checkBirthday(c, birthday)) {
                c.announce(MaplePacketCreator.showCashShopMessage((byte) 0xC4));
                return;
            } else if (recipient == null) {
                c.announce(MaplePacketCreator.showCashShopMessage((byte) 0xA9));
                return;
            } else if (recipient.get("accountid").equals(String.valueOf(c.getAccID()))) {
                c.announce(MaplePacketCreator.showCashShopMessage((byte) 0xA8));
                return;
            }
            doc.put("recipient",ImmutableMap.of("id",recipient.get("id"),"name",recipient.get("name"),"accountid",recipient.get("accountid")));
            MongoReporter.INSTANCE.insertReport(doc);
            cs.gift(Integer.parseInt(recipient.get("id")), chr.getName(), message, cItem.getSN());
            c.announce(MaplePacketCreator.showGiftSucceed(recipient.get("name"), cItem));
            cs.gainCash(4, -cItem.getPrice());
            c.announce(MaplePacketCreator.showCash(chr));
            try {
                chr.sendNote(recipient.get("name"), chr.getName() + " has sent you a gift! Go check out the Cash Shop.", (byte) 0); //fame or not
            } catch (SQLException ignored) { }
            MapleCharacter receiver = c.getChannelServer().getPlayerStorage().getCharacterByName(recipient.get("name"));
            if (receiver != null) receiver.showNote();
        } else if (action == 0x05) { // Modify wish list
            cs.clearWishList();
            for (byte i = 0; i < 10; i++) {
                int sn = slea.readInt();
                CashItem cItem = CashItemFactory.getItem(sn);
                if (cItem != null && cItem.isOnSale() && sn != 0) {
                    cs.addToWishList(sn);
                }
            }
            c.announce(MaplePacketCreator.showWishList(chr, true));
        } else if (action == 0x06) { // Increase Inventory Slots
            slea.skip(1);
            int cash = slea.readInt();
            byte mode = slea.readByte();
            if (mode == 0) {
                byte type = slea.readByte();
                if (cs.getCash(cash) < 4000) {
                    return;
                }
                if (chr.gainSlots(type, 4, false)) {
                    c.announce(MaplePacketCreator.showBoughtInventorySlots(type, chr.getSlots(type)));
                    cs.gainCash(cash, -4000);
                    c.announce(MaplePacketCreator.showCash(chr));
                }
            } else {
                CashItem cItem = CashItemFactory.getItem(slea.readInt());
                int type = (cItem.getItemId() - 9110000) / 1000;
                if (!canBuy(cItem, cs.getCash(cash))) {
                    return;
                }
                if (chr.gainSlots(type, 8, false)) {
                    c.announce(MaplePacketCreator.showBoughtInventorySlots(type, chr.getSlots(type)));
                    cs.gainCash(cash, -cItem.getPrice());
                    c.announce(MaplePacketCreator.showCash(chr));
                }
            }
        } else if (action == 0x07) { // Increase Storage Slots
            slea.skip(1);
            int cash = slea.readInt();
            byte mode = slea.readByte();
            if (mode == 0) {
                if (cs.getCash(cash) < 4000) {
                    return;
                }
                if (chr.getStorage().gainSlots(4)) {
                    c.announce(MaplePacketCreator.showBoughtStorageSlots(chr.getStorage().getSlots()));
                    cs.gainCash(cash, -4000);
                    c.announce(MaplePacketCreator.showCash(chr));
                }
            } else {
                CashItem cItem = CashItemFactory.getItem(slea.readInt());

                if (!canBuy(cItem, cs.getCash(cash))) {
                    return;
                }
                if (chr.getStorage().gainSlots(8)) {
                    c.announce(MaplePacketCreator.showBoughtStorageSlots(chr.getStorage().getSlots()));
                    cs.gainCash(cash, -cItem.getPrice());
                    c.announce(MaplePacketCreator.showCash(chr));
                }
            }
        } else if (action == 0x08) { // Increase Character Slots
            slea.skip(1);
                int cash = slea.readInt();
                CashItem cItem = CashItemFactory.getItem(slea.readInt());

                if (!canBuy(cItem, cs.getCash(cash)))
                    return;

                if (c.gainCharacterSlot()) {
                    c.announce(MaplePacketCreator.showBoughtCharacterSlot(c.getCharacterSlots()));
                    cs.gainCash(cash, -cItem.getPrice());
                    c.announce(MaplePacketCreator.showCash(chr));
                }
        } else if (action == 0x0D) { // Take from Cash Inventory
            Item item = cs.findByCashId(slea.readInt());
            if (item == null) {
                return;
            }
            if (chr.getInventory(MapleItemInformationProvider.getInstance().getInventoryType(item.getItemId())).addItem(item) != -1) {
                cs.removeFromInventory(item);
                c.announce(MaplePacketCreator.takeFromCashInventory(item));
            }
        } else if (action == 0x0E) { // Put into Cash Inventory
            int cashId = slea.readInt();
            slea.skip(4);
            MapleInventory mi = chr.getInventory(MapleInventoryType.getByType(slea.readByte()));
            Item item = mi.findByCashId(cashId);
            if (item == null) {
                return;
            }
            cs.addToInventory(item);
            mi.removeSlot(item.getPosition());
            c.announce(MaplePacketCreator.putIntoCashInventory(item, c.getAccID()));
        } else if (action == 0x1D) { //crush ring (action 28)
            doc.put("operation","CRUSH_RING");
            if (checkBirthday(c, slea.readInt())) {
                int toCharge = slea.readInt();
                int SN = slea.readInt();
                String recipient = slea.readMapleAsciiString();
                String text = slea.readMapleAsciiString();
                CashItem ring = CashItemFactory.getItem(SN);
                if (!GameConstants.isValidRing(ring) || !canBuy(ring,chr.getCashShop().getCash(toCharge))) {
                    //c.announce(MaplePacketCreator.serverNotice(1,"nowai bruh."));
                    return;
                }
                MapleCharacter partner = c.getChannelServer().getPlayerStorage().getCharacterByName(recipient);
                if (partner == null) {
                    chr.getClient().announce(MaplePacketCreator.serverNotice(1, "The partner you specified cannot be found.\r\nPlease make sure your partner is online and in the same channel."));
                } else {
                    Equip item = (Equip) ring.toItem();
                    int ringid = MapleRing.createRing(ring.getItemId(), chr, partner);
                    item.setRingId(ringid);
                    doc.putAll(ImmutableMap.of(
                            "price",toCharge,
                            "char",chr.toLogFormat(),
                            "partner",partner.toLogFormat(),
                            "ringIds",new int[] {ringid,ringid+1}
                    ));
                    cs.addToInventory(item);
                    c.announce(MaplePacketCreator.showBoughtCashItem(item, c.getAccID()));
                    cs.gift(partner.getId(), chr.getName(), text, item.getSN(), (ringid + 1));
                    cs.gainCash(toCharge, -ring.getPrice());
                    chr.addCrushRing(MapleRing.loadFromDb(ringid));
                    MongoReporter.INSTANCE.insertReport(doc);
                    try {
                        chr.sendNote(partner.getName(), text, (byte) 1);
                    } catch (SQLException ex) {
                    }
                    partner.showNote();
                }
            } else {
                chr.dropMessage("The birthday you entered was incorrect.");
            }
            c.announce(MaplePacketCreator.showCash(c.getPlayer()));
        } else if (action == 0x20) { // everything is 1 meso...
            int itemId = CashItemFactory.getItem(slea.readInt()).getItemId();
            if (chr.getMeso() > 0) {
                if (itemId == 4031180 || itemId == 4031192 || itemId == 4031191) {
                    chr.gainMeso(-1, false);
                    MapleInventoryManipulator.addById(c, itemId, (short) 1);
                    c.announce(MaplePacketCreator.showBoughtQuestItem(itemId));
                }
            }
            c.announce(MaplePacketCreator.showCash(c.getPlayer()));
        } else if (action == 0x23) { //Friendship :3
            doc.put("operation","FRIENDSHIP_RING");
            if (checkBirthday(c, slea.readInt())) {
                int payment = slea.readByte();
                slea.skip(3); //0s
                int snID = slea.readInt();
                CashItem ring = CashItemFactory.getItem(snID);
                if (!GameConstants.isValidRing(ring) || !canBuy(ring,chr.getCashShop().getCash(payment))) {
                    //c.announce(MaplePacketCreator.serverNotice(1,"nowai bruh."));
                    return;
                }
                String sentTo = slea.readMapleAsciiString();
                int available = slea.readShort() - 1;
                String text = slea.readAsciiString(available);
                slea.readByte();
                MapleCharacter partner = c.getChannelServer().getPlayerStorage().getCharacterByName(sentTo);
                if (partner == null) {
                    chr.dropMessage("The partner you specified cannot be found.\r\nPlease make sure your partner is online and in the same channel.");
                } else {
                    Equip item = (Equip) ring.toItem();
                    int ringid = MapleRing.createRing(ring.getItemId(), chr, partner);
                    item.setRingId(ringid);
                    cs.addToInventory(item);
                    c.announce(MaplePacketCreator.showBoughtCashItem(item, c.getAccID()));
                    cs.gift(partner.getId(), chr.getName(), text, item.getSN(), (ringid + 1));
                    cs.gainCash(payment, -ring.getPrice());
                    chr.addFriendshipRing(MapleRing.loadFromDb(ringid));
                    try {
                        chr.sendNote(partner.getName(), text, (byte) 1);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                    doc.putAll(ImmutableMap.of(
                            "price",ring.getPrice(),
                            "char",chr.toLogFormat(),
                            "partner",partner.toLogFormat(),
                            "item",item.toLogFormat()
                    ));
                    MongoReporter.INSTANCE.insertReport(doc);
                    partner.showNote();
                }
            } else {
                chr.dropMessage("The birthday you entered was incorrect.");
            }
            c.announce(MaplePacketCreator.showCash(c.getPlayer()));
        } else {
            System.out.println(slea);
        }
    }
}
