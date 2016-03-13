package tools
import net.server.Server
import org.bson.Document
/**
 * Created by cipher on 3/12/16.
 */
object MongoReporter {
    fun insertReport(doc: Document, unique: Boolean = false, packet: Boolean = false): Unit {
        val collection = if(packet) Server.getInstance().packetCollection else Server.getInstance().logCollection;
        if(unique) {
            if(collection.count(doc).toInt() == 0) collection.insertOne(doc);
        } else {
            Server.getInstance().packetCollection.insertOne(doc)
        }
    }
}