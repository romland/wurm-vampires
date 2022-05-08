package com.friya.wurmonline.server.vamps.events;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.friya.wurmonline.server.vamps.Vampires;
import com.wurmonline.server.creatures.Creature;

public class DelayedDeVamp extends EventOnce
{
	private static Logger logger = Logger.getLogger(DelayedDeVamp.class.getName());
	
	private Creature creature;
	private String message;
	
	public DelayedDeVamp(int fromNow, Unit unit, Creature c, String msg)
	{
        super(fromNow, unit);
        
        creature = c;
        message = msg;

		logger.log(Level.INFO, "DelayedDeVamp created");
	}

	@Override
	public boolean invoke()
	{
		if(creature == null || creature.isOffline()) {
			return true;
		}

		creature.getCommunicator().sendAlertServerMessage(message, (byte)4);

		Vampires.deVamp(creature);

		if(creature.getPower() < 1) {
			creature.die(false, "Friya's Curse");
		} else {
			creature.getCommunicator().sendAlertServerMessage("You are an admin, sparing you from death...", (byte)4);
		}
		
		return true;
	}
}
