package com.friya.wurmonline.server.vamps.actions;

import java.util.Arrays;

import java.util.zip.CRC32;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import com.friya.wurmonline.server.vamps.BloodlessHusk;
import com.friya.wurmonline.server.vamps.Mod;
import com.friya.wurmonline.server.vamps.VampAchievements;
import com.friya.wurmonline.server.vamps.VampSkills;
import com.friya.wurmonline.server.vamps.Vampires;
import com.friya.wurmonline.server.vamps.items.SmallRat;
import com.wurmonline.server.Items;
import com.wurmonline.server.WurmCalendar;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.Methods;
import com.wurmonline.server.behaviours.MethodsItems;
import com.wurmonline.server.behaviours.NoSuchActionException;
import com.wurmonline.server.bodys.Wound;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureStatus;
import com.wurmonline.server.creatures.CreaturesProxy;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.players.Achievements;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.skills.AffinitiesTimed;
import com.wurmonline.server.skills.Skill;
import com.wurmonline.server.skills.SkillSystem;
import com.wurmonline.server.skills.SkillTemplate;
import com.wurmonline.server.skills.Skills;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;


public class DevourAction implements ModAction 
{
	private static Logger logger = Logger.getLogger(DevourAction.class.getName());

	static private short actionId;
	private final ActionEntry actionEntry;
	private final int castTime = 10 * 3;

	static public short getActionId()
	{
		return actionId;
	}
	

	public DevourAction() {
		logger.log(Level.INFO, "DevourAction()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"Devour", 
			"devouring",
			new int[] { 6 }	// 6 /* ACTION_TYPE_NOMOVE */
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
				if(performer instanceof Player && object != null && (object.getTemplateId() == ItemList.corpse || object.getTemplateId() == SmallRat.getId())) {
					if(Vampires.isHalfOrFullVampire(performer.getWurmId())) {
						return Arrays.asList(actionEntry);
					}
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
				return devour(act, performer, target, counter);
			}
			
			@Override
			public boolean action(Action act, Creature performer, Item source, Item target, short action, float counter)
			{
				return this.action(act, performer, target, action, counter);
			}
		}; // ActionPerformer
	}


	/**
	 * Note: Does not take age of corpse into consideration.
	 * 
	 * @param corpse
	 * @return
	 */
	static boolean isDevourableCorpse(Item corpse)
	{
		if(corpse.getName().toLowerCase().contains(" zombie ") 
				|| corpse.getName().contains("bloodless husk")
				|| corpse.getModelName().contains("towerguard")
				|| corpse.getModelName().startsWith("model.corpse.guard")
				|| corpse.getModelName().startsWith("model.corpse.salesman")
				|| corpse.getModelName().startsWith("model.corpse.human")
			) {
			return false;
		}

		return true;
	}
	

