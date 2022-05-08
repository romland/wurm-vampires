package com.friya.wurmonline.server.vamps.actions;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import com.friya.wurmonline.server.vamps.VampSkills;
import com.friya.wurmonline.server.vamps.Vampires;
import com.friya.wurmonline.server.vamps.items.SmallRat;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.NoSuchActionException;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.questions.AidQuestion;


public class AidAction implements ModAction
{
	private static Logger logger = Logger.getLogger(AidAction.class.getName());

	static public short actionId = -10;
	private final ActionEntry actionEntry;
	private int castTime = 10 * 100;			// * seconds -- but is reduced with 'aiding'

	static public short getActionId()
	{
		return actionId;
	}
	
	public AidAction()
	{
		logger.log(Level.INFO, "AidAction()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"Aid a vampire",
			"aiding",
			new int[] {		// ACTION_TYPE_NOMOVE
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
				if(performer.isPlayer() && Vampires.isVampire(performer.getWurmId()) && target.getTemplateId() == SmallRat.getId()) {
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
				if(performer.isPlayer() == false || Vampires.isVampire(performer.getWurmId()) == false || target.getTemplateId() != SmallRat.getId()) {
					return true;
				}

				try {
					if(counter == 1.0f) {
						int tmpTime = castTime - ((int)performer.getSkills().getSkillOrLearn(VampSkills.AIDING).getKnowledge());
						performer.getCurrentAction().setTimeLeft(tmpTime);
						performer.sendActionControl("Aiding vampire", true, tmpTime);
						return false;
					}
					
					if(counter * 10.0f <= act.getTimeLeft()) {
						return false;
					}
				} catch (NoSuchActionException e) {
					return true;
				}

				AidQuestion aq = new AidQuestion(
		        		performer, 
		        		"Aid...", 
		        		"To whom would you like to send this rat?",
		        		performer.getWurmId(), 
		        		100,
		        		target
		        );
		        aq.sendQuestion();

				return true;
			}
		}; // ActionPerformer
	}

}
