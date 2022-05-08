package com.friya.wurmonline.server.vamps;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.classhooks.InvocationHandlerFactory;

import com.wurmonline.server.Server;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureTemplate;
import com.wurmonline.server.creatures.CreatureTemplateFactory;
import com.wurmonline.server.creatures.CreatureTemplateIds;
import com.wurmonline.server.creatures.CreaturesProxy;
import com.wurmonline.server.creatures.ai.AiProxy;
import com.wurmonline.server.creatures.ai.ChatManager;
import com.wurmonline.server.kingdom.Kingdoms;
import com.wurmonline.server.skills.Skills;
import com.wurmonline.server.skills.SkillsFactory;
import com.wurmonline.server.villages.Village;
import com.wurmonline.server.villages.Villages;
import com.wurmonline.server.zones.Zones;
import com.wurmonline.shared.constants.CreatureTypes;

import javassist.CtClass;
import javassist.CtPrimitiveType;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;

public class Creatures
{
	static public int vampireId = 8712300;
	static public int vampireGuardId = 8712301;
	static public int protectedHumanId = 8712302;
	static public int blackPetDragonId = 8712303;
	
	private static Logger logger = Logger.getLogger(Creatures.class.getName());
	
	static void onTemplatesCreated()
	{
		String name = "Vampire";
		String longDesc = "I would not mess with this one...";
		
		setupShutUpHook();
		
		Skills skills = SkillsFactory.createSkills(name);
		skills.learnTemp(102, 20.0f);
		skills.learnTemp(104, 20.0f);
		skills.learnTemp(103, 20.0f);
		skills.learnTemp(100, 20.0f);
		skills.learnTemp(101, 20.0f);
		skills.learnTemp(105, 20.0f);
		skills.learnTemp(106, 20.0f);
		
		try {
			CreatureTemplate tmp = CreatureTemplateFactory.getInstance().createCreatureTemplate(
				vampireId,											// int id,
				name,												// String name, 
				"Vampires",
				longDesc,											// String longDesc, 
				"model.creature.humanoid.avenger.light",			// Yes! But smaller!
				new int[]{											// int[] types, 	com/wurmonline/server/creatures/CreatureTypes.java
					CreatureTypes.C_TYPE_GHOST, 
					CreatureTypes.C_TYPE_SPIRIT_GUARD,
					CreatureTypes.C_TYPE_SWIMMING,
					CreatureTypes.C_TYPE_HUNTING,
					CreatureTypes.C_TYPE_BURNING,
					CreatureTypes.C_TYPE_INVULNERABLE
				},
				(byte)0, 											// byte bodyType,	0=human 1=horse 2=bear 3=dog 4=ettin 5=cyclops 6=dragon 7=bird 8=spider 9=snake 
				skills, 											// Skills skills, 
				(short)5,											// short vision,  was 5 
				(byte)0, 											// byte sex, 
				(short)180,											// short centimetersHigh, 
				(short)20, 											// short centimetersLong, 
				(short)35, 											// short centimetersWide, 
				"sound.death.spirit.male", 							// String deathSndMale, 
				"sound.death.spirit.female", 						// String deathSndFemale, 
				"sound.combat.hit.spirit.male", 					// String hitSndMale, 
				"sound.combat.hit.spirit.female", 					// String hitSndFemale, 
				0.1f, 												// float naturalArmour, 
				70.0f, 												// float handDam,
				70.0f, 												// float kickDam, 
				70.0f, 												// float biteDam, 
				70.0f, 												// float headDam, 
				0.0f, 												// float breathDam, 
				1.5f,												// float speed, 
				100, 												// int moveRate,
				new int[0], 										// int[] itemsButchered, 
				40, 												// int maxHuntDist, 
				100													// int aggress
				, (byte)0												// NEW IN 1.3: byte meatMaterial (just keeping it at 0 to see what happens)
			);
			tmp.setSizeModX(10);
			tmp.setSizeModY(10);
			tmp.setSizeModZ(10);

			// -------------------------------
			// Non-ghosty HOTS spirit shadow
			// -------------------------------
			name = Vampires.headVampireName + "'s watchman";
			longDesc = "I would not mess with this one either...";

			skills.learnTemp(102, 30.0f);
			skills.learnTemp(104, 30.0f);
			skills.learnTemp(103, 35.0f);
			skills.learnTemp(100, 17.0f);
			skills.learnTemp(101, 27.0f);
			skills.learnTemp(105, 24.0f);
			skills.learnTemp(106, 24.0f);
			skills.learnTemp(10052, 80.0f);
			/*
			int[] types = new int[]{
				// original: 22, 23, 12, 13
				//CreatureTypes.C_TYPE_GHOST,
				CreatureTypes.C_TYPE_SPIRIT_GUARD,
				CreatureTypes.C_TYPE_SWIMMING,
				CreatureTypes.C_TYPE_HUNTING
			};
			*/

			tmp = CreatureTemplateFactory.getInstance().createCreatureTemplate(
				vampireGuardId,										// int id,
				name,												// String name, 
				"Watchmen",
				longDesc,											// String longDesc, 
				"model.creature.humanoid.human.spirit.shadow",		// 
				new int[]{											// int[] types, 	com/wurmonline/server/creatures/CreatureTypes.java
					//CreatureTypes.C_TYPE_GHOST, 
					CreatureTypes.C_TYPE_INVULNERABLE
				},
				(byte)0, 											// byte bodyType,	0=human 1=horse 2=bear 3=dog 4=ettin 5=cyclops 6=dragon 7=bird 8=spider 9=snake 
				skills, 											// Skills skills, 
				(short)5,											// short vision,  was 5 
				(byte)0, 											// byte sex, 
				(short)180,											// short centimetersHigh, 
				(short)20, 											// short centimetersLong, 
				(short)35, 											// short centimetersWide, 
				"sound.death.spirit.male", 							// String deathSndMale, 
				"sound.death.spirit.female", 						// String deathSndFemale, 
				"sound.combat.hit.spirit.male", 					// String hitSndMale, 
				"sound.combat.hit.spirit.female", 					// String hitSndFemale, 
				0.4f, 												// float naturalArmour, 
				3.0f, 												// float handDam, 
				5.0f, 												// float kickDam, 
				0.0f, 												// float biteDam, 
				0.0f, 												// float headDam, 
				0.0f, 												// float breathDam, 
				1.5f,												// float speed, 
				0, 													// int moveRate,
				new int[0], 										// int[] itemsButchered, 
				100, 												// int maxHuntDist, 
				100												// int aggress
				, (byte)0												// NEW IN 1.3: byte meatMaterial (just keeping it at 0 to see what happens)
			);
			
			tmp.setHandDamString("claw");
			tmp.setKickDamString("claw");
			tmp.setAlignment(-70.0f);
			tmp.setBaseCombatRating(25.0f);
			tmp.combatDamageType = 1;
			tmp.setMaxGroupAttackSize(4);
			//tmp.hasHands = true;
			tmp.setSizeModX(40);
			tmp.setSizeModY(40);
			tmp.setSizeModZ(40);
		
			// -------------------------------
			// Black pet dragon
			// -------------------------------
			name = "Domestic Black Dragon";
			skills.learnTemp(102, 20.0f);
			skills.learnTemp(104, 20.0f);
			skills.learnTemp(103, 30.0f);
			skills.learnTemp(100, 5.0f);
			skills.learnTemp(101, 4.0f);
			skills.learnTemp(105, 10.0f);
			skills.learnTemp(106, 1.0f);
			skills.learnTemp(10052, 8.0f);
			int[] types = new int[]{7, 41, 3, 43, 14, 9, 28, 32};

			CreatureTemplate temp = CreatureTemplateFactory.getInstance().createCreatureTemplate(
				blackPetDragonId, name, name+"s", longDesc,
				"model.creature.dragon.black",
				types, (byte)1, skills, 
				(short)3, (byte)0, (short)180, (short)50, (short)250, 
				"sound.death.dragon", "sound.death.dragon", "sound.combat.hit.dragon", "sound.combat.hit.dragon", 
				1.0f, 2.0f, 2.0f, 3.0f, 4.0f, 0.0f, 0.5f, 100, new int[]{307, 306, 140, 71, 309, 308, 312, 312}, 5, 10
				, (byte)0												// NEW IN 1.3: byte meatMaterial (just keeping it at 0 to see what happens)
			);
			temp.keepSex = true;
			temp.setMaxAge(100);
			temp.setBaseCombatRating(1.0f);
			temp.setChildTemplateId(50);
			temp.setMateTemplateId(49);
			temp.setMaxGroupAttackSize(2);
			temp.combatDamageType = 0;
			temp.setMaxPercentOfCreatures(0.0f);
			temp.setSizeModX(35);
			temp.setSizeModY(35);
			temp.setSizeModZ(35);
			
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Could not create creature template(s)", e);
		}
	
	}


	//
	// We need to intercept NPC's chatting so we can shut some of them up.
	//
	private static void setupShutUpHook()
	{
		String descriptor;

		try {
			descriptor = Descriptor.ofMethod(CtPrimitiveType.voidType, new CtClass[] {
				HookManager.getInstance().getClassPool().get("com.wurmonline.server.players.Player"),
				HookManager.getInstance().getClassPool().get("java.lang.String"),
				CtPrimitiveType.booleanType
			});
			HookManager.getInstance().registerHook("com.wurmonline.server.creatures.ai.ChatManager", "createAndSendMessage", descriptor, new InvocationHandlerFactory()
			{
				@Override
				public InvocationHandler createInvocationHandler() {
					return new InvocationHandler() {
	
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

							if(Mod.logExecutionCost) {
								Mod.tmpExecutionStartTime = System.nanoTime();
							}

							if(shutUpNpcHook(AiProxy.getChatManagerOwner((ChatManager)proxy))) {
								AiProxy.clearChatManagerChats((ChatManager)proxy);

								if(Mod.logExecutionCost) {
									logger.log(Level.INFO, "setupShutUpHook[hook1.1] done, spent " + Mod.executionLogDf.format((System.nanoTime() - Mod.tmpExecutionStartTime) / 1000000000.0) + "s");
								}
								return null;
							}

							if(Mod.logExecutionCost) {
								logger.log(Level.INFO, "setupShutUpHook[hook1.2] done, spent " + Mod.executionLogDf.format((System.nanoTime() - Mod.tmpExecutionStartTime) / 1000000000.0) + "s");
							}
							
							Object result = method.invoke(proxy, args);
							return result;
						}
					};
				}
			});

			descriptor = Descriptor.ofMethod(CtPrimitiveType.voidType, new CtClass[] {
				HookManager.getInstance().getClassPool().get("com.wurmonline.server.Message"),
				HookManager.getInstance().getClassPool().get("java.lang.String")
			});
			HookManager.getInstance().registerHook("com.wurmonline.server.creatures.ai.ChatManager", "answerLocalChat", descriptor, new InvocationHandlerFactory()
			{
				@Override
				public InvocationHandler createInvocationHandler() {
					return new InvocationHandler() {
	
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
	
							if(Mod.logExecutionCost) {
								Mod.tmpExecutionStartTime = System.nanoTime();
							}
							
							if(shutUpNpcHook(AiProxy.getChatManagerOwner((ChatManager)proxy))) {
								AiProxy.clearChatManagerChats((ChatManager)proxy);
	
								if(Mod.logExecutionCost) {
									logger.log(Level.INFO, "setupShutUpHook[hook2.1] done, spent " + Mod.executionLogDf.format((System.nanoTime() - Mod.tmpExecutionStartTime) / 1000000000.0) + "s");
								}
								return null;
							}
	
							if(Mod.logExecutionCost) {
								logger.log(Level.INFO, "setupShutUpHook[hook2.2] done, spent " + Mod.executionLogDf.format((System.nanoTime() - Mod.tmpExecutionStartTime) / 1000000000.0) + "s");
							}
							
							Object result = method.invoke(proxy, args);
							return result;
						}
					};
				}
			});

