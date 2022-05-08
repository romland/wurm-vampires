package com.friya.wurmonline.server.vamps.actions;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import com.friya.wurmonline.server.vamps.BloodlessHusk;
import com.friya.wurmonline.server.vamps.Locate;
import com.friya.wurmonline.server.vamps.VampSkills;
import com.wurmonline.server.NoSuchPlayerException;
import com.wurmonline.server.Players;
import com.wurmonline.server.WurmCalendar;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.NoSuchActionException;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.skills.Skill;

public class TraceAction implements ModAction 
{
	private static Logger logger = Logger.getLogger(TraceAction.class.getName());

	static private short actionId;
	private final ActionEntry actionEntry;
	private final int castTime = 10 * 5;

	static public short getActionId()
	{
		return actionId;
	}
	
	public TraceAction() {
		logger.log(Level.INFO, "TraceAction()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"Trace", 
			"tracing",
			new int[] { 6 /* ACTION_TYPE_NOMOVE */ }
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
				if(performer instanceof Player && object != null && object.getTemplateId() == ItemList.corpse && object.getName().startsWith("bloodless husk of ")) {
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
				return trace(act, performer, target, counter);
			}
			
			@Override
			public boolean action(Action act, Creature performer, Item source, Item target, short action, float counter)
			{
				return this.action(act, performer, target, action, counter);
			}
		}; // ActionPerformer
	}


	private String getWurmTimeAgo(long timeStamp, boolean evenLowerRes)
	{
		if(evenLowerRes) {
			String s = getWurmTimeAgo(timeStamp);
			String segs[] = s.split(" ");
			StringBuffer ret = new StringBuffer();
			if(segs[0].equals("1")) {
				ret.append("a");
				if(segs[1].equals("hour")) {
					ret.append("n");
				}
				
				ret.append(" ");
				ret.append(segs[1]);
				ret.append(" ago");
			} else {
				ret.append(segs[1]);
				ret.append("s ago");
			}
			
			return ret.toString();
		}
		
		return getWurmTimeAgo(timeStamp);
	}
	
	private String getWurmTimeAgo(long timeStamp)
	{
		long timeDiffernce;
		long unixTime = WurmCalendar.getCurrentTime();  //get current time in seconds. 
		int j;
		
		String[] periods = {" second", " minute", " hour", " day", " week", " month", " year", " decade"};

		// you may choose to write full time intervals like seconds, minutes, days and so on
		double[] lengths = {60, 60, 24, 7, 4.35, 12, 10};

		timeDiffernce = unixTime - timeStamp;
		String tense = "ago";
		
		for (j = 0; timeDiffernce >= lengths[j] && j < lengths.length - 1; j++) {
			timeDiffernce /= lengths[j];
		}

		return timeDiffernce + periods[j] + " " + tense;
	}

	/**
	 * NOTE: This will forget who created a corpse if the server rebooted! 
	 *       I don't consider that a disaster.
	 * NOTE: This will not say when it turned into a bloodless husk, but when
	 * 		 the NPC was killed.
	 * @param act
	 * @param performer
	 * @param target
	 * @param counter
	 * @return
	 */
	private boolean trace(Action act, Creature performer, Item target, float counter)
	{
		if(performer.isPlayer() == false || target == null || target.getTemplateId() != ItemList.corpse || target.getName().startsWith("bloodless husk of ") == false) {
            performer.getCommunicator().sendNormalServerMessage("You can't seem to figure out how that would work.");
			return true;
		}

		try {
			if(counter == 1.0f) {
				int tmpTime = castTime;
				performer.getCurrentAction().setTimeLeft(tmpTime);
				performer.sendActionControl("tracing", true, tmpTime);
				return false;
			}
			
			if(counter * 10.0f <= act.getTimeLeft()) {
				return false;
			}
		} catch (NoSuchActionException e) {
			return true;
		}

		try {
			Creature vampire = Players.getInstance().getPlayer(BloodlessHusk.getBloodSucker(target));

            performer.getCommunicator().sendNormalServerMessage("This creature was killed " + getWurmTimeAgo(target.creationDate, true) + ".");
            performer.getCommunicator().sendNormalServerMessage("There are clues telling you that the beast who did this went " + Locate.getCompassDirection(target, vampire) + ".");

			Skill s = performer.getSkills().getSkillOrLearn(VampSkills.PERCEPTION);
			s.skillCheck(1.0f, 0.0f, false, 1.0f);

		} catch (NoSuchPlayerException e) {
            performer.getCommunicator().sendNormalServerMessage("You can't seem to figure out which direction they went.");
            return true;
		}

		return true;
	}
}
