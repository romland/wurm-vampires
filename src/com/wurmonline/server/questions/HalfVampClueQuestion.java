package com.wurmonline.server.questions;

import java.util.Properties;

import com.friya.tools.BmlForm;
import com.friya.wurmonline.server.vamps.Mod;
import com.friya.wurmonline.server.vamps.actions.HalfVampClueAction;
import com.wurmonline.server.Servers;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.NoSuchActionException;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.questions.Question;

public class HalfVampClueQuestion extends Question 
{
	private boolean properlySent = false;

	HalfVampClueQuestion(Creature aResponder, String aTitle, String aQuestion, int aType, long aTarget)
	{
		super(aResponder, aTitle, aQuestion, aType, aTarget);
	}

	public HalfVampClueQuestion(Creature aResponder, String aTitle, String aQuestion, long aTarget)
	{
		super(aResponder, aTitle, aQuestion, 79, aTarget);		// 79: locate-question
	}
	


    @Override
	public void answer(Properties answer)
	{
    	if (!this.properlySent) {
            return;
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
		        if (act.getNumber() == HalfVampClueAction.actionId) {
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
			f.addHidden("id", "" + this.id);

			// Need to have some idea of which servers are requesting these things in case bandwidth goes bonkers. So passing in server-name for the image.
			f.addImage(Mod.halfVampClueUrl + "?s=" + Servers.localServer.getName() + "&i=" + Servers.localServer.EXTERNALIP, 345, 420);

			f.beginHorizontalFlow();
			f.addButton("Okay", "accept");
			f.endHorizontalFlow();
			f.addText(" \n");
			f.addText(" \n");

			this.getResponder().getCommunicator().sendBml(
					370,				// width
					520,				// height
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