			descriptor = Descriptor.ofMethod(CtPrimitiveType.voidType, new CtClass[] {
			});
			HookManager.getInstance().registerHook("com.wurmonline.server.creatures.ai.ChatManager", "startLocalChat", descriptor, new InvocationHandlerFactory()
			{
				@Override
				public InvocationHandler createInvocationHandler() {
					return new InvocationHandler() {
	
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
	
							if(Mod.logExecutionCost) {
								Mod.tmpExecutionStartTime = System.nanoTime();
							}
							
							if(shutUpNpcHook(AiProxy.getChatManagerOwner((ChatManager)proxy))) {
								AiProxy.clearChatManagerChats((ChatManager)proxy);
	
								if(Mod.logExecutionCost) {
									logger.log(Level.INFO, "setupShutUpHook[hook3.1] done, spent " + Mod.executionLogDf.format((System.nanoTime() - Mod.tmpExecutionStartTime) / 1000000000.0) + "s");
								}
								return null;
							}
	
							if(Mod.logExecutionCost) {
								logger.log(Level.INFO, "setupShutUpHook[hook3.2] done, spent " + Mod.executionLogDf.format((System.nanoTime() - Mod.tmpExecutionStartTime) / 1000000000.0) + "s");
							}
							
							Object result = method.invoke(proxy, args);
							return result;
						}
					};
				}
			});
		} catch (NotFoundException e) {
			logger.log(Level.SEVERE, "Failed to intercept 'setupShutUpHook', this probably means some Vampire related NPC's will chat in local");
			throw new RuntimeException(e);
		}
	}


	public static boolean stopNpcMoveHook(Creature npc)
	{
		if(npc.getName().equals(Vampires.headVampireName)
				|| npc.getName().equals(Vampires.kitSalesManName)
				|| npc.getName().equals(Vampires.deVampManName)
				|| npc.getName().equals(Vampires.halfVampMakerName)
			) {
			return true;
		}
		
		return false;
	}


	public static boolean shutUpNpcHook(Creature npc)
	{
		if(npc.getName().equals(Vampires.headVampireName)
				|| npc.getName().equals(Vampires.kitSalesManName)
				|| npc.getName().equals(Vampires.deVampManName)
				|| npc.getName().equals(Vampires.halfVampMakerName)
			) {
			return true;
		}

		return false;
	}
	
	
	static private void spawnHeadVampire()
	{
		try {
			CreatureTemplate template = CreatureTemplateFactory.getInstance().getTemplate(vampireId);

			int xPos = VampZones.getCovenCentre().getX() + 1;
			int yPos = VampZones.getCovenCentre().getY() + 1;
			
			if(Zones.isGoodTileForSpawn(xPos, yPos, VampZones.getCovenLayer() != -1) == false) {
				logger.log(Level.SEVERE, "Could not spawn " + Vampires.headVampireName + ", designated tile is not suitable for spawning: " + xPos + ", " + yPos);
				return;
			}

			Creature newCreature = Creature.doNew(
				template.getTemplateId(), 
				true,									// createPossessions,
				(float)xPos * 4,
				(float)yPos * 4,
				0f, 									// rotation
				VampZones.getCovenLayer(),
				Vampires.headVampireName, 
				(byte)0, 
				(byte)Kingdoms.KINGDOM_FREEDOM, 
				(byte)0,								// Fierce, angry, 99 = champion; com/wurmonline/server/creatures/CreatureStatus.java getTypeString()
				false, 									// reborn
				(byte)3									// age
			);
			
			newCreature.shouldStandStill = true;
			
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Could not spawn " + Vampires.headVampireName, e);
		}
	}


	static private void spawnAtVillageIfNotExists(int templateId, String npcName, byte kingdom, boolean standStill, boolean female)
	{
		Creature[] creatures;
		
		creatures = CreaturesProxy.getCreaturesWithName(npcName);
		if(creatures.length == 0) {
			Village v;

			v = Villages.getCapital(kingdom);
			if(v == null) {
				v = Villages.getFirstPermanentVillageForKingdom(kingdom);
			}

			if(v == null && Villages.getVillages().length > 0) {
				v = Villages.getVillages()[0];
			}

			if(v != null) {
				logger.log(Level.INFO, npcName + " did not exist, so spawning it near a village: " + v.getTokenX() + ", " + v.getTokenY());
				
				int xPos = v.getTokenX() - 20 + Server.rand.nextInt(40);
				int yPos = v.getTokenY() - 20 + Server.rand.nextInt(40);
				
				/*
				// Actually, screw whether suitable... 
				// Admin should move it if it's not in a good place.
				if(Zones.isGoodTileForSpawn(xPos, yPos, VampZones.getCovenLayer() != -1) == false) {
					logger.log(Level.SEVERE, "Could not spawn " + npcName + ", designated tile is not suitable for spawning: " + xPos + ", " + yPos + " -- TODO FOR YOU: spawn manually!");
					return;
				}
				*/

				try {
					Creature newCreature = Creature.doNew(
						templateId, 
						true,									// createPossessions,
						(float)xPos * 4,
						(float)yPos * 4,
						Server.rand.nextFloat(), 				// rotation
						0,										// layer (-1 = cave, 0 = ground)
						npcName, 
						(byte)0, 
						(byte)kingdom,
						(byte)0,								// Fierce, angry, 99 = champion; com/wurmonline/server/creatures/CreatureStatus.java getTypeString()
						false, 									// reborn
						(byte)3									// age
					);
					
					newCreature.setSex(female ? (byte)1 : (byte)0);

					if(standStill) {
						newCreature.shouldStandStill = true;
					}
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Could not spawn " + npcName + " -- TODO FOR YOU: spawn manually!", e);
				}
				
			} else {
				logger.log(Level.SEVERE, "Could not find a village to spawn " + npcName + " TODO FOR YOU: spawn manually!");
			}
		} else {
			logger.log(Level.INFO, npcName + " Exists. Good. There are " + creatures.length + " of them: " + Arrays.toString(creatures));
		}
	}
	

	static void onServerStarted()
	{
		// The NPCs we spawn can be placed, moved, destroyed, whatever. We are just making sure 
		// that they DO exist when the server start up.
		logger.log(Level.INFO, "onServerStarted");

		// The related NPC's we have, are:
		// 		van Helsing -- to de-(half)vamp
		// 		Vampire hunter D -- stake salesman
		// 		Dhampira -- get a bite to become half-vampire
		// 		Orlok -- head vampire

		if(com.wurmonline.server.creatures.Creatures.getInstance().creatureWithTemplateExists(vampireId) == false) {
			logger.log(Level.INFO, Vampires.headVampireName + " did not exist, so spawning him near: " + VampZones.getCovenCentre().getX() + ", " + VampZones.getCovenCentre().getY());
			spawnHeadVampire();
		} else {
			logger.log(Level.INFO, Vampires.headVampireName + " existed. Good.");
		}

		spawnAtVillageIfNotExists(CreatureTemplateIds.NPC_HUMAN_CID, Vampires.kitSalesManName, Kingdoms.KINGDOM_FREEDOM, false, false);
		spawnAtVillageIfNotExists(CreatureTemplateIds.NPC_HUMAN_CID, Vampires.deVampManName, Kingdoms.KINGDOM_FREEDOM, true, false);
		spawnAtVillageIfNotExists(CreatureTemplateIds.NPC_HUMAN_CID, Vampires.halfVampMakerName, Kingdoms.KINGDOM_FREEDOM, false, true);

	}
}
