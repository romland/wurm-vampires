package com.friya.wurmonline.server.vamps.events;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.friya.wurmonline.server.vamps.Mod;
import com.friya.wurmonline.server.vamps.items.Stake;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

public class StakeRecoverEvent extends EventOnce
{
	private static Logger logger = Logger.getLogger(StakeRecoverEvent.class.getName());
	
	private Item stake;
	private Creature wielder;
	
	public StakeRecoverEvent(int fromNow, Unit unit, Creature wielder, Item stake)
	{
        super(fromNow, unit);
        this.stake = stake;
        this.wielder = wielder;
        
		stake.setAuxData(Stake.STATUS_RECOVERING);

		logger.log(Level.FINE, "StakeRecoverEvent created");
	}

	@Override
	public boolean invoke()
	{
		if(stake.getAuxData() == Stake.STATUS_RECOVERING) {
			stake.setAuxData(Stake.STATUS_READY);
	    	Mod.actionNotify(
	    			wielder,
	    			"You have fully recovered from your recent staking attempt.",
	    			null,
	    			null
        	);
		}

		return true;
	}
}
