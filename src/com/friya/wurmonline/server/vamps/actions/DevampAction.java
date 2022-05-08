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
import com.friya.wurmonline.server.vamps.items.HalfVampireClue;
import com.wurmonline.server.Items;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.questions.DeVampQuestion;


public class DevampAction implements ModAction
{
	private static Logger logger = Logger.getLogger(DevampAction.class.getName());

	static public short actionId;
	private final ActionEntry actionEntry;
	
	public DevampAction()
	{
		logger.log(Level.INFO, "DevampAction()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"Talk about vampires",
			"talking",
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
				if(performer.isPlayer() == true && target.isPlayer() == false && target.getName().equals(Vampires.deVampManName)) {
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
				return action(act, performer, null, target, action, counter);
			}

			@Override
			public boolean action(Action act, Creature performer, Item activeItem, Creature target, short action, float counter)
			{
				if(performer.isPlayer() == false || target == null || target.getName().equals(Vampires.deVampManName) == false) {
					return true;
				}
				
				if(Vampires.isHalfOrFullVampire(performer.getWurmId()) == false) {
					performer.getCommunicator().sendNormalServerMessage("They are around, you just don't see them. You may come back ... later.");
					return true;
				}
				
				// destroy Dhampira's clue -- if they are still carrying it
				Item clue = performer.getInventory().findItem(HalfVampireClue.getId(), true);
				if(clue != null) {
					Items.destroyItem(clue.getWurmId());
				}

				DeVampQuestion aq = new DeVampQuestion(
		        		performer, 
		        		"Talk about vampires...", 
		        		"Are you sure you want to get rid of your bloodlust?",
		        		performer.getWurmId()
		        );
		        aq.sendQuestion();

				return true;
			}
		}; // ActionPerformer
	}
}
