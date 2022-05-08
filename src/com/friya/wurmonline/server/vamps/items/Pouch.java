package com.friya.wurmonline.server.vamps.items;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.ItemTemplateBuilder;

import com.wurmonline.server.MiscConstants;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTypes;
import com.wurmonline.shared.constants.ItemMaterials;


public class Pouch implements ItemTypes, MiscConstants, ItemMaterials
{
	private static Logger logger = Logger.getLogger(Pouch.class.getName());
	private static int pouchId;

	static public int getId()
	{
		return pouchId;
	}
	
	static public void onItemTemplatesCreated()
	{
		try {
			ItemTemplateBuilder itemTemplateBuilder = new ItemTemplateBuilder("friya.velvetpouch");
			itemTemplateBuilder.name("black velvet pouch", "black pouches", "A black velvet pouch.");
			itemTemplateBuilder.descriptions("excellent", "good", "ok", "poor");
			itemTemplateBuilder.itemTypes(new short[] { 
					ITEM_TYPE_CLOTH,
					ITEM_TYPE_HOLLOW,
					ITEM_TYPE_COLORABLE
			});
			itemTemplateBuilder.imageNumber((short) 242);
			itemTemplateBuilder.behaviourType((short) 1);
			itemTemplateBuilder.combatDamage(0);
			itemTemplateBuilder.decayTime(3024000L);
			itemTemplateBuilder.dimensions(50, 50, 50);
			itemTemplateBuilder.primarySkill((int) NOID);
			itemTemplateBuilder.bodySpaces(EMPTY_BYTE_PRIMITIVE_ARRAY);
			itemTemplateBuilder.modelName("model.container.satchel.");
			itemTemplateBuilder.difficulty(5.0f);
			itemTemplateBuilder.weightGrams(500);
			itemTemplateBuilder.material(MATERIAL_MAGIC);
			
			ItemTemplate pouchTemplate = itemTemplateBuilder.build();
			pouchId = pouchTemplate.getTemplateId();
			logger.log(Level.INFO, "Using template id " + pouchId);
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

        logger.log(Level.INFO, "Setup completed");
	}

	static public void onServerStarted()
	{
	}
}