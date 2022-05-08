package com.wurmonline.server.skills;

import java.io.IOException;

public class SimSkill extends DbSkill
{
	public SimSkill(long aId, Skills aParent) throws IOException
	{
		super(aId, aParent);
	}
	
	public SimSkill(long aId, Skills aParent, int aNumber, double aKnowledge, double aMinimum, long aLastused)
	{
		super(aId, aParent, aNumber, aKnowledge, aMinimum, aLastused);
	}
	
	public SimSkill(long aId, int aNumber, double aKnowledge, double aMinimum, long aLastused)
	{
		super(aId, aNumber, aKnowledge, aMinimum, aLastused);
	}
	
	public Skills getParent()
	{
		return parent;
	}
}
