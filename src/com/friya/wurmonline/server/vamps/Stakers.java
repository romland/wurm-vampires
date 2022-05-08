package com.friya.wurmonline.server.vamps;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.classhooks.InvocationHandlerFactory;
import org.gotti.wurmunlimited.modsupport.ModSupportDb;

import com.friya.wurmonline.server.vamps.events.EventOnce;
import com.friya.wurmonline.server.vamps.events.StakeWieldedEvent;
import com.friya.wurmonline.server.vamps.events.EventOnce.Unit;
import com.friya.wurmonline.server.vamps.items.Stake;
import com.wurmonline.server.DbConnector;
import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchPlayerException;
import com.wurmonline.server.Players;
import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.Terraforming;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.players.Cultist;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.players.PlayerInfo;
import com.wurmonline.server.players.PlayerInfoFactory;
import com.wurmonline.server.questions.KarmaQuestion;
import com.wurmonline.server.questions.VillageTeleportQuestion;
import com.wurmonline.server.utils.DbUtilities;
import com.wurmonline.server.villages.Village;
import com.wurmonline.server.villages.Villages;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtPrimitiveType;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;

public class Stakers
{
    private static Logger logger = Logger.getLogger(Stakers.class.getName());
    
    public static int HUNTED_TIME = (60*1000) * 90;		// * 90 minutes -- 5400 seconds
    public static final int POLL_INTERVAL = (5*1000);			// 5 seconds
    public static final int BITE_CAP = 50;						// number of times a hunter can be bitten during a hunt -- modify at any time
    public static final double STAKER_REQUIRED_FS = 35f;
    
    private static HashMap<Long, Staker> stakers = new HashMap<Long, Staker>();
    private static HashMap<Long, Long> bitables = new HashMap<Long, Long>();		// wurmId : timestamp 


    static void onItemTemplatesCreated()
    {
    	if(Mod.isTestEnv()) {
    	    HUNTED_TIME = 20000;				// 20 seconds while testing
    	}
    	
    	setUpEquipInterception();
    	setUpUnequipInterception();
    	setUpTeleportInterception();
    }


	static public void createStaker(Creature slayer, Creature vampire, int exchangedStatNum, String exchangedStatName, double vampireStatBefore, 
			double vampireLostAmount, int vampireLostActions, double slayerStatLevelBefore, double slayerGainedAmount)
	{
		Staker s = new Staker(slayer, vampire, exchangedStatNum, exchangedStatName, vampireStatBefore, vampireLostAmount, vampireLostActions, slayerStatLevelBefore, slayerGainedAmount);
		stakers.put(slayer.getWurmId(), s);
	}


	static void onServerStarted()
	{
		String sql;
		
		try {
			Connection con = ModSupportDb.getModSupportDb();

			// Keep track of slayers that log out during hunt (and statistics).
			if(ModSupportDb.hasTable(con, "FriyaVampireSlayers") == false) {
				sql = "CREATE TABLE FriyaVampireSlayers ("
					+ "		id						INTEGER			PRIMARY KEY AUTOINCREMENT,"
					+ "		slayerid				BIGINT			NOT NULL,"
					+ "		slayersteamid			VARCHAR(40)		NOT NULL DEFAULT '',"
					+ "		slayername				VARCHAR(40)		NOT NULL DEFAULT 'Unknown',"
					+ ""
					+ "		vampirename				VARCHAR(40)		NOT NULL DEFAULT 'Unknown',"
					+ "		vampireid				BIGINT			NOT NULL DEFAULT 0,"
					+ "		vampirestat				INT				NOT NULL DEFAULT 0,"
					+ "		vampirestatname			VARCHAR(40)		NOT NULL DEFAULT '',"
					+ "		vampireloststatlevel	FLOAT			NOT NULL DEFAULT 0,"
					+ "		vampirelostamount		FLOAT			NOT NULL DEFAULT 0,"
					+ "		vampirelostactions		INT				NOT NULL DEFAULT 0,"
					+ ""
					+ "		slayerstatlevel			FLOAT			NOT NULL DEFAULT 0,"
					+ "		slayergainedamount		FLOAT			NOT NULL DEFAULT 0,"
					+ ""
					+ "		staketime				BIGINT			NOT NULL DEFAULT 0,"
					+ "		timeelapsed				BIGINT			NOT NULL DEFAULT 0,"
					+ "		huntover				TINYINT			NOT NULL DEFAULT 0"
					+ ")";
				PreparedStatement ps = con.prepareStatement(sql);
				ps.execute();

				ps.close();
			}

		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		loadAll();
	}
	

	static private void loadAll()
	{
		logger.log(Level.INFO, "Loading all hunted stakers...");

		Connection dbcon = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			dbcon = ModSupportDb.getModSupportDb();
		    ps = dbcon.prepareStatement("SELECT * FROM FriyaVampireSlayers WHERE timeelapsed < ? AND huntover = 0");
			ps.setInt(1, HUNTED_TIME);
		    rs = ps.executeQuery();

			while (rs.next()) {
				Staker s = new Staker();

				s.setId(rs.getLong("id"));
				s.setPlayerId(rs.getLong("slayerid"));
				s.setPlayerName(rs.getString("slayername"));
				s.setStartTime(rs.getLong("staketime"));
				s.setLastPoll(System.currentTimeMillis() - POLL_INTERVAL);
				s.setLastSave(System.currentTimeMillis());
				s.setElapsedTime(rs.getLong("timeelapsed"));
				s.setHuntOverNoSave((rs.getByte("huntover") == 1 ? true : false));		// can't trigger a save from here as DB is locked
				s.setAffectedSkill(rs.getInt("vampirestat"));

				stakers.put(s.getPlayerId(), s);
				logger.log(Level.INFO, "Loaded staker " + s.getPlayerName() + " (slaying ID " + s.getId() + ")");
		    }
			
			rs.close();
			ps.close();
		}
		catch (SQLException e) {
		    throw new RuntimeException(e);
		}
	}


	static HashMap<Long, Staker> getStakers()
	{
		return stakers;
	}


	static public boolean isHunted(Creature player)
    {
    	return isHunted(player.getWurmId());
    }
    
    
    static public boolean mayPunish(long wurmId)
    {
    	// if they were just disarmed | if they are hunted | if they are wielding a stake
    	if(isHunted(wurmId) || bitables.containsKey(wurmId) || isWieldingStake(wurmId)) {
    		return true;
    	}
    	return false;
    }


    static public boolean isWieldingStake(long wurmId)
    {
    	Creature player = Players.getInstance().getPlayerOrNull(wurmId);

		if(player == null) {
			return false;
		}

		if(player.getRighthandItem() != null && player.getRighthandItem().getTemplateId() == Stake.getId()) {
			return true;
		} else if(player.getLefthandItem() != null && player.getLefthandItem().getTemplateId() == Stake.getId()) {
			return true;
		} else {
			return false;
		}
    }


    static public Item getWieldedStake(Creature creature)
    {
    	Item stake = null;
    	
		if(creature.getRighthandItem() != null && creature.getRighthandItem().getTemplateId() == Stake.getId()) {
			stake = creature.getRighthandItem();
		} else if(creature.getLefthandItem() != null && creature.getLefthandItem().getTemplateId() == Stake.getId()) {
			stake = creature.getLefthandItem();
		}
		
		return stake;
    }
    
    
    static public boolean isHunted(long wurmId)
    {
    	if(stakers.containsKey(wurmId) == false) {
    		return false;
    	}
    	
    	return stakers.get(wurmId).isHuntOver() == false;
    }
    
    
    static public boolean isHuntedMount(Creature creature)
    {
    	return creature != null && creature.isVehicle() && creature.getMountVehicle() != null && creature.getMountVehicle().getPilotId() > 0 && isHunted(creature.getMountVehicle().getPilotId());
    }
    
    
    static public Staker getStaker(long wurmId) throws NoSuchPlayerException
    {
    	if(stakers.containsKey(wurmId) == false) {
        	throw new NoSuchPlayerException("Staker not found");
    	}
    	
    	return stakers.get(wurmId);
    }
    

    static public boolean isStaker(long wurmId)
    {
    	return stakers.containsKey(wurmId);
    }


    static Creature getPlayer(String name)
    {
        try {
            PlayerInfo pinf = PlayerInfoFactory.createPlayerInfo(name.toLowerCase());
			Creature target = Server.getInstance().getCreature(pinf.wurmId);
			return target.isPlayer() ? target : null;
		} catch (NoSuchPlayerException | NoSuchCreatureException e) {
			return null;
		}
    }
    
    
    static public void onPlayerLogin(Player p)
    {
    	if(isHunted(p.getWurmId())) {
    		Vampires.broadcastLight("", true);
    		Vampires.broadcastLight(p.getName() + " returned to this world.  Let the hunt continue!", true);
    		Vampires.broadcastLight("", true);

    		p.getCommunicator().sendAlertServerMessage("You are a hunted a vampire slayer.", (byte)4);
    	}
    }


    static public void onPlayerLogout(Player p)
    {
    	if(isHunted(p.getWurmId())) {
    		Vampires.broadcastLight(p.getName() + " has fled this world, but will surely return.  The hunt will continue then.", false);
    	}
    }


