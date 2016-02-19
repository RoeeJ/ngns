/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
                       Matthias Butz <matze@odinms.de>
                       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation. You may not use, modify
    or distribute this program under any other version of the
    GNU Affero General Public License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    
    This class, net.sf.odinms.client.sexbot.Sexbot.java, was written
    by Ethan Jenkins (Sathon) and is his property.
*/


package client.sexbot;

import client.BuddylistEntry;
import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import net.server.Server;
import net.server.channel.Channel;
import net.server.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.MapleItemInformationProvider;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleMonsterInformationProvider;
import server.life.MonsterDropEntry;
import server.maps.AnimatedMapleMapObject;
import server.maps.MapleMap;
import server.movement.AbsoluteLifeMovement;
import server.movement.LifeMovement;
import server.movement.LifeMovementFragment;
import tools.MaplePacketCreator;
import tools.MockIOSession;
import tools.Pair;
import tools.StringUtil;

import java.awt.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.List;


/* Class Sexbot, a fun feature for servers
 * Sexbot is a fake character (bot) that interacts with players in different ways
 * Sexbot is by Sathon aka Ethan Jenkins
 */
public class SexBot
{
	private static final Logger log = LoggerFactory.getLogger(SexBot.class);
    private static Thread patrolThread;
    private MapleClient c;
    private MapleCharacter player;
    private int map;
	private boolean spawned;
	private MapleCharacter follow;
    private boolean recording;
    private boolean moving = false;
    private boolean patroling = false;
    private List<List<LifeMovementFragment>> moves;
    private MapleInventory savedInv;
    private List<String> prethink = Arrays.asList("hm", "hmm, lemme think", "uhhh", "uhh", "its uhm..", "its uhh..", "its", "that's");
    private boolean monitoringDrops;
    private boolean sitting;
    private Boolean lock = false;
    //private TimerManager smegaTimer;

	public SexBot()
	{
		this.spawned = false;
		this.c = new MapleClient(null, null, new MockIOSession());

		follow = null;
	}

    public static MapleCharacter getCharacter(SexBot instance) {
        return instance.player;
    }

    public int getMap()
	{
		return this.map;
	}

	public MapleClient getClient()
	{
		return this.c;
	}

    public Boolean getLock() {
        return lock;
    }

    public void setLock(Boolean b) {
        this.lock = b;
    }

    public boolean isSpawned()
	{
		return this.spawned;
	}
	public boolean isPatroling(){
        return patroling || moving;
    }
	public MapleCharacter getFollow()
	{
		return this.follow;
	}

	public void setFollow(MapleCharacter newc)
	{
		this.follow = newc;
	}
    public void stopPatrol(){
    patrolThread.interrupt();
    }

    public void move(LifeMovementFragment move) {
        List res = Arrays.asList(move);
        byte[] packet = MaplePacketCreator.movePlayer(getCharacter(this).getId(), res);
        getCharacter(this).getMap().broadcastMessage(getCharacter(this), packet, false);
        updatePosition(res, getCharacter(this), 0);
        player.getMap().movePlayer(player, player.getPosition());
    }

    public void move(List<LifeMovementFragment> moves) {
        moves.stream().forEachOrdered(_res -> {
            List<LifeMovementFragment> res = Arrays.asList(_res);
            byte[] packet = MaplePacketCreator.movePlayer(getCharacter(this).getId(), res);
            getCharacter(this).getMap().broadcastMessage(getCharacter(this), packet, false);
            updatePosition(res, getCharacter(this), 0);
            player.getMap().movePlayer(player, player.getPosition());
        });
    }

