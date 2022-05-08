package com.friya.wurmonline.server.vamps;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.logging.Logger;

import com.wurmonline.server.skills.Skill;

import java.util.logging.Level;

public class ActionSkillGain
{
	private static Logger logger = Logger.getLogger(ActionSkillGain.class.getName());
	private int id = -1;
	private String name;
	private float difficultyMultiplier = 1.0f;
	HashMap<Integer, Double> gains = new HashMap<Integer, Double>();
	
	
	public ActionSkillGain(int skillId, String skillName)
	{
		this.setId(skillId);
		this.setName(skillName); 
	}
	
	
	public ActionSkillGain(int skillId, String skillName, float difficultyMultiplier)
	{
		this.setId(skillId);
		this.setName(skillName); 
		this.setDifficultyMultiplier(difficultyMultiplier);
	}
	
	
	public void simulate(Skill skill)
	{
		double before;
		double diff;
		boolean gained = false;
		int attempts = 0;

		for(int n = 1; n <= 100; n++) {
			if(n == 100) {
				skill.setKnowledge(99.99, false);
			} else {
				skill.setKnowledge(n + 0.5, false);
			}
			
			gained = false;
			attempts = 100;
			while(!gained && attempts-- > 0) {
				before = skill.getKnowledge();
				
				skill.skillCheck(50.0, 0.0, false, 1.0f);
				
				diff = skill.getKnowledge() - before;

				if(diff > 0.0) {
					this.add(n, diff);
					//logger.log(Level.INFO, s.getName() + " at skill " + (n + 0.5) + " gain: " + df.format(diff) + " approx actions per point: " + df.format(1.0 / diff) );
					gained = true;
				}
			}
			
			if(attempts == 0) {
				logger.log(Level.SEVERE, "Could not get skill gain of " + skill.getName() + " it could be because it is on cooldown");
				throw new RuntimeException("Could not get a skill gain out of " + skill.getName());
			}
		}
	}


	public void add(int level, double gain)
	{
		gains.put(level, gain);
	}


	/**
	 * We need this method to be able to cap a max-loss of a skill.
	 * 
	 * @param skillLevel
	 * @param skillLossAmount
	 * @return
	 */
	private double getActionCountForLoss(double skillLevel, double skillLossAmount)
	{
		double actionCount = 0;
		
		logger.log(Level.FINE, "getActionCountForLoss(): " + skillLevel + ", " + skillLossAmount + " in skill " + getName());
		
		if(skillLossAmount < 0 || skillLossAmount > 99 || skillLevel < 1) {
			// TODO: this should throw an exception?
			return 0;
		}
		
		double tmpLoss = 0;
		double endLevel = Math.max(1.0, skillLevel - skillLossAmount);
		double currentLevel = Math.max(1.0, skillLevel);
		while(true) {
			if(currentLevel < endLevel || currentLevel < 1) {
				//logger.log(Level.INFO, "getActionCountForLoss(): bailing because currentLevel: " + currentLevel + " endLevel: " + endLevel);
				break;
			}
			
			tmpLoss = gains.get((int)currentLevel);
			
			if((endLevel - currentLevel) > 0.0) {
				// a fraction of a skill point
				actionCount += (endLevel-currentLevel) / tmpLoss;
				//logger.log(Level.INFO, "getActionCountForLoss(): partial skillpoint - actions now at: " + actionCount);
				break;
			} else {
				// a full skill point
				actionCount += 1.0 / tmpLoss;
				//logger.log(Level.INFO, "getActionCountForLoss(): full skillpoint - actions now at: " + actionCount);
			}

			currentLevel--;
		}
		
		return actionCount;
	}
	
	
	/**
	 * Returns an approximate number of actions needed to get from skill level to skill level + amount. 
	 * 
	 * So, passing in 90 and 1 will return ~900 actions
	 * 
	 * @param skillLevel
	 * @param skillGainAmount
	 * @return
	 */
	private double getActionCountForGain(double skillLevel, double skillGainAmount)
	{
		double actionCount = 0;
		
		if(skillGainAmount < 0 || skillGainAmount > 99 || skillLevel < 1) {
			// TODO: this should throw an exception?
			return 0;
		}
		
		double tmpGain = 0;
		double endLevel = (skillLevel + skillGainAmount);
		double tmpLevel = skillLevel;
		while(true) {
			tmpGain = gains.get((int)tmpLevel);
			
			if((endLevel - tmpLevel) < 1.0) {
				// a fraction of a skill point
				actionCount += (endLevel-tmpLevel) / tmpGain;
				break;
			} else {
				// a full skill point
				actionCount += 1.0 / tmpGain;
			}
			
			if(tmpLevel > endLevel || tmpLevel >= 100) {
				break;
			}
			
			tmpLevel++;
		}
		
		return actionCount;
	}
	

	private double getSkillGainOrLossForActionCount(double skillLevel, int actionCount, boolean isLoss)
	{
		double ret = 0.0;
		double tmpLevel = skillLevel;

		int actionsStillNeeded = actionCount;

		while(true) {
			double tmpGain = gains.get((int)tmpLevel);
			double tmpActionCount = 1.0 / tmpGain;
			
			if((actionsStillNeeded - tmpActionCount) <= 0) {
				// we need less actions than the full skill-level would provide
				ret += actionsStillNeeded / tmpActionCount;
				break;
			} else {
				// we need a full skill level's number of actions, so just add 1.0 to the skill-gain
				ret += 1.0;
			}
			
			if(isLoss) {
				// loss of skill
				tmpLevel--;
				if(tmpLevel <= 1) 
					break;
			} else {
				// gain of skill
				tmpLevel++;
				if(tmpLevel > 100) 
					break;
			}
			actionsStillNeeded -= tmpActionCount;
		}
		
		return ret;
	}

	// ---

	public int getRawGainedActionCount(double skillLevel, double skillGainAmount)
	{
		return (int)Math.max(1, getActionCountForGain(skillLevel, skillGainAmount));
	}
	
	public int getRawLostActionCount(double skillLevel, double skillLossAmount)
	{
		return (int)Math.max(1, getActionCountForLoss(skillLevel, skillLossAmount));
	}
	
	// ---

	public double getRawSkillGainForActionCount(double skillLevel, int actionCount)
	{
		return getSkillGainOrLossForActionCount(skillLevel, actionCount, false);
	}
	
	public double getRawSkillLossForActionCount(double skillLevel, int actionCount)
	{
		return getSkillGainOrLossForActionCount(skillLevel, actionCount, true);
	}

	// ---
	
	/**
	 * NOTEL: Returns the NEW skill (not a difference)
	 * @param skillLevel
	 * @param actionCount
	 * @return
	 */
	public double getGainedSkillLevelForActionCount(double skillLevel, int actionCount)
	{
		return skillLevel + getRawSkillGainForActionCount(skillLevel, actionCount);
	}

	/**
	 * NOTE: Returns the NEW skill (not a difference)
	 * 
	 * @param skillLevel
	 * @param actionCount
	 * @return
	 */
	public double getLostSkillLevelForActionCount(double skillLevel, int actionCount)
	{
		return skillLevel - getRawSkillLossForActionCount(skillLevel, actionCount);
	}


	/**
	 * This method smoothes things out with a difficulty and a cap. It aims to make sure
	 * that if you are at skill 59 in something, you do not get knocked down to skill 1.
	 * 
	 * It also makes sure that there is more reason to go after targets that are of
	 * higher skill.
	 * 
	 * The rest of this class work with "raw" values.
	 * 
	 * The use case is: call this method to get a modified action count to apply to the
	 * victim, then call getRawSkillLossForActionCount() with the modified actionCount 
	 * gotten from this.
	 * 
	 * @param skillLevel
	 * @param actionCount
	 * @param capSkillLossAt Set to 0.25 for a cap of 25% of maximum skill lost. Typically 25% for a 'stake', and a lot less for a 'bite' -- say 1-2%
	 * @return number of ACTUAL actions to work with for this skill
	 */
	public int getModifiedLostActionCount(double skillLevel, int actionCount, float capSkillLossAt)
	{
		// 1. Some skills are harder to skill than others, so a difficulty grade should be applied
		int modifiedActionCount = (int)(actionCount * getDifficultyMultiplier());

		// 2. You should never lose more than 25% of any skill
		double skillPointLoss = 0.0;
		double cappedSkillPointLoss = skillLevel * capSkillLossAt;									// at 50, cap at a max of 12.5 point loss (25%)
		double rawSkillPointLoss = getRawSkillLossForActionCount(skillLevel, modifiedActionCount);	// e.g. 29.5

		if(rawSkillPointLoss > cappedSkillPointLoss) {
			skillPointLoss = cappedSkillPointLoss;
		} else {
			skillPointLoss = rawSkillPointLoss;
		}
		
		// Now translate the actual loss to number of actions
		modifiedActionCount = Math.min(modifiedActionCount, getRawLostActionCount(skillLevel, skillPointLoss));

		// 3. See to it that there is a net-loss in the transfer (more deducted from target than is given to performer)
		// TODO: but should not be done here?

		// This is just debugging.
		DecimalFormat df = new DecimalFormat("#.#######");
		if(modifiedActionCount == (int)(actionCount * getDifficultyMultiplier())) {
			// We did not need to cap number of actions...
			logger.log(Level.FINE, 
				this.getName() 
				+ " (" + getDifficultyMultiplier() + " difficulty) "
				+ " at skill " + skillLevel 
				+ ", kept lost action count at " + modifiedActionCount + " (" + actionCount + ")" 
				+ " which means a skill loss of " + df.format(skillPointLoss)
			);

		} else {
			// We capped the number of actions...
			logger.log(Level.FINE, 
				this.getName() 
				+ " (" + getDifficultyMultiplier() + " difficulty) "
				+ " at skill " + skillLevel 
				+ ", modified lost action count from " + (int)(actionCount * getDifficultyMultiplier()) + " (" + actionCount + ") to " + modifiedActionCount 
				+ " which means a skill loss of " + df.format(skillPointLoss) + " pts instead of " + df.format(rawSkillPointLoss)
			);
		}
		
		return modifiedActionCount;
	}


	public int getId() {
		return id;
	}


	private void setId(int id) {
		this.id = id;
	}


	public String getName() {
		return name;
	}


	private void setName(String name) {
		this.name = name;
	}


	public float getDifficultyMultiplier() {
		return difficultyMultiplier;
	}


	private void setDifficultyMultiplier(float difficultyMultiplier) {
		this.difficultyMultiplier = difficultyMultiplier;
	}
}
