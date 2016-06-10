package net.server;

import client.MapleCharacter;
import client.MapleClient;

import java.util.Calendar;

/**
 * Created by cipher on 22/05/2016.
 */
public class HeartbeatWorker implements Runnable {
    @Override
    public void run() {
        Server.getInstance().getWorlds().parallelStream().forEach((world) -> {
            world.getPlayerStorage().getAllCharacters().parallelStream().map(MapleCharacter::getClient).filter((client) -> client.getLoginState() == MapleClient.LOGIN_LOGGEDIN).forEach((client) -> {
                if(client.getLastHeartbeat() == -1 && ++client.failedHeartbeatCount > 2) {
                    client.disconnect(false,false);
                    client.getSession().close(true);
                }
                if(client.getPlayer() != null && client.getLastHeartbeat() > 0) {
                    client.getPlayer().dropMessage(String.format("It has been %dms since your last heartbeat", Calendar.getInstance().getTimeInMillis()-client.getLastHeartbeat()));
                }
            });
        });
    }
}
