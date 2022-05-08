package com.friya.wurmonline.server.vamps.items;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.ItemTemplateBuilder;

import com.wurmonline.server.MiscConstants;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTypes;
import com.wurmonline.shared.constants.ItemMaterials;

public class Crown implements ItemTypes, MiscConstants, ItemMaterials
{
	private static Logger logger = Logger.getLogger(Crown.class.getName());
	private static int crownId;

	static public int getId()
	{
		return crownId;
	}

	static public void onItemTemplatesCreated()
	{
		try {
			ItemTemplateBuilder itemTemplateBuilder = new ItemTemplateBuilder("friya.crown");
			itemTemplateBuilder.name("Crown of Friya", "Crowns of Friya", "This abominable crown was lost in ancient times. It is made of white metal, studded with green emeralds. It can be used to find two-legged friends.");
			itemTemplateBuilder.descriptions("excellent", "good", "ok", "poor");
			itemTemplateBuilder.itemTypes(new short[] { 
					ITEM_TYPE_METAL,
					ITEM_TYPE_ARMOUR,
					ITEM_TYPE_NO_IMPROVE
			});
			itemTemplateBuilder.imageNumber((short) 974);
			itemTemplateBuilder.behaviourType((short) 1);
			itemTemplateBuilder.combatDamage(0);
			itemTemplateBuilder.decayTime(3024000L);
			itemTemplateBuilder.dimensions(20, 20, 20);
			itemTemplateBuilder.primarySkill((int) NOID);
			itemTemplateBuilder.bodySpaces(new byte[]{ 1, 28 });				// head slots
			itemTemplateBuilder.modelName("model.artifact.crownmight.");
			itemTemplateBuilder.difficulty(5.0f);
			itemTemplateBuilder.weightGrams(2152);
			itemTemplateBuilder.material(MATERIAL_GOLD);
			
			ItemTemplate template = itemTemplateBuilder.build();
			crownId = template.getTemplateId();
			logger.log(Level.INFO, "Using template id " + crownId);
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

        logger.log(Level.INFO, "Setup completed");
	}

	static public void onServerStarted()
	{
		if (crownId > 0) {
		}
	}
}