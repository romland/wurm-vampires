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
import com.friya.wurmonline.server.vamps.EventDispatcher;
import com.friya.wurmonline.server.vamps.Mod;
import com.friya.wurmonline.server.vamps.Stakers;
import com.friya.wurmonline.server.vamps.VampSkills;
import com.friya.wurmonline.server.vamps.Vampires;
import com.friya.wurmonline.server.vamps.events.EventOnce.Unit;
import com.friya.wurmonline.server.vamps.events.RemoveBitableEvent;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.NoSuchPlayerException;
import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.NoSuchActionException;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.skills.Skill;
import com.wurmonline.server.sounds.SoundPlayer;
import com.wurmonline.server.zones.NoSuchZoneException;


public class DisarmAction implements ModAction
{
	private static Logger logger = Logger.getLogger(DisarmAction.class.getName());

	static private short actionId;
	private final ActionEntry actionEntry;
	private final String effectName = "disarmHunter";
	private final int cooldown = 1000 * 10;					// 10 seconds
	private final int castTime = 10 * 2;					// 10ths of seconds
	static public short getActionId()
	{
		return actionId;
	}
	

	public DisarmAction()
	{
		logger.log(Level.INFO, "CrippleAction()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId,
			"Disarm Hunter",
			"disarming",
			new int[] {
				6,			// ACTION_TYPE_NOMOVE
				23			// ACTION_TYPE_IGNORERANGE
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
				if(performer.isPlayer() && target.isPlayer() && Vampires.isVampire(performer.getWurmId())) {
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
				if(performer instanceof Player == false || target instanceof Creature == false || Vampires.isVampire(performer.getWurmId()) == false) {
					return true;
				}

				// 1 tile range for this action (3 inclusive)
				if (performer.isWithinTileDistanceTo(target.getTileX(), target.getTileY(), 0, 1) == false) {
					performer.getCommunicator().sendNormalServerMessage("That is too far away.");
					return true;
				}

				String playerEffect = performer.getName() + effectName;

				if(Cooldowns.isOnCooldown(playerEffect, cooldown)) {
			    	performer.getCommunicator().sendNormalServerMessage("You are still recovering from your previous disarm.");
					return true;
				}
				
				try {
					if(counter == 1.0f) {
						performer.getCurrentAction().setTimeLeft(castTime);
						performer.sendActionControl("Disarming Hunter", true, castTime);
						return false;
					}
					
					if(counter * 10.0f <= act.getTimeLeft()) {
						return false;
					}
				} catch (NoSuchActionException e) {
					return true;
				}
				
				// Incur some cost: 50% of current stamina
				performer.getStatus().modifyStamina((int)( performer.getStatus().getStamina() * 0.5f ));

				double rand = (double)Server.rand.nextInt(100);
				double stakerDex = target.getSkills().getSkillOrLearn(VampSkills.DEXTERITY).getKnowledge();
				double vampireDex = performer.getSkills().getSkillOrLearn(VampSkills.DEXTERITY).getKnowledge();
				double vampireDisarming = performer.getSkills().getSkillOrLearn(VampSkills.DISARMING).getKnowledge();
				
				// Disarming skill helps vampire
				vampireDex += Math.min(100.0, vampireDisarming / 10 + vampireDex);
				
				double dexCheck = Math.min(vampireDex / stakerDex * 90, 90.0);							// dex difference contributes a max of 90% chance to land a disarm

				if(dexCheck < rand) {
					// The slayer dodged!
					Mod.actionNotify(
			    			performer,
			    			target.getName() + "'s sixth sense enables them to dodge your disarming attempt.",
			    			"%NAME makes a weak attempt to disarm " + target.getName() + " and fails.",
			    			null,
			    			new Creature[]{performer, target}
			    	);

					target.getCommunicator().sendNormalServerMessage(
							performer.getName() + " makes an attempt to disarm you! But you dodge."
					);

					target.playAnimation("wounded", false, performer.getWurmId());		// well, we don't seem to have any dodge...
					SoundPlayer.playSound("sound.combat.miss.heavy", target, 1.6f);
					
					return true;
				}
				
				try {
					Item stake = Stakers.getWieldedStake(target);
					if(stake == null) {
				    	performer.getCommunicator().sendNormalServerMessage("They are not wielding a stake, someone must have beaten you to it.");
						return true;
					}
					
					stake.putItemInfrontof(target);
					target.getCommunicator().sendNormalServerMessage("You were disarmed" + (performer.isStealth() ? "." : (" by " + performer.getName() + ".")) + " The stake lands on the ground.");
			    	performer.getCommunicator().sendNormalServerMessage("You disarm " + target.getName() + ", the stake lands on the ground.");
					
					Stakers.addBitable(target.getWurmId());
					EventDispatcher.add(new RemoveBitableEvent(Vampires.DISARM_BITABLE_DURATION, Unit.SECONDS, target.getWurmId()));

					Skill sk = performer.getSkills().getSkillOrLearn(VampSkills.DISARMING);
					sk.skillCheck(1.0f, 0.0f, false, 1.0f);
				} catch (NoSuchCreatureException | NoSuchItemException | NoSuchPlayerException | NoSuchZoneException e) {
			    	performer.getCommunicator().sendNormalServerMessage("Error in the fabric of space when disarming, please let an admin know.");
					e.printStackTrace();
				}

				return true;
			}
		}; // ActionPerformer
	}


}
