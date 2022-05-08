package com.friya.wurmonline.server.vamps.actions;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import com.friya.wurmonline.server.vamps.EventDispatcher;
import com.friya.wurmonline.server.vamps.Stakers;
import com.friya.wurmonline.server.vamps.Vampires;
import com.friya.wurmonline.server.vamps.events.DelayedMessage;
import com.friya.wurmonline.server.vamps.events.DelayedVamp;
import com.friya.wurmonline.server.vamps.events.EventOnce.Unit;
import com.friya.wurmonline.server.vamps.items.HalfVampireClue;
import com.wurmonline.server.Items;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.NoSuchActionException;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.players.Player;


public class SacrificeOrlokAction implements ModAction
{
	private static Logger logger = Logger.getLogger(SacrificeOrlokAction.class.getName());

	private final short actionId;
	private final ActionEntry actionEntry;
	int castTime = 10 * 60;			// * seconds
	
	public SacrificeOrlokAction()
	{
		logger.log(Level.INFO, "SacrificeAction()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"Sacrifice Corpse",
			"sacrificing",
			new int[] {
				6		// 6 ACTION_TYPE_NOMOVE 
			}	
		);
		ModActions.registerAction(actionEntry);
	}


	@Override
	public BehaviourProvider getBehaviourProvider()
	{
		return new BehaviourProvider() {

			@Override
			public List<ActionEntry> getBehavioursFor(Creature performer, Creature object)
			{
				// We want them to have a corpse activated, give them a handy error in the action...
				return this.getBehavioursFor(performer, null, object);
			}

			public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Creature target)
			{
				if(performer.isPlayer() == true && target.isPlayer() == false && target.getName().equals(Vampires.headVampireName)) {
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
			public boolean action(Action act, Creature performer, Creature target, short action, float counter)
			{
				if(performer instanceof Player && target instanceof Creature && target.getName().equals(Vampires.headVampireName)) {
					performer.getCommunicator().sendNormalServerMessage("You will need a fresh corpse of something worthy.");
				}

				return true;
			}

			@Override
			public boolean action(Action act, Creature performer, Item activeItem, Creature target, short action, float counter)
			{
				// MUST have this or keybinds-mod will work even though you are nowhere near the NPC 
				if(performer.isPlayer() == false || target == null || activeItem == null || target.getName().equals(Vampires.headVampireName) == false) {
					return true;
				}
				
				if(Stakers.isHunted(performer.getWurmId())) {
					performer.getCommunicator().sendNormalServerMessage("Grinning, " + Vampires.headVampireName + " says: You are an enemy of my bretheren, slayer. You will not be accepted among them until you petition them for their permission.");
					return true;
				}

				if(Vampires.isVampire(performer.getWurmId())) {
					performer.getCommunicator().sendNormalServerMessage("You have already proven yourself worthy.");
					return true;
				}
				
				if(Vampires.isHalfVampire(performer.getWurmId()) == false) {
					performer.getCommunicator().sendNormalServerMessage("Who do you think you are? You are a mere human. Go away.");
					return true;
				}

				if(activeItem.getTemplateId() != ItemList.corpse) {
					performer.getCommunicator().sendNormalServerMessage("You will need a fresh corpse of something worthy.");
					return true;
				}

				if(activeItem.getName().contains("bloodless husk") == true) {
					performer.getCommunicator().sendNormalServerMessage("Some leftovers that you fed on? Something fresh!");
					return true;
				}

				try {
					if(counter == 1.0f) {
						int tmpTime = (performer.getPower() > 0 ? 20 : castTime);
						performer.getCurrentAction().setTimeLeft(tmpTime);
						performer.sendActionControl("Sacrificing Corpse", true, tmpTime);
						return false;
					}
					
					if(counter * 10.0f <= act.getTimeLeft()) {
						return false;
					}
				} catch (NoSuchActionException e) {
					return true;
				}

				if(activeItem.getName().contains(" champion ") == false) {
					performer.getCommunicator().sendNormalServerMessage("That is simply not good enough...");
					return true;
				}

				// destroy Dhampira's clue -- if they are still carrying it
				Item clue = performer.getInventory().findItem(HalfVampireClue.getId(), true);
				if(clue != null) {
					Items.destroyItem(clue.getWurmId());
				}

				// Destroy the corpse
				Items.destroyItem(activeItem.getWurmId());
				
            	performer.getStatus().setStunned(21.0f);

				EventDispatcher.add(new DelayedMessage(1, Unit.SECONDS, performer, "Uh oh..."));
                EventDispatcher.add(new DelayedMessage(3, Unit.SECONDS, performer, Vampires.headVampireName + " smiles, bestowing his unholy blessing upon you."));
                EventDispatcher.add(new DelayedMessage(8, Unit.SECONDS, performer, "Slashing his wrist, he feeds you from his own blood."));
                EventDispatcher.add(new DelayedMessage(12, Unit.SECONDS, performer, "You become a complete vampire in all respects."));
                EventDispatcher.add(new DelayedMessage(16, Unit.SECONDS, performer, "Powers that you did not know even existed course through your veins."));
                EventDispatcher.add(
                		new DelayedVamp(
                				20, 
                				Unit.SECONDS, 
                				performer, 
                				"You have awakened to the life of a true vampire."
                		)
                );

				return true;
			}
		}; // ActionPerformer
	}
}
