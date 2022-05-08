package com.wurmonline.server.creatures.ai;

import com.wurmonline.server.creatures.Creature;

public class AiProxy
{
	static public void clearChatManagerChats(ChatManager cm)
	{
		cm.mychats.clear();
		cm.localchats.clear();
		cm.unansweredLChats.clear();
		cm.receivedchats.clear();
		cm.unansweredChats.clear();
		cm.localChats.clear();
	}
	
	static public Creature getChatManagerOwner(ChatManager cm)
	{
		return cm.owner;
	}
}
