package com.wurmonline.server.creatures;

import com.wurmonline.server.creatures.ai.ChatManager;

public class CreaturesProxy
{
	static public long getTraits(Offspring offspring)
	{
		return offspring.getTraits();
	}
	
	static public long getFather(Offspring offspring)
	{
		return offspring.getFather();
	}

	static public void setHunNutSta(Creature creature, int hunger, float nutrition, int stamina, float ccfp)
	{
		CreatureStatus cs = creature.getStatus();
		int oldStam = cs.stamina;

		cs.hunger = hunger;
		cs.nutrition = nutrition;
		cs.stamina = stamina;
		
		cs.calories = ccfp;
		cs.carbs = ccfp;
		cs.fats = ccfp;
		cs.proteins = ccfp;
		
		cs.setChanged(true);
		cs.sendStamina();
		cs.sendStateString();
		cs.checkStaminaEffects(oldStam);
		cs.sendHunger();
	}

	static public Creature[] getCreaturesWithName(String name)
	{
		return Creatures.getInstance().getCreaturesWithName(name);
	}
	
	static public ChatManager getChatManager(Creature npc)
	{
		return npc.isNpc() ? ((Npc)npc).chatManager : null;
	}
	
	static public void deleteOffspringSettings(long motherid)
	{
		Offspring.deleteSettings(motherid);
	}
}
