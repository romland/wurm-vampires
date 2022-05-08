package com.wurmonline.server.spells;

import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import com.friya.wurmonline.server.vamps.Mod;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.questions.PinpointHumanoidQuestion;
import com.wurmonline.server.skills.Skill;
import com.wurmonline.server.spells.ReligiousSpell;


public class PinpointHumanoid extends ReligiousSpell
{
	static public int actionId;
	
	static public short getActionId()
	{
		return (short)actionId;
	}

	public PinpointHumanoid()
    {
    	// TODO:
    	// 		at 20 perception cheaper cast
   	 	//		at 40 perception faster cast

    	super("Pinpoint Humanoid",
        	(actionId = ModActions.getNextActionId()),		// action id
        	10,												// casting time
        	(Mod.isTestEnv() ? 1 : 25),						// cost
        	25,												// difficulty
        	30,												// faith required
        	(60 * 1000)										// 60 sec cooldown
        );

        this.targetTile = true;
        this.description = "locates a humanoid in the lands. More precise than locate soul";

		ActionEntry actionEntry = ActionEntry.createEntry((short) number, name, "enchanting",
			new int[] {
					2,		/* ACTION_TYPE_SPELL */
					36,		/* ACTION_TYPE_ALWAYS_USE_ACTIVE_ITEM */
			        48		/* ACTION_TYPE_ENEMY_ALWAYS */
			}
		);
		ModActions.registerAction(actionEntry);
	}


    @Override
    void doEffect(Skill castSkill, double power, Creature performer, int tilex, int tiley, int layer, int heightOffset)
    {
        PinpointHumanoidQuestion phq = new PinpointHumanoidQuestion(
        		performer, 
        		"Pinpoint a humanoid", 
        		"Which humanoid do you wish to locate?",
        		performer.getWurmId(), 
        		false, 
        		power
        );
        phq.sendQuestion();
    }
}
