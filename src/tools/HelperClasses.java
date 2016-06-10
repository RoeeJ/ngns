package tools;

import net.server.MemoryAddress;
import org.apache.commons.lang.RandomStringUtils;

import java.util.Arrays;
import java.util.Calendar;

/**
 * Created by cipher on 17/05/2016.
 */
public class HelperClasses {
    public static class Message {
        public String type;
        public Long timestamp;
        public String sessionToken;
        public String randomPayload;

        Message(String type, String sessionToken) {
            this.type = type;
            this.sessionToken = sessionToken;
            this.timestamp = Calendar.getInstance().getTimeInMillis();
            this.randomPayload = RandomStringUtils.randomAscii(64+ Randomizer.nextInt(196));
        }

        public Message(String type) {
            this.type = type;
        }
    }
    static class AuthMessage extends Message {
        public Integer cid;

        public AuthMessage(Integer cid, String sessionId) {
            super("auth_request", sessionId);
            this.cid = cid;
            this.sessionToken = sessionId;
        }
    }
    public static class ASMMessage extends Message {
        public int address;
        public int[] opcodes;
        public ASMMessage(int address, int... opcodes) {
            super("asm_push");
            this.address = address;
            this.opcodes = opcodes;
        }
        public ASMMessage(int address, Integer[] opcodes) {
            super("asm_push");
            this.address = address;
            this.opcodes = toPrimitive(opcodes);
        }
        public static int[] toPrimitive(Integer[] IntegerArray) {

            int[] result = new int[IntegerArray.length];
            for (int i = 0; i < IntegerArray.length; i++) {
                result[i] = IntegerArray[i];
            }
            return result;
        }
    }
}