    public void patrol() {
        patrolThread = new Thread(() -> {
            try {
                while (hasRecording()) {
                    for (List<LifeMovementFragment> res : moves) {
                        byte[] packet = MaplePacketCreator.movePlayer(getCharacter(this).getId(), res);
                        getCharacter(this).getMap().broadcastMessage(getCharacter(this), packet, false);
                        updatePosition(res, getCharacter(this), 0);
                        player.getMap().movePlayer(player, player.getPosition());
                        Thread.sleep(500);
                    }
                }
            } catch (Exception e) {
                moves.clear();
                patrolThread = null;
            }
        });
        patrolThread.start();
    }

    public void moveTest() {
        Point pos = getCharacter(this).getPosition();
        ArrayList<LifeMovementFragment> res = new ArrayList<>();
        AbsoluteLifeMovement alm = new AbsoluteLifeMovement((byte) 0, new Point(pos.x + 5, pos.y), 500, (byte) 0);

        alm.setUnk(17);
        alm.setPixelsPerSecond(new Point(0, 0));

        res.add(new AbsoluteLifeMovement((byte) 0, new Point(pos.x + 200, pos.y), 500, (byte) 0));

        byte[] packet = MaplePacketCreator.movePlayer(getCharacter(this).getId(), res);
        getCharacter(this).getMap().broadcastMessage(getCharacter(this), packet, false);
        updatePosition(res, getCharacter(this), 0);
        player.getMap().movePlayer(player, player.getPosition());
    }

