package com.friya.wurmonline.server.vamps;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import org.gotti.wurmunlimited.modloader.classhooks.HookException;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.classhooks.InvocationHandlerFactory;

import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.skills.Skill;
import com.wurmonline.server.skills.SkillList;
import com.wurmonline.server.skills.SkillSystem;
import com.wurmonline.server.skills.Skills;
import com.wurmonline.server.skills.SkillsProxy;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtPrimitiveType;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;

public class VampSkills
{
	private static Logger logger = Logger.getLogger(VampSkills.class.getName());
	
	public static final int BLOODLUST = 2147483641; 	// 818801;		// was: 2147483641
	public static final int DEXTERITY = 2147483640;		// 818802;		// was: 2147483640
	public static final int PERCEPTION = 2147483639;	
	public static final int ANATOMY = 2147483638;
	public static final int CRIPPLING = 2147483637;
	public static final int DISARMING = 2147483636;
	public static final int AIDING = 2147483635;
	
	private static ArrayList<VampSkillTemplate> tpls = new ArrayList<VampSkillTemplate>();


	static public void onItemTemplatesCreated()
	{
		onItemTemplatesCreated(true);
	}


	static public void onItemTemplatesCreated(boolean enableSkillGainInterception)
	{
		// TODO: Make this use a hook instead of injection
		//		 ACTUALLY: That is very annoying, as we'd need reflection to change a few methods to public to be able to do that...
		
		try {
        	ClassPool classPool = HookManager.getInstance().getClassPool();
        	CtClass theClass;
        	CtMethod theMethod;
        	String str;

        	// Add hook for addSkillTemplatesHook
			theClass = classPool.get("com.wurmonline.server.skills.SkillSystem");
			theMethod = theClass.getDeclaredMethod("addSkillTemplate");
			
			// Iterates over the skills we define and sees to it that they get added to skill-templates.
			// 10095 here is just an arbitrary skill so that we only do this once.
	        str = "{"
	        	//+ "		System.out.println(\"addSkillTemplate() called for skill \" + $1.getName() );"
	        	+ "		if($1.getNumber() == 10095) {"
	        	+ "			com.friya.wurmonline.server.vamps.VampSkillTemplate[] tpls = com.friya.wurmonline.server.vamps.VampSkills.addSkillTemplateHook();"
	        	+ "			for(int i = 0; i < tpls.length; i++) {"
	        	+ "				com.wurmonline.server.skills.SkillSystem.addSkillTemplate(new com.wurmonline.server.skills.SkillTemplate("
	        	+ "					tpls[i].number,"
	        	+ "					tpls[i].name,"
	        	+ "					tpls[i].difficulty,"
	        	+ "					tpls[i].dependencies,"
	        	+ "					tpls[i].decayTime,"
	        	+ "					tpls[i].type,"
	        	+ "					tpls[i].fightingSkill,"
	        	+ "					tpls[i].ignoreEnemy"
	        	+ "				));"
	        	+ "			}"
	        	+ "		}"
	        	+ "}";
	        theMethod.insertAfter(str);
	        
		} catch (NotFoundException | CannotCompileException e) {
            Mod.appendToFile((Exception)e);
            throw new HookException((Throwable)e);
		}
		
		if(enableSkillGainInterception) {
			interceptSkillGains();
		}
		
        logger.log(Level.INFO, "preInit completed");
	}
	
