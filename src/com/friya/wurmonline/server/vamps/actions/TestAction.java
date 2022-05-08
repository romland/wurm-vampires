package com.friya.wurmonline.server.vamps.actions;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modloader.classhooks.HookException;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.classhooks.InvocationHandlerFactory;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import com.friya.wurmonline.server.vamps.ActionSkillGain;
import com.wurmonline.server.Servers;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.skills.SimSkill;
import com.wurmonline.server.skills.SimSkills;
import com.wurmonline.server.skills.Skill;
import com.wurmonline.server.skills.SkillList;
import com.wurmonline.server.skills.SkillSystem;
import com.wurmonline.server.skills.Skills;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtPrimitiveType;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.text.DecimalFormat;

public class TestAction implements ModAction 
{
	private static Logger logger = Logger.getLogger(TestAction.class.getName());

	private final short actionId;
	private final ActionEntry actionEntry;

	// THIS IS ONLY HERE FOR DEBUGGING!
	// we want to see what arguments skill-related methods get...
	static public void preInit()
	{
/*
		try {
	        CtClass theClass;
	        ClassPool classPool = HookManager.getInstance().getClassPool();

			theClass = classPool.get("com.wurmonline.server.skills.Skill");
			CtMethod theMethod = theClass.getDeclaredMethods("doSkillGainNew")[0];
	        String str = "{"
	        		+ "		com.friya.wurmonline.server.vamps.debugDoSkillGainNew($$);"
	        		+ "}";
	        theMethod.insertBefore(str);

		} catch (NotFoundException | CannotCompileException e) {
            Mod.appendToFile((Exception)e);
            throw new HookException((Throwable)e);
		}
*/
		
        logger.log(Level.INFO, "preInit completed");
	}
	
	static public void testHook()
	{
		logger.log(Level.INFO, "testHook() called!");
	}
	
	static public void debugDoSkillGainNew(double check, double power, double learnMod, float times, double skillDivider)
	{
		logger.log(Level.INFO, "debugDoSkillGainNew() check: " + check);
		logger.log(Level.INFO, "debugDoSkillGainNew() power: " + power);
		logger.log(Level.INFO, "debugDoSkillGainNew() learnMod: " + learnMod);
		logger.log(Level.INFO, "debugDoSkillGainNew() times: " + times);
		logger.log(Level.INFO, "debugDoSkillGainNew() skillDivider: " + skillDivider);
	}
	
	static public void debugAlterSkill(double advanceMultiplicator, boolean decay, float times, boolean useNewSystem, double skillDivider)
	{
		DecimalFormat df = new DecimalFormat("#.########");

		logger.log(Level.INFO, "alterSkill() advanceMultiplicator: " + df.format(advanceMultiplicator));
		logger.log(Level.INFO, "alterSkill() decay: " + decay);
		logger.log(Level.INFO, "alterSkill() times: " + times);
		logger.log(Level.INFO, "alterSkill() useNewSystem: " + useNewSystem);
		logger.log(Level.INFO, "alterSkill() skillDivider: " + skillDivider);
	}
	

	public TestAction() {
		logger.log(Level.INFO, "SimulateAction()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"Simulate", 
			"simulating", 
			new int[] { 6 /* ACTION_TYPE_NOMOVE */ }	// 6 /* ACTION_TYPE_NOMOVE */, 48 /* ACTION_TYPE_ENEMY_ALWAYS */, 36 /* ACTION_TYPE_ALWAYS_USE_ACTIVE_ITEM */
		);
		ModActions.registerAction(actionEntry);
	}


	@Override
	public BehaviourProvider getBehaviourProvider()
	{
		return new BehaviourProvider() {
			// Menu with activated object
			@Override
			public List<ActionEntry> getBehavioursFor(Creature performer, Item source, Item object)
			{
				return this.getBehavioursFor(performer, object);
			}

			// Menu without activated object
			@Override
			public List<ActionEntry> getBehavioursFor(Creature performer, Item object)
			{
				if(performer instanceof Player && performer.getPower() > 1) {
					return Arrays.asList(actionEntry);
				}
				return null;
			}
		};
	}

	
	@Override
	public ActionPerformer getActionPerformer()
	{
		return new ActionPerformer() {

			@Override
			public short getActionId() {
				return actionId;
			}

			// Without activated object
			@Override
			public boolean action(Action act, Creature performer, Item target, short action, float counter)
			{
				return simulate(performer, target, counter);
			}
			
			@Override
			public boolean action(Action act, Creature performer, Item source, Item target, short action, float counter)
			{
				return this.action(act, performer, target, action, counter);
			}
		}; // ActionPerformer
	}


	@SuppressWarnings("unused")
	private boolean simulate(Creature performer, Item target, float counter)
	{
		if(true) {
			performer.getCommunicator().sendNormalServerMessage("Test!");
			return true;
		}
		
		
/*
		if(true) {
			Creature crippleTarget = performer;	// just for testing
			
			DoubleValueModifier slowMod = new DoubleValueModifier(7, -0.25);
			crippleTarget.getMovementScheme().addModifier(slowMod);
			if (crippleTarget.isPlayer()) {
				crippleTarget.getCommunicator().sendNormalServerMessage("You are slowed down by " + performer.getName() + "'s cripple effect.");
				EventDispatcher.add(new RemoveModifierEvent(100, Unit.SECONDS, crippleTarget, slowMod, SpellEffectsEnum.WOUNDMOVE));
				crippleTarget.getCommunicator().sendAddSpellEffect(SpellEffectsEnum.WOUNDMOVE, 100 * 1000, 100.0f);		// SpellEffectsEnum effect, int duration, float power
			}
			return true;
		}
*/		
		
/*
		if(true) {
			try {
				logger.log(Level.INFO, "Supposedly neck: " + ((Player)performer).getEquippedItem((byte)36));
			} catch (NoSuchItemException | NoSpaceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		}
		
		if(true) {
			performer.getCommunicator().sendNormalServerMessage("NOT Running simulation; testing rift stuff!");
			
			try {
				TilePos tp = performer.getTilePos();
				Zone z;
				z = Zones.getZone(tp, true);
				z.setHasRift(true);

			} catch (NoSuchZoneException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return true;
		}
*/		
		performer.getCommunicator().sendNormalServerMessage("Running simulation");

		// Casting in game: [10:47:15] Channeling increased by 0.0011 to 90.5011
		// From SimSkill: 									   0.0003509 approx actions per point: 2849.5529542
		// from real skill: 								   0.00110438378404
		//
		//[11:50:16 AM] INFO com.friya.wurmonline.server.vamps.SimulateAction:   simSkill now at: 90.50035093223957
		//[11:50:16 AM] INFO com.friya.wurmonline.server.vamps.SimulateAction: real skill now at: 90.50110438378404		

		// from 10.5
		//[04:18:28 PM] INFO com.friya.wurmonline.server.vamps.SimulateAction:   simSkill now at: 10.614683637058748
		//[04:18:28 PM] INFO com.friya.wurmonline.server.vamps.SimulateAction: real skill now at: 10.860909414354808

//		[05:23:00 PM] INFO com.friya.wurmonline.server.vamps.SimulateAction:   simSkill now at: 10.844050911176247
//		[05:23:00 PM] INFO com.friya.wurmonline.server.vamps.SimulateAction: real skill now at: 11.5

		try {
			//Skill s = performer.getSkills().getSkill(Skills.AGILITY);

			Skills skills = performer.getSkills();
			Skill s = performer.getSkills().getSkill(SkillList.CHANNELING);

			//[12:06:28 PM] INFO com.friya.wurmonline.server.vamps.SimulateAction: isTemplate: true    false
			SimSkills simSkills = new SimSkills(null);		// passing in a name here flags it as template (whatever side-effects that might have)
			simSkills.priest = true;
			
			//Skill simSkill = (Skill)(new SimSkill(-10, (Skills)simSkills, SkillList.CHANNELING, 1.0, 0.0, 0));
			//Skill simSkill = (Skill)(new SimSkill(-10, (Skills)simSkills, SkillList.CHANNELING, 1.0, 99.99990000242077f, 1479035543351L));
			Skill simSkill = (Skill)(new SimSkill(-152, (Skills)simSkills, SkillList.CHANNELING, 1.0, 99.99990000242077f, 1479035543351L));
//			simSkill.touch();
			

			for(int p : simSkill.getUniqueDependencies()) {
				// just a test to see if this influences skillgain
				// IT DID NOT, but it will throw an exception if it's not here
				simSkills.learn(p, 30.0f);
			}

// also check: initialize() on the simSkill

			// ---
			simSkill.setKnowledge(10.5f, false);
			s.setKnowledge(10.5f, false);
			// ---
			
			logger.log(Level.INFO, "counter: " + counter);
			
			// Comparison of Skills object
			logger.log(Level.INFO, "nutrition: " + performer.getStatus().getNutritionlevel() );
			logger.log(Level.INFO, "id: " + simSkills.getId() + "    " + skills.getId() );
			logger.log(Level.INFO, "getTemplateName: " + simSkills.getTemplateName() + "    " + skills.getTemplateName() );
			logger.log(Level.INFO, "hashCode: " + simSkills.hashCode() + "    " + skills.hashCode() );
			logger.log(Level.INFO, "toString" + simSkills.toString() + "    " + skills.toString() );
			logger.log(Level.INFO, "minChallengeValue: " + SimSkills.minChallengeValue + "    " + Skills.minChallengeValue );
			logger.log(Level.INFO, "hasSkillGain: " + simSkills.hasSkillGain + "    " + skills.hasSkillGain );
			logger.log(Level.INFO, "paying: " + simSkills.paying + "    " + skills.paying );
			logger.log(Level.INFO, "priest: " + simSkills.priest + "    " + skills.priest );
			logger.log(Level.INFO, "isTemplate: " + simSkills.isTemplate() + "    " + skills.isTemplate() );

			// Comparison of one skill
			logger.log(Level.INFO, "affinity: " + simSkill.affinity + "    " + s.affinity);
			logger.log(Level.INFO, "id: " + simSkill.id + "    " + s.id);
			logger.log(Level.INFO, "knowledge: " + simSkill.getKnowledge() + "    " + s.getKnowledge());
			logger.log(Level.INFO, "lastUsed: " + simSkill.lastUsed + "    " + s.lastUsed);
			logger.log(Level.INFO, "minimum: " + simSkill.minimum + "    " + s.minimum);
			logger.log(Level.INFO, "getDecayTime: " + simSkill.getDecayTime() + "    " + s.getDecayTime());
			logger.log(Level.INFO, "getDifficulty: " + simSkill.getDifficulty(true) + "    " + s.getDifficulty(true));
			logger.log(Level.INFO, "getModifierValues: " + simSkill.getModifierValues() + "    " + s.getModifierValues());
			logger.log(Level.INFO, "getName: " + simSkill.getName() + "    " + s.getName());
			logger.log(Level.INFO, "getNumber: " + simSkill.getNumber() + "    " + s.getNumber());
			logger.log(Level.INFO, "getParentBonus: " + simSkill.getParentBonus() + "    " + s.getParentBonus());
			logger.log(Level.INFO, "getRealKnowledge: " + simSkill.getRealKnowledge() + "    " + s.getRealKnowledge());
			logger.log(Level.INFO, "getType: " + simSkill.getType() + "    " + s.getType());
			logger.log(Level.INFO, "getDependencies: " + Arrays.toString( simSkill.getDependencies() ) + "    " + Arrays.toString(s.getDependencies()));
			logger.log(Level.INFO, "getUniqueDependencies: " + Arrays.toString(simSkill.getUniqueDependencies()) + "    " + Arrays.toString(s.getUniqueDependencies()));
			logger.log(Level.INFO, "isDirty: " + simSkill.isDirty() + "    " + s.isDirty());
			logger.log(Level.INFO, "isFightingSkill: " + simSkill.isFightingSkill() + "    " + s.isFightingSkill());

			logger.log(Level.INFO, "############ calling skillCheck on simSkill");
			// does this: return this.skillCheck(check, bonus, test, 10.0f, true, 2.0);
			//simSkill.skillCheck(50.0, 0.0, false, counter);
			simSkill.skillCheck(20.0, 0.0, false, counter);

			logger.log(Level.INFO, "############ calling skillCheck on real skill");
			
			// does this: return this.skillCheck(check, bonus, test, 10.0f, true, 2.0);
			//s.skillCheck(50.0, 0.0, false, counter);
			s.skillCheck(20.0, 0.0, false, counter);

			logger.log(Level.INFO, "  simSkill now at: " + simSkill.getKnowledge());
			logger.log(Level.INFO, "real skill now at: " + s.getKnowledge());
			logger.log(Level.INFO, "############ done calling skillCheck");
			
			//TempSkill s = new TempSkill(-10, ss, SkillList.CHANNELING, 1.0, 0.0, 0);
			

			ActionSkillGain asg = new ActionSkillGain(s.getNumber(), s.getName());

            //ReflectionUtil.callPrivateMethod(Skill.class, ReflectionUtil.getMethod(Skill.class, "checkAdvance"), s);

			//Item item = ItemFactory.createItem(ItemList.axeHuge, 1.0f, 0, 0, null);

			DecimalFormat df = new DecimalFormat("#.#######");
			double before;
			double diff;
			boolean gained = false;
			
			// Zenath:
			// [08:15:10] Channeling increased by 0.0012 to 90.5012
			// Test-server:
			// Channeling at skill 90.5 gain: 0.0011044 
			
			for(int n = 1; n <= 100; n++) {
				// skillCheck(double check, double bonus, boolean test, float times)
				// is check "difficulty"?

				if(n == 100) {
					s.setKnowledge(99.99, false);
				} else {
					s.setKnowledge(n + 0.5, false);
				}
				gained = false;
				while(!gained) {
					before = s.getKnowledge();
					
					// NOTE: THIS LINE IS THE PLACE THAT NEEDS TWEAKING IF DATA DOES NOT MATCH WITH REALITY WHEN IT COMES TO SKILL GAIN
					s.skillCheck(50.0, 0.0, false, counter);
					
					diff = s.getKnowledge() - before;
					if(diff > 0.0) {
						asg.add(n, diff);
						logger.log(Level.INFO, s.getName() + " at skill " + (n + 0.5) + " gain: " + df.format(diff) + " approx actions per point: " + df.format(1.0 / diff) );
						gained = true;
					}
				}
			}
/*
			// add the "almost 100" case
			s.setKnowledge(99.9999, false);
			sgs.add
*/			
			
			performer.getCommunicator().sendNormalServerMessage(s.getName() + ": 50.0 - " + s.getKnowledge());

			logger.log(Level.INFO, s.getName() + " actions needed for 1 to 10: " + asg.getRawGainedActionCount(1.0, 9.0));
			logger.log(Level.INFO, s.getName() + " actions needed for 1 to 20: " + asg.getRawGainedActionCount(1.0, 19.0));
			logger.log(Level.INFO, s.getName() + " actions needed for 1 to 30: " + asg.getRawGainedActionCount(1.0, 29.0));
			logger.log(Level.INFO, s.getName() + " actions needed for 1 to 40: " + asg.getRawGainedActionCount(1.0, 39.0));
			logger.log(Level.INFO, s.getName() + " actions needed for 1 to 50: " + asg.getRawGainedActionCount(1.0, 49.0));
			logger.log(Level.INFO, s.getName() + " actions needed for 1 to 60: " + asg.getRawGainedActionCount(1.0, 59.0));
			logger.log(Level.INFO, s.getName() + " actions needed for 1 to 70: " + asg.getRawGainedActionCount(1.0, 69.0));
			logger.log(Level.INFO, s.getName() + " actions needed for 1 to 80: " + asg.getRawGainedActionCount(1.0, 79.0));
			logger.log(Level.INFO, s.getName() + " actions needed for 1 to 90: " + asg.getRawGainedActionCount(1.0, 89.0));
			logger.log(Level.INFO, s.getName() + " actions needed for 1 to 100: " + asg.getRawGainedActionCount(1.0, 99.0));
			
			logger.log(Level.INFO, s.getName() + " actions needed for 90 to 90.1: " + asg.getRawGainedActionCount(90.0, 0.1));
			logger.log(Level.INFO, s.getName() + " actions needed for 90 to 90.2: " + asg.getRawGainedActionCount(90.0, 0.2));
			logger.log(Level.INFO, s.getName() + " actions needed for 90 to 90.3: " + asg.getRawGainedActionCount(90.0, 0.3));
			logger.log(Level.INFO, s.getName() + " actions needed for 90 to 90.4: " + asg.getRawGainedActionCount(90.0, 0.4));
			logger.log(Level.INFO, s.getName() + " actions needed for 90 to 90.5: " + asg.getRawGainedActionCount(90.0, 0.5));
			logger.log(Level.INFO, s.getName() + " actions needed for 90 to 90.6: " + asg.getRawGainedActionCount(90.0, 0.6));
			logger.log(Level.INFO, s.getName() + " actions needed for 90 to 90.7: " + asg.getRawGainedActionCount(90.0, 0.7));
			logger.log(Level.INFO, s.getName() + " actions needed for 90 to 90.8: " + asg.getRawGainedActionCount(90.0, 0.8));
			logger.log(Level.INFO, s.getName() + " actions needed for 90 to 90.9: " + asg.getRawGainedActionCount(90.0, 0.9));
			logger.log(Level.INFO, s.getName() + " actions needed for 90 to 91: " + asg.getRawGainedActionCount(90.0, 1.0));

			logger.log(Level.INFO, s.getName() + " actions needed for 90 to 92.123123: " + asg.getRawGainedActionCount(90.0, 2.123123));
			logger.log(Level.INFO, s.getName() + " actions needed for 90 to 95.8761237: " + asg.getRawGainedActionCount(90.0, 5.8761237));

			logger.log(Level.INFO, "------------------------------------------------------");
			logger.log(Level.INFO, s.getName() + " skill gain for 1200 actions at skill 1: " + asg.getRawSkillGainForActionCount(1, 1200) );

			logger.log(Level.INFO, s.getName() + " skill gain for 100 actions at skill 90: " + asg.getRawSkillGainForActionCount(90, 100) );
			logger.log(Level.INFO, s.getName() + " skill gain for 200 actions at skill 90: " + asg.getRawSkillGainForActionCount(90, 200) );
			logger.log(Level.INFO, s.getName() + " skill gain for 300 actions at skill 90: " + asg.getRawSkillGainForActionCount(90, 300) );
			logger.log(Level.INFO, s.getName() + " skill gain for 400 actions at skill 90: " + asg.getRawSkillGainForActionCount(90, 400) );
			logger.log(Level.INFO, s.getName() + " skill gain for 500 actions at skill 90: " + asg.getRawSkillGainForActionCount(90, 500) );
			logger.log(Level.INFO, s.getName() + " skill gain for 600 actions at skill 90: " + asg.getRawSkillGainForActionCount(90, 600) );
			logger.log(Level.INFO, s.getName() + " skill gain for 700 actions at skill 90: " + asg.getRawSkillGainForActionCount(90, 700) );
			logger.log(Level.INFO, s.getName() + " skill gain for 800 actions at skill 90: " + asg.getRawSkillGainForActionCount(90, 800) );
			logger.log(Level.INFO, s.getName() + " skill gain for 900 actions at skill 90: " + asg.getRawSkillGainForActionCount(90, 900) );
			logger.log(Level.INFO, s.getName() + " skill gain for 1000 actions at skill 90: " + asg.getRawSkillGainForActionCount(90, 1000) );
			logger.log(Level.INFO, s.getName() + " skill gain for 2500 actions at skill 90: " + asg.getRawSkillGainForActionCount(90, 2500) );

			logger.log(Level.INFO, "------------------------------------------------------");
			logger.log(Level.INFO, s.getName() + " skill loss for 500 actions at skill 90: " + asg.getRawSkillLossForActionCount(90, 500) );
			logger.log(Level.INFO, s.getName() + " skill loss for 600 actions at skill 90: " + asg.getRawSkillLossForActionCount(90, 600) );
			logger.log(Level.INFO, s.getName() + " skill loss for 700 actions at skill 90: " + asg.getRawSkillLossForActionCount(90, 700) );
			logger.log(Level.INFO, s.getName() + " skill loss for 800 actions at skill 90: " + asg.getRawSkillLossForActionCount(90, 800) );
			logger.log(Level.INFO, s.getName() + " skill loss for 800 actions at skill 90: " + asg.getRawSkillLossForActionCount(90, 900) );
			logger.log(Level.INFO, s.getName() + " skill loss for 2500 actions at skill 90: " + asg.getRawSkillLossForActionCount(90, 2500) );

			logger.log(Level.INFO, s.getName() + " skill loss for 500 actions at skill 95: " + asg.getRawSkillLossForActionCount(95, 500) );
			logger.log(Level.INFO, s.getName() + " skill loss for 2000 actions at skill 95: " + asg.getRawSkillLossForActionCount(95, 2000) );
			logger.log(Level.INFO, s.getName() + " skill loss for 4000 actions at skill 95: " + asg.getRawSkillLossForActionCount(95, 4000) );
			logger.log(Level.INFO, s.getName() + " skill loss for 8000 actions at skill 95: " + asg.getRawSkillLossForActionCount(95, 8000) );
			logger.log(Level.INFO, s.getName() + " skill loss for 16000 actions at skill 95: " + asg.getRawSkillLossForActionCount(95, 16000) );
			logger.log(Level.INFO, s.getName() + " skill loss for 300000 actions at skill 95: " + asg.getRawSkillLossForActionCount(95, 300000) );
			
			logger.log(Level.INFO, s.getName() + " skill loss for 500 actions at skill 99.99: " + asg.getRawSkillLossForActionCount(99.99, 500) );
			logger.log(Level.INFO, s.getName() + " skill loss for 600 actions at skill 99.99: " + asg.getRawSkillLossForActionCount(99.99, 600) );
			logger.log(Level.INFO, s.getName() + " skill loss for 700 actions at skill 99.99: " + asg.getRawSkillLossForActionCount(99.99, 700) );
			logger.log(Level.INFO, s.getName() + " skill loss for 5000 actions at skill 99.99: " + asg.getRawSkillLossForActionCount(99.99, 5000) );
			logger.log(Level.INFO, s.getName() + " skill loss for 12000 actions at skill 99.99: " + asg.getRawSkillLossForActionCount(99.99, 12000) );
			logger.log(Level.INFO, s.getName() + " skill loss for 1376194 actions at skill 99.99: " + asg.getRawSkillLossForActionCount(99.99, 1376194) );

			logger.log(Level.INFO, s.getName() + " skill loss for 11185 actions at skill 100: " + asg.getRawSkillLossForActionCount(100, 11185) );
			logger.log(Level.INFO, s.getName() + " skill loss for 21185 actions at skill 100: " + asg.getRawSkillLossForActionCount(100, 21185) );
			logger.log(Level.INFO, s.getName() + " skill loss for 31185 actions at skill 100: " + asg.getRawSkillLossForActionCount(100, 31185) );
			logger.log(Level.INFO, s.getName() + " skill loss for 41185 actions at skill 100: " + asg.getRawSkillLossForActionCount(100, 41185) );
			logger.log(Level.INFO, s.getName() + " skill loss for 51185 actions at skill 100: " + asg.getRawSkillLossForActionCount(100, 51185) );
			logger.log(Level.INFO, s.getName() + " skill loss for 1376194 actions at skill 100: " + asg.getRawSkillLossForActionCount(100, 1376194) );
			logger.log(Level.INFO, s.getName() + " skill loss for 2376194 actions at skill 100: " + asg.getRawSkillLossForActionCount(100, 2376194) );
			
			logger.log(Level.INFO, "------------------------------------------------------");
			// These will log themselves for now...
			asg.getModifiedLostActionCount(10, 1000, 0.25f);
			asg.getModifiedLostActionCount(20, 1000, 0.25f);
			asg.getModifiedLostActionCount(30, 1000, 0.25f);
			asg.getModifiedLostActionCount(40, 1000, 0.25f);
			asg.getModifiedLostActionCount(50, 1000, 0.25f);
			asg.getModifiedLostActionCount(60, 1000, 0.25f);
			asg.getModifiedLostActionCount(70, 1000, 0.25f);
			asg.getModifiedLostActionCount(80, 1000, 0.25f);
			asg.getModifiedLostActionCount(90, 1000, 0.25f);
			asg.getModifiedLostActionCount(100, 1000, 0.25f);


		} catch(Exception /*NoSuchSkillException | InvocationTargetException | NoSuchMethodException | IllegalAccessException */ e) {
			logger.log(Level.SEVERE, "No skill...", e);
		}
		
		
		return true;
	}

