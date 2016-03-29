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

import client.ChatLog;
import client.MapleCharacter;
import client.MapleClient;
import client.sexbot.Muriel;
import net.AbstractMaplePacketHandler;
import net.server.world.World;
import org.bson.Document;
import server.TimerManager;
import tools.DatabaseConnection;
import tools.FilePrinter;
import tools.MaplePacketCreator;
import tools.MongoReporter;
import tools.data.input.SeekableLittleEndianAccessor;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

/**
 *
 * @author Matze
 */
public final class WhisperHandler extends AbstractMaplePacketHandler {
    public final void handlePacket(SeekableLittleEndianAccessor slea, final MapleClient c, int header) {
        byte mode = slea.readByte();
        if(c.getPlayer() != null) c.getPlayer().updateLastActive();
        Document doc = new Document("action","WHISPER");
        doc.put("char",c.getPlayer().toLogFormat());
        if (mode == 6) { // whisper
            String recipient = slea.readMapleAsciiString();
            final String text = slea.readMapleAsciiString();
            if (c.getChannelServer().getMuriel() != null && recipient.equalsIgnoreCase(Muriel.getCharacter(c.getChannelServer().getMuriel()).getName())) {
                c.announce(MaplePacketCreator.getWhisperReply(Muriel.getCharacter(c.getChannelServer().getMuriel()).getName(), (byte) 1));
                TimerManager.getInstance().schedule((Runnable) () -> {
                    String res = null;
                    try {
                        res = c.getChannelServer().getMuriel().handleChat(("sexbot, " + text).toLowerCase(), c.getPlayer().getName(), c.getPlayer());
                    } catch (IOException e) {
                        res = null;
                    }
                    if (res != null && res.length() > 0) {
                        c.announce(MaplePacketCreator.getWhisper(Muriel.getCharacter(c.getChannelServer().getMuriel()).getName(), c.getChannel(), res));
                    }
                }, new Random(System.currentTimeMillis()).nextInt(4000));
                //try {c.getWorldServer().whisper("Muriel",c.getPlayer().getName(),c.getChannel(),c.getChannelServer().getMuriel().handleChat(recipient, text, c.getPlayer()));} catch (IOException e) {}
                return;
            }
            MapleCharacter player = c.getChannelServer().getPlayerStorage().getCharacterByName(recipient);
            if (player != null) {
                player.getClient().announce(MaplePacketCreator.getWhisper(c.getPlayer().getName(), c.getChannel(), text));
                doc.put("target",player.toLogFormat());

                c.announce(MaplePacketCreator.getWhisperReply(recipient, (byte) 1));
            } else {// not found
                doc.put("target",recipient);
                World world = c.getWorldServer();
                    if (world.isConnected(recipient)) {
                        world.whisper(c.getPlayer().getName(), recipient, c.getChannel(), text);
                        c.announce(MaplePacketCreator.getWhisperReply(recipient, (byte) 1));
                    } else {
                        c.announce(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                    }
            }
        } else if (mode == 5) { // - /find
            doc.put("type","FIND");
            String recipient = slea.readMapleAsciiString();
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(recipient);
            doc.put("target", victim == null ? recipient : victim.toLogFormat());
            if (victim != null && c.getPlayer().gmLevel() >= victim.gmLevel()) {
                if (victim.getCashShop().isOpened()) {
                    c.announce(MaplePacketCreator.getFindReply(victim.getName(), -1, 2));
                //} else if (victim.inMTS()) {
                //    c.announce(MaplePacketCreator.getFindReply(victim.getName(), -1, 0));
                } else {
                    c.announce(MaplePacketCreator.getFindReply(victim.getName(), victim.getMap().getId(), 1));
                }
            } else { // not found
                try {
                    PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT gm FROM characters WHERE name = ?");
                    ps.setString(1, recipient);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        if (rs.getInt("gm") > c.getPlayer().gmLevel()) {
                            c.announce(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                            return;
                        }
                    }
                    rs.close();
                    ps.close();
                    //c.getWorldServer().getPlayerStorage().getCharacterByName(recipient);
                    byte channel = (byte) (c.getWorldServer().find(recipient) - 1);
                    if (channel > -1) {
                        c.announce(MaplePacketCreator.getFindReply(recipient, channel, 3));
                    } else {
                        c.announce(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                    }
                } catch (SQLException e) {
                    //e.printStackTrace();
                }
            }
        } else if (mode == 0x44) {
            //Buddy find?
        }
        MongoReporter.INSTANCE.insertReport(doc);
    }
}
