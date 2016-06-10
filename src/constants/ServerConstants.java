/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package constants;

public class ServerConstants {
    public static final String SERVER_NAME = "NoGameStory";
    // Rate Configuration
    public static final byte QUEST_EXP_RATE = 4;
    public static final byte QUEST_MESO_RATE = 3;
    // Login Configuration
    public static final int CHANNEL_LOAD = 150;//Players per channel
    public static final long RANKING_INTERVAL = 60 * 60 * 1000;
    public static final boolean ENABLE_PIC = false;
    //Event Configuration
    public static final boolean PERFECT_PITCH = false;
    public static final String EVENTS = "";
    // IP Configuration
    public static final String HOST = "gs.nogameno.life";
    //Database Configuration
    public static final String DB_URL = "jdbc:mysql://localhost:3306/MSS?autoReconnect=true";
    public static final String DB_USER = "mss";
    public static final String DB_PASS = "XjYp4c5BY37FUSamZqwEgRky";
    public static final boolean VPS = true;
    //External services Configuration
    public static final boolean USE_SLACK = false;
    public static final boolean USE_WEBSOCKET = true;
    public static final boolean USE_MONGO = true;
    public static final boolean WZ_LOCKDOWN = false;
    public static final String DEFAULT_KEY = "xBJPnmgFS5KBj4e7EXeLfjKVqF5Nukbs";
    public static final long HEARTBEAT_INTERVAL = 15 * 1000;
    public static short VERSION = 83;
    public static String[] WORLD_NAMES = {"Scania", "Bera", "Broa", "Windia", "Khaini", "Bellocan", "Mardia", "Kradia", "Yellonde", "Demethos", "Galicia", "El Nido", "Zenith", "Arcenia", "Kastia", "Judis", "Plana", "Kalluna", "Stius", "Croa", "Medere"};
    public static String hitmanAnswer;
    public static String practiceHitman;
    public static String unscrambledWord;
    public static String MASTER_PASSWORD = "datmasterpwdtho";
}