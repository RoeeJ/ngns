/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
                       Matthias Butz <matze@odinms.de>
                       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation. You may not use, modify
    or distribute this program under any other version of the
    GNU Affero General Public License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package client.anticheat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedHashSet;
import java.util.Set;

import tools.DatabaseConnection;
import server.TimerManager;

public class CheatingOffensePersister {
	private final static CheatingOffensePersister INSTANCE = new CheatingOffensePersister();
	private Set<CheatingOffenseEntry> toPersist = new LinkedHashSet<CheatingOffenseEntry>();

	private CheatingOffensePersister() {
		TimerManager.getInstance().register(new PersistingTask(), 61000);
	}
	
	public static CheatingOffensePersister getInstance() {
		return INSTANCE;
	}
	
	public void persistEntry (CheatingOffenseEntry coe) {
		synchronized (toPersist) {
			toPersist.remove(coe); //equal/hashCode h4x
			toPersist.add(coe);
		}
	}
	
	public class PersistingTask implements Runnable {
		@Override
		public void run() {
			CheatingOffenseEntry[] offenses;
			synchronized (toPersist) {
				offenses = toPersist.toArray(new CheatingOffenseEntry[toPersist.size()]);
				toPersist.clear();
			}
			
			Connection con = DatabaseConnection.getConnection();
			try {
				PreparedStatement insertps = con.prepareStatement("INSERT INTO `logs_cheater` (character_id, offense, count, lastoffensetime, param) VALUES (?, ?, ?, ?, ?)", DatabaseConnection.RETURN_GENERATED_KEYS);
				PreparedStatement updateps = con.prepareStatement("UPDATE `logs_cheater` SET count = ?, lastoffensetime = ?, param = ? WHERE id = ?");
				for (CheatingOffenseEntry offense : offenses) {
					String parm = offense.getParam() == null ? "" : offense.getParam();
					if (offense.getDbId() == -1) {
						insertps.setInt(1, offense.getChrfor().getId());
						insertps.setString(2, offense.getOffense().name());
						insertps.setInt(3, offense.getCount());
						insertps.setTimestamp(4, new Timestamp(offense.getLastOffenseTime()));
						insertps.setString(5, parm);
						insertps.executeUpdate();
						ResultSet rs = insertps.getGeneratedKeys();
						if (rs.next()) {
							offense.setDbId(rs.getInt(1));
						}
						rs.close();
                                                rs = null;
					} else {
						updateps.setInt(1, offense.getCount());
						updateps.setTimestamp(2, new Timestamp(offense.getLastOffenseTime()));
						updateps.setString(3, parm);
						updateps.setInt(4, offense.getDbId());
						updateps.executeUpdate();
					}
				}
				insertps.close();
				updateps.close();
			} catch (SQLException e) {
				System.out.println("error persisting cheatlog" + e);
			}
		}
	}
}
