package com.friya.wurmonline.server.vamps.actions;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import com.friya.wurmonline.server.vamps.Cooldowns;
import com.friya.wurmonline.server.vamps.Mod;
import com.friya.wurmonline.server.vamps.Vampires;
import com.friya.wurmonline.server.vamps.items.Mirror;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.NoSuchActionException;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;


public class MirrorAction implements ModAction
{
	private static Logger logger = Logger.getLogger(MirrorAction.class.getName());

	private final short actionId;
	private final ActionEntry actionEntry;
	private final String effectName = "mirror";
	private final int cooldown = 1000 * 20;			// * seconds
	private final int castTime = 10 * 3;			// * seconds

	public MirrorAction()
	{
		logger.log(Level.INFO, "MirrorAction()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"Check reflection", 
			"mirroring",
			new int[] {		// 6 /* ACTION_TYPE_NOMOVE */
				6,
				23			// ACTION_TYPE_IGNORERANGE
			}
		);
		ModActions.registerAction(actionEntry);
	}


	@Override
	public BehaviourProvider getBehaviourProvider()
	{
		return new BehaviourProvider() {

			public List<ActionEntry> getBehavioursFor(Creature performer, Creature object)
			{
				// We want them to have a mallet activated, give them a handy error in the action...
				return this.getBehavioursFor(performer, null, object);
			}

			public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Creature target)
			{
				if(subject != null && performer.isPlayer() && target.isPlayer() && subject.getTemplateId() == Mirror.getId()) {
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
			public boolean action(Action act, Creature performer, Creature object, short action, float counter)
			{
				return action(act, performer, null, object, action, counter);
			}


			@Override
			public boolean action(Action act, Creature performer, Item mirror, Creature target, short action, float counter)
			{
				if(mirror.getTemplateId() != Mirror.getId()) {
					return true;
				}

				if(mirror.getAuxData() == 1) {
					Mod.actionNotify(
						performer, 
						"The mirror is all smudged from before, you will need a top notch pelt to clean it. NOTE: This might consume the pelt.",
						"%NAME looks curiously at a silver mirror then frowns.",
						"In the corner of your eye you see a shiny reflection of something."
					);
					return true;
				}

				if(target.isPlayer() == false && performer.getPower() <= 1) {
					return true;
				}
				
				// Allow some distance -- 1 tile inbetween (so action can reach from up to three tiles, inclusive)
				if (performer.isWithinTileDistanceTo(target.getTileX(), target.getTileY(), 0, 1) == false) {
					performer.getCommunicator().sendNormalServerMessage("That is too far away.");
					return true;
				}

				if(performer.getWurmId() == target.getWurmId()) {
			    	performer.getCommunicator().sendNormalServerMessage("You look fantastic.");
					return true;
				}

				String playerEffect = performer.getName() + effectName;

				if(Cooldowns.isOnCooldown(playerEffect, cooldown)) {
			    	performer.getCommunicator().sendNormalServerMessage("The silver mirror needs to rest a little while.");
					return true;
				}

				try {
					// At start of action (or not?):
					// 		You angle the mirror to look at XX.
					// 		XX tilts HIS silver mirror, looking at you through it, 
					// 		XX looks at XX in a silver mirror.
					
					if(counter == 1.0f) {
						int tmpTime = castTime;
						performer.getCurrentAction().setTimeLeft(tmpTime);
						performer.sendActionControl("Checking reflection", true, tmpTime);
						performer.getCommunicator().sendNormalServerMessage("You angle the mirror to look at " + target.getName() + ".");
						return false;
					}
					
					if(counter * 10.0f <= act.getTimeLeft()) {
						return false;
					}
				} catch (NoSuchActionException e) {
					return true;
				}

				Cooldowns.setUsed(playerEffect);

				performer.getStatus().modifyStamina((int)( performer.getStatus().getStamina() * 0.5f ));
				
				if(Vampires.isHalfVampire(target.getWurmId())) {
					// is half:
					String msg = Mod.fixActionString(target, "You see %NAME as a faded image in the mirror. %NAME is a half vampire, but there is still hope for %HIM in this world.");
					performer.getCommunicator().sendNormalServerMessage(msg);
					target.getCommunicator().sendNormalServerMessage(Mod.fixActionString(performer, "%NAME tilts %HIS silver mirror, looking at you through it."));

				} else if(Vampires.isVampire(target.getWurmId())) {
					// is full:
					String msg = Mod.fixActionString(target, "%NAME has absolutely no image in the mirror at all! %NAME is a vampire in all respects, a demon of the night.");
					performer.getCommunicator().sendNormalServerMessage(msg);

					// if Vampire, give them announcement that their reflection was checked
					target.getCommunicator().sendAlertServerMessage(performer.getName() + " checked your (lack of) reflection in a shiny silver mirror.", (byte)0);

				} else {
					// is human:
					String msg = Mod.fixActionString(
						target, 
						"You see a common reflection of %NAME in the mirror. You feel kind of silly now, in the embarrassment you smudge the mirror. You'll need to polish it."
					);
					performer.getCommunicator().sendNormalServerMessage(msg);
					target.getCommunicator().sendNormalServerMessage(Mod.fixActionString(performer, "%NAME tilts %HIS silver mirror, looking at you through it."));

					mirror.setAuxData((byte)1);
				}
				
		    	return true;
			}
		}; // ActionPerformer
	}
	
}
