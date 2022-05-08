package com.wurmonline.server.skills;

public class SkillsProxy
{
	static public Skills getParent(Skill s)
	{
		return s.parent;
	}

	static public long getSkillOwnerId(Skill s)
	{
		return getParent(s).id;
	}
}
