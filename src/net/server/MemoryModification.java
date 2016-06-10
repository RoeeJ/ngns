package net.server;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cipher on 04/06/2016.
 */
public class MemoryModification {
    public MemoryAddress address;
    public int[] onOpcodes;
    public int[] offOpcodes;

    public MemoryModification(MemoryAddress address, int[] onOpcodes, int[] offOpcodes) {
        this.address = address;
        this.onOpcodes = onOpcodes;
        this.offOpcodes = offOpcodes;
    }

    public MemoryModification(MemoryAddress memoryAddress, ArrayList<Integer> on, ArrayList<Integer> off) {
        this.address = memoryAddress;
        this.onOpcodes = Ints.toArray(on);
        this.offOpcodes = Ints.toArray(off);
    }

    public MemoryModification(MemoryAddress memoryAddress, List<Integer> onOpcodes, List<Integer> offOpcodes) {
        this.address = memoryAddress;
        this.onOpcodes = Ints.toArray(onOpcodes);
        this.offOpcodes = Ints.toArray(offOpcodes);
    }

    public static int[] toPrimitive(Integer[] IntegerArray) {

        int[] result = new int[IntegerArray.length];
        for (int i = 0; i < IntegerArray.length; i++) {
            result[i] = IntegerArray[i];
        }
        return result;
    }
}
