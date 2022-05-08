package com.friya.wurmonline.server.vamps.actions;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import com.friya.wurmonline.server.vamps.Mod;
import com.friya.wurmonline.server.vamps.VampZones;
import com.friya.wurmonline.server.vamps.Vampires;
import com.wurmonline.server.Point;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.NoSuchActionException;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;

public class FlyAction implements ModAction 
{
	private static Logger logger = Logger.getLogger(FlyAction.class.getName());

	static private short actionId;
	private final ActionEntry actionEntry;
	private final int castTime = 10 * 300;		// 5 minute cast

	static public short getActionId()
	{
		return actionId;
	}
	

	public FlyAction() {
		logger.log(Level.INFO, "FlyAction()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"Fly to the Coven",
			"teleporting",
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
						int tmpTime = (Mod.isTestEnv() ? 50 : castTime);
						if(performer.getPower() > 2) {
							tmpTime = 20;	// 2 seconds
						}
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
				
		    	Point loc = VampZones.getCovenRespawnPoint();
		    	performer.setTeleportPoints((short)loc.getX(), (short)loc.getY(), VampZones.getCovenLayer(), 0);
		    	if(!performer.startTeleporting()) {
					performer.getCommunicator().sendNormalServerMessage("Fzzzt!");
		    		return true;
		    	}
		    	performer.getCommunicator().sendTeleport(false);
		    	performer.setBridgeId(-10);
		    	performer.teleport(true);		// destroyVisionArea
		    	//performer.stopTeleporting();	// teleport() calls this
				
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
