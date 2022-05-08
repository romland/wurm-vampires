package com.friya.wurmonline.server.vamps.items;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.ItemTemplateBuilder;

import com.wurmonline.server.MiscConstants;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTypes;
import com.wurmonline.shared.constants.ItemMaterials;


public class Mirror implements ItemTypes, MiscConstants, ItemMaterials
{
	private static Logger logger = Logger.getLogger(Mirror.class.getName());
	private static int mirrorId;

	static public int getId()
	{
		return mirrorId;
	}

	static public void onItemTemplatesCreated()
	{
		try {
			ItemTemplateBuilder itemTemplateBuilder = new ItemTemplateBuilder("friya.slayermirror");
			itemTemplateBuilder.name("silver mirror", "silver mirrors", "This is a very shiny silver mirror and you look marvellous. You can probably use it to check other individuals' reflections too.");
			itemTemplateBuilder.descriptions("excellent", "good", "ok", "poor");
			itemTemplateBuilder.itemTypes(new short[] { 
					ITEM_TYPE_METAL,
					ITEM_TYPE_NO_IMPROVE
			});
			itemTemplateBuilder.imageNumber((short) 920);
			itemTemplateBuilder.behaviourType((short) 1);
			itemTemplateBuilder.combatDamage(0);
			itemTemplateBuilder.decayTime(3024000L);
			itemTemplateBuilder.dimensions(7, 7, 7);
			itemTemplateBuilder.primarySkill((int) NOID);
			itemTemplateBuilder.bodySpaces(EMPTY_BYTE_PRIMITIVE_ARRAY);
			itemTemplateBuilder.modelName("model.tool.handmirror.");
			itemTemplateBuilder.difficulty(5.0f);
			itemTemplateBuilder.weightGrams(500);
			itemTemplateBuilder.material(MATERIAL_SILVER);
			
			ItemTemplate mirrorTemplate = itemTemplateBuilder.build();
			mirrorId = mirrorTemplate.getTemplateId();
			logger.log(Level.INFO, "Using template id " + mirrorId);
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

        logger.log(Level.INFO, "Setup completed");
	}

	static public void onServerStarted()
	{
	}
}