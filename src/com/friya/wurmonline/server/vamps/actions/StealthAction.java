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
import com.friya.wurmonline.server.vamps.Mod;
import com.friya.wurmonline.server.vamps.Vampires;
import com.wurmonline.server.Players;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.NoSuchActionException;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.players.Player;

public class StealthAction implements ModAction 
{
	private static Logger logger = Logger.getLogger(StealthAction.class.getName());

	static private short actionId;
	private final ActionEntry actionEntry;
	private final int castTime = 5;		// half second cast (it's in 10ths of seconds)
	private final String effectName = "instantstealth";
	private final int cooldown = 1000 * 60 * 15;	// * minutes

	
	static public short getActionId()
	{
		return actionId;
	}
	

	public StealthAction() {
		logger.log(Level.INFO, "StealthAction()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"Stealth",
			"stealthing",
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
				// For now, just have this on test-env
				//if(Mod.isTestEnv() == false) {
				//	return null;
				//}

				if(performer.isPlayer() && object != null && object.getTemplateId() == ItemList.bodyBody && Vampires.isVampire(performer.getWurmId())) {
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

				try {
					if(counter == 1.0f) {
						performer.getCurrentAction().setTimeLeft(castTime);
						performer.sendActionControl("Stealthing", true, castTime);
						return false;
					}
					
					if(counter * 10.0f <= act.getTimeLeft()) {
						return false;
					}
				} catch (NoSuchActionException e) {
					return true;
				}

				if(isWithinDistanceToOthers(performer)) {
			    	performer.getCommunicator().sendNormalServerMessage("You are too close to someone to be able to stealth.");
					return true;
				}

				String playerEffect = performer.getName() + effectName;

				if(Cooldowns.isOnCooldown(playerEffect, cooldown)) {
			    	performer.getCommunicator().sendNormalServerMessage("You need to wait a little while before you can do this again. You can still use the normal stealth.");
			    	
			    	if(Mod.isTestEnv() == true) {
				    	performer.getCommunicator().sendNormalServerMessage("... but you are on testenv, so allowing it!");
			    	} else {
			    		return true;
			    	}
				}
				
				performer.setStealth(true);
				
				Cooldowns.setUsed(playerEffect);

				return true;
			}

			private boolean isWithinDistanceToOthers(Creature stealther)
			{
				Player[] players = Players.getInstance().getPlayers();
				
				for(Player p : players) {
					if(p.isStealth() || p.getPower() > 0 || p.getWurmId() == stealther.getWurmId()) {
						// To make sure you cannot use this to check whether other people are stealthed or admins are around.
						continue;
					}

					if(p.isWithinDistanceTo(stealther, 30f)) {
						logger.info(p.getName() + " is too close to " + stealther.getName() + " to be able to insta-stealth");
						return true;
					}
				}
				
				return false;
			}

			@Override
			public boolean action(Action act, Creature performer, Item source, Item target, short action, float counter)
			{
				return this.action(act, performer, target, action, counter);
			}
			
		}; // ActionPerformer
	}

}
