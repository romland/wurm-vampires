package com.friya.wurmonline.server.vamps;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.wurmonline.server.players.Achievement;
import com.wurmonline.server.players.AchievementTemplate;

public class VampAchievements
{
    private static Logger logger = Logger.getLogger(VampAchievements.class.getName());
    
    public static int STAKINGS = 818801;
    public static int STAKE_A_VAMPIRE = 818802;
    public static int STAKE_5_VAMPIRES = 818803;

    public static int POUCHES = 818821;
    public static int BUY_A_POUCH = 818822;

    public static int STAKED = 818831;
    public static int GET_STAKED = 818832;

    public static int FEED = 818841;
    
    public static int HALFVAMP = 818851;
    public static int FULLVAMP = 818852;

    public static int SOULFEED = 818853;
    public static int ALTAR_SOULFEED = 818854;
    
    public VampAchievements()
	{
	}


	static public void onServerStarted()
	{
		logger.log(Level.INFO, "Creating achievements...");

		// This is the "invisible" achievement that we want to trigger on to track number of stakings.
		addAchievement(
			new AchievementTemplate(
				STAKINGS,											// id
				"Invisible:Stakings",								// achievement name
				true,												// is invisible
				1,													// trigger on
				(byte)3,											// Achievement level: steel=1; 2=?; 3=silver; 4=gold; 5=diamond
				false,												// play update sound
				false,												// is one timer -- set to false if it should be repeated every time it happens, true if it can only happen once
				""													// requirement string (stated in goals)
			),
			null,													// Description of the achievement (listed in achievements when done)
			new int[] {												// This achievement is nothing, but it is meta for the following:
					STAKE_A_VAMPIRE,
					STAKE_5_VAMPIRES
			},
			new int[]{}												// Prerequisites
		);

		addAchievement(
			new AchievementTemplate(
				STAKE_A_VAMPIRE,									// id
				"Stake a Vampire",									// achievement name
				false,												// is invisible
				1,													// trigger on
				(byte)1,											// Achievement level: 1=?; 2=steel; 3=silver; 4=gold; 5=diamond
				true,												// play update sound
				true,												// is one timer -- set to false if it should be repeated every time it happens, true if it can only happen once
				"Find and stake a vampire"							// requirement string
			),
			"This is just one, there are more of them...",			// Description of the achievement (listed in achievements when done)
			new int[]{},											// Makes it a meta of the achievements listed in here (see STAKINGS)
			new int[]{}												// Prerequisites
		);

		addAchievement(
			new AchievementTemplate(
				STAKE_5_VAMPIRES,									// id
				"Stake five vampires",								// achievement name
				false,												// is invisible
				5,													// trigger on
				(byte)2,											// Achievement level: steel=1; 2=?; 3=silver; 4=gold; 5=diamond
				true,												// play update sound
				true,												// is one timer -- set to false if it should be repeated every time it happens, true if it can only happen once
				"Just a handful..."									// requirement string (stated in goals)
			),
			"On my way to a necklace...",							// Description of the achievement (listed in achievements when done)
			new int[]{},											// Makes it a meta of the achievements listed in here (see STAKINGS)
			new int[]{}												// Prerequisites
		);

		// This is the "invisible" achievement that we want to use to trigger others.
		addAchievement(
			new AchievementTemplate(
				POUCHES,											// id
				"Invisible:Pouches",								// achievement name
				true,												// is invisible
				1,													// trigger on
				(byte)1,											// Achievement level: steel=1; 2=?; 3=silver; 4=gold; 5=diamond
				false,												// play update sound
				false,												// is one timer -- set to false if it should be repeated every time it happens, true if it can only happen once
				""													// requirement string (stated in goals)
			),
			null,													// Description of the achievement (listed in achievements when done)
			new int[] {												// This achievement is nothing, but it is meta for the following:
					BUY_A_POUCH,
			},
			new int[]{}												// Prerequisites
		);

		addAchievement(
			new AchievementTemplate(
				BUY_A_POUCH,										// id
				"Wonder what's in it...",							// achievement name
				false,												// is invisible
				1,													// trigger on
				(byte)3,											// Achievement level: steel=1; 2=?; 3=silver; 4=gold; 5=diamond
				true,												// play update sound
				true,												// is one timer -- set to false if it should be repeated every time it happens, true if it can only happen once
				"Buy a pouch from Vampire Hunter D"					// requirement string (stated in goals)
			),
			"Should probably have read the instructions...",		// Description of the achievement (listed in achievements when done)
			new int[]{},											// Makes it a meta of the achievements listed in here (see STAKINGS)
			new int[]{}												// Prerequisites
		);

		// This is the "invisible" achievement that we want to trigger on to track number of stakings.
		addAchievement(
			new AchievementTemplate(
				STAKED,												// id
				"Invisible:Staked",									// achievement name
				true,												// is invisible
				1,													// trigger on
				(byte)3,											// Achievement level: steel=1; 2=?; 3=silver; 4=gold; 5=diamond
				false,												// play update sound
				false,												// is one timer -- set to false if it should be repeated every time it happens, true if it can only happen once
				""													// requirement string (stated in goals)
			),
			null,													// Description of the achievement (listed in achievements when done)
			new int[] {												// This achievement is nothing, but it is meta for the following:
					GET_STAKED,
			},
			new int[]{}												// Prerequisites
		);

		addAchievement(
			new AchievementTemplate(
				GET_STAKED,											// id
				"Ouch! Through the heart!",							// achievement name
				false,												// is invisible
				1,													// trigger on
				(byte)1,											// Achievement level: 1=?; 2=steel; 3=silver; 4=gold; 5=diamond
				true,												// play update sound
				true,												// is one timer -- set to false if it should be repeated every time it happens, true if it can only happen once
				"Don't."											// requirement string
			),
			"Luckily beaing near immortal helps...",				// Description of the achievement (listed in achievements when done)
			new int[]{},											// Makes it a meta of the achievements listed in here (see STAKINGS)
			new int[]{}												// Prerequisites
		);

		// devour
		addAchievement(
			new AchievementTemplate(
				FEED,												// id
				"Ahhh!",											// achievement name
				false,												// is invisible
				1,													// trigger on
				(byte)1,											// Achievement level: 1=?; 2=steel; 3=silver; 4=gold; 5=diamond
				true,												// play update sound
				true,												// is one timer -- set to false if it should be repeated every time it happens, true if it can only happen once
				"Feed on a corpse."									// requirement string
			),
			"Rat or not; tasty.",									// Description of the achievement (listed in achievements when done)
			new int[]{},											// Makes it a meta of the achievements listed in here (see STAKINGS)
			new int[]{}												// Prerequisites
		);
		
		// become half-vamp
		addAchievement(
			new AchievementTemplate(
				HALFVAMP,											// id
				"Becoming a dark one!",								// achievement name
				false,												// is invisible
				1,													// trigger on
				(byte)1,											// Achievement level: 1=?; 2=steel; 3=silver; 4=gold; 5=diamond
				true,												// play update sound
				true,												// is one timer -- set to false if it should be repeated every time it happens, true if it can only happen once
				"Become a half vampire."							// requirement string
			),
			"Must feed now.",										// Description of the achievement (listed in achievements when done)
			new int[]{},											// Makes it a meta of the achievements listed in here (see STAKINGS)
			new int[]{}												// Prerequisites
		);

		// become full-vamp
		addAchievement(
			new AchievementTemplate(
				FULLVAMP,											// id
				"Becoming a dweller of darkness!",					// achievement name
				false,												// is invisible
				1,													// trigger on
				(byte)1,											// Achievement level: 1=?; 2=steel; 3=silver; 4=gold; 5=diamond
				true,												// play update sound
				true,												// is one timer -- set to false if it should be repeated every time it happens, true if it can only happen once
				"Become a vampire."									// requirement string
			),
			"Mmmm blood.",											// Description of the achievement (listed in achievements when done)
			new int[]{},											// Makes it a meta of the achievements listed in here (see STAKINGS)
			new int[]{}												// Prerequisites
		);

		// get soulfeed
		addAchievement(
			new AchievementTemplate(
				SOULFEED,											// id
				"Soulfeed",											// achievement name
				false,												// is invisible
				1,													// trigger on
				(byte)1,											// Achievement level: 1=?; 2=steel; 3=silver; 4=gold; 5=diamond
				true,												// play update sound
				true,												// is one timer -- set to false if it should be repeated every time it happens, true if it can only happen once
				"Wait..."											// requirement string
			),
			"It fed on YOUR soul.",									// Description of the achievement (listed in achievements when done)
			new int[]{},											// Makes it a meta of the achievements listed in here (see STAKINGS)
			new int[]{}												// Prerequisites
		);

		// get soulfeed and be protected by altar
		addAchievement(
			new AchievementTemplate(
				ALTAR_SOULFEED,										// id
				"Protected Soul",									// achievement name
				false,												// is invisible
				1,													// trigger on
				(byte)1,											// Achievement level: 1=?; 2=steel; 3=silver; 4=gold; 5=diamond
				true,												// play update sound
				true,												// is one timer -- set to false if it should be repeated every time it happens, true if it can only happen once
				"Saved by the altar"								// requirement string
			),
			"It fed on Mr. Doe.",									// Description of the achievement (listed in achievements when done)
			new int[]{},											// Makes it a meta of the achievements listed in here (see STAKINGS)
			new int[]{}												// Prerequisites
		);

		logger.log(Level.INFO, "Done");
	}


	static private void addAchievement(AchievementTemplate tpl, String desc, int[] triggeredAchievements, int[] requiredAchievements)
	{
		if(triggeredAchievements != null && triggeredAchievements.length > 0) {
			tpl.setAchievementsTriggered(triggeredAchievements);
		}

		if(requiredAchievements != null && requiredAchievements.length > 0) {
			tpl.setRequiredAchievements(requiredAchievements);
		}
		
		if(desc != null) {
			tpl.setDescription(desc);
		}

		try {
			Method m = Achievement.class.getDeclaredMethod("addTemplate", AchievementTemplate.class);
			m.setAccessible(true);
			// use null if the method is static
			m.invoke(null, tpl);

		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}
}
