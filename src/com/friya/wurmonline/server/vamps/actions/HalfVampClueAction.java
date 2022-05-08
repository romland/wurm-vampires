package com.friya.wurmonline.server.vamps.actions;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import com.friya.wurmonline.server.vamps.items.HalfVampireClue;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.HalfVampClueQuestion;

public class HalfVampClueAction implements ModAction 
{
	private static Logger logger = Logger.getLogger(HalfVampClueAction.class.getName());

	static public short actionId;
	private final ActionEntry actionEntry;

	public HalfVampClueAction() {
		logger.log(Level.INFO, "HalfVampClueAction()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"Take a closer look...", 
			"studying",
			new int[] { 6 }	// 6 ACTION_TYPE_NOMOVE
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
				if(performer instanceof Player && object != null && object.getTemplateId() == HalfVampireClue.getId()) {
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
				return study(act, performer, target, counter);
			}
			
			@Override
			public boolean action(Action act, Creature performer, Item source, Item target, short action, float counter)
			{
				return this.action(act, performer, target, action, counter);
			}
		}; // ActionPerformer
	}


	private boolean study(Action act, Creature performer, Item target, float counter)
	{
		if(performer.isPlayer() == false || target == null || target.getTemplateId() != HalfVampireClue.getId()) {
			return true;
		}

		HalfVampClueQuestion aq = new HalfVampClueQuestion(
        		performer,																// who
        		"This is the best clue I have so far, " + performer.getName() + "...",	// window title
        		"",
        		performer.getWurmId()
        );
        aq.sendQuestion();
		
		return true;
	}
}
