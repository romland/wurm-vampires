package com.friya.wurmonline.server.vamps.events;

import com.friya.wurmonline.server.vamps.Mod;
import com.friya.wurmonline.server.vamps.Vampire;
import com.friya.wurmonline.server.vamps.Vampires;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.players.Player;

public class DelayedVamp extends EventOnce
{
	private Creature creature;
	private String message;
	
	public DelayedVamp(int fromNow, Unit unit, Creature c, String msg)
	{
        super(fromNow, unit);
        
        creature = c;
        message = msg;
	}

	@Override
	public boolean invoke()
	{
		if(creature == null || creature.isOffline()) {
			return true;
		}

		creature.getCommunicator().sendAlertServerMessage(message, (byte)4);

        Vampire v = Vampires.getVampire(creature.getWurmId());
        v.convertHalfToFull();
		
		Mod.loginVampire((Player)creature);
		
		return true;
	}
}
