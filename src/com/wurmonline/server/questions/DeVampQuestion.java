package com.wurmonline.server.questions;

import java.util.Properties;

import com.friya.tools.BmlForm;
import com.friya.wurmonline.server.vamps.EventDispatcher;
import com.friya.wurmonline.server.vamps.Vampires;
import com.friya.wurmonline.server.vamps.actions.DevampAction;
import com.friya.wurmonline.server.vamps.events.DelayedDeVamp;
import com.friya.wurmonline.server.vamps.events.DelayedMessage;
import com.friya.wurmonline.server.vamps.events.EventOnce.Unit;
import com.wurmonline.server.Servers;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.NoSuchActionException;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.questions.Question;

public class DeVampQuestion extends Question 
{
	private boolean properlySent = false;

	DeVampQuestion(Creature aResponder, String aTitle, String aQuestion, int aType, long aTarget)
	{
		super(aResponder, aTitle, aQuestion, aType, aTarget);
	}

	public DeVampQuestion(Creature aResponder, String aTitle, String aQuestion, long aTarget)
	{
		super(aResponder, aTitle, aQuestion, 79, aTarget);		// 79: locate-question
	}
	

    @Override
	public void answer(Properties answer)
	{
    	if (!this.properlySent) {
            return;
        }

        boolean accepted = answer.containsKey("accept") && answer.get("accept") == "true";

        if (accepted) {
            boolean success = Vampires.deVamp(this.getResponder());
            
            if(success) {
            	this.getResponder().getStatus().setStunned(30.0f);
            	
                this.getResponder().getCommunicator().sendAlertServerMessage("You quaff the potion. Was this a good idea?", (byte)4);

                EventDispatcher.add(new DelayedMessage(4, Unit.SECONDS, this.getResponder(), "#*(&$_)@&#*&@#"));
                EventDispatcher.add(new DelayedMessage(8, Unit.SECONDS, this.getResponder(), "You realize with grim certainty, that you are about to die..."));
                EventDispatcher.add(new DelayedMessage(12, Unit.SECONDS, this.getResponder(), "There is a shattering scream from within your head as the beast is driven from your lifeless body."));
                EventDispatcher.add(new DelayedMessage(18, Unit.SECONDS, this.getResponder(), "Pain, the likes of which you haven't felt since you last were mortal floods your body."));
                EventDispatcher.add(new DelayedMessage(22, Unit.SECONDS, this.getResponder(), "Your vision fades and you slowly die as your lifeblood spills."));
                EventDispatcher.add(new DelayedDeVamp(26, Unit.SECONDS, this.getResponder(), "Finally your soul rises from your body, and you find peace, leaving this unnatural form behind forever!"));

            } else {
                this.getResponder().getCommunicator().sendNormalServerMessage("HUH! You could not be de-vamped. Please talk to an admin, there should be some errors in their logs.");
            }
        } else {
            this.getResponder().getCommunicator().sendNormalServerMessage("You choose not to...");
        }
	}


	@Override
	public void sendQuestion()
	{
		boolean ok = true;

		if (this.getResponder().getPower() <= 0) {
		    try {
		        ok = false;
		        Action act = this.getResponder().getCurrentAction();
		        if (act.getNumber() == DevampAction.actionId) {
		            ok = true;
		        }
		    }
		    catch (NoSuchActionException act) {
		    	throw new RuntimeException("No such action", act);
		    }
		}
		
		if (ok) {
			this.properlySent = true;

			BmlForm f = new BmlForm("");
			//BmlForm f = new BmlForm();
			f.addHidden("id", "" + this.id);

			// Need to have some idea of which servers are requesting these things in case bandwidth goes bonkers. So passing in server-name for the image.
			f.addImage("http://filterbubbles.com/img/wu/devamp-potion.png?s=" + Servers.localServer.getName() + "&i=" + Servers.localServer.EXTERNALIP, 200, 200);

			f.addBoldText(getQuestion());

			f.addText(
				"\n"
				+ "You are one of them. A dweller of darkness. A vampire.\n"
				+ "\n"
				+ "I can cure you, but be warned, not only will it kill you, it will cost you dearly too. A lot more than you could possibly imagine.\n"
				+ "\n"
				+ "\n"
				+ "(Really, no joke ... you will lose a lot of skill. You may also not be able to become a vampire again for a some time! You ARE warned!)\n\n",
				getResponder().getName(),
				Vampires.headVampireName
			);

			f.addBoldText("\nYou will no longer be a vampire; are you sure you want to drink " + Vampires.deVampManName + "'s potion?");
			
			f.addText(" \n");

			f.beginHorizontalFlow();
			f.addButton("No thank you (the safe answer).", "decline");
			f.addText("                          ");
			f.addButton("Yes, I want to drink the potion. Cure me!", "accept");
			f.endHorizontalFlow();
			f.addText(" \n");
			f.addText(" \n");

			this.getResponder().getCommunicator().sendBml(
					550,				// width
					500,				// height
					true,				// resizable
					true,				// closable
					f.toString(), 		// the BML
					150,				// window color int r
					150,				// window color int g
					200,				// window color int b
					this.title			// title (in title bar)
			);
		}
	}
	
	
}
