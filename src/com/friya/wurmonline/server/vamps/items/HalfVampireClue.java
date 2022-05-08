package com.friya.wurmonline.server.vamps.items;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.ItemTemplateBuilder;

import com.friya.wurmonline.server.vamps.Vampires;
import com.wurmonline.server.MiscConstants;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTypes;
import com.wurmonline.shared.constants.ItemMaterials;

public class HalfVampireClue implements ItemTypes, MiscConstants, ItemMaterials
{
	private static Logger logger = Logger.getLogger(HalfVampireClue.class.getName());
	private static int halfVampClueId;

	static public int getId()
	{
		return halfVampClueId;
	}

	static public void onItemTemplatesCreated()
	{
		try {
			ItemTemplateBuilder itemTemplateBuilder = new ItemTemplateBuilder("friya.halfvampclue");
			itemTemplateBuilder.name(Vampires.halfVampMakerName + "'s clue", Vampires.halfVampMakerName + "'s clues", "This is a papyrus sheet given to you by the half vampire " + Vampires.halfVampMakerName + ".");
			itemTemplateBuilder.descriptions("excellent", "good", "ok", "poor");
			itemTemplateBuilder.itemTypes(new short[] { 
					ITEM_TYPE_METAL,
					//ITEM_TYPE_ARMOUR,
					ITEM_TYPE_NO_IMPROVE
			});
			itemTemplateBuilder.imageNumber((short) 640);
			itemTemplateBuilder.behaviourType((short) 1);
			itemTemplateBuilder.combatDamage(0);
			itemTemplateBuilder.decayTime(3024000L);
			itemTemplateBuilder.dimensions(20, 20, 20);
			itemTemplateBuilder.primarySkill((int) NOID);
			itemTemplateBuilder.bodySpaces(new byte[]{ });
			itemTemplateBuilder.modelName("model.resource.sheet.");
			itemTemplateBuilder.difficulty(5.0f);
			itemTemplateBuilder.weightGrams(20);
			itemTemplateBuilder.material(MATERIAL_PAPER);
			
			ItemTemplate template = itemTemplateBuilder.build();
			halfVampClueId = template.getTemplateId();
			logger.log(Level.INFO, "Using template id " + halfVampClueId);
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

        logger.log(Level.INFO, "Setup completed");
	}

	static public void onServerStarted()
	{
		if (halfVampClueId > 0) {
		}
	}
}