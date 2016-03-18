/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.command.rank;

import client.*;
import client.command.CommandAbstract;
import client.command.CommandInterface;
import client.inventory.MapleInventoryType;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import constants.UnlockableJob;
import net.server.Server;
import net.server.channel.Channel;
import net.server.world.World;
import scripting.npc.NPCScriptManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MaplePortal;
import server.maps.MapleMap;
import tools.DatabaseConnection;
import tools.FilePrinter;
import tools.MaplePacketCreator;
import tools.StringUtil;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 *
 * @author Administrator
 */
public class PlayerCommand extends CommandAbstract implements CommandInterface {

    @Override
    public boolean execute(MapleClient c, String[] splitted, char heading) throws RemoteException {
        Channel cserv = c.getChannelServer();
        MapleCharacter player = c.getPlayer();
        if (!player.getJailed()) {
            switch (splitted[0].toLowerCase()) {
                case "smega":
                {
                    if (player.getLastSmega() + 5000 > System.currentTimeMillis()) {
                        player.dropMessage("You may only smega once every five seconds!");
                        break;
                    }
                    if (splitted.length >= 2) {
                        if (!player.isMuted()) {
                            Server.getInstance().broadcastMessage(c.getWorld(), MaplePacketCreator.serverNotice(3, c.getChannel(), player.getDisplayName() + " : " + joinStringFrom(splitted, 1), true));
                            player.updateLastSmega();
                        } else {
                            player.dropMessage("Your smega has been muted by a GM!");
                        }
                    } else {
                        player.dropMessage("Please use the following syntax: @smega [message]");
                    }
                    
                    break;
                }
                case "dathitmantho":
                {
                	if (player.getMap().getHitman() != null) {
                		player.getMap().broadcastMessage(MaplePacketCreator.getChatText(player.getId(), player.getMap().getHitman() , false, 0));
                        player.getMap().broadcastMessage(MaplePacketCreator.serverNotice(6, String.format("[Hitman] Congrats to %s for entering the list of names correctly first!", player.getName())));
                        player.getMap().setHitman(null);
                        break;
                    } else {
                    	player.dropMessage("There's no hitman entry for your map");
                        c.announce(MaplePacketCreator.enableActions());
                        break;
                    }
                }
                case "datntitho":
                {
                	player.toggleNTI();
                	player.dropMessage("Your NTI hax is now " + (player.getNTI() ? "on" : "off"));
                	break;
                }
                case "resetap":
                {
                    if (splitted.length == 2) {
                        if (player.resetAp(splitted[1])) {
                            player.dropMessage("Your AP have been reset.");
                        } else {
                            player.dropMessage("Please use the following syntax: @resetap <all/str/dex/int/luk>");
                        }
                    } else {
                        player.dropMessage("Please use the following syntax: @resetap <all/str/dex/int/luk>");
                    }
                    
                    break;
                }
                case "str":
                case "int":
                case "luk":
                case "dex":
                {
                    if (splitted.length == 2) {
                        int amount = Integer.parseInt(splitted[1]);
                        boolean str = splitted[0].equals("str");
                        boolean Int = splitted[0].equals("int");
                        boolean luk = splitted[0].equals("luk");
                        boolean dex = splitted[0].equals("dex");
                        
                        if (amount > 0 && amount <= player.getRemainingAp() && amount <= 32763 || amount < 0 && amount >= -32763 && Math.abs(amount) + player.getRemainingAp() <= 32767) {
                            if (str && amount + player.getStr() <= 32767 && amount + player.getStr() >= 4) {
                                player.setStr(player.getStr() + amount);
                                player.updateSingleStat(MapleStat.STR, player.getStr());
                                player.setRemainingAp(player.getRemainingAp() - amount);
                                player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
                            } else if (Int && amount + player.getInt() <= 32767 && amount + player.getInt() >= 4) {
                                player.setInt(player.getInt() + amount);
                                player.updateSingleStat(MapleStat.INT, player.getInt());
                                player.setRemainingAp(player.getRemainingAp() - amount);
                                player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
                            } else if (luk && amount + player.getLuk() <= 32767 && amount + player.getLuk() >= 4) {
                                player.setLuk(player.getLuk() + amount);
                                player.updateSingleStat(MapleStat.LUK, player.getLuk());
                                player.setRemainingAp(player.getRemainingAp() - amount);
                                player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
                            } else if (dex && amount + player.getDex() <= 32767 && amount + player.getDex() >= 4) {
                                player.setDex(player.getDex() + amount);
                                player.updateSingleStat(MapleStat.DEX, player.getDex());
                                player.setRemainingAp(player.getRemainingAp() - amount);
                                player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
                            } else {
                                player.dropMessage("Please make sure the stat you are trying to raise is not over 32,767 or under 4.");
                            }
                        } else {
                            player.dropMessage("Please make sure your AP is not over 32,767 and you have enough to distribute.");
                        }
                    } else {
                        player.dropMessage("Please use the following syntax: @str/@dex/@int/@luk [amount]");
                    }
                    
                    break;
                }
                case "shop":
                {
                    NPCScriptManager.getInstance().start(c, 9010002, null, null);
                    break;
                }
                case "howoldami": {
                    player.dropMessage(String.format("You're %d years old!", c.getAge()));
                    break;
                }
                case "unlock":
                {
                    if (splitted.length >= 2) {
                            int job = Integer.parseInt(splitted[1]);
                            UnlockableJob ujob = UnlockableJob.getJobById(job);
                            if (ujob == null) {
                                player.dropMessage(String.format("%d is not a valid unlockable job", job));
                                break;
                            }
                            if (player.getUnlockedJobs().contains(ujob)) {
                                player.dropMessage(String.format("You already own jobid %d", ujob.getId()));
                                break;
                            }
                            if (player.getMeso() < ujob.getPrice()) {
                                player.dropMessage(String.format("Insufficient mesos, price is %d", ujob.getPrice()));
                                break;
                            }
                        player.unlockJob(ujob);
                        player.gainMeso(-ujob.getPrice(), true);
                        player.dropMessage(String.format("job %s unlocked!", ujob.getName()));
                    } else {
                        List<UnlockableJob> unlockedJobs = player.getUnlockedJobs();
                        UnlockableJob.getJobs().stream().forEach(job -> {
                            player.dropMessage(String.format("Job:%s id:%d Price:%d ", job.getName(), job.getId(), job.getPrice()) + (unlockedJobs.contains(job) ? "<Unlocked>" : "<Locked>"));
                        });
                    }
                    break;
                }
                case "event":
                case "events":
                {
                    NPCScriptManager.getInstance().start(c, 9000001, null, null);
                    break;
                }
                case "dispose":
                {
                    NPCScriptManager.getInstance().dispose(c);
                    c.getSession().write(MaplePacketCreator.enableActions());
                    player.message("Done.");
                    break;
                }
                case "save":
                {
                    player.saveToDB();
                    player.dropMessage("Save complete!");
                    break;
                }
                case "unsetkey": {
                    player.dropMessage("Key unset");
                    player.setKey(null);
                    break;
                }
                case "setkey": {
                    if (splitted.length == 2) {
                        player.setKey(splitted[1]);
                        player.dropMessage(String.format("Key set to %s", splitted[1]));
                    } else {
                        player.dropMessage(String.format("Usage:%s <key>", splitted[0]));
                    }
                    break;
                }
                case "job":
                {
                	if(splitted.length == 2){
                		int jobid = Integer.parseInt(splitted[1]);
                        if (player.getUnlockedJobs().contains(UnlockableJob.getJobById(jobid))) {
                            player.changeJob(MapleJob.getById(jobid),false);
                			break;
                		}
                		else{
                			player.dropMessage(String.format("You do not own jobid %d",jobid));
                			break;
                		}
                	}
                	break;
                }
                case "buypup": {
                    if (player.getMeso() > 2000000000) {
                        if (!player.getInventory(MapleInventoryType.USE).isFull()) {
                            player.gainMeso(-2000000000, false);
                            MapleInventoryManipulator.addById(c, 4001454, (short) 1, player.getName(), -1, -1);
                        } else {
                            player.dropMessage(6,"Your inventory is full.");
                        }
                    } else {
                        player.dropMessage(6,"You have insufficient mesos.");
                    }
                    break;
                }
                case "sellpup": {
                    if (!((long) player.getMeso() + 2000000000 > Integer.MAX_VALUE)) {
                        MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, 4001454, 1, false, false);
                        player.gainMeso(2000000000, true);
                        player.dropMessage(6,"You have successfully converted your puppet to mesos.");
                    } else {
                        player.dropMessage("You have too much mesos, please stash some of them before attempting to sell a puppet.");
                    }
                    break;
                }
                case "emo":
                case "suicide":
                {
                    int exp = player.getExp();
                    
                    player.setHp(0);
                    player.setExp(exp);
                    player.updateSingleStat(MapleStat.HP, 0);
                    player.updateSingleStat(MapleStat.EXP, exp);
                    
                    break;
                }
                case "help":
                {
                    NPCScriptManager.getInstance().start(c,9120104,null,null);
                    break;
                }
                case "skillbooks":
                {
                    Lists.newArrayList(21120001, 21120002, 21120004, 21120005, 21120006, 21120007, 21120009, 21120010, 21121000, 21121003, 1120003, 1120004, 1120005, 1121000, 1121001, 1121002, 1121006, 1121008, 1121010, 1220005, 1220006, 1220010, 1221000, 1221001, 1221002, 1221003, 1221004, 1221007, 1221009, 1221011, 1320005, 1320006, 1320008, 1320009, 1321000, 1321001, 1321002, 1321003, 1321007, 2121000, 2121001, 2121002, 2121003, 2121004, 2121005, 2121006, 2121007, 2221000, 2221001, 2221002, 2221003, 2221004, 2221005, 2221006, 2221007, 2321000, 2321001, 2321002, 2321003, 2321004, 2321005, 2321006, 2321007, 2321008, 3120005, 3121000, 3121002, 3121003, 3121004, 3121006, 3121007, 3121008, 3220004, 3221000, 3221001, 3221002, 3221003, 3221005, 3221006, 3221007, 4120002, 4120005, 4121000, 4121003, 4121004, 4121006, 4121007, 4121008, 4220002, 4220005, 4221000, 4221001, 4221003, 4221004, 4221006, 4221007, 5121000, 5121001, 5121002, 5121003, 5121004, 5121005, 5121007, 5121008, 5121009, 5121010, 5220001, 5220002, 5220011, 5221000, 5221003, 5221004, 5221006, 5221007, 5221008, 5221009, 5221010).stream().filter(skillid->skillid/10000==player.getJob().getId()).forEach(skillId->{
                        Skill skill = SkillFactory.getSkill(skillId);
                        if(!player.getSkills().containsKey(skill)){
                            player.changeSkillLevel(skill,(byte) 0,10,-1);
                        }
                        else if(player.getSkills().get(skill).masterlevel < 10) {
                            player.changeSkillLevel(skill,player.getSkills().get(skill).skillevel,10,-1);
                        }
                    });
                    break;
                }
                case "resetsp":
                {
                 player.getSkills().entrySet().stream().forEach(new Consumer<Map.Entry<Skill, MapleCharacter.SkillEntry>>() {
                     @Override
                     public void accept(Map.Entry<Skill, MapleCharacter.SkillEntry> sse) {
                         Skill _skill = SkillFactory.getSkill(sse.getKey().getId());
                         int sp = player.getSkillLevel(_skill.getId());
                         player.changeSkillLevel(_skill, (byte) 0, _skill.getMaxLevel(), -1);
                         player.setRemainingSp(player.getRemainingSp()+sp);
                     }
                 });
                    break;
                }
                case "rewarp":
                case "fakerelog":
                {
                    player.setMap(player.getMapId());
                    break;
                }
                case "command":
                case "commands":
                {
                    player.dropMessage("@go <Town Name>");
                    player.dropMessage("@ep2id <Item ID>");
                    player.dropMessage("@unlock <Job ID>");
                    player.dropMessage("@job <Job ID>");
                    player.dropMessage("@smega <Message>");
                    player.dropMessage("@str | @dex | @int | @luk <Amount>");
                    player.dropMessage("@emo | suicide");
                    player.dropMessage("@save (saves progress)");
                    player.dropMessage("@buypup ");
                    player.dropMessage("@sellpup");
                    player.dropMessage("@dispose (if shit's buggy)");
                    player.dropMessage("@online | @connected");
                    player.dropMessage("@joinevent | @je");
                    player.dropMessage("@style | @beauty");
                    player.dropMessage("@resetap all/str/dex/int/luk");
                    player.dropMessage("@chalktalk <message>");
                    player.dropMessage("@checkep (check your current amount of event points)");
                    player.dropMessage("@checkvp (check your current amount of vote points)");
                    break;
                }
                case "stash": {
                    player.getStorage().sendStorage(player.getClient(), 1032006);
                    break;
                }
                case "clearinv": {
                    if (splitted.length == 2) {
                        MapleInventoryType inventoryType = null;
                        switch (splitted[1].toLowerCase()) {
                            case "equip":
                            case "eqp": {
                                inventoryType = MapleInventoryType.EQUIP;
                                break;
                            }
                            case "use": {
                                inventoryType = MapleInventoryType.USE;
                                break;
                            }
                            case "etc": {
                                inventoryType = MapleInventoryType.ETC;
                                break;
                            }
                            case "setup": {
                                inventoryType = MapleInventoryType.SETUP;
                                break;
                            }
                            case "cash": {
                                inventoryType = MapleInventoryType.CASH;
                            }
                            default: {
                                if (splitted[1].toLowerCase().equals("all")) {
                                    player.getInventory(MapleInventoryType.CASH).allInventories().stream().filter(type -> type.getType() != MapleInventoryType.EQUIPPED).forEach(inv -> {
                                        inv.list().stream().forEach(item -> MapleInventoryManipulator.removeById(c, inv.getType(), item.getItemId(), item.getQuantity(), false, false));
                                    });
                                    break;
                                }
                                break;
                            }
                        }
                        if (inventoryType != null) {
                            final MapleInventoryType finalInventoryType = inventoryType;
                            player.getInventory(inventoryType).list().stream().forEach(item -> {
                                MapleInventoryManipulator.removeById(c, finalInventoryType, item.getItemId(), item.getQuantity(), false, false);
                            });
                        }
                    }
                    break;
                }
                case "online":
                case "connected":
                {
                    int onlinePlayers = 0;
                    String playerStr = "";
                    World world = Server.getInstance().getWorld(player.getWorld());
                    boolean first = true;
                    for (Channel worldCserv : world.getChannels()) {
                        for (MapleCharacter chanChar : worldCserv.getPlayerStorage().getAllCharacters()) {
                            try {
                                if (chanChar.gmLevel() <= 5) {
                                    onlinePlayers++;
                                    
                                    if (first) {
                                        playerStr += chanChar.getName();
                                        first = false;
                                    } else {
                                        playerStr += ", " + chanChar.getName();
                                    }
                            }
                            } catch (Exception e) {
                                System.out.println("@connected error: " + e.getMessage());
                            }
                        }
                    }       player.dropMessage("Online players: " + onlinePlayers);
                    player.dropMessage(playerStr);
                    
                    break;
                }
                case "fm":
                {
                    if (player.getMapId() != 910000000) {
                        MapleMap target = cserv.getMapFactory().getMap(910000000);
                        MaplePortal targetPortal = target.getPortal(0);
                        player.changeMap(target, targetPortal);
                    } else {
                        player.dropMessage("You are already in the Free Market Entrance.");
                    }
                    
                    break;
                }
                case "home":{
                    player.changeMap(400000000);
                    break;
                }
                case "joinevent":
                case "je": {
                    if (cserv.getEventMap() != null) {
                        player.changeMap(cserv.getEventMap());
                        break;
                    } else {
                        player.dropMessage("An event is not running.");
                        break;
                    }
                }
                case "ep2id": {
                    if (splitted.length == 2) {
                        try {
                            if(player.getEventPoints()<3){
                                player.dropMessage("Insufficient event points.");
                                break;
                            }
                            if (Long.parseLong(splitted[1]) > 0) {
                                int itemId = Integer.parseInt(splitted[1]);
                                Connection con = DatabaseConnection.getConnection();
                                PreparedStatement ps = con.prepareStatement("SELECT * FROM ep2id WHERE itemid = ?");
                                ps.setInt(1, itemId);
                                if (ps.executeQuery().next()) {
                                    player.dropMessage("The item you have requested is a banned item.");
                                    break;
                                }
                                if (MapleItemInformationProvider.getItemPairById(itemId) != null) {
                                    MapleInventoryManipulator.addById(player.getClient(), itemId, (short) 1);
                                    player.dropMessage(String.format("Enjoy your %s", MapleItemInformationProvider.getItemPairById(itemId).getRight()));
                                    player.setEventPoints(player.getEventPoints() - 3);
                                    FilePrinter.print("ep2id.txt", String.format("%s (%d)->%d", player.getName(), player.getId(), itemId));
                                    break;
                                }
                                player.dropMessage("The item id you have entered does not exist.");
                                break;
                            }
                            player.dropMessage(String.format("%s is not a valid item id", splitted[1]));
                            break;
                        } catch (NumberFormatException | SQLException nfe) {
                            player.dropMessage(splitted[2] + " is not a valid itemid");
                        }
                    }
                    player.dropMessage("Invalid syntax, usage is @ep2id <itemid>");
                    break;
                }
                case "go":
                {
                    int gotomap = 0;

                        switch (splitted[1].toLowerCase()) {
                            case "temple":
                                gotomap = gotomap = 270000100;
                            case "fm":
                                gotomap = 910000000;
                                break;
                            case "lith":
                                gotomap = 104000000;
                                break;
                            case "henesys":
                            case "hen":
                                gotomap = 100000000;
                                break;
                            case "perion":
                                gotomap = 102000000;
                                break;
                            case "ellinia":
                                gotomap = 101000000;
                                break;
                            case "kerning":
                                gotomap = 103000000;
                                break;
                            case "ludi":
                            case "ludibrium":
                                gotomap = 220000000;
                                break;
                            case "el nath":
                            case "elnath":
                            case "nath":
                                gotomap = 211000000;
                                break;
                            case "leafre":
                            case "leaf":
                                gotomap = 240000000;
                                break;
                            case "aqua":
                                gotomap = 230000000;
                                break;
                            case "mulung":
                                gotomap = 250000000;
                                break;
                            case "ariant":
                                gotomap = 260000200;
                                break;
                            case "rien":
                                gotomap = 140000000;
                                break;
                            case "orbis":
                                gotomap = 200000000;
                                break;
                            case "sleepywood":
                                gotomap = 105040300;
                                break;
                            case "omega":
                                gotomap = 221000000;
                                break;
                            case "nlc":
                                gotomap = 600000000;
                                break;
                            case "haunted":
                                gotomap = 682000000;
                                break;
                            case "shrine":
                                gotomap = 800000000;
                                break;
                            case "showa":
                                gotomap = 801000000;
                                break;
                            case "altaire":
                                gotomap = 300000000;
                                break;
                            case "herb town":
                                gotomap = 251000000;
                                break;
                            case "ereve":
                                gotomap = 130000000;
                                break;
                            case "time":
                                gotomap = 222020111;
                                break;
                            case "hang":
                                gotomap = 922231001;
                                break;
                            case "guildhq":
                                gotomap = 261030000;
                                break;
                            case "guild":
                                gotomap = 200000301;
                                break;
                            case "carnival":
                                gotomap = 970042505;
                                break;
                            default:
                                NPCScriptManager.getInstance().start(c, 9200000, null, null);
                                break;
                        }

                    if (gotomap != 0) {
                        MapleMap target = cserv.getMapFactory().getMap(gotomap);
                        player.changeMap(target);
                    }

                    break;
                }
                case "d2h": {
                    if (splitted[1] != null) {
                        Integer input = Integer.parseInt(splitted[1]);
                        try {
                            player.dropMessage(6, input + " = 0x" + Integer.toHexString(input).toUpperCase());
                        } catch (Exception e) {
                            //player.dropMessage("nowai bruh");
                        }
                    } else {
                        player.dropMessage("Usage: d2h <number>");
                    }
                    break;
                }
                case "h2d": {
                    if (splitted[1] != null) {
                        try {
                            player.dropMessage(6, "0x" + splitted[1].toUpperCase() + " = " + Integer.toString(Integer.valueOf(splitted[1], 16)).toUpperCase());
                        } catch (Exception e) {
                            //player.dropMessage("nowai bruh");
                        }
                    } else {
                        player.dropMessage("Usage: h2d <number>");
                    }
                    break;
                }
                case "boss":
                {
                    int gotobossmap = 0;
                    boolean hasUnlock = true;

                    switch (splitted[1].toLowerCase()) {
                        case "mushmom":
                            gotobossmap = 100000005;
                            break;
                        case "zmushmom":
                        case "zombiemushmom":
                            gotobossmap = 105070002;
                            break;
                        case "jrbalrog":
                        case "juniorbalrog":
                            gotobossmap = 105090900;
                            break;
                        case "pianus":
                            gotobossmap = 230040420;
                            break;
                        case "zakum":
                            gotobossmap = 280030000;
                            break;
                        case "pap":
                        case "papulatus":
                            gotobossmap = 220080000;
                            break;
                        case "manon":
                            gotobossmap = 240020402;
                            break;
                        case "griffey":
                            gotobossmap = 240020101;
                            break;
                        case "body":
                        case "bodyguard":
                            gotobossmap = 801040100;
                            break;
                        case "bf":
                        case "bigfoot":
                            gotobossmap = 610010005;
                            break;
                        case "bf2":
                        case "bigfoot2":
                            gotobossmap = 610010012;
                            break;
                        case "bf3":
                        case "bigfoot3":
                            gotobossmap = 610010013;
                            break;
                        case "bf4":
                        case "bigfoot4":
                            gotobossmap = 610010100;
                            break;
                        case "bf5":
                        case "bigfoot5":
                            gotobossmap = 610010101;
                            break;
                        case "bf6":
                        case "bigfoot6":
                            gotobossmap = 610010102;
                            break;
                        case "bf7":
                        case "bigfoot7":
                            gotobossmap = 610010103;
                            break;
                        case "bf8":
                        case "bigfoot8":
                            gotobossmap = 610010104;
                            break;
                        case "ht":
                        case "horntail":
                            gotobossmap = 240060200;
                            break;
                        default:
                            player.dropMessage("@boss [boss] (mushmom, zmushmom, jrbalrog, pianus, zakum, papulatus, manon, griffey, bodyguard, bigfoot, bigfoot2 -> bigfoot8 & horntail)");
                            break;
                    }

                    if (gotobossmap != 0) {
                        MapleMap target = cserv.getMapFactory().getMap(gotobossmap);
                        player.changeMap(target);
                    }
                    break;
                }
                case "getep":
                case "checkep":
                case "geteventpoints": {
                    player.dropMessage(String.format("You currently have %d event points!", player.getEventPoints()));
                    break;
                }
                case "getvp":
                case "checkvp":
                case "getvotepoints": {
                    player.dropMessage(String.format("You currently have %d vote points!", player.getVotePoints()));
                    break;
                }
                /*
                case "nx":
                {
                    int votePoints = player.getVotePoints();
                    int nxPerPoint = 4000;
                    
                    if (player.hasUnlock("extra_nx_per_vp")) {
                        nxPerPoint = 5000;
                    }
                    
                    int nxToAdd = votePoints * nxPerPoint;
                    player.getCashShop().gainCash(1, nxToAdd);
                    player.setVotePoints(0);
                    player.dropMessage("You have converted " + votePoints + " vote points to " + nxToAdd + " NX!");
                    break;
                }
                */
                case "chalktalk": {
                    player.setChalkboard(splitted.length > 1 ? StringUtil.joinStringFrom(splitted, 1) : null);
                    if (player.isHidden()) {
                        player.getMap().broadcastGMMessage(MaplePacketCreator.useChalkboard(player, !(splitted.length > 1)));
                    } else {
                        player.getMap().broadcastMessage(MaplePacketCreator.useChalkboard(player, !(splitted.length > 1)));
                    }
                    break;
                }
                case "style":
                case "beauty":
                {
                	NPCScriptManager.getInstance().start(c, 9330021, null, null);
                    break;
                }
                case "perms": {
                    player.dropMessage(String.format("isGM:%s isClientGM:%s isUnderCover:%s GMRank:%s", player.isGM(), c.isClientGM(), player.isUnderCover(), player.getGMRank()));
                    break;
                }
                case "ucauth": {
                    if (splitted.length == 2) {
                        if (player.underCoverCheck(splitted[1])) {
                            player.setUnderCover(true);
                            player.dropMessage("Undercover mode: ON");
                        } else {
                            player.dropMessage("Invalid password");
                        }
                    }
                    break;
                }
                case "ucdeauth": {
                    if (player.isUnderCover()) {
                        player.setUnderCover(false);
                        player.dropMessage("Undercover mode: OFF");
                    }
                }
                case "info":
                {
                    try {
                        Gson gson = new Gson();
                        JsonElement charinfo = gson.toJsonTree(player);
                        System.out.println(charinfo);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    break;
                }
                default:
                {
                    return false;
                }
            }
        } else {
            player.dropMessage("You can't use Player Commands at the moment.");
        }
        
        return true;
    }
}
