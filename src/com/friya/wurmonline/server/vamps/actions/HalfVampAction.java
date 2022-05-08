package com.friya.wurmonline.server.vamps.actions;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import com.friya.wurmonline.server.vamps.Stakers;
import com.friya.wurmonline.server.vamps.Vampires;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.questions.HalfVampQuestion;
import com.wurmonline.server.skills.SkillList;


public class HalfVampAction implements ModAction
{
	private static Logger logger = Logger.getLogger(HalfVampAction.class.getName());

	static public short actionId;
	private final ActionEntry actionEntry;
	int castTime = 10 * 3;			// * seconds
	
	public HalfVampAction()
	{
		logger.log(Level.INFO, "HalfVampAction()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"Offer Instructions",
			"offering",
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
				if(performer.isPlayer() == true && target.isPlayer() == false && target.getName().equals(Vampires.halfVampMakerName)) {
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
				if(performer.isPlayer() && target.isPlayer() == false && target.getName().equals(Vampires.halfVampMakerName)) {
					performer.getCommunicator().sendNormalServerMessage(Vampires.halfVampMakerName + " is looking for a papyrus sheet with some information about vampires...");
				}

				return true;
			}

			@Override
			public boolean action(Action act, Creature performer, Item activeItem, Creature target, short action, float counter)
			{
				if(performer.isPlayer() == false || target.isPlayer() || target == null || activeItem == null || target.getName().equals(Vampires.halfVampMakerName) == false) {
					return true;
				}
				
				if(performer.getPower() < 2) {
					if(activeItem.getTemplateId() == ItemList.papyrusSheet) {
						if(activeItem.getAuxData() != (byte)152) {
							performer.getCommunicator().sendNormalServerMessage("That's an interesting papyrus sheet, alas, not what I am looking for. I need information about the vampires.");
							return true;
						}
						// if we get here, it's the right papyrus sheet
					} else {
						performer.getCommunicator().sendNormalServerMessage("I need information about the vampires. I've heard of a papyrus sheet spreading through the world with information about them. Please bring that to me.");
						return true;
					}
	
					if(Vampires.isVampire(performer.getWurmId())) {
						performer.getCommunicator().sendNormalServerMessage("You are one of them, I can sense it. Do you know where to find ... him?");
						return true;
					}
	
					if(Stakers.isHunted(performer.getWurmId()) || Stakers.isWieldingStake(performer.getWurmId())) {
						performer.getCommunicator().sendNormalServerMessage("Go away. I don't trust you.");
						return true;
					}
	
					if(performer.getSkills().getSkillOrLearn(SkillList.GROUP_FIGHTING).getKnowledge() < 35.0f) {
						performer.getCommunicator().sendNormalServerMessage("You are clearly not experienced enough to be of any use to me. Come back when you have at least improved in fighting...");
						return true;
					}
				} else {
					performer.getCommunicator().sendNormalServerMessage("You are admin, skipping all checks.");
				}

				HalfVampQuestion aq = new HalfVampQuestion(
		        		performer,															// who
		        		"There are no such things as Vampires?",							// window title
		        		"I urge you to read carefully, " + performer.getName() + "...",		// header IN window
		        		performer.getWurmId()
		        );
		        aq.sendQuestion();
				
				return true;
			}
		}; // ActionPerformer
	}
}
