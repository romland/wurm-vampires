package com.friya.wurmonline.server.vamps;

import java.util.ArrayList;

import com.friya.wurmonline.server.vamps.events.EventOnce;

public class EventDispatcher
{
    private static ArrayList<EventOnce> events = new ArrayList<EventOnce>();

    public EventDispatcher()
    {
	}

	static public void add(EventOnce event)
	{
		events.add(event);
	}

	static public void poll()
	{
		if(events.size() == 0) {
			return;
		}

		ArrayList<EventOnce> found = new ArrayList<EventOnce>();
		long ts = System.currentTimeMillis();

		for(EventOnce event : events){
			if(event.getInvokeAt() < ts) {
				if(event.invoke()) {
					found.add(event);
				}
			}
		}

		if(found.size() > 0) {
			events.removeAll(found);
		}
	}
}
