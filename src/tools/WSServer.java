package tools;

import client.command.CommandProcessor;
import net.server.Server;
import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.sql.SQLException;

/**
 * Created by NROL on 5/9/2015.
 */
public class WSServer extends WebSocketServer {
    public WSServer() throws UnknownHostException {

    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        onMessage(((WebSocketImpl) conn), message);
    }

    @Override
    public void onMessage(WebSocketImpl conn, String message) {
        if (message.startsWith("~LOGIN")) {
            System.out.print("Authenticating user with " + message.split(" ")[1]);
            if (conn.auth(message.split(" ")[1])) {
                conn.send("Authentication successfull!");
                return;
            }
            conn.send("Failed authenticating.");
            return;
        }
        if (message.startsWith("~")) {
            if (conn.isAuthed()) {
                try {
                    CommandProcessor.processCommand(conn.getClient(), message, '!');
                } catch (RemoteException | SQLException e) {
                    e.printStackTrace();
                    conn.send("Exception thrown! " + e.toString());
                }
            } else {
                conn.send(String.valueOf(conn.isAuthed()));
            }
        }
        if (message.startsWith("#") && conn.isAuthed()) {
            Server.getInstance().broadcastMessage(0, MaplePacketCreator.serverNotice(0, message.substring(1)));
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }
}
