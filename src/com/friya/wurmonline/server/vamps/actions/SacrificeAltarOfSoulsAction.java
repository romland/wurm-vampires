package com.friya.wurmonline.server.vamps.actions;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import com.friya.wurmonline.server.vamps.Vampires;
import com.friya.wurmonline.server.vamps.items.AltarOfSouls;
import com.wurmonline.server.Items;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.NoSuchActionException;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;


public class SacrificeAltarOfSoulsAction implements ModAction
{
	private static Logger logger = Logger.getLogger(SacrificeAltarOfSoulsAction.class.getName());

	private final short actionId;
	private final ActionEntry actionEntry;
	private final int castTime = 10 * 4;			// * seconds

	public SacrificeAltarOfSoulsAction()
	{
		logger.log(Level.INFO, "AltarOfSoulsSacrifice()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"Sacrifice", 
			"sacrificing",
			new int[] {		// 6 /* ACTION_TYPE_NOMOVE */
				6,
				23			// ACTION_TYPE_IGNORERANGE
			}
		);
		ModActions.registerAction(actionEntry);
	}


	@Override
	public BehaviourProvider getBehaviourProvider()
	{
		return new BehaviourProvider() {

			public List<ActionEntry> getBehavioursFor(Creature performer, Item target)
			{
				return this.getBehavioursFor(performer, null, target);
			}

			public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Item target)
			{
				if(target.getTemplateId() == AltarOfSouls.getId() && Vampires.isVampire(performer.getWurmId()) == true) {
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
			public boolean action(Action act, Creature performer, Creature object, short action, float counter)
			{
				return action(act, performer, null, object, action, counter);
			}


			@Override
			public boolean action(Action act, Creature performer, Item item, Item target, short action, float counter)
			{
				if(target.getTemplateId() != AltarOfSouls.getId()) {
					return true;
				}

				if(item == null || item.getTemplateId() != ItemList.corpse) {
					performer.getCommunicator().sendNormalServerMessage("You can only sacrifice corpses.");
					return true;
				}

				if(Vampires.isVampire(performer.getWurmId()) == false) {
					return true;
				}

				if(DevourAction.isDevourableCorpse(item) == false) {
					performer.getCommunicator().sendNormalServerMessage("Not enough nourishment in that.");
					return true;
				}
				
				if(AltarOfSouls.isCleanArea(target) == false) {
					performer.getCommunicator().sendNormalServerMessage("The area around the altar is too cluttered.");
					return true;
				}

				try {
					if(counter == 1.0f) {
						performer.getCurrentAction().setTimeLeft(castTime);
						performer.sendActionControl("Sacrificing", true, castTime);
						return false;
					}
					
					if(counter * 10.0f <= act.getTimeLeft()) {
						return false;
					}
				} catch (NoSuchActionException e) {
					return true;
				}

				performer.getCommunicator().sendNormalServerMessage("You sacrifice " + item.getName() + " at the Altar of Souls.");
				Items.destroyItem(item.getWurmId());

				//
				// Max charge is 127
				// 
				// 6 corpses = fully charged (at 21 charge)
				//
				// At 1/5th chance to decrease altar's charge:
				//		127 charges * 5 (chance) * 30 sec = 19,050 seconds = 317 minutes = 5 hours
				//
				AltarOfSouls.setCharge(target, (byte)(AltarOfSouls.getCharge(target) + 21));
				
				
		    	return true;
			}
		}; // ActionPerformer
	}
	
}
