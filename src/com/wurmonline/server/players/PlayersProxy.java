package com.wurmonline.server.players;

import java.util.Set;

public class PlayersProxy
{
	public static Set<Titles.Title> getTitles(PlayerInfo pi)
	{
		return pi.titles;
	}
}
