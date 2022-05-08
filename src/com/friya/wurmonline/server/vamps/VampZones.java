package com.friya.wurmonline.server.vamps;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.wurmonline.server.Point;
import com.wurmonline.server.zones.FocusZone;

public class VampZones
{
    private static Logger logger = Logger.getLogger(VampZones.class.getName());
    private static String zoneName = "The Coven";
    private static FocusZone covenZone;
    
	// Start coordinate defines northwest corner
	private static int startX = 1686;					// testserver center: 1689, 1773 
	private static int startY = 1770;
	private static int zoneSize = 6;					// this is radius
	private static int spawnPointLayer = -1;
	
	private static int respawnX = 0;					// destination for 'fly' and when vampires get staked
	private static int respawnY = 0;

	public VampZones()
	{
		if(Mod.isTestEnv() == false) {
			// Zenath, NW corner:
			startX = 2290;
			startY = 1273;
			zoneSize = 30;

			respawnX = 2297;
			respawnY = 1283;
		}
	}


	static void onServerStarted()
	{
		logger.log(Level.INFO, "Getting or creating a Vampire spawn-zone");
		
		for(FocusZone z : FocusZone.getAllZones()) {
			if(z.getName().equals(zoneName)) {
				setCovenZone(z);
				logger.log(Level.INFO, "Found a zone called " + zoneName + ", good.");
				
				startX = z.getStartX();
				startY = z.getStartY();
				zoneSize = Math.max(z.getEndX() - z.getStartX(), z.getEndY() - z.getStartY());
				
				return;
			}
		}
		
		setCovenZone(new FocusZone(startX, startX + zoneSize, startY, startY + zoneSize, FocusZone.TYPE_NO_BUILD, zoneName, "Friya's Vamps", true));
		logger.log(Level.INFO, "Created a zone called " + zoneName + ", at " + startX + ", " + startY);
	}

	static public FocusZone getCovenZone()
	{
		return covenZone;
	}
	
	static public Point getCovenCentre()
	{
		return new Point(startX + (zoneSize/2), startY + (zoneSize/2));
	}
	
	static public Point getCovenRespawnPoint()
	{
		if(respawnX > 0 && respawnY > 0) {
			return new Point(respawnX, respawnY);
		} else {
			return getCovenCentre();
		}
	}
	

	static public int getCovenLayer()
	{
		return spawnPointLayer;
	}

	static private void setCovenZone(FocusZone covenZone)
	{
		VampZones.covenZone = covenZone;
	}
}
