package com.friya.wurmonline.server.vamps;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.classhooks.InvocationHandlerFactory;

import com.friya.tools.EnumBuster;
import com.wurmonline.server.DbConnector;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.players.PlayerInfo;
import com.wurmonline.server.players.PlayersProxy;
import com.wurmonline.server.players.Titles;
import com.wurmonline.server.players.Titles.Title;
import com.wurmonline.server.players.Titles.TitleType;
import com.wurmonline.server.utils.DbUtilities;

import javassist.CtClass;
import javassist.CtPrimitiveType;
import javassist.bytecode.Descriptor;


public class VampTitles
{
	private static Logger logger = Logger.getLogger(VampTitles.class.getName());
	private static Title[] titleArray;

	public static int VAMPIRE_SLAYER = 818801;
	public static int VAMPIRE_HUNTER = 818802;
	public static int VAN_HELSING = 818803;
	public static int ESCAPIST = 818804;
	
	// To set a title for a player, use, e.g.: player.addTitle(VampTitles.getTitle(VAMPIRE_SLAYER));
	public VampTitles()
	{
	}
	
/*
	static private void outputTitles()
	{
		Mod.debug("titleArray");
		for(Title t : titleArray) {
			Mod.debug("t: " + t.getName() + " - " + t.getTitleId());
		}
	}
*/

	static public boolean hasTitle(Creature c, int titleId)
	{
		if(c.isPlayer()) {
			Title[] titles = ((Player)c).getTitles();

			for(Title title : titles) {
				if(title == null) {
					throw new RuntimeException("We have NULL in titles collection, that is not nice at all!");
				}
				if(title.getTitleId() == titleId) {
					return true;
				}
			}
		} else {
			// We pretend NPC's have all titles
			return true;
		}
		
		return false;
	}
	
	
	static public void onItemTemplatesCreated()
	{
		interceptLoadTitles();
		
	    EnumBuster<Titles.Title> buster = new EnumBuster<Titles.Title>(Titles.Title.class, Titles.Title.class);

        createTitle(buster, "VAMPIRE_SLAYER", VAMPIRE_SLAYER, "Vampire Slayer", -1, TitleType.MINOR);	// stake a vampire
        createTitle(buster, "VAMPIRE_HUNTER", VAMPIRE_HUNTER, "Vampire Hunter", -1, TitleType.MINOR);	// buy a pouch
        createTitle(buster, "VAN_HELSING", VAN_HELSING, "van Helsing", -1, TitleType.MINOR);			// stake 25 vampires
        createTitle(buster, "ESCAPIST", ESCAPIST, "Escapist", -1, TitleType.MINOR);						// get away unscathed after a hunt
	    
        titleArray = Titles.Title.values();
	}


	static private void createTitle(EnumBuster<Titles.Title> buster, String enumName, int id, String title, int skillId, TitleType type)
	{
		Titles.Title testTitle = buster.make(enumName, 0, new Class[]
	    	{
                int.class,
                String.class,
                String.class,
                int.class,
                TitleType.class
	        },
			new Object[] {
				id,
				title,				// male
				title,				// TODO: female
				skillId,
				type
			}
    	);

		buster.addByValue(testTitle);
		
		
		logger.log(Level.INFO, "Created title: " + title);
	}

	static private void interceptLoadTitles()
	{
		String descriptor;

		//
		// Intercept loadTitles, we want to fix any NULL in there and insert our own titles
		//
		descriptor = Descriptor.ofMethod(CtPrimitiveType.voidType, new CtClass[] {
				CtClass.longType
		});
		HookManager.getInstance().registerHook("com.wurmonline.server.players.DbPlayerInfo", "loadTitles", descriptor, new InvocationHandlerFactory()
		{
			@Override
			public InvocationHandler createInvocationHandler() {
				return new InvocationHandler() {

					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						
						if(Mod.logExecutionCost) {
							Mod.tmpExecutionStartTime = System.nanoTime();
						}

		    	        Object result = method.invoke(proxy, args);

		    	        PlayerInfo pi = (PlayerInfo)proxy;
						Set<Titles.Title> titles = PlayersProxy.getTitles(pi);
						//Mod.debug("Titles after loadTitles(): " + titles.toString());
						
						titles.remove(null);	// Must do this since the real method inserted NULL(s) on missing titles

						Connection dbcon = null;
						PreparedStatement ps = null;
						ResultSet rs = null;
						try {
							dbcon = DbConnector.getPlayerDbCon();
							ps = dbcon.prepareStatement("select TITLEID from TITLES where WURMID=?");
							ps.setLong(1, pi.getPlayerId());
							rs = ps.executeQuery();
							while (rs.next()) {
								// For every null we get from getTitle() here, use our own getTitle()
								if(Titles.Title.getTitle(rs.getInt("TITLEID")) == null) {
									titles.add( VampTitles.getTitle(rs.getInt("TITLEID")) );
								}
							}
						}
						catch (SQLException ex) {
							logger.log(Level.INFO, "Failed to load titles for  " + pi.getPlayerId(), ex);
						}
						finally {
							DbUtilities.closeDatabaseObjects(ps, rs);
							DbConnector.returnConnection(dbcon);
						}
						
						if(Mod.logExecutionCost) {
							logger.log(Level.INFO, "interceptLoadTitles[hook] done, spent " + Mod.executionLogDf.format((System.nanoTime() - Mod.tmpExecutionStartTime) / 1000000000.0) + "s");
						}
						
						return result;
					}
				};
			}
		});
	}
	
	
	static public Title getTitle(int titleAsInt)
	{
		for (int i = 0; i < titleArray.length; ++i) {
			if (titleAsInt != titleArray[i].getTitleId()) {
				continue;
			}
			
			return titleArray[i];
		}
		
		throw new RuntimeException("Could not find title: " + titleAsInt);
	}
}