	private boolean devour(Action act, Creature performer, Item target, float counter)
	{
		boolean isSmallRat = (target.getTemplateId() == SmallRat.getId());
		long corpseAge = 0;
		
		if(!(performer instanceof Player)) {
			return true;
		}
		
		if(Vampires.isHalfOrFullVampire(performer.getWurmId()) == false) {
			return true;
		}
		
		// maybe look at model name? model.corpse.guardbrutal, model.corpse.guardtough, *towerguard*
		if(isDevourableCorpse(target) == false) {
			performer.getCommunicator().sendNormalServerMessage("Sadly, little nourishment there.");
			return true;
		}
		
		
		if (target.getTopParentOrNull() != performer.getInventory() && !Methods.isActionAllowed(performer, (short) 120, target)) {	// butcher = action 120
			performer.getCommunicator().sendNormalServerMessage("You are not allowed to do that here.");
			return true;
		}
		
		if(MethodsItems.isLootableBy(performer, target) == false) {
			performer.getCommunicator().sendNormalServerMessage("You are not allowed to do that.");
			return true;
		}
		
		try {
			if(counter == 1.0f) {
				int tmpTime = castTime;
				performer.getCurrentAction().setTimeLeft(tmpTime);
				performer.sendActionControl("Feeding", true, tmpTime);
				performer.getCommunicator().sendNormalServerMessage("You pull the corpse to your mouth to suck the soul from it...");
				return false;
			}
			
			if(counter * 10.0f <= act.getTimeLeft()) {
				return false;
			}
		} catch (NoSuchActionException e) {
			performer.getCommunicator().sendNormalServerMessage("Hrrm. Did not seem to help at all.");
			return true;
		}

		if(isSmallRat == false && BloodlessHusk.isBloodlessHusk(target)) {
			Mod.actionNotify(
				performer,
				"You can't get anything nourishing from a bloodless husk. It is ... bloodless.",
				"%NAME lets out a snarl, frowning with disappointment.",
				"You hear a snarl, seemingly from out of nowhere."
			);
			return true;
		}
		
		if(isSmallRat == false && target.isButchered()) {
			Mod.actionNotify(
				performer,
				"You take a quick peek at the remains and realize that you will not get much nourishment from this.",
				"%NAME eagerly looks at the butchered remains, but come to %HIS senses.",
				"You hear a soft snarl from the direction of the butchered remains."
			);
			return true;
		}

		// Note: age is in Wurm seconds!
		if(isSmallRat) {
			corpseAge = 1;
		} else {
			corpseAge = (WurmCalendar.getCurrentTime() - target.creationDate);
		}

		Skills ss = performer.getSkills();
		double bloodLust = ss.getSkillOrLearn(VampSkills.BLOODLUST).getKnowledge();
		double anatomy = ss.getSkillOrLearn(VampSkills.ANATOMY).getKnowledge();
		long ageMod = corpseAge / 60;			// WURM minutes
		
		if(ageMod < 10) {
			ageMod = 100;
		} else if(ageMod < 20) {
			ageMod = 90;
		} else if(ageMod < 30) {
			ageMod = 80;
		} else if(ageMod < 40) {
			ageMod = 60;
		} else if(ageMod < 50) {
			ageMod = 40;
		} else if(ageMod < 60) {
			ageMod = 30;
		} else if(ageMod < 70) {
			ageMod = 20;
		} else if(ageMod < 80) {
			ageMod = 10;
		} else if(ageMod < 120) {
			ageMod = 5;
		} else if(ageMod < 480) {
			ageMod = 1;
		} else {
			performer.getCommunicator().sendNormalServerMessage("Upon closer inspection, that corpse is just too old to give you any nourishment.");
			return true;
		}

		int feedStrength = (int)(bloodLust + anatomy + ageMod);

		// Make low bloodlust be a bit more dominant.
		if(bloodLust < 15 && feedStrength > 79) {
			feedStrength = 79;
		}
		
		if(Vampires.isHalfVampire(performer.getWurmId())) {
			feedStrength *= 0.4;
		}
		
		/*
		logger.log(Level.INFO, "currentTime: " + WurmCalendar.getCurrentTime());
		logger.log(Level.INFO, "creationDate: " + target.creationDate);
		logger.log(Level.INFO, "corpseAge: " + corpseAge);
		logger.log(Level.INFO, "ageMod: " + ageMod);
		logger.log(Level.INFO, "anatomy: " + anatomy);
		logger.log(Level.INFO, "bloodLust: " + bloodLust);
		logger.log(Level.INFO, "Feed strength: " + feedStrength);
		logger.log(Level.INFO, "Feed strength/100: " + (feedStrength / 100));
		*/
		
		Mod.actionNotify(
			performer,
			"It satisfies your vampiric blood lust temporarily. " + 
			(feedStrength < 100 ? "You gain some little nourishment from the dead, cooling blood." : "You are strengthened by devouring its soul."),
			"%NAME sucks the life out of a corpse, tearing its throat out in the process!",
			"A shadowy form sucks the life out of a corpse, tearing its throat out in the process!"
		);
		
		// corpse-name hash
		CRC32 crc = new CRC32();
		crc.update(target.getName().getBytes());
		long corpseHash = crc.getValue();
		
		if(isSmallRat) {
			// Rats are just simply destroyed.
			Items.destroyItem(target.getWurmId());
		} else {
			// to get bloodsucker from corpse: Players.getInstance().getNameFor(BloodlessHusk.getBloodSucker(corpseItem))
			BloodlessHusk.setBloodSucker(target, performer.getWurmId());	// (long)((Player)performer).getSaveFile().getPlayerId()

			VolaTile t = Zones.getTileOrNull(target.getTilePos(), target.isOnSurface());
			target.setName(target.getName().replace("corpse", "bloodless husk"));
			if(t != null) {
				t.renameItem(target);
			}
			target.setButchered();
		}
		
		boolean success = feed(performer, feedStrength);
		
		if(!success) {
			performer.getCommunicator().sendNormalServerMessage("Uh oh. Something is off with that one, you spit it all out.");
			return true;
		}
		
		// set temporary affinities
		if(bloodLust >= 20) {
			addTimedAffinityFromBonus(performer, corpseHash);
		}
		
		if(bloodLust >= 70) {
			addTimedAffinityFromBonus(performer, corpseHash + 10);
		}

		if(bloodLust >= 90) {
			addTimedAffinityFromBonus(performer, corpseHash + 20);
		}
		
		if(bloodLust >= 95) {
			addTimedAffinityFromBonus(performer, corpseHash + 30);
		}
		
		
		performer.getSkills().getSkillOrLearn(VampSkills.ANATOMY).skillCheck(1.0f, 0.0f, false, 1.0f);
    	Achievements.triggerAchievement(performer.getWurmId(), VampAchievements.FEED);

		return true;
	}


	public static void addTimedAffinityFromBonus(Creature creature, long corpseHash) //int weight, Item item)
    {
        if (!creature.isPlayer()) {
            return;
        }

        int ibonus = 122; //item.getBonus();	// ItemMealData byte this.bonus

        ibonus = (int)((long)ibonus + (corpseHash & 255));
        ibonus = (int)((long)ibonus + (corpseHash >>> 8 & 255));
        ibonus = (int)((long)ibonus + (corpseHash >>> 16 & 255));
        ibonus = (int)((long)ibonus + (corpseHash >>> 24 & 255));
        ibonus = (int)((long)ibonus + (corpseHash >>> 32 & 255));
        ibonus = (int)((long)ibonus + (corpseHash >>> 40 & 255));
        ibonus = (int)((long)ibonus + (corpseHash >>> 48 & 255));
        ibonus = (int)((long)ibonus + (corpseHash >>> 56 & 255));
        
        SkillTemplate skillTemplate = SkillSystem.getSkillTemplateByIndex(ibonus = (ibonus & 255) % SkillSystem.getNumberOfSkillTemplates());
        if (skillTemplate == null) {
            return;
        }

        int skillId = skillTemplate.getNumber();

        int duration = (int)60 * 120;	// seconds

        AffinitiesTimed at = AffinitiesTimed.getTimedAffinitiesByPlayer(creature.getWurmId(), true);
        
        at.add(skillId, duration);

        creature.getCommunicator().sendNormalServerMessage("You realize that you have more of an insight about " + skillTemplate.getName().toLowerCase() + "!", (byte)2);
        at.sendTimedAffinity(creature, skillTemplate.getNumber());
    }   

	
	
	private boolean feed(Creature p, int feedStr)
	{
		// With bloodlust, anatomy, ageMod, range is: 2-300.
		// 200-250 would be somewhat common (since max bloodlust and corpse age is not that hard to get)
		// Above that is rare.
		
		// public final float getSpellLifeTransferModifier()
		//	return this.getBonusForSpellEffect(26);		-- return 1-100
		
		
		// stamina, nutrition, food, bloodlust
		int nutr = 0;				// how much it's increased (1-100)
		int food = 0;				// how much it's increased (1-100)
		int stam = 0;				// how much it's increased (1-100)
		int lust = 0;				// how much it's DEcreased (1-100)
		int heal = feedStr / 3;
		
		boolean isHalfVamp = Vampires.isHalfVampire(p.getWurmId());
		
		if(isHalfVamp && heal > 0) {
			heal *= 0.4;
		}
		
		if(feedStr < 40) {
			nutr = 1;
			food = 1;
			stam = 5;
			lust = 5;
		} else if(feedStr < 60) {
			nutr = 2;
			food = 2;
			stam = 8;
			lust = 8;
		} else if(feedStr < 80) {
			nutr = 4;
			food = 4;
			stam = 12;
			lust = 12;
		} else if(feedStr < 100) {
			nutr = 8;
			food = 8;
			stam = 16;
			lust = 16;
		} else if(feedStr < 120) {
			nutr = 12;
			food = 12;
			stam = 12;
			lust = 15;
		} else if(feedStr < 140) {
			nutr = 12;
			food = 12;
			stam = 12;
			lust = 20;
		} else if(feedStr < 160) {
			nutr = 15;
			food = 15;
			stam = 20;
			lust = 17;
		} else if(feedStr < 180) {
			nutr = 18;
			food = 18;
			stam = 20;
			lust = 25;
		} else if(feedStr < 200) {
			nutr = 20;
			food = 20;
			stam = 20;
			lust = 30;
		} else if(feedStr < 220) {
			nutr = 20;
			food = 20;
			stam = 20;
			lust = 35;
		} else if(feedStr < 240) {
			nutr = 20;
			food = 20;
			stam = 20;
			lust = 40;
		} else if(feedStr < 260) {
			nutr = 20;
			food = 20;
			stam = 20;
			lust = 45;
		} else if(feedStr < 280) {
			nutr = 20;
			food = 20;
			stam = 20;
			lust = 60;
		} else {
			// Whoa, nice!
			nutr = 50;
			food = 50;
			stam = 50;
			lust = 100;
		}
		
		Wound[] w;
		double healMod = 50000;

		// public boolean setDamage(Creature defender, Item attWeapon, double damage, byte position, byte _type)
		// LT at 104 heals for around 5100-5900 per hit ... So eh, 10-20k is not out of line for a full kill, I suppose?
		// Actually, let it span over multiple wounds instead. Now we allow healing of three of them.
		if (heal > 0.0f
				&& healMod * (double)heal / (double)(p.isChampion() ? 1000.0f : 500.0f) > 500.0 
				&& p.getBody() != null 
				&& p.getBody().getWounds() != null 
				&& (w = p.getBody().getWounds().getWounds()).length > 0) {

			int reduceSeverity = -(int)( healMod * (double)heal / (double)(p.isChampion() ? 1000.0f : (p.getCultist() != null && p.getCultist().healsFaster() ? 250.0f : 500.0f)) );
			int healedWounds = 0;

			// We will heal up to three wounds.
			for(Wound wound : w) {
				if(healedWounds >= 3) {
					break;
				}
				wound.modifySeverity(reduceSeverity);
				healedWounds++;
			}
		}

		CreatureStatus cs = p.getStatus();

		float modifiedNut = cs.getNutritionlevel() + ((float)nutr / 100.0f);			// nutrition, 0.0-1.0, where 1.0 is 100 nutrition
		float ccfp = 0;

		// Half vampires capped at 0.5 nut
		if(modifiedNut > 0.5f && isHalfVamp) {
			modifiedNut = 0.5f;
		}
		
		if(isHalfVamp) {
			ccfp = 0.1f;
		} else {
			ccfp = modifiedNut * 3f;
		}

		// public final int modifyHunger(int hungerModification, float newNutritionLevel, float addCalories, float addCarbs, float addFats, float addProteins)
		
		CreaturesProxy.setHunNutSta(
				p,
				Math.max(0,		cs.getHunger() - (65535/100 * food)),					// hunger, 0-65535 where 0 = not hungry
				Math.min(0.99f,	modifiedNut),
				Math.min(65535,	cs.getStamina() + (65535/100 * stam)),					// stamina, 0-65535 where 0 = no stamina
				Math.min(0.999f,ccfp)
		);
		
		Skill bl = p.getSkills().getSkillOrLearn(VampSkills.BLOODLUST);
		bl.setKnowledge(bl.getKnowledge() - lust, false);

		return true;
	}
}
