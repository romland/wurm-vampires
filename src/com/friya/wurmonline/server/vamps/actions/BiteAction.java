package com.friya.wurmonline.server.vamps.actions;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import com.friya.wurmonline.server.vamps.ActionSkillGain;
import com.friya.wurmonline.server.vamps.ActionSkillGains;
import com.friya.wurmonline.server.vamps.Cooldowns;
import com.friya.wurmonline.server.vamps.Mod;
import com.friya.wurmonline.server.vamps.Staker;
import com.friya.wurmonline.server.vamps.Stakers;
import com.friya.wurmonline.server.vamps.VampSkills;
import com.friya.wurmonline.server.vamps.Vampires;
import com.wurmonline.server.NoSuchPlayerException;
import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.skills.Affinities;
import com.wurmonline.server.skills.Affinity;
import com.wurmonline.server.skills.Skill;
import com.wurmonline.server.skills.SkillList;


public class BiteAction implements ModAction
{
	private static Logger logger = Logger.getLogger(BiteAction.class.getName());

	private static short actionId;
	private final ActionEntry actionEntry;
	private final String effectName = "bite";
	private final int cooldown = 1000 * 7;			// * seconds

	static public short getActionId()
	{
		return actionId;
	}
	
	public BiteAction()
	{
		logger.log(Level.INFO, "BiteAction()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"BITE (beware)", 
			"biting",
			new int[] {		// 6 ACTION_TYPE_NOMOVE
				6,
				23			// ACTION_TYPE_IGNORERANGE
			}
		);
		ModActions.registerAction(actionEntry);
	}


