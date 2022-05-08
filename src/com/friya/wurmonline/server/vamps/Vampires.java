package com.friya.wurmonline.server.vamps;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.classhooks.InvocationHandlerFactory;
import org.gotti.wurmunlimited.modsupport.ModSupportDb;

import com.friya.wurmonline.server.vamps.actions.AidAction;
import com.friya.wurmonline.server.vamps.actions.AssistSlainAction;
import com.friya.wurmonline.server.vamps.actions.BiteAction;
import com.friya.wurmonline.server.vamps.actions.CrippleAction;
import com.friya.wurmonline.server.vamps.actions.CrownFindAction;
import com.friya.wurmonline.server.vamps.actions.DevourAction;
import com.friya.wurmonline.server.vamps.actions.DisarmAction;
import com.friya.wurmonline.server.vamps.actions.FlyAction;
import com.friya.wurmonline.server.vamps.actions.SmashAction;
import com.friya.wurmonline.server.vamps.actions.SprintAction;
import com.friya.wurmonline.server.vamps.actions.TraceAction;
import com.friya.wurmonline.server.vamps.events.EventOnce.Unit;
import com.friya.wurmonline.server.vamps.events.RemoveStakedTeleportEvent;
import com.wurmonline.server.Message;
import com.wurmonline.server.Players;
import com.wurmonline.server.Servers;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreaturesProxy;
import com.wurmonline.server.creatures.MovementScheme;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.modifiers.DoubleValueModifier;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.players.PlayerInfo;
import com.wurmonline.server.players.PlayerInfoFactory;
import com.wurmonline.server.skills.AffinitiesTimed;
import com.wurmonline.server.skills.Skill;
import com.wurmonline.server.zones.VolaTile;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtPrimitiveType;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class Vampires
{
	private static Logger logger = Logger.getLogger(Vampires.class.getName());
	private static HashMap<Long, Vampire> vampires = new HashMap<Long, Vampire>();
	
	public final static String kitSalesManName = "Vampire hunter D";
	public static final String headVampireName = "Orlok"; 
	public static final String deVampManName = "van Helsing"; 
	public static final String halfVampMakerName = "Dhampira the Ponderer";
	public static int STATUS_NONE = 0;
	public static int STATUS_HALF = 1;
	public static int STATUS_FULL = 2; 

	public static final int DISARM_BITABLE_DURATION = 25;			// * seconds
	public static final int AMULET_HIT_BITABLE_DURATION = 60 * 5;	// 5 minutes

	public static int VAMPIRE_RETIREMENT_COST = 5000;
	public static int HALF_VAMPIRE_RETIREMENT_COST = 800;
	public static int BITE_ACTION_COUNT_REWARD = 250;

	private static boolean createTestCharacters = false;
	static ArrayList<String> fakeGMs = new ArrayList<String>();

	private static VolaTile lastStakedTile = null;


	public Vampires()
	{
	}


	static public void onItemTemplatesCreated()
	{
		setupStealthModInterception();
		setupEatInterception();
		setupBuryInterception();
		setupStealthActionInterception();
	}


	//
	// Faster stealth for vampires
	//
	static private void setupStealthModInterception()
	{
		String descriptor;

		descriptor = Descriptor.ofMethod(CtPrimitiveType.voidType, new CtClass[] {
			CtPrimitiveType.booleanType
		});
		HookManager.getInstance().registerHook("com.wurmonline.server.creatures.MovementScheme", "setStealthMod", descriptor, new InvocationHandlerFactory()
		{
			@Override
			public InvocationHandler createInvocationHandler() {
				return new InvocationHandler() {

					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						if(Mod.logExecutionCost) {
							Mod.tmpExecutionStartTime = System.nanoTime();
						}

			        	if((boolean)args[0]) {
			        		MovementScheme mover = (MovementScheme)proxy;

			        		// TODO: Rarely called method, but this is nasty and expensive, we have to get to creature through reflection :(
					        Field field = MovementScheme.class.getDeclaredField("creature");
					        field.setAccessible(true);
					        Creature creature = ((Creature)field.get(mover));

		        			if(com.friya.wurmonline.server.vamps.Vampires.isVampire(creature.getWurmId())) {
						        field = MovementScheme.class.getDeclaredField("stealthMod");
						        field.setAccessible(true);
						        DoubleValueModifier stealthMod = ((DoubleValueModifier)field.get(mover));
			        			stealthMod.setModifier(1.0);
			        		}
			        	}
						
			    		if(Mod.logExecutionCost) {
			    			logger.log(Level.INFO, "setupStealthModInterception[hook] done, spent " + Mod.executionLogDf.format((System.nanoTime() - Mod.tmpExecutionStartTime) / 1000000000.0) + "s");
			    		}

			        	Object result = method.invoke(proxy, args);
						return result;
					}
				};
			}
		});
	}
	
	
	//
	// Vampires can't eat normal food
	//
	static private void setupEatInterception()
	{
		try {
			String descriptor = Descriptor.ofMethod(CtPrimitiveType.intType, new CtClass[] {
				HookManager.getInstance().getClassPool().get("com.wurmonline.server.creatures.Creature"),
				HookManager.getInstance().getClassPool().get("com.wurmonline.server.items.Item")
			});
			HookManager.getInstance().registerHook("com.wurmonline.server.behaviours.MethodsItems", "eat", descriptor, new InvocationHandlerFactory()
			{
				@Override
				public InvocationHandler createInvocationHandler() {
					return new InvocationHandler() {

						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							
							if(Mod.logExecutionCost) {
								Mod.tmpExecutionStartTime = System.nanoTime();
							}

							if(stopEat((Creature)args[0], (Item)args[1])) {
								return 0;
							}

				    		if(Mod.logExecutionCost) {
				    			logger.log(Level.INFO, "setupEatInterception[hook1] done, spent " + Mod.executionLogDf.format((System.nanoTime() - Mod.tmpExecutionStartTime) / 1000000000.0) + "s");
				    		}

							Object result = method.invoke(proxy, args);
							return result;
						}
					};
				}
			});

			descriptor = Descriptor.ofMethod(CtPrimitiveType.booleanType, new CtClass[] {
				HookManager.getInstance().getClassPool().get("com.wurmonline.server.behaviours.Action"),
				HookManager.getInstance().getClassPool().get("com.wurmonline.server.creatures.Creature"),
				HookManager.getInstance().getClassPool().get("com.wurmonline.server.items.Item"),
				CtPrimitiveType.floatType
			});
			HookManager.getInstance().registerHook("com.wurmonline.server.behaviours.MethodsItems", "eat", descriptor, new InvocationHandlerFactory()
			{
				@Override
				public InvocationHandler createInvocationHandler() {
					return new InvocationHandler() {

						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							if(Mod.logExecutionCost) {
								Mod.tmpExecutionStartTime = System.nanoTime();
							}

							if(stopEat((Creature)args[1], (Item)args[2])) {
								return true;
							}

				    		if(Mod.logExecutionCost) {
				    			logger.log(Level.INFO, "setupEatInterception[hook2] done, spent " + Mod.executionLogDf.format((System.nanoTime() - Mod.tmpExecutionStartTime) / 1000000000.0) + "s");
				    		}

							Object result = method.invoke(proxy, args);
							return result;
						}
					};
				}
			});
		} catch (NotFoundException e) {
			logger.log(Level.SEVERE, "Failed to intercept 'eat', this probably means vampires can eat normal food this restart");
			throw new RuntimeException(e);
		}
	}


	static private void setupBuryInterception()
	{
		try {
			//
			// Vampires have a slower bury if it's a bloodless husk, to tempt them to leave them around.
			// In-theme excuse: it's a ritual they do... habits die hard
			//
			// private static boolean bury(Action act, Creature performer, Item source, Item corpse, float counter, short action)
			//
			String descriptor = Descriptor.ofMethod(CtPrimitiveType.booleanType, new CtClass[] {
				HookManager.getInstance().getClassPool().get("com.wurmonline.server.behaviours.Action"),
				HookManager.getInstance().getClassPool().get("com.wurmonline.server.creatures.Creature"),
				HookManager.getInstance().getClassPool().get("com.wurmonline.server.items.Item"),
				HookManager.getInstance().getClassPool().get("com.wurmonline.server.items.Item"),
				CtPrimitiveType.floatType,
				CtPrimitiveType.shortType
			});
			HookManager.getInstance().registerHook("com.wurmonline.server.behaviours.CorpseBehaviour", "bury", descriptor, new InvocationHandlerFactory()
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
			    	        
			    	        //if(((float)args[4]) == 1.0f && Vampires.isHalfOrFullVampire(((Creature)args[1]).getWurmId()) && BloodlessHusk.isBloodlessHusk(((Item)args[3]))) {
			    	        if(((float)args[4]) == 1.0f && BloodlessHusk.isBloodlessHusk(((Item)args[3]))) {
								int time = 900;		// 90s to bury for vampires
			    	        	((Action)args[0]).setTimeLeft(time);
			    	        	((Creature)args[1]).sendActionControl("Ritual of burying", true, time);
			    	        }

			    	        if(Mod.logExecutionCost) {
				    			logger.log(Level.INFO, "setupBuryInterception[hook1] done, spent " + Mod.executionLogDf.format((System.nanoTime() - Mod.tmpExecutionStartTime) / 1000000000.0) + "s");
				    		}

			    	        return result;
						}
					};
				}
			});
		} catch (NotFoundException e) {
			logger.log(Level.SEVERE, "Failed to intercept 'bury'");
			throw new RuntimeException(e);
		}
	}

	
	//
	// Intercept isStealth() in com/wurmonline/server/behaviours/Action.java poll() so that some actions can be performed while stealthed
	//
	static private void setupStealthActionInterception()
	{
        CtClass ctc;
		try {
			ctc = HookManager.getInstance().getClassPool().get("com.wurmonline.server.behaviours.Action");
	        ctc.getDeclaredMethod("poll").instrument(new ExprEditor(){

	        	// Well, this is why we are here (see above)
	            public void edit(MethodCall m) throws CannotCompileException {
	                if (m.getMethodName().equals("isStealth")) {
	                    m.replace("$_ = (performer.isStealth() && com.friya.wurmonline.server.vamps.Vampires.isVampire(performer.getWurmId()) ? com.friya.wurmonline.server.vamps.Vampires.isAllowedVampireStealthAction(this) == false : performer.isStealth());");
	    				logger.log(Level.INFO, "Applied interception of isStealth in Action.poll()");
	                    return;
	                }
	            }
	        });

	        /*
			// Intercept stealth() in MethodsCreatures on TestEnv to make it a faster cast
			if(Mod.isTestEnv()) {
				ctLoginHandler = HookManager.getInstance().getClassPool().get("com.wurmonline.server.behaviours.MethodsCreatures");
		        ctLoginHandler.getDeclaredMethod("stealth").instrument(new ExprEditor() {
		            public void edit(MethodCall m) throws CannotCompileException {
		                if (m.getMethodName().equals("sendActionControl")) {
		                    m.replace("$_ = $proceed($1, $2, 5);");
		    				logger.log(Level.INFO, "Applied interception of stealth() to make it faster cast");
		                    return;
		                }
		            }
		        });
			}
			*/
		} catch (NotFoundException | CannotCompileException e) {
			logger.log(Level.SEVERE, "Failed to apply Action.poll() means Vampire actions will never be performed in stealth", e);
		}
	}

	
	/**
	 * This will only ever apply to vampires. For now. Which means we have a lot of 
	 * written (stealth-)text that may apply to non-vamps, but fine, future...
	 * 
	 * Allowed stealth actions: 
	 * aid, bite, cripple, crownfind, devour, disarm, fly, sprint, trace, smash (but will break stealth after cast)
	 * 
	 * @param a
	 * @return
	 */
	static public boolean isAllowedVampireStealthAction(Action a)
	{
        if(Mod.logExecutionCost) {
			Mod.tmpExecutionStartTime = System.nanoTime();
		}

		short aNum = a.getActionEntry().getNumber();
		
		if(aNum == AidAction.getActionId()
			|| aNum == BiteAction.getActionId()
			|| aNum == CrippleAction.getActionId()
			|| aNum == CrownFindAction.getActionId()
			|| aNum == DevourAction.getActionId()
			|| aNum == DisarmAction.getActionId()
			|| aNum == FlyAction.getActionId()
			|| aNum == SprintAction.getActionId()
			|| aNum == TraceAction.getActionId()
			|| aNum == SmashAction.getActionId()
			|| aNum == AssistSlainAction.getActionId()
			//|| aNum == PinpointHumanoid.getActionId()
			) {
			//logger.log(Level.INFO, "Action " + aNum + " can be used in stealth");
			return true;
		}

        if(Mod.logExecutionCost) {
			logger.log(Level.INFO, "isAllowedVampireStealthAction done, spent " + Mod.executionLogDf.format((System.nanoTime() - Mod.tmpExecutionStartTime) / 1000000000.0) + "s");
		}

		// return false to break stealth of action's performer
		return false;
	}


	static private boolean stopEat(Creature creature, Item item)
	{
		if(item != null && (item.getTemplateId() == ItemList.sleepPowder/* || item.getTemplateId() == ItemList.sourceSalt*/)) {
			return false;
		}

		if(isHalfOrFullVampire(creature.getWurmId())) {
			creature.getCommunicator().sendNormalServerMessage("You have a lust for blood, normal food will not sustain you.");
			return true;
        }
		return false;
	}


	//performer, target, exchangedStatNum, exchangedStatName, vampireSkillLevelBefore, skillLoss, actionCount, slayerSkillLevelBefore, skillGain
	static public void createBite(Creature vampire, Creature slayer, int exchangedStatNum, String exchangedStatName, double vampireSkillLevelBefore, 
			double skillLoss, int actionCount, double slayerSkillLevelBefore, double skillGain, long staking)
	{
		try {
			String sql = "INSERT INTO FriyaVampireBites"
					+ " (vampireid, vampiresteamid, vampirename, slayerid, slayerstat, slayerstatname, slayerloststatlevel, slayerlostamount, slayerlostactions, vampirestatlevel,"
					+ " vampiregainedamount, bitetime, stakingid, slayersteamid, slayername)"
					+ " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			
			Connection dbcon = ModSupportDb.getModSupportDb();
			PreparedStatement ps = dbcon.prepareStatement(sql);

			int i = 1;
			ps.setLong(i++, vampire.getWurmId());
			ps.setString(i++, ((Player)vampire).getSteamId().toString());
			ps.setString(i++, vampire.getName());
			ps.setLong(i++, slayer.getWurmId());
			ps.setInt(i++, exchangedStatNum);
			ps.setString(i++, exchangedStatName);
			ps.setDouble(i++, slayerSkillLevelBefore);
			ps.setDouble(i++, skillLoss);
			ps.setInt(i++, actionCount);
			ps.setDouble(i++, vampireSkillLevelBefore);
			ps.setDouble(i++, skillGain);
			ps.setLong(i++, System.currentTimeMillis());
			ps.setLong(i++, staking);
			ps.setString(i++, ((Player)slayer).getSteamId().toString());
			ps.setString(i++, slayer.getName());
			
			ps.execute();
			ps.close();

		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to insert bite");
			throw new RuntimeException(e);
		}

	}

	public static Player[] getAll()
	{
		ArrayList<Player> ret = new ArrayList<Player>();
		Player[] players = Players.getInstance().getPlayers();
		for(Player p : players) {
			if(isVampire(p)) {
				ret.add(p);
			}
		}
		
		return ret.toArray(new Player[]{});
	}


	static void onPlayerLogin(Player p)
	{
		if(Mod.isTestEnv()) {
			logger.warning("Forcing hun-nut-sta-ccfp-bl-affs to test-values on testenv");
			CreaturesProxy.setHunNutSta((Creature)p, 0, 0, 0, 0);
			Skill bl = p.getSkills().getSkillOrLearn(VampSkills.BLOODLUST);
			bl.setKnowledge(80f, false);
			AffinitiesTimed.deleteTimedAffinitiesForPlayer(p.getWurmId());
		}


		if(isVampire(p)) {
			p.getCommunicator().sendAlertServerMessage("Welcome, dweller of darkness.", (byte)4);
			p.getCommunicator().sendNormalServerMessage("You are a vampire.");
			ChatCommands.cmdSlayers(p.getCommunicator(), "/slayers");
		}

		if(isHalfVampire(p)) {
			p.getCommunicator().sendNormalServerMessage("Welcome, dark one.", (byte)4);
			p.getCommunicator().sendNormalServerMessage("You lead the life of one who is entranced by the night.");
			p.getCommunicator().sendNormalServerMessage("You are infected by bloodlust.");
			p.getCommunicator().sendNormalServerMessage("You are half vampire.");
			p.getCommunicator().sendAlertServerMessage("You should find " + headVampireName + ". " + halfVampMakerName + "'s clue might help...", (byte)4);
		}
	}
	
	public static void broadcast(String message)
	{
		broadcast(message, false, false, false);
	}
	
	public static void broadcast(String message, boolean includeChat)
	{
		broadcast(message, includeChat, false, false);
	}
	
	public static void broadcast(String message, boolean includeChat, boolean playSound)
	{
		broadcast(message, includeChat, playSound, false);
	}
	

	public static void broadcast(String message, boolean includeChat, boolean playSound, boolean emptyLines)
	{
		Player[] players = Players.getInstance().getPlayers();

		Message covenMsg;
		
		Message emptyMsg = new Message(null, (byte)10, CovenChat.CHANNEL_NAME, "", 255, 90, 90);

		for(Player p : players) {
			if(isVampire(p) || Vampires.fakeGMs.contains(p.getName()) == true || p.getPower() > 2) {
				p.getCommunicator().sendAlertServerMessage(message, (byte)4);	// 4 = orange-ish floating text?

				if(includeChat) {
					if(emptyLines) {
						p.getCommunicator().sendMessage(emptyMsg);
					}
					covenMsg = new Message(null, (byte)10, CovenChat.CHANNEL_NAME, "      " + message, 255, 90, 90);		// 10 normal
					p.getCommunicator().sendMessage(covenMsg);
					if(emptyLines) {
						p.getCommunicator().sendMessage(emptyMsg);
					}
				}

				if(playSound) {
					p.playPersonalSound("sound.spawn.item.central");	// ehh, no idea what this sound is...

				}
			}
		}
	}
	

	/**
	 * Broadcasts only to Even log and *possibly* to chat.
	 * 
	 * @param message
	 * @param includeChat
	 */
	public static void broadcastLight(String message, boolean includeChat)
	{
		Player[] players = Players.getInstance().getPlayers();

		Message covenMsg;
		for(Player p : players) {
			if(isVampire(p) || Vampires.fakeGMs.contains(p.getName()) == true) {
				p.getCommunicator().sendAlertServerMessage(message, (byte)0);		// red in event log (only)
				if(includeChat) {
					covenMsg = new Message(null, (byte)10, CovenChat.CHANNEL_NAME, "      " + message, 255, 90, 90);		// 10 normal
					p.getCommunicator().sendMessage(covenMsg);
				}
			}
		}
	}


	/**
	 * This gets called roughly once per second for every player logged in.
	 * 
	 * @param player
	 */
	public static void poll()
	{
		Player[] players = Players.getInstance().getPlayers();

		for(Player p : players) {
			
			if (!p.isFullyLoaded() || p.loggedout) {
				continue;
			}
			
			if(Vampires.isHalfOrFullVampire(p.getWurmId())) {
				BloodLust.poll(p);
			}
			
		}
	}
	
	
	static void saveBitingStats()
	{
	}
	

	static void onServerStarted()
	{
		try {
			Connection con = ModSupportDb.getModSupportDb();
			String sql = "";
			
			if(ModSupportDb.hasTable(con, "FriyaVampires") == false) {
				sql = "CREATE TABLE FriyaVampires ("
					+ "		playerid				BIGINT			NOT NULL PRIMARY KEY,"
					+ "		steamid					VARCHAR(40)		NOT NULL DEFAULT '',"
					+ "		name					VARCHAR(40)		NOT NULL DEFAULT 'Unknown',"
					+ "		alias					VARCHAR(40)		NOT NULL DEFAULT 'Unknown',"
					+ "		vampirestatus			INT				NOT NULL DEFAULT 0,"
					+ "		halfstarttime			BIGINT			NOT NULL DEFAULT 0,"
					+ "		fullstarttime			BIGINT			NOT NULL DEFAULT 0,"
					+ "		fullendtime				BIGINT			NOT NULL DEFAULT 0"
					+ ")";
				PreparedStatement ps = con.prepareStatement(sql);
				ps.execute();
				ps.close();
			}

			// This is for statistics.
			if(ModSupportDb.hasTable(con, "FriyaVampireBites") == false) {
				sql = "CREATE TABLE FriyaVampireBites ("
					+ "		id						INTEGER			PRIMARY KEY AUTOINCREMENT,"
					+ "		vampireid				BIGINT			NOT NULL,"
					+ "		vampiresteamid			VARCHAR(40)		NOT NULL DEFAULT '',"
					+ "		vampirename				VARCHAR(40)		NOT NULL DEFAULT 'Unknown',"
					+ ""
					+ "		slayersteamid			VARCHAR(40)		NOT NULL DEFAULT '',"
					+ "		slayername				VARCHAR(40)		NOT NULL DEFAULT 'Unknown',"
					+ ""
					+ "		slayerid				BIGINT			NOT NULL DEFAULT 0,"
					+ "		slayerstat				INT				NOT NULL DEFAULT 0,"
					+ "		slayerstatname			VARCHAR(40)		NOT NULL DEFAULT '',"
					+ "		slayerloststatlevel		FLOAT			NOT NULL DEFAULT 0,"
					+ "		slayerlostamount		FLOAT			NOT NULL DEFAULT 0,"
					+ "		slayerlostactions		INT				NOT NULL DEFAULT 0,"
					+ ""
					+ "		vampirestatlevel		FLOAT			NOT NULL DEFAULT 0,"
					+ "		vampiregainedamount		FLOAT			NOT NULL DEFAULT 0,"
					+ ""
					+ "		bitetime				BIGINT			NOT NULL DEFAULT 0,"
					+ "		stakingid				INTEGER			NOT NULL DEFAULT -1"
					+ ")";
				PreparedStatement ps = con.prepareStatement(sql);
				ps.execute();
				ps.close();
			}

			
			if(columnExists(con, "FriyaVampireBites", "slayersteamid") == false) {
				PreparedStatement ps = con.prepareStatement("ALTER TABLE FriyaVampireBites ADD COLUMN slayersteamid	VARCHAR(40)	NOT NULL DEFAULT ''");
				ps.execute();
				ps.close();

				ps = con.prepareStatement("ALTER TABLE FriyaVampireBites ADD COLUMN slayername VARCHAR(40) NOT NULL DEFAULT 'Unknown'");
				ps.execute();
				ps.close();
			}
			
			
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		loadAll();
		
		// Create some test-characters if they don't exist
		if(Mod.isTestEnv() && createTestCharacters) {
			PlayerInfo pinf = PlayerInfoFactory.createPlayerInfo("Friya");
			if(pinf != null && isHalfOrFullVampire(pinf.getPlayerId()) == false) {
				logger.log(Level.INFO, "Creating Friya the Vampire");
				createOfflineVampire(pinf, false);
			}
	
			pinf = PlayerInfoFactory.createPlayerInfo("Artemis");
			if(pinf != null && isHalfOrFullVampire(pinf.getPlayerId()) == false) {
				logger.log(Level.INFO, "Creating Artemis the Vampire");
				createOfflineVampire(pinf, false);
			}
			
			pinf = PlayerInfoFactory.createPlayerInfo("Aurora");
			if(pinf != null && isHalfOrFullVampire(pinf.getPlayerId()) == false) {
				logger.log(Level.INFO, "Creating Aurora the Half Vampire");
				createOfflineVampire(pinf, true);
			}
		}

		// Zenath GM's (but they are not necessarily given any GM powers)
		if(Servers.localServer.getName().equals("Zenath")) {
	    	fakeGMs.add("Friya");
	    	fakeGMs.add("Jaygriff");
	    	fakeGMs.add("Raidsoft");
	    	logger.log(Level.INFO, "These characters will have Coven channel without being vampire: " + String.join(", ", fakeGMs));
		}

	}
	

	static private boolean columnExists(Connection con, String table, String column)
	{
		boolean found = false;
		ResultSet rs = null;
		PreparedStatement ps = null;
		
		try {
			ps = con.prepareStatement("PRAGMA table_info(" + table + ")");
			rs = ps.executeQuery();
			while(rs.next()) {
				if(rs.getString("name").equals(column)) {
					found = true;
					break;
				}
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Could not determine whether a column existed in a table, this might cause problems later on....", e);

		} finally {
			try {
				rs.close();
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return found;
	}
	
	
	static public boolean deVamp(Creature creature)
	{
		if(creature == null) {
			return false;
		}

		Vampire vampire = getVampire(creature.getWurmId());
		
		if(vampire == null) {
			return false;
		}
		
		int allocatedActions = 0;
		int deVampActionCost = 0;
		
		if(isVampire(creature.getWurmId())) {
			deVampActionCost = VAMPIRE_RETIREMENT_COST;
		} else if(isHalfVampire(creature.getWurmId())) {
			deVampActionCost = HALF_VAMPIRE_RETIREMENT_COST;
		}
		
		HashMap<Integer, Integer> skillsToPunish = new HashMap<Integer, Integer>();

		int i = 0;
		while(i++ < 50) {
			ActionSkillGain actionSkillGain = ActionSkillGains.getRandomHighSkillToPunish(creature);
	    	Skill vampireSkill = creature.getSkills().getSkillOrLearn(actionSkillGain.getId());
			int actionCount = actionSkillGain.getModifiedLostActionCount(vampireSkill.getKnowledge(), (deVampActionCost / 5), 0.25f);	// take max one fifth of total cost (can ofc hit it multiple times)
			allocatedActions += actionCount;

			if(skillsToPunish.containsKey(actionSkillGain.getId())) {
				skillsToPunish.put(actionSkillGain.getId(), skillsToPunish.get(actionSkillGain.getId()) + actionCount);
			} else {
				skillsToPunish.put(actionSkillGain.getId(), actionCount);
			}

			if(allocatedActions >= deVampActionCost) {
				break;
			}
		}

		logger.log(Level.INFO, "deVamp() about to devamp " + creature.getName());
		logger.log(Level.INFO, "deVamp() is half vampire: " + isHalfVampire(creature.getWurmId()));
		logger.log(Level.INFO, "deVamp() iterations to get to " + deVampActionCost + " actions: " + i);
		logger.log(Level.INFO, "deVamp() actions found: " + allocatedActions);
		logger.log(Level.INFO, "deVamp() skill losses: " + skillsToPunish.toString());
		
		for(int skillNum : skillsToPunish.keySet()) {
	    	Skill vampireSkill = creature.getSkills().getSkillOrLearn(skillNum);

	    	double vampireSkillLevelBefore = vampireSkill.getKnowledge();
			double skillLoss = ActionSkillGains.getSkill(skillNum).getRawSkillLossForActionCount(vampireSkill.getKnowledge(), skillsToPunish.get(skillNum));

			logger.log(Level.INFO, "Skill: " + vampireSkill.getName() + " Actions: " + skillsToPunish.get(skillNum) + " Before: " + vampireSkillLevelBefore + " Loss: " + skillLoss);
	    	vampireSkill.setKnowledge(vampireSkill.getKnowledge() - skillLoss, false, true);
		}

		// These are the things that are REALLY needed to devamp, the rest is just to balance the 
		// perks and prevent hopping between vampire/staker.
		deVampWithoutLoss(creature);

		return true;
	}
	

	static public boolean deVampWithoutLoss(Creature creature)
	{
		Vampire vampire = getVampire(creature.getWurmId());
		if(vampire == null) {
			return false;
		}

		vampire.setFullEndTime(System.currentTimeMillis());
		vampire.setVampireStatus(STATUS_NONE);
		Vampires.updateVampire(vampire);
		vampires.remove(creature.getWurmId());
		return true;
	}
	
	
	static private Vampire createOfflineVampire(PlayerInfo p, boolean half)
	{
		Vampire v = new Vampire(
				p.getPlayerId(),
				"unknown",
				p.getName(),
				CovenChat.generateAlias(p.getPlayerId(), p.creationDate),
				(half ? Vampires.STATUS_HALF : Vampires.STATUS_FULL),
				System.currentTimeMillis(),
				(!half ? System.currentTimeMillis() : 0),
				0
		);
		
		if(insertVampire(v)) {
			vampires.put(v.getId(), v);
			return v;
		}
		
		return null;
	}
	
	static public Vampire createVampire(Player p, boolean half)
	{
		Vampire v = new Vampire(
				p.getWurmId(),
				p.getSteamId().toString(),
				p.getName(),
				CovenChat.generateAlias(p),
				(half ? Vampires.STATUS_HALF : Vampires.STATUS_FULL),
				System.currentTimeMillis(),
				(!half ? System.currentTimeMillis() : 0),
				0
		);
		
		if(insertVampire(v)) {
			vampires.put(v.getId(), v);
			return v;
		}
		
		return null;
	}


	static public void updateVampire(Vampire v)
	{
		try {
			Connection dbcon = ModSupportDb.getModSupportDb();
			PreparedStatement ps = dbcon.prepareStatement("UPDATE FriyaVampires "
					+ "SET playerid = ?, steamid = ?, name = ?, alias = ?, vampirestatus = ?, halfstarttime = ?, fullstarttime = ?, fullendtime  = ? "
					+ "WHERE playerid = ?"
			);
			ps.setLong(1, v.getId());
			ps.setString(2, v.getSteamId());
			ps.setString(3, v.getName());
			ps.setString(4, v.getAlias());
			ps.setInt(5, v.getVampireStatus());
			ps.setLong(6, v.getHalfStartTime());
			ps.setLong(7, v.getFullStartTime());
			ps.setLong(8, v.getFullEndTime());
			ps.setLong(9, v.getId());
			ps.executeUpdate();
			ps.close();

		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to update database entry for existing vampire");
			throw new RuntimeException(e);
		}
	}

	static private boolean existsInDatabase(long playerId)
	{
		Connection dbcon = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		int foundCount = 0;
		
		try {
			dbcon = ModSupportDb.getModSupportDb();
		    ps = dbcon.prepareStatement("SELECT COUNT(*) AS cnt FROM FriyaVampires WHERE playerid = " + playerId);
		    rs = ps.executeQuery();

			if (rs.next()) {
				foundCount = rs.getInt("cnt");
		    }
			rs.close();
			ps.close();
		}
		catch (SQLException e) {
		    throw new RuntimeException(e);
		}
		
		return foundCount > 0;
	}

	static private boolean insertVampire(Vampire v)
	{
		logger.log(Level.INFO, "About to insert a vampire: " + v.toString());

		if(getVampire(v.getId()) != null && v.getVampireStatus() != 0) {
			logger.log(Level.SEVERE, "Can't create a vampire that already exists in database and are still a vampire; refusing...");
			return false;
		}

		if(existsInDatabase(v.getId())) {
			deleteVampire(v);
		}

		try {
			Connection dbcon = ModSupportDb.getModSupportDb();
			PreparedStatement ps = dbcon.prepareStatement("INSERT INTO FriyaVampires (playerid, steamid, name, alias, vampirestatus, halfstarttime, fullstarttime, fullendtime) VALUES(?,?,?,?,?,?,?,?)");
			ps.setLong(1, v.getId());
			ps.setString(2, v.getSteamId());
			ps.setString(3, v.getName());
			ps.setString(4, v.getAlias());
			ps.setInt(5, v.getVampireStatus());
			ps.setLong(6, v.getHalfStartTime());
			ps.setLong(7, v.getFullStartTime());
			ps.setLong(8, v.getFullEndTime());
			ps.execute();
			ps.close();

		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to create database entry for new vampire");
			throw new RuntimeException(e);
		}
		
		return true;
	}
	

	static private void deleteVampire(Vampire v)
	{
		logger.log(Level.INFO, "Deleting vampire: " + v.getName() + ", " + v.getId());

		try {
			Connection dbcon = ModSupportDb.getModSupportDb();
			PreparedStatement ps = dbcon.prepareStatement("DELETE FROM FriyaVampires WHERE playerid = ?");
			ps.setLong(1, v.getId());
			ps.execute();
			ps.close();

		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to delete vampire");
			throw new RuntimeException(e);
		}
	}
	
	
	static private void loadAll()
	{
		logger.log(Level.INFO, "Loading all Vampires...");

		Connection dbcon = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			dbcon = ModSupportDb.getModSupportDb();
		    ps = dbcon.prepareStatement("SELECT * FROM FriyaVampires WHERE fullendtime = 0 AND vampirestatus != 0");
		    rs = ps.executeQuery();

			while (rs.next()) {
		        vampires.put(rs.getLong("playerid"), new Vampire(
						rs.getLong("playerid"),
						rs.getString("steamid"),
						rs.getString("name"),
						rs.getString("alias"),
						rs.getInt("vampirestatus"),
						rs.getLong("halfstarttime"),
						rs.getLong("fullstarttime"),
						rs.getLong("fullendtime")
				));
		    }
			rs.close();
			ps.close();
		}
		catch (SQLException e) {
		    throw new RuntimeException(e);
		}
	}


	public static Vampire getVampire(long id)
	{
		if(vampires.containsKey(id)) {
			return vampires.get(id);
		}
		return null;
	}

	public static boolean isVampire(long id)
	{
		Vampire vamp = getVampire(id);
		return vamp != null && vamp.isFull();
	}

	public static boolean isHalfVampire(long id)
	{
		Vampire vamp = getVampire(id);
		return vamp != null && vamp.isHalf();
	}

	public static boolean isHalfOrFullVampire(long id)
	{
		return isVampire(id) || isHalfVampire(id);
	}

	public static boolean isVampire(Player p)
	{
		return isVampire(p.getWurmId());
	}

	public static boolean isHalfVampire(Player p)
	{
		return isHalfVampire(p.getWurmId());
	}

	public static boolean isHalfOrFullVampire(Player p)
	{
		return isHalfOrFullVampire(p.getWurmId());
	}

	static public void setStakedTeleportPosition(VolaTile t, int validSeconds)
	{
		EventDispatcher.add(new RemoveStakedTeleportEvent(validSeconds, Unit.SECONDS));
		lastStakedTile = t;

		logger.log(Level.INFO, "Setting teleport point for staking: " + lastStakedTile);
	}
	
	static public void clearStakedTeleportPosition()
	{
		logger.log(Level.INFO, "Clearing teleport point for staking: " + lastStakedTile);

		lastStakedTile = null;
	}

	static public VolaTile getStakedTeleportPosition()
	{
		return lastStakedTile;
	}

}
