package com.friya.wurmonline.server.vamps;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.ModSupportDb;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.players.Player;

public class Staker
{
    private static Logger logger = Logger.getLogger(Staker.class.getName());

    private long id = 0;
	private long playerId = 0;
	private String playerName = null;
	private long startTime = 0;
	private long lastPoll = 0;
	private long lastSave = 0;
	private long elapsedTime = 0;
	private boolean huntOver = false;
	private int bitten = 0;
	private int affectedSkill = 0;
	private static final String insertSlayerSql = "INSERT INTO FriyaVampireSlayers"
			+ "("
			+ " slayerid, slayersteamid, slayername, vampireid, vampirename, vampirestat, vampirestatname, vampireloststatlevel, "
			+ " vampirelostamount, vampirelostactions, slayerstatlevel, slayergainedamount, staketime, timeelapsed, huntover"
			+ ") "
			+ "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	
	static long SAVE_INTERVAL = (1000 * 60) * 2;	// 2 minutes

	public Staker()
	{
	}
	
	Staker(Creature slayer, Creature vampire, int exchangedStatNum, String exchangedStatName, double vampireStatBefore,  double vampireLostAmount, int vampireLostActions, double slayerStatLevelBefore, double slayerGainedAmount)
	{
		setPlayerId(slayer.getWurmId());
		setPlayerName(slayer.getName());
		setStartTime(System.currentTimeMillis());
		setLastPoll(getStartTime());
		setLastSave(getStartTime());
		setElapsedTime(0);
		setAffectedSkill(exchangedStatNum);

		try {
			Connection dbcon = ModSupportDb.getModSupportDb();
			PreparedStatement ps = dbcon.prepareStatement(insertSlayerSql, Statement.RETURN_GENERATED_KEYS);

			int i = 1;
			ps.setLong(i++, slayer.getWurmId());
			ps.setString(i++, ((Player)slayer).getSteamId().toString());
			ps.setString(i++, slayer.getName());
			ps.setLong(i++, vampire.getWurmId());
			ps.setString(i++, vampire.getName());
			ps.setInt(i++, exchangedStatNum);
			ps.setString(i++, exchangedStatName);
			ps.setDouble(i++, vampireStatBefore);
			ps.setDouble(i++, vampireLostAmount);
			ps.setInt(i++, vampireLostActions);
			ps.setDouble(i++, slayerStatLevelBefore);
			ps.setDouble(i++, slayerGainedAmount);
			ps.setLong(i++, System.currentTimeMillis());	// stakeTime
			ps.setLong(i++, 0);								// elapsedTime
			ps.setByte(i++, (byte)0);						// huntOver
			
			ps.execute();
			
			ResultSet rs = ps.getGeneratedKeys();
			if(rs != null) {
				rs.next();
				this.setId(rs.getLong(1));

				logger.log(Level.FINE, "Inserted item as: " + id);
			} else {
				logger.log(Level.SEVERE, "no resultset back from getGeneratedKeys(), probably means nothing was created!");
			}

			rs.close();
			ps.close();

		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to insert staker");
			throw new RuntimeException(e);
		}
	}


	void increaseElapsedTime()
	{
		if(isHuntOver()) {
			logger.log(Level.INFO, "Hunt is over, why are we calling increaseElapsedTime() on " + getPlayerName() + "?");
		}

		long ts = System.currentTimeMillis();

		if(getLastPoll() <= 0) {
			logger.log(Level.WARNING, "lastPoll was set to 0 for hunter (probably just loaded?), setting it to 'now'");
			setLastPoll(ts);
		}
		
		setElapsedTime(elapsedTime + ts - getLastPoll());
		logger.log(Level.FINE, "Staker's elapsed hunted time now: " + Math.max(0, getElapsedTime() / 1000) + " seconds");
		setLastPoll(ts);
		
		if(getElapsedTime() >= Stakers.HUNTED_TIME) {
			setHuntOver(true);
		}

		if(ts > (getLastSave() + SAVE_INTERVAL)) {
			save();
		}
	}


	void save()
	{
		logger.log(Level.INFO, "Updating hunted staker " + getPlayerName());

		String sql = "UPDATE FriyaVampireSlayers"
				+ " SET "
				+ " timeelapsed = ?, huntover = ?"
				+ " WHERE id = ?";
		
		try {
			Connection dbcon = ModSupportDb.getModSupportDb();
			PreparedStatement ps = dbcon.prepareStatement(sql);

			int i = 1;
			ps.setLong(i++, getElapsedTime());
			ps.setByte(i++, (huntOver ? (byte)1 : 0));
			ps.setLong(i++, getId());
			ps.executeUpdate();
			ps.close();

		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to update staker: " + getPlayerName());
			throw new RuntimeException(e);
		}
		
		setLastSave(System.currentTimeMillis());
	}


	long getPlayerId() {
		return playerId;
	}

	void setPlayerId(long playerId) {
		this.playerId = playerId;
	}

	String getPlayerName() {
		return playerName;
	}

	void setPlayerName(String playerName) {
		this.playerName = playerName;
	}

	long getStartTime() {
		return startTime;
	}

	void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	long getLastPoll() {
		return lastPoll;
	}

	void setLastPoll(long lastPoll) {
		this.lastPoll = lastPoll;
	}

	long getElapsedTime() {
		return elapsedTime;
	}

	void setElapsedTime(long elapsedTime) {
		this.elapsedTime = elapsedTime;
	}

	long getLastSave() {
		return lastSave;
	}

	void setLastSave(long lastSave) {
		this.lastSave = lastSave;
	}

	boolean isHuntOver()
	{
		return huntOver;
	}

	void setHuntOver(boolean huntOver)
	{
		if(huntOver && this.huntOver != huntOver) {
			Creature staker = Stakers.getPlayer(getPlayerName());
			staker.getCommunicator().sendAlertServerMessage("Your hands finally wash clean of the blood. You are no longer marked as a vampire slayer.", (byte)4);
			Vampires.broadcast(getPlayerName() + " is no longer marked as a vampire slayer. The time of the hunt will now cease!", true, true, false);
			if(bitten == 0 && VampTitles.hasTitle(staker, VampTitles.ESCAPIST) == false) {
				staker.addTitle(VampTitles.getTitle(VampTitles.ESCAPIST));
			}
		}

		this.huntOver = huntOver;
		save();
	}

	void setHuntOverNoSave(boolean huntOver)
	{
		this.huntOver = huntOver;
	}
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
	
	public void addBitten()
	{
		bitten++;
	}
	
	public boolean mayBite()
	{
		return bitten < Stakers.BITE_CAP;
	}
	
	public int getAffectedSkill()
	{
		return affectedSkill;
	}

	public void setAffectedSkill(int num)
	{
		affectedSkill = num;
	}
}
