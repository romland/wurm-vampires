package com.friya.wurmonline.server.vamps.events;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.friya.wurmonline.server.vamps.Stakers;
import com.friya.wurmonline.server.vamps.Vampires;
import com.wurmonline.server.Players;
import com.wurmonline.server.players.Player;

public class RemoveBitableEvent extends EventOnce
{
	private static Logger logger = Logger.getLogger(RemoveBitableEvent.class.getName());
	
	private long wurmId;
	private boolean giveMessage = false;
	
	public RemoveBitableEvent(int fromNow, Unit unit, long wurmId)
	{
        super(fromNow, unit);
        this.wurmId = wurmId;
        
		logger.log(Level.INFO, "RemoveBitableEvent created");
	}

	public RemoveBitableEvent(int fromNow, Unit unit, long wurmId, boolean giveMessage)
	{
		this(fromNow, unit, wurmId);
		this.giveMessage = giveMessage;
	}

	@Override
	public boolean invoke()
	{
		Stakers.removeBitable(wurmId);
		
		// Don't give message if they are hunted for real.
		if(giveMessage && Stakers.isHunted(wurmId) == false) {
			Player p = Players.getInstance().getPlayerOrNull(wurmId);
			if(p != null) {
				Vampires.broadcast(p.getName() + " is no longer marked as a vampire slayer. The time of the hunt will now cease!", true, true, false);
			}
		}

		return true;
	}
}