	private static void interceptSkillGains()
	{
		// TODO: Improve. This is a pretty slow way of going about this, but well, little code :)
		
		String descriptor;
		try {
			// private double checkAdvance(double check, @Nullable Item item, double bonus, boolean dryRun, float times, boolean useNewSystem, double skillDivider)
			descriptor = Descriptor.ofMethod(CtClass.doubleType, new CtClass[] {
				CtPrimitiveType.doubleType,
				HookManager.getInstance().getClassPool().get("com.wurmonline.server.items.Item"),
				CtPrimitiveType.doubleType,
				CtPrimitiveType.booleanType,
				CtPrimitiveType.floatType,
				CtPrimitiveType.booleanType,
				CtPrimitiveType.doubleType
			});
			HookManager.getInstance().registerHook("com.wurmonline.server.skills.Skill", "checkAdvance", descriptor, new InvocationHandlerFactory()
			{
				@Override
				public InvocationHandler createInvocationHandler() {
					return new InvocationHandler() {

						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							if(Mod.logExecutionCost) {
								Mod.tmpExecutionStartTime = System.nanoTime();
							}

							Skill skill = (Skill)proxy;

							Skills skills = SkillsProxy.getParent(skill);

					        for(VampSkillTemplate t : tpls) {
								if(IntStream.of(t.skillUpOn).anyMatch(x -> x == skill.getNumber())) {
									//logger.log(Level.INFO, "trigger gains of " + t.name + " via " + skill.getName());
							        skills.getSkillOrLearn(t.number).skillCheck(
							        		(double)args[0],
							        		(Item)args[1],
							        		(double)args[2],
							        		(boolean)args[3],
							        		(float)args[4],
							        		(boolean)args[5],
							        		(double)args[6]
							        );
								}
							}

							if(Mod.logExecutionCost) {
								logger.log(Level.INFO, "interceptSkillGains[hook] done, spent " + Mod.executionLogDf.format((System.nanoTime() - Mod.tmpExecutionStartTime) / 1000000000.0) + "s");
							}

					        Object result = method.invoke(proxy, args);
							return result;
						}
					};
				}
			});
		} catch (NotFoundException e) {
			logger.log(Level.SEVERE, "Failed!", e);
			throw new RuntimeException("Failed to intercept checkAdvance()");
		}
	}


	/**
	 * This gets called when server is started (not launcher).
	 * 
	 * @return
	 */
	static public VampSkillTemplate[] addSkillTemplateHook()
	{
		// SkillList:
		//151     public static final short TYPE_BASIC = 0;			// BStr, BC, BS, ML, MS, SD, Sstr (characteristics)
		//152     public static final short TYPE_MEMORY = 1;		// Mind, Body, Soul (characteristic groups -- the parent)
		//153     public static final short TYPE_ENHANCING = 2;		// Swords (etc), hammers, archery, tailoring, cooking, smithing, AS, WS, misc, shields, alch, nature, toys, healing, religion
		//154     public static final short TYPE_NORMAL = 4;		// fighting, farming, ... everything else
		
		/* SkillSystem:
		 28     public static final long SKILLGAIN_BASIC = 300000;
		 29     public static final long SKILLGAIN_CHARACTERISTIC = 200000;
		 30     public static final long SKILLGAIN_CHARACTERISTIC_BC = 150000;
		 31     public static final long SKILLGAIN_GROUP = 20000;
		 32     public static final long SKILLGAIN_FIGHTING = 4000;
		 33     public static final long SKILLGAIN_TOOL = 7000;
		 34     public static final long SKILLGAIN_NORMAL = 4000;
		 35     public static final long SKILLGAIN_FAST = 3000;
		 36     public static final long SKILLGAIN_RARE = 2000;
		 37     public static final long SKILLGAIN_FIGHTING_GROUP = 10000;
		*/
		
		// True/false on dependencies: if first dep is not a characteristic, it becomes its parent?

		logger.log(Level.INFO, "addSkillTemplateHook() called, Vampire related skills will be added");

		// Skill:	BLOODLUST
		// What:	"Hunger" for Vampires
		// How to:	Is not skilled up, works like favor in that it regens over time
		// Where:	We want this listed under soul.
		tpls.add(new VampSkillTemplate(
			VampSkills.BLOODLUST,						// skill number
			"Bloodlust",								// skill name
			SkillSystem.SKILLGAIN_CHARACTERISTIC,		// difficulty
			new int[] {									// dependencies
				SkillList.SOUL
			},
			1209600000,									// decay time, 1209600000 = 14 days -- no idea what this is used for
			(short)SkillList.TYPE_BASIC,				// type
			false,										// is fighting skill
			true,										// ignore enemy
			new int[]{									// how it's skilled up using other skills (if any)
			}
		));

		// Skill:	DEXTERITY
		// What:	The main skill for staking and dodging stakes
		// How to:	Quite a broad range of ways to skill up, see below
		// Where:	We want this listed at first level (like e.g. paving)
		tpls.add(new VampSkillTemplate(
			VampSkills.DEXTERITY,						// skill number
			"Dexterity",								// skill name
			SkillSystem.SKILLGAIN_CHARACTERISTIC_BC / 2,	// difficulty
			new int[] {
			},
			1209600000,									// decay time, 1209600000 = 14 days -- no idea what this is used for
			(short)SkillList.TYPE_NORMAL,				// type
			false,										// is fighting skill
			true,										// ignore enemy
			new int[]{									// how it's skilled up using other skills (if any)
				SkillList.ROPEMAKING,
				SkillList.CLOTHTAILORING,
				SkillList.SMITHING_ARMOUR_CHAIN,
				SkillList.STAFF,
				SkillList.SHIELD_SMALL_WOOD,
				SkillList.SHIELD_SMALL_METAL,
				SkillList.WEAPONLESS_FIGHTING,
				SkillList.LOCKPICKING
			}
		));

		// Skill:	PERCEPTION
		// What:	Used to get better locates
		// How to:	tracking, trapping, locate soul, channeling, ...
		// Where:	We want this listed at first level (like e.g. paving)
		tpls.add(new VampSkillTemplate(
			VampSkills.PERCEPTION,						// skill number
			"Perception",								// skill name
			SkillSystem.SKILLGAIN_CHARACTERISTIC_BC / 2,	// difficulty
			new int[] {									// dependencies
			},
			1209600000,									// decay time, 1209600000 = 14 days -- no idea what this is used for
			(short)SkillList.TYPE_NORMAL,				// type
			false,										// is fighting skill
			true,										// ignore enemy
			new int[] {									// skills that should trigger gains in this one
				SkillList.TRACKING,
				SkillList.TRAPS,
				SkillList.CHANNELING
			}
		));

		// Skill:	ANATOMY
		// What:	Gives better effect when feeding/devouring off of corpses
		// How to:	see below and actually devouring corpses
		// Where:	We want this listed under nature
		tpls.add(new VampSkillTemplate(
			VampSkills.ANATOMY,							// skill number
			"Anatomy",									// skill name
			SkillSystem.SKILLGAIN_CHARACTERISTIC_BC / 2,	// difficulty
			new int[] {									// dependencies
				SkillList.GROUP_NATURE
			},
			1209600000,									// decay time, 1209600000 = 14 days -- no idea what this is used for
			(short)SkillList.TYPE_NORMAL,				// type
			false,										// is fighting skill
			true,										// ignore enemy
			new int[] {									// skills that should trigger gains in this one
				SkillList.BUTCHERING,
				SkillList.BREEDING,
				SkillList.MILKING,
				SkillList.TAMEANIMAL
			}
		));

		// Skill:	CRIPPLING
		// What:	Gives better effect when crippling livings
		// How to:	see below and actually doing
		// Where:	where do we want it listed?
		tpls.add(new VampSkillTemplate(
			VampSkills.CRIPPLING,						// skill number
			"Crippling",								// skill name
			SkillSystem.SKILLGAIN_CHARACTERISTIC_BC / 2,	// difficulty
			new int[] {									// dependencies
				SkillList.GROUP_HEALING
			},
			1209600000,									// decay time, 1209600000 = 14 days -- no idea what this is used for
			(short)SkillList.TYPE_NORMAL,				// type
			false,										// is fighting skill
			true,										// ignore enemy
			new int[] {									// skills that should trigger gains in this one
				SkillList.ALCHEMY_NATURAL
			}
		));

		// Skill:	DISARMING
		// What:	Used to better disarm hunters
		// How to:	see below
		// Where:	where do we want it listed?
		tpls.add(new VampSkillTemplate(
			VampSkills.DISARMING,						// skill number
			"Disarming",								// skill name
			SkillSystem.SKILLGAIN_CHARACTERISTIC_BC / 2,	// difficulty
			new int[] {									// dependencies
			},
			1209600000,									// decay time, 1209600000 = 14 days -- no idea what this is used for
			(short)SkillList.TYPE_NORMAL,				// type
			false,										// is fighting skill
			true,										// ignore enemy
			new int[] {									// skills that should trigger gains in this one
				SkillList.FIGHT_DEFENSIVESTYLE
			}
		));

		// Skill:	AIDING
		// What:	Used to better aid people
		// How to:	see below
		// Where:	where do we want it listed?
		tpls.add(new VampSkillTemplate(
			VampSkills.AIDING,							// skill number
			"Aiding",									// skill name
			SkillSystem.SKILLGAIN_CHARACTERISTIC_BC / 2,				// difficulty
			new int[] {									// dependencies
			},
			1209600000,									// decay time, 1209600000 = 14 days -- no idea what this is used for
			(short)SkillList.TYPE_NORMAL,				// type
			false,										// is fighting skill
			true,										// ignore enemy
			new int[] {									// skills that should trigger gains in this one
				SkillList.FIRSTAID
			}
		));

		return tpls.toArray(new VampSkillTemplate[0]);
	}

	static public void learnSkills(Skills s)
	{
		for(VampSkillTemplate t : tpls) {
			s.getSkillOrLearn(t.number);
		}
	}

	static public void onPlayerLogin(Player p)
	{
		learnSkills(p.getSkills());
	}
}
