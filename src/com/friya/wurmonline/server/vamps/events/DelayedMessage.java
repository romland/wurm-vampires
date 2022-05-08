package com.friya.wurmonline.server.vamps.events;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.wurmonline.server.creatures.Creature;

public class DelayedMessage extends EventOnce
{
	private static Logger logger = Logger.getLogger(DelayedMessage.class.getName());
	
	private Creature creature;
	private String message;
	
	public DelayedMessage(int fromNow, Unit unit, Creature c, String msg)
	{
        super(fromNow, unit);
        
        creature = c;
        message = msg;

		logger.log(Level.INFO, "DelayedMessage created");
	}

	@Override
	public boolean invoke()
	{
		if(creature == null || creature.isOffline()) {
			return true;
		}
		creature.getCommunicator().sendAlertServerMessage(message, (byte)4);
		return true;
	}
}
