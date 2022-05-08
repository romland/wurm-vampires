package com.friya.wurmonline.server.vamps.items;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.ItemTemplateBuilder;

import com.friya.wurmonline.server.vamps.DynamicExaminable;
import com.friya.wurmonline.server.vamps.DynamicExamine;
import com.friya.wurmonline.server.vamps.VampZones;
import com.wurmonline.server.MiscConstants;
import com.wurmonline.server.behaviours.Terraforming;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.AdvancedCreationEntry;
import com.wurmonline.server.items.CreationCategories;
import com.wurmonline.server.items.CreationEntryCreator;
import com.wurmonline.server.items.CreationRequirement;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTypes;
import com.wurmonline.server.skills.SkillList;
import com.wurmonline.server.zones.FocusZone;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;
import com.wurmonline.shared.constants.IconConstants;
import com.wurmonline.shared.constants.ItemMaterials;


public class AltarOfSouls implements ItemTypes, MiscConstants, ItemMaterials, DynamicExaminable
{
	private static Logger logger = Logger.getLogger(AltarOfSouls.class.getName());
	private static int itemId;
	private static AltarOfSouls instance;

	static public int getId()
	{
		return itemId;
	}
	
	public int getTemplateId()
	{
		return getId();
	}

	public static AltarOfSouls getInstance()
	{
		if(instance == null) {
			instance = new AltarOfSouls();
		}

		return instance; 
	}


	static public void onItemTemplatesCreated()
	{
		try {
			ItemTemplateBuilder itemTemplateBuilder = new ItemTemplateBuilder("friya.altarsouls");
			itemTemplateBuilder.name("altar of souls", "altars of souls", "Clearly dark magic. When placed on an uncluttered and flattened area inside a cave you can sacrifice corpses at it. When standing next to a charged altar, vampire soulfeed can never kill you.");
			itemTemplateBuilder.descriptions("excellent", "good", "ok", "poor");
			itemTemplateBuilder.itemTypes(new short[] { 
					ITEM_TYPE_NAMED,
					ITEM_TYPE_HASDATA,
					ITEM_TYPE_IMPROVEITEM,
					ITEM_TYPE_DECORATION,
					ITEM_TYPE_REPAIRABLE,
					ITEM_TYPE_USE_GROUND_ONLY,
					ITEM_TYPE_ONE_PER_TILE,
					ITEM_TYPE_OWNER_DESTROYABLE
					// TODO: make it not show up in piles
			});
			itemTemplateBuilder.imageNumber((short) IconConstants.ICON_ARTIFACT_VALREI);
			itemTemplateBuilder.behaviourType((short) 1);
			itemTemplateBuilder.combatDamage(0);
			itemTemplateBuilder.decayTime(3024000L);
			itemTemplateBuilder.dimensions(100, 100, 50);
			itemTemplateBuilder.bodySpaces(new byte[]{});
			itemTemplateBuilder.modelName("model.structure.rift.altar.1.");
			itemTemplateBuilder.weightGrams(95000);
			itemTemplateBuilder.material(MATERIAL_STONE);

			itemTemplateBuilder.difficulty(90.0f);
			itemTemplateBuilder.primarySkill(-10);

			//itemTemplateBuilder.isTraded(true);

			ItemTemplate tpl = itemTemplateBuilder.build();
			itemId = tpl.getTemplateId();
			logger.log(Level.INFO, "Using template id " + itemId);
			
			DynamicExamine.getInstance().listen((DynamicExaminable)(AltarOfSouls.getInstance()));
			
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

        logger.log(Level.INFO, "Setup completed");
	}

	static public void onServerStarted()
	{
		if (itemId > 0) {
			final AdvancedCreationEntry creationEntry = CreationEntryCreator.createAdvancedEntry(
					SkillList.MASONRY,
					ItemList.rock,
					ItemList.clay,
					itemId,
					false,
					false,
					0.0f,
					true,
					true,
					CreationCategories.ALTAR
			);
			
			creationEntry.addRequirement(new CreationRequirement(1, ItemList.rock, 400, true));
			creationEntry.addRequirement(new CreationRequirement(2, ItemList.dirtPile, 20, true));
			creationEntry.addRequirement(new CreationRequirement(3, ItemList.concrete, 42, true));
			creationEntry.addRequirement(new CreationRequirement(4, ItemList.clay, 200, true));
			creationEntry.addRequirement(new CreationRequirement(5, ItemList.charcoal, 75, true));
			
			creationEntry.addRequirement(new CreationRequirement(6, ItemList.diamond, 20, true));
			creationEntry.addRequirement(new CreationRequirement(7, ItemList.opal, 20, true));
			creationEntry.addRequirement(new CreationRequirement(8, ItemList.opalBlack, 1, true));
		}
	}


	static public byte getCharge(Item altar)
	{
		if(isInTheCoven(altar)) {
			return 127;
		}

		return ((altar.getAuxData() & 0xFF) < 0 ? 0 : altar.getAuxData());
	}


	static public void setCharge(Item altar, byte amount)
	{
		if((amount & 0xFF) < 0) {
			amount = 0;
		}
		
		if((amount & 0xFF) > 127) {
			amount = 127;
		}

		altar.setAuxData(amount);
	}


	static public boolean isCharged(Item altar)
	{
		return getCharge(altar) > 0 || isInTheCoven(altar);
	}

	
	static public boolean isInTheCoven(Item item)
	{
		Set<FocusZone> zones = FocusZone.getZonesAt(item.getTileX(), item.getTileY());

		for(FocusZone fz : zones) {
			if(fz.getName().equals(VampZones.getCovenZone().getName())) {
				return true;
			}
		}

		return false;
	}
	

	static public boolean isCleanArea(Item altar)
	{
		Item[] tmpItems = null;
		VolaTile tmpTile = null;

		for(int x = -2; x <= 2; x++) {
			for(int y = -2; y <= 2; y++) {

				// does tile contain anything that is NOT an altar?
				tmpTile = Zones.getTileOrNull(altar.getTileX() + x, altar.getTileY() + y, false);
				if(tmpTile == null) {
					continue;
				}
				
				if(Terraforming.isFlat(tmpTile.getTileX(), tmpTile.getTileY(), false, 0) == false) {
					return false;
				}
				
				tmpItems = tmpTile.getItems();
				
				if(tmpItems.length > 0 && tmpItems[0] != null && tmpItems[0].getTemplateId() != AltarOfSouls.getId()) {
					logger.info("Unclean. Bailing. Found a disallowed item around Altar of Souls: " + tmpItems[0]);
					return false;
				}
			}
		}

		return true;
	}


	@Override
	public String examine(Item altar, Creature performer)
	{
		String ret = "";
		
		// Is the altar even working?
		if(isCleanArea(altar) == false) {
			ret = "The surrounding area is too cluttered or choopy for it to function properly. ";
		}
		
		byte charge = getCharge(altar);

		if(charge > 120) {
			// 121-127
			ret += "It can hold no more souls.";
		} else if(charge > 80) {
			// 81-120
			ret += "It's at near capacity.";
		} else if(charge > 40) {
			// 41-80
			ret += "It's not nearly full.";
		} else if(charge > 10) {
			// 11-40
			ret += "It's running out of souls.";
		} else if(charge > 0) {
			// 0-10
			ret += "It's nearly out of souls.";
		} else {
			// empty
			ret += "It has no souls.";
		}

		return ret + (performer.getPower() > 2 ? " [" + charge + "]" : "");
	}
}
