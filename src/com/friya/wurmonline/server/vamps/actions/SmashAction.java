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
import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.ActionTypesProxy;
import com.wurmonline.server.behaviours.CreatureBehaviour;
import com.wurmonline.server.behaviours.NoSuchActionException;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.skills.Skill;


public class SmashAction implements ModAction, ActionTypesProxy
{
	private static Logger logger = Logger.getLogger(SmashAction.class.getName());

	static private short actionId;
	private final ActionEntry actionEntry;
	int castTime = 10 * 3;							// * second(s)
	private final String effectName = "smash";
	private final int cooldown = 1000 * 5;			// * seconds
	private final int staminaCost = 15000;
	private boolean canMiss = false;
	
	static public short getActionId()
	{
		return actionId;
	}
	
	public SmashAction()
	{
		logger.log(Level.INFO, "SmashAction()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"Smash", 
			"smashing",
			new int[] {
				ACTION_TYPE_NOMOVE,
				ACTION_TYPE_ATTACK,
				ACTION_TYPE_ENEMY_ALWAYS
//				ACTION_TYPE_SHOW_ON_SELECT_BAR,
//				ACTION_TYPE_SPELL
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
				return this.getBehavioursFor(performer, null, object);
			}

			public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Creature target)
			{
				// Yep, does not work on players.
				if(Vampires.isVampire(performer.getWurmId()) && performer.isPlayer() == true && target.isPlayer() == false) {
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
				String playerEffect = performer.getName() + effectName;

				// Yep, does not work on players.
				if(Vampires.isVampire(performer.getWurmId()) == false || performer.isPlayer() == false || target.isPlayer() == true) {
					return true;
				}

				if (target.getWurmId() == performer.getWurmId() || target.getKingdomId() == performer.getKingdomId() || target.getPower() >= 2) {
					performer.getCommunicator().sendNormalServerMessage("That would be frowned upon.");
					return true;
				}

				if(Cooldowns.isOnCooldown(playerEffect, cooldown)) {
			    	performer.getCommunicator().sendNormalServerMessage("You need to wait a little while. ");
					return true;
				}
				
				if(performer.isFighting()) {
			    	performer.getCommunicator().sendNormalServerMessage("You can't use smash while fighting.");
					return true;
				}

				try {
					if(counter == 1.0f) {
						performer.getCurrentAction().setTimeLeft(castTime);
						performer.sendActionControl("Smashing", true, castTime);
						return false;
					}
					
					if(counter * 10.0f <= act.getTimeLeft()) {
						return false;
					}
				} catch (NoSuchActionException e) {
					return true;
				}

				Cooldowns.setUsed(playerEffect);

				if(performer.mayAttack(target) == false) {
					performer.getCommunicator().sendNormalServerMessage("You may not attack that.");
					return true;
				}
				
				double dex = performer.getSkills().getSkillOrLearn(VampSkills.DEXTERITY).getKnowledge();

				if(dex < 30.0f) {
					performer.getCommunicator().sendNormalServerMessage("You still lack the dexterity to do that efficiently.");
					return true;
				}

				if(performer.getStatus().getStamina() > staminaCost) { 
					performer.getStatus().modifyStamina(performer.getStatus().getStamina() - staminaCost);
				} else {
			    	performer.getCommunicator().sendNormalServerMessage("You are too tired.");
					return true;
				}

				if(canMiss) {
					// This is here in case we want smash to work IN combat too
					double dexCheck = Math.min(95, 30 + (dex * 0.70));
					int rndCheck = Server.rand.nextInt(100);
					logger.log(Level.INFO, "smash(): rnd " + rndCheck + " > dex " + dexCheck + " == " + (rndCheck > dexCheck ? "" : "not ") + "miss!");
					if(rndCheck > dexCheck) {
						Mod.actionNotify(
								performer,
								"You swing viciously, but miss.",
								"%NAME swings viciously, but miss.",
								"A shadowy form swings viciously, but miss"
						);
						return true;
					}
				}

				try {
					double bloodLust = performer.getSkills().getSkill(VampSkills.BLOODLUST).getKnowledge();

					double power = 23 + (bloodLust * 0.77f);
					double damage = 20000.0 + 13000.0 * (power / 100.0);

					// give bonus for doing from stealth
					damage *= (performer.isStealth() ? 1.1f : 1.0f);
					
					byte pos = target.getBody().getRandomWoundPos();

					/*
					 * Wound types:
					 * 	 4	fire
					 * 	 6	rot
					 * 	 7	drown
					 * 	 8	cold
					 * 	 9	internal
					 * 	10	acid
					 * 	any	physical
					 * 	
					 * 
					 */
					target.addWoundOfType(
							performer, 			// attacker
							(byte)0, 			// woundtype (0 = not a specific type, but ... physical, I suppose)
							pos, 				// int pos
							false, 				// random pos 
							1.0f, 				// armour mod
							true, 				// calculate armour
							damage				// damage
					);

					Mod.actionNotify(
							performer,
							"You smash the mortal viciously with preternatural strength!",
							"%NAME smashes " + target.getName() + " viciously!",
							"A shadowy form smashes " + target.getName() + " viciously!"
					);

					// Start combat with the target.
					target.setStealth(false);
					boolean done = performer.getCombatHandler().attack(target, Server.getCombatCounter(), false, counter, act);
					CreatureBehaviour.setOpponent(performer, target, done, act);

					Skill s = target.getSkills().getSkillOrLearn(VampSkills.DEXTERITY);
					s.skillCheck(1.0f, 0.0f, false, 1.0f);

				} catch (Exception e) {
					logger.log(Level.SEVERE, "failed to smash", e);
					return true;
				}

				return true;
			}
		}; // ActionPerformer
	}

}
