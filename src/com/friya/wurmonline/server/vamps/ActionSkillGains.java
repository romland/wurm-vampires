package com.friya.wurmonline.server.vamps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.wurmonline.server.Server;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.skills.SimSkill;
import com.wurmonline.server.skills.SimSkills;
import com.wurmonline.server.skills.Skill;
import com.wurmonline.server.skills.SkillList;
import com.wurmonline.server.skills.SkillSystem;
import com.wurmonline.server.skills.SkillTemplate;
import com.wurmonline.server.skills.Skills;


public class ActionSkillGains
{
	private static Logger logger = Logger.getLogger(ActionSkillGains.class.getName());
	private static HashMap<Integer, ActionSkillGain> skillGains = new HashMap<Integer, ActionSkillGain>();
	private static SimSkills simSkills;
	
	
	static void onServerStarted()
	{
		// Passing in a name here flags it as template and whatever side-effects that might have. Do not want.
		simSkills = new SimSkills(null);
		VampSkills.learnSkills(simSkills);
		simSkills.priest = true;

		createSimulatedSkillGains();
	}
	
	public ActionSkillGains()
	{
	}
	
	
	static public ActionSkillGain getSkill(int skillNumber)
	{
		return skillGains.get(skillNumber);
	}
	

	static private Skill getSimulatedSkill(int skillNumber)
	{
		// We MUST have ID set to -152 here. Otherwise we will not be able to properly simulate
		// normal skill gain on the server; this is checked by the alterSkill hook we should have in place.
		Skill simSkill = (Skill)(new SimSkill(-152, (Skills)simSkills, skillNumber, 1.0, 99.99990000242077f, 1479035543351L));

		// We will throw a lot of exceptions if we have not learned the dependencies.
		for(int p : simSkill.getUniqueDependencies()) {
			simSkills.learn(p, 30.0f, false);
		}

		return simSkill;
	}
	

	static private boolean isSkillExcluded(int s)
	{
		return s == SkillList.FIGHT_AGGRESSIVESTYLE
			|| s == SkillList.FIGHT_DEFENSIVESTYLE
			|| s == SkillList.FIGHT_NORMALSTYLE
			|| s == SkillList.GROUP_FIGHTING
			|| s == SkillList.WEAPONLESS_FIGHTING
			|| s == SkillList.FAVOR
			|| s == SkillList.BODY
			|| s == SkillList.MIND
			|| s == SkillList.SOUL
			|| s == SkillList.STEALING				// TODO: seems to be impossible to get a skill tick out of this one; likely because it's on cooldown
			|| s == SkillList.LOCKPICKING			// TODO: seems to be impossible to get a skill tick out of this one; likely because it's on cooldown
			|| s == VampSkills.BLOODLUST; 
	}
	

	/**
	 * Since not all skills are equal in difficulty to grind. When it comes to difficulty 
	 * multiplier, 1.0 is considered a normal skill (i.e. blacksmithing). The higher this 
	 * is, the bigger numbers are transferred (where 1.0 is normal). So, say you want to 
	 * take "1000 actions" from someone, at 0.5 this will take the equivalent of 500 actions
	 */
	static private float getSkillDifficultyMultiplier(int skillNumber)
	{
		switch(skillNumber) {
			case SkillList.DIGGING :
			case SkillList.WOODCUTTING :
				return 1.2f;
			
			case SkillList.CLIMBING :
			case SkillList.CARPENTRY_FINE :
			case SkillList.MINING :
			case SkillList.GROUP_SMITHING_WEAPONSMITHING :
				return 0.5f;
	
			case SkillList.CHANNELING : 
				return 0.3f;
	
			case SkillList.BODY_CONTROL :
			case SkillList.BODY_STAMINA :
			case SkillList.BODY_STRENGTH :
			case SkillList.MIND_LOGICAL :
			case SkillList.MIND_SPEED :
			case SkillList.SOUL_DEPTH :
			case SkillList.SOUL_STRENGTH :
				return 0.01f;
				
			case SkillList.FAITH :
				return 0.006f;
				
			case SkillList.MEDITATING :
			case SkillList.LOCKPICKING :	// NOTE: this cannot happen because the skill is currently excluded (see above)
				return 0.003f;
				
			default :
				return 1.0f;
		}
	}


