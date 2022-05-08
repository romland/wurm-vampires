package com.friya.wurmonline.server.vamps;

public class VampSkillTemplate
{
	public int number;
	public String name;
	public float difficulty;
	public int[] dependencies;
	public long decayTime;
	public short type;
	public boolean fightingSkill;
	public boolean ignoreEnemy;
	public int[] skillUpOn;

    VampSkillTemplate(int aNumber, String aName, float aDifficulty, int[] aDependencies, long aDecayTime, short aType, boolean aFightingSkill, boolean aIgnoreEnemy, int[] skillUpOn)
    {
    	number = aNumber;
    	name = aName;
    	difficulty = aDifficulty;
    	dependencies = aDependencies;
    	decayTime = aDecayTime;
    	type = aType;
    	fightingSkill = aFightingSkill;
    	ignoreEnemy = aIgnoreEnemy;

    	this.skillUpOn = skillUpOn;
    }
}
