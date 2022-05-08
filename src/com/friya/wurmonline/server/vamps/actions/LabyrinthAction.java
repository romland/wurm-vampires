package com.friya.wurmonline.server.vamps.actions;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import com.friya.wurmonline.server.maze.Maze;
import com.friya.wurmonline.server.vamps.Cooldowns;
import com.friya.wurmonline.server.vamps.Mod;
import com.friya.wurmonline.server.vamps.Vampires;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

import com.wurmonline.shared.constants.FenceConstants;
import com.wurmonline.shared.constants.StructureConstantsEnum;

public class LabyrinthAction implements ModAction  
{
	private static Logger logger = Logger.getLogger(LabyrinthAction.class.getName());

	static private short actionId;
	private final ActionEntry actionEntry;
	
	private static String effectName = "labyrinth";
	private static long cooldown = (60 * 1000) * 600;		// * minutes

	public static boolean isGMonly = true;
	public static int requiredGMlevel = 5;
	
	static public short getActionId()
	{
		return actionId;
	}
	

	public LabyrinthAction()
	{
		logger.log(Level.INFO, "LabyrinthAction()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"Labyrinth", 
			"creating",
			new int[] {
				23,		//ACTION_TYPE_IGNORERANGE 
				6
			}	// 6 /* ACTION_TYPE_NOMOVE */
		);
		ModActions.registerAction(actionEntry);
	}


	boolean isAllowed(Creature performer)
	{
		if(isGMonly && performer.getPower() < requiredGMlevel) {
			return false;
		}
		
		if(performer.getPower() >= requiredGMlevel) {
			return true;
		}
		
		if(Vampires.isVampire(performer.getWurmId()) == false) {
			return false;
		}

		return true;
	}
	
	
	@Override
	public BehaviourProvider getBehaviourProvider()
	{
		return new BehaviourProvider() {

			// when targetting creature
			@Override
			public List<ActionEntry> getBehavioursFor(Creature performer, Creature target)
			{
				return getBehavioursFor(performer, target.getTileX(), target.getTileY(), target.isOnSurface(), target.getCurrentTileNum());
			}

			// when targetting creature
			public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Creature target)
			{
				return getBehavioursFor(performer, target.getTileX(), target.getTileY(), target.isOnSurface(), target.getCurrentTileNum());
			}

			// when targetting tile
			public List<ActionEntry> getBehavioursFor(Creature performer, int tilex, int tiley, boolean onSurface, int tile, int dir)
			{
				if(isAllowed(performer) == false) {
					return null;
				}

				return Arrays.asList(actionEntry);
			}

			// when targetting tile
			public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, int tilex, int tiley, boolean onSurface, int tile)
			{
				if(isAllowed(performer) == false) {
					return null;
				}

				return Arrays.asList(actionEntry);
			}

			// when targetting tile
			@Override
			public List<ActionEntry> getBehavioursFor(Creature performer, int tilex, int tiley, boolean onSurface, int tile)
			{
				if(isAllowed(performer) == false) {
					return null;
				}

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
			
			// targetted creature
			public boolean action(Action act, Creature performer, Creature target, short action, float counter)
			{
				if(performer.isOnSurface() == false || target.isOnSurface() == false) {
					performer.getCommunicator().sendNormalServerMessage("You can only do this above ground.");
					return true;
				}

				return action(act, performer, target.getTileX(), target.getTileY(), target.isOnSurface(), target.getCurrentTileNum(), action, counter);
			}

			// targetted creature
			public boolean action(Action act, Creature performer, Item source, Creature target, short action, float counter)
			{
				if(performer.isOnSurface() == false || target.isOnSurface() == false) {
					performer.getCommunicator().sendNormalServerMessage("You can only do this above ground.");
					return true;
				}

				return action(act, performer, target.getTileX(), target.getTileY(), target.isOnSurface(), target.getCurrentTileNum(), action, counter);
			}
			
			// targetted tile
			public boolean action(Action act, Creature performer, Item source, int tilex, int tiley, boolean onSurface, int heightOffset, int tile, short action, float counter)
			{
				action(act, performer, tilex, tiley, onSurface, tile, action, counter);
				return true;
			}
			
			// targetted tile
			public boolean action(Action act, Creature performer, int tilex, int tiley, boolean onSurface, int tile, short action, float counter)
			{
				if(isAllowed(performer) == false) {
					return true;
				}

				if (!performer.isPlayer() || performer.getVehicle() != -10) {
					return true;
				}
				
				if(performer.isOnSurface() == false) {
					performer.getCommunicator().sendNormalServerMessage("You can only do this above ground.");
					return true;
				}

				String playerEffect = performer.getName() + effectName;
				
				if(Mod.isTestEnv() == false && performer.getPower() < requiredGMlevel && Cooldowns.isOnCooldown(playerEffect, cooldown)) {
					performer.getCommunicator().sendNormalServerMessage("You did this too recently.");
					return true;
				}
				Cooldowns.setUsed(playerEffect);
				
				performer.getCommunicator().sendNormalServerMessage("Labyrinth!");

				// On entire area:
/*
2852                 if (!Methods.isActionAllowed(performer, 660, tilex, tiley)) {
2853                     return true;
2854                 }
*/

				Maze m = new Maze(tilex, tiley, 21, StructureConstantsEnum.HEDGE_FLOWER3_HIGH);
				//Maze m = new Maze(tilex, tiley, 21, FenceConstants.FENCE_PALISADE);
				//Maze m = new Maze(tilex, tiley, 21, (byte)126);	// 126 = magic wall
				m.create(true, false);
				
				return true;
			}

		}; // ActionPerformer
	}
}
