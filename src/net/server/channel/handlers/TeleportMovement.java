package net.server.channel.handlers;

import server.movement.AbsoluteLifeMovement;
import tools.data.output.LittleEndianWriter;

import java.awt.*;

/**
 * Created by cipher on 2/11/16.
 */
public class TeleportMovement extends AbsoluteLifeMovement {
    public TeleportMovement(byte command, Point point, byte newstate) {
        super(command,point,0,newstate);
    }

    @Override
    public void serialize(LittleEndianWriter lew) {
        lew.write(getType());
        lew.writeShort(getPosition().x);
        lew.writeShort(getPosition().y);
        lew.writeShort(getPixelsPerSecond().x);
        lew.writeShort(getPixelsPerSecond().y);
        lew.write(getNewstate());
    }
}
