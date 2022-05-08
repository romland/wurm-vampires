package com.friya.wurmonline.server.vamps.actions;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import com.friya.wurmonline.server.vamps.Cooldowns;
import com.friya.wurmonline.server.vamps.VampSkills;
import com.friya.wurmonline.server.vamps.Vampires;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.NoSuchActionException;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.skills.Skill;
import com.wurmonline.server.zones.VolaTile;

public class AssistSlainAction implements ModAction 
{
	private static Logger logger = Logger.getLogger(AssistSlainAction.class.getName());

	static private short actionId;
	private final ActionEntry actionEntry;
	private final int castTime = 10 * 60;		// 60 second cast
	private final int cooldown = 1000 * 60 * 60 * 12;	// 12 hours

	static public short getActionId()
	{
		return actionId;
	}


	public AssistSlainAction() {
		logger.log(Level.INFO, "AssistSlainAction()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"Assist Slain...",
			"assisting",
			new int[] {
				6			/* ACTION_TYPE_NOMOVE */
			}
		);
		ModActions.registerAction(actionEntry);
	}


	@Override
	public BehaviourProvider getBehaviourProvider()
	{
		return new BehaviourProvider() {
			// Menu with activated object
			@Override
			public List<ActionEntry> getBehavioursFor(Creature performer, Item source, Item object)
			{
				return this.getBehavioursFor(performer, object);
			}

			// Menu without activated object
			@Override
			public List<ActionEntry> getBehavioursFor(Creature performer, Item object)
			{
				if(performer.isPlayer() && object != null && object.getTemplateId() == ItemList.bodyBody && Vampires.isVampire(performer.getWurmId()) && Vampires.getStakedTeleportPosition() != null) {
					return Arrays.asList(actionEntry);
				}
				
				return null;
			}
		};
	}

	@Override
	public ActionPerformer getActionPerformer()
	{
		return new ActionPerformer() {

			@Override
			public short getActionId() {
				return actionId;
			}

			// Without activated object
			@Override
			public boolean action(Action act, Creature performer, Item target, short action, float counter)
			{
				if(Vampires.isVampire(performer.getWurmId()) == false) {
					return true;
				}
				
				String playerEffect = performer.getName() + "assistSlain";

				if(Cooldowns.isOnCooldown(playerEffect, cooldown)) {
			    	performer.getCommunicator().sendNormalServerMessage("You can only use this every few hours. Don't let the hunter go unpunished, though.");
					return true;
				}

				try {
					if(counter == 1.0f) {
						// add 100 skills, 10 sec cast time
						int tmpTime = castTime - ((int)performer.getSkills().getSkillOrLearn(VampSkills.AIDING).getKnowledge() / 2);
						
						performer.getCurrentAction().setTimeLeft(tmpTime);
						performer.sendActionControl("Teleporting", true, tmpTime);
						return false;
					}
					
					if(counter * 10.0f <= act.getTimeLeft()) {
						return false;
					}
				} catch (NoSuchActionException e) {
					return true;
				}

				if(Vampires.getStakedTeleportPosition() == null) {
					performer.getCommunicator().sendNormalServerMessage("The red pillar of the slain faded.");
					return true;
				}
				
				VolaTile t = Vampires.getStakedTeleportPosition();

				performer.setTeleportPoints((short)t.getTileX(), (short)t.getTileY(), (t.isOnSurface() ? 0 : -1), 0);
		    	if(!performer.startTeleporting()) {
					performer.getCommunicator().sendNormalServerMessage("Fzzzt!");
		    		return true;
		    	}
		    	performer.getCommunicator().sendTeleport(false);
		    	performer.setBridgeId(-10);
		    	performer.teleport(true);		// destroyVisionArea

				Cooldowns.setUsed(playerEffect);

				Skill s = performer.getSkills().getSkillOrLearn(VampSkills.AIDING);
				s.skillCheck(1.0f, 5.0f, false, 1.0f);
				
		    	return true;
			}
			
			@Override
			public boolean action(Action act, Creature performer, Item source, Item target, short action, float counter)
			{
				return this.action(act, performer, target, action, counter);
			}
			
		}; // ActionPerformer
	}
}
