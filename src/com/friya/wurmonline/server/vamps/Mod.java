package com.friya.wurmonline.server.vamps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.DecimalFormat;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import org.gotti.wurmunlimited.modloader.classhooks.HookException;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.classhooks.InvocationHandlerFactory;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.ItemTemplatesCreatedListener;
import org.gotti.wurmunlimited.modloader.interfaces.PlayerLoginListener;
import org.gotti.wurmunlimited.modloader.interfaces.PlayerMessageListener;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.ServerPollListener;
import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;
import org.gotti.wurmunlimited.modsupport.vehicles.ModVehicleBehaviours;

import com.friya.wurmonline.server.loot.LootSystem;
import com.friya.wurmonline.server.vamps.actions.AbortAction;
import com.friya.wurmonline.server.vamps.actions.AdminDevampAction;
import com.friya.wurmonline.server.vamps.actions.AdminVampAction;
import com.friya.wurmonline.server.vamps.actions.AidAction;
import com.friya.wurmonline.server.vamps.actions.SacrificeAltarOfSoulsAction;
import com.friya.wurmonline.server.vamps.actions.AssistSlainAction;
import com.friya.wurmonline.server.vamps.actions.BiteAction;
import com.friya.wurmonline.server.vamps.actions.BuyKitAction;
import com.friya.wurmonline.server.vamps.actions.CrippleAction;
import com.friya.wurmonline.server.vamps.actions.CrownFindAction;
import com.friya.wurmonline.server.vamps.actions.DevampAction;
import com.friya.wurmonline.server.vamps.actions.DevourAction;
import com.friya.wurmonline.server.vamps.actions.DisarmAction;
import com.friya.wurmonline.server.vamps.actions.FlyAction;
import com.friya.wurmonline.server.vamps.actions.HalfVampAction;
import com.friya.wurmonline.server.vamps.actions.HalfVampClueAction;
import com.friya.wurmonline.server.vamps.actions.LabyrinthAction;
import com.friya.wurmonline.server.vamps.actions.LabyrinthRemoveAction;
import com.friya.wurmonline.server.vamps.actions.MakeSeryllStakeAction;
import com.friya.wurmonline.server.vamps.actions.MirrorAction;
import com.friya.wurmonline.server.vamps.actions.PolishMirrorAction;
import com.friya.wurmonline.server.vamps.actions.SacrificeOrlokAction;
import com.friya.wurmonline.server.vamps.actions.SenseAction;
import com.friya.wurmonline.server.vamps.actions.SmashAction;
import com.friya.wurmonline.server.vamps.actions.SprintAction;
import com.friya.wurmonline.server.vamps.actions.StakeAction;
import com.friya.wurmonline.server.vamps.actions.StealthAction;
import com.friya.wurmonline.server.vamps.actions.TestAction;
import com.friya.wurmonline.server.vamps.actions.ToplistVampsAction;
import com.friya.wurmonline.server.vamps.actions.TraceAction;
import com.friya.wurmonline.server.vamps.items.AltarOfSouls;
import com.friya.wurmonline.server.vamps.items.Amulet;
import com.friya.wurmonline.server.vamps.items.Crown;
import com.friya.wurmonline.server.vamps.items.HalfVampireClue;
import com.friya.wurmonline.server.vamps.items.Mirror;
import com.friya.wurmonline.server.vamps.items.Pouch;
import com.friya.wurmonline.server.vamps.items.SmallRat;
import com.friya.wurmonline.server.vamps.items.Stake;
import com.friya.wurmonline.server.vamps.items.VampireFang;
import com.wurmonline.server.Server;
import com.wurmonline.server.Servers;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.skills.Skill;
import com.wurmonline.server.skills.Skills;
import com.wurmonline.shared.constants.Version;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtPrimitiveType;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * Entry point.
 * 
 * @author Friya
 */
public class Mod implements WurmServerMod, Initable, Configurable, ServerStartedListener, 
							PreInitable, ItemTemplatesCreatedListener, ServerPollListener, 
							PlayerLoginListener, PlayerMessageListener
{
    private static Logger logger = Logger.getLogger(Mod.class.getName());
    private static long lastPoll = 0;

    private static boolean enableSkillsOnly = false;			// configuration option
    private static boolean executionCostLogging = false;		// configuration option
    public static String halfVampClueUrl = "http://filterbubbles.com/img/wu/half-vamp-clue2.png";
    
    public static boolean logExecutionCost = true;				// Enable to see execution cost of all aspects of this mod
    public static long tmpExecutionStartTime = 0;				// Keeping this as global variable so that excessive logging of time spent can be neatly disabled 
	public static DecimalFormat executionLogDf = new DecimalFormat("#.#########");
	public static double totalExecutionCost = 0;
	public static long totalExecutionCostStartTime = -1;


	public void configure(Properties properties)
	{
		logger.log(Level.INFO, "configure called");

		Mod.enableSkillsOnly = Boolean.valueOf(properties.getProperty("enableSkillsOnly", String.valueOf(Mod.enableSkillsOnly))).booleanValue();
		Mod.executionCostLogging = Boolean.valueOf(properties.getProperty("executionCostLogging", String.valueOf(Mod.executionCostLogging))).booleanValue();
		logExecutionCost = executionCostLogging;
		Mod.halfVampClueUrl = String.valueOf(properties.getProperty("halfVampClueUrl", String.valueOf(halfVampClueUrl)));

		if(enableSkillsOnly == false) {
			logger.log(Level.INFO, "          _   ,_,   _       ");
			logger.log(Level.INFO, "         / `'=) (='` \\       Configuring Vampires for Wurm Unlimited");
			logger.log(Level.INFO, "        /.-.-.\\ /.-.-.\\      (c)2016-2017 Friya (aka Friyanouce) <dgdhttpd@gmail.com>");
			logger.log(Level.INFO, "        `      \"      `      ");
		}

		logger.log(Level.INFO, "enableSkillsOnly: " + enableSkillsOnly);
		logger.log(Level.INFO, "executionCostLogging: " + executionCostLogging);
		logger.log(Level.INFO, "halfVampClueUrl: " + halfVampClueUrl);

		logger.log(Level.INFO, "all configure completed");


		logger.info("Major version: " + Version.getMajor());
	}


	public void preInit()
	{
		if(enableSkillsOnly) {
			return;
		}
	
		logger.log(Level.INFO, "preInit called");
		
		logger.log(Level.INFO, "all preInits completed");
	}


	public void init()
    {
		if(enableSkillsOnly) {
			return;
		}

		logger.log(Level.INFO, "init called");

		setUpLogoutTimeInterception();
		setUpNpcMovementPrevention();
		setUpMountSpeedInterception();
		setUpStealthDetectInterception();
		setupCovenChatHook();
		
		ModActions.init();
		ModVehicleBehaviours.init();

		logger.log(Level.INFO, "all init completed");
    }


	public void onItemTemplatesCreated()
	{
		logger.log(Level.INFO, "onItemTemplatesCreated called");

		thaw("com.wurmonline.server.players.Player");
		thaw("com.wurmonline.server.items.Item");

		if(enableSkillsOnly) {
			VampSkills.onItemTemplatesCreated(false);				// false: don't intercept skill gains
			VampTitles.onItemTemplatesCreated();					// black magic to modify enum...
			addItems();
			return;
		}

		DynamicExamine.onItemTemplatesCreated();
		
		startLogoutListener();
		startAlterSkillListener();									// increased meditation gains

		BloodlessHusk.onItemTemplatesCreated();						// modifies Item (add methods)

		VampSkills.onItemTemplatesCreated();						// modifies SkillSystem (inject hook)

		Vampires.onItemTemplatesCreated();							// n/a reflection
		BloodLust.onItemTemplatesCreated();							// n/a friendly hook
		//CovenChat.onItemTemplatesCreated();							// modifies Communicator (inject hook)
		VampTitles.onItemTemplatesCreated();						// black magic to modify enum...
		Creatures.onTemplatesCreated();
		Stakers.onItemTemplatesCreated();							// n/a friendly hook

		addItems();

		Traders.onServerStarted();

		logger.log(Level.INFO, "all onItemTemplatesCreated completed");
	}


	private void addItems()
	{
		SmallRat.onItemTemplatesCreated();							// n/a friendly hook
		Amulet.onItemTemplatesCreated();							// n/a friendly hook
		Stake.onItemTemplatesCreated();								// n/a friendly hook
		Pouch.onItemTemplatesCreated();								// n/a friendly hook
		Mirror.onItemTemplatesCreated();							// n/a friendly hook
		VampireFang.onItemTemplatesCreated();						// n/a friendly hook
		HalfVampireClue.onItemTemplatesCreated();
		Crown.onItemTemplatesCreated();
		AltarOfSouls.onItemTemplatesCreated();
	}
	

	private void setupItems()
	{
		Amulet.onServerStarted();
		Stake.onServerStarted();
		Pouch.onServerStarted();
		HalfVampireClue.onServerStarted();
		Crown.onServerStarted();
		AltarOfSouls.onServerStarted();
	}


	public void onServerStarted()
    {
		logger.log(Level.INFO, "onServerStarted called");

		if(enableSkillsOnly) {
			VampAchievements.onServerStarted();
			return;
		}
		
		// Do this one first because it's so spammy.
		ActionSkillGains.onServerStarted();
		Vampires.onServerStarted();
		Stakers.onServerStarted();

		setupItems();

		PriestSpells.onServerStarted();
		VampZones.onServerStarted();
		VampAchievements.onServerStarted();
		Creatures.onServerStarted();

		if(Mod.isTestEnv()) {
			ModActions.registerAction(new TestAction());			// test-env only
		}
		ModActions.registerAction(new DevourAction());				// n/a
		ModActions.registerAction(new BuyKitAction());				// n/a
		ModActions.registerAction(new StakeAction());				// n/a
		ModActions.registerAction(new SenseAction());				// n/a
		ModActions.registerAction(new SprintAction());				// n/a
		ModActions.registerAction(new BiteAction());				// n/a
		ModActions.registerAction(new CrippleAction());				// n/a
		ModActions.registerAction(new DisarmAction());				// n/a
		ModActions.registerAction(new AidAction());					// n/a
		ModActions.registerAction(new TraceAction());				// n/a
		ModActions.registerAction(new FlyAction());					// n/a
		ModActions.registerAction(new MirrorAction());				// n/a
		ModActions.registerAction(new PolishMirrorAction());		// n/a
		ModActions.registerAction(new MakeSeryllStakeAction());		// n/a
		ModActions.registerAction(new SmashAction());				// n/a
		ModActions.registerAction(new HalfVampAction());			// n/a
		ModActions.registerAction(new SacrificeOrlokAction());			// n/a
		ModActions.registerAction(new HalfVampClueAction());		// n/a
		ModActions.registerAction(new DevampAction());				// n/a
		ModActions.registerAction(new AdminDevampAction());			// n/a
		ModActions.registerAction(new AdminVampAction());			// n/a
		ModActions.registerAction(new CrownFindAction());			// n/a
		ModActions.registerAction(new AssistSlainAction());			// n/a
		ModActions.registerAction(new AbortAction());				// n/a
		ModActions.registerAction(new ToplistVampsAction());		// n/a
		ModActions.registerAction(new SacrificeAltarOfSoulsAction());		// n/a
		ModActions.registerAction(new StealthAction());				// n/a

		// These two are for GM's only at this point
		ModActions.registerAction(new LabyrinthAction());
		ModActions.registerAction(new LabyrinthRemoveAction());

		// Just to pre-load the loot-data
		LootSystem.getInstance();

		// We must have initialized the LootSystem before this (just in case tables were not created yet).
		CreatureLoot.onServerStarted();

		logger.log(Level.INFO, "all onServerStarted completed");
    }

	
    public void onPlayerLogin(Player p)
	{
		logger.info("Player login. Name: " + p.getName() + " SteamID: " + p.getSteamId());

		if(enableSkillsOnly) {
			return;
		}
		
		if(logExecutionCost) {
			logger.log(Level.INFO, "onPlayerLogin called");
			tmpExecutionStartTime = System.nanoTime();
		}
		
		Mod.loginVampire(p);
		Stakers.onPlayerLogin(p);

		if(logExecutionCost) {
			logger.log(Level.INFO, "onPlayerLogin done, spent " + Mod.executionLogDf.format((System.nanoTime() - Mod.tmpExecutionStartTime) / 1000000000.0) + "s");
		}
	}


	public static void loginVampire(Player p)
	{
		if(enableSkillsOnly) {
			return;
		}

		if(logExecutionCost) {
			logger.log(Level.INFO, "loginVampire called");
			tmpExecutionStartTime = System.nanoTime();
		}
		
		VampSkills.onPlayerLogin(p);
		CovenChat.onPlayerLogin(p);
		Vampires.onPlayerLogin(p);

		if(logExecutionCost) {
			logger.log(Level.INFO, "loginVampire done, spent " + Mod.executionLogDf.format((System.nanoTime() - Mod.tmpExecutionStartTime) / 1000000000.0) + "s");
		}
	}


	public void onPlayerLogout(Player p)
	{
		if(enableSkillsOnly) {
			return;
		}

		if(logExecutionCost) {
			logger.log(Level.INFO, "onPlayerLogout called");
			tmpExecutionStartTime = System.nanoTime();
		}
		
		Stakers.onPlayerLogout(p);

		if(logExecutionCost) {
			logger.log(Level.INFO, "onPlayerLogout done, spent " + Mod.executionLogDf.format((System.nanoTime() - Mod.tmpExecutionStartTime) / 1000000000.0) + "s");
		}
	}


	public void onServerPoll()
	{
		/*
		if(logExecutionCost && (System.currentTimeMillis() - totalExecutionCostStartTime) > 300000) {
			logger.log(Level.INFO, "onServerPoll() total time spent in Vamps code: " + (totalExecutionCost / 1000000000.0) + " seconds of " + Server.getSecondsUptime() + " seconds uptime");
			totalExecutionCostStartTime = System.currentTimeMillis();
			totalExecutionCost = 0;
		}
		*/

		if(enableSkillsOnly) {
			return;
		}

		if((System.currentTimeMillis() - lastPoll) < 1000)
			return;
		
		if(logExecutionCost) {
			tmpExecutionStartTime = System.nanoTime();
		}
		
		EventDispatcher.poll();
		Vampires.poll();
		Stakers.poll();
		
		lastPoll = System.currentTimeMillis();

		if(logExecutionCost) {
			logger.log(Level.INFO, "onServerPoll done, spent " + Mod.executionLogDf.format((System.nanoTime() - Mod.tmpExecutionStartTime) / 1000000000.0) + "s");
		}
	}
	

	// Each onChatMessage should return 'true'if it was intercepted.
	public boolean onPlayerMessage(Communicator c, String msg)
	{
		if(enableSkillsOnly) {
			return false;
		}

		if(logExecutionCost) {
			logger.log(Level.INFO, "onPlayerMessage called");
			tmpExecutionStartTime = System.nanoTime();
		}
		
		boolean intercepted = false;
		
		if(ChatCommands.onPlayerMessage(c, msg)) {
			intercepted = true;
		}
		
		if(logExecutionCost) {
			logger.log(Level.INFO, "onPlayerMessage done, spent " + Mod.executionLogDf.format((System.nanoTime() - Mod.tmpExecutionStartTime) / 1000000000.0) + "s");
		}

		return intercepted;
	}



	static void stopPrune(String className)
	{
        try {
    		ClassPool cp = HookManager.getInstance().getClassPool();
    		CtClass c = cp.get(className);
    		c.stopPruning(true);

    		logger.log(Level.INFO, "Stopped pruning " + className);
        }
        catch (NotFoundException e) {
            throw new HookException((Throwable)e);
        }
		
	}


	/**
	 * Defrost a frozen class. :D (naughty, use only if you know what you are doing)
	 * 
	 * @param className
	 */
	static void thaw(String className)
	{
        try {
    		ClassPool cp = HookManager.getInstance().getClassPool();
    		CtClass c = cp.get(className);
    		c.stopPruning(true);    	// don't delete the data if it is written out.
    		c.writeFile();
    		c.defrost();             	// now modifiable again
    		
    		logger.log(Level.INFO, "Thawed " + className);
        }
        catch (CannotCompileException | IOException | NotFoundException e) {
        //catch (NotFoundException e) {
            throw new HookException((Throwable)e);
        }

	}


	static void appendToFile(Exception e)
    {
        try {
            FileWriter fstream = new FileWriter("VampsException.txt", true);
            BufferedWriter out = new BufferedWriter(fstream);
            PrintWriter pWriter = new PrintWriter(out, true);
            e.printStackTrace(pWriter);
            pWriter.close();
        }
        catch (Exception ie) {
            throw new RuntimeException("Could not write Exception to file", ie);
        }
    }

	
	String loadString(String id)
    {
		try {
        	String path = System.getProperty("user.dir").replace(" ", "%20").replace("\\", "/");

        	URLClassLoader myLoader = java.net.URLClassLoader.newInstance(new URL[] {
				new URL("jar:file:/" + path + "/mods/vamps/vamps.jar!/com/friya/wurmonline/server/vamps/")
			});

			InputStream is = myLoader.getResourceAsStream(id);
            if (is == null)
            	throw new RuntimeException("Failed to load resource: " + id);
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder builder = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append(System.lineSeparator());
            }
            
            String ret = builder.toString();

            return ret;
            
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


	private void startLogoutListener()
	{
		logger.log(Level.INFO, "startLogoutListener()");
		
		String descriptor = Descriptor.ofMethod(CtClass.voidType, new CtClass[] {});
		HookManager.getInstance().registerHook("com.wurmonline.server.players.Player", "logout", descriptor, new InvocationHandlerFactory()
		{
			@Override
			public InvocationHandler createInvocationHandler() {
				return new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						Object result = method.invoke(proxy, args);
						
						if(proxy instanceof Player) {
							onPlayerLogout((Player)proxy);
						}
						
						return result;
					}
				};
			}
		});
	}


	// XXX: Not sure what idea I had when I was doing this. It is currently not used at least.
	@SuppressWarnings("unused")
	private void setupFreeDeathInterception()
	{
		logger.log(Level.INFO, "setupFreeDeathInterception()");
		
		String descriptor = Descriptor.ofMethod(CtClass.voidType, new CtClass[] {
			CtPrimitiveType.booleanType,
			CtPrimitiveType.intType,
			CtPrimitiveType.intType
		});
		HookManager.getInstance().registerHook("com.wurmonline.server.players.Player", "setDeathEffects", descriptor, new InvocationHandlerFactory()
		{
			@Override
			public InvocationHandler createInvocationHandler() {
				return new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						// TODO: check if suiciding
						Object result = method.invoke(proxy, args);
						return result;
					}
				};
			}
		});
	}


	private void startAlterSkillListener()
	{
		/**
		 * This is so that we can properly simulate skill-gains per action.
		 * We need to interfere in call of alterSkill and pretend we have
		 * server-bonus and 99 nutrition.
		 * 
		 * Additionally, it will give a skill increase for meditation skills.
		 */
		String descriptor = Descriptor.ofMethod(CtClass.voidType, new CtClass[] {
				CtPrimitiveType.doubleType,
				CtPrimitiveType.booleanType,
				CtPrimitiveType.floatType,
				CtPrimitiveType.booleanType,
				CtPrimitiveType.doubleType,
		});

		HookManager.getInstance().registerHook("com.wurmonline.server.skills.Skill", "alterSkill", descriptor, new InvocationHandlerFactory()
		{
			@Override
			public InvocationHandler createInvocationHandler() {
				return new InvocationHandler() {
					
					// protected void alterSkill(double advanceMultiplicator, boolean decay, float times, boolean useNewSystem, double skillDivider)
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						if(logExecutionCost) {
							tmpExecutionStartTime = System.nanoTime();
						}
						
						Skill s = (Skill)proxy;
						//
						// Vampires have increased skill gain in meditation
						// [15:03:31] Meditating increased by 1.0000 to 3.0000		- with modifier (same without modifier!)
						// [15:09:06] Meditating increased by 1.0000 to 21.0000		-- with modifier (vampire)
						// [15:11:10] Meditating increased by 0.6076 to 20.6076		-- without modifier
						if(s.getNumber() == 10086 && s.getId() != -152) {
					        Field field = Skill.class.getDeclaredField("parent");
					        field.setAccessible(true);
							Skills skills = (Skills)(field.get(s));

							long playerId = skills.getId();
							
							if(Vampires.isVampire(playerId)) {
								Double multiplier = (double)args[0];
								args[0] = multiplier * 2.5;
							}
						}

						// This is so we can properly simulate skill gains. Any skill we simulate we set to ID of -152.
						if(s.getId() == -152) {
							double advanceMultiplicator = (double)args[0];
							advanceMultiplicator *= (Servers.localServer.EPIC ? 3.0 : 1.5);

							// We have stamina, we have 99 nutrition.
							float staminaMod = 1.0f;
							staminaMod += Math.max(0.99 / 10.0f - 0.05f, 0.0f);
							advanceMultiplicator *= (double)staminaMod;

							args[0] = advanceMultiplicator;
						}

						if(logExecutionCost) {
							logger.log(Level.INFO, "startAlterSkillListener[hook] done, spent " + Mod.executionLogDf.format((System.nanoTime() - Mod.tmpExecutionStartTime) / 1000000000.0) + "s");
						}
						
						Object result = method.invoke(proxy, args);
						return result;
					}
				};
			}
		});
	}
	

	static private void setUpLogoutTimeInterception()
	{
    	logger.log(Level.INFO, "doing setUpLogoutTimeInterception()");

		String descriptor;

		descriptor = Descriptor.ofMethod(CtClass.intType, new CtClass[] {});

		HookManager.getInstance().registerHook("com.wurmonline.server.players.Player", "getSecondsToLogout", descriptor, new InvocationHandlerFactory()
		{
			@Override
			public InvocationHandler createInvocationHandler() {
				return new InvocationHandler() {

					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						Object result = method.invoke(proxy, args);

						if(logExecutionCost) {
							tmpExecutionStartTime = System.nanoTime();
						}
						
						if((int)result < 60 && Stakers.isHunted((Player)proxy)) {
							//logger.log(Level.INFO, "Intercepting getSecondsToLogout() for hunted slayer, forcing 60 second logout");
							result = 60;
						}
						
						if(logExecutionCost) {
							logger.log(Level.INFO, "setUpLogoutTimeInterception[hook] done, spent " + Mod.executionLogDf.format((System.nanoTime() - Mod.tmpExecutionStartTime) / 1000000000.0) + "s");
						}

						return result;
					}
				};
			}
		});
	}
	
		
	//
	// Prevent movement of related NPC's
	//
	static private void setUpNpcMovementPrevention()
	{
		String descriptor = Descriptor.ofMethod(CtPrimitiveType.voidType, new CtClass[] {
			CtPrimitiveType.intType,
		});
		HookManager.getInstance().registerHook("com.wurmonline.server.creatures.Creature", "startPathing", descriptor, new InvocationHandlerFactory()
		{
			@Override
			public InvocationHandler createInvocationHandler() {
				return new InvocationHandler() {

					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						
						if(logExecutionCost) {
							tmpExecutionStartTime = System.nanoTime();
						}

						if(((Creature)proxy).isNpc() && Creatures.stopNpcMoveHook((Creature)proxy)) {
							//logger.log(Level.INFO, "Stopping move: " + ((Creature)proxy).getName());
							return null;
						}

						if(logExecutionCost) {
							logger.log(Level.INFO, "setUpNpcMovementPrevention[hook] done, spent " + Mod.executionLogDf.format((System.nanoTime() - Mod.tmpExecutionStartTime) / 1000000000.0) + "s");
						}

						Object result = method.invoke(proxy, args);
						return result;
					}
				};
			}
		});
	}


	// NOTE:  Stakers.isHunted() is very cheap. Adding lag logging here would spam the log, so let's not.
	static private void setUpMountSpeedInterception()
	{
		String descriptor = Descriptor.ofMethod(CtPrimitiveType.floatType, new CtClass[] {
			CtPrimitiveType.booleanType
		});
		HookManager.getInstance().registerHook("com.wurmonline.server.creatures.Creature", "getTraitMovePercent", descriptor, new InvocationHandlerFactory()
		{
			@Override
			public InvocationHandler createInvocationHandler() {
				return new InvocationHandler() {

					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						Object result = method.invoke(proxy, args);
						
						if((float)result > 0 && Stakers.isHunted(((Creature)proxy).getMountVehicle().pilotId)) {
							result = (Object)0f;
						}
						
						return result;
					}
				};
			}
		});

	}


	static private void setUpStealthDetectInterception()
	{
        try {
			String descriptor = Descriptor.ofMethod(CtPrimitiveType.booleanType, new CtClass[] {
				HookManager.getInstance().getClassPool().get("com.wurmonline.server.creatures.Creature"),
				CtPrimitiveType.floatType
			});
			HookManager.getInstance().registerHook("com.wurmonline.server.creatures.Creature", "visibilityCheck", descriptor, new InvocationHandlerFactory()
			{
				@Override
				public InvocationHandler createInvocationHandler() {
					return new InvocationHandler() {
	
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							Creature watcher;
							Creature me;

							// Only interfere in the result if both are players and none are admins
							if((me = (Creature)proxy) instanceof Player && me.isStealth() && (watcher = (Creature)args[0]) instanceof Player && me.getPower() == 0 && watcher.getPower() == 0) {
								if(Vampires.isVampire(me.getWurmId())) {
									//logger.info(me.getName() + " the vampire will be kept hidden from " + watcher.getName() + "!");
									return false;
								}
							}

							Object result = method.invoke(proxy, args);
							return result;
						}
					};
				}
			});
        }
        catch (NotFoundException e) {
            throw new HookException((Throwable)e);
        }
	}
	

    private static void setupCovenChatHook()
	{
    	ClassPool cp = HookManager.getInstance().getClassPool();

        try {
			CtClass c;
			c = cp.get("com.wurmonline.server.creatures.Communicator");

			c.getDeclaredMethod("reallyHandle_CMD_MESSAGE").instrument(new ExprEditor() {
				public void edit(MethodCall m) throws CannotCompileException {
					if(m.getMethodName().equals("isGlobalKingdomChat")) {
						m.replace(""
							+ "com.friya.wurmonline.server.vamps.CovenChat.message(title, message, this.player);"
							+ "$_ = $proceed($$);"
						);
						logger.log(Level.INFO, "applied hook for channel chat");
						return;
					}
				}
		    });
        }
        catch (CannotCompileException | NotFoundException e) {
            throw new HookException((Throwable)e);
        }
	}


    static public String fixActionString(Creature c, String s)
	{
        s = s.replace("%HIS", (c.isNotFemale() ? "his" : "her"));
        s = s.replace("%NAME", c.getName());
        s = s.replace("%NAME'S", c.getName() + "'s");
        s = s.replace("%HIMSELF", (c.isNotFemale() ? "himself" : "herself"));
        s = s.replace("%HIM", (c.isNotFemale() ? "him" : "her"));
        return s;
	}


	static public void actionNotify(Creature c, @Nullable String myMsg, @Nullable String othersMsg, @Nullable String stealthOthersMsg, @Nullable Creature[] excludeFromBroadCast)
	{
		if(excludeFromBroadCast != null && excludeFromBroadCast.length > 0) {
			//Server.getInstance().broadCastAction(this.getNameWithGenus() + " stops following " + this.leader.getNameWithGenus() + ".", this.leader, this, 5);
			//logger.log(Level.INFO, "TODO: Exclude creatures in actionNotify() -- broadcast will not give message to passed in creature, I think");
		}
		
		if(myMsg != null) {
			myMsg = fixActionString(c, myMsg);
			c.getCommunicator().sendNormalServerMessage(myMsg);
		}

		if(stealthOthersMsg != null && c.isStealth()) {
			stealthOthersMsg = fixActionString(c, stealthOthersMsg);
			Server.getInstance().broadCastAction(stealthOthersMsg, c, 8);
		} else if(othersMsg != null) {
			othersMsg = fixActionString(c, othersMsg);
			Server.getInstance().broadCastAction(othersMsg, c, 8);
		}
	}

	static public void actionNotify(Creature c, @Nullable String myMsg, @Nullable String othersMsg, @Nullable String stealthOthersMsg)
	{
		actionNotify(c, myMsg, othersMsg, stealthOthersMsg, null);
	}


	static public void debug(String s)
	{
		logger.log(Level.INFO, s);
	}
	
	
	static public boolean isTestEnv()
	{
		return Servers.localServer.getName().equals("Friya");
	}
}
