package net.server.handlers.login;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.server.Server;
import server.MegatronListener;
import tools.MaplePacketCreator;
import tools.StringUtil;
import tools.data.input.SeekableLittleEndianAccessor;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class CharSelectedWithPicHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {

        String pic = slea.readMapleAsciiString();
        int charId = slea.readInt();
        String macs = slea.readMapleAsciiString();
        c.updateMacs(macs);

        if (c.hasBannedMac()) {
            c.getSession().close(true);
            return;
        }
        //if (c.checkPic(pic)) {
            if (c.getIdleTask() != null) {
                c.getIdleTask().cancel(true);
            }
            c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION,c.getSessionIPAddress());

            String[] socket = Server.getInstance().getIP(c.getWorld(), c.getChannel()).split(":");
            try {
                c.announce(MaplePacketCreator.getServerIP(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]), charId));
            } catch (UnknownHostException | NumberFormatException e) {
                MegatronListener.getInstance().log("decepticons", StringUtil.exceptionStacktraceToString(e));
            }
       // } else {
         //   c.announce(MaplePacketCreator.wrongPic());
        //}
    }
}