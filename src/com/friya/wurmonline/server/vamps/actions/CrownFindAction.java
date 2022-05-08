package com.friya.wurmonline.server.vamps.actions;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import com.friya.wurmonline.server.vamps.items.Crown;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.NoSuchActionException;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemsProxy;
import com.wurmonline.server.questions.PinpointHumanoidQuestion;


public class CrownFindAction implements ModAction
{
	private static Logger logger = Logger.getLogger(CrownFindAction.class.getName());

	static public short actionId = -10;
	private final ActionEntry actionEntry;
	private int castTime = 10 * 5;			// * seconds

	static public short getActionId()
	{
		return actionId;
	}
	

	public CrownFindAction()
	{
		logger.log(Level.INFO, "CrownFindAction()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"Find...",
			"finding",
			new int[] {		// 6 /* ACTION_TYPE_NOMOVE */
				6
			}
		);
		ModActions.registerAction(actionEntry);
	}


	@Override
	public BehaviourProvider getBehaviourProvider()
	{
		return new BehaviourProvider() {

			@Override
			public List<ActionEntry> getBehavioursFor(Creature performer, Item target)
			{
				return getBehavioursFor(performer, null, target);
			}
			
			@Override
			public List<ActionEntry> getBehavioursFor(Creature performer, Item source, Item target)
			{
				if(performer.isPlayer() && source != null && source.getTemplateId() == Crown.getId() && ItemsProxy.isWornAsArmour(source)) {
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
				return action(act, performer, null, target, action, counter);
			}

			@Override
			public boolean action(Action act, Creature performer, Item source, Item target, short action, float counter)
			{
				if(performer.isPlayer() == false || target.getTemplateId() != Crown.getId()) {
					return true;
				}
				
				if(source.getQualityLevel() < 99.0f) {
		            performer.getCommunicator().sendNormalServerMessage("Finding with the crown will only work if it is of absolute top quality. It's still nice, though!");
		            return true;
				}
				
				if(ItemsProxy.isWornAsArmour(source) == false) {
		            performer.getCommunicator().sendNormalServerMessage("You must be wearing the crown.");
		            return true;
				}

				try {
					if(counter == 1.0f) {
						int tmpTime = castTime;
						performer.getCurrentAction().setTimeLeft(tmpTime);
						performer.sendActionControl("Find human", true, tmpTime);
						return false;
					}
					
					if(counter * 10.0f <= act.getTimeLeft()) {
						return false;
					}
				} catch (NoSuchActionException e) {
					return true;
				}

				PinpointHumanoidQuestion aq = new PinpointHumanoidQuestion(
		        		performer, 
		        		"Find...", 
		        		"Who are you looking for?",
		        		-1,
		        		source.getWurmId()
		        );

				aq.extraQuestionNote = "Warning: Be careful, the one you are finding will get YOUR name and YOUR location when you use it!";

				aq.ignoreNoLo = true;		// ignore nolocate
				aq.reverseFind = true;		// will let the one found know where and who is trying to find you
		        aq.sendQuestion();

				return true;
			}
		}; // ActionPerformer
	}

}
