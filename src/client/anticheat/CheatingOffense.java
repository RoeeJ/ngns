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

public enum CheatingOffense {
	FASTATTACK(1, 60000),
    FASTATTACK2(1, 60000),
    MOVE_MONSTERS,
	TUBI,
	FAST_HP_REGEN,
	FAST_MP_REGEN(1, 60000),
	SAME_DAMAGE,
	ATTACK_WITHOUT_GETTING_HIT,
	HIGH_DAMAGE(10, 300000),
    EXTREME_HIGH_DAMAGE(100, 300000, 5),
    ATTACK_FARAWAY_MONSTER(5),
	REGEN_HIGH_HP(50),
	REGEN_HIGH_MP(50),
	ITEMVAC(5, 60000, 10),
	SHORT_ITEMVAC(2),
	USING_FARAWAY_PORTAL(30, 300000),
	FAST_TAKE_DAMAGE(1),
	FAST_MOVE(1, 60000, -1, false),
	HIGH_JUMP(1, 60000, -1, false),
	MISMATCHING_BULLETCOUNT(50),
	ETC_EXPLOSION(50, 300000),
	FAST_SUMMON_ATTACK,
	ATTACKING_WHILE_DEAD(10, 300000),
	USING_UNAVAILABLE_ITEM(10, 300000),
	FAMING_SELF(10, 300000), // purely for marker reasons (appears in the database)
	FAMING_UNDER_15(10, 300000),
	EXPLODING_NONEXISTANT,
	SUMMON_HACK,
    MOB_COUNT(100),
    MISS_GODMODE;

	private final int points;
	private final long validityDuration;
	private final int autobancount;
	private boolean enabled = true;

	private CheatingOffense() {
		this(1);
	}

	private CheatingOffense(int points) {
		this(points, 60000);
	}

    private CheatingOffense(int points, long validityDuration) {
		this(points, validityDuration, -1);
	}

    private CheatingOffense(int points, long validityDuration, int autobancount) {
		this(points, validityDuration, autobancount, true);
	}
	
	private CheatingOffense(int points, long validityDuration, int autobancount, boolean enabled) {
		this.points = points;
		this.validityDuration = validityDuration;
		this.autobancount = autobancount;
        this.enabled = enabled;
    }

    public int getPoints() {
        return points;
    }

    public long getValidityDuration() {
        return validityDuration;
    }

    public boolean shouldAutoban(int count) {
        if (autobancount == -1) {
            return false;
        }
        return count >= autobancount;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
	}
}
