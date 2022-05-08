package com.friya.wurmonline.server.vamps.items;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.ItemTemplateBuilder;

import com.wurmonline.server.MiscConstants;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTypes;


public class Stake implements ItemTypes, MiscConstants
{
	private static Logger logger = Logger.getLogger(Stake.class.getName());
	private static int stakeId;
	
	public static byte STATUS_READY = 0;
	public static byte STATUS_RECOVERING = 126;
	public static byte STATUS_WIELDING = 127;

	static public int getId()
	{
		return stakeId;
	}

	// We have a cooldown of five seconds after wielding before they can use it. 
	// This is used to tell players that they can now use the stake...
	static public boolean handleEvent(Object[] args)
	{
		return true;
	}
	
	static public void onItemTemplatesCreated()
	{
		/*
		 * Create the item template.
		 * 
		 * onItemTemplatesCreated is called right after ItemTemplateCreator.createItemTemplates() finished. 
		 * Other item templates should be added after this hook to make them available during server startup.
		 * 
		 * This method uses the ItemTemplateBuilder. While it's more text than the simple createItemTemplate(......) call it's more readable.
		 */

		try {
			ItemTemplateBuilder itemTemplateBuilder = new ItemTemplateBuilder("friya.stake");
			itemTemplateBuilder.name("stake of vampire banishment", "stakes of vampire banishment", "This is a thick, pointed, wooden stake. It is made of a very hard wood of some type, and has been crafted into a formidable weapon for a short stick. It has been sanded smooth and chiselled to a point at one end. There are grooves carved in its shaft to improve your grip and magical runes of undead banishment spiraled around it. This would be the perfect weapon to 'stake' a vampire with. The runes will know the blood of a true vampire, and will punish you for using the stake on anything else. If you are wielding this weapon, you are considered a vampire hunter. Once wielded, the only way to get rid of it is to either stake a vampire or toss it in a garbage heap.");
			itemTemplateBuilder.descriptions("excellent", "good", "ok", "poor");
			itemTemplateBuilder.itemTypes(new short[] { 
					ITEM_TYPE_WOOD,
					//ITEM_TYPE_NODROP,
					//ITEM_TYPE_NOTRADE,
					ITEM_TYPE_NOBANK,
					//ITEM_TYPE_NODISCARD,
					ITEM_TYPE_WEAPON,
					ITEM_TYPE_WEAPON_PIERCE,
					ITEM_TYPE_TWOHANDED
					//ITEM_TYPE_NOTAKE,
					//ITEM_TYPE_REPAIRABLE,
					//ITEM_TYPE_TURNABLE,
					//ITEM_TYPE_DECORATION,
					//ITEM_TYPE_DESTROYABLE,
					//ITEM_TYPE_ONE_PER_TILE,
					//ITEM_TYPE_VEHICLE,
					//ITEM_TYPE_IMPROVEITEM,
					//ITEM_TYPE_OWNER_DESTROYABLE,
					//ITEM_TYPE_OWNER_TURNABLE,
					//ITEM_TYPE_OWNER_MOVEABLE
			});
			itemTemplateBuilder.imageNumber((short) 60);
			itemTemplateBuilder.behaviourType((short) 41);
			itemTemplateBuilder.combatDamage(0);
			itemTemplateBuilder.decayTime(9072000L);
			itemTemplateBuilder.dimensions(3, 7, 10);
			itemTemplateBuilder.primarySkill((int) NOID);
			itemTemplateBuilder.bodySpaces(EMPTY_BYTE_PRIMITIVE_ARRAY);
			//itemTemplateBuilder.modelName("model.resource.shaft.birchwood.");
			itemTemplateBuilder.modelName("model.part.tenon.");
			itemTemplateBuilder.difficulty(5.0f);
			itemTemplateBuilder.weightGrams(3000);
			itemTemplateBuilder.material((byte) 14);
			itemTemplateBuilder.imageNumber((short)646);		// icon; make it look like a shaft - com/wurmonline/server/items/ItemTemplateCreator.java
			
			ItemTemplate stakeTemplate = itemTemplateBuilder.build();
			stakeId = stakeTemplate.getTemplateId();
			logger.log(Level.INFO, "Using template id " + stakeId);
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

        logger.log(Level.INFO, "Setup completed");
	}
	
	static public void onServerStarted()
	{
		// for stake, include ingredients:
		//	- kelp (can not be planted, kelp tiles will most likely be ruined by vampires -- but that's okay, stakes can be bought)
		
		if (stakeId > 0) {
			/*
			 * CreationEntry
			 */
		}
	}

}