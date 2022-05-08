package com.friya.wurmonline.server.vamps;

import java.util.HashMap;

/**
 * We don't even persist these, if we reboot, fine.
 * 
 * @author Friya
 *
 */
public class Cooldowns
{
	// playerid-utilityid : timestamp
	static HashMap<String, Long> lastUses = new HashMap<String, Long>();

	public Cooldowns()
	{
	}

	static public boolean isOnCooldown(String playerEffect, long cooldown)
	{
		if(lastUses.containsKey(playerEffect) == false) {
			return false;
		}

		return System.currentTimeMillis() < (lastUses.get(playerEffect) + cooldown);
	}

	static public long getPreviousUse(String playerEffect)
	{
		if(lastUses.containsKey(playerEffect) == false) {
			return 0;
		}
		
		return lastUses.get(playerEffect);
	}
	
	static public void setUsed(String playerEffect)
	{
		lastUses.put(playerEffect, System.currentTimeMillis());
	}
}
