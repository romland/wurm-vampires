package com.wurmonline.server.questions;

import java.util.Properties;

import com.friya.tools.BmlForm;
import com.friya.wurmonline.server.vamps.ChatCommands;
import com.friya.wurmonline.server.vamps.Toplist;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.questions.Question;

public class ToplistVampsQuestion extends Question 
{
	private boolean properlySent = false;

	ToplistVampsQuestion(Creature aResponder, String aTitle, String aQuestion, int aType, long aTarget)
	{
		super(aResponder, aTitle, aQuestion, aType, aTarget);
	}

	public ToplistVampsQuestion(Creature aResponder, String aTitle, String aQuestion, long aTarget)
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
		this.properlySent = true;

		BmlForm f = new BmlForm("");
		f.addHidden("id", "" + this.id);

		int listSize = 15;
		Toplist toplist = ChatCommands.getToplistVampsData(listSize);
		
		f.beginTable(listSize, new String[]{ "Position            ", "Vampire alias                  ", "Rating           " });
		for(int i = 0; i < toplist.added; i++) {
			f.addLabel("" + (i + 1));
			f.addLabel(toplist.getName(i));
			f.addLabel("" + toplist.getScore(i));
		}
		f.endTable();
		f.addText(" \n");
		f.addText(" \n");

		f.beginHorizontalFlow();
		f.addButton("Close", "accept");
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