	@Override
	public BehaviourProvider getBehaviourProvider()
	{
		return new BehaviourProvider() {

			public List<ActionEntry> getBehavioursFor(Creature performer, Creature object)
			{
				// We want them to have a mallet activated, give them a handy error in the action...
				return this.getBehavioursFor(performer, null, object);
			}

			public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Creature target)
			{
				if(Vampires.isVampire(performer.getWurmId()) && target.isPlayer()) {
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
			public boolean action(Action act, Creature performer, Creature object, short action, float counter)
			{
				return action(act, performer, null, object, action, counter);
			}


			@Override
			public boolean action(Action act, Creature performer, Item source, Creature target, short action, float counter)
			{
				if(Vampires.isVampire(performer.getWurmId()) == false) {
					return true;
				}
				
				if(target.isPlayer() == false && performer.getPower() <= 1) {
					return true;
				}

				// Allow some distance -- 1 tile inbetween (so action can reach from up to three tiles, inclusive)
				if (performer.isWithinTileDistanceTo(target.getTileX(), target.getTileY(), 0, 1) == false) {
					performer.getCommunicator().sendNormalServerMessage("That is too far away.");
					return true;
				}

				if(target.getSkills().getSkillOrLearn(SkillList.GROUP_FIGHTING).getKnowledge() < Stakers.STAKER_REQUIRED_FS) {
					performer.getCommunicator().sendNormalServerMessage(target.getName() + " is just too inexperienced to even pose a threat.");
					return true;
				}

				String playerEffect = performer.getName() + effectName;

				if(Cooldowns.isOnCooldown(playerEffect, cooldown)) {
			    	performer.getCommunicator().sendNormalServerMessage("You are temporarily satiated and can draw no more blood for a little while.");
					return true;
				}
				
				if(Stakers.isHunted(target) == false && Stakers.mayPunish(target.getWurmId()) == false) {
					performer.getCommunicator().sendAlertServerMessage("OUCH!", (byte)4);
			    	Mod.actionNotify(performer,
			    		"You reel in pain! You cannot go around and feed on ordinary citizens! You have been warned!",
			    		"%NAME reels in pain, screaming loudly!",
			    		"The shadowy form of a vampire reels in pain, screaming loudly!"
			    	);

			    	// Remove a bit of skill from vampire...
			    	ActionSkillGain actionSkillGain = ActionSkillGains.getRandomSkillToPunish(target);
			    	if(actionSkillGain != null) {
				    	Skill vampireSkill = performer.getSkills().getSkillOrLearn(actionSkillGain.getId());
						int actionCount = actionSkillGain.getModifiedLostActionCount(vampireSkill.getKnowledge(), 40, 0.05f);
						double skillLoss = actionSkillGain.getRawSkillLossForActionCount(vampireSkill.getKnowledge(), actionCount);
						vampireSkill.setKnowledge(vampireSkill.getKnowledge() - skillLoss, false, true);
			    	}

			    	if(performer.getPower() < 3) {
			    		return true;
			    	} else {
			    		logger.log(Level.INFO, "Admin bit something illegal, continuing past illegal check (might throw errors after this)...");
			    	}
				}
				
				
				if(performer.getWurmId() == target.getWurmId()) {
			    	performer.getCommunicator().sendNormalServerMessage("Yourself? Really?");
					return true;
				}

				Staker staker = null;
				long staking = -1;

				if(Stakers.isStaker(target.getWurmId())) {
					try {
						staker = Stakers.getStaker(target.getWurmId());
						staking = staker.getId();
					} catch(NoSuchPlayerException e) {
						// This should not be able to happen
						e.printStackTrace();
					}
				} else {
					// This happens when a player gets bitten without them having staked yet. That is,
					// they may still be wielding a stake or having just been disarmed.
					logger.info("Bitten target " + target.getName() + " is not (yet) a marked staker");
				}
		    	
		    	if(staker != null && staker.mayBite() == false) {
			    	performer.getCommunicator().sendNormalServerMessage("This poor slayer's been sucked dry.");
					return true;
		    	}

				Cooldowns.setUsed(playerEffect);
				performer.getStatus().modifyStamina((int)( performer.getStatus().getStamina() * 0.8f ));
				
				double dodgeChance = Math.min(20, target.getSkills().getSkillOrLearn(VampSkills.DEXTERITY).getKnowledge() + target.getSkills().getSkillOrLearn(VampSkills.PERCEPTION).getKnowledge());  
				if(Server.rand.nextInt(100) < dodgeChance) {
			    	Mod.actionNotify(performer,
			    		"You attempt to pierce " + target.getName() + "'s neck with your fangs, but " + (target.isNotFemale() ? "he" : "she") + " dodges.",
			    		"%NAME attacks " + target.getName() + "'s neck with %HIS lethal fangs bared, but " + (target.isNotFemale() ? "he" : "she") + " dodges!",
			    		"The shadowy form of a vampire makes " + target.getName() + " quickly move out of its way!",
			    		new Creature[]{target}
			    	);

					if(performer.isStealth()) {
						target.getCommunicator().sendAlertServerMessage(Mod.fixActionString(performer, "The shadowy form of a vampire lunges at your neck with %HIS lethal fangs bared, you dodge!"), (byte)4);
					} else {
						target.getCommunicator().sendAlertServerMessage(Mod.fixActionString(performer, "%NAME lunges at your neck with %HIS lethal fangs bared, you dodge!"), (byte)4);
					}

					return true;
				}
				
				double slayerSkillLevelBefore = 1.0f;
				double vampireSkillLevelBefore = 1.0f;
				int exchangedStatNum = 0;
				String exchangedStatName = "";
				int actionCount = 0;
				double skillLoss = 0;
				double skillGain = 0;
				float transactionModifier = 0.8f;		// means we will only let the vampire have 80% of the skill lost by the slayer
				
				// Do skill gain transfer
		    	//ActionSkillGain actionSkillGain = ActionSkillGains.getRandomSkillToPunish(target);
				ActionSkillGain actionSkillGain;
				if(staker != null) {
					actionSkillGain = ActionSkillGains.getRandomHighSkillToPunish(target, staker.getAffectedSkill());
				} else {
					// If they are not actually a hunted staker, we will not prioritize the skill they stole
					actionSkillGain = ActionSkillGains.getRandomHighSkillToPunish(target);
				}
		    	if(actionSkillGain != null) {
			    	Skill stakerSkill = target.getSkills().getSkillOrLearn(actionSkillGain.getId());
			    	Skill vampireSkill = performer.getSkills().getSkillOrLearn(actionSkillGain.getId());

			    	slayerSkillLevelBefore = stakerSkill.getKnowledge();
			    	vampireSkillLevelBefore = vampireSkill.getKnowledge();
			    	exchangedStatNum = stakerSkill.getNumber();
			    	exchangedStatName = stakerSkill.getName();

			    	logger.log(Level.INFO, "Skill affected by bite: " + vampireSkill.getName());
			    	
					Skill bl = performer.getSkills().getSkillOrLearn(VampSkills.BLOODLUST);
					int bloodlustBonus = 0;
					if(bl.getKnowledge() > 85) {
						// 10% bonus
						bloodlustBonus = (int)(Vampires.BITE_ACTION_COUNT_REWARD / 10);
					}
					
					actionCount = actionSkillGain.getModifiedLostActionCount(stakerSkill.getKnowledge(), bloodlustBonus + Vampires.BITE_ACTION_COUNT_REWARD, 0.05f);
					
					if(Stakers.isHunted(target.getWurmId()) && actionCount > 0) {
						actionCount += (actionCount / 3);
					}

					skillLoss = actionSkillGain.getRawSkillLossForActionCount(stakerSkill.getKnowledge(), actionCount);
					skillGain = actionSkillGain.getRawSkillGainForActionCount(vampireSkill.getKnowledge(), actionCount);

			    	logger.log(Level.INFO, "Skill loss for Staker: " + skillLoss);
			    	logger.log(Level.INFO, "Skill gain for Vampire: " + skillGain + " moved down to 80%: " + (skillGain * transactionModifier));

			    	skillGain *= transactionModifier;
					DecimalFormat df = new DecimalFormat("#.####");

					stakerSkill.setKnowledge(stakerSkill.getKnowledge() - skillLoss, false, true);
			    	target.getCommunicator().sendNormalServerMessage("You have lost " + df.format(skillLoss) + " points in " + exchangedStatName + " to " + performer.getName() + ".");

			    	vampireSkill.setKnowledge(vampireSkill.getKnowledge() + skillGain, false, false);

			    	//performer.getCommunicator().sendNormalServerMessage("You have successfully taken " + df.format(skillGain) + " points in " + exchangedStatName + ".");
					performer.getCommunicator().sendAlertServerMessage(Mod.fixActionString(performer, "You have taken " + df.format(skillGain) + " points in " + exchangedStatName + " from " + target.getName() + "."), (byte)4);

			    	if(Server.rand.nextInt(100) < 9) {
			    		Affinity[] affs = Affinities.getAffinities(target.getWurmId());
			    		if(affs.length > 0) {
				    		Affinity aff = affs[Server.rand.nextInt((int)affs.length)];
				    		String affName = target.getSkills().getSkillOrLearn(aff.getSkillNumber()).getName();
	
				    		target.decreaseAffinity(aff.skillNumber, 1);
					    	target.getCommunicator().sendNormalServerMessage("You lost your affinity in " + affName + ".");
	
					    	performer.increaseAffinity(aff.skillNumber, 1);
					    	performer.getCommunicator().sendNormalServerMessage("You have gained an affinity in " + affName + ".");

					    	logger.log(Level.INFO, "Vampire " + performer.getName() + " took affinity in " + affName + " from slayer (" + target.getName() + ")");
			    		}
			    	}

					// Vampire will get a bit of anatomy skill
					Skill anatomy = performer.getSkills().getSkillOrLearn(VampSkills.ANATOMY);
					anatomy.skillCheck(1.0f, 0.0f, false, 1.0f);

					// As will the staker...
					//anatomy = target.getSkills().getSkillOrLearn(VampSkills.ANATOMY);
					//anatomy.skillCheck(1.0f, 0.0f, false, 1.0f);

		    	} else {
			    	performer.getCommunicator().sendNormalServerMessage("This poor slayer had no skills to speak of...");
			    	logger.log(Level.SEVERE, "No suitable skill found for: " + target.getName());
		    	}

		    	Mod.actionNotify(performer,
		    		"You pierce " + target.getName() + "'s neck with your fangs, feeding on the vital life force.",
		    		"%NAME pierces " + target.getName() + "'s neck with %HIS lethal fangs!",
		    		"The shadowy form of a vampire pierces " + target.getName() + "'s neck with %HIS  lethal fangs!",
		    		new Creature[]{target}
		    	);
		    	
				target.playAnimation("wounded", false, performer.getWurmId());

				if(performer.isStealth()) {
					target.getCommunicator().sendAlertServerMessage(Mod.fixActionString(performer, "The shadowy form of a vampire pierces your neck with %HIS lethal fangs, feeding on your vital lifeforce!"), (byte)4);
				} else {
					target.getCommunicator().sendAlertServerMessage(Mod.fixActionString(performer, "%NAME pierces your neck with %HIS lethal fangs, feeding on your vital lifeforce!"), (byte)4);
				}
				
		    	if(staker != null) {
		    		staker.addBitten();
		    	}

				// store in db
		    	Vampires.createBite(performer, target, exchangedStatNum, exchangedStatName, vampireSkillLevelBefore, skillLoss, actionCount, slayerSkillLevelBefore, skillGain, staking);

		    	return true;
			}
		}; // ActionPerformer
	}
	
}