	static int randomGaussian(int max, float scale)
	{
	    double v;
	    
	    do {
	        v = Math.abs(Server.rand.nextGaussian() / scale);
	    } while (v >= 1.0);

	    return (int)(v * max);
	}


	static private ArrayList<Skill> getSortedSkills(Skill[] skills)
	{
		ArrayList<Skill> s = new ArrayList<Skill>();
		Collections.addAll(s, skills);
		//s.sort(Comparator.comparing(Skill::getKnowledge));
		s.sort(Comparator.comparing((Skill u) -> u.getKnowledge()).reversed());
		
		return s;
	}


	/**
	 * 40% chance to steal highChanceSkillNum
	 * 
	 * @param c
	 * @param highChanceSkillNum
	 * @return
	 */
	static public ActionSkillGain getRandomSkillToPunish(Creature c, int highChanceSkillNum)
	{
		if(Server.rand.nextInt(100) < 40) {
			return getSkill(highChanceSkillNum);
		}
		
		return getRandomSkillToPunish(c);
	}

	static public ActionSkillGain getRandomHighSkillToPunish(Creature c)
	{
		return getRandomHighSkillToPunish(c, 0);
	}
	

	static public ActionSkillGain getRandomHighSkillToPunish(Creature c, int highChanceSkillNum)
	{
		if(highChanceSkillNum > 0 && Server.rand.nextInt(100) < 20) {
			return getSkill(highChanceSkillNum);
		}
		
		ArrayList<Skill> targetSkills = getSortedSkills(c.getSkills().getSkills());

		Skill skill;
		
		int i = 0;
		while(i++ <= 200) {
			skill = targetSkills.get(randomGaussian(targetSkills.size(), 3.5f));		// 3.5 will prefer lower indices over higher

			if(isSkillExcluded(skill.getNumber()) == false && skill.getKnowledge() > 1.0) {
				return getSkill(skill.getNumber());
			}
		}
		
		return null;
	}
	

	/**
	 * 
	 * 
	 * @param c
	 * @return
	 */
	static public ActionSkillGain getRandomSkillToPunish(Creature c)
	{
		Skill[] targetSkills = c.getSkills().getSkills();
		return getRandomSkillToPunish(c, targetSkills);
	}


	// Higher chance to gain when staking:
	//			dexterity - 20%
	//			top 10 highest skills - 80% (so 10% chance for each skill)
	//
	// Higher chance to gain when biting:
	//			dexterity - 20%
	//			the skill staker stole from last slaying - 20%
	//			top 10 highest skills - 60% (so 10% chance for each skill)
	static private ActionSkillGain getRandomSkillToPunish(Creature c, Skill[] targetSkills)
	{
		Skill skill;
		
		int i = 200;
		while(true && i-- > 0) {
			skill = targetSkills[Server.rand.nextInt(targetSkills.length)];

			//logger.log(Level.INFO, "Might take gains from: " + skill.getName());
			
			if(isSkillExcluded(skill.getNumber()) == false && skill.getKnowledge() > 1.0) {
				return getSkill(skill.getNumber());
			}
		}
		
		return null;
	}
	
	
	static private void createSimulatedSkillGains()
	{
		Skill skill;
		ActionSkillGain asg;

		logger.log(Level.INFO, "createSimulatedSkillGains() starting...");

		// TODO: Why don't we get characteristics here? I do want them to be in play.
		SkillTemplate[] templates = SkillSystem.getAllSkillTemplates();
		
		for(SkillTemplate tpl : templates) {
			if(isSkillExcluded(tpl.getNumber())) {
				continue;
			}

			skill = getSimulatedSkill(tpl.getNumber());
			asg = new ActionSkillGain(skill.getNumber(), skill.getName(), getSkillDifficultyMultiplier(tpl.getNumber()));
			
			asg.simulate(skill);
			
			// This is just to test (will output a debug statement)...
			asg.getModifiedLostActionCount(90, 1000, 0.25f);

			skillGains.put(skill.getNumber(), asg);
		}
		
		logger.log(Level.INFO, "createSimulatedSkillGains() done");
	}
}
