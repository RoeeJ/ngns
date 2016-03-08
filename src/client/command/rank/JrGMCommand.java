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
import constants.ExpTable;
import constants.ItemConstants;
import net.server.Server;
import net.server.channel.Channel;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleShopFactory;
import server.MapleTrade;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleMonsterStats;
import server.maps.HiredMerchant;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import tools.*;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class JrGMCommand extends GuardCommand implements CommandInterface
{
    @Override
    public boolean execute(MapleClient c, String[] splitted, char heading) throws SQLException, RemoteException {
        
        if (super.execute(c, splitted, heading)) {
            return true;
        }
        
        MapleCharacter player = c.getPlayer();
        Channel cserv = c.getChannelServer();
        
        switch (splitted[0].toLowerCase()) {
            case "ap":
            {
                if (splitted.length == 2) {
                    player.setRemainingAp(Integer.parseInt(splitted[1]));
                    player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
                } else {
                    player.dropMessage("Please use the following syntax: !ap [amount]");
                }
                break;
            }
            case "chattype":
            {
                player.toggleWhiteChat();
                player.message("You now chat in " + (player.getWhiteChat() ? "white" : "black") + ".");
                break;
            }
            case "cleardrops":
            {
                player.getMap().clearDrops(player);
                break;
            }
            case "heal":
            {
                player.heal();
                break;
            }
            case "sp":
            {
                if (splitted.length == 2) {
                    player.setRemainingSp(Integer.parseInt(splitted[1]));
                    player.updateSingleStat(MapleStat.AVAILABLESP, player.getRemainingSp());
                } else {
                    player.dropMessage("Please use the following syntax: !sp [amount]");
                }
                break;
            }
            case "shop":
            {
                if (splitted.length == 2) {
                    MapleShopFactory.getInstance().getShop(Integer.parseInt(splitted[1])).sendShop(c);
                } else {
                    player.dropMessage("Please use the following syntax: !shop [shopid]");
                }
                break;
            }
            case "whereami":
            {
                player.dropMessage("You are currently at: '" + player.getMap().getId() + "'");
                break;
            }
            case "killall":
            {
                //List<MapleMapObject> monsters = player.getMap().getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER));
                player.getMap().killAllMonsters();
                /*for (MapleMapObject monstermo : monsters) {
                    MapleMonster monster = (MapleMonster) monstermo;
                    map.killMonster(monster, player, true);
                    monster.giveExpToCharacter(player, monster.getExp() * c.getPlayer().getExpRate(), true, 1);
                }
                player.dropMessage("Killed " + monsters.size() + " monsters.");
                */
                break;
            }
            case "level":
            {
                player.setLevel(Integer.parseInt(splitted[1]));
                player.gainExp(-player.getExp(), false, false);
                player.updateSingleStat(MapleStat.LEVEL, player.getLevel());
                player.setExp(0);
                player.updateSingleStat(MapleStat.EXP, 0);
                break;
            }
            case "notice":
            {
                Server.getInstance().broadcastMessage(player.getWorld(), MaplePacketCreator.serverNotice(6, "[Notice] " + joinStringFrom(splitted, 1)));
                break;
            }
            case "hitman":
                  if (splitted.length >= 2) {
                    List<Integer> charids = new ArrayList<>();
                    int amt = 0;
                    try {
                        amt = Integer.parseInt(splitted[1]);
                    } catch (NumberFormatException e) {
                        String message = StringUtil.joinStringFrom(splitted, 1);
                        player.getMap().setHitman(message);
                        player.getMap().broadcastMessage(MaplePacketCreator.serverNotice(6, "[Hitman] " + message));
                        return true;
                    }
                    if (amt == 0) {
                        player.dropMessage("You need to specify the amount of names you want to generate");
                        return false;
                    }
                    amt = amt > 30 ? 30 : amt;
                    for (MapleCharacter players : c.getWorldServer().getPlayerStorage().getAllCharacters()) {
                        charids.add(players.getId());
                    }
                    Collections.shuffle(charids);
                    StringBuilder fakemessage = new StringBuilder();
                    StringBuilder realmessage = new StringBuilder();
                    for (int i = 1; i <= amt; i++) {
                        if (i > charids.size()) {
                            Collections.shuffle(charids);
                            amt -= i - 1;
                            i = 0;
                        } else {
                            MapleCharacter rplayer = c.getWorldServer().getPlayerStorage().getCharacterById(charids.get(i - 1));
                            if (rplayer != null) {
                                realmessage.append(rplayer.getName()).append(i < amt ? " " : "");
                                fakemessage.append(rplayer.getName()).append(i < amt ? ", " : "");
                            }
                        }
                    }
                    player.getMap().setHitman(realmessage.toString());
                    player.getMap().broadcastMessage(MaplePacketCreator.sendYellowTip("[Hitman] " + fakemessage.toString()));
                    charids.clear();
                } else {
                    player.dropMessage("Syntax: !" + splitted[0] + " <amount/message>");
                }
                
                break;
            case"say":
            {
                String prefix = "[" + c.getPlayer().getName() + "] ";
                String message = prefix + joinStringFrom(splitted, 1);
                c.getChannelServer().broadcastPacket(MaplePacketCreator.serverNotice(6, message));
                break;
            }
            case "whosthere":
            {
                //	MessageCallback callback = new ServernoticeMapleClientMessageCallback(c);
                StringBuilder builder = new StringBuilder("Players on Map: ");
                for (MapleCharacter chr : c.getPlayer().getMap().getCharacters()) {
                    if (builder.length() > 150) { // wild guess :o
                        builder.setLength(builder.length() - 2);
                        player.dropMessage(builder.toString());
                        builder = new StringBuilder();
                    }
                    builder.append(MapleCharacter.makeMapleReadable(chr.getName()));
                    builder.append(", ");
                }
                builder.setLength(builder.length() - 2);
                player.dropMessage(builder.toString());
                c.getSession().write(MaplePacketCreator.serverNotice(6, builder.toString()));
                break;
            }
            case "search":
            {
                StringBuilder sb = new StringBuilder();
                if (splitted.length > 2) {
                    String search = joinStringFrom(splitted, 2);
                    long start = System.currentTimeMillis();//for the lulz
                    MapleData data = null;
                    MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File("wz/String.wz"));
                    if (!splitted[1].equalsIgnoreCase("ITEM")) {
                        if (splitted[1].equalsIgnoreCase("NPC")) {
                            data = dataProvider.getData("Npc.img");
                        } else if (splitted[1].equalsIgnoreCase("MOB") || splitted[1].equalsIgnoreCase("MONSTER")) {
                            data = dataProvider.getData("Mob.img");
                        } else if (splitted[1].equalsIgnoreCase("SKILL")) {
                            data = dataProvider.getData("Skill.img");
                        } else if (splitted[1].equalsIgnoreCase("MAP")) {
                            sb.append("#bUse the '/m' command to find a map. If it finds a map with the same name, it will warp you to it.");
                        } else {
                            sb.append("#bInvalid search.\r\nSyntax: '/search [type] [name]', where [type] is NPC, ITEM, MOB, or SKILL.");
                        }
                        if (data != null) {
                            String name;
                            for (MapleData searchData : data.getChildren()) {
                                name = MapleDataTool.getString(searchData.getChildByPath("name"), "NO-NAME");
                                if (name.toLowerCase().contains(search.toLowerCase())) {
                                    sb.append("#b").append(Integer.parseInt(searchData.getName())).append("#k - #r").append(name).append("\r\n");
                                }
                            }
                        }
                    } else {
                        for (Pair<Integer, String> itemPair : MapleItemInformationProvider.getItemPairByNameLike(search)) {
                            if (sb.length() < 32654) {//ohlol
                                if (itemPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                                    //if(MapleItemInformationProvider.getInstance().getAllItems())
                                    //#v").append(id).append("# #k-
                                    sb.append("\r\n ").append(String.format("#b%d #k - #r%s #v%d", itemPair.getLeft(), itemPair.getRight(), itemPair.getLeft())).append("\r\n");
                                }
                            } else {
                                sb.append("#bCouldn't load all items, there are too many results.\r\n");
                                break;
                            }
                        }
                    }
                    if (sb.length() == 0) {
                        sb.append("#bNo ").append(splitted[1].toLowerCase()).append("s found.\r\n");
                    }

                    sb.append("\r\n#kLoaded within ").append((double) (System.currentTimeMillis() - start) / 1000).append(" seconds.");//because I can, and it's free

                } else {
                    sb.append("#bInvalid search.\r\nSyntax: '/search [type] [name]', where [type] is NPC, ITEM, MOB, or SKILL.");
                }
                c.announce(MaplePacketCreator.getNPCTalk(9010000, (byte) 0, sb.toString(), "00 00", (byte) 0));
                break;
            }
            case "testshit": {
                try {
                    MapleCharacter mc;
                    if (splitted.length >= 2) {
                        mc = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    } else {
                        mc = player;
                    }
                    //byte[] packet = MaplePacketCreator.openCashShop(mc.getClient(), false);
                    //byte[] packet = MaplePacketCreator.marriageMessage(0,"Your mother");
                    //byte[] packet = MaplePacketCreator.getSeniorMessage("Your father");
                    //byte[] packet = MaplePacketCreator.openUI((byte)0x0A);
                    byte[] packet = MaplePacketCreator.disableUI(Boolean.parseBoolean(splitted[2]));
                    byte[] packet1 = MaplePacketCreator.lockUI(Boolean.parseBoolean(splitted[2]));
                    //byte[] packet = MaplePacketCreator.leftKnockBack();
                    //byte[] packet = MaplePacketCreator.spawnGuide(true);
                    //byte[] packet = MaplePacketCreator.showCashShopMessage((byte)0xA6);
                    mc.getClient().announce(packet);
                    mc.getClient().announce(packet1);
                } catch (Exception e) {
                }
                break;
            }
            case "unban":
            {
                try {
                    try (PreparedStatement p = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET banned = -1 WHERE id = " + MapleCharacter.getIdByName(splitted[1]))) {
                        p.executeUpdate();
                    }
                } catch (Exception e) {
                    player.message("Failed to unban " + splitted[1]);
                    return true;
                }
                player.message("Unbanned " + splitted[1]);
                break;
            }
            case "map":
            {
                c.getPlayer().changeMap(cserv.getMapFactory().getMap(Integer.parseInt(splitted[1])));
                break;
            }
            case "warphere":
            {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim.getAdmin()) {
                } else if (victim.gmLevel() >= player.gmLevel()) {
                    player.dropMessage("You can't warp a GM with an equal or higher GM level");
                } else {
                    victim.changeMap(c.getPlayer().getMap(), c.getPlayer().getMap().findClosestSpawnpoint(c.getPlayer().getPosition()));
                }
                break;
            }
            case "item":
            case "drop":
            {
                int itemId = Integer.parseInt(splitted[1]);
                short quantity = 1;
                try {
                    quantity = Short.parseShort(splitted[2]);
                } catch (Exception e) {
                }

                if (splitted[0].equals("item")) {
                    int petid = -1;
                    if (ItemConstants.isPet(itemId)) {
                        petid = MaplePet.createPet(itemId);
                    }
                    MapleInventoryManipulator.addById(c, itemId, quantity, player.getName(), petid, -1);
                } else {
                    Item toDrop;
                    if (MapleItemInformationProvider.getInstance().getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                        toDrop = MapleItemInformationProvider.getInstance().getEquipById(itemId);
                    } else {
                        toDrop = new Item(itemId, (byte) 0, quantity);
                    }
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), toDrop, c.getPlayer().getPosition(), true, true);
                }
                break;
            }
            case "w":
            case "warp":
            case "wa":
            {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim != null) {

                    if (splitted.length == 2) {
                        MapleMap target = victim.getMap();
                        c.getPlayer().changeMap(target, target.findClosestSpawnpoint(victim.getPosition()));
                    } else {
                        int mapid = Integer.parseInt(splitted[2]);
                        MapleMap target = c.getChannelServer().getMapFactory().getMap(mapid);
                        victim.changeMap(target, target.getPortal(0));
                    }
                } else {
                    try {
                        try {
                            MapleMap map = cserv.getMapFactory().getMap(Integer.parseInt(splitted[1]));

                            if (map != null) {
                                c.getPlayer().changeMap(map);
                            }

                        } catch (Exception e) {
                            // Perhaps I should not log this error
                            FilePrinter.printError(FilePrinter.CUSTOM + this.getClass().getName() + ".txt", e);
                        }

                        MapleCharacter victimChar = null;
                        for (MapleCharacter worldChar : Server.getInstance().getWorld(player.getWorld()).getPlayerStorage().getAllCharacters()) {

                            if (worldChar.getName().toLowerCase().equals(splitted[1].toLowerCase())) {
                                victimChar = worldChar;
                            }
                        }

                        if (victimChar == null) {
                            return true;
                        }

                        player.dropMessage("You will be warped to channel " + victimChar.getClient().getChannel() + ", this may take some time.");
                        
                        c.getPlayer().getMap().removePlayer(c.getPlayer());
                        c.getPlayer().setMap(victimChar.getMap());

                        int channel = victimChar.getClient().getChannel();
                        MapleCharacter chr = c.getPlayer();
                        Server server = Server.getInstance();

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
                        chr.getClient().updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION,c.getSessionIPAddress());

                        try {
                            c.announce(MaplePacketCreator.getChannelChange(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1])));
                        } catch (IOException e) {
                            FilePrinter.printError(FilePrinter.CUSTOM + this.getClass().getName() + ".txt", e);
                        }
                        
                    } catch (Exception e) {
                        FilePrinter.printError(FilePrinter.CUSTOM + this.getClass().getName() + ".txt", e);
                    }
                }
                break;
            }
            case "spawn":
            {
                int mid = Integer.parseInt(splitted[1]);
                int num = Math.min(getOptionalIntArg(splitted, 2, 1), 500);
                boolean cpq = false;

                if (splitted[0].equals("!spawncpq")) cpq = true;
                Integer hp = getNamedIntArg(splitted, 1, "hp");
                Integer exp = getNamedIntArg(splitted, 1, "exp");
                Double php = getNamedDoubleArg(splitted, 1, "php");
                Double pexp = getNamedDoubleArg(splitted, 1, "pexp");

                MapleMonster onemob = MapleLifeFactory.getMonster(mid);
                if (onemob == null) return false;

                int newhp = 0;
                int newexp = 0;

                double oldExpRatio = ((double) onemob.getHp() / onemob.getExp());

                if (hp != null) {
                    newhp = hp.intValue();
                } else if (php != null) {
                    newhp = (int) (onemob.getMaxHp() * (php.doubleValue() / 100));
                } else {
                    newhp = onemob.getMaxHp();
                }
                if (exp != null) {
                    newexp = exp.intValue();
                } else if (pexp != null) {
                    newexp = (int) (onemob.getExp() * (pexp.doubleValue() / 100));
                } else {
                    newexp = onemob.getExp();
                }

                if (newhp < 1) {
                    newhp = 1;
                }

                MapleMonsterStats overrideStats = new MapleMonsterStats();
                overrideStats.setHp(newhp);
                overrideStats.setExp(newexp);
                overrideStats.setMp(onemob.getMaxMp());

                for (int i = 0; i < num; i++) {
                    MapleMonster mob = MapleLifeFactory.getMonster(mid);
                    mob.setHp(newhp);
                    mob.setOverrideStats(overrideStats);
                    c.getPlayer().getMap().spawnMonsterOnGroundBelow(mob, c.getPlayer().getPosition());
                }
                MapleMonster monster = MapleLifeFactory.getMonster(Integer.parseInt(splitted[1]));
                if (monster == null) {
                    return true;
                }
                if (splitted.length > 2) {
                    for (int i = 0; i < Integer.parseInt(splitted[2]); i++) {
                        player.getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(Integer.parseInt(splitted[1])), player.getPosition());
                    }
                } else {
                    player.getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(Integer.parseInt(splitted[1])), player.getPosition());
                }
                break;
            }
            case "levelup":
            {
                player.gainExp(ExpTable.getExpNeededForLevel(player.getLevel()) - player.getExp(), false, false);
                break;
            }
            case "maxstat":
            {
                final String[] s = {"setall", String.valueOf(Short.MAX_VALUE)};
                execute(c, s, heading);
                player.setLevel(255);
                player.setFame(13337);
                player.setMaxHp(30000);
                player.setMaxMp(30000);
                player.updateSingleStat(MapleStat.LEVEL, 255);
                player.updateSingleStat(MapleStat.FAME, 13337);
                player.updateSingleStat(MapleStat.MAXHP, 30000);
                player.updateSingleStat(MapleStat.MAXMP, 30000);
                player.dropMessage("Done.");
                break;
            }
            case "maxskills":
            {
                MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/" + "String.wz")).getData("Skill.img").getChildren().stream().forEach(skill -> {
                    try {
                        Skill _skill = SkillFactory.getSkill(Integer.parseInt(skill.getName()));
                        player.changeSkillLevel(_skill, (byte) _skill.getMaxLevel(), _skill.getMaxLevel(), -1);
                    } catch (Exception e) {
                    }
                });
                break;
            }
            case "mesos":
            {
                player.gainMeso(Integer.parseInt(splitted[1]), true);
                break;
            }
            case "dc":
            {
                MapleCharacter victim = c.getWorldServer().getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim == null) {
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (victim == null) {
                        victim = player.getMap().getCharacterByName(splitted[1]);
                        if (victim != null) {
                            if(victim.gmLevel() < player.gmLevel()) {
                                try { // Sometimes bugged because the map = null
                                    victim.getClient().getSession().close(true);
                                    player.getMap().removePlayer(victim);
                                } catch (Exception e) {
                                    // Do nothing
                                }
                            } else if (victim.getAdmin()) {
                                player.getClient().getSession().close(true);
                                player.getMap().removePlayer(player);
                                victim.dropMessage(player.getName() + " has tried to !dc you!");
                            } else {
                                player.dropMessage("You can't dc a GM with a higher GM level than you!");
                                victim.dropMessage(player.getName() + " has tried to !dc you!");
                            }
                        } else {
                            break;
                        }
                    }
                }
                if (player.gmLevel() < victim.gmLevel()) {
                    victim = player;
                }
                victim.getClient().disconnect(false, false);
                break;
            }
            case "monsterids":
            {
                List<MapleMapObject> monsters = player.getMap().getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER));
                
                ArrayList<Integer> monsterIds = new ArrayList<>();
                for (MapleMapObject monstermo : monsters) {
                    MapleMonster monster = (MapleMonster) monstermo;
                    
                    if (monsterIds.contains(monster.getId())) {
                        player.message(monster.getId() + " - " + monster.getName());
                        monsterIds.add(monster.getId());
                    }
                }
                break;
            }
            case "channel":
            {
                int channel = Integer.valueOf(splitted[1]);
                MapleCharacter chr = c.getPlayer();
                Server server = Server.getInstance();

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
                chr.getClient().updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION,c.getSessionIPAddress());
                try {
                    c.announce(MaplePacketCreator.getChannelChange(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1])));
                } catch (IOException e) {
                }
                break;
            }
            case "resetreactors":
            {
                player.getMap().resetReactors();
                player.dropMessage("Done.");
                break;
            }
            case "resetmap":
            {
                player.getMap().killAllMonsters();
                player.getMap().clearDrops();
                player.getMap().resetReactors();
                player.dropMessage("Done.");
                break;
            }
            case "dropbee":
            {
                int[] itemArray = new int[4];
                itemArray[0] = 1002944;
                itemArray[1] = 1052193;
                itemArray[2] = 1102210;
                itemArray[3] = 1072259;
                
                short quantity = 1;
                
                for (int itemId : itemArray) {
                    Item toDrop;
                if (MapleItemInformationProvider.getInstance().getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                    toDrop = MapleItemInformationProvider.getInstance().getEquipById(itemId);
                } else {
                    toDrop = new Item(itemId, (byte) 0, quantity);
                }
                c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), toDrop, c.getPlayer().getPosition(), true, true);
                }                
            }
            default:
            {
                return false;
            }
        }
        
        return true;
    }
}
