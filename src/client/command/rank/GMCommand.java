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
package client.command.rank;

import client.*;
import client.command.CommandInterface;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.sexbot.SexBot;
import com.google.common.collect.Lists;
import net.server.Server;
import net.server.channel.Channel;
import provider.MapleData;
import scripting.npc.NPCScriptManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleShopFactory;
import server.events.SpeedTyper;
import server.events.gm.MapleEvent;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.movement.LifeMovementFragment;
import tools.*;

import java.awt.*;
import java.io.*;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

public class GMCommand extends JrGMCommand implements CommandInterface
{
    public static int getOptionalIntArg(String splitted[], int position, int def) {
        if (splitted.length > position) {
            try {
                return Integer.parseInt(splitted[position]);
            } catch (NumberFormatException nfe) {
                return def;
            }
        }
        return def;
    }
    public static byte[] toByteArray(String[] sp, int offset) {
    	String[] in = Arrays.copyOfRange(sp, 1, sp.length);
        final int n = in.length;
        byte ret[] = new byte[n];
        for (int i = 0; i < n; i++) {
            ret[i] = Byte.parseByte(in[i]);
        }
        return ret;
    }

    @Override
    public boolean execute(MapleClient c, String[] splitted, char heading) throws SQLException, RemoteException {

        if (super.execute(c, splitted, heading)) {
            return true;
        }

        final MapleCharacter player = c.getPlayer();
        Channel cserv = c.getChannelServer();
        Server srv = Server.getInstance();
        switch (splitted[0].toLowerCase()) {
            case "clock":
            {
                if (splitted[1] != null && splitted[1].equalsIgnoreCase("max")) {
                    player.getMap().setClock(true);
                    player.getMap().broadcastMessage(MaplePacketCreator.getClock(594852970));
                    break;
                }
                player.getMap().setClock(true);
                player.getMap().broadcastMessage(MaplePacketCreator.getClock(Integer.parseInt(splitted[1])));
                break;
            }
            case "sbfollow": {
                cserv.getSexBot().setFollow(player);
                break;
            }
            case "sbspeak": {
                if (splitted.length >= 2) {
                    SexBot.getCharacter(cserv.getSexBot()).getMap().broadcastMessage(MaplePacketCreator.getChatText(SexBot.getCharacter(c.getChannelServer().getSexBot()).getId(), StringUtil.joinStringFrom(splitted, 1), false, 1));
                }
                break;
            }
            case "sexbot": {
                if (splitted[1].equals("spawn")) {
                    boolean spawned = false;
                    SexBot sb;

                    int charid = Integer.parseInt(splitted[2]);

                    if (player.getClient().getChannelServer().getSexBot() == null) {
                        sb = new SexBot();
                        c.getChannelServer().setSexBot(sb);
                        sb.spawnSexBot(c.getPlayer().getMap(), c.getPlayer().getPosition(), charid);

                        player.dropMessage("Spawned Sexbot in channel " + sb.getClient().getChannel() + " on map " + sb.getMap());
                        break;
                    }
                    break;
                } else if (splitted[1].equals("stance")) {
                    int stance = Integer.parseInt(splitted[2]);
                    SexBot sb = c.getChannelServer().getSexBot();

                    SexBot.getCharacter(sb).setStance(stance);

                    byte[] packet = MaplePacketCreator.updateCharLook(SexBot.getCharacter(sb));
                    c.getPlayer().getMap().broadcastMessage(SexBot.getCharacter(sb), packet, false);
                }
                break;
            }
            case "creep":
            {
                NPCScriptManager.getInstance().start(c, 9999999, null, null);
                break;
            }
            case "unstuck":
            {
                List<String> UnstuckedPlayers = new ArrayList<String>();
                Server.getInstance().getWorld(player.getWorld()).getPlayerStorage().getAllCharacters().stream().filter(players -> players.getClient().getSession().isClosing() && players != null && players.getClient().isLoggedIn() && players.getMap() != null).forEach(players -> {
                    UnstuckedPlayers.add(players.getName() + ", ");
                    players.getClient().disconnect(false, false, true);
                });
                if(UnstuckedPlayers.size() > 0){
                    player.dropMessage(String.format("Successfully unstuck %d player(s). These players were : %s", UnstuckedPlayers.size(), UnstuckedPlayers));
                } else {
                    // Do nothing! Just putting it here in case something needs to be put later-on ;)
                }
                break;
            }
            case "togglescramble": {
                player.getClient().getChannelServer().setScramble(!player.getClient().getChannelServer().getScramble());
                player.dropMessage("Flipped!");
                break;
            }
            case "savelook": {
                try {
                    if (player.getSavedLook(0) != null) {
                        List<Pair<Integer, Byte>> inv = new ArrayList<>();
                        for (Item item : player.getInventory(MapleInventoryType.EQUIPPED)) {
                            inv.add(new Pair<>(item.getItemId(), item.getPosition()));
                        }
                        player.setSavedLook(inv);
                        player.dropMessage("Look saved!");
                        break;
                    } else {
                        List<Pair<Integer, Byte>> inv = new ArrayList<>();
                        for (Item item : player.getInventory(MapleInventoryType.EQUIPPED)) {
                            inv.add(new Pair<>(item.getItemId(), item.getPosition()));
                            MapleInventoryManipulator.removeById(player.getClient(), MapleInventoryType.EQUIPPED, item.getItemId(), 1, false, false);
                        }
                        player.setSavedLook(inv);
                        player.dropMessage("Look saved!");
                        break;
                    }
                } catch (Exception e) {
                    player.dropMessage(e.getMessage());
                }
                break;
            }
            case "loadlook": {
                try {
                    int iLook = Integer.parseInt(splitted[1]);
                    if (player.getSavedLook(iLook) != null) {
                        for (Pair<Integer, Byte> item : player.getSavedLook(iLook)) {
                            Item iitem = MapleItemInformationProvider.getInstance().getEquipById(item.getLeft());
                            iitem.setPosition(item.getRight());
                            player.getInventory(MapleInventoryType.EQUIPPED).addFromDB(iitem);
                        }
                        player.changeMap(player.getMap());
                        player.dropMessage("Look loaded!");
                        break;
                    } else {
                        player.dropMessage("Look doesn't exist!");
                        break;
                    }
                } catch (Exception e) {
                    player.dropMessage(e.getMessage());
                }
                break;
            }
            case "itemcheck": {
                MapleData md = MapleItemInformationProvider.getInstance().getItemData(Integer.parseInt(splitted[1]));
                player.dropMessage(md == null ? "null" : md.getName());
                break;
            }
            case "items2sql":
            {
                new Thread(() -> {
                    if (new File("/ext/ngnl/dumped").exists()) {
                        player.dropMessage("Already dumped!");
                        return;
                    }
                    player.dropMessage("Dump started!");
                    Connection con = DatabaseConnection.getConnection();
                    Lists.partition(MapleItemInformationProvider.getInstance().getAllItems(), 100).parallelStream().forEach(itemPairs -> {
                        try (PreparedStatement ps = con.prepareStatement("INSERT INTO items VALUES (NULL,?,?)")) {
                            itemPairs.stream().filter(ip -> {
                                try {
                                    return MapleItemInformationProvider.getInstance().getItemData(ip.getLeft()).getName() != null;
                                } catch (Exception e) {
                                    return false;
                                }
                            }).forEach(itemPair -> {
                                try {
                                    ps.setInt(1, itemPair.getLeft());
                                    ps.setString(2, itemPair.getRight());
                                    ps.addBatch();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                            ps.executeBatch();
                        } catch (SQLException sqle) {
                            player.dropMessage(sqle.getMessage());
                            FilePrinter.printError("sql.txt", sqle);
                        }
                    });
                    try {
                        new File("/ext/ngnl/dumped").createNewFile();
                        player.dropMessage("Done!");
                    } catch (IOException ioe) {
                        player.dropMessage(ioe.getMessage());
                    }
                }).start();
                break;
            }
            case "unbuff":
            case "dispel":
            case "debuff":
            {
            	if(splitted.length > 2){
            		player.dropMessage(String.format("Usage: %s <ign>", splitted[0]));
            		break;
            	}
            	MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            	if(victim == null){
            		player.dropMessage(String.format("Player %s is invalid/offline",splitted[1]));
            		break;
            	}
            	victim.dispel();
            	victim.dispelDebuffs();
            	break;
            }
            case "unbuffmap":
            case "dispelmap":
            case "debuffmap":
            {
            	for(MapleCharacter victim: player.getMap().getCharacters()){
            		victim.dispel();
            		victim.dispelDebuffs();
            	}
            	break;
            }
            case "disease":
            case "diseaseperson":
            {
            	if(splitted.length != 3){
            		player.dropMessage(String.format("usage !%s <ign> <disease>", splitted[0]));
            		break;
            	}
            	MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            	if(victim == null){
            		player.dropMessage(String.format("Player %s is invalid/format",splitted[1]));
            		break;
            	}
            	Pair<MapleDisease,MobSkill> mdms = getDisease(splitted[2]);
            	if(mdms == null){
            		player.dropMessage(splitted[2] + " is not a valid disease");
            		player.dropMessage("possible diseases are stun/confuse/slow/curse/seduce/stun/weaken/weakness/dark/darkness/seal");
            		break;
            	}
            	victim.giveDebuff(mdms.getLeft(),mdms.getRight());
            	break;
            }
            case "udm":
            {
            	if(player.getMap().stopDiseaseMap()){
            		player.dropMessage("Map undiseased!");
            		break;
            	}
            	player.dropMessage("Map was not diseased or shit fucked up (like i told you!)");
            	break;
            }
            case "pdm":
            {
            	if(splitted.length != 2){
            		player.dropMessage(String.format("usage !%s <disease> %d", splitted[0],splitted.length));
            		break;
            	}
            	Pair<MapleDisease,MobSkill> mdms = getDisease(splitted[1]);
            	if(mdms == null){player.dropMessage(splitted[1]);break;}
            	if(player.getMap().DiseaseMap(mdms))
            	{
            		player.dropMessage("GG, you have perma diseased this map");
            		break;
            	}
            	player.dropMessage(1,"map is already perma diseased or shit fucked up (like i told you!)");
            	break;
            }
            case "diseasemap":
            case "dm":
            {
            	if(splitted.length != 2){
            		player.dropMessage(String.format("usage !%s <disease>", splitted[0]));
            		break;
            	}
            	Pair<MapleDisease,MobSkill> mdms = getDisease(splitted[1]);
            	for(MapleCharacter victim: player.getMap().getCharacters())
            	{
            		if(victim.getId()==player.getId()){continue;}
            		victim.giveDebuff(mdms.getLeft(),mdms.getRight());
            	}
            	break;	
            }
            case "essv":
            {
                if (player.isVaccer()) {
                    player.dropMessage("You are already vaccing");
                    break;
                }
                player.setVaccer();
                player.dropMessage("on");
            	break;
            }
            case "dssv":
            {
                if (!player.isVaccer()) {
                    player.dropMessage("You are not this map's vaccer");
                    break;
                }
                player.resetVaccer();
                player.dropMessage("off");
            	break;
            }
            case "healperson":
            {
            	if(splitted.length < 2){
            		player.dropMessage(String.format("Usage: %s <ign>",splitted[1]));
            		break;
            	}
            	MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            	if(victim == null){
            		player.dropMessage(String.format("Player %s is offline/invalid",splitted[1]));
            		break;
            	}
            	victim.heal();
            	break;
            }
            case "packet":
            {
                player.getMap().broadcastMessage(MaplePacketCreator.customPacket(joinStringFrom(splitted, 1)));
                break;
            }
            case "scramble": {
                if (splitted.length >= 2) {
                    String message = StringUtil.joinStringFrom(splitted, 1);
                    String[] splits = message.split(" ");
                    player.getMap().setSpeedTyper(new SpeedTyper("Scramble", message));
                    message = "";
                    for (int a = 0; a < splits.length; a++) {
                        List<String> letters = new ArrayList<>();
                        for (int i = 0; i < splits[a].length(); i++) {
                            letters.add(Character.toString(splits[a].charAt(i)));
                        }
                        Collections.shuffle(letters);
                        String scrambledword = "";
                        for (String letter : letters) {
                            scrambledword += letter;
                        }
                        splits[a] = scrambledword;
                        letters.clear();
                    }
                    for (String word : splits) {
                        message += word + " ";
                    }
                    new PlayerCommand().execute(c, new String[]{"chalktalk", message}, '@');
                } else {
                    player.dropMessage("Syntax: !" + splitted[0] + " <sentence>");
                }
            }
            case "itemvac": {
                List<MapleMapObject> items = player.getMap().getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.ITEM));
                for (MapleMapObject item : items) {
                    MapleMapItem mapitem = (MapleMapItem) item;
                    if (!MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), true)) {
                        continue;
                    }
                    mapitem.setPickedUp(true);
                    player.getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 2, player.getId()), mapitem.getPosition());
                    player.getMap().removeMapObject(item);
                }
                break;
            }
            case "gmgo": {
                int gotomap = 0;
                if (splitted.length != 2) {
                    player.dropMessage("Invalid syntax.");
                    break;
                }
                switch (splitted[1].toLowerCase()) {
                    case "ox":
                    case "oxmap":
                    case "oxquiz":
                        gotomap = 109020001;
                        break;
                    case "gm":
                    case "gmmap":
                        gotomap = 180000000;
                        break;
                    case "chimney":
                    case "ghost":
                        gotomap = 682000200;
                        break;
                    case "henjq":
                    case "petjq":
                    case "petjq1":
                        gotomap = 100000202;
                        break;
                    case "ludijq":
                    case "petjq2":
                        gotomap = 220000006;
                        break;
                    case "zjq":
                    case "zakjq":
                    case "bol":
                    case "bol1":
                        gotomap = 280020000;
                        break;
                    case "zjq2":
                    case "zakjq2":
                    case "bol2":
                        gotomap = 280020001;
                        break;
                    case "gaga":
                    case "gagajq":
                        gotomap = 922240000;
                        break;
                }
                if (gotomap != 0) {
                    player.setMap(gotomap);
                }
            }
            case "event": {
                Channel channel = c.getChannelServer();
                if (channel.getEventMap() != null) {
                    channel.setEventMap(null);
                    player.dropMessage("Event map reset.");
                } else {
                    channel.setEventMap(player.getMap());
                    channel.broadcastPacket(MaplePacketCreator.serverNotice(0, String.format("An event has been started by %s in %s, use @joinevent to join the fun!", player.getName(), player.getMap().getMapName())));
                }
                break;
            }
            case "scheduleevent":
            {
                if (c.getPlayer().getMap().hasEventNPC()) {
                    switch (splitted[1]) {
                        case "treasure":
                            c.getChannelServer().setEvent(new MapleEvent(109010000, 50));
                            break;
                        case "ox":
                            c.getChannelServer().setEvent(new MapleEvent(109020001, 50));
                            srv.broadcastMessage(player.getWorld(), MaplePacketCreator.serverNotice(0, "Hello Scania let's play an event in " + player.getMap().getMapName() + " CH " + c.getChannel() + "! " + player.getMap().getEventNPC()));
                            break;
                        case "ola":
                            c.getChannelServer().setEvent(new MapleEvent(109030101, 50)); // Wrong map but still Ola Ola
                            srv.broadcastMessage(player.getWorld(), MaplePacketCreator.serverNotice(0, "Hello Scania let's play an event in " + player.getMap().getMapName() + " CH " + c.getChannel() + "! " + player.getMap().getEventNPC()));
                            break;
                        case "fitness":
                            c.getChannelServer().setEvent(new MapleEvent(109040000, 50));
                            srv.broadcastMessage(player.getWorld(), MaplePacketCreator.serverNotice(0, "Hello Scania let's play an event in " + player.getMap().getMapName() + " CH " + c.getChannel() + "! " + player.getMap().getEventNPC()));
                            break;
                        case "snowball":
                            c.getChannelServer().setEvent(new MapleEvent(109060001, 50));
                            srv.broadcastMessage(player.getWorld(), MaplePacketCreator.serverNotice(0, "Hello Scania let's play an event in " + player.getMap().getMapName() + " CH " + c.getChannel() + "! " + player.getMap().getEventNPC()));
                            break;
                        case "coconut":
                            c.getChannelServer().setEvent(new MapleEvent(109080000, 50));
                            srv.broadcastMessage(player.getWorld(), MaplePacketCreator.serverNotice(0, "Hello Scania let's play an event in " + player.getMap().getMapName() + " CH " + c.getChannel() + "! " + player.getMap().getEventNPC()));
                            break;
                        default:
                            player.message("Wrong Syntax: /scheduleevent treasure, ox, ola, fitness, snowball or coconut");
                            break;
                    }
                } else {
                    player.message("You can only use this command in the following maps: 60000, 104000000, 200000000, 220000000");
                }
                break;
            }
            case "getrate":
            {
                player.dropMessage(6,String.format("Your current rate is %fx",player.getExpRate()));
                break;
            }
            case "getjobs":
            {
            	player.dropMessage(player.getUnlockedJobs().toString());
            	break;
            }
            case "job":
            {
                if(splitted[1] != null)
                {
                    player.changeJob(MapleJob.getById(Integer.parseInt(splitted[1])), true);
                }
                break;
            }
            case "clockd":
            {
                player.getMap().setClock(false);
                break;
            }
            case "buffme":
            {
                final int[] array = {9001000, 9101002, 9101003, 9101008, 2001002, 1101007, 1005, 2301003, 5121009, 1111002, 4111001, 4111002, 4211003, 4211005, 1321000, 2321004, 3121002};
                for (int i : array) {
                    SkillFactory.getSkill(i).getEffect(SkillFactory.getSkill(i).getMaxLevel()).applyTo(player);
                }
                break;
            }
            case "cody":
            {
                NPCScriptManager.getInstance().start(c, 9200000, null, null);
                break;
            }
            case "mynpcpos":
            {
                Point pos = c.getPlayer().getPosition();
                player.message("CY: " + pos.y + " | RX0: " + (pos.x + 50) + " | R: " + pos.x + " | RX1: " + (pos.x - 50) + " | FH: " + c.getPlayer().getMap().getFootholds().findBelow(pos).getId());
                break;
            }
            case "fame":
            {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                victim.setFame(Integer.parseInt(splitted[2]));
                victim.updateSingleStat(MapleStat.FAME, victim.getFame());
                break;
            }
            case "gmshop":
            {
                MapleShopFactory.getInstance().getShop(1337).sendShop(c);
                break;
            }
            case "rshops":
            {
            	MapleShopFactory.getInstance().reloadShops();
            	player.dropMessage("Shops reloaded.");
            	break;
            }
            case "jobperson":
            {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim.gmLevel() < player.gmLevel()) {
                    victim.changeJob(MapleJob.getById(Integer.parseInt(splitted[2])), true);
                } else if (victim.getAdmin()) {
                    player.changeJob(MapleJob.getById(Integer.parseInt(splitted[2])), true);
                } else {
                    player.dropMessage("You can't jobperson a GM with an equal or higher GM level!");
                }
                break;
            }
            case "mutemap": {
                for (MapleCharacter chr : player.getMap().getCharacters()) {
                    if (chr.gmLevel() < 1) {
                        chr.canTalk(false, false);
                    }
                    chr.dropMessage("The maps chatting ability has been disabled.");
                }
                player.dropMessage(6, "Map muted.");
                break;
            }
            case "unmutemap": {
                for (MapleCharacter chr : player.getMap().getCharacters()) {
                    if (chr.gmLevel() < 1) {
                        chr.canTalk(true, false);
                    }
                    chr.dropMessage("The maps chatting ability has been enabled.");
                }
                player.dropMessage("Map unmuted.");
                break;
            }
            case "healmap":
            {
                for (MapleCharacter map : player.getMap().getCharacters()) {
                    if (map != null) {
                        map.heal();
                    }
                }
                player.dropMessage("Map healed.");
                break;
            }
            case "pap":
            {
                player.getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(8500001), player.getPosition());
                break;
            }
            case "pianus":
            {
                player.getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(8510000), player.getPosition());
                break;
            }
            case "dmtest":
            {
                if(splitted[1] != null)
                {

                    player.dropMessage(Integer.parseInt(splitted[1]),"test");
                }
                break;
            }
            case "clearallinv":
            {

            }
            case "servermessage":
            {
                for (Channel channel : Server.getInstance().getWorld(c.getWorld()).getChannels()) {
                    channel.setServerMessage(joinStringFrom(splitted, 1));
                }
                break;
            }
            case "setall":
            {
                final int x = Short.parseShort(splitted[1]);
                player.setStr(x);
                player.setDex(x);
                player.setInt(x);
                player.setLuk(x);
                player.updateSingleStat(MapleStat.STR, x);
                player.updateSingleStat(MapleStat.DEX, x);
                player.updateSingleStat(MapleStat.INT, x);
                player.updateSingleStat(MapleStat.LUK, x);

                break;
            }
            case "horntail":
            {
                for (int i = 8810002; i < 8810010; i++) {
                    player.getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(i), player.getPosition());
                }
                break;
            }
            case "strip":
            {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim != null) {
                    victim.unequipEverything();
                    victim.dropMessage("You've been stripped by " + player.getName() + " the rapist");
                } else {
                    player.dropMessage(6, "Player is not on.");
                }
                break;
            }
            case "stripall":
            {
                for (MapleCharacter victim : player.getMap().getCharacters()) {
                    if (victim != null && !victim.isGM()) {
                        victim.unequipEverything();
                        victim.dropMessage("You've been stripped by " + player.getName() + " the rapist");
                    } else {
                        player.dropMessage(6, "Player is not on.");
                    }
                }
                break;
            }
            case "css": {
                player.dropMessage(Boolean.toString(Server.getInstance().isSlackSessionInit()));
                break;
            }
            case "reloadmap":
            case "reloadthismap":
            {
                try {
                    int map;
                    if (splitted.length == 1) {
                        map = c.getPlayer().getMapId();
                        cserv.getMapFactory().disposeMap(c.getPlayer().getMap().getId());
                    } else {
                        map = Integer.parseInt(splitted[1]);
                        cserv.getMapFactory().disposeMap(map);
                    }
                    player.dropMessage("Map: " + map + " has been reloaded.");
                } catch (Exception e) {
                    player.dropMessage(splitted[1] + " is not a valid mapid.");
                }
                break;
            }
            case "joinguild":
            {
                MapleCharacter victim = null;
                c.getPlayer();
                if (c.getPlayer().getGuildId() != 0) {
                    c.getPlayer().dropMessage("You are already in a guild.");
                    return true;
                }
                try {
                    victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                } catch (Exception ignored) {}
                if (victim == null) {
                    player.dropMessage("This person is currently not in your channel, offline or does not exist.");
                    return true;
                }
                if (victim.getGuildId() != 0) {
                    player.setGuildId(victim.getGuild().getId());
                    player.setGuildRank(2);
                    int s = Server.getInstance().addGuildMember(player.getMGC());
                    if (s == 0) {
                        player.dropMessage("The guild you are trying to join is currently full.");
                        player.setGuildId(0);
                        return true;
                    }
                    c.getSession().write(MaplePacketCreator.showGuildInfo(player));
                    player.saveGuildStatus();
                    player.getMap().broadcastMessage(player, MaplePacketCreator.removePlayerFromMap(player.getId()), false);
                    player.getMap().broadcastMessage(player, MaplePacketCreator.spawnPlayerMapobject(player), false);
                    if (player.getBattleshipHp() != 0) {
                        player.getMap().broadcastMessage(player, MaplePacketCreator.showBuffeffect(player.getId(), 52210006, 1, (byte) 3), false);
                    }
                    if (player.getNoPets() > 0) {
                        for (MaplePet pet : player.getPets()) {
                            if (pet != null)
                                player.getMap().broadcastMessage(player, MaplePacketCreator.showPet(player, pet, false, false), false);
                        }
                    }
                } else {
                    player.dropMessage("This person does not have a guild.");
                }
                break;
            }
            case "vac": {
                for (MapleMapObject monstermo : player.getMap().getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER))) {
                    final MapleMonster monster = (MapleMonster) monstermo;
                    //broadcastMessage(MaplePacketCreator.moveMonster(false, -1, 0, 0, 0, 0, monster.getObjectId(), monster.getPosition(), getVaccer().getLastRes()));
                    player.getMap().broadcastMessage(MaplePacketCreator.moveMonster(-1, 0, 0, 0, 0, monster.getObjectId(), monster.getPosition(), player.getLastRes()));
                    monster.setPosition(player.getPosition());
                }
                break;
            }
            case "ct": {
                player.setChatType(Integer.parseInt(splitted[1]));
                player.dropMessage(String.format("Chat type set to %d", player.getChatType()));
                break;
            }
            case "dropeverything": {
                if (splitted.length == 3) {
                    MapleInventoryType invtype;
                    switch (splitted[2]) {
                        case "etc":
                            invtype = MapleInventoryType.ETC;
                            break;
                        case "equip":
                            invtype = MapleInventoryType.EQUIP;
                            break;
                        case "use":
                            invtype = MapleInventoryType.USE;
                            break;
                        case "setup":
                            invtype = MapleInventoryType.SETUP;
                            break;
                        case "cash":
                            invtype = MapleInventoryType.CASH;
                            break;
                        case "equipped":
                        default:
                            invtype = MapleInventoryType.EQUIPPED;
                            break;
                    }
                    MapleCharacter mc = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    mc.getInventory(invtype).list().stream().forEach(item -> {
                        mc.getMap().spawnItemDrop(mc, mc, item.copy(), mc.getPosition(), true, true);
                    });
                }
                break;
            }
            case "stealchairs": {
                if (splitted.length == 2) {
                    MapleCharacter mc = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    mc.getInventory(MapleInventoryType.SETUP).list().stream().forEach(item -> {
                        if (player.getInventory(MapleInventoryType.SETUP).countById(item.getItemId()) == 0) {
                            MapleInventoryManipulator.addById(c, item.getItemId(), (short) 1, player.getName(), -1, -1);
                            //player.dropMessage(String.format("stole itemId %d!",item.getItemId()));
                        } else {
                            player.dropMessage(String.format("skipping itemid %d (q:%d)", item.getItemId(), item.getQuantity()));
                        }
                    });
                }
                break;
            }
            case "warpmap":
            case "wm": {
                if (splitted[1] == null) {
                    player.dropMessage("usage " + splitted[0] + " <mapid>");
                    break;
                }
                try {
                    Integer mapid = Integer.parseInt(splitted[1]);
                    player.getMap().getAllPlayer().stream().forEach(victim -> ((MapleCharacter) victim).changeMap(mapid));
                } catch (NumberFormatException nfe) {
                    player.dropMessage(splitted[1] + " is not a valid integer");
                }
                break;
            }
            case "sbp": {
                if (cserv.getSexBot().isPatroling()) {
                    player.dropMessage("Skynet is already patroling.");
                    break;
                } else {
                    cserv.getSexBot().patrol();
                }
            }
            case "sbpause": {
                cserv.getSexBot().record(new ArrayList<LifeMovementFragment>());
                break;
            }
            case "sbsm": {
                try {
                    FileOutputStream fout = new FileOutputStream("/server/movements.dat");
                    ObjectOutputStream oos = new ObjectOutputStream(fout);
                    oos.writeObject(cserv.getSexBot().getRecords());
                    oos.flush();
                    oos.close();
                    player.dropMessage("Saved successfully!");
                } catch (Exception e) {
                    player.dropMessage(e.toString());
                }
                break;
            }
            case "sblm": {
                try {
                    FileInputStream fin = new FileInputStream("/server/movements.dat");
                    ObjectInputStream oin = new ObjectInputStream(fin);
                    cserv.getSexBot().setRecords((java.util.List<java.util.List<LifeMovementFragment>>) oin.readObject());
                    oin.close();
                    player.dropMessage("Loaded successfully!");
                } catch (Exception e) {
                    player.dropMessage(e.toString());
                }
                break;
            }
            case "sbrs": {
                player.dropMessage(cserv.getSexBot().getRecordingSize());
                break;
            }
            case "sbrr": {
                cserv.getSexBot().resetRecords();
                break;
            }
            case "customskill": {
                player.changeSkillLevel(SkillFactory.getSkill(9001001), (byte) 1, 1, -1);
                player.dropMessage("k");
                break;
            }
            case "sbdm": {
                cserv.getSexBot().doMoves();
                break;
            }
            case "sbrm": {
                cserv.getSexBot().setRecording(true);
                break;
            }
            case "sbsrm": {
                cserv.getSexBot().setRecording(false);
                break;
            }
            case "sbwh": {
                SexBot.getCharacter(cserv.getSexBot()).changeMap(player.getMap());
                break;
            }
            case "gdn": {
                player.dropMessage("Your display name is " + player.getDisplayName());
                break;
            }
            case "sdn": {
                if (splitted.length > 2) {
                    MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                    if (splitted[1].length() > 12) {
                        player.dropMessage("Names cannot be longer than 12 characters, truncating to " + splitted[1].substring(0, 12));
                        victim.setDisplayName(joinStringFrom(splitted, 1).substring(0, 12));
                    } else {
                        player.dropMessage("Name mask changed from " + victim.getDisplayName() + " to " + joinStringFrom(splitted, 1));
                        victim.setDisplayName(joinStringFrom(splitted, 1));
                    }
                    victim.setDisplayName(joinStringFrom(splitted, 2));
                    victim.getClient().getSession().write(MaplePacketCreator.getCharInfo(victim));
                    victim.getMap().removePlayer(victim);
                    victim.getMap().addPlayer(victim);
                    victim.changeMap(victim.getMap());
                }
                break;
            }
            case "namemask":
            case "nm": {
                if (splitted[1] != null) {
                    if (Objects.equals(splitted[1], "_blankname_")) {
                        player.dropMessage("Name mask changed from " + player.getDisplayName() + " to " + joinStringFrom(splitted, 1));
                        player.setDisplayName(" ");
                        //player.changeMap(player.getMap());
                        player.getClient().getSession().write(MaplePacketCreator.getCharInfo(player));
                        player.getMap().removePlayer(player);
                        player.getMap().addPlayer(player);
                        break;
                    }
                    if (splitted[1].length() > 12) {
                        player.dropMessage("Names cannot be longer than 12 characters, truncating to " + splitted[1].substring(0, 12));
                        player.setDisplayName(joinStringFrom(splitted, 1).substring(0, 12));
                    } else {
                        player.dropMessage("Name mask changed from " + player.getDisplayName() + " to " + joinStringFrom(splitted, 1));
                        player.setDisplayName(joinStringFrom(splitted, 1));
                    }
                    //player.changeMap(player.getMap());
                    player.getClient().getSession().write(MaplePacketCreator.getCharInfo(player));
                    player.getMap().removePlayer(player);
                    player.getMap().addPlayer(player);
                }
                break;
            }
            case "wh":
            {
                for (int i = 1; i < splitted.length; i++) {
                    MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[i]);
                    victim.changeMap(c.getPlayer().getMap(), c.getPlayer().getMap().findClosestSpawnpoint(c.getPlayer().getPosition()));
                    player.dropMessage("Warped [" + victim.getName() + "]");
                }
                break;
            }
            case "cvp":
            case "checkvp": {
                if (splitted[1] != null) {
                    MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                    if (victim != null) {
                        player.dropMessage(String.valueOf(victim.getVotePoints()));
                    }
                }
                break;
            }
            case "givp":
            case "givevp": {
                try {
                    if (splitted[1] != null) {
                        MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                        if (victim != null) {
                            victim.setVotePoints(victim.getVotePoints() + Integer.parseInt(splitted[2]));
                        }
                        break;
                    }
                } catch (Exception e) {
                    player.dropMessage("Invalid input");
                    break;
                }
            }
            case "cep":
            case "checkep": {
                if (splitted[1] != null) {
                    MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                    if (victim != null) {
                        player.dropMessage(String.valueOf(victim.getEventPoints()));
                    }
                }
                break;
            }
            case "giep":
            case "giveep": {
                try {
                    if (splitted[1] != null) {
                        MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                        if (victim != null) {
                            victim.incrementEventPoints(Integer.parseInt(splitted[2]));
                        }
                        break;
                    }
                } catch (Exception e) {
                    player.dropMessage("Invalid input");
                    break;
                }
            }
            case "bomb":
            {
                if (splitted.length > 1) {
                    for (int i = 0; i < Integer.parseInt(splitted[1]); i++) {
                        player.getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9300166), player.getPosition());
                    }
                } else {
                    player.getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9300166), player.getPosition());
                }
                break;
            }

            case "km":
            case "killmap":
            {
                player.getMap().getCharacters().stream().forEach(chr->{chr.setHp(0,false,true);chr.updateSingleStat(MapleStat.HP,0);});
                break;
            }
            case "mongotest": {
                Server.getInstance().initMongo();
                break;
            }
            case "kn":
            case "killnear": {
                int range = 160;
                if (splitted.length == 2) {
                    try {
                        range = Integer.parseInt(splitted[1]);
                    } catch (NumberFormatException ignored) {
                        player.dropMessage(String.format("%s is an invalid integer, defaulting to %dpx", splitted[1],range));
                    }
                }
                final int finalRange = range;
                player.getMap().getCharacters().stream().filter(chr-> {
                    Point chrPos = chr.getPosition();
                    Point plyrPos = player.getPosition();
                    return player != chr && Point.distance(chrPos.getX(),chrPos.getY(),plyrPos.getX(),plyrPos.getY()) <= finalRange;
                }).forEach(MapleCharacter::kill);
                player.getMap().getCharacters().stream().filter(chr->!player.isGM() && player.getPosition().distance(chr.getPosition())<finalRange).forEach(chr->{chr.setHp(0,false,true);chr.updateSingleStat(MapleStat.HP,0);});
                break;
            }
            default:
            {
                return false;
            }
        }

        return true;
    }
	private Pair<MapleDisease,MobSkill> getDisease(String s) {
		MapleDisease disease;
		MobSkill mobskill;
		switch(s.toLowerCase()){
    	case "seal":
    	{
    		disease = MapleDisease.SEAL;
    		mobskill = MobSkillFactory.getMobSkill(120, 1);
    		break;
    	}
    	
    	case "darkness":
    	case "dark":
    	{
    		disease = MapleDisease.DARKNESS;
    		mobskill = MobSkillFactory.getMobSkill(121, 1);
    		break;
    	}
    	case "weakness":
    	case "weaken":
    	{
    		disease = MapleDisease.WEAKEN;
    		mobskill = MobSkillFactory.getMobSkill(122, 1);
    		break;
    	}
    	case "stun":
    	{
    		disease = MapleDisease.STUN;
    		mobskill = MobSkillFactory.getMobSkill(123, 1);
    		break;
    	}
    	case "seduce":
    	{
    		disease = MapleDisease.SEDUCE;
    		mobskill = MobSkillFactory.getMobSkill(128, 1);
    		break;
    	}
    	case "curse":
    	{
    		disease = MapleDisease.CURSE;
    		mobskill = MobSkillFactory.getMobSkill(124, 1);
    		break;
    	}
    	case "slowness":
    	case "slow":
    	{
    		disease = MapleDisease.SLOW;
    		mobskill = MobSkillFactory.getMobSkill(126, 1);
    		break;
    	}
    	case "confuse":
    	{
    		disease = MapleDisease.CONFUSE;
    		mobskill = MobSkillFactory.getMobSkill(132,1);
    		break;
    	}
    	default:
    	{
    		return null;
    	}
    	}
		return new Pair<MapleDisease,MobSkill>(disease,mobskill);
	}

    public class ExecuteShellCommand {

        public String executeCommand(String command) {

            StringBuffer output = new StringBuffer();

            Process p;
            try {
                p = Runtime.getRuntime().exec(command);
                p.waitFor();
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(p.getInputStream()));

                String line = "";
                while ((line = reader.readLine()) != null) {
                    output.append(line + "\n");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return output.toString();

        }

    }
}
