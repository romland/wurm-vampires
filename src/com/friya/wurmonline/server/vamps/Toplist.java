package com.friya.wurmonline.server.vamps;

import com.wurmonline.server.players.Player;

public class Toplist
{
	private String[] names;
	private long[] scores;
	public int added = 0;
	
	public Toplist(int listSize)
	{
		names = new String[listSize];
		scores = new long[listSize];
	}

	public void sendTo(Player p)
	{
		for(int i = 0; i < added; i++) {
	        p.getCommunicator().sendNormalServerMessage((i+1) + "    " + names[i] + ", " + scores[i] + " rating");
		}
	}
	
	public String toString()
	{
		StringBuffer s = new StringBuffer();
		for(int i = 0; i < added; i++) {
	        s.append((i+1) + " " + String.format("%1$20s %2$20s", names[i], scores[i]));
		}
		return s.toString();
	}
	
	void addNameScore(String name, long score)
	{
		if(added == names.length) {
			throw new IndexOutOfBoundsException("Tried to add too many scores to toplist, bailing");
		}
		names[added] = name;
		scores[added] = score;
		added++;
	}
	
	public String getName(int index)
	{
		return names[index];
	}

	public long getScore(int index)
	{
		return scores[index];
	}

	public String[] getNames()
	{
		return names;
	}

	public long[] getScores()
	{
		return scores;
	}
}
