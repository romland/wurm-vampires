package com.friya.wurmonline.server.vamps;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.friya.wurmonline.server.loot.LootItem;
import com.friya.wurmonline.server.loot.BeforeDropListener;
import com.friya.wurmonline.server.loot.LootResult;
import com.friya.wurmonline.server.loot.LootRule;
import com.friya.wurmonline.server.loot.LootSystem;
import com.friya.wurmonline.server.vamps.items.Amulet;
import com.friya.wurmonline.server.vamps.items.Crown;
import com.friya.wurmonline.server.vamps.items.SmallRat;
import com.wurmonline.server.items.Item;


public class CreatureLoot implements BeforeDropListener
{
	private static Logger logger = Logger.getLogger(CreatureLoot.class.getName());
	private static CreatureLoot instance;


	CreatureLoot()
	{
	}


	public static CreatureLoot getInstance()
	{
		if(instance == null) {
			instance = new CreatureLoot();
		}

		return instance; 
	}


	@Override
	public boolean onBeforeDrop(LootResult lootResult)
	{
		if(Mod.logExecutionCost) {
			logger.log(Level.INFO, "onBeforeDrop called");
			Mod.tmpExecutionStartTime = System.nanoTime();
		}

		Item[] items = lootResult.getItems();
		
		for(Item i : items) {
			// Force "locate" crown to rare
			if(i.getTemplateId() == Crown.getId()) {
				i.setRarity((byte)1);
				i.setQualityLevel(99.0f);
				i.setOriginalQualityLevel(99.0f);
			}
		}

		if(items.length > 0) {
			logger.log(Level.INFO, "LootTable drops: " + Arrays.toString(lootResult.getItems()) + " at " + lootResult.getCreature().getTileX() + ", " + lootResult.getCreature().getTileY());
		}

		if(Mod.logExecutionCost) {
			logger.log(Level.INFO, "onBeforeDrop done, spent " + Mod.executionLogDf.format((System.nanoTime() - Mod.tmpExecutionStartTime) / 1000000000.0) + "s");
		}

		// Returning false will discard any items that would otherwise drop.
		return true;
	}


	public static void onServerStarted() 
	{
		createLootRules();

		// Set up a onBeforeDropListener so that we get notifications when something might drop.
		LootSystem.getInstance().listen((BeforeDropListener)getInstance());
	}


	@SuppressWarnings("unused")
	static void createLootRules() 
	{
		String ruleName;
		LootSystem ls = LootSystem.getInstance();
		
		ruleName = "[all NPCs] mod:vamps, Ancient Amulet";

		if(ls.hasLootRule(ruleName) == false) {
			logger.log(Level.INFO, "Adding loot rule: " + ruleName);

			// This rule says: Every NPC has the chance to drop the contents from this loot-table.
			LootRule lr = new LootRule(ruleName);

			// The item(s) in question is/are:
			// We treat Wurm's item IDs as strings so that we can support multiple IDs per SQL statement (for those who don't go near code)
			LootItem[] li = new LootItem[]{
				// We specify some things, the rest are all default.
				new LootItem("" + Amulet.getId(), Item.MATERIAL_SERYLL, 0.25f, "Ceno")
			};

			ls.addLootRule(lr, li);
		}

		ruleName = "[all NPCs] mod:vamps, Crown";
		//ls.deleteRuleAndItsLootTable(ruleName);

		if(ls.hasLootRule(ruleName) == false) {
			logger.log(Level.INFO, "Adding loot rule: " + ruleName);

			// Every NPC
			LootRule lr = new LootRule(ruleName);

			// We treat Wurm's item IDs as strings so that we can support multiple IDs per SQL statement (for those who don't go near code)
			LootItem[] li = new LootItem[] {
				new LootItem("" + Crown.getId(), Item.MATERIAL_GOLD, 0.25f, "Zenath")
			};

			ls.addLootRule(lr, li);
		}

		ruleName = "[all NPCs] mod:vamps, Rat";
		//ls.deleteRuleAndItsLootTable(ruleName);

		if(ls.hasLootRule(ruleName) == false) {
			logger.log(Level.INFO, "Adding loot rule: " + ruleName);

			// Every NPC
			LootRule lr = new LootRule(ruleName);

			// We treat Wurm's item IDs as strings so that we can support multiple IDs per SQL statement (for those who don't go near code)
			LootItem[] li = new LootItem[] {
				new LootItem("" + SmallRat.getId(), Item.MATERIAL_ANIMAL, 0.3f, "Friya")
			};

			ls.addLootRule(lr, li);
		}

		if(false) {
			ruleName = "[all NPCs] Rare stuff";
			//ls.deleteRuleAndItsLootTable(ruleName);
	
			if(ls.hasLootRule(ruleName) == false) {
				logger.log(Level.INFO, "Adding loot rule: " + ruleName);
	
				// Every NPC
				LootRule lr = new LootRule(ruleName);
	
				// We treat Wurm's item IDs as strings so that we can support multiple IDs per SQL statement (for those who don't go near code)
				/*
				 * removed:
					174
					299
					300
					371
					372
					602
					805
					1009
				 */
	
				/* 
				 	select * from friyalootitems where id in (
						370,443,465,468,469,470,471,472,473,474,475,476,477,478,489,509,515,600,666,667,
						668,700,738,781,795,796,797,798,799,800,801,802,803,804,806,807,808,809,810,834,836,844,867,967,972,
						973,974,975,976,977,978,979,980,1014,1015,1032,1049,1050,1051,1052,1053,1054,1055,1056,1057,1058,1059,
						1060,1061,1062,1063,1064,1065,1066,1076,1077,1078,1079,1080,1081,1082,1083,1084,1085,1086,1087,1088,1089,
						1090,1092,1093,1094,1095,1099
					);
				 */
				
				/*
				 * removing sorcery as well:
				 * 795 796 797 798 799 800 801 802 803 804 806 807 808 809 810
				 */
	
				/*
				 * removed drake/scale pieces: 468, 469, 470, 471, 472, 473, 474, 475, 476, 477, 478
				 */
				
				/*
					SELECT lr.id AS LootRuleID, lr.rulename AS LootRuleName, lt.tableid AS LootTableID, lt.lootid AS LootItemID, li.itemids AS LootItemItems
						FROM FriyaLootRules AS lr
						INNER JOIN FriyaLootTables AS lt ON (lr.loottable = lt.tableid)
						INNER JOIN FriyaLootItems AS li ON (lt.lootid = li.id);
				 */
				/*
				 * removed brown potion: 836, (it's buggy -- can be used over and over)
				 */
				LootItem[] li = new LootItem[] {
					new LootItem(
						"370,443,465,489,509,515,600,666,667,"
					  + "668,700,738,781,834,844,867,967,972," 
					  + "973,974,975,976,977,978,979,980,1014,1015,1032,1049,1050,1051,1052,1053,1054,1055,1056,1057,1058,1059,"
					  + "1060,1061,1062,1063,1064,1065,1066,1076,1077,1078,1079,1080,1081,1082,1083,1084,1085,1086,1087,1088,1089,"
					  + "1090,1092,1093,1094,1095,1099",
						(1.0/102.0)		// This (1%) is per item, so divide with number of items in this collection (kept the division as it was despite removing items from list)
					)
				};
	
				ls.addLootRule(lr, li);
			} // if(false)
			
		}
	}
}
