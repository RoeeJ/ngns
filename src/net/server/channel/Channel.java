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
package net.server.channel;

import client.MapleCharacter;
import client.sexbot.Muriel;
import constants.ServerConstants;
import net.MapleServerHandler;
import net.mina.MapleCodecFactory;
import net.server.PlayerStorage;
import net.server.world.MapleParty;
import net.server.world.MaplePartyCharacter;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.SimpleBufferAllocator;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import provider.MapleDataProviderFactory;
import scripting.event.EventScriptManager;
import server.TimerManager;
import server.events.gm.MapleEvent;
import server.expeditions.MapleExpedition;
import server.expeditions.MapleExpeditionType;
import server.maps.HiredMerchant;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import tools.MaplePacketCreator;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

public final class Channel {

    public boolean portalOpen;
    public boolean eventRunning;
    public MapleMap eventMap;
    private PlayerStorage players = new PlayerStorage();
    private int world, channel;
    private NioSocketAcceptor acceptor;
    private String ip, serverMessage;
    private MapleMapFactory mapFactory;
    private EventScriptManager eventSM;
    private Map<Integer, HiredMerchant> hiredMerchants = new HashMap<>();
    private ReentrantReadWriteLock merchant_lock = new ReentrantReadWriteLock(true);
    private EnumMap<MapleExpeditionType, MapleExpedition> expeditions = new EnumMap<>(MapleExpeditionType.class);
    private MapleEvent event;
    private boolean finishedShutdown = false;
    private String[] lastEvent = new String[3];
    private long lastEventTime;
    private boolean eventKill;
    private Muriel muriel = null;
    private boolean scramble;

    public Channel(final int world, final int channel) {
        this.world = world;
        this.channel = channel;
        this.mapFactory = new MapleMapFactory(MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Map.wz")), MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/String.wz")), world, channel);

        try {
            eventSM = new EventScriptManager(this, ServerConstants.EVENTS.split(" "));
            int port = 7575 + this.channel - 1;
            port += (world * 100);
            ip = ServerConstants.HOST + ":" + port;
            IoBuffer.setUseDirectBuffer(false);
            IoBuffer.setAllocator(new SimpleBufferAllocator());
            acceptor = new NioSocketAcceptor();
            TimerManager.getInstance().register(new respawnMaps(), 10000);
            acceptor.setReuseAddress(true);
            acceptor.setHandler(new MapleServerHandler(world, channel));
            acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 30);
            acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new MapleCodecFactory()));
            acceptor.bind(new InetSocketAddress(port));
            acceptor.getSessionConfig().setTcpNoDelay(true);

            eventSM.init();
            System.out.println("    Channel " + getId() + ": Listening on port " + port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public final void shutdown() {
        try {
            System.out.println("Shutting down Channel " + channel + " on World " + world);

            closeAllMerchants();
            System.out.println(String.format("Closed all merchants on channel %d on world %d",channel,world));
            players.disconnectAll();
            System.out.println(String.format("Unbinding on channel %d", channel));
            acceptor.setCloseOnDeactivation(true);
            new Thread(()->{
                acceptor.getManagedSessions().values().parallelStream().forEach((conn) -> conn.close(true));
            }).start();
            acceptor.unbind();
            acceptor.dispose();

            finishedShutdown = true;
            System.out.println("Successfully shut down Channel " + channel + " on World " + world + "\r\n");
        } catch (Exception e) {
            //System.err.println("Error while shutting down Channel " + channel + " on World " + world + "\r\n" + e);
            e.printStackTrace();
        }
    }

    public void closeAllMerchants() {
        WriteLock wlock = merchant_lock.writeLock();
        wlock.lock();
        try {
            final Iterator<HiredMerchant> hmit = hiredMerchants.values().iterator();
            while (hmit.hasNext()) {
                hmit.next().forceClose();
                hmit.remove();
            }
        } catch (Exception e) {
        } finally {
            wlock.unlock();
        }
    }

    public MapleMapFactory getMapFactory() {
        return mapFactory;
    }

    public int getWorld() {
        return world;
    }

    public Muriel getMuriel() {
        return this.muriel;
    }

    public void setMuriel(Muriel newsb) {
        this.muriel = newsb;
    }
    public void addPlayer(MapleCharacter chr) {
        players.addPlayer(chr);
        chr.announce(MaplePacketCreator.serverMessage(serverMessage));
    }

    public PlayerStorage getPlayerStorage() {
        return players;
    }

    public void removePlayer(MapleCharacter chr) {
        players.removePlayer(chr.getId());
    }

    public int getConnectedClients() {
        return players.getAllCharacters().size();
    }

    public void broadcastPacket(final byte[] data) {
        for (MapleCharacter chr : players.getAllCharacters()) {
            chr.announce(data);
        }
    }

    public final int getId() {
        return channel;
    }

    public String getIP() {
        return ip;
    }

    public MapleEvent getEvent() {
        return event;
    }

    public void setEvent(MapleEvent event) {
        this.event = event;
    }

    public EventScriptManager getEventSM() {
        return eventSM;
    }

    public void broadcastGMPacket(final byte[] data) {
        for (MapleCharacter chr : players.getAllCharacters()) {
            if (chr.isGM()) {
                chr.announce(data);
            }
        }
    }

    public void broadcastGMPacket(final byte[] data, String exclude) {
        for (MapleCharacter chr : players.getAllCharacters()) {
            if (chr.isGM() && !chr.getName().equals(exclude)) {
                chr.announce(data);
            }
        }
    }

    public void yellowWorldMessage(String msg) {
        for (MapleCharacter mc : getPlayerStorage().getAllCharacters()) {
            mc.announce(MaplePacketCreator.sendYellowTip(msg));
        }
    }

    public void worldMessage(String msg) {
        for (MapleCharacter mc : getPlayerStorage().getAllCharacters()) {
            mc.dropMessage(msg);
        }
    }

    public List<MapleCharacter> getPartyMembers(MapleParty party) {
        List<MapleCharacter> partym = new ArrayList<>(8);
        for (MaplePartyCharacter partychar : party.getMembers()) {
            if (partychar.getChannel() == getId()) {
                MapleCharacter chr = getPlayerStorage().getCharacterByName(partychar.getName());
                if (chr != null) {
                    partym.add(chr);
                }
            }
        }
        return partym;


    }

    public boolean isEventKill() {
        return this.eventKill;
    }

    public void setEventKill(boolean on) {
        this.eventKill = on;
    }

    public boolean isPortalOpen() {
        return this.portalOpen;
    }

    public void setPortalOpen(boolean on) {
        this.portalOpen = on;
    }

    public boolean isEventRunning() {
        return this.eventRunning;
    }

    public void setEventRunning(boolean on) {
        this.eventRunning = on;
    }

    public String[] getLastEvent() {
        return this.lastEvent;
    }

    public void setLastEvent(String event, String host, String playerHost) {
        this.lastEvent[0] = event;
        this.lastEvent[1] = host;
        this.lastEvent[2] = playerHost;
    }

    public long getLastEventTime() {
        return this.lastEventTime;
    }

    public void setLastEventTime(long time) {
        this.lastEventTime = time;
    }

    public Map<Integer, HiredMerchant> getHiredMerchants() {
        return hiredMerchants;
    }

    public void addHiredMerchant(int chrid, HiredMerchant hm) {
        WriteLock wlock = merchant_lock.writeLock();
        wlock.lock();
        try {
            hiredMerchants.put(chrid, hm);
        } finally {
            wlock.unlock();
        }
    }

    public void removeHiredMerchant(int chrid) {
        WriteLock wlock = merchant_lock.writeLock();
        wlock.lock();
        try {
            hiredMerchants.remove(chrid);
        } finally {
            wlock.unlock();
        }
    }

    public int[] multiBuddyFind(int charIdFrom, int[] characterIds) {
        List<Integer> ret = new ArrayList<>(characterIds.length);
        PlayerStorage playerStorage = getPlayerStorage();
        for (int characterId : characterIds) {
            MapleCharacter chr = playerStorage.getCharacterById(characterId);
            if (chr != null) {
                if (chr.getBuddylist().containsVisible(charIdFrom)) {
                    ret.add(characterId);
                }
            }
        }
        int[] retArr = new int[ret.size()];
        int pos = 0;
        for (Integer i : ret) {
            retArr[pos++] = i.intValue();
        }
        return retArr;
    }

    public boolean hasExpedition(MapleExpeditionType type) {
        return expeditions.containsKey(type);
    }

    public void addExpedition(MapleExpeditionType type, MapleExpedition exped) {
        expeditions.put(type, exped);
    }

    public MapleExpedition getExpedition(MapleExpeditionType type) {
        return expeditions.get(type);
    }

    public boolean isConnected(String name) {
        return getPlayerStorage().getCharacterByName(name) != null;
    }

    public boolean finishedShutdown() {
        return finishedShutdown;
    }

    public void setServerMessage(String message) {
        this.serverMessage = message;
        broadcastPacket(MaplePacketCreator.serverMessage(message));
    }

    public boolean getScramble() {
        return scramble;
    }

    public boolean isScramble() {
        return scramble;
    }

    public void setScramble(boolean scramble) {
        this.scramble = scramble;
    }

    public MapleMap getEventMap() {
        return eventMap;
    }

    public void setEventMap(MapleMap eventMap) {
        this.eventMap = eventMap;
    }

    public class respawnMaps implements Runnable {

        @Override
        public void run() {
            for (Entry<Integer, MapleMap> map : mapFactory.getMaps().entrySet()) {
                map.getValue().respawn();
            }
        }
    }
}