	void test()
	{
//		Mod.thaw("com.wurmonline.server.skills.Skill");
		
		try {
			String descriptor;

			/*
			logger.log(Level.INFO, "test: " + descriptor);
			logger.log(Level.INFO, "foo: " + byte.class);
			logger.log(Level.INFO, "fooo: " + CtClass.intType.toString());

			CtClass c = HookManager.getInstance().getClassPool().get("com.wurmonline.server.skills.Skill");
			CtMethod m = c.getDeclaredMethod("doSkillGainNew");
			//logger.log(Level.INFO, "doSkillGainNew signature: " +  m.getGenericSignature() );
			//logger.log(Level.INFO, "meep: ", ((CtPrimitiveType)c).getGetMethodDescriptor() );
			
			logger.log(Level.INFO, "blib: " + c.getGenericSignature());
			logger.log(Level.INFO, "blah: " + c.toClass());
			logger.log(Level.INFO, "blob: " + getMethodDescriptor(m));
*/			

/*
			descriptor = Descriptor.ofMethod(CtClass.voidType, new CtClass[] {
				//HookManager.getInstance().getClassPool().get("com.wurmonline.server.players.Player")
				CtPrimitiveType.doubleType,
				CtPrimitiveType.doubleType,
				CtPrimitiveType.doubleType,
				CtPrimitiveType.floatType,
				CtPrimitiveType.doubleType,
			});
			HookManager.getInstance().registerHook("com.wurmonline.server.skills.Skill", "doSkillGainNew", descriptor, new InvocationHandlerFactory() {
				@Override
				public InvocationHandler createInvocationHandler() {
					return new InvocationHandler() {
						
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							logger.log(Level.INFO, "name: " + ((Skill)proxy).getName());
							logger.log(Level.INFO, "type: " + ((Skill)proxy).getType());
							
							Object result = method.invoke(proxy, args);
							//debugDoSkillGainNew((Player)args[0]);
							SimulateAction.debugDoSkillGainNew((double)args[0], (double)args[1], (double)args[2], (float)args[3], (double)args[4]);
							return result;
						}
					};
				}
			});
*/

			descriptor = Descriptor.ofMethod(CtClass.doubleType, new CtClass[] {
				//HookManager.getInstance().getClassPool().get("com.wurmonline.server.players.Player")
				CtPrimitiveType.doubleType,
				HookManager.getInstance().getClassPool().get("com.wurmonline.server.items.Item"),
				CtPrimitiveType.doubleType,
				CtPrimitiveType.booleanType,
				CtPrimitiveType.floatType,
				CtPrimitiveType.booleanType,
				CtPrimitiveType.doubleType
			});
			HookManager.getInstance().registerHook("com.wurmonline.server.skills.Skill", "checkAdvance", descriptor, new InvocationHandlerFactory() {
				@Override
				public InvocationHandler createInvocationHandler() {
					return new InvocationHandler() {
/*
[08:52:54 PM] INFO com.friya.wurmonline.server.vamps.SimulateAction: ############ calling skillCheck on simSkill
[08:52:54 PM] INFO com.friya.wurmonline.server.vamps.Mod: checkAdvance name: Channeling
[08:52:54 PM] INFO com.friya.wurmonline.server.vamps.Mod: checkAdvance check: 50.0
[08:52:54 PM] INFO com.friya.wurmonline.server.vamps.Mod: checkAdvance item: null
[08:52:54 PM] INFO com.friya.wurmonline.server.vamps.Mod: checkAdvance bonus: 0.0
[08:52:54 PM] INFO com.friya.wurmonline.server.vamps.Mod: checkAdvance dryRun: false
[08:52:54 PM] INFO com.friya.wurmonline.server.vamps.Mod: checkAdvance times: 10.0
[08:52:54 PM] INFO com.friya.wurmonline.server.vamps.Mod: checkAdvance useNewSystem: true
[08:52:54 PM] INFO com.friya.wurmonline.server.vamps.Mod: checkAdvance skillDivider: 2.0

[08:52:54 PM] INFO com.friya.wurmonline.server.vamps.SimulateAction: ############ calling skillCheck on real skill
[08:52:54 PM] INFO com.friya.wurmonline.server.vamps.Mod: checkAdvance name: Channeling
[08:52:54 PM] INFO com.friya.wurmonline.server.vamps.Mod: checkAdvance check: 50.0
[08:52:54 PM] INFO com.friya.wurmonline.server.vamps.Mod: checkAdvance item: null
[08:52:54 PM] INFO com.friya.wurmonline.server.vamps.Mod: checkAdvance bonus: 0.0
[08:52:54 PM] INFO com.friya.wurmonline.server.vamps.Mod: checkAdvance dryRun: false
[08:52:54 PM] INFO com.friya.wurmonline.server.vamps.Mod: checkAdvance times: 10.0
[08:52:54 PM] INFO com.friya.wurmonline.server.vamps.Mod: checkAdvance useNewSystem: true
[08:52:54 PM] INFO com.friya.wurmonline.server.vamps.Mod: checkAdvance skillDivider: 2.0

in-game:
[08:55:35 PM] INFO com.friya.wurmonline.server.vamps.Mod: checkAdvance name: Channeling
[08:55:35 PM] INFO com.friya.wurmonline.server.vamps.Mod: checkAdvance check: 20.0
[08:55:35 PM] INFO com.friya.wurmonline.server.vamps.Mod: checkAdvance item: null
[08:55:35 PM] INFO com.friya.wurmonline.server.vamps.Mod: checkAdvance bonus: 64.5
[08:55:35 PM] INFO com.friya.wurmonline.server.vamps.Mod: checkAdvance dryRun: false
[08:55:35 PM] INFO com.friya.wurmonline.server.vamps.Mod: checkAdvance times: 10.0
[08:55:35 PM] INFO com.friya.wurmonline.server.vamps.Mod: checkAdvance useNewSystem: true
[08:55:35 PM] INFO com.friya.wurmonline.server.vamps.Mod: checkAdvance skillDivider: 2.0

 */
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							logger.log(Level.INFO, "checkAdvance name: " + ((Skill)proxy).getName());
							logger.log(Level.INFO, "checkAdvance check: " + (double)args[0]);
							logger.log(Level.INFO, "checkAdvance item: " + (Item)args[1]);
							logger.log(Level.INFO, "checkAdvance bonus: " + (double)args[2]);
							logger.log(Level.INFO, "checkAdvance dryRun: " + (boolean)args[3]);
							logger.log(Level.INFO, "checkAdvance times: " + (float)args[4]);
							logger.log(Level.INFO, "checkAdvance useNewSystem: " + (boolean)args[5]);
							logger.log(Level.INFO, "checkAdvance skillDivider: " + (double)args[6]);
							
							Object result = method.invoke(proxy, args);
							return result;
						}
					};
				}
			});
			
			descriptor = Descriptor.ofMethod(CtClass.voidType, new CtClass[] {
					//HookManager.getInstance().getClassPool().get("com.wurmonline.server.players.Player")
					CtPrimitiveType.doubleType,
					CtPrimitiveType.booleanType,
					CtPrimitiveType.floatType,
					CtPrimitiveType.booleanType,
					CtPrimitiveType.doubleType,
				});
			HookManager.getInstance().registerHook("com.wurmonline.server.skills.Skill", "alterSkill", descriptor, new InvocationHandlerFactory() {
				
				@Override
				public InvocationHandler createInvocationHandler() {
					return new InvocationHandler() {
						
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							Skill s = (Skill)proxy;
							logger.log(Level.INFO, "name: " + s.getName());
							logger.log(Level.INFO, "type: " + s.getType());
// NOTE:
// isplayer IS different, and it does seem to influence skill-gain at the very bottom of the method

							logger.log(Level.INFO, "tickTime: " + SkillSystem.getTickTimeFor(s.getNumber()));
							logger.log(Level.INFO, "getSkillGainRate: " + Servers.localServer.getSkillGainRate());
							
// simskill:	[05:41:01 PM] INFO com.friya.wurmonline.server.vamps.SimulateAction: alterSkill() advanceMultiplicator: 0.00060884
// realskill:	[05:41:01 PM] INFO com.friya.wurmonline.server.vamps.SimulateAction: alterSkill() advanceMultiplicator: 0.00020295
//				[05:41:01 PM] INFO com.friya.wurmonline.server.vamps.SimulateAction:   simSkill now at: 10.844050911176247
//				[05:41:01 PM] INFO com.friya.wurmonline.server.vamps.SimulateAction: real skill now at: 10.860909414354808
							// this is so we can properly simulate skill gains
							if(s.getId() == -152) {
//[05:53:31 PM] INFO com.friya.wurmonline.server.vamps.SimulateAction:   simSkill now at: 10.860909414354808
//[05:53:31 PM] INFO com.friya.wurmonline.server.vamps.SimulateAction: real skill now at: 10.860909414354808

// SLIGHTLY more gain in-game, why!?
// actual in-game:							 [17:55:37] Channeling increased by 0.3838 to 10.8838		-- above 20 favor
//								             [18:00:47] Channeling increased by 0.3838 to 10.8838		-- below 20 favor

								// Simulate a player
								double advanceMultiplicator = (double)args[0];
								advanceMultiplicator *= (Servers.localServer.EPIC ? 3.0 : 1.5);
								// advanceMultiplicator *= (double)(1.0f + ItemBonus.getSkillGainBonus(player, this.getNumber()));

								// stamina, nutrition -- we pretend that a player walk around with 99 nutrition
								float staminaMod = 1.0f;
								staminaMod += Math.max(0.99 / 10.0f - 0.05f, 0.0f);
								advanceMultiplicator *= (double)staminaMod;
								
								// 3x skillgain due to previous loss
								// 2x skill for sleep bonus
								// 10% extra for affinity
								// is paying and above 20
								
								args[0] = advanceMultiplicator;
							}
							
							Object result = method.invoke(proxy, args);
							TestAction.debugAlterSkill((double)args[0], (boolean)args[1], (float)args[2], (boolean)args[3], (double)args[4]);
							return result;
						}
					};
				}
			});
			
		} catch (Exception /*NotFoundException | CannotCompileException */e) {
			throw new HookException(e);
		}
		
	}

	static String getDescriptorForClass(final CtClass c)
	{
		if(c.isPrimitive())
	    {
			return ((CtPrimitiveType)c).getGetMethodDescriptor();
/*
			if(c == byte.class)
	            return "B";
	        if(c == char.class)
	            return "C";
	        if(c == double.class)
	            return "D";
	        if(c == float.class)
	            return "F";
	        if(c == int.class)
	            return "I";
	        if(c == long.class)
	            return "J";
	        if(c == short.class)
	            return "S";
	        if(c == boolean.class)
	            return "Z";
	        if(c == void.class)
	            return "V";
	        throw new RuntimeException("Unrecognized primitive "+c);
*/
	    }

	    if(c.isArray()) return c.getName().replace('.', '/');
	    return ('L'+c.getName()+';').replace('.', '/');
	}

	static String getMethodDescriptor(CtMethod m) throws NotFoundException
	{
	    String s="(";
	    for(final CtClass c:(m.getParameterTypes()))
	        s += getDescriptorForClass(c);
	    s+=')';
	    return s+getDescriptorForClass(m.getReturnType());
	}

}
