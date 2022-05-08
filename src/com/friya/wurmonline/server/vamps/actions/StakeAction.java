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
import com.friya.wurmonline.server.vamps.EventDispatcher;
import com.friya.wurmonline.server.vamps.Mod;
import com.friya.wurmonline.server.vamps.Stakers;
import com.friya.wurmonline.server.vamps.VampAchievements;
import com.friya.wurmonline.server.vamps.VampSkills;
import com.friya.wurmonline.server.vamps.VampTitles;
import com.friya.wurmonline.server.vamps.VampZones;
import com.friya.wurmonline.server.vamps.Vampires;
import com.friya.wurmonline.server.vamps.events.StakeRecoverEvent;
import com.friya.wurmonline.server.vamps.events.EventOnce.Unit;
import com.friya.wurmonline.server.vamps.events.RemoveBitableEvent;
import com.friya.wurmonline.server.vamps.events.RemoveEffectEvent;
import com.friya.wurmonline.server.vamps.items.Amulet;
import com.friya.wurmonline.server.vamps.items.Stake;
import com.friya.wurmonline.server.vamps.items.VampireFang;
import com.wurmonline.mesh.Tiles;
import com.wurmonline.server.FailedException;
import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.NoSuchPlayerException;
import com.wurmonline.server.Players;
import com.wurmonline.server.Point;
import com.wurmonline.server.Server;
import com.wurmonline.server.WurmCalendar;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemFactory;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.items.Materials;
import com.wurmonline.server.items.NoSpaceException;
import com.wurmonline.server.items.NoSuchTemplateException;
import com.wurmonline.server.players.Achievement;
import com.wurmonline.server.players.Achievements;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.skills.Affinities;
import com.wurmonline.server.skills.Affinity;
import com.wurmonline.server.skills.Skill;
import com.wurmonline.server.skills.SkillList;
import com.wurmonline.server.sounds.SoundPlayer;
import com.wurmonline.server.zones.NoSuchZoneException;
import com.wurmonline.server.zones.Zones;


public class StakeAction implements ModAction
{
	private static Logger logger = Logger.getLogger(StakeAction.class.getName());

