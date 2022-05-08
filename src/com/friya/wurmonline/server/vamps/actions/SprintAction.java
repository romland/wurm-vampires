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
import com.friya.wurmonline.server.vamps.Vampires;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;


public class SprintAction implements ModAction
{
	private static Logger logger = Logger.getLogger(SprintAction.class.getName());

	static private short actionId;
	private final ActionEntry actionEntry;
	
	private static String effectName = "sprint";
	private static long cooldown = (60 * 1000) * 60;		// * minutes
	
	static public short getActionId()
	{
		return actionId;
	}
	

	public SprintAction()
	{
		logger.log(Level.INFO, "SprintAction()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"Sprint", 
			"sprinting",
			new int[] { 6 }	// 6 /* ACTION_TYPE_NOMOVE */
		);
		ModActions.registerAction(actionEntry);
	}


	@Override
	public BehaviourProvider getBehaviourProvider()
	{
		return new BehaviourProvider() {

			public List<ActionEntry> getBehavioursFor(Creature performer, int tilex, int tiley, boolean onSurface, int tile, int dir)
			{
				/*
				if(Vampires.isVampire(performer.getWurmId())) {
					return Arrays.asList(actionEntry);
				}
				return null;
				*/
				return Arrays.asList(actionEntry);
			}

			public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, int tilex, int tiley, boolean onSurface, int tile)
			{
				/*
				if(Vampires.isVampire(performer.getWurmId())) {
					return Arrays.asList(actionEntry);
				}
				return null;
				*/
				return Arrays.asList(actionEntry);
			}

			@Override
			public List<ActionEntry> getBehavioursFor(Creature performer, int tilex, int tiley, boolean onSurface, int tile)
			{
				/*
				if(Vampires.isVampire(performer.getWurmId())) {
					return Arrays.asList(actionEntry);
				}
				return null;
				*/
				return Arrays.asList(actionEntry);
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
			
			
			public boolean action(Action act, Creature performer, Item source, int tilex, int tiley, boolean onSurface, int heightOffset, int tile, short action, float counter) {
				action(act, performer, tilex, tiley, onSurface, tile, action, counter);
				return true;
			}
			
			public boolean action(Action act, Creature performer, int tilex, int tiley, boolean onSurface, int tile, short action, float counter)
			{
				if (!performer.isPlayer() || performer.getVehicle() != -10) {
					return true;
				}
				
				String playerEffect = performer.getName() + effectName;
				
				if(Cooldowns.isOnCooldown(playerEffect, (Vampires.isVampire(performer.getWurmId()) ? (cooldown/2) : cooldown))) {
					performer.getCommunicator().sendNormalServerMessage("You're still exhausted.");
					return true;
				}

				Cooldowns.setUsed(playerEffect);

				((Player)performer).setFarwalkerSeconds((byte) 20);
				performer.getMovementScheme().setFarwalkerMoveMod(true);
				performer.getStatus().sendStateString();
				performer.getCommunicator().sendNormalServerMessage("Your legs tingle and you feel fantastic!");

				return true;
			}

		}; // ActionPerformer
	}
}
