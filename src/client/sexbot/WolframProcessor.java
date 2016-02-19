package client.sexbot;

import client.MapleCharacter;
import com.wolfram.alpha.*;
import net.server.Server;
import tools.FilePrinter;
import tools.MaplePacketCreator;
import tools.Randomizer;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by NROL on 3/21/2015.
 */
public class WolframProcessor {
    private static List<String> answerTypes = Arrays.asList("BasicInformation", "Result", "Age", "DecimalApproximation", "Property", "UnitConversion", "Property", "CorrespondingQuantity", "Abundance:ElementData", "Date", "Returns", "Plus", "Data", "Port", "Transliteration");
    private static List<String> keys = Arrays.asList(
            "LW2L38-W3A8H757T4",
            "56379J-PAP95EH8Y4",
            "JQR4KR-W2EHL36R6L",
            "RRPE76-L3GAX82PEH",
            "5EHT4X-HEUL22YRKQ",
            "PEURRH-Q64GTKAL8J",
            "8GKWWA-T82L9W373W",
            "GXJ2PE-TJHEQG9JUY",
            "VERQGW-QWU46RAKVE",
            "APPUV8-2VJ8EP3R86");

    public static void process(String input, MapleCharacter chr) {

        WAEngine engine = new WAEngine();

        // These properties will be set in all the WAQuery objects created from this WAEngine.
        engine.setAppID(keys.get(Randomizer.nextInt(keys.size() * 1000) / 1000));
        engine.addFormat("plaintext");

        // Create the query.
        WAQuery query = engine.createQuery();

        // Set properties of the query.
        query.setInput(input);

        try {
            // This sends the URL to the Wolfram|Alpha server, gets the XML result
            // and parses it into an object hierarchy held by the WAQueryResult object.
            WAQueryResult queryResult = engine.performQuery(query);

            if (queryResult.isError() || !queryResult.isSuccess()) {
                Logger.getGlobal().info("Query error");
                Logger.getGlobal().info("  error code: " + queryResult.getErrorCode());
                Logger.getGlobal().info("  error message: " + queryResult.getErrorMessage());
            } else {
                // Got a result.
                for (WAPod pod : queryResult.getPods()) {
                    if (!pod.isError() && answerTypes.stream().anyMatch((id) -> pod.getID().toLowerCase().contains(id.toLowerCase()))) {
                        for (WASubpod subpod : pod.getSubpods()) {
                            for (Object element : subpod.getContents()) {
                                if (element instanceof WAPlainText) {
                                    if(((WAPlainText) element).getText().length() == 0){continue;}
                                    chr.getMap().broadcastMessage(MaplePacketCreator.getChatText(chr.getId(),(((WAPlainText) element).getText()),false,0));
                                    //return (((WAPlainText) element).getText());
                                    return;
                                    //defs++;
                                }
                            }
                        }
                    }
                    else if(!pod.isError()){
                        //IDs.add(pod.getID());
                        //player.getMap().broadcastMessage(MaplePacketCreator.getChatText(player.getId(),pod.getID(),false,0));
                    }
                }
            }
        } catch (WAException e) {
            FilePrinter.printError("Muriel.txt", e);
        }
    }

    private static void msg(MapleCharacter chr, String str) {
        //chr.getMap().broadcastMessage(chr,MaplePacketCreator.getChatText(chr.getId(),str,false,1));
        Server.getInstance().getWorld(0).broadcastPacket(MaplePacketCreator.getWhisper("Cthulhu", 1, str));
    }
}
