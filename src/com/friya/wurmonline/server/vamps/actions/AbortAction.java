/**
 * Zenath concerns addressed (some forum post I wrote)
 * -------------------------
 * The balance of the game is already in the hands of us. Every tweak, every action timer, every gain, 
 * everything affects overall game balance. The Wurm we are playing here does not have a *designed* 
 * balance (like WO).
 * 
 * At the end of the day I guess you have to just trust us that *our* goal is to create an enjoyable 
 * and as far as possible, a well-balanced experience. Hopefully that is not be harder than, say, 
 * trusting that we will keep the server available and up.
 * 
 * I'm thrilled to have a dialog around everything vampire, any feedback is good and rest assured that 
 * due to your concern this is something I will pay extra attention to. The abortions done until today 
 * can probably be counted on one hand with a few lost fingers from a chainsaw.
 * 
 * If you'd ask the vampires around; of course, it's not easy to do so since they are not revealing 
 * themselves, but they will know that I'd rather give them the hard and painful ride than the easy 
 * one.
 * 
 * Let's discuss the specifics of this spell:
 * It has a cooldown of 23 hours. If the worry is that not everyone are equally good breeders, then 
 * you should probably worry more about the difference between having a Fo priest or not. 
 */
package com.friya.wurmonline.server.vamps.actions;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import com.friya.wurmonline.server.vamps.Cooldowns;
import com.friya.wurmonline.server.vamps.Mod;
import com.friya.wurmonline.server.vamps.VampSkills;
import com.friya.wurmonline.server.vamps.Vampires;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.NoSuchActionException;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreaturesProxy;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.skills.Skill;


public class AbortAction implements ModAction
{
	private static Logger logger = Logger.getLogger(AbortAction.class.getName());

	private final short actionId;
	private final ActionEntry actionEntry;
	private final int cooldown = 1000 * 60 * 60 * 23;		// * 23 hours (was 6 hours, for which I blamed Ceno)


	public AbortAction()
	{
		logger.log(Level.INFO, "AbortAction()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"Abort offspring", 
			"aborts offspring",
			new int[] { 6 }	// ACTION_TYPE_NOMOVE
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
				return this.getBehavioursFor(performer, null, object);
			}

			public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Creature target)
			{
				if(performer instanceof Player && target instanceof Creature && Vampires.isVampire(performer.getWurmId()) && target.isAnimal() && target.isPregnant()) {
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
			public boolean action(Action act, Creature performer, Item source, Creature target, short action, float counter)
			{
				int castTime = 10 * 10;		// 10ths of seconds
				String playerEffect = performer.getName() + "abortoffspring";
				
				if(performer instanceof Player == false || Vampires.isVampire(performer.getWurmId()) == false || target instanceof Creature == false || target.isAnimal() == false || target.isPregnant() == false) {
					return true;
				}

				if(Vampires.isVampire(performer.getWurmId()) == false && performer.getPower() < 1) {
					// vampire spell
					return true;
				}
				
				Skill anatomy = performer.getSkills().getSkillOrLearn(VampSkills.ANATOMY);
				if(anatomy.getKnowledge() < 30f) {
					performer.getCommunicator().sendNormalServerMessage("You need a bit more skill in anatomy for that...");
					return true;
				}

				if(Cooldowns.isOnCooldown(playerEffect, cooldown)) {
			    	performer.getCommunicator().sendNormalServerMessage("It's mentally exhausting, you will need to wait before you can do that again.");
					return true;
				}

				try {
					if(counter == 1.0f) {
						performer.getCommunicator().sendNormalServerMessage("You close your eyes and focus your senses the offspring of " + target.getName() + "...");
						performer.getCurrentAction().setTimeLeft(castTime);
						performer.sendActionControl("Aborting offspring", true, castTime);
						return false;
					}
					
					if(counter * 10.0f <= act.getTimeLeft()) {
						return false;
					}
				} catch (NoSuchActionException e) {
					return true;
				}

				// Make sure the animal is branded by the settlement of the vampire
				if(performer.getVillageId() <= 0 || target.isBranded() == false || target.isBrandedBy(performer.getVillageId()) == false) {
					performer.getCommunicator().sendNormalServerMessage(target.getName() + " is not branded by your settlement.");
					return true;
				}

				CreaturesProxy.deleteOffspringSettings(target.getWurmId());
				Mod.actionNotify(performer,
					"You magically and swiftly make the offspring of " + target.getName() + " vanish in a puff of smoke.",
					"%NAME mumbles some incoherent phrases and " + target.getName() + " seems healthier.",
					"A shadowy form mumbles some incoherent phrases and " + target.getName() + " seems healthier."
				);

				Cooldowns.setUsed(playerEffect);
				return true;
			}
		}; // ActionPerformer
	}
}
