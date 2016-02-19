/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.command;

import client.MapleClient;
import java.rmi.RemoteException;
import java.sql.SQLException;

/**
 *
 * @author Menno
 */
public interface CommandInterface {
    
    public boolean execute(MapleClient c, String[] splitted, char heading) throws RemoteException, SQLException;
}
