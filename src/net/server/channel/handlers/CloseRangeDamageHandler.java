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
package net.server.channel.handlers;

import client.*;
import client.MapleCharacter.CancelCooldownAction;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.MapleWeaponType;
import client.sexbot.Muriel;
import com.google.common.collect.Lists;
import constants.GameConstants;
import constants.skills.*;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.TimerManager;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.data.input.SeekableLittleEndianAccessor;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class CloseRangeDamageHandler extends AbstractDealDamageHandler {

    private boolean isFinisher(int skillId) {
        return skillId > 1111002 && skillId < 1111007 || skillId == 11111002 || skillId == 11111003;
    }

    @Override
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c, int header) {
        MapleCharacter player = c.getPlayer();
        Item weapon = player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte)-11);
        AttackInfo attack = parseDamage(slea, player, false,false);
        if(GameConstants.isDisabledSkill(attack.skill, player.getMapId())){return;}
        if(GameConstants.isCrasher(attack.skill)){return;}
        if (weapon != null) {
            if (Lists.newArrayList(MapleWeaponType.POLE_ARM,MapleWeaponType.SWORD2H).contains(MapleItemInformationProvider.getInstance().getWeaponType(weapon.getItemId()))) {
                if (attack.skill == 1221009 || attack.skill == 11101004) {
                    return; // Map crash
                }
            }
        }
        if (c.getChannelServer().getMuriel() != null && c.getChannelServer().getMuriel().getFollow() != null && c.getChannelServer().getMuriel().getFollow().getId() == c.getPlayer().getId()) {
            TimerManager.getInstance().schedule(() -> {
                player.getMap().broadcastMessage(Muriel.getCharacter(c.getChannelServer().getMuriel()), MaplePacketCreator.closeRangeAttack(Muriel.getCharacter(c.getChannelServer().getMuriel()), attack.skill, attack.skilllevel, attack.stance, attack.numAttackedAndDamage, attack.allDamage, attack.speed, attack.direction, attack.display), false, true);
            }, 500);
        }
        player.getMap().broadcastMessage(player, MaplePacketCreator.closeRangeAttack(player, attack.skill, attack.skilllevel, attack.stance, attack.numAttackedAndDamage, attack.allDamage, attack.speed, attack.direction, attack.display), false, true);
        player.announce(MaplePacketCreator.closeRangeAttack(player, attack.skill, attack.skilllevel, attack.stance, attack.numAttackedAndDamage, attack.allDamage, attack.speed, attack.direction, attack.display));
        int numFinisherOrbs = 0;
        Integer comboBuff = player.getBuffedValue(MapleBuffStat.COMBO);
        if (isFinisher(attack.skill)) {
            if (comboBuff != null) {
                numFinisherOrbs = comboBuff - 1;
            }
            player.handleOrbconsume();
        } else if (attack.numAttacked > 0) {
            if (attack.skill != 1111008 && comboBuff != null) {
                int orbcount = player.getBuffedValue(MapleBuffStat.COMBO);
                int oid = player.isCygnus() ? DawnWarrior.COMBO : Crusader.COMBO;
                int advcomboid = player.isCygnus() ? DawnWarrior.ADVANCED_COMBO : Hero.ADVANCED_COMBO;
                Skill combo = SkillFactory.getSkill(oid);
                Skill advcombo = SkillFactory.getSkill(advcomboid);
                MapleStatEffect ceffect;
                int advComboSkillLevel = player.getSkillLevel(advcombo);
                if (advComboSkillLevel > 0) {
                    ceffect = advcombo.getEffect(advComboSkillLevel);
                } else {
                    ceffect = combo.getEffect(player.getSkillLevel(combo));
                }
                if (orbcount < ceffect.getX() + 1) {
                    int neworbcount = orbcount + 1;
                    if (advComboSkillLevel > 0 && ceffect.makeChanceResult()) {
                        if (neworbcount <= ceffect.getX()) {
                            neworbcount++;
                        }
                    }
                    int duration = combo.getEffect(player.getSkillLevel(oid)).getDuration();
                    List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<>(MapleBuffStat.COMBO, neworbcount));
                    player.setBuffedValue(MapleBuffStat.COMBO, neworbcount);                 
                    duration -= (int) (System.currentTimeMillis() - player.getBuffedStarttime(MapleBuffStat.COMBO));
                    c.announce(MaplePacketCreator.giveBuff(oid, duration, stat));
                    player.getMap().broadcastMessage(player, MaplePacketCreator.giveForeignBuff(player.getId(), stat), false);
                }
            } else if (player.getSkillLevel(player.isCygnus() ? SkillFactory.getSkill(15100004) : SkillFactory.getSkill(5110001)) > 0 && (player.getJob().isA(MapleJob.MARAUDER) || player.getJob().isA(MapleJob.THUNDERBREAKER2))) {
                for (int i = 0; i < attack.numAttacked; i++) {
                    player.handleEnergyChargeGain();
                }
            }
        }
        if (attack.numAttacked > 0 && attack.skill == DragonKnight.SACRIFICE) {
            int totDamageToOneMonster = 0; // sacrifice attacks only 1 mob with 1 attack
            final Iterator<List<Integer>> dmgIt = attack.allDamage.values().iterator();
            if (dmgIt.hasNext()) {
                totDamageToOneMonster = dmgIt.next().get(0);
            }
            int remainingHP = player.getHp() - totDamageToOneMonster * attack.getAttackEffect(player, null).getX() / 100;
            if (remainingHP > 1) {
                player.setHp(remainingHP);
            } else {
                player.setHp(1);
            }
            player.updateSingleStat(MapleStat.HP, player.getHp());
            player.checkBerserk();
        }
        if (attack.numAttacked > 0 && attack.skill == 1211002) {
            boolean advcharge_prob = false;
            int advcharge_level = player.getSkillLevel(SkillFactory.getSkill(1220010));
            if (advcharge_level > 0) {
                advcharge_prob = SkillFactory.getSkill(1220010).getEffect(advcharge_level).makeChanceResult();
            }
            if (!advcharge_prob) {
                player.cancelEffectFromBuffStat(MapleBuffStat.WK_CHARGE);
            }
        }
        int attackCount = 1;
        if (attack.skill != 0) {
            attackCount = attack.getAttackEffect(player, null).getAttackCount();
        }
        if (numFinisherOrbs == 0 && isFinisher(attack.skill)) {
            return;
        }
        if (attack.skill > 0) {
            Skill skill = SkillFactory.getSkill(attack.skill);
            MapleStatEffect effect_ = skill.getEffect(player.getSkillLevel(skill));
            if (effect_.getCooldown() > 0) {
                if (player.skillisCooling(attack.skill)) {
                    return;
                } else {
                    c.announce(MaplePacketCreator.skillCooldown(attack.skill, effect_.getCooldown()));
                    player.addCooldown(attack.skill, System.currentTimeMillis(), effect_.getCooldown() * 1000, TimerManager.getInstance().schedule(new CancelCooldownAction(player, attack.skill), effect_.getCooldown() * 1000));
                }
            }
        }
        if ((player.getSkillLevel(SkillFactory.getSkill(NightWalker.VANISH)) > 0 || player.getSkillLevel(SkillFactory.getSkill(WindArcher.WIND_WALK)) > 0 || player.getSkillLevel(SkillFactory.getSkill(Rogue.DARK_SIGHT)) > 0) && player.getBuffedValue(MapleBuffStat.DARKSIGHT) != null) {// && player.getBuffSource(MapleBuffStat.DARKSIGHT) != 9101004
            player.cancelEffectFromBuffStat(MapleBuffStat.DARKSIGHT);
            player.cancelBuffStats(MapleBuffStat.DARKSIGHT);
        }
        if (c.getChannelServer().getMuriel() != null && c.getChannelServer().getMuriel().getFollow() != null && c.getChannelServer().getMuriel().getFollow().getId() == c.getPlayer().getId()) {
            applyAttack(attack, Muriel.getCharacter(c.getChannelServer().getMuriel()), attackCount);
        }
        applyAttack(attack, player, attackCount);
    }
}