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
import com.friya.wurmonline.server.vamps.items.Amulet;
import com.friya.wurmonline.server.vamps.items.Stake;
import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.NoSuchPlayerException;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.NoSuchActionException;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.Materials;
import com.wurmonline.server.zones.NoSuchZoneException;

public class MakeSeryllStakeAction implements ModAction 
{
	private static Logger logger = Logger.getLogger(MakeSeryllStakeAction.class.getName());

	private final short actionId;
	private final ActionEntry actionEntry;
	private final int castTime = 10 * 30;

	public MakeSeryllStakeAction() {
		logger.log(Level.INFO, "MakeSeryllStakeAction()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"Merge with Amulet", 
			"merging",
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
			public List<ActionEntry> getBehavioursFor(Creature performer, Item amulet, Item stake)
			{
				if(amulet != null && stake != null && amulet.getTemplateId() == Amulet.getId() && stake.getTemplateId() == Stake.getId()) {
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
				return makeSeryllStake(act, performer, source, target, counter);
			}
		}; // ActionPerformer
	}


	private boolean makeSeryllStake(Action act, Creature performer, Item amulet, Item stake, float counter)
	{
		if(performer.isPlayer() == false || stake == null || amulet == null || stake.getTemplateId() != Stake.getId() || amulet.getTemplateId() != Amulet.getId()) {
			return true;
		}
		
		try {
			if(counter == 1.0f) {
				int tmpTime = castTime;
				performer.getCurrentAction().setTimeLeft(tmpTime);
				performer.sendActionControl("merging", true, tmpTime);
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
			"In a puff of smoke you merge the amulet with the stake, making it absolutely amazing.",
			"%NAME fiddles with an ancient amulet and a stake of vampire banishment.",
			"A shadow fiddles with an ancient amulet and a stake of vampire banishment."
		);

		// Whoops, need to make sure they don't get 3kg seryll
		stake.setWeight(amulet.getWeightGrams(), true);

		// destroy amulet
		Items.destroyItem(amulet.getWurmId());
		
		// make material of stake seryll
		stake.setMaterial(Materials.MATERIAL_SERYLL);
		stake.updateName();

		try {
			// Eh, we have to do this cheap trick to get it to update the material :(
			performer.getCommunicator().sendAlertServerMessage("Whoops! In the excitement you accidentally drop it on the ground!", (byte)4);
			stake.putItemInfrontof(performer);
		} catch (NoSuchCreatureException | NoSuchItemException | NoSuchPlayerException | NoSuchZoneException e) {
			e.printStackTrace();
		}
		
		return true;
	}
}
