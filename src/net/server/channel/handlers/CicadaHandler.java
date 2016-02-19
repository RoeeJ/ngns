package net.server.channel.handlers;

import net.AbstractMaplePacketHandler;
import net.MaplePacketHandler;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import client.MapleClient;

public class CicadaHandler extends AbstractMaplePacketHandler implements
		MaplePacketHandler {

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		Byte b1 = slea.readByte();
		Integer i1 = slea.readInt();
		c.getPlayer().dropMessage(String.format("b1=%d, i1=%d", b1,i1));
	}
}
