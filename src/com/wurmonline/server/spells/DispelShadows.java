package com.wurmonline.server.spells;

import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import com.friya.wurmonline.server.vamps.VampSkills;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.skills.Skill;
import com.wurmonline.server.spells.ReligiousSpell;
import com.wurmonline.server.zones.Zone;
import com.wurmonline.server.zones.Zones;


public class DispelShadows extends ReligiousSpell
{
	static public int actionId;

	static public short getActionId()
	{
		return (short)actionId;
	}
	
	public DispelShadows()
    {
        super(
        	"Dispel Shadows",							// name
        	(actionId = ModActions.getNextActionId()),	// action id
        	40,											// casting time
        	25,											// cost
        	25,											// difficulty
        	30,											// faith required
        	(60 * 1000)									// 60 sec cooldown
        );
        this.targetTile = true;
        this.description = "dispel stealth of creatures nearby";

        ActionEntry actionEntry = ActionEntry.createEntry((short) number, name, "enchanting",
            new int[] {
            		2 /* ACTION_TYPE_SPELL */,
            		36 /* ACTION_TYPE_ALWAYS_USE_ACTIVE_ITEM */,
                    48 /* ACTION_TYPE_ENEMY_ALWAYS */
    		}
        );
        ModActions.registerAction(actionEntry);
    }

    
    @SuppressWarnings("unused")
	@Override
    void doEffect(Skill castSkill, double power, Creature performer, int tilex, int tiley, int layer, int heightOffset)
    {
        performer.getCommunicator().sendNormalServerMessage("You force all hidden creatures around you into plain sight.");
        int sx = Zones.safeTileX(performer.getTileX() - 30 - (performer.getNumLinks() * 5));
        int sy = Zones.safeTileY(performer.getTileY() - 30 - (performer.getNumLinks() * 5));
        int ex = Zones.safeTileX(performer.getTileX() + 30 + (performer.getNumLinks() * 5));
        int ey = Zones.safeTileY(performer.getTileY() + 30 + (performer.getNumLinks() * 5));

        int dispelled = 0;
        Zone[] zones;

        for (Zone lZone : zones = Zones.getZonesCoveredBy(sx, sy, ex, ey, performer.isOnSurface())) {
            Creature[] crets;

            for (Creature cret : crets = lZone.getAllCreatures()) {
            	if(cret.isStealth()) {
            		dispelled++;

            		if(cret.isPlayer()) {
            			cret.getCommunicator().sendNormalServerMessage(performer.getName() + " casted dispel shadows, you are no longer hidden!");
            		}

            		cret.setStealth(false);
            	}
            }
        }
        
        Skill perception = performer.getSkills().getSkillOrLearn(VampSkills.PERCEPTION);
		perception.skillCheck(1.0f, 0.0f, false, 1.0f);

		performer.getCommunicator().sendNormalServerMessage("Your magic brought " + (dispelled > 0 ? dispelled : "no") + " creatures out of their stealth.");
    }
}
