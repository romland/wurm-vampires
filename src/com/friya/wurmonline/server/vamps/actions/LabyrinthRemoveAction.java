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
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.shared.constants.StructureConstantsEnum;


public class LabyrinthRemoveAction implements ModAction
{
	private static Logger logger = Logger.getLogger(LabyrinthRemoveAction.class.getName());

	static private short actionId;
	private final ActionEntry actionEntry;
	
	static public short getActionId()
	{
		return actionId;
	}
	

	public LabyrinthRemoveAction()
	{
		logger.log(Level.INFO, "SprintAction()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"Remove labyrinth", 
			"removing",
			new int[] {
				23, // ACTION_TYPE_IGNORERANGE
				29, // ACTION_TYPE_BLOCKED_NONE
				
			}
		);
		ModActions.registerAction(actionEntry);
	}


	@Override
	public BehaviourProvider getBehaviourProvider()
	{
		return new BehaviourProvider() {

			public List<ActionEntry> getBehavioursFor(Creature performer, int tilex, int tiley, boolean onSurface, int tile, int dir)
			{
				if(performer.getPower() >= LabyrinthAction.requiredGMlevel) {
					return Arrays.asList(actionEntry);
				}
				return null;
			}

			public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, int tilex, int tiley, boolean onSurface, int tile)
			{
				if(performer.getPower() >= LabyrinthAction.requiredGMlevel) {
					return Arrays.asList(actionEntry);
				}
				return null;
			}

			@Override
			public List<ActionEntry> getBehavioursFor(Creature performer, int tilex, int tiley, boolean onSurface, int tile)
			{
				if(performer.getPower() >= LabyrinthAction.requiredGMlevel) {
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
			
			public boolean action(Action act, Creature performer, Item source, int tilex, int tiley, boolean onSurface, int heightOffset, int tile, short action, float counter) {
				action(act, performer, tilex, tiley, onSurface, tile, action, counter);
				return true;
			}
			
			public boolean action(Action act, Creature performer, int tilex, int tiley, boolean onSurface, int tile, short action, float counter)
			{
				if(performer.getPower() < LabyrinthAction.requiredGMlevel) {
					return true;
				}

				performer.getCommunicator().sendNormalServerMessage("Remove Labyrinth!");
				
				//Maze m = new Maze(tilex, tiley, 60, FenceConstants.FENCE_PALISADE);
				Maze m = new Maze(tilex, tiley, 60, StructureConstantsEnum.FENCE_MAGIC_STONE);
				m.clear();
				
				return true;
			}

		}; // ActionPerformer
	}
}
