package com.wurmonline.server.questions;

import java.util.Properties;
import java.util.logging.Level;

import com.friya.tools.BmlForm;
import com.friya.wurmonline.server.vamps.Mod;
import com.friya.wurmonline.server.vamps.Vampires;
import com.friya.wurmonline.server.vamps.actions.HalfVampAction;
import com.friya.wurmonline.server.vamps.items.HalfVampireClue;
import com.wurmonline.server.FailedException;
import com.wurmonline.server.Servers;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.NoSuchActionException;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemFactory;
import com.wurmonline.server.items.NoSuchTemplateException;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.Question;

public class HalfVampQuestion extends Question 
{
	private boolean properlySent = false;

	HalfVampQuestion(Creature aResponder, String aTitle, String aQuestion, int aType, long aTarget)
	{
		super(aResponder, aTitle, aQuestion, aType, aTarget);
	}

	public HalfVampQuestion(Creature aResponder, String aTitle, String aQuestion, long aTarget)
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
			try {
				Item clue = ItemFactory.createItem(HalfVampireClue.getId(), 10.0f, (byte)0, Vampires.halfVampMakerName);
				this.getResponder().getInventory().insertItem(clue, true);

			} catch (FailedException | NoSuchTemplateException e) {
				logger.log(Level.SEVERE, "Could not find the half-vampire clue, but continuing anyway...", e);
			}
        	
            this.getResponder().getCommunicator().sendNormalServerMessage("You chose to accept...");
            this.getResponder().getCommunicator().sendNormalServerMessage(Vampires.halfVampMakerName + " discreetly hands you a papyrus sheet.");
            this.getResponder().getCommunicator().sendNormalServerMessage(Vampires.halfVampMakerName + " leans over to carefully pierce your skin with her lethal fangs... What could possibly go wrong...");
            Vampires.createVampire((Player)(this.getResponder()), true);
            this.getResponder().getCommunicator().sendAlertServerMessage("You are half vampire!", (byte)4);
            Mod.loginVampire((Player)(this.getResponder()));

        } else {
            this.getResponder().getCommunicator().sendNormalServerMessage("You decide to turn down the offer for now...");
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
		        if (act.getNumber() == HalfVampAction.actionId) {
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
			f.addImage("http://filterbubbles.com/img/wu/bloody-hand.png?s=" + Servers.localServer.getName() + "&i=" + Servers.localServer.EXTERNALIP, 300, 300);

			f.addBoldText(getQuestion());
			
			f.addText(
				"\n"
				+ "I am still looking for %2$s, so I thank you for the papyrus sheet. It will hopefully help me in my quest to find him.\n"
				+ "\n"
				+ "To show my gratitude, I will make you an offer to become a half vampire (not a vampire). Your life will surely change for the better, with heightened senses and new abilities.\n" 
				+ "\n"
				+ "In short, as a half vampire: \n"
				+ "    - you WILL have a lust for blood, albeit not as strong as a full vampire\n"
				+ "    - you will not have any other special abilities\n"
				+ "    - you will not be able to participate in hunting\n"
				+ "    - you will not have to fear slayers\n"
				+ "\n"
				+ "...that is, until you become a full vampire.\n"
				+ "\n"
				+ "WARNING: If for some reason you want to get rid of the vampiric beast within you, it will come at a cost. At a cost you can feel. You are warned.\n"
				+ "\n"
				+ "There is one problem, though, should you accept, you will need to find %2$s and convince him that you are worthy of becoming a full member of his coven.\n"
				+ "\n"
				+ "I wish you the best of luck.\n\n",
				getResponder().getName(),
				Vampires.headVampireName
			);

			f.addBoldText("\nWould you like " + Vampires.halfVampMakerName + " to make you a half vampire?");
			
			f.addText(" \n");

			f.beginHorizontalFlow();
			f.addText("                                                    ");
			f.addButton("No, I will pass.", "decline");
			f.addText("                          ");
			f.addButton("Yes, I accept!", "accept");
			f.endHorizontalFlow();
			f.addText(" \n");
			f.addText(" \n");

			this.getResponder().getCommunicator().sendBml(
					400,				// width
					540,				// height
					true,				// resizable
					true,				// closable
					f.toString(), 		// the BML
					200,				// window color int r
					150,				// window color int g
					150,				// window color int b
					this.title			// title (in title bar)
			);
		}
	}
	
	
}
