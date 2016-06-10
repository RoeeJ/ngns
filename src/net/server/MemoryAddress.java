package net.server;

/**
 * Created by cipher on 04/06/2016.
 */
public class MemoryAddress {
    public int address;
    public int offset;

    public MemoryAddress(int address, int offset) {
        this.address = address;
        this.offset = offset;
    }

    public MemoryAddress(int address) {
        this.address = address;
        this.offset = -1;
    }

    public boolean isPointer() {
        return offset != -1;
    }
}
