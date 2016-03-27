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
import client.command.CommandProcessor;
import client.inventory.Item;
import client.sexbot.Muriel;
import org.bson.Document;
import server.TimerManager;
import tools.FilePrinter;
import tools.MaplePacketCreator;
import tools.MongoReporter;
import tools.data.input.SeekableLittleEndianAccessor;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public final class GeneralchatHandler extends net.AbstractMaplePacketHandler {


    public final void handlePacket(SeekableLittleEndianAccessor slea, final MapleClient c, int header) {
        try {
            Document doc = new Document("action","GENERAL_CHAT");
            MapleCharacter chr = c.getPlayer();
            doc.put("char",chr.toLogFormat());
            if (System.currentTimeMillis() - chr.getLastSpoke() < 100) {
                //chr.dropMessage("nowai bruh");
                chr.announce(MaplePacketCreator.enableActions());
                return;
            }
            if(c.getPlayer() != null) c.getPlayer().updateLastActive();
            c.getPlayer().updateLastSpoke();
            //if(chr.getMapId()==970042506 && !chr.isGM()){return;} //Jail
            String text = slea.readMapleAsciiString();
            doc.put("text",text);
            byte show = slea.readByte();
            final String[] ctext = {text.charAt(0) == '$' ? tools.AES.encrypt(chr.getKey(), text.substring(1)) : text};
            final String lctext = text.toLowerCase();
            char heading = text.charAt(0);
            boolean commandProcessed = false;
            String names = chr.getMap().getHitman();
            if (heading == '/' || heading == '!' || heading == '@') {
                try {
                    commandProcessed = CommandProcessor.processCommand(c, text, heading);
                } catch (RemoteException | SQLException ex) {
//                Logger.getLogger(GeneralchatHandler.class.getName()).log(Level.SEVERE, null, ex);
                }

            }

            if (!commandProcessed && !c.isMuted()) {
                MongoReporter.INSTANCE.insertReport(doc);
                chr.getMap().getCharacters().stream().forEach(player -> {
                    String ptext = player.getKey() != null ? tools.AES.decrypt(player.getKey(), ctext[0]) : ctext[0];
                    try {
                        if (chr.getLastSpoke() - System.currentTimeMillis() > 500) {
                            chr.dropMessage(1, "Nigga, stop spamming.");
                            return;
                        }
                        if (!chr.isHidden()) {
                            switch (chr.getChatType()) {
                                default:
                                    chr.setChatType(0);
                                    System.out.println("Unhandled chattype: " + chr.getChatType() + " for player " + chr.getName());
                                case 11:
                                    String normal = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
                                    String leet = "48(d3f9h1jk1mn0PQR57uvwxyz@6cD3F9hiJk|Mn0pqr$7uvWXy2";
                                    for (int i = 0; i < 52; i++) {
                                        ptext = ptext.replace(normal.charAt(i), leet.charAt(i));
                                    }
                                    ptext = ptext.replaceAll("y0u", "j00");
                                case 0:
                                case 1:
                                    player.announce(MaplePacketCreator.getChatText(chr.getId(), ptext, chr.getChatType() == 1, show));
                                    break;
                                case 2:
                                    player.announce(MaplePacketCreator.multiChat(chr.getName(), ptext, 0));
                                    player.announce(MaplePacketCreator.getChatText(chr.getId(), ptext, false, 1));
                                    break;
                                case 3:
                                    player.announce(MaplePacketCreator.multiChat(chr.getName(), ptext, 1));
                                    player.announce(MaplePacketCreator.getChatText(chr.getId(), ptext, false, 1));
                                    break;
                                case 4:
                                    player.announce(MaplePacketCreator.multiChat(chr.getName(), ptext, 2));
                                    player.announce(MaplePacketCreator.getChatText(chr.getId(), ptext, false, 1));
                                    break;
                                case 5:
                                    player.announce(MaplePacketCreator.multiChat(chr.getName(), ptext, 3));
                                    player.announce(MaplePacketCreator.getChatText(chr.getId(), ptext, false, 1));
                                    break;
                                case 6:
                                    player.announce(MaplePacketCreator.serverNotice(5, chr.getName() + " : " + ptext));
                                    player.announce(MaplePacketCreator.getChatText(chr.getId(), ptext, false, 1));
                                    break;
                                case 7:
                                    player.announce(MaplePacketCreator.serverNotice(6, chr.getName() + " : " + ptext));
                                    player.announce(MaplePacketCreator.getChatText(chr.getId(), ptext, false, 1));
                                    break;
                                case 8:
                                    player.announce(MaplePacketCreator.getWhisper(chr.getName(), c.getChannel(), ptext));
                                    player.announce(MaplePacketCreator.getChatText(chr.getId(), ptext, false, 1));
                                    break;
                                case 9:
                                    player.announce(MaplePacketCreator.sendYellowTip(chr.getName() + ": " + ptext));
                                    player.announce(MaplePacketCreator.getChatText(chr.getId(), ptext, false, 1));
                                    break;
                                case 10:
                                    player.announce(MaplePacketCreator.sendSpouseChat(player,ptext));
                            }
                        } else {
                            if (player.isGM()) {
                                player.announce(MaplePacketCreator.getChatText(chr.getId(), ptext, chr.getChatType() == 1, show));
                            }
                        }
                    } catch (Exception ignored) {
                        ignored.printStackTrace();
                    }
                });
                if (lctext.length() > 6) {
                    if (lctext.startsWith("muriel")) {
                        TimerManager.getInstance().schedule((Runnable) () -> {
                            String res = null;
                            try {
                                res = c.getChannelServer().getMuriel().handleChat(lctext, c.getPlayer().getName(), c.getPlayer());
                            } catch (IOException e) {
                                res = null;
                            }
                            if (res != null && res.length() >= 2) {
                                Muriel.getCharacter(c.getChannelServer().getMuriel()).getMap().broadcastMessage(MaplePacketCreator.getChatText(Muriel.getCharacter(c.getChannelServer().getMuriel()).getId(), res, false, 0));
                            }
                        }, new Random(System.currentTimeMillis()).nextInt(4000));
                    }
                }
                chr.updateLastSpoke();
                //Object names = null;
                if (names != null) {
                    if (names.equals(text)) {
                        chr.announce(MaplePacketCreator.serverNotice(6, String.format("[Hitman] Congrats to %s for entering the list of names correctly first!", chr.getName())));
                        chr.getMap().setHitman(null);
                    }
                }
            } else if (!commandProcessed) {
                c.getPlayer().dropMessage(1, "You are muted.");
            }
            if (chr.getMap().getSpeedTyper() != null && lctext.equals(chr.getMap().getSpeedTyper().getMessage())) {
                chr.getMap().broadcastMessage(MaplePacketCreator.serverNotice(6, String.format("[Scramble] Congrats to %s for entering the scrambled sentence first!", chr.getName())));
                chr.dropMessage("You have been awarded 1000 NX points!");
                chr.getCashShop().gainCash(4, 1000);
                chr.getCashShop().addToInventory(new Item(2022179, (byte) 0, (short) 1));
            }
        } catch(Exception e) {
            //THIS SHOULD NOT HAPPEN!
            //FilePrinter.printError("SHOULD_NOT_HAPPEN",e);
            c.announce(MaplePacketCreator.enableActions());
        }
    }
}

