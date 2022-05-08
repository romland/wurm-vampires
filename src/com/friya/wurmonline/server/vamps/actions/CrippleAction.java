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
import com.friya.wurmonline.server.vamps.Stakers;
import com.friya.wurmonline.server.vamps.VampSkills;
import com.friya.wurmonline.server.vamps.Vampires;
import com.friya.wurmonline.server.vamps.events.EventOnce.Unit;
import com.friya.wurmonline.server.vamps.events.RemoveModifierEvent;
import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.NoSuchActionException;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.SpellEffectsEnum;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.modifiers.DoubleValueModifier;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.skills.Skill;


public class CrippleAction implements ModAction
{
	private static Logger logger = Logger.getLogger(CrippleAction.class.getName());

	static private short actionId;
	private final ActionEntry actionEntry;
	private final int DURATION = 30;
	private final String effectName = "crippleLiving";
	private final int cooldown = 1000 * 120;				// 120 seconds
	private final int castTime = 10 * 3;					// 10ths of seconds - so 3 seconds

	static public short getActionId()
	{
		return actionId;
	}
	

	public CrippleAction()
	{
		logger.log(Level.INFO, "CrippleAction()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId,
			"Cripple Living",
			"crippling living",
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
				// TODO: Should MAYBE include a wielder and wielder's mount here (but need to be careful so we don't get NPE's)
				if(performer.isPlayer() && Vampires.isVampire(performer.getWurmId()) 
						&& (Stakers.isHunted(target) || Stakers.isHuntedMount(target))
					) {
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
				Creature crippleTarget = target;

				if(performer instanceof Player == false || crippleTarget instanceof Creature == false || Vampires.isVampire(performer.getWurmId()) == false) {
					return true;
				}

				// 20 tile range for this action
				if (performer.isWithinTileDistanceTo(crippleTarget.getTileX(), crippleTarget.getTileY(), 0, 20) == false) {
					performer.getCommunicator().sendNormalServerMessage("That is too far away.");
					return true;
				}

				String playerEffect = performer.getName() + effectName;

				if(Cooldowns.isOnCooldown(playerEffect, cooldown)) {
			    	performer.getCommunicator().sendNormalServerMessage("You are still recovering from your previous cast.");
					return true;
				}
				
				boolean targetIsHuntedMount = false;
				
				if(performer.getPower() < 2) {
					// Standard (non-admin) behaviour
					targetIsHuntedMount = Stakers.isHuntedMount(crippleTarget);
					
					if(Stakers.isHunted(crippleTarget) == false && targetIsHuntedMount == false) {
						performer.getCommunicator().sendNormalServerMessage("You can only cast Cripple Living on hunted slayers or their mounts.");
						return true;
					}
				} else {
					// Make it easy for admins to test.
					targetIsHuntedMount = crippleTarget.isVehicle();
				}
				
				try {
					if(counter == 1.0f) {
						performer.getCommunicator().sendNormalServerMessage("You start casting Cripple Living...");
						performer.getCurrentAction().setTimeLeft(castTime);
						performer.sendActionControl("Crippling Living", true, castTime);
						return false;
					}
					
					if(counter * 10.0f <= act.getTimeLeft()) {
						return false;
					}
				} catch (NoSuchActionException e) {
					return true;
				}
				
				double successChance = Math.min(90, (performer.getSkills().getSkillOrLearn(VampSkills.CRIPPLING).getKnowledge() * 2));
				if(Server.rand.nextInt(100) < successChance) {
					performer.getCommunicator().sendNormalServerMessage("Your Cripple Living failed...");
					return true;
				}
				

				DoubleValueModifier slowMod = new DoubleValueModifier(7, -0.40);		// 0.25 is original wound
				crippleTarget.getMovementScheme().addModifier(slowMod);
				EventDispatcher.add(new RemoveModifierEvent(DURATION, Unit.SECONDS, crippleTarget, slowMod, SpellEffectsEnum.WOUNDMOVE));

				performer.getCommunicator().sendNormalServerMessage("You slow down " + crippleTarget.getName() + " with Cripple Living.");
				if (crippleTarget.isPlayer()) {
					crippleTarget.getCommunicator().sendAddSpellEffect(SpellEffectsEnum.WOUNDMOVE, DURATION * 1000, 100.0f);		// SpellEffectsEnum effect, int duration, float power
					crippleTarget.getCommunicator().sendNormalServerMessage("You are slowed down by " + (performer.isStealth() ? "" : (performer.getName() + "'s ")) + "Cripple Living.");
				}
				
				if(targetIsHuntedMount) {
					crippleTarget.getCommunicator().sendNormalServerMessage("Your mount is affected by " + (performer.isStealth() ? "" : (performer.getName() + "'s ")) + "Cripple Living.");
				}
				
				Skill sk = performer.getSkills().getSkillOrLearn(VampSkills.CRIPPLING);
				sk.skillCheck(1.0f, 0.0f, false, 1.0f);

				return true;

			}
		}; // ActionPerformer
	}


}
