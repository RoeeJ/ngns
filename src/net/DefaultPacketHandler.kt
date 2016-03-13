package net

import client.MapleClient
import org.bson.Document
import tools.MongoReporter
import tools.data.input.SeekableLittleEndianAccessor

/**
 * Created by cipher on 3/12/16.
 */
class DefaultPacketHandler : MaplePacketHandler {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: MapleClient, header: Int) {
        val doc = Document()
        doc.put("header",header);
        doc.put("packet",slea.read(slea.available().toInt()))
        MongoReporter.insertReport(doc,packet = true);
    }

    override fun validateState(c: MapleClient): Boolean {
        return true;
    }
}
