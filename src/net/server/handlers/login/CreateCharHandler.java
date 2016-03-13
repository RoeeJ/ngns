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
package net.server.handlers.login;

import client.*;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import com.google.common.collect.Lists;
import constants.GameConstants;
import net.AbstractMaplePacketHandler;
import server.MapleItemInformationProvider;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

import java.util.List;

public final class CreateCharHandler extends AbstractMaplePacketHandler {

    @Override
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c, int header) {
        String name = slea.readMapleAsciiString();
        if (!MapleCharacter.canCreateChar(name)) {
            return;
        }
        MapleCharacter newchar = MapleCharacter.getDefault(c);
        newchar.setWorld(c.getWorld());
        int job = slea.readInt();
        int face = verifyFace(slea.readInt());
        int hair = verifyHair(slea.readInt() + slea.readInt());
        newchar.setFace(face);
        newchar.setHair(hair);
        int skincolor = verifySkinColor(slea.readInt());
        newchar.setSkinColor(MapleSkinColor.getById(skincolor));
        int top = verifyTop(slea.readInt());
        int bottom = verifyBottom(slea.readInt());
        int shoes = verifyShoes(slea.readInt());
        int weapon = verifyWeapon(slea.readInt());
        newchar.setGender(verifyGender(slea.readByte()));
        newchar.setName(name);
        if (!GameConstants.isValidCreationConfiguration(c, face, hair, top, bottom, shoes, weapon, skincolor)) {
            return;
        }
        if (job == 0) { // Knights of Cygnus
            newchar.setJob(MapleJob.NOBLESSE);
            newchar.setMapId(130030000);
            newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161047, (byte) 0, (short) 1));
        } else if (job == 1) { // Adventurer
            newchar.setJob(MapleJob.BEGINNER);
            newchar.setMapId(/*specialJobType == 2 ? 3000600 : */10000);
            newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161001, (byte) 0, (short) 1));
        } else if (job == 2) { // Aran
            newchar.setJob(MapleJob.LEGEND);
            newchar.setMapId(914000000);
            newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161048, (byte) 0, (short) 1));
        } else {
            c.announce(MaplePacketCreator.deleteCharResponse(0, 9));
            return;
        }
        //CHECK FOR EQUIPS
        MapleInventory equip = newchar.getInventory(MapleInventoryType.EQUIPPED);
        if (newchar.isGM()) {
            Item eq_hat = MapleItemInformationProvider.getInstance().getEquipById(1002140);
            eq_hat.setPosition((byte) -1);
            equip.addFromDB(eq_hat);
            top = 1042003;
            bottom = 1062007;
            weapon = 1322013;
        }
        List<Integer> skills = Lists.newArrayList(1003,1004,8,1006,1009,1010,1011,1013,1015,1017,1020,9000,9001,9002,1018,1019,1031,1007);
        if (newchar.isGM()) {
            skills.addAll(Lists.newArrayList(9001000, 9001001, 9001000, 9101000, 9101001, 9101002, 9101003, 9101004, 9101005, 9101006, 9101007, 9101008));
        }
        skills.stream().forEach(skillId->{
            Skill skill = SkillFactory.getSkill(skillId);
            newchar.skills.put(skill,new MapleCharacter.SkillEntry((byte)skill.getMaxLevel(),(byte)skill.getMaxLevel(),-1));
        });
        Item eq_top = MapleItemInformationProvider.getInstance().getEquipById(top);
        eq_top.setPosition((byte) -5);
        equip.addFromDB(eq_top);
        Item eq_bottom = MapleItemInformationProvider.getInstance().getEquipById(bottom);
        eq_bottom.setPosition((byte) -6);
        equip.addFromDB(eq_bottom);
        Item eq_shoes = MapleItemInformationProvider.getInstance().getEquipById(shoes);
        eq_shoes.setPosition((byte) -7);
        equip.addFromDB(eq_shoes);
        Item eq_weapon = MapleItemInformationProvider.getInstance().getEquipById(weapon);
        eq_weapon.setPosition((byte) -11);
        equip.addFromDB(eq_weapon.copy());
        if (!newchar.insertNewChar()) {
            c.announce(MaplePacketCreator.deleteCharResponse(0, 9));
            return;
        }
        c.announce(MaplePacketCreator.addNewCharEntry(newchar));
    }

    private int verifyGender(byte b) {
        return (b == 0 || b == 1) ? b : 0;
    }

    private int verifyWeapon(int i) {
        return GameConstants.characterWeapon.contains(i) ? i : GameConstants.characterWeapon.get(0);
    }

    private int verifyShoes(int i) {
        return GameConstants.characterShoes.contains(i) ? i : GameConstants.characterShoes.get(0);
    }

    private int verifyBottom(int i) {
        return GameConstants.characterBottom.contains(i) ? i : GameConstants.characterBottom.get(0);
    }

    private int verifyTop(int i) {
        return GameConstants.characterTop.contains(i) ? i : GameConstants.characterTop.get(0);
    }

    private int verifySkinColor(int i) {
        return GameConstants.characterSkin.contains(i) ? i : GameConstants.characterSkin.get(0);
    }

    private int verifyFace(int i) {
        return GameConstants.characterEyes.contains(i) ? i : GameConstants.characterEyes.get(0);
    }

    private int verifyHair(int hair) {
        return GameConstants.characterHair.contains(hair) ? hair : GameConstants.characterHair.get(0);
    }
}