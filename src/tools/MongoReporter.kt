package tools
import net.server.Server
import org.bson.Document
import java.util.*

/**
 * Created by cipher on 3/12/16.
 */
object MongoReporter {
    fun insertReport(doc: Document, unique: Boolean = false, packet: Boolean = false): Unit {
        val collection = if(packet) Server.getInstance().packetCollection else Server.getInstance().logCollection;
        if(unique) {
            if(collection.count(doc).toInt() == 0) collection.insertOne(doc.withTimestamp());
        } else {
            Server.getInstance().packetCollection.insertOne(doc.withTimestamp())
        }
    }
    fun insertReport(map: Map<String, Any>, unique: Boolean = false, packet: Boolean = false): Unit {
        insertReport(Document(map),unique,packet)
    }
    fun insertReport(doc: Document): Unit {
        insertReport(doc,false,false);
    }
    fun Document.withTimestamp(): Document {
        this.put("createdAt",Calendar.getInstance().timeInMillis);
        return this;
    }
}