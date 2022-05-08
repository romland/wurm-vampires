package com.friya.wurmonline.server.vamps;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.friya.wurmonline.server.vamps.items.AltarOfSouls;
import com.wurmonline.math.TilePos;
import com.wurmonline.server.Server;
import com.wurmonline.server.WurmCalendar;
import com.wurmonline.server.behaviours.Terraforming;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Achievements;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.skills.Skill;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;

public class BloodLust
{
	private static Logger logger = Logger.getLogger(BloodLust.class.getName());

	static public void onItemTemplatesCreated()
	{
		logger.log(Level.INFO, "preInit completed");
	}

	static public void setBloodlust(Player p, double amount)
	{
		p.getSkills().getSkillOrLearn(VampSkills.BLOODLUST).setKnowledge(amount, false);
		logger.log(Level.INFO, "setBloodlust() to " + amount + " for " + p.getName());
	}


	/*
		at 27 sec / tick = 4050 seconds to max bloodlust = 67.5 minutes
		max tick = 3 bloodlust
		lMod = 10.0f
		
		Bloodlust		Ticks	Seconds		Minutes
			99+			150		4050		67.5
			 90			122		3294		54.9
			 80			 97		2619		43.6
			 70			 75		2025		33.7
			 60			 55		1485		24.7
			 50			 39		1053		17.5
			 40			 25		 675		11.2
			 30			 15		 405		 6.7
			 20			  8		 216		 3.6
			 10			  3		  81		 1.3
	
		This is the frequency at which players will get bloodlust notifications:
		
		[13:06:59] Your vampiric bloodlust is beginning to grow. @4.0
		[13:26:20] You feel the need to feed once again. @53.708923
		[13:31:20] The bloodlust is neglected. You must feed soon. @60.18135
		[13:39:39] The bloodlust is neglected. You must feed soon. @69.95354
		[13:48:00] Vampiric urges gnaw at your gut. @78.084404
		[13:56:20] Your bloodlust tears at your soul with a vengeance. It must be sated! @85.83123
		[14:04:40] Your bloodlust tears at your soul with a vengeance. It must be sated! @92.57174
		[14:08:20] The vampiric bloodlust begins feeding slowly on YOUR soul! You must feed soon or waste away into oblivion. @95.41421
		[14:11:50] The vampiric bloodlust is feeding slowly on YOUR soul! You must feed soon or waste away into oblivion. @98.17411
		
		
		12dec2016: Changing lMod from 10 to 8 to slow down increase a bit
		22dec2016: Changing lMod from 8 to 5 to slow down increase a bit
		// at lMod 8: [07:58:29] <Lady Deux> [Izzy] [16:58:22] Bloodlust increased by 0.2797 to 95.6201
		//[12:00:11] Bloodlust increased by 0.1743 to 95.7944
	 */
	/**
	 * Called roughly once per second by CreatureVampire.poll()
	 * 
	 * TODO: Explain.
	 * 
	 * @param player
	 */
	static public void poll(Player p)
	{
		Skill bl = p.getSkills().getSkillOrLearn(VampSkills.BLOODLUST);
		float currentBloodlust = (float)(bl.getKnowledge());

		if(p.isDead()) {
			if(currentBloodlust > 60f) {
				BloodLust.setBloodlust(p, 60f);
			}
			return;
		}

		// 13jan2017: lowered from 6.0 to 5.0
		float lMod = 5.0f;

		if(bl.affinity > 0) {
			lMod -= (bl.affinity * 0.1f);
		}
		
		// 13jan2017: If under influence of sleep bonus, cut bloodlust in half. This should allow for easier skilling.
		if(p.hasSleepBonus()) {
			lMod *= 0.5f;
		}

		if(currentBloodlust <= 5f && Server.getSecondsUptime() % 40 == 0) {
			p.getCommunicator().sendNormalServerMessage("Your vampiric bloodlust is beginning to grow.");
			
		} else if(currentBloodlust >= 40f && currentBloodlust <= 60f && Server.getSecondsUptime() % 600 == 0) {
			p.getCommunicator().sendNormalServerMessage("You feel the need to feed once again.");
			
		} else if(currentBloodlust > 60f && currentBloodlust <= 70f && Server.getSecondsUptime() % 500 == 0) {
			p.getCommunicator().sendNormalServerMessage("The bloodlust is neglected. You must feed soon.");
			
		} else if(currentBloodlust > 70f && currentBloodlust <= 80f && Server.getSecondsUptime() % 500 == 0) {
			p.getCommunicator().sendNormalServerMessage("Vampiric urges gnaw at your gut.");
			
		} else if(currentBloodlust > 80f && currentBloodlust <= 95f && Server.getSecondsUptime() % 500 == 0) {
			p.getCommunicator().sendNormalServerMessage("Your bloodlust tears at your soul with a vengeance. It must be sated!");
			
		} else if(currentBloodlust > 95f && currentBloodlust <= 98f && Server.getSecondsUptime() % 180 == 0) {
			// hurt a little...
			Item altar = getAltarProtection(p);
			if(altar != null) {
				p.getCommunicator().sendNormalServerMessage("Your vampiric bloodlust begins feeding on souls in the altar next to you!");
		    	Achievements.triggerAchievement(p.getWurmId(), VampAchievements.ALTAR_SOULFEED);
				
				if(Server.rand.nextInt(5) == 0) {
					AltarOfSouls.setCharge(altar, (byte)(AltarOfSouls.getCharge(altar) - 1));
				}

			} else {
				p.getCommunicator().sendNormalServerMessage("The vampiric bloodlust begins feeding slowly on your soul!");
		    	Achievements.triggerAchievement(p.getWurmId(), VampAchievements.SOULFEED);
				damagePlayer(p, 3500.0f);
			}

		} else if(currentBloodlust > 98f && currentBloodlust <= 99f && Server.getSecondsUptime() % 30 == 0) {
			// hurt the same, but more often
			Item altar = getAltarProtection(p);
			if(altar != null) {
				p.getCommunicator().sendNormalServerMessage("Your vampiric bloodlust is feeding slowly on souls in the altar next to you!");

				if(Server.rand.nextInt(5) == 0) {
					AltarOfSouls.setCharge(altar, (byte)(AltarOfSouls.getCharge(altar) - 1));
				}

			} else {
				p.getCommunicator().sendNormalServerMessage("The vampiric bloodlust is feeding slowly on YOUR soul! You must feed soon or waste away into oblivion.");
				damagePlayer(p, 3500.0f);
			}

		} else if(currentBloodlust > 99f && Server.getSecondsUptime() % 30 == 0) {
			// hurt EVEN more
			Item altar = getAltarProtection(p);
			if(altar != null) {
				p.getCommunicator().sendNormalServerMessage("Your vampiric bloodlust is feeding on souls in the altar next to you!");

				if(Server.rand.nextInt(5) == 0) {
					AltarOfSouls.setCharge(altar, (byte)(AltarOfSouls.getCharge(altar) - 1));
				}
				
			} else {
				p.getCommunicator().sendNormalServerMessage("The vampiric bloodlust is feeding on YOUR soul! You MUST feed now.");
				damagePlayer(p, 7000.0f);
			}
		}

		if(Server.getSecondsUptime() % 27 == 0) {		// every 27 seconds -- why? because I wanted an uneven number
			float blIncrease = 0f;
			float maxTick = 2.0f;

			if(Vampires.isHalfVampire(p.getWurmId())) {
				// half vamps
				blIncrease = (float) ((lMod/2) * (100.0f / (Math.max(1.0f, currentBloodlust) * 30.0f)));
				maxTick = 1.5f;
			} else {
				// full vamps
				blIncrease = (float) (lMod * (100.0f / (Math.max(1.0f, currentBloodlust) * 30.0f)));
			}

			p.setSkill(VampSkills.BLOODLUST, (float) (currentBloodlust + Math.min(blIncrease, maxTick)));
			
			//p.getCommunicator().sendNormalServerMessage("Bloodlust increased to: " + (currentBloodlust + Math.min(blIncrease, 3.0f)) );
			//logger.log(Level.FINEST, "Bloodlust modified for " + p.getName());
		}
	}
	
	
	static private void damagePlayer(Player p, float amount)
	{
		if(WurmCalendar.isNight()) {
			amount *= 0.9;
		}
		
    	p.addWoundOfType(
    			null, 				// creature
    			(byte)6, 			// woundType
    			1,					// position (was 21)
    			true, 				// randomize position (was false)
    			0.0f, 				// armour mod (was 1.0)
    			true, 				// calculate armour
    			amount, 			// damage
    			0.0f, 				// infection (was 10)
    			0.0					// poison
    	);
	}
	
	
	// $ = altar
	// X = must be empty
	// # = Must be empty, and where you are protected from soulfeed damage
	//
	// X X X X X
	// X # # # X
	// X # $ # X
	// X # # # X
	// X X X X X
	static private Item getAltarProtection(Player p)
	{
		if(p.isOnSurface() || Terraforming.isFlat(p.getTileX(), p.getTileY(), p.isOnSurface(), 0) == false) {
			return null;
		}

		Item foundAltar = getNearbyAltarOfSouls(p);

		if(foundAltar == null) {
			return null;
		}

		if(AltarOfSouls.isCleanArea(foundAltar) == false) {
			p.getCommunicator().sendNormalServerMessage("The area around Altar of Souls is too cluttered with items.");
			return null;
		}
		
		if(AltarOfSouls.isCharged(foundAltar) == false) {
			p.getCommunicator().sendNormalServerMessage("The altar demand new souls.");
			return null;
		}
		
		return foundAltar;
	}


	static private Item getNearbyAltarOfSouls(Player p)
	{
		TilePos t = p.getTilePos();
		Item[] tmpItems = null;
		VolaTile tmpTile = null;
		
		// Is there an altar ON current tiles or adjacent to this player?
		for(int x = -1; x <= 1; x++) {
			for(int y = -1; y <= 1; y++) {
				tmpTile = Zones.getTileOrNull(t.x + x, t.y + y, false);

				if(tmpTile == null) {
					continue;
				}

				tmpItems = tmpTile.getItems();
				
				for(Item item : tmpItems) {
					if(item.getTemplateId() == AltarOfSouls.getId()) {
						return item;
					}
				}
			}
		}
		
		return null;
	}


}
