package com.friya.wurmonline.server.vamps.events;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.wurmonline.server.Players;

public class RemoveEffectEvent extends EventOnce
{
	private static Logger logger = Logger.getLogger(RemoveEffectEvent.class.getName());
	
	private int effectId;
	
	public RemoveEffectEvent(int fromNow, Unit unit, int effectId)
	{
        super(fromNow, unit);
        this.effectId = effectId;
        
		logger.log(Level.INFO, "RemoveEffectEvent created");
	}

	@Override
	public boolean invoke()
	{
		Players.getInstance().removeGlobalEffect(effectId);

		return true;
	}
}
