package com.friya.wurmonline.server.vamps.items;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.ItemTemplateBuilder;

import com.wurmonline.server.MiscConstants;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTypes;
import com.wurmonline.shared.constants.ItemMaterials;

/*
100     public static final byte tabardSlot = 35;
101     public static final byte neckSlot = 36;
102     public static final byte lHeldSlot = 37;
103     public static final byte rHeldSlot = 38;
104     public static final byte lRingSlot = 39;
105     public static final byte rRingSlot = 40;
106     public static final byte quiverSlot = 41;
107     public static final byte backSlot = 42;
108     public static final byte beltSlot = 43;
109     public static final byte shieldSlot = 44;
110     public static final byte capeSlot = 45;
111     public static final byte lShoulderSlot = 46;
112     public static final byte rShoulderSlot = 47;
113     public static final byte inventory = 48;
*/

public class Amulet implements ItemTypes, MiscConstants, ItemMaterials
{
	private static Logger logger = Logger.getLogger(Amulet.class.getName());
	private static int amuletId;

	static public int getId()
	{
		return amuletId;
	}

	static public void onItemTemplatesCreated()
	{
		try {
			ItemTemplateBuilder itemTemplateBuilder = new ItemTemplateBuilder("friya.ancientamulet");
			itemTemplateBuilder.name("ancient amulet", "ancient amulets", "The amulet is of archaic design beaten into a thick coin of hammered bronze and hanging from a chain. Its time worn surface still shows a myriad of magical protection runes.");
			itemTemplateBuilder.descriptions("excellent", "good", "ok", "poor");
			itemTemplateBuilder.itemTypes(new short[] { 
					ITEM_TYPE_METAL,
					//ITEM_TYPE_ARMOUR,
					ITEM_TYPE_NO_IMPROVE
			});
			itemTemplateBuilder.imageNumber((short) 779);
			itemTemplateBuilder.behaviourType((short) 1);
			itemTemplateBuilder.combatDamage(0);
			itemTemplateBuilder.decayTime(3024000L);
			itemTemplateBuilder.dimensions(20, 20, 20);
			itemTemplateBuilder.primarySkill((int) NOID);
			itemTemplateBuilder.bodySpaces(new byte[]{ (byte)36 });		// supposedly neck?
			itemTemplateBuilder.modelName("model.magic.amulet.farwalker.");
			itemTemplateBuilder.difficulty(5.0f);
			itemTemplateBuilder.weightGrams(20);
			itemTemplateBuilder.material(MATERIAL_SERYLL);
			
			ItemTemplate mirrorTemplate = itemTemplateBuilder.build();
			amuletId = mirrorTemplate.getTemplateId();
			logger.log(Level.INFO, "Using template id " + amuletId);
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

        logger.log(Level.INFO, "Setup completed");
	}

	static public void onServerStarted()
	{
		if (amuletId > 0) {
			/*
			 * Create a CreationEntry
			 */
		}
	}
}