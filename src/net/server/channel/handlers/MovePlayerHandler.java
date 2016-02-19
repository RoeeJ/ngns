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
import client.sexbot.SexBot;
import server.TimerManager;
import server.life.MapleMonster;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.movement.LifeMovementFragment;
import tools.FilePrinter;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

public final class MovePlayerHandler extends AbstractMovementPacketHandler {
    public final void handlePacket(SeekableLittleEndianAccessor slea, final MapleClient c) {
        slea.skip(9);
        List<LifeMovementFragment> res = parseMovement(slea);
        if (res != null) {
        	c.getPlayer().setLastRes(res);
            updatePosition(res, c.getPlayer(), 0);
            c.getPlayer().getMap().movePlayer(c.getPlayer(), c.getPlayer().getPosition());
            if (c.getPlayer().isHidden()) {
                c.getPlayer().getMap().broadcastGMMessage(c.getPlayer(), MaplePacketCreator.movePlayer(c.getPlayer().getId(), res), false);
            } else {
                c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.movePlayer(c.getPlayer().getId(), res), false);
            }
            if (c.getPlayer().isVaccer()) {
                MapleMap map = c.getPlayer().getMap();
                for (MapleMapObject monstermo : map.getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER))) {
                    final MapleMonster monster = (MapleMonster) monstermo;
                    //broadcastMessage(MaplePacketCreator.moveMonster(false, -1, 0, 0, 0, 0, monster.getObjectId(), monster.getPosition(), getVaccer().getLastRes()));
                    map.broadcastMessage(MaplePacketCreator.moveMonster(0, 0, 0, 0, 0, monster.getObjectId(), monster.getPosition(), res));
                    monster.setPosition(c.getPlayer().getPosition());
                    updatePosition(res, monster, 0);
                }
            }
            if (c.getChannelServer().getSexBot() != null) {
                if (c.getChannelServer().getSexBot().getFollow() == c.getPlayer()) {
                    if (c.getPlayer().getMap() == SexBot.getCharacter(c.getChannelServer().getSexBot()).getMap() && !c.getChannelServer().getSexBot().isPatroling()) {
                        final MapleCharacter player2 = SexBot.getCharacter(c.getChannelServer().getSexBot());
                        final List<LifeMovementFragment> res2 = res;

                        TimerManager.getInstance().schedule(() -> {
                            byte[] packet = MaplePacketCreator.movePlayer(player2.getId(), res2);
                            player2.getMap().broadcastMessage(player2, packet, false);
                            updatePosition(res2, player2, 30);
                            Point newpos = player2.getPosition();
                            newpos.x = newpos.x + 30;
                            player2.getMap().movePlayer(player2, newpos);
                            if (c.getChannelServer().getSexBot().isRecording()) {
                                c.getChannelServer().getSexBot().record(res2);
                                c.getPlayer().dropMessage(c.getChannelServer().getSexBot().getRecordingSize());
                            }
                        }, 500);

                    } else {
                        c.getChannelServer().getSexBot().setFollow(null);
                    }
                }
            }
        }
    }
}
