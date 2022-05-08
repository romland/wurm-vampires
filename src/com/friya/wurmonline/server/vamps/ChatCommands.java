package com.friya.wurmonline.server.vamps;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
//import java.util.logging.Level;
//import java.util.logging.Logger;
import java.util.stream.Stream;

import org.gotti.wurmunlimited.modsupport.ModSupportDb;

import com.wurmonline.server.NoSuchPlayerException;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.players.Player;

public class ChatCommands
{
	//private static Logger logger = Logger.getLogger(Vampires.class.getName());


	static boolean onPlayerMessage(Communicator com, String msg)
	{
		if(msg.startsWith("/")) {
			if(msg.startsWith("/slayers")) {
				return cmdSlayers(com, msg);
			}

			if(msg.startsWith("/hunted")) {
				return cmdHunted(com, msg);
			}

			if(msg.startsWith("/toplist")) {
				return cmdToplistVamps(com, msg);
			}
		}

		return false;
	}


	static boolean cmdToplistVamps(Communicator com, String msg)
	{
		if(com.player.getPower() == 0 && Vampires.isVampire(com.player.getWurmId()) == false) {
			return false;
		}
		
		com.sendNormalServerMessage("The highest rated hunting vampires");
		Toplist toplist = getToplistVampsData(15);
		toplist.sendTo(com.player);
		return true;
	}


	static public Toplist getToplistVampsData(int listSize)
	{
		Connection dbcon = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		HashMap<String, Long> scores = new HashMap<String, Long>();

		try {
			dbcon = ModSupportDb.getModSupportDb();
		    ps = dbcon.prepareStatement("SELECT playerid, alias, slayerlostactions, stakingid, slayersteamid, vampiresteamid FROM FriyaVampires AS vamps INNER JOIN FriyaVampireBites AS bites ON (bites.vampireid = vamps.playerid)");
		    rs = ps.executeQuery();

		    String alias = null;
		    long score = 0;
			while (rs.next()) {
				alias = rs.getString("alias");
				
				if(alias == null) {
					continue;
				}

				if(rs.getString("slayersteamid").equals(rs.getString("vampiresteamid"))) {
					// same steam id? shoo cheater
					continue;
				}

				score = rs.getLong("slayerlostactions");

				// It's -1 if it was not during hunt (it's generally a lot harder to get someone during a hunt)
				if(rs.getLong("stakingid") > 0) {
					score *= 3;
				}
				
				if(scores.containsKey(alias)) {
					scores.put(alias, scores.get(alias) + score);
				} else {
					scores.put(alias, score);
				}
		    }
		}
		catch (SQLException e) {
		    throw new RuntimeException(e);
		} finally {
			try {
				rs.close();
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		Stream<Map.Entry<String,Long>> sorted = scores.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()));
		
		Toplist toplist = new Toplist(listSize);
		
		Entry<String, Long> entry;
		Iterator<Entry<String, Long>> it = sorted.iterator();
		int i = 0;
		while(it.hasNext()) {
			entry = it.next();
			
			toplist.addNameScore(entry.getKey(), entry.getValue());

			if(i++ >= listSize) {
				break;
			}
		}
		
		sorted.close();

		return toplist;
	}
	
	static boolean cmdSlayers(Communicator com, String msg)
	{
		Player p = com.player;

		if(p.getPower() == 0 && Vampires.isVampire(p.getWurmId()) == false) {
			return false;
		}
		
		boolean found = false;

		HashMap<Long, Staker> stakers = Stakers.getStakers();
		for(Staker s : stakers.values()) {
			if(Stakers.getPlayer(s.getPlayerName()) != null && s.isHuntOver() == false) {
				found = true;
				com.sendNormalServerMessage(s.getPlayerName() + " is a hunted vampire slayer and is in this world.");
			}
		}
		
		if(found == false) {
			com.sendNormalServerMessage("There are currently no hunted vampire slayers in the world.");
		}
		
		return true;
	}


	static private boolean cmdHunted(Communicator com, String msg)
	{
		Player p = com.player;

		// Keep this at approximate time or it will be too easy for hunted 
		// slayers to "cheese" it by standing near protected ground
		if(Stakers.isHunted(p)) {
			try {
				long minutes = (Stakers.HUNTED_TIME - Stakers.getStaker(p.getWurmId()).getElapsedTime()) / 1000 / 60;
				if(minutes < 5) {
					com.sendNormalServerMessage("You are marked as a vampire slayer and is hunted for a few more minutes.");
				} else {
					com.sendNormalServerMessage("You are marked as a vampire slayer and is hunted for approximately " + (5*(Math.round(minutes/5))) + " more minutes.");
				}
			} catch(NoSuchPlayerException e) {
				com.sendNormalServerMessage("You are marked as a vampire slayer and is hunted by all vampires.");
			}
		} else {
			com.sendNormalServerMessage("Your hands are clean, you are not hunted.");
		}

		return true;
	}

}
