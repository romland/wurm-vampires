package com.friya.wurmonline.server.vamps.items;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.ItemTemplateBuilder;

import com.wurmonline.server.MiscConstants;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTypes;
import com.wurmonline.shared.constants.ItemMaterials;

public class VampireFang implements ItemTypes, MiscConstants, ItemMaterials
{
	private static Logger logger = Logger.getLogger(VampireFang.class.getName());
	private static int fangId;

	static public int getId()
	{
		return fangId;
	}

	static public void onItemTemplatesCreated()
	{
		try {
			ItemTemplateBuilder itemTemplateBuilder = new ItemTemplateBuilder("friya.vampirefang");
			itemTemplateBuilder.name("bloody fang", "bloody fangs", "The fang of a vampire.");
			itemTemplateBuilder.descriptions("excellent", "good", "ok", "poor");
			itemTemplateBuilder.itemTypes(new short[] { 
					ITEM_TYPE_WEAPON,
					ITEM_TYPE_NO_IMPROVE
			});
			itemTemplateBuilder.imageNumber((short) 495);
			itemTemplateBuilder.behaviourType((short) 1);
			itemTemplateBuilder.combatDamage(0);
			itemTemplateBuilder.decayTime(3024000L);
			itemTemplateBuilder.dimensions(1, 1, 1);
			itemTemplateBuilder.primarySkill((int) NOID);
			itemTemplateBuilder.bodySpaces(new byte[]{});		// supposedly neck?
			itemTemplateBuilder.modelName("model.resource.tooth.");
			itemTemplateBuilder.difficulty(5.0f);
			itemTemplateBuilder.weightGrams(20);
			itemTemplateBuilder.material(MATERIAL_ANIMAL);

			ItemTemplate tpl = itemTemplateBuilder.build();
			fangId = tpl.getTemplateId();
			logger.log(Level.INFO, "Using template id " + fangId);
			
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

        logger.log(Level.INFO, "Setup completed");
	}

	static public void onServerStarted()
	{
		if (fangId > 0) {
		}
	}
}