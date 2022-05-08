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
import com.friya.wurmonline.server.vamps.items.Mirror;
import com.wurmonline.server.Items;
import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.NoSuchActionException;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
//import com.wurmonline.server.items.ItemFactory;
import com.wurmonline.server.items.ItemList;

public class PolishMirrorAction implements ModAction 
{
	private static Logger logger = Logger.getLogger(PolishMirrorAction.class.getName());

	private final short actionId;
	private final ActionEntry actionEntry;
	private final int castTime = 10 * 10;

	public PolishMirrorAction() {
		logger.log(Level.INFO, "PolishMirrorAction()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"Polish mirror", 
			"polishing",
			new int[] { 6 /* ACTION_TYPE_NOMOVE */ }
		);
		ModActions.registerAction(actionEntry);
	}


	@Override
	public BehaviourProvider getBehaviourProvider()
	{
		return new BehaviourProvider() {
			// Menu with activated object
			@Override
			public List<ActionEntry> getBehavioursFor(Creature performer, Item pelt, Item mirror)
			{
				if(pelt != null && mirror != null && pelt.getTemplateId() == ItemList.pelt && mirror.getTemplateId() == Mirror.getId()) {
					return Arrays.asList(actionEntry);
				}
				return null;
			}

			// Menu without activated object
			@Override
			public List<ActionEntry> getBehavioursFor(Creature performer, Item object)
			{
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
				return true;
			}
			
			@Override
			public boolean action(Action act, Creature performer, Item source, Item target, short action, float counter)
			{
				return polishMirror(act, performer, source, target, counter);
			}
		}; // ActionPerformer
	}


	private boolean polishMirror(Action act, Creature performer, Item pelt, Item mirror, float counter)
	{
		if(performer.isPlayer() == false || mirror == null || pelt == null || mirror.getTemplateId() != Mirror.getId() || pelt.getTemplateId() != ItemList.pelt) {
			return true;
		}
		
		if(mirror.getAuxData() == 0) {
	    	performer.getCommunicator().sendNormalServerMessage("The mirror is already as clean as it can be.");
	    	return true;
		}

		if(pelt.getCurrentQualityLevel() < 75.0f) {
	    	performer.getCommunicator().sendNormalServerMessage("That pelt is simply too low quality to clean this mess.");
	    	return true;
		}

		try {
			if(counter == 1.0f) {
				int tmpTime = castTime;
				performer.getCurrentAction().setTimeLeft(tmpTime);
				performer.sendActionControl("polishing", true, tmpTime);
				return false;
			}
			
			if(counter * 10.0f <= act.getTimeLeft()) {
				return false;
			}
		} catch (NoSuchActionException e) {
			return true;
		}

		Mod.actionNotify(
			performer, 
			"You polish the mirror to perfection. It can now be used again.",
			"%NAME looks proudly into a shiny silver mirror.",
			"In the corner of your eye you see a shiny reflection of something."
		);
		
		if(Server.rand.nextInt(100) < 80) {
	    	performer.getCommunicator().sendNormalServerMessage("The pelt disappears in a puff of smoke.");
			Items.destroyItem(pelt.getWurmId());
		}
		
		mirror.setAuxData((byte)0);
		
		return true;
	}
}