    public String handleChat(String text, final String name, MapleCharacter player) throws IOException {
        final String[] splitted = text.split(" ");

        final String[] response = {"Error processing chat."};
        if(StringUtil.joinStringFrom(splitted,1).toLowerCase().equals("what does the scouter say about his power level?"))
        {
            getCharacter(this).getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.facialExpression(c.getPlayer(), 5), false);
            return "ITS OVER NINE THOUSAND!!!";
        }
        switch(splitted[1])
        {
            case "define":
            {
                preThink();
                try {
                    Thread.sleep(new Random().nextInt(1000+2000));
                    response[0] =  ChatHandler.Companion.processDefinition(splitted, name);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            }
            case "blind": {
                player.getMap().getCharacters().stream().forEach(chr -> {
                    chr.announce(new byte[]{(byte) 0xC6, 0x00, 0x31, 0x75, 0x00, 0x00, 0x01, 0x72, (byte) 0x9A, (byte) 0x98, 0x00, 0x03, 0x01});
                });
                break;
            }
            case "pwn": {
                try {
                    MapleCharacter mc = player.getClient().getChannelServer().getPlayerStorage().getCharacterByName(splitted[2]);
                    if (mc.getAccountID() != 1) {
                        mc.kick("being a faggot");
                        return "Done!";
                    }
                    return "nope.";
                } catch (Exception e) {
                    return "nope";
                }
            }
            case "lock": {
                if (player.getClient().getAccountName().equals("Thor")) {
                    setLock(true);
                    return lock ? "Locked!" : "Unlocked!";
                }
                return "nope.";
            }
            case "unlock": {
                if (player.getClient().getAccountName().equals("Thor")) {
                    setLock(false);
                    return lock ? "Locked!" : "Unlocked!";
                }
                return "nope.";
            }
            case "tell":
            {
                response[0] = ChatHandler.Companion.generateJoke(splitted[2]);
                break;
            }
            case "drop":
            {
                if(!player.isGM()){return "Nope.";}
                if (StringUtil.joinStringFrom(splitted, 2).toLowerCase().equals("the bass")) {
                    getCharacter(this).getMap().broadcastMessage(MaplePacketCreator.trembleEffect(1, 100));
                    return "WUB WUB WUB";
                }
                String itemName = StringUtil.joinStringFrom(splitted,2);
                Pair<Integer, String> itemPair;
                try {
                    try {
                        itemPair = MapleItemInformationProvider.getItemPairById(Integer.parseInt(itemName));
                    } catch (NumberFormatException nfe) {
                        itemPair = MapleItemInformationProvider.getItemPairByNameLike(itemName).get(0);
                    }
                    response[0] = String.format("Have fun with your %s!", itemPair.getRight());
                    int itemId;
                    try {
                        itemId = Integer.parseInt(splitted[2]);
                    }
                    catch (Exception e){
                        itemId = itemPair.getLeft();
                    }
                    Item toDrop;
                    if (MapleItemInformationProvider.getInstance().getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                        toDrop = MapleItemInformationProvider.getInstance().getEquipById(itemId);
                    } else {
                        toDrop = new Item(itemId, (byte) 0, (short) 1);
                    }
                    getCharacter(this).getMap().spawnItemDrop(getCharacter(this), getCharacter(this), toDrop, player.getPosition(), true, true);
                    getCharacter(this).getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.facialExpression(c.getPlayer(), 2), false);
                } catch (Exception e) {
                    getCharacter(this).getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.facialExpression(c.getPlayer(), 6), false);
                    return String.format("WTF is a %s", itemName);
                }
                break;
            }
            case "whatdrops":
            case "whatdrop":
            case "whodrops":
            case "whodrop":
            {
                preThink();
                String itemName = StringUtil.joinStringFrom(splitted,2);
                List<Pair<Integer, String>> itemPairs = MapleItemInformationProvider.getItemPairByNameLike(itemName);
                List<MonsterDropEntry> drops = new ArrayList<>();
                if(itemPairs == null || (itemPairs != null && itemPairs.size() == 0)){return "test";}
                for(Pair<Integer, String> itemPair : itemPairs)
                {
                    drops.addAll(MapleMonsterInformationProvider.getInstance().retrieveDropByItemId(itemPair.getLeft()));
                }
                if(drops.size() == 1) {
                    MapleMonster mob = MapleLifeFactory.getMonster(drops.get(0).mobId);
                    return String.format("item %s is dropped by %s", itemName, mob.getName());
                }
                else if(drops.size() > 1){
                    for(MonsterDropEntry monsterDropEntry:drops.subList(0,15)){
                        MapleMonster mob = MapleLifeFactory.getMonster(monsterDropEntry.mobId);
                        player.getClient().announce(MaplePacketCreator.getWhisper("Skynet", getClient().getChannel(), String.format("item %s is dropped by %s", itemName, mob.getName())));
                    }
                    return "Entries whispered!";
                }
                else {
                    return "TBH, IDK";
                }
            }
            case "follow":
            {
                if(player.isGM())
                {
                    if(splitted[2].equals("me")) {
                        setFollow(player);
                        response[0] = "Yay! A friend! I will follow you, " + player.getName() + "!";
                    }
                    else {
                        MapleCharacter tofollow = player.getClient().getChannelServer().getPlayerStorage().getCharacterByName(splitted[2]);
                        if(tofollow != null){
                            setFollow(tofollow);
                            response[0] = "Yay! A friend! I will follow you, " + tofollow.getName() + "!";
                        }
                        else {
                            getCharacter(this).getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.facialExpression(c.getPlayer(), 6), false);
                            return String.format("Who the fuck is %s???",splitted[2]);
                        }
                    }
                }