    static public void poll()
    {
    	long ts = System.currentTimeMillis();

    	// Check if any players are wielding a stake OR are hunted and if they have animals on lead, if this is
    	// the case: make the animals stop following.
    	if((ts / 1000) % 2 == 1) {
	    	Player[] players = Players.getInstance().getPlayers();
	    	for(Player p : players) {
	    		if(Stakers.isWieldingStake(p.getWurmId()) || Stakers.isHunted(p.getWurmId())) {
	    			Creature[] followers = p.getFollowers();
	    			for(Creature f : followers) {
	    				Server.getInstance().broadCastAction(f.getName() + " refuses to follow " + p.getName() + " further.", p, 5);
	    				if(Stakers.isHunted(p.getWurmId())) {
	    					p.getCommunicator().sendNormalServerMessage(f.getName() + " looks nervously at your bloodstained hands and stops in its tracks.");
	    				} else {
	    					p.getCommunicator().sendNormalServerMessage("You are too busy wielding a stake, " + f.getName() + " looks disinterested in following you.");
	    				}
	    				f.setLeader(null);
	    			}
	    		}
	    	}
    	}
    	
    	for(Staker s : stakers.values()) {
    		if(s.isHuntOver()) {
    			// TODO: Should remove this staker from 'stakers', perhaps save it first?
    			continue;
    		}

    		if((ts - s.getLastPoll()) < POLL_INTERVAL) {
    			continue;
    		}

    		Creature player = getPlayer(s.getPlayerName());
    		if(player == null) {
    			s.setLastPoll(ts);
    			continue;
    		}
    		
    		if(isAtLegalLocation(player) == true) {
    			s.increaseElapsedTime();
    		} else {
    			// Every now and then, give message to staker that they are not in an allowed place
    			// 10% chance every 5 seconds, should be good enough?
    			if(Server.rand.nextInt(100) < 10) {
    				if(player.isOnGround() == false) {
	    				player.getCommunicator().sendNormalServerMessage(
		    					"It will take the purity of nature to wear the vampiric blood from your hands. As long as you remain on your mount, you will remain marked as a vampire slayer."
		    				);
    				} else {
	    				player.getCommunicator().sendNormalServerMessage(
	    					"It will take the purity of nature to wear the vampiric blood from your hands. As long as you remain here, you will remain marked as a vampire slayer."
	    				);
    				}
    			}

    			logger.log(Level.FINE, "Disallowed location for staker " + s.getPlayerName() + " " + player.getTileX() + ", " + player.getTileY() + ". Elapsed hunted timer remains at: " + Math.max(0, s.getElapsedTime() / 1000));
    			s.setLastPoll(ts);
    		}
    	}
    }


    static public boolean isAtLegalLocation(Creature player)
    {
    	if(player == null 
    			|| player.isOnSurface() == false
    			|| player.isOnDeed() == true
    			|| player.isOnGround() == false
    			|| player.isAlive() == false
    			|| player.isDead() == true
    			|| player.isFloating() == true
    			|| player.isGhost() == true
    			|| player.isLoggedOut() == true
    			|| player.isTeleporting() == true
    			|| Terraforming.isTileUnderWater(player.getCurrentTileNum(), player.getTileX(), player.getTileY(), player.isOnSurface())
    			|| isWithinBannedZone(player) == true
    			|| isOnAPerimeter(player) == true
    			|| isWithinEnclosure(player) == true) {
    		// When flying (as admin) ground = false; this also happens intermittently when you go down a hill.
    		//logger.log(Level.INFO, "Loc, surface: " + player.isOnSurface() + " deed: " + player.isOnDeed() + " ground: " + player.isOnGround() + " alive: " + player.isAlive() + " dead: " + player.isDead() + " floating: " + player.isFloating() + " ghost: " + player.isGhost() + " loggedout: " + player.isLoggedOut() + " teleporting: " + player.isTeleporting());
    		return false;
    	}

    	return true;
    }


    static public boolean isWithinBannedZone(Creature c)
    {
    	// TODO: be able to specify rectangles that are not allowed (makes abuse boring; use it once then it's banned -- create new table for this)
    	return false;
    }


