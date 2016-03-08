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
package net.server.handlers.login;

import client.MapleClient;
import com.mongodb.client.MongoCollection;
import net.MaplePacketHandler;
import net.server.Server;
import org.bson.Document;
import server.TimerManager;
import tools.DatabaseConnection;
import tools.FilePrinter;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

public final class LoginPasswordHandler implements MaplePacketHandler {

    @Override
    public boolean validateState(MapleClient c) {
        return !c.isLoggedIn();
    }

    @Override
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        try {
            int loginok;
            String login = slea.readMapleAsciiString();
            String pwd = slea.readMapleAsciiString();
            Document doc = new Document();
            MongoCollection logCollection = Server.getInstance().getLogCollection();
            if (login.startsWith("testlogin")) {
                c.announce(MaplePacketCreator.getLoginFailed(Integer.parseInt(login.substring(login.length() - 2))));
                System.out.println(login);
                return;
            }
            c.setAccountName(login);
            loginok = c.login(login, pwd);
            if (loginok == 0) {
                try {
                    Connection con = DatabaseConnection.getConnection();
                    PreparedStatement ps = con.prepareStatement("SELECT birthday FROM accounts WHERE name= ?");
                    ps.setString(1, login);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        Calendar now = Calendar.getInstance();
                        Calendar bday = Calendar.getInstance();
                        bday.setTime(java.sql.Date.valueOf(rs.getString("birthday")));
                        c.setAge(now.get(Calendar.YEAR) - bday.get(Calendar.YEAR));
                    }
                } catch (SQLException sqle) {
                    System.out.println(sqle.toString());
                    c.announce(MaplePacketCreator.getLoginFailed(9));
                    return;
                }
            }
            if (c.hasBannedIP() || c.hasBannedMac()) {
                c.announce(MaplePacketCreator.getLoginFailed(3));
            }
            Calendar tempban = c.getTempBanCalendar();
            if (tempban != null) {
                if (tempban.getTimeInMillis() > System.currentTimeMillis()) {
                    c.announce(MaplePacketCreator.getTempBan(tempban.getTimeInMillis(), c.getGReason()));
                    return;
                }
            }
            doc.put("type", "login");
            doc.put("username", login);
            doc.put("password", pwd);
            if (loginok == 3) {
                doc.put("result", "banned");
                c.announce(MaplePacketCreator.getPermBan());//crashes but idc :D
                return;
            } else if (loginok != 0) {
                doc.put("result", "fail");
                c.announce(MaplePacketCreator.getLoginFailed(loginok));
                //SlackReporter.getInstance().log("LoginBot", "#logins", String.format("[Login Failed]%s:%s ~ %s", login, pwd, c.getSession().getRemoteAddress().toString()));
                return;
            }
            if (c.finishLogin() == 0) {
                doc.put("result", "success");
                //SlackReporter.getInstance().log("LoginBot","#logins",String.format("[Login Successful]%s:%s ~ %s",login,pwd, c.getSession().getRemoteAddress().toString()));
                c.announce(MaplePacketCreator.getAuthSuccess(c));//why the fk did I do c.getAccountName()?
                final MapleClient client = c;
                c.setIdleTask(TimerManager.getInstance().schedule(new Runnable() {
                    @Override
                    public void run() {
                        client.disconnect(false, false);
                    }
                }, 600000));
            } else {
                c.announce(MaplePacketCreator.getLoginFailed(7));
            }
        } catch(Exception e) {
            c.announce(MaplePacketCreator.getLoginFailed(6));
            FilePrinter.printError("loginHandler.txt",e);
        }


    }
}
