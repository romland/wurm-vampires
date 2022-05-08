package com.friya.wurmonline.server.vamps;

/*
100     public static final byte tabardSlot = 35;
101     public static final byte neckSlot = 36;
102     public static final byte lHeldSlot = 37;
103     public static final byte rHeldSlot = 38;
104     public static final byte lRingSlot = 39;
105     public static final byte rRingSlot = 40;
106     public static final byte quiverSlot = 41;
107     public static final byte backSlot = 42;
108     public static final byte beltSlot = 43;
109     public static final byte shieldSlot = 44;
110     public static final byte capeSlot = 45;
111     public static final byte lShoulderSlot = 46;
112     public static final byte rShoulderSlot = 47;
113     public static final byte inventory = 48;
*/
public class Vampire
{
	private long playerId;
	private String steamId;
	private String name;
	private String alias;
	private int vampireStatus;
	private long halfStartTime;
	private long fullStartTime;
	private long fullEndTime;
	
	public Vampire(long playerId, String steamId, String name, String alias, int vampireStatus, long halfStartTime, long fullStartTime, long fullEndTime)
	{
		this.setPlayerId(playerId);
		this.setSteamId(steamId);
		this.setName(name);
		this.setAlias(alias);
		this.setVampireStatus(vampireStatus);
		this.setHalfStartTime(halfStartTime);
		this.setFullStartTime(fullStartTime);
		this.setFullEndTime(fullEndTime);
	}
	
	public String toString()
	{
		return "Vampire#" + playerId + " [steam:" + steamId + " alias:" + alias + " status:" + vampireStatus + " halfStart:" + halfStartTime + " fullstart:" + fullStartTime + " fullEnd:" + fullEndTime + "]";
				
	}
	
	long getId()
	{
		return playerId;
	}
	
	boolean isFull()
	{
		return vampireStatus == Vampires.STATUS_FULL;
	}
	
	boolean isHalf()
	{
		return vampireStatus == Vampires.STATUS_HALF;
	}
	
	boolean isFullOrHalf()
	{
		return isFull() || isHalf();
	}
	
	public boolean convertHalfToFull()
	{
		if(isHalf() == false) {
			return false;
		}
		
		setFullStartTime(System.currentTimeMillis());
		setVampireStatus(Vampires.STATUS_FULL);

		Vampires.updateVampire(this);
		return true;
	}
	
	int getVampireStatus()
	{
		return vampireStatus;
	}
	
	void setVampireStatus(int val)
	{
		vampireStatus = val;
	}
	
	String getName()
	{
		return name;
	}
	
	public String getAlias()
	{
		return alias;
	}

	public String getSteamId() {
		return steamId;
	}

	public void setSteamId(String steamId) {
		this.steamId = steamId;
	}

	
	public void setName(String name) {
		this.name = name;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	
	public long getHalfStartTime() {
		return halfStartTime;
	}

	public void setHalfStartTime(long halfStartTime) {
		this.halfStartTime = halfStartTime;
	}

	
	public long getFullStartTime() {
		return fullStartTime;
	}

	public void setFullStartTime(long fullStartTime) {
		this.fullStartTime = fullStartTime;
	}

	
	public long getFullEndTime() {
		return fullEndTime;
	}

	public void setFullEndTime(long fullEndTime) {
		this.fullEndTime = fullEndTime;
	}


	public long getPlayerId() {
		return playerId;
	}

	public void setPlayerId(long playerId) {
		this.playerId = playerId;
	}
}