    /**
     * Don't call directly, call isAtLegalLocation() instead
     * 
     * @param c
     * @return
     */
    static private boolean isWithinEnclosure(Creature c)
    {
    	long startTime = System.currentTimeMillis();

    	int radius = 75;
    	int diameter = radius*2;
    	
    	// Coordinates are normalized, the player is in the center
    	int plrX = radius;
    	int plrY = radius;
    	
    	boolean[][] bits = getBuiltTilesSlow(c, radius);

    	int raysChecked = 0;
    	int obstaclesFound = 0;
    	float enclosureThreshold = 0.85f;	// if 85% of total rays say enclosed, call it an enclosure

		/*    	
		    	 0,0           150,0
		    	
		    	
		    	       75,75
		
		
		    	0,150         150,150
		*/
    	// Initial check to cover for insane cost when outside of enclosures
    	if(hasObstacleAlongRay(bits, plrX, plrY, 0, 0) == false
    		|| hasObstacleAlongRay(bits, plrX, plrY, diameter, 0) == false
    		|| hasObstacleAlongRay(bits, plrX, plrY, 0, diameter) == false
    		|| hasObstacleAlongRay(bits, plrX, plrY, diameter, diameter) == false
    		) {
    		//logger.log(Level.INFO, "Precheck said we are not inside enclosure");
			return false;
		}

    	for(int destX = 0; destX < diameter; destX += 4) {			// note: use ++ instead of +=X for more precise checks
    		for(int destY = 0; destY < diameter; destY += 4) {		// note: use ++ instead of +=X for more precise checks
    			raysChecked++;
    			if( hasObstacleAlongRay(bits, plrX, plrY, destX, destY) ) {
    				obstaclesFound++;
    			}
    		}
    	}
    	
    	long cost = System.currentTimeMillis() - startTime;
    	
    	float amountSurrounded = (float)obstaclesFound / (float)raysChecked;

    	// Enable this to output an (a rather pretty) overview of obstacles in server-log
    	//draw(bits, radius, plrX, plrY);
    	
    	logger.log(Level.FINE, "Location: " + c.getTileX() + "," + c.getTileY() + " Rays: " + raysChecked + " Obstacles: " + obstaclesFound + " Amount surrounded: " + amountSurrounded  + " Threshold: " + enclosureThreshold + " inEnclosure Verdict: " + (amountSurrounded > enclosureThreshold) + " Cost: " + cost);
    	
    	if(amountSurrounded > enclosureThreshold) {
    		// Yes, within enclosure
    		return true;
    	}
    	
    	return false;
    }


    // standard bresenham
    static private boolean hasObstacleAlongRay(final boolean[][] obstacles, int x1, int y1, int destX, int destY)
    {
        int w = destX - x1 ;
        int h = destY - y1 ;
        int dx1 = 0, dy1 = 0, dx2 = 0, dy2 = 0;
        
        if (w < 0) {
        	dx1 = -1;
        } else if (w > 0) { 
        	dx1 = 1;
        }
        
        if (h < 0) {
        	dy1 = -1;
        } else if (h > 0) { 
        	dy1 = 1;
        }
        
        if (w < 0) {
        	dx2 = -1;
        } else if (w > 0) { 
        	dx2 = 1;
        }
        
        int longest = Math.abs(w);
        int shortest = Math.abs(h);

        if (!(longest > shortest)) {
            longest = Math.abs(h);
            shortest = Math.abs(w);

            if (h < 0) {
            	dy2 = -1;
            } else if (h > 0) {
            	dy2 = 1;
            }
            
            dx2 = 0;
        }

        int numerator = longest >> 1;

        for (int i = 0; i <= longest; i++) {
        	// Check if this tile along this line contains an obstacle (fence, bridge, wall)
        	if(obstacles[x1][y1]) {
        		return true;
        	}
            
            numerator += shortest ;

            if (!(numerator < longest)) {
                numerator -= longest;
                x1 += dx1;
                y1 += dy1;
            } else {
                x1 += dx2;
                y1 += dy2;
            }
        }
        
        return false;
    }


/*
    static private void draw(boolean[][] bits, int radius, int centerX, int centerY)
    {
    	StringBuffer ret = new StringBuffer();
    	for(int x = 0; x < bits.length; x++) {
    		ret.append("\r\n");

    		for(int y = 0; y < bits.length; y++) {
    			if(x == radius && y == radius) {
    				ret.append( "# " );
    			} else {
    				ret.append( (bits[x][y] ? "+-" : "  ") );
    			}
        	}
    	}
    	
    	logger.log(Level.INFO, ret.toString());

    }
*/    

    /*
     * Gets a 2D matrix of all tiles in a 'radius' around creature c. 
     * Tiles containing a fence, bridge or a wall will be set to true in this matrix, false otherwise.
     * 
     * TODO: We grab this directly from database, this can (and should) be optimized. It was written as a proof of concept. 
     *       I anticipated it being hilariously expensive, but it wasn't.
     * 
     * NOTE: I suppose a quick fix would be still using database queries, but do a join or a union or so.
     * NOTE: This is data the server should have in memory. Preferably we'd use "that" data structure.
     */
    static private boolean[][] getBuiltTilesSlow(Creature c, int radius)
    {
    	boolean[][] obstacles;
    	
    	Connection dbcon = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            dbcon = DbConnector.getZonesDbCon();
            
            // Fences
            ps = dbcon.prepareStatement("SELECT TILEX, TILEY FROM FENCES WHERE TILEX <= ? AND TILEX >= ? AND TILEY <= ? AND TILEY >= ? ORDER BY TILEX,TILEY");
            ps.setInt(1, c.getTileX() + radius);
            ps.setInt(2, c.getTileX() - radius);
            ps.setInt(3, c.getTileY() + radius);
            ps.setInt(4, c.getTileY() - radius);

            rs = ps.executeQuery();
            
        	obstacles = new boolean[radius*2+1][radius*2+1];

        	while (rs.next()) {
            	int normalizedX = rs.getInt("TILEX") - c.getTileX() + radius;
            	int normalizedY = rs.getInt("TILEY") - c.getTileY() + radius;

        		obstacles[normalizedX][normalizedY] = true;
            }

            rs.close();
            ps.close();
            
            // Buildings
            ps = dbcon.prepareStatement("SELECT TILEX, TILEY FROM WALLS WHERE TILEX <= ? AND TILEX >= ? AND TILEY <= ? AND TILEY >= ?");
            ps.setInt(1, c.getTileX() + radius);
            ps.setInt(2, c.getTileX() - radius);
            ps.setInt(3, c.getTileY() + radius);
            ps.setInt(4, c.getTileY() - radius);

            rs = ps.executeQuery();
        
        	while (rs.next()) {
            	int normalizedX = rs.getInt("TILEX") - c.getTileX() + radius;
            	int normalizedY = rs.getInt("TILEY") - c.getTileY() + radius;

        		obstacles[normalizedX][normalizedY] = true;
            }


            rs.close();
            ps.close();
            
            // Bridges
            ps = dbcon.prepareStatement("SELECT TILEX, TILEY FROM BRIDGEPARTS WHERE TILEX <= ? AND TILEX >= ? AND TILEY <= ? AND TILEY >= ?");
            ps.setInt(1, c.getTileX() + radius);
            ps.setInt(2, c.getTileX() - radius);
            ps.setInt(3, c.getTileY() + radius);
            ps.setInt(4, c.getTileY() - radius);

            rs = ps.executeQuery();
        
        	while (rs.next()) {
            	int normalizedX = rs.getInt("TILEX") - c.getTileX() + radius;
            	int normalizedY = rs.getInt("TILEY") - c.getTileY() + radius;

        		obstacles[normalizedX][normalizedY] = true;
            }
        }
        catch (SQLException sqx) {
            throw new RuntimeException(sqx);
        }
        finally {
            DbUtilities.closeDatabaseObjects(ps, rs);
            DbConnector.returnConnection(dbcon);
        }
    	
    	return obstacles;
    }


    static private boolean isOnAPerimeter(Creature c)
    {
		Village[] villages = Villages.getVillages();
		for(Village v : villages) {
			if(v.isWithinMinimumPerimeter(c.getTileX(), c.getTileY())) {
				return true;
			}
		}

		return false;
    }


	static void setUpEquipInterception()
    {
		//com/wurmonline/server/behaviours/Actions.java:    public static final short EQUIP = 582;
		//com/wurmonline/server/behaviours/Actions.java:    public static final short EQUIP_LEFT = 583;
		//com/wurmonline/server/behaviours/Actions.java:    public static final short EQUIP_RIGHT = 584;
		//com/wurmonline/server/behaviours/Actions.java:    public static final short EQUIP_TIMED = 723;
		//com/wurmonline/server/behaviours/Actions.java:    public static final short EQUIP_TIMED_AUTO = 724;
		    	
    	logger.log(Level.INFO, "doing setUpEquipInterception()");
    	
		String descriptor;
		try {
			// private static final boolean autoEquipWeapon(Item item, Creature player, byte slot, boolean isLeft)
			descriptor = Descriptor.ofMethod(CtClass.booleanType, new CtClass[] {
					HookManager.getInstance().getClassPool().get("com.wurmonline.server.items.Item"),
					HookManager.getInstance().getClassPool().get("com.wurmonline.server.creatures.Creature"),
					CtPrimitiveType.byteType,
					CtPrimitiveType.booleanType
			});
			HookManager.getInstance().registerHook("com.wurmonline.server.behaviours.AutoEquipMethods", "autoEquipWeapon", descriptor, new InvocationHandlerFactory()
			{
				@Override
				public InvocationHandler createInvocationHandler() {
					return new InvocationHandler() {

						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							Object result = method.invoke(proxy, args);

							if(Mod.logExecutionCost) {
								Mod.tmpExecutionStartTime = System.nanoTime();
							}

							if((boolean)result == true) {
								afterWieldHook((Creature)args[1], (Item)args[0]);
							}

							if(Mod.logExecutionCost) {
								logger.log(Level.INFO, "setUpEquipInterception[hook] done, spent " + Mod.executionLogDf.format((System.nanoTime() - Mod.tmpExecutionStartTime) / 1000000000.0) + "s");
							}

							return result;
						}
					};
				}
			});
			
			// Drop on (equip on) body slot is intercepted in the unequip methods; see comment at moveToItem interception

		} catch (NotFoundException e) {
			logger.log(Level.SEVERE, "Failed!", e);
			throw new RuntimeException("Failed to set up equip-interception");
		}
    	
    }
    
    static void setUpUnequipInterception()
    {
    	logger.log(Level.INFO, "doing setUpUnequipInterception()");

    	try {
			String descriptor;

            CtClass c = HookManager.getInstance().getClassPool().get("com.wurmonline.server.behaviours.MethodsItems");
            CtMethod m = c.getDeclaredMethod("drop");

			descriptor = Descriptor.ofMethod(m.getReturnType(), new CtClass[] {
					HookManager.getInstance().getClassPool().get("com.wurmonline.server.creatures.Creature"),
					HookManager.getInstance().getClassPool().get("com.wurmonline.server.items.Item"),
					CtPrimitiveType.booleanType
			});

			HookManager.getInstance().registerHook("com.wurmonline.server.behaviours.MethodsItems", "drop", descriptor, new InvocationHandlerFactory()
			{
				@Override
				public InvocationHandler createInvocationHandler() {
					return new InvocationHandler() {

						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

							if(Mod.logExecutionCost) {
								Mod.tmpExecutionStartTime = System.nanoTime();
							}

							Creature performer = (Creature)args[0];
							Item item = (Item)args[1];
							if(performer instanceof Player 
									&& (
											(performer.getRighthandItem() != null && performer.getRighthandItem().getWurmId() == item.getWurmId())
										||	(performer.getLefthandItem() != null && performer.getLefthandItem().getWurmId() == item.getWurmId())
										)
									) {
								if(allowUnequip(performer, item, null) == false) {
									//logger.log(Level.INFO, "Prevented unequip of item in drop()");
									return new String[]{};
								}
							}

							if(Mod.logExecutionCost) {
								logger.log(Level.INFO, "setUpUnequipInterception[hook1] done, spent " + Mod.executionLogDf.format((System.nanoTime() - Mod.tmpExecutionStartTime) / 1000000000.0) + "s");
							}

							Object result = method.invoke(proxy, args);
							return result;
						}
					};
				}
			});

			descriptor = Descriptor.ofMethod(CtClass.booleanType, new CtClass[] {
					HookManager.getInstance().getClassPool().get("com.wurmonline.server.creatures.Creature"),
					CtPrimitiveType.longType,
					CtPrimitiveType.booleanType
			});
			HookManager.getInstance().registerHook("com.wurmonline.server.items.Item", "moveToItem", descriptor, new InvocationHandlerFactory()
			{
				@Override
				public InvocationHandler createInvocationHandler() {
					return new InvocationHandler() {

						//public final boolean moveToItem(Creature mover, long targetId, boolean lastMove)
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							if(Mod.logExecutionCost) {
								Mod.tmpExecutionStartTime = System.nanoTime();
							}

							Creature mover = (Creature)args[0];

							if(mover instanceof Player) {
								Item item = (Item)proxy;
								Item target = Items.getItem((long)args[1]);

								if((mover.getRighthandItem() != null && mover.getRighthandItem().getWurmId() == item.getWurmId())
									|| (mover.getLefthandItem() != null && mover.getLefthandItem().getWurmId() == item.getWurmId())) {

									// This is called when unequipping
									if(allowUnequip(mover, (Item)proxy, Items.getItem((long)args[1])) == false) {
										//logger.log(Level.INFO, "Prevented unequip of item in moveToItem()");
										return false;
									}
									Object result = method.invoke(proxy, args);
									return result;
								}

								if(target != null && target.isBodyPart() && item.isWeapon() && item.getTemplateId() == Stake.getId()) {
									// This is called when equipping
									Object result = method.invoke(proxy, args);
									
									if(((boolean)result)) { 
										afterWieldHook(mover, item);
									}
									
									return result;
								}
							}
							
							if(Mod.logExecutionCost) {
								logger.log(Level.INFO, "setUpUnequipInterception[hook2] done, spent " + Mod.executionLogDf.format((System.nanoTime() - Mod.tmpExecutionStartTime) / 1000000000.0) + "s");
							}
							
							// Default
							Object result = method.invoke(proxy, args);
							return result;
						}
					};
				}
			});

		
			descriptor = Descriptor.ofMethod(CtClass.booleanType, new CtClass[] {
					HookManager.getInstance().getClassPool().get("com.wurmonline.server.items.Item"),
					HookManager.getInstance().getClassPool().get("com.wurmonline.server.creatures.Creature")
					//CtPrimitiveType.doubleType,
			});
			HookManager.getInstance().registerHook("com.wurmonline.server.behaviours.AutoEquipMethods", "unequip", descriptor, new InvocationHandlerFactory()
			{
				@Override
				public InvocationHandler createInvocationHandler() {
					return new InvocationHandler() {

						// public static final boolean unequip(Item item, Creature player)
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							if(Mod.logExecutionCost) {
								Mod.tmpExecutionStartTime = System.nanoTime();
							}

							Item item = (Item)args[0];
							Creature mover = (Creature)args[1];
							if(mover instanceof Player 
								&& (
										(mover.getRighthandItem() != null && mover.getRighthandItem().getWurmId() == item.getWurmId())
									||	(mover.getLefthandItem() != null && mover.getLefthandItem().getWurmId() == item.getWurmId())
									)
								) {
								if(allowUnequip(mover, item, null) == false) {
									//logger.log(Level.INFO, "Prevented unequip of item in unequip()");
									return false;
								}
							}
							
							if(Mod.logExecutionCost) {
								logger.log(Level.INFO, "setUpUnequipInterception[hook3] done, spent " + Mod.executionLogDf.format((System.nanoTime() - Mod.tmpExecutionStartTime) / 1000000000.0) + "s");
							}
							
							Object result = method.invoke(proxy, args);
							return result;
						}
					};
				}
			});

			descriptor = Descriptor.ofMethod(CtClass.booleanType, new CtClass[] {
					HookManager.getInstance().getClassPool().get("com.wurmonline.server.items.Item"),
					HookManager.getInstance().getClassPool().get("com.wurmonline.server.creatures.Creature")
					//CtPrimitiveType.doubleType,
			});
			HookManager.getInstance().registerHook("com.wurmonline.server.behaviours.AutoEquipMethods", "dropToInventory", descriptor, new InvocationHandlerFactory()
			{
				@Override
				public InvocationHandler createInvocationHandler() {
					return new InvocationHandler() {

						// private static final boolean dropToInventory(Item item, Creature player) 
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							if(Mod.logExecutionCost) {
								Mod.tmpExecutionStartTime = System.nanoTime();
							}

							Item item = (Item)args[0];
							Creature mover = (Creature)args[1];
							if(mover instanceof Player 
								&& (
										(mover.getRighthandItem() != null && mover.getRighthandItem().getWurmId() == item.getWurmId())
									||	(mover.getLefthandItem() != null && mover.getLefthandItem().getWurmId() == item.getWurmId())
									)
								) {
								if(allowUnequip(mover, item, null) == false) {
									//logger.log(Level.INFO, "Prevented unequip of item in dropToInventory()");
									return false;
								}
							}
							
							if(Mod.logExecutionCost) {
								logger.log(Level.INFO, "setUpUnequipInterception[hook4] done, spent " + Mod.executionLogDf.format((System.nanoTime() - Mod.tmpExecutionStartTime) / 1000000000.0) + "s");
							}
							
							Object result = method.invoke(proxy, args);
							return result;
						}
					};
				}
			});
		} catch (NotFoundException e) {
			logger.log(Level.SEVERE, "Failed!", e);
			throw new RuntimeException("Failed to set up unequip-interception");
		}
	}


    static void setUpTeleportInterception()
    {
    	logger.log(Level.INFO, "doing setUpTeleportInterception()");

    	try {
			String descriptor;

			descriptor = Descriptor.ofMethod(CtClass.voidType, new CtClass[] {
					HookManager.getInstance().getClassPool().get("java.util.Properties")
			});

			HookManager.getInstance().registerHook("com.wurmonline.server.questions.VillageTeleportQuestion", "answer", descriptor, new InvocationHandlerFactory()
			{
				@Override
				public InvocationHandler createInvocationHandler() {
					return new InvocationHandler() {

						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							if(Mod.logExecutionCost) {
								Mod.tmpExecutionStartTime = System.nanoTime();
							}

							Object result = null;
							Properties answers = (Properties)args[0];
							VillageTeleportQuestion q = (VillageTeleportQuestion)proxy;
							boolean teleport = answers.getProperty("teleport") != null && answers.getProperty("teleport").equals("true");

							if(teleport && Stakers.isHunted(q.getResponder())) {
								q.getResponder().getCommunicator().sendNormalServerMessage("You are hunted. The blood on your hands prevent you from teleporting now.");
								result = null;
							} else {
								result = method.invoke(proxy, args);
							}
							
							if(Mod.logExecutionCost) {
								logger.log(Level.INFO, "setUpTeleportInterception[hook1] done, spent " + Mod.executionLogDf.format((System.nanoTime() - Mod.tmpExecutionStartTime) / 1000000000.0) + "s");
							}
							
							return result;
						}
					};
				}
			});

			HookManager.getInstance().registerHook("com.wurmonline.server.questions.KarmaQuestion", "answer", descriptor, new InvocationHandlerFactory()
			{
				@Override
				public InvocationHandler createInvocationHandler() {
					return new InvocationHandler() {

						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							if(Mod.logExecutionCost) {
								Mod.tmpExecutionStartTime = System.nanoTime();
							}

							Object result;
							Properties answers = (Properties)args[0];
							KarmaQuestion q = (KarmaQuestion)proxy;
							boolean teleport = (answers.getProperty("val") != null && answers.getProperty("val").equals("townportal"));

							if(teleport && Stakers.isHunted(q.getResponder())) {
								q.getResponder().getCommunicator().sendNormalServerMessage("You are hunted. The blood on your hands prevent you from teleporting now.");
								result = null;
							} else {
								result = method.invoke(proxy, args);
							}
							
							if(Mod.logExecutionCost) {
								logger.log(Level.INFO, "setUpTeleportInterception[hook2] done, spent " + Mod.executionLogDf.format((System.nanoTime() - Mod.tmpExecutionStartTime) / 1000000000.0) + "s");
							}
							
							return result;
						}
					};
				}
			});
		
			descriptor = Descriptor.ofMethod(CtClass.booleanType, new CtClass[] {});

			HookManager.getInstance().registerHook("com.wurmonline.server.players.Cultist", "mayTeleport", descriptor, new InvocationHandlerFactory()
			{
				@Override
				public InvocationHandler createInvocationHandler() {
					return new InvocationHandler() {

						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							if(Mod.logExecutionCost) {
								Mod.tmpExecutionStartTime = System.nanoTime();
							}

							if(Stakers.isHunted(((Cultist)proxy).getWurmId())) {
								Player p = Players.getInstance().getPlayer(((Cultist)proxy).getWurmId());
								p.getCommunicator().sendNormalServerMessage("You are hunted. The blood on your hands prevent you from teleporting now.");
								return false;
							}

							if(Mod.logExecutionCost) {
								logger.log(Level.INFO, "setUpTeleportInterception[hook3] done, spent " + Mod.executionLogDf.format((System.nanoTime() - Mod.tmpExecutionStartTime) / 1000000000.0) + "s");
							}
							
							Object result = method.invoke(proxy, args);
							return result;
						}
					};
				}
			});

    	} catch (NotFoundException e) {
			logger.log(Level.SEVERE, "Failed!", e);
			throw new RuntimeException("Failed to setUpTeleportInterception");
		}
	}
    


    static public boolean allowUnequip(Creature performer, Item item, @Nullable Item destination)
    {
    	if(item.getTemplateId() != Stake.getId()) {
    		return true;
    	}
    	
    	// If destination is null, it typically is attempting move to ground.
    	if(destination != null && destination.getTemplateId() == ItemList.trashBin) {
    		return true;
    	}
    	
    	Mod.actionNotify(
			performer,
			"The magical runes of the stake are fused to you. You can get rid of the stake is by throwing it in a trash heap or using it on a Vampire.",
			"%NAME tries to get rid of a magical stake in frustration.",
			"You hear some muffled sounds, almost as if someone is grumbling."
    	);
    	
    	performer.getCommunicator().sendNormalServerMessage("The magical runes of the stake are fused to you. You can get rid of the stake is by throwing it in a trash heap or using it on a Vampire.");
    	return false;
    }


    static public void afterWieldHook(Creature performer, Item item)
    {
    	if(item.getTemplateId() == Stake.getId()) {
        	EventOnce ev = new StakeWieldedEvent(10, Unit.SECONDS, performer, item);
    		EventDispatcher.add(ev);
    	}
    }
    
    static public void addBitable(long wurmId)
    {
    	bitables.put(wurmId, System.currentTimeMillis());
    }
    
    static public void removeBitable(long wurmId)
    {
    	bitables.remove(wurmId);
    }
    
}
