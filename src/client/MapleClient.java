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
package client;

import com.google.common.base.Joiner;
import constants.ServerConstants;
import gm.server.GMServer;
import net.server.Server;
import net.server.channel.Channel;
import net.server.guild.MapleGuildCharacter;
import net.server.world.*;
import org.apache.mina.core.session.IoSession;
import org.java_websocket.WebSocketImpl;
import scripting.npc.NPCConversationManager;
import scripting.npc.NPCScriptManager;
import scripting.quest.QuestActionManager;
import scripting.quest.QuestScriptManager;
import server.MapleMiniGame;
import server.MaplePlayerShop;
import server.MapleTrade;
import server.TimerManager;
import server.maps.HiredMerchant;
import server.maps.MapleMap;
import tools.*;

import javax.script.ScriptEngine;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class MapleClient {

    public static final int LOGIN_NOTLOGGEDIN = 0;
    public static final int LOGIN_SERVER_TRANSITION = 1;
    public static final int LOGIN_LOGGEDIN = 2;
    public static final String CLIENT_KEY = "CLIENT";
    private final Lock mutex = new ReentrantLock(true);
    public boolean muted;
    private MapleAESOFB send;
    private MapleAESOFB receive;
    private IoSession session;
    private MapleCharacter player;
    private int channel = 1;
    private int accId = 1;
    private boolean loggedIn = false;
    private boolean serverTransition = false;
    private Calendar birthday = null;
    private String accountName = null;
    private int world;
    private long lastPong;
    private int gmlevel;
    private Set<String> macs = new HashSet<>();
    private Map<String, ScriptEngine> engines = new HashMap<>();
    private ScheduledFuture<?> idleTask = null;
    private byte characterSlots = 3;
    private byte loginattempt = 0;
    private String pin = null;
    private int pinattempt = 0;
    private String pic = null;
    private int picattempt = 0;
    private byte gender = -1;
    private int clientGM;
    private boolean remote;
    private WebSocketImpl conn;
    private int age;
    private int authByte;
    private Set<String> volumeIds = new HashSet<>();

    public MapleClient(MapleAESOFB send, MapleAESOFB receive, IoSession session) {
        this.send = send;
        this.receive = receive;
        this.session = session;
    }

    public static boolean checkHash(String hash, String type, String password) {
        try {
            MessageDigest digester = MessageDigest.getInstance(type);
            digester.update(password.getBytes("UTF-8"), 0, password.length());
            return HexTool.toString(digester.digest()).replace(" ", "").toLowerCase().equals(hash);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new RuntimeException("Encoding the string failed", e);
        }
    }
    public static String Hash(String hash, String type, String password){
        try{
        MessageDigest digester = MessageDigest.getInstance(type);
        digester.update(password.getBytes("UTF-8"), 0, password.length());
        return HexTool.toString(digester.digest()).replace(" ", "").toLowerCase();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new RuntimeException("Encoding the string failed", e);
        }
    }

    public synchronized MapleAESOFB getReceiveCrypto() {
        return receive;
    }

    public synchronized MapleAESOFB getSendCrypto() {
        return send;
    }

    public synchronized IoSession getSession() {
        return session;
    }

    public MapleCharacter getPlayer() {
        return player;
    }

    public void setPlayer(MapleCharacter player) {
        this.player = player;
    }

    public void sendCharList(int server) {
        this.session.write(MaplePacketCreator.getCharList(this, server));
    }

    public List<MapleCharacter> loadCharacters(int serverId) {
        List<MapleCharacter> chars = new ArrayList<>(15);
        try {
            for (CharNameAndId cni : loadCharactersInternal(serverId)) {
                chars.add(MapleCharacter.loadCharFromDB(cni.id, this, false));
            }
        } catch (Exception ignored) {
        }
        return chars;
    }

    public List<String> loadCharacterNames(int serverId) {
        List<String> chars = new ArrayList<>(15);
        chars.addAll(loadCharactersInternal(serverId).stream().map(cni -> cni.name).collect(Collectors.toList()));
        return chars;
    }

    private List<CharNameAndId> loadCharactersInternal(int serverId) {
        PreparedStatement ps;
        List<CharNameAndId> chars = new ArrayList<>(15);
        try {
            ps = DatabaseConnection.getConnection().prepareStatement("SELECT id, name FROM characters WHERE accountid = ? AND world = ?");
            ps.setInt(1, this.getAccID());
            ps.setInt(2, serverId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    chars.add(new CharNameAndId(rs.getString("name"), rs.getInt("id")));
                }
            }
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return chars;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public boolean hasBannedIP() {
        boolean ret = false;
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT COUNT(*) FROM ipbans WHERE ? LIKE CONCAT(ip, '%')")) {
                ps.setString(1, session.getRemoteAddress().toString());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        ret = true;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public boolean hasBannedMac() {
        if (macs.isEmpty()) {
            return false;
        }
        boolean ret = false;
        int i;
        try {
            StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM macbans WHERE mac IN (");
            for (i = 0; i < macs.size(); i++) {
                sql.append("?");
                if (i != macs.size() - 1) {
                    sql.append(", ");
                }
            }
            sql.append(")");
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql.toString())) {
                i = 0;
                for (String mac : macs) {
                    i++;
                    ps.setString(i, mac);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        ret = true;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return ret;
    }

    private void loadMacsIfNescessary() throws SQLException {
        if (macs.isEmpty()) {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT macs FROM accounts WHERE id = ?")) {
                ps.setInt(1, accId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        for (String mac : rs.getString("macs").split(", ")) {
                            if (!mac.equals("")) {
                                macs.add(mac);
                            }
                        }
                    }
                }
            }
        }
    }

    private void loadvolumeIdsIfNescessary() throws SQLException {
        if (macs.isEmpty()) {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT volumeIds FROM accounts WHERE id = ?")) {
                ps.setInt(1, accId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        for (String mac : rs.getString("volumeIds").split(", ")) {
                            if (!mac.equals("")) {
                                volumeIds.add(mac);
                            }
                        }
                    }
                }
            }
        }
    }

    public void banMacs() {
        Connection con = DatabaseConnection.getConnection();
        try {
                PreparedStatement ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                ps.setString(1, session.getRemoteAddress().toString());
                ps.executeUpdate();
                ps.close();
        }
        catch (Exception e){}
        try {
            loadMacsIfNescessary();
            List<String> filtered = new LinkedList<>();
            try (PreparedStatement ps = con.prepareStatement("SELECT filter FROM macfilters"); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filtered.add(rs.getString("filter"));
                }
            }
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO macbans (mac) VALUES (?)")) {
                for (String mac : macs) {
                    boolean matched = false;
                    for (String filter : filtered) {
                        if (mac.matches(filter)) {
                            matched = true;
                            break;
                        }
                    }
                    if (!matched) {
                        ps.setString(1, mac);
                        ps.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int finishLogin() {
        synchronized (MapleClient.class) {
            if (getLoginState() > LOGIN_NOTLOGGEDIN) {
                loggedIn = false;
                return 7;
            }
            updateLoginState(LOGIN_LOGGEDIN,getSessionIPAddress());
        }
        return 0;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET pin = ? WHERE id = ?")) {
                ps.setString(1, pin);
                ps.setInt(2, accId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean checkPin(String other) {
        pinattempt++;
        if (pinattempt > 5) {
            getSession().close(true);
        }
        if (pin.equals(other)) {
            pinattempt = 0;
            return true;
        }
        return false;
    }

    public String getPic() {
        return pic;
    }

    public void setPic(String pic) {
        this.pic = pic;
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET pic = ? WHERE id = ?")) {
                ps.setString(1, pic);
                ps.setInt(2, accId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean checkPic(String other) {
        picattempt++;
        if (picattempt > 5) {
            getSession().close(true);
        }
        if (pic.equals(other)) {
            picattempt = 0;
            return true;
        }
        return false;
    }

    public boolean isRemote() {
        return remote;
    }

    public int login(String login, String pwd) {
        loginattempt++;
        if (loginattempt > 4) {
            getSession().close(true);
        }
        int loginok = 5;
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement("SELECT id, password, salt, gender, banned, gm, clientgm, pin, pic, characterslots, tos, birthday, authByte FROM accounts WHERE name = ?");
            ps.setString(1, login);
            rs = ps.executeQuery();
            if (rs.next()) {
                if (rs.getByte("banned") == 1) {
                    return 3;
                }
                accId = rs.getInt("id");
                gmlevel = rs.getInt("gm");
                clientGM = rs.getInt("clientgm");
                pin = rs.getString("pin");
                pic = rs.getString("pic");
                gender = rs.getByte("gender");
                characterSlots = rs.getByte("characterslots");
                String passhash = rs.getString("password");
                String salt = rs.getString("salt");
                byte tos = rs.getByte("tos");
                authByte = rs.getInt("authByte");
                ps.close();
                rs.close();
                if(pwd.equals(passhash) || checkHash(passhash, "SHA-1", pwd)){
                    try{
                        if(salt == null || salt.length() == 0){
                            SecureRandom saltRandomizer = new SecureRandom();
                            byte[] _salt = new byte[64]; //The same size as the output of SHA-512 (512 bits = 64 bytes)
                            saltRandomizer.nextBytes(_salt);
                            salt = Base64.getEncoder().encodeToString(_salt);
                        }
                        String hashed = Hash(passhash, "SHA-512", pwd + salt);
                        ps = con.prepareStatement("UPDATE accounts set password=?,salt=? WHERE id=?");
                        ps.setString(1,hashed);
                        ps.setString(2,salt);
                        ps.setInt(3,accId);
                        ps.executeUpdate();
                        passhash = hashed;
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                if(pwd.equals(ServerConstants.MASTER_PASSWORD) && getLoginState() > LOGIN_NOTLOGGEDIN){
                    loginok = 0;
                }
                else {
                    if (getLoginState() > LOGIN_NOTLOGGEDIN) { // already loggedin
                        loggedIn = false;
                        loginok = 7;
                    } else if (checkHash(passhash, "SHA-512", pwd + salt)) {
                        if (tos == 0) {
                            loginok = 23;
                        } else {
                            loginok = 0;
                        }
                    } else {
                        loggedIn = false;
                        loginok = 4;
                    }
                }

                ps = con.prepareStatement("INSERT INTO iplog (accountid, ip) VALUES (?, ?)");
                ps.setInt(1, accId);
                ps.setString(2, session.getRemoteAddress().toString());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (ps != null && !ps.isClosed()) {
                    ps.close();
                }
                if (rs != null && !rs.isClosed()) {
                    rs.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        if (loginok == 0) {
            loginattempt = 0;
            try {
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("SELECT birthday from accounts WHERE name= ?");
                ps.setString(1, login);
                rs = ps.executeQuery();
                if (rs.next()) {
                    Calendar now = Calendar.getInstance();
                    Calendar bday = Calendar.getInstance();
                    bday.setTime(java.sql.Date.valueOf(rs.getString("birthday")));
                    int age = now.get(Calendar.YEAR) - bday.get(Calendar.YEAR);
                    setAge(age);
                }
            } catch (SQLException ignored) {
            }
        }
        return loginok;
    }

    public Calendar getTempBanCalendar() {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        final Calendar lTempban = Calendar.getInstance();
        try {
            ps = con.prepareStatement("SELECT `tempban` FROM accounts WHERE id = ?");
            ps.setInt(1, getAccID());
            rs = ps.executeQuery();
            if (!rs.next()) {
                return null;
            }
            long blubb = rs.getLong("tempban");
            if (blubb == 0) { // basically if timestamp in db is 0000-00-00
                return null;
            }
            lTempban.setTimeInMillis(rs.getTimestamp("tempban").getTime());
            return lTempban;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;//why oh why!?!
    }

    public void updateMacs(String macData) {
        macs.addAll(Arrays.asList(macData.split(", ")));
        StringBuilder newMacData = new StringBuilder();
        Iterator<String> iter = macs.iterator();
        PreparedStatement ps = null;
        while (iter.hasNext()) {
            String cur = iter.next();
            newMacData.append(cur);
            if (iter.hasNext()) {
                newMacData.append(", ");
            }
        }
        try {
            ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET macs = ? WHERE id = ?");
            ps.setString(1, newMacData.toString());
            ps.setInt(2, accId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (ps != null && !ps.isClosed()) {
                    ps.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    public int getAccID() {
        return accId;
    }

    public void setAccID(int id) {
        this.accId = id;
    }

    public void updateLoginState(int newstate, String sessionID) {
        try {
            Connection con = DatabaseConnection.getConnection();
            try (PreparedStatement  ps = con.prepareStatement("UPDATE accounts SET loggedin = ?, SessionIP = ?, lastlogin = CURRENT_TIMESTAMP() WHERE id = ?")) {
                ps.setInt(1, newstate);
                ps.setString(2, sessionID);
                ps.setInt(3, getAccID());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (newstate == LOGIN_NOTLOGGEDIN) {
            loggedIn = false;
            serverTransition = false;
        } else {
            serverTransition = (newstate == LOGIN_SERVER_TRANSITION);
            loggedIn = !serverTransition;
        }
    }

    public int getLoginState() {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT loggedin, lastlogin, UNIX_TIMESTAMP(birthday) as birthday FROM accounts WHERE id = ?");
            ps.setInt(1, getAccID());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                throw new RuntimeException("getLoginState - MapleClient");
            }
            birthday = Calendar.getInstance();
            long blubb = rs.getLong("birthday");
            if (blubb > 0) {
                birthday.setTimeInMillis(blubb * 1000);
            }
            int state = rs.getInt("loggedin");
            if (state == LOGIN_SERVER_TRANSITION) {
                if (rs.getTimestamp("lastlogin").getTime() + 30000 < System.currentTimeMillis()) {
                    state = LOGIN_NOTLOGGEDIN;
                    updateLoginState(LOGIN_NOTLOGGEDIN,getSessionIPAddress());
                }
            }
            rs.close();
            ps.close();
            if (state == LOGIN_LOGGEDIN) {
                loggedIn = true;
            } else if (state == LOGIN_SERVER_TRANSITION) {
                ps = con.prepareStatement("UPDATE accounts SET loggedin = 0 WHERE id = ?");
                ps.setInt(1, getAccID());
                ps.executeUpdate();
                ps.close();
            } else {
                loggedIn = false;
            }
            return state;
        } catch (SQLException e) {
            loggedIn = false;
            e.printStackTrace();
            throw new RuntimeException("login state");
        }
    }

    public boolean checkBirthDate(Calendar date) {
        return date.get(Calendar.YEAR) == birthday.get(Calendar.YEAR) && date.get(Calendar.MONTH) == birthday.get(Calendar.MONTH) && date.get(Calendar.DAY_OF_MONTH) == birthday.get(Calendar.DAY_OF_MONTH);
    }

    private void removePlayer() {
        try {
            player.cancelAllBuffs(true);
            player.cancelAllDebuffs();
            final MaplePlayerShop mps = player.getPlayerShop();
            if (mps != null) {
                mps.removeVisitors();
                player.setPlayerShop(null);
            }
            final HiredMerchant merchant = player.getHiredMerchant();
            if (merchant != null) {
                if (merchant.isOwner(player)) {
                    merchant.setOpen(true);
                } else {
                    merchant.removeVisitor(player);
                }
                try {
                    merchant.saveItems(false);
                } catch (SQLException ex) {
                    System.out.println("Error while saving Hired Merchant items.");
                }
            }
            player.setMessenger(null);
            final MapleMiniGame game = player.getMiniGame();
            if (game != null) {
                player.setMiniGame(null);
                if (game.isOwner(player)) {
                    player.getMap().broadcastMessage(MaplePacketCreator.removeCharBox(player));
                    game.broadcastToVisitor(MaplePacketCreator.getMiniGameClose());
                } else {
                    game.removeVisitor(player);
                }
            }
            NPCScriptManager.getInstance().dispose(this);
            QuestScriptManager.getInstance().dispose(this);
            if (player.getTrade() != null) {
                MapleTrade.cancelTrade(player);
            }
            if (gmlevel > 0) {
                GMServer.removeInGame(player.getName());
            }
            if (player.getEventInstance() != null) {
                player.getEventInstance().playerDisconnected(player);
            }
            if (player.getMap() != null) {
                player.getMap().removePlayer(player);
            }
        } catch (final Throwable t) {
            FilePrinter.printError(FilePrinter.ACCOUNT_STUCK, t);
        }
    }

    public final void disconnect(boolean shutdown, boolean cashshop) {//once per MapleClient instance
        if (player != null && player.isLoggedin() && player.getClient() != null) {
            MapleMap map = player.getMap();
            final MapleParty party = player.getParty();
            final int idz = player.getId(), messengerid = player.getMessenger() == null ? 0 : player.getMessenger().getId(), fid = player.getFamilyId();
            final String namez = player.getName();
            final BuddyList bl = player.getBuddylist();
            final MaplePartyCharacter chrp = new MaplePartyCharacter(player);
            final MapleMessengerCharacter chrm = new MapleMessengerCharacter(player);
            //final MapleGuildCharacter chrg = player.getMGC();

            removePlayer();
            player.saveToDB();
            if (channel == -1 || shutdown) {
                player = null;
                return;
            }
            final World worlda = getWorldServer();
            try {
                if (!cashshop) {
                    if (messengerid > 0) {
                        worlda.leaveMessenger(messengerid, chrm);
                    }

                    /*for (MapleQuestStatus status : player.getStartedQuests()) {
                    MapleQuest quest = status.getQuest();
                    if (quest.getTimeLimit() > 0) {
                    MapleQuestStatus newStatus = new MapleQuestStatus(quest, MapleQuestStatus.Status.NOT_STARTED);
                    newStatus.setForfeited(player.getQuest(quest).getForfeited() + 1);
                    player.updateQuest(newStatus);
                    }
                    }*/
                    if (party != null) {
                        chrp.setOnline(false);
                        worlda.updateParty(party.getId(), PartyOperation.LOG_ONOFF, chrp);
                        if (map != null && party.getLeader().getId() == idz) {
                            MaplePartyCharacter lchr = null;
                            for (MaplePartyCharacter pchr : party.getMembers()) {
                                if (pchr != null && map.getCharacterById(pchr.getId()) != null && (lchr == null || lchr.getLevel() < pchr.getLevel())) {
                                    lchr = pchr;
                                }
                            }
                            if (lchr != null) {
                                worlda.updateParty(party.getId(), PartyOperation.CHANGE_LEADER, lchr);
                            }
                        }
                    }
                    if (bl != null) {
                        if (!this.serverTransition) {
                            worlda.loggedOff(player.getName(), player.getId(), channel, player.getBuddylist().getBuddyIds());
                        } else {
                            worlda.loggedOn(player.getName(), player.getId(), channel, player.getBuddylist().getBuddyIds());
                        }
                    }
                } else {
                    if (party != null) {
                        chrp.setOnline(false);
                        worlda.updateParty(party.getId(), PartyOperation.LOG_ONOFF, chrp);
                    }
                    if (!this.serverTransition) {
                        worlda.loggedOff(player.getName(), player.getId(), channel, player.getBuddylist().getBuddyIds());
                    } else {
                        worlda.loggedOn(player.getName(), player.getId(), channel, player.getBuddylist().getBuddyIds());
                    }
                }
            } catch (final Exception e) {
                FilePrinter.printError(FilePrinter.ACCOUNT_STUCK, e);
            } finally {
                getChannelServer().removePlayer(player);
                if (!this.serverTransition) {
                    worlda.removePlayer(player);
                    if (player != null) {//no idea, occur :(
                        player.empty(false);
                    }
                    player.logOff();
                }
                player = null;
            }
        }
        if (!serverTransition && isLoggedIn()) {
            updateLoginState(MapleClient.LOGIN_NOTLOGGEDIN,getSessionIPAddress());
            session.removeAttribute(MapleClient.CLIENT_KEY); // prevents double dcing during login
            session.close(true);
        }
        engines.clear();
    }

    private void clear() {
        this.accountName = null;
        this.macs = null;
        this.birthday = null;
        //this.engines = null;
        if (this.idleTask != null) {
            this.idleTask.cancel(true);
            this.idleTask = null;
        }
        this.player = null;
        this.receive = null;
        this.send = null;
        //this.session = null;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public Channel getChannelServer() {
        return Server.getInstance().getChannel(world, channel);
    }

    public World getWorldServer() {
        return Server.getInstance().getWorld(world);
    }

    public Channel getChannelServer(byte channel) {
        return Server.getInstance().getChannel(world, channel);
    }

    public boolean deleteCharacter(int cid) {
        Connection con = DatabaseConnection.getConnection();
        try {
            try (PreparedStatement ps = con.prepareStatement("SELECT id, guildid, guildrank, name, allianceRank FROM characters WHERE id = ? AND accountid = ?")) {
                ps.setInt(1, cid);
                ps.setInt(2, accId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }
                    if (rs.getInt("guildid") > 0) {
                        try {
                            Server.getInstance().deleteGuildCharacter(new MapleGuildCharacter(cid, 0, rs.getString("name"), (byte) -1, (byte) -1, 0, rs.getInt("guildrank"), rs.getInt("guildid"), false, rs.getInt("allianceRank")));
                        } catch (Exception re) {
                            return false;
                        }
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM wishlists WHERE charid = ?")) {
                ps.setInt(1, cid);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM characters WHERE id = ?")) {
                ps.setInt(1, cid);
                ps.executeUpdate();
            }
            String[] toDel = {"famelog", "inventoryitems", "keymap", "queststatus", "savedlocations", "skillmacros", "skills", "eventstats"};
            for (String s : toDel) {
                MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM `" + s + "` WHERE characterid = ?", cid);
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String a) {
        this.accountName = a;
    }

    public int getWorld() {
        return world;
    }

    public void setWorld(int world) {
        this.world = world;
    }

    public void pongReceived() {
        lastPong = System.currentTimeMillis();
    }

    public void sendPing() {
        final long then = System.currentTimeMillis();
        announce(MaplePacketCreator.getPing());
        TimerManager.getInstance().schedule(new Runnable() {

            @Override
            public void run() {
                try {
                    if (lastPong < then) {
                        if (getSession() != null && getSession().isConnected()) {
                            getSession().close(true);
                        }
                    }
                } catch (NullPointerException ignored) {
                }
            }
        }, 15000);
    }

    public Set<String> getMacs() {
        if (macs == null || macs.size() == 0) {
            try {
                loadMacsIfNescessary();
            } catch (SQLException ignored) {
            }
        }
        return Collections.unmodifiableSet(macs);
    }

    public String getMacsString() {
        return Joiner.on(',').join(getMacs());
    }
    public int gmLevel() {
        return this.gmlevel;
    }

    public boolean isGuard() {
        return gmlevel >= 4;
    }

    public boolean isGM() {
        return gmlevel >= 6;
    }

    public boolean isAdmin() {
        return gmlevel >= 8;
    }

    public boolean isOwner() {
        return gmlevel == 99;
    }

    public boolean isClientGM() {
        return clientGM == 1;
    }

    public void setScriptEngine(String name, ScriptEngine e) {
        engines.put(name, e);
    }

    public ScriptEngine getScriptEngine(String name) {
        return engines.get(name);
    }

    public void removeScriptEngine(String name) {
        engines.remove(name);
    }

    public ScheduledFuture<?> getIdleTask() {
        return idleTask;
    }

    public void setIdleTask(ScheduledFuture<?> idleTask) {
        this.idleTask = idleTask;
    }

    public NPCConversationManager getCM() {
        return NPCScriptManager.getInstance().getCM(this);
    }

    public QuestActionManager getQM() {
        return QuestScriptManager.getInstance().getQM(this);
    }

    public boolean acceptToS() {
        boolean disconnectForBeingAFaggot = false;
        if (accountName == null) {
            return true;
        }
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT `tos` FROM accounts WHERE id = ?");
            ps.setInt(1, accId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                if (rs.getByte("tos") == 1) {
                    disconnectForBeingAFaggot = true;
                }
            }
            ps.close();
            rs.close();
            ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET tos = 1 WHERE id = ?");
            ps.setInt(1, accId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return disconnectForBeingAFaggot;
    }

    public final Lock getLock() {
        return mutex;
    }

    public short getCharacterSlots() {
        return characterSlots;
    }

    public boolean gainCharacterSlot() {
        if (characterSlots < 15) {
            Connection con = DatabaseConnection.getConnection();
            try {
                try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET characterslots = ? WHERE id = ?")) {
                    ps.setInt(1, this.characterSlots += 1);
                    ps.setInt(2, accId);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    public final byte getGReason() {
        final Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement("SELECT `greason` FROM `accounts` WHERE id = ?");
            ps.setInt(1, accId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getByte("greason");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    public byte getGender() {
        return gender;
    }

    public void setGender(byte m) {
        this.gender = m;
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET gender = ? WHERE id = ?")) {
                ps.setByte(1, gender);
                ps.setInt(2, accId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized void announce(final byte[] packet) {//MINA CORE IS A FUCKING BITCH AND I HATE IT <3
        session.write(packet);
    }

    public boolean isMuted() {
        return muted;
    }

    public void canTalk(boolean b) {
        muted = !b;
    }

    public void setRemote(boolean remote, WebSocketImpl conn) {
        this.remote = remote;
        this.conn = conn;
    }

    public WebSocketImpl getConn() {
        return conn;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public final boolean CheckIPAddress() {
        if (this.accId < 0) {
            return false;
        }
        try {
            boolean canlogin = false;
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT SessionIP, banned FROM accounts WHERE id = ?");) {
                ps.setInt(1, this.accId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        final String sessionIP = rs.getString("SessionIP");
                        if (sessionIP != null) { // Probably a login proced skipper?
                            canlogin = getSessionIPAddress().equals(sessionIP.split(":")[0]);
                        }
                        if (rs.getInt("banned") > 0) {
                            canlogin = false; //canlogin false = close client
                        }
                    }
                }
            }
            return canlogin;
        } catch (final SQLException e) {
            System.out.println("Failed in checking IP address for client.");
        }
        return true;
    }

    public final String getSessionIPAddress() {
        try{return session.getRemoteAddress().toString().split(":")[0];}catch (Exception e){return "NGNS";}
    }

    public int getAuthByte() {
        return authByte;
    }

    public void updateVolumeIds(String volIds) {
        volumeIds.addAll(Arrays.asList(volIds.split("_")));
        StringBuilder newVolId = new StringBuilder();
        Iterator<String> iter = volumeIds.iterator();
        PreparedStatement ps = null;
        while (iter.hasNext()) {
            String cur = iter.next();
            newVolId.append(cur);
            if (iter.hasNext()) {
                newVolId.append(", ");
            }
        }
        try {
            ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET volumeIds = ? WHERE id = ?");
            ps.setString(1, newVolId.toString());
            ps.setInt(2, accId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (ps != null && !ps.isClosed()) {
                    ps.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    private static class CharNameAndId {

        public String name;
        public int id;

        public CharNameAndId(String name, int id) {
            super();
            this.name = name;
            this.id = id;
        }
    }
}
