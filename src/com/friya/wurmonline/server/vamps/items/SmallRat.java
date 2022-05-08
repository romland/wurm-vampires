package com.friya.wurmonline.server.vamps.items;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.ItemTemplateBuilder;

import com.wurmonline.server.MiscConstants;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTypes;
import com.wurmonline.shared.constants.ItemMaterials;


public class SmallRat implements ItemTypes, MiscConstants, ItemMaterials
{
	private static Logger logger = Logger.getLogger(SmallRat.class.getName());
	private static int itemId;

	static public int getId()
	{
		return itemId;
	}

	static public void onItemTemplatesCreated()
	{
		try {
			ItemTemplateBuilder itemTemplateBuilder = new ItemTemplateBuilder("friya.smallrat");
			itemTemplateBuilder.name("small rat", "small rats", "A dirty rat with beady little, evil-looking eyes.");
			itemTemplateBuilder.descriptions("excellent", "good", "ok", "poor");
			itemTemplateBuilder.itemTypes(
				new short[] {
					ITEM_TYPE_FOOD,
					ITEM_TYPE_FULLPRICE,
					ITEM_TYPE_NOSELLBACK
				}
			);

			itemTemplateBuilder.imageNumber((short) 561);		// cheese
			itemTemplateBuilder.behaviourType((short) 1);
			itemTemplateBuilder.combatDamage(0);
			itemTemplateBuilder.decayTime(3024000L);
			itemTemplateBuilder.dimensions(1, 1, 1);
			itemTemplateBuilder.primarySkill((int) NOID);
			itemTemplateBuilder.bodySpaces(new byte[]{});
			itemTemplateBuilder.modelName("model.creature.quadraped.rat.large.");
			itemTemplateBuilder.difficulty(5.0f);
			itemTemplateBuilder.weightGrams(200);
			itemTemplateBuilder.material(MATERIAL_ANIMAL);
			itemTemplateBuilder.value(10000 * 10);			// * silver
			itemTemplateBuilder.isTraded(false);			// must be set to false to be sold by traders
			
			ItemTemplate tpl = itemTemplateBuilder.build();
			itemId = tpl.getTemplateId();
			logger.log(Level.INFO, "Using template id " + itemId);

			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		logger.log(Level.INFO, "Setup completed");
	}

	static public void onServerStarted()
	{
	}
}