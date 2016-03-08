package server;

import client.MapleCharacter;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackMessage;
import com.ullink.slack.simpleslackapi.SlackMessageListener;
import com.ullink.slack.simpleslackapi.SlackSession;
import net.server.Server;
import net.server.channel.Channel;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import tools.MaplePacketCreator;

public class MegatronListener implements SlackMessageListener {
	
	private static MegatronListener instance;
	private static SlackSession session;
	private static Server server;
	
	public static SlackSession getSession() {
		return session;
	}
	
	public static Server getServer() {
		return Server.getInstance();
	}
	
	public static MegatronListener getInstance() {
		if(instance == null){
			instance = new MegatronListener();
		}
		return instance;
	}

    protected static String joinStringFrom(String arr[], int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < arr.length; i++) {
            builder.append(arr[i]);
            if (i != arr.length - 1) {
                builder.append(" ");
            }
        }
        return builder.toString();
    }

    private void reply(SlackMessage message, String s) {
        getSession().sendMessageOverWebSocket(message.getChannel(), s, null);
    }

    @Override
    public void onSessionLoad(SlackSession session) {
        MegatronListener.session = session;
        //session.sendMessageOverWebSocket(getSession().findChannelByName("decepticons"), "Megatron reporting for duty!", null);
        //System.out.println("Megatron is now online.");
    }

    @Override
    public void onMessage(SlackMessage message) {
        if(message.getSender().getUserName().equals("megatron")){return;}
        String[] splitted = message.getMessageContent().split(" ");
        switch (splitted[0].toLowerCase()) {
            case "dump": {
                if (splitted.length >= 2) {
                    SlackChannel channel = getChannelByName(splitted[1]);
                }
                break;
            }
            case "notice":
            case "n":
            {
                getServer().broadcastMessage(0, MaplePacketCreator.serverNotice(6, joinStringFrom(splitted, 1)));
                break;
            }
            case "echo":
            {
            	reply(message,message.getMessageContent());
            	break;
            }
            case "online": {
                for (Channel ch : getServer().getChannelsFromWorld(0)) {
                    String s = "Characters online (Channel " + ch.getId() + " Online: " + ch.getPlayerStorage().getAllCharacters().size() + ") : ";
                    if (ch.getPlayerStorage().getAllCharacters().size() < 50) {
                        for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                            s += MapleCharacter.makeMapleReadable(chr.getName()) + ", ";
                        }
                        reply(message, s.substring(0));
                    }
                }
                break;
            }
            case "status":
            {
                Server server = Server.getInstance();

                int totalConnectedClients = 0;
                for (Channel channel : server.getAllChannels()) {
                    totalConnectedClients += channel.getConnectedClients();
                    reply(message,"Channel " + channel.getId() + " has " + channel.getConnectedClients() + " players.");
                }

                reply(message,"Total amount of players: " + totalConnectedClients);
                reply(message,"-------------------------------------------------");

                int totalMaps = 0;
                for (Channel channel : server.getAllChannels()) {
                    MapleMapFactory mapFactory = channel.getMapFactory();

                    totalMaps += mapFactory.getMaps().size();
                    reply(message,"Channel " + channel.getId() + " has " + mapFactory.getMaps().size() + " maps.");
                }

                reply(message,"Total amount of maps: " + totalMaps);
                reply(message,"-------------------------------------------------");

                int totalMapObjects = 0;
                for (Channel channel : server.getAllChannels()) {
                    MapleMapFactory mapFactory = channel.getMapFactory();

                    int currentChannelObjects = 0;
                    for (MapleMap map : mapFactory.getMaps().values()) {
                        currentChannelObjects += map.getMapObjects().size();
                    }

                    totalMapObjects += currentChannelObjects;
                    reply(message,"Channel " + channel.getId() + " has " + currentChannelObjects + " map objects.");
                }

                reply(message,"Total amount of map objects: " + totalMapObjects);

                break;
            }
            case "autodeploy":
            {
            	if(!message.getChannel().getName().equals("autodeploy")){
            		reply(message,"nuh uh, you've been reported to NROL!");
            		getSession().sendMessageOverWebSocket(getSession().findChannelByName("thor"), message.getSender().getUserName() + " Has tried to trigger autodeploy!", null);
            		break;
            	}
            	Server.getInstance().broadcastMessage(0, MaplePacketCreator.serverNotice(1, "Server will restart in 1 minute"));
            	reply(message,"kk, willdo!");
            	new Thread(new Runnable() {

                    @Override
					public void run() {
						try{
						Thread.sleep(60000);
                            new Thread(Server.getInstance().shutdown(false)).start();
                        } catch(Exception e){}
					}
                }).start();
                break;
            }
            case "dc":
            case "disconnect": {
                MapleCharacter victim = getServer().getWorld(0).getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim != null) {
                    victim.getClient().getSession().close(true);
                } else {
                    reply(message, String.format("%s is not a valid target", splitted[1]));
                }
                break;
            }
            case "charinfo": {
                StringBuilder builder = new StringBuilder();
                if (splitted[1] != null) {
                    MapleCharacter other = getServer().getChannel(0, 1).getPlayerStorage().getCharacterByName(splitted[1]);
                    builder.append(other.getName());
                    builder.append(" is at ");
                    builder.append(other.getPosition().x);
                    builder.append("/");
                    builder.append(other.getPosition().y);
                    builder.append(" ");
                    builder.append(other.getHp());
                    builder.append("/");
                    builder.append(other.getCurrentMaxHp());
                    builder.append("hp ");
                    builder.append(other.getMp());
                    builder.append("/");
                    builder.append(other.getCurrentMaxMp());
                    builder.append("mp ");
                    builder.append(other.getExp());
                    builder.append("exp hasParty: ");
                    builder.append(other.getParty() != null);
                    builder.append(" hasTrade: ");
                    builder.append(other.getTrade() != null);
                    builder.append(" remoteAddress: ");
                    builder.append(other.getClient().getSession().getRemoteAddress());
                    reply(message, builder.toString());
                }
                break;
            }
            default:
            {
            	getSession().sendMessageOverWebSocket(getSession().findChannelByName("autodeploy"), "test",null);
            	break;
            }
        }
    }

	private SlackChannel getChannelByName(String name){
		return getSession().findChannelByName(name);
	}
}
