package tools;

import client.MapleCharacter;
import client.MapleClient;
import com.google.gson.Gson;
import net.server.Server;
import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Objects;

import tools.HelperClasses.*;

/**
 * Created by NROL on 5/9/2015.
 */
public class WSServer extends WebSocketServer {
    private Gson gson = new Gson();
    private SecureRandom random = new SecureRandom();
    public WSServer(int port) throws UnknownHostException {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        onOpen((WebSocketImpl)conn);
    }

    private void onOpen(WebSocketImpl conn) {
        String token = org.apache.commons.lang.RandomStringUtils.random(64+Randomizer.nextInt(196));
        conn.send(new Message("hello",token));
        conn.setToken(token);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        onMessage(((WebSocketImpl) conn), message);
    }

    @Override
    synchronized public void onMessage(WebSocketImpl conn, String _message) {
        try {
            Message message = gson.fromJson(_message, Message.class);
            if(message == null) {
                conn.send(new Message("invalid_object"));
                return;
            }
            String serverToken = conn.getToken();
            if(serverToken != null && serverToken.length() > 0) {
                if(serverToken.hashCode() != message.sessionToken.hashCode()) {
                    conn.mismatchCount++;
                    if(conn.mismatchCount >= 5) {
                        if(conn.getClient() != null) {
                            conn.getClient().disconnect(false, false);
                            conn.close();
                        }
                    }
                    if (conn.getClient() != null) {
                        conn.getClient().announce(MaplePacketCreator.serverNotice(6, String.format("Token mismatch! %d/5 mismatches till you get rekt",conn.mismatchCount)));
                    }
                return;
                } else {
                    conn.mismatchCount = 0;
                }
            }

            switch (message.type) {
                case "auth_response": {
                    HelperClasses.AuthMessage msg = gson.fromJson(_message, HelperClasses.AuthMessage.class);
                    if(msg.cid == 0) break;
                    MapleCharacter chr = Server.getInstance().getWorld(0).getPlayerStorage().getCharacterById(msg.cid);
                    MapleClient client = null;
                    if(chr != null) { client = chr.getClient();}
                    if(client != null) {
                        if(msg.sessionToken == null || msg.sessionToken.length()==0 || msg.sessionToken.hashCode() != conn.getToken().hashCode()) {
                            conn.send(new Message("invalid_token"));
                            return;
                        }
                        conn.setClient(client);
                        client.setWebSocket(conn);
                        conn.isAuthed = true;
                    }
                    break;
                }
                case "ping": {
                    if(!conn.isAuthed) {
                        conn.send(new Message("auth_request"));
                    }
                    else if(conn.tokenUses >= 15) {
                        String token = org.apache.commons.lang.RandomStringUtils.random(64+Randomizer.nextInt(196));
                        conn.setToken(token);
                        conn.send(new Message("change_token",token));
                    } else {
                        if(conn.getClient() != null){
                            conn.send(new Message("pong",serverToken));
                        } else {
                            conn.send(new Message("auth_request"));
                        }
                    }
                    if(conn.getClient() != null) {
                        conn.getClient().doHeartbeat();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            conn.send(new Message("invalid_object"));
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }
}


