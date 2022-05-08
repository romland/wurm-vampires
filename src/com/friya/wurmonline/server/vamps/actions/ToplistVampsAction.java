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
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.questions.ToplistVampsQuestion;

public class ToplistVampsAction implements ModAction 
{
	private static Logger logger = Logger.getLogger(ToplistVampsAction.class.getName());

	static private short actionId;
	private final ActionEntry actionEntry;

	static public short getActionId()
	{
		return actionId;
	}
	

	public ToplistVampsAction() {
		logger.log(Level.INFO, "ToplistVampsAction()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"Top Vampires",
			"checking",
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
				
				ToplistVampsQuestion aq = new ToplistVampsQuestion(
		        		performer,											// who
		        		"Highest rated vampires",							// window title
		        		"",
		        		performer.getWurmId()
		        );
		        aq.sendQuestion();
				
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
