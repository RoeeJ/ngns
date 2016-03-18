package server;

/**
 * Created by cipher on 3/15/16.
 */
public enum MapleEquipSlot {
    Ae("Ae", new byte[]{-4}),
    Af("Af", new byte[]{-2,-3}),
    Ay("Ay", new byte[]{-3}),
    Bd("Bd", new byte[]{0x02}),
    Be("Be", new byte[]{-50}),
    Cp("Cp", new byte[]{-1,-7,-8}),
    Fc("Fc", new byte[]{-1,-7,-8}),
    Gv("Gv", new byte[]{-9,-8}),
    Hd("Hd", new byte[]{0x02}),
    Hr("Hr", new byte[]{0x02}),
    HrCp("HrCp", new byte[]{0x02}),
    Ma("Ma", new byte[]{-5}),
    MaPn("MaPn", new byte[]{-5,-105}),
    Me("Me", new byte[]{-49}),
    Pe("Pe", new byte[]{-17}),
    Pn("Pn", new byte[]{-6}),
    Ri("Ri", new byte[]{-12,-13,-15,-16}),
    Sd("Sd", new byte[]{-19}),
    Si("Si", new byte[]{-10}),
    So("So", new byte[]{-7}),
    Sr("Sr", new byte[]{-9}),
    Tm("Tm", new byte[]{-18}),
    Wp("Wp", new byte[]{-11}),
    WpSi("WpSi", new byte[]{-11});

    public final String slot;
    public final byte[] slots;

    private MapleEquipSlot(String slot, byte[] slots) {
        this.slot = slot;
        this.slots = slots;
    }

    public static MapleEquipSlot fromSlot(String string) {
        return MapleEquipSlot.valueOf(string);
    }
}
/*
Pn
Ri
Sd
Si
So
Sr
Tm
Wp
WpSi
 */
//public enum MapleSquadType {
//    ZAKUM(0),
//    HORNTAIL(1),
//    PINK_BEAN(2),
//    UNKNOWN(-1);
//    final byte type;
//
//    private MapleSquadType(int type) {
//        this.type = (byte) type;
//    }
//}
