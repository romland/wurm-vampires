package com.friya.wurmonline.server.vamps.events;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.friya.wurmonline.server.vamps.Mod;
import com.friya.wurmonline.server.vamps.Vampires;
import com.friya.wurmonline.server.vamps.items.Stake;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.NoSuchPlayerException;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.zones.NoSuchZoneException;

public class StakeWieldedEvent extends EventOnce
{
	private static Logger logger = Logger.getLogger(StakeWieldedEvent.class.getName());
	
	private Item stake;
	private Creature wielder;
	
	public StakeWieldedEvent(int fromNow, Unit unit, Creature wielder, Item stake)
	{
        super(fromNow, unit);
        this.stake = stake;
        this.wielder = wielder;
        
        logger.log(Level.INFO, "Stake auxdata BEFORE: " + stake.getAuxData());
		stake.setAuxData(Stake.STATUS_WIELDING);

    	Mod.actionNotify(
			wielder,
			"You wield the stake of Vampire Banishment, you must let it settle for a few seconds before you can use it. You can get rid of the stake by using it on a vampire or tossing it into a trash heap. BEWARE: If a vampire sees you with this wielded, they will likely punish you!",
			"%NAME started wielding a stake of Vampire Banishment!",
			"You hear the runes of a stake of Vampire Banishment flare up."
    	);

		logger.log(Level.INFO, "StakeWieldedEvent created");
	}

	@Override
	public boolean invoke()
	{
		if(stake.getAuxData() == Stake.STATUS_WIELDING) {
			stake.setAuxData(Stake.STATUS_READY);
			
			if(wielder != null && Vampires.isHalfOrFullVampire(wielder.getWurmId())) {
		    	Mod.actionNotify(
		    			wielder,
		    			"The magical runes of the stake prevent you from holding on to it longer! You are a vampire!",
		    			"%NAME's stake of Vampire Banishment drops to the ground.",
		    			"A stake of Vampire Banishment drops to the ground."
	        	);
		    	try {
					stake.putItemInfrontof(wielder);
				} catch (NoSuchCreatureException | NoSuchItemException | NoSuchPlayerException | NoSuchZoneException e) {
					logger.log(Level.SEVERE, "Failed to move stake out of a vampires hands", e);
				}
			} else {
		    	Mod.actionNotify(
		    			wielder,
		    			"The magical runes of the stake of Vampire Banishment settle. You can now use it!",
		    			"%NAME's stake of Vampire Banishment has settled.",
		    			"You hear the runes of a stake of Vampire Banishment settle."
	        	);
			}
		}

		return true;
	}
}