                else
                {
                    response[0] = name + ", I don't want to follow you.";
                }
                break;
            }
            case "warp2me":
            {
                getCharacter(this).changeMap(player.getMap());
                return "OMW!";
            }
            case "ty":
            case "thank":
            case "thanks":
            {
                getCharacter(this).getMap().broadcastMessage(MaplePacketCreator.facialExpression(getCharacter(this), 0x2));
                return "yw";
            }
            case "walk":
            {
                doMoves();
                return "Aye!";
            }
            case "stop":
            {
                patrolThread.interrupt();
                patroling = false;
                return "Aye!";
            }
            case "kill":
            case "heal":
            {
                if(player.isGM())
                {
                    switch (splitted[2]) {
                        case "me":
                            if (splitted[1].equals("kill")) {
                                player.setHpMp(0);
                                getCharacter(this).getMap().broadcastMessage(MaplePacketCreator.facialExpression(getCharacter(this), 0xF));
                                return String.format("DIE %s!", player.getName());
                            } else {
                                player.heal();
                                getCharacter(this).getMap().broadcastMessage(MaplePacketCreator.facialExpression(getCharacter(this), 0x11));
                                return String.format("You're welcome %s!", player.getName());
                            }
                        case "map":
                        case "everyone":
                            if (splitted[1].equals("kill")) {
                                if (player.getAccountID() == 5) return "stfu";
                                getCharacter(this).getMap().getCharacters().forEach(client.MapleCharacter::kill);
                                for (MapleCharacter vic : getCharacter(this).getMap().getCharacters()) vic.setHpMp(0);
                                player.setHpMp(0);
                                getCharacter(this).getMap().broadcastMessage(MaplePacketCreator.facialExpression(getCharacter(this), 0xF));
                                return "FUCK YOU ALL!";
                            } else {
                                getCharacter(this).getMap().getCharacters().forEach(client.MapleCharacter::heal);
                                getCharacter(this).getMap().broadcastMessage(MaplePacketCreator.facialExpression(getCharacter(this), 0x11));
                                return "You're welcome";
                            }
                        default:
                            MapleCharacter tofollow = player.getClient().getChannelServer().getPlayerStorage().getCharacterByName(splitted[2]);
                            if (tofollow != null) {
                                if (splitted[1].equals("kill")) {
                                    if (player.getAccountID() == 5) return "stfu";
                                    tofollow.setHpMp(0);
                                    getCharacter(this).getMap().broadcastMessage(MaplePacketCreator.facialExpression(getCharacter(this), 0xF));
                                    return String.format("DIE %s!", tofollow.getName());
                                } else {
                                    tofollow.heal();
                                    getCharacter(this).getMap().broadcastMessage(MaplePacketCreator.facialExpression(getCharacter(this), 0x11));
                                    return String.format("You're welcome %s!", tofollow.getName());
                                }
                            } else {
                                getCharacter(this).getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.facialExpression(c.getPlayer(), 6), false);
                                return String.format("Who the fuck is %s???", splitted[2]);
                            }
                    }
                }

                else
                {
                    response[0] = name + ", no way i'm going to jail for you!.";
                }
                break;
            }
            case "unfollow":
            {
                if(getFollow() != null){
                    response[0] = "Aye!";
                    setFollow(null);
                } else {
                    response[0] = "I'm not following you";
                    getCharacter(this).getMap().broadcastMessage(MaplePacketCreator.facialExpression(getCharacter(this), 0x6));
                }
                break;
            }
            case "savei":
            {
                savedInv = getCharacter(this).getInventory(MapleInventoryType.EQUIPPED);
                getCharacter(this).getMap().broadcastMessage(MaplePacketCreator.updateCharLook(getCharacter(this)));
                response[0] = "saved!";
                break;
            }
            case "loadi":
            {
                getCharacter(this).setInventory(MapleInventoryType.EQUIPPED, savedInv);
                getCharacter(this).getMap().broadcastMessage(MaplePacketCreator.updateCharLook(getCharacter(this)));
                response[0] = "loaded!";
                break;
            }
            case "ping":
            {
                response[0] = "pong";
                break;
            }
            case "movetest": {
                moveTest();
                return "Done!";
            }
            case "monitor":
            case "unmonitor":
            case "demonitor":
            {
                Boolean monitor = splitted[1].equals("monitor");
                setMonitoringDrops(monitor);
                return monitor ? "Monitoring drops!" : "Aye!";
            } case "sit":
            {
                if(!sitting) {
                    int itemId = 3010515;
                    getCharacter(this).setChair(itemId);
                    getCharacter(this).getMap().broadcastMessage(MaplePacketCreator.showChair(getCharacter(this).getId(), itemId));
                    //getCharacter().getMap().broadcastMessage(MaplePacketCreator.showChair(getCharacter().getId(), 3010515));
                    sitting = true;
                    response[0] = "Sitting!";
                    break;
                }
                else
                {
                    getCharacter(this).setChair(0);
                    getCharacter(this).getMap().broadcastMessage(MaplePacketCreator.cancelChair(-1));
                    response[0] = "Aye!";
                    sitting = false;
                    break;
                }
            }
            case "copy":
            {
                if (lock && !player.getClient().getAccountName().equals("Thor")) {
                    return "nope.";
                }
                if(splitted[2].equals("me")) {
                    copy(player);
                    getCharacter(this).getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.facialExpression(c.getPlayer(), 2), false);
                    return "copied you!";
                }
                else {
                    try {
                        MapleCharacter tofollow = player.getClient().getChannelServer().getPlayerStorage().getCharacterByName(splitted[2]);

                        if (tofollow != null) {
                            copy(tofollow);
                            getCharacter(this).getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.facialExpression(c.getPlayer(), 2), false);
                            return String.format("copied %s!", tofollow.getName());
                        } else {
                            getCharacter(this).getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.facialExpression(c.getPlayer(), 6), false);
                            return String.format("Who the fuck is %s???", splitted[2]);
                        }
                    }
                    catch (Exception e){return e.getMessage();}
                }
            }
            default:
            {
                preThink();
                try {
                    Thread.sleep(new Random().nextInt(1000+2000));
                    WolframProcessor.process(StringUtil.joinStringFrom(splitted, 1), SexBot.getCharacter(this));
                    //else response[0] = ChatHandler.processChat(splitted, name);
                    return "";
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return response[0];
	}

    private void preThink() {
        getCharacter(this).getMap().broadcastMessage(MaplePacketCreator.facialExpression(getCharacter(this), 3));
        getCharacter(this).getMap().broadcastMessage(MaplePacketCreator.getChatText(getCharacter(this).getId(), prethink.get(new Random().nextInt(prethink.size() - 1)), false, 1));
    }

    private void copy(MapleCharacter player) {
        getCharacter(this).setItemEffect(player.getItemEffect());
        getCharacter(this).getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.itemEffect(getCharacter(this).getId(), player.getItemEffect()), false);
        getCharacter(this).setInventory(MapleInventoryType.EQUIPPED, player.getInventory(MapleInventoryType.EQUIPPED));
        getCharacter(this).setFace(player.getFace());
        getCharacter(this).setHair(player.getHair());
        getCharacter(this).setGender(player.getGender());
        getCharacter(this).setSkinColor(player.getSkinColor());
        getCharacter(this).getMap().broadcastMessage(MaplePacketCreator.updateCharLook(getCharacter(this)));
    }
    public Integer tryParse(Object obj) {
        try {
            return Integer.parseInt(String.valueOf(obj));
        } catch (NumberFormatException nfe) {
            return -1; // or null if that is your preference
        }
    }

    /* Spawn Sexbot
     *Sexbot is by Sathon aka Ethan Jenkins
     */
	public void spawnSexBot(MapleMap map, Point spawnPoint, int cid)
	{
		try
		{
			this.player = MapleCharacter.loadCharFromDB(cid, this.c, true);
			this.c.setPlayer(this.player);
		}

		catch (SQLException e)
		{
			log.error("Loading the char failed", e);
		}

		this.c.setAccID(this.player.getAccountID());
		boolean allowLogin = true;

		Channel channelServer = this.c.getChannelServer();
		synchronized (this)
		{
            World worldInterface = Server.getInstance().getWorld(0);

            for (String charName : this.c.loadCharacterNames(this.c.getWorld()))
            {
                if (worldInterface.isConnected(charName))
                {
                    //log.warn(MapleClient.getLogMessage(this.player, "Attempting to doublelogin with " + charName));
                    allowLogin = false;
                    break;
                }
            }

            c.updateLoginState(MapleClient.LOGIN_LOGGEDIN,c.getSessionIPAddress());
		}

		Channel cserv = c.getChannelServer();
		cserv.addPlayer(this.player);

		this.player.getMap().addPlayer(this.player);

        Collection<BuddylistEntry> buddies = this.player.getBuddylist().getBuddies();
        int buddyIds[] = this.player.getBuddylist().getBuddyIds();
        Server.getInstance().getWorld(0).loggedOn(this.player.getName(), this.player.getId(), c.getChannel(), buddyIds);

        this.player.changeMap(map, map.getPortal(0));

		this.spawned = true;
		this.map = this.player.getMapId();



		AbsoluteLifeMovement alm1 = new AbsoluteLifeMovement((byte)0, spawnPoint, 90, (byte) 6);
		AbsoluteLifeMovement alm2 = new AbsoluteLifeMovement((byte) 0, spawnPoint, 5,(byte) 6);
		AbsoluteLifeMovement alm3 = new AbsoluteLifeMovement((byte) 0, spawnPoint, 415,(byte) 4);

		List<LifeMovementFragment> lmf = new ArrayList<LifeMovementFragment>();
		lmf.add(new ChangeEquipSpecialAwesome((byte) 1));
		lmf.add(alm1);
		lmf.add(alm2);
		lmf.add(alm3);

        byte[] packet = MaplePacketCreator.movePlayer(SexBot.getCharacter(this).getId(), lmf);
        System.out.println("Spawning4");
        SexBot.getCharacter(this).getMap().broadcastMessage(SexBot.getCharacter(this), packet);
        System.out.println("Spawning5");
        SexBot.getCharacter(this).setPosition(spawnPoint);
        System.out.println("Spawning6");
        SexBot.getCharacter(this).setStance(4);
        System.out.println("Spawning7");
        SexBot.getCharacter(this).getMap().movePlayer(SexBot.getCharacter(this), SexBot.getCharacter(this).getPosition());
    }

    public boolean isRecording() {
        return recording;
    }

    public void setRecording(boolean recording) {
        this.recording = recording;
    }

    public void record(List<LifeMovementFragment> res2) {
        if(moves == null){moves = new ArrayList<List<LifeMovementFragment>>();}
        moves.add(res2);
    }
    public void resetRecords(){
        moves.clear();
    }

    public boolean hasRecording() {
        return moves.size() > 0;
    }

    public String getRecordingSize() {
        return String.valueOf(moves.size());
    }
    public void doMoves(){
        if(hasRecording() && !moving) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (List<LifeMovementFragment> res : moves) {
                            byte[] packet = MaplePacketCreator.movePlayer(getCharacter(SexBot.this).getId(), res);
                            getCharacter(SexBot.this).getMap().broadcastMessage(getCharacter(SexBot.this), packet, false);
                            updatePosition(res, getCharacter(SexBot.this), 0);
                            player.getMap().movePlayer(player, player.getPosition());
                            Thread.sleep(500);
                        }
                        moving = false;
                    } catch(Exception e){}
                    moving = false;
                }
            }).start();
        }
    }
    protected void updatePosition(List<LifeMovementFragment> movement, AnimatedMapleMapObject target, int yoffset) {
        for (LifeMovementFragment move : movement) {
            if (move instanceof LifeMovement) {
                if (move instanceof AbsoluteLifeMovement) {
                    Point position = move.getPosition();
                    position.y += yoffset;
                    target.setPosition(position);
                }
                target.setStance(((LifeMovement) move).getNewstate());
            }
        }
    }

    public List<List<LifeMovementFragment>> getRecords() {
        return moves;
    }

    public void setRecords(List<List<LifeMovementFragment>> moves) {
        this.moves = moves;
    }

    public boolean isMonitoringDrops() {
        return monitoringDrops;
    }

    public void setMonitoringDrops(boolean monitoringDrops) {
        this.monitoringDrops = monitoringDrops;
    }
}