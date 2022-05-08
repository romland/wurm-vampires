-- This is a custom item called ancient amulet in my case, 1% drop-chance.
INSERT INTO FriyaLootItems
	(id, itemids, dropchance) VALUES(
		100001,					-- the LOOT id of this item
		'22763',				-- the actual Wurm ID of the item
		1						-- 1% drop chance
	);

-- Let's create a new loot-table for this amulet
INSERT INTO FriyaLootTables
	(tableid, lootid) VALUES
	(
		2,						-- loot-table 2
		100001					-- the id of the amulet we added to loot-items
	);

INSERT INTO FriyaLootRules
	(loottable, rulename) VALUES
	(
		2,				-- For this rule, use the loot-table we created above
		'Ancient Amulet Global drop'
						-- Note the complete lack of ANY rules. This means
						-- every NPC in the game will be able to drop the
						-- items specified by this loot-table.
	);