	private final short actionId;
	private final ActionEntry actionEntry;
	private final static int STAMINA_COST = 4000;
	private final static int STAKE_ACTION_COUNT_REWARD = 2000;
	private final static float STAKE_ACTION_COUNT_CAP_MULTIPLIER = 0.25f;
	private final static float transactionModifier = 0.8f;			// means we will only let the slayer have 80% of the skill lost by the vampire

	
	public StakeAction()
	{
		logger.log(Level.INFO, "StakeAction()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"STAKE (beware)", 
			"staking",
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

			public List<ActionEntry> getBehavioursFor(Creature performer, Creature object)
			{
				// We want them to have a mallet activated, give them a handy error in the action...
				return this.getBehavioursFor(performer, null, object);
			}

			public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Creature target)
			{
				if(performer instanceof Player 
					&& (target instanceof Player || (performer.getPower() > 2))		// admins can stake NPC's 
					&& (
							(performer.getRighthandItem() != null && performer.getRighthandItem().getTemplateId() == Stake.getId())
						||  (performer.getLefthandItem() != null && performer.getLefthandItem().getTemplateId() == Stake.getId()))
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
			public boolean action(Action act, Creature performer, Creature object, short action, float counter)
			{
				if(performer.getRighthandItem() != null && performer.getRighthandItem().getTemplateId() == Stake.getId() || (performer.getLefthandItem() != null && performer.getLefthandItem().getTemplateId() == Stake.getId())) {
					return true;
				}

				Mod.actionNotify(
		    			performer,
		    			"You fumble a bit with the stake. You should probably activate a mallet. Embarrassing...",
		    			"%NAME fingers a pointed wooden stake.",
		    			"In the corner of your eye you see a shadowy figure moving."
	        	);
				
				return true;
			}


			@SuppressWarnings("unused")
			@Override
			public boolean action(Action act, Creature performer, Item source, Creature target, short action, float counter)
			{
				boolean isVampire;
				boolean undodgableStake = false;
				int redPillarSeconds = 600;
				
				Item stake;
				
				if(performer.getRighthandItem() != null && performer.getRighthandItem().getTemplateId() == Stake.getId()) {
					stake = performer.getRighthandItem();
				} else if(performer.getLefthandItem() != null && performer.getLefthandItem().getTemplateId() == Stake.getId()) {
					stake = performer.getLefthandItem();
				} else {
					//performer.getCommunicator().sendNormalServerMessage("Could not find a stake in either hand :/ This is probably a Friya booboo. Tell me.");
					//logger.log(Level.SEVERE, "Somehow staker managed to get to action without having a stake...");
					//throw new RuntimeException("Could not find a stake on performer");
					return true;
				}
				
				if(source.getTemplateId() != ItemList.hammerWood) {
					return action(act, performer, target, action, counter);
				}

				if(((Player)target).isInvulnerable()) {
					performer.getCommunicator().sendNormalServerMessage(target.getName() + " is still invulnerable.");
					return true;
				}

				if(performer.getPower() > 2) {
					// We allow this for testing purposes, everything is a vampire, including NPC's
					logger.log(Level.SEVERE, "WARNING: admins AND players with the name 'Staker' can stake anything due to testing!" );
					isVampire = true;
				} else if(target instanceof Player) {
					isVampire = Vampires.isVampire((Player)target);
				} else {
					// If you are not an admin and it's an NPC, then it's never a stakable vampire.
					isVampire = false;
				}

				// If vampire is within the coven focus zone, they are safe...
				if(VampZones.getCovenZone().covers(target.getTileX(), target.getTileY())) {
					performer.getCommunicator().sendNormalServerMessage("Fzzzt. The stake does not seem to work here.");
					return true;
				}
				
				if(performer.getSkills().getSkillOrLearn(SkillList.GROUP_FIGHTING).getKnowledge() < Stakers.STAKER_REQUIRED_FS) {
					performer.getCommunicator().sendNormalServerMessage("You cannot trifle with such power yet. You should at least go get a bit of experience in fighting.");
					return true;
				}

				if(stake.getAuxData() == Stake.STATUS_WIELDING) {
			    	Mod.actionNotify(
			    			performer,
			    			"The magical runes of the stake need a few seconds to settle before you can use it.",
			    			"%NAME swings a pointed stake around.",
			    			null
		        	);
					return true;
				}
 
				if(stake.getAuxData() == Stake.STATUS_RECOVERING) {
			    	Mod.actionNotify(
			    			performer,
			    			"You are still recovering from your previous attempt.",
			    			null,
			    			null
		        	);
					return true;
				}
				
				if(performer.getWurmId() == target.getWurmId()) {
			    	Mod.actionNotify(
			    			performer,
			    			"Stake yourself? Suicide is never the answer.",
			    			null,
			    			null
		        	);
			    	return true;
				}
				
				if(Vampires.isHalfOrFullVampire(performer.getWurmId()) && performer.getPower() <= 0) {
			    	Mod.actionNotify(
			    			performer,
			    			"You are a Vampire, unable to use this weapon in such a way.",
			    			null,
			    			null
		        	);
					return true;
				}
				
				if(Stakers.isHunted(performer)) {
			    	Mod.actionNotify(
			    			performer,
			    			"The blood on your hands prevent you from using its magical properties.",
			    			"%NAME handles a pointed wooden stake.",
			    			"A shadowy figure temporarily reveals %HIMSELF, but is gone before you could identify them."
		        	);
			    	return true;
				}

				if(performer.getVehicle() != -10) {
			    	Mod.actionNotify(
			    			performer,
			    			"You have to be on solid ground.",
			    			null,
			    			null
		        	);
					return true;
				}

				if(performer.getPower() <= 0 && target.getPower() > 0) {
			    	Mod.actionNotify(
			    			performer,
			    			"You would have to be a FOOL to trifle with such power.",
			    			"%NAME tries to injure " + target.getName() + ", but fails.",
			    			"%NAME tries to injure " + target.getName() + ", but fails."
		        	);
					return true;
				}

				// Allow some distance -- 1 tile inbetween (so action can reach from up to three tiles, inclusive)
				if (performer.isWithinTileDistanceTo(target.getTileX(), target.getTileY(), 0, 1) == false) {
					performer.getCommunicator().sendNormalServerMessage("That is too far away.");
					return true;
				}

				performer.getStatus().modifyStamina(-STAMINA_COST);

				performer.playAnimation("fight_strike", false);

				if(Vampires.isHalfVampire(target.getWurmId()) == true) {
			    	performer.addWoundOfType(
			    			null, 				// creature
			    			(byte)6, 			// woundType
			    			1,					// position (was 21)
			    			true, 				// randomize position (was false)
			    			0.0f, 				// armour mod (was 1.0)
			    			true, 				// calculate armour
			    			100000.0, 			// damage
			    			0.0f, 				// infection (was 10)
			    			0.0					// poison
			    	);

					performer.getCommunicator().sendAlertServerMessage("&*^#$*&^#!", (byte)4);

					Mod.actionNotify(
			    			performer,
			    			target.getName() + " is not a true vampire! " + target.getName() + " is only a half vampire for whom there is still hope in this world! The magic of the stake turns on you, punishing you for using it against its purpose!",
			    			"%NAME is burnt as %HIS stake flares up!",
			    			"%NAME is burnt as %HIS stake flares up!"
		        	);

			    	return true;
				}
				
				if(isVampire == false) {
					performer.getCommunicator().sendAlertServerMessage("&*^#$*&^#!", (byte)4);

					Mod.actionNotify(
			    			performer,
			    			"The magical runes of the stake light up, burning you!  That is not a Vampire!",
			    			"%NAME's stake lights up, burning %HIM!",
			    			"%NAME's stake lights up, burning %HIM!"
			    	);

					// Kill performer. Yep. Harsh.
					performer.die(false, "Friya's Curse");

					Items.destroyItem(stake.getWurmId());
					return true;
				}
				
				if(stake.getMaterial() == Materials.MATERIAL_SERYLL) {
					undodgableStake = true;
				}

				// Not sure if undodgable stakes should hit amulet or not... Now they do.
				try {
					if(target.getEquippedItem((byte)36).getTemplateId() == Amulet.getId()) {
						performer.getCommunicator().sendAlertServerMessage("BOOM!", (byte)4);

						Mod.actionNotify(
				    			performer,
				    			"Your stake strikes a solid ancient amulet lying over the vampire's heart. Both the stake and the amulet are consumed in a flash of fire!",
				    			"%NAME's stake is consumed in a flash of fire as it strikes a magical amulet over " + target.getName() + "'s heart!",
				    			"A stake is consumed in a flash of fire as it strikes a magical amulet over " + target.getName() + "'s heart!"
				    	);

						target.getCommunicator().sendAlertServerMessage("BOOM! Your amulet protects you!", (byte)4);
						target.getCommunicator().sendNormalServerMessage(
								"Your ancient amulet is consumed in a burst of fire as it is hit with a magical stake of vampire banishment wielded by " + performer.getName()
						);

						target.playAnimation("wounded", false);
						
						// Lightning when we hit amulet
						Zones.flash(target.getTileX(), target.getTileY(), false);

		    			Items.destroyItem(target.getEquippedItem((byte)36).getWurmId());
		    			Items.destroyItem(stake.getWurmId());

				    	Vampires.broadcast( Mod.fixActionString(performer, "%NAME has revealed %HIMSELF as a hunter by slamming a stake at an ancient amulet!"), true, false, true);
						Vampires.broadcast( Mod.fixActionString(performer, "They can be hunted, but this is not a full-length hunt and they can still stake you!"), true, true, false);

						performer.getCommunicator().sendAlertServerMessage("YOU HIT AN ANCIENT AMULET AND ARE HUNTED!", (byte)4);
						performer.getCommunicator().sendAlertServerMessage("You have blood on your hands for a *short* while. *All* Vampires can seek their revenge. Run!", (byte)4);
						performer.playPersonalSound("sound.spawn.item.central");

						Stakers.addBitable(performer.getWurmId());
						// Last argument here says that it should be announced to Coven (and hunter) when this duration is up.
						EventDispatcher.add(new RemoveBitableEvent(Vampires.AMULET_HIT_BITABLE_DURATION, Unit.SECONDS, performer.getWurmId(), true));
		    			return true;
					}
				} catch (NoSuchItemException | NoSpaceException e) { }

				// Skill & luck checks!
				if(undodgableStake == false) {
					double stakerDex = performer.getSkills().getSkillOrLearn(VampSkills.DEXTERITY).getKnowledge();
					double vampireDex = target.getSkills().getSkillOrLearn(VampSkills.DEXTERITY).getKnowledge();
					
					double stakerPerception = performer.getSkills().getSkillOrLearn(VampSkills.PERCEPTION).getKnowledge();
					double vampirePerception = target.getSkills().getSkillOrLearn(VampSkills.PERCEPTION).getKnowledge();
					
					double stakerStaminaPerCent = (double)(performer.getStatus().getStamina() + STAMINA_COST) / 65535f * 100f;
					double vampireStaminaPerCent = (double)target.getStatus().getStamina() / 65535f * 100f;

					double stakerPower  = (stakerDex  * 1.5) + (stakerStaminaPerCent  * 0.50) + (stakerPerception  * 0.75);
					double vampirePower = (vampireDex * 1.5) + (vampireStaminaPerCent * 0.50) + (vampirePerception * 0.75) + (WurmCalendar.isNight() ? 27.5f : 0f);

					// If vampire outskills staker, this will be a negative value.
					double skillCheck = (stakerPower - vampirePower);

					double rand = (double)Server.rand.nextInt(100);
					boolean dodged = (skillCheck < -50 || rand < 60) && rand > 4;

					DecimalFormat df = new DecimalFormat("#.##");
					logger.log(Level.INFO, "StakeAction: "
							+ (dodged ? "DODGED" : "STAKED")
							+ ", rand " + rand
							+ ", DEX " + df.format(stakerDex) + " vs Vamp's " + df.format(vampireDex)  
							+ ", PER " + df.format(stakerPerception) + " vs Vamp's " + df.format(vampirePerception)  
							+ ", STA " + df.format(stakerStaminaPerCent) + " vs Vamp's " + df.format(vampireStaminaPerCent)
							+ ", Staker power: " + df.format(stakerPower) + "; Vamp power: " + df.format(vampirePower) + "; skillCheck: " + df.format(skillCheck)
					);

					// FOR TESTING: set the false to true to always dodge
					if(dodged || (false && Mod.isTestEnv())) {
						// The vampire dodged!
						target.getCommunicator().sendAlertServerMessage("*dodge*", (byte)4);
						performer.getCommunicator().sendAlertServerMessage("*dodge*", (byte)4);
						Mod.actionNotify(
				    			performer,
				    			target.getName() + "'s sixth sense enables them to dodge your feeble attack.",
				    			"%NAME makes a weak attempt to stake " + target.getName() + " and misses.",
				    			null
				    	);

						target.getCommunicator().sendNormalServerMessage(
								performer.getName() + " makes an attempt to stake you! But your sixth sense allow you to dodge the blow."
						);

						target.playAnimation("wounded", false, performer.getWurmId());		// well, we don't seem to have any dodge...
						SoundPlayer.playSound("sound.combat.miss.heavy", target, 1.6f);
						
						// Put this stake on cooldown for a few seconds...
						EventDispatcher.add(new StakeRecoverEvent(3, Unit.SECONDS, performer, stake));
						return true;
					}
				} else {
					performer.getCommunicator().sendNormalServerMessage("The beast tried to dodge, but your seryll stake does the job.");
				}

				// Passed all checks, now target is actually being staked.
				Mod.actionNotify(
		    			performer,
		    			"You stake the vampire through the heart! The stake banishes it back to the realm of darkness and rewards you for your successful hunt!",
		    			"%NAME stakes " + target.getName() + " through the heart! "  + target.getName() + " is revealed as a vampire and banished to the realm of darkness!",
		    			"%NAME stakes " + target.getName() + " through the heart! "  + target.getName() + " is revealed as a vampire and banished to the realm of darkness!"
		    	);
		    	Mod.actionNotify(
		    			target,
		    			performer.getName() + " stakes you through the heart! You are punished for being caught by the mortal. You are banished from your material form.",
		    			null,
		    			null
		    	);

		    	performer.getCommunicator().sendNormalServerMessage("A bloody fang drops to the ground.");
				Server.getInstance().broadCastAction("A bloody fang drops to the ground.", performer, 6);

		    	Mod.actionNotify(
		    			target,
		    			"You turn to mist and flow back to the Coven.",
		    			target.getName() + " turns to mist and flows away.",
		    			null
		    	);

		    	// 15 = charge darkness, 16 = charge darkness (but eh, should be light i guess), 2 = white light pillar, 3 = black light pillar, 25 = red light pillar
				int tmpEffectId = Server.rand.nextInt(12345678) + 12345678;
				short effectNum = 25;
				Players.getInstance().sendGlobalNonPersistantEffect(tmpEffectId, effectNum, target.getTileX(), target.getTileY(), 
						Tiles.decodeHeightAsFloat(
								(int)Server.surfaceMesh.getTile(target.getTileX(), target.getTileY())
						)
				);
				EventDispatcher.add(new RemoveEffectEvent(redPillarSeconds, Unit.SECONDS, tmpEffectId));

				double vampireSkillLevelBefore = 1.0f;
				double stakerSkillLevelBefore = 1.0f;
				int exchangedStatNum = 0;
				String exchangedStatName = "";
				int actionCount = 0;
				double skillLoss = 0;
				double skillGain = 0;
				
				// Do skill gain transfer
		    	ActionSkillGain actionSkillGain = ActionSkillGains.getRandomHighSkillToPunish(target);
		    	if(actionSkillGain != null) {
			    	Skill vampireSkill = target.getSkills().getSkillOrLearn(actionSkillGain.getId());
			    	Skill stakerSkill = performer.getSkills().getSkillOrLearn(actionSkillGain.getId());

			    	vampireSkillLevelBefore = vampireSkill.getKnowledge();
			    	stakerSkillLevelBefore = stakerSkill.getKnowledge();
			    	exchangedStatNum = vampireSkill.getNumber();
			    	exchangedStatName = vampireSkill.getName();

			    	logger.log(Level.INFO, "Skill affected by stake: " + vampireSkill.getName());

					actionCount = actionSkillGain.getModifiedLostActionCount(vampireSkill.getKnowledge(), STAKE_ACTION_COUNT_REWARD, STAKE_ACTION_COUNT_CAP_MULTIPLIER);
					skillLoss = actionSkillGain.getRawSkillLossForActionCount(vampireSkill.getKnowledge(), actionCount);
					skillGain = actionSkillGain.getRawSkillGainForActionCount(stakerSkill.getKnowledge(), actionCount);

			    	logger.log(Level.INFO, "Skill loss for Vampire: " + skillLoss);
			    	logger.log(Level.INFO, "Skill gain for Staker: " + skillGain + " moved down to create a loss to system: " + (skillGain * transactionModifier));
			    	
			    	skillGain *= transactionModifier;
					DecimalFormat df = new DecimalFormat("#.####");

			    	vampireSkill.setKnowledge(vampireSkill.getKnowledge() - skillLoss, false, true);
			    	target.getCommunicator().sendNormalServerMessage("You have lost " + df.format(skillLoss) + " points in " + vampireSkill.getName() + " to " + performer.getName() + ".");

			    	stakerSkill.setKnowledge(stakerSkill.getKnowledge() + skillGain, false, false);
			    	performer.getCommunicator().sendNormalServerMessage("You have successfully taken " + df.format(skillGain) + " points in " + stakerSkill.getName() + ".");

			    	if(Server.rand.nextInt(100) < 25) {
			    		Affinity[] affs = Affinities.getAffinities(target.getWurmId());
			    		if(affs.length > 0) {
				    		Affinity aff = affs[Server.rand.nextInt((int)affs.length)];
				    		String affName = target.getSkills().getSkillOrLearn(aff.getSkillNumber()).getName();
	
				    		target.decreaseAffinity(aff.skillNumber, 1);
					    	target.getCommunicator().sendNormalServerMessage("You lost your affinity in " + affName + ".");
	
					    	performer.increaseAffinity(aff.skillNumber, 1);
					    	performer.getCommunicator().sendNormalServerMessage("You have gained an affinity in " + affName + ".");
					    	logger.log(Level.INFO, "Staker " + performer.getName() + " took affinity in " + affName + " from vampire (" + target.getName() + ")");
			    		}
			    	}

					// Target will get a bit of anatomy skill
					Skill anatomy = target.getSkills().getSkillOrLearn(VampSkills.ANATOMY);
					anatomy.skillCheck(1.0f, 0.0f, false, 1.0f);

		    	} else {
			    	performer.getCommunicator().sendNormalServerMessage("This poor bloodsucker had no skills to speak of...");
			    	logger.log(Level.SEVERE, "No suitable skill found for: " + target.getName());
		    	}
		    	
				// Vampire will "lose" a fang (no worries, they grow back) (we have already sent the message)
				Item item;
				try {
					item = ItemFactory.createItem(
						VampireFang.getId(),
						(float)vampireSkillLevelBefore,	// float ql - this sort of determines the value due to the ql being what Vampire had in the skill they lost
						(byte)0,					// byte aRarity, 
						null 
					);

					item.setMaterial(Materials.MATERIAL_ANIMAL);
					item.putItemInfrontof(performer);
				} catch (FailedException | NoSuchTemplateException | NoSuchCreatureException | NoSuchItemException | NoSuchPlayerException | NoSuchZoneException e) {
					logger.log(Level.SEVERE, "Could not drop Vampire fang", e);
				}

		    	target.getStatus().setStunned(2.0f);
				target.playAnimation("die", false);

		    	Vampires.broadcast( Mod.fixActionString(performer, "%NAME has revealed %HIMSELF as a vampire slayer!"), true, false, true);
				Vampires.broadcast( Mod.fixActionString(performer, "LET THE HUNT BEGIN!"), true, true, false);

				performer.getCommunicator().sendAlertServerMessage("YOU HAVE SLAIN A VAMPIRE!", (byte)4);
				performer.getCommunicator().sendAlertServerMessage("You have blood on your hands. *All* Vampires can seek their revenge. Run!", (byte)4);
				performer.playPersonalSound("sound.spawn.item.central");
				target.playPersonalSound("sound.spawn.item.central");

				// Achievement for staking and being staked.
		    	Achievements.triggerAchievement(performer.getWurmId(), VampAchievements.STAKINGS);
		    	Achievements.triggerAchievement(target.getWurmId(), VampAchievements.STAKED);

		    	if(VampTitles.hasTitle(performer, VampTitles.VAMPIRE_SLAYER) == false) {
		    		performer.addTitle(VampTitles.getTitle(VampTitles.VAMPIRE_SLAYER));
		    	}

		    	// 25 slain vampires
		    	Achievement stakings = Achievements.getAchievementObject(performer.getWurmId()).getAchievement(VampAchievements.STAKINGS);
		    	if(stakings.getCounter() == 25 && VampTitles.hasTitle(performer, VampTitles.VAN_HELSING) == false) {
		    		performer.addTitle(VampTitles.getTitle(VampTitles.VAN_HELSING));
		    	}

		    	Items.destroyItem(stake.getWurmId());
		    	
		    	Vampires.setStakedTeleportPosition(target.currentTile, redPillarSeconds);
		    	
		    	// Set bloodlust to 1
				Skill bl = target.getSkills().getSkillOrLearn(VampSkills.BLOODLUST);
				bl.setKnowledge(1f, false);

		    	// Move vampire to "the Coven" zone
		    	if(target instanceof Player) {
			    	Point loc = VampZones.getCovenRespawnPoint();
			    	target.setTeleportPoints((short)loc.getX(), (short)loc.getY(), VampZones.getCovenLayer(), 0);
			    	target.startTeleporting();
			    	target.getCommunicator().sendTeleport(false);
			    	target.setBridgeId(-10);
		    	} else {
			    	performer.getCommunicator().sendNormalServerMessage("Target is an NPC, not moving it to The Coven...");
		    	}

		    	// Create Staker entry (will also create a log of sorts)
		    	Stakers.createStaker(performer, target, exchangedStatNum, exchangedStatName, vampireSkillLevelBefore, skillLoss, actionCount, stakerSkillLevelBefore, skillGain);
		    	return true;
			}
		}; // ActionPerformer
	}
	
}
