package com.orangerhymelabs.orangedb.event;

import org.restexpress.plugin.statechange.domain.StateChangeEvent;

public abstract class AbstractEvent<T>
extends StateChangeEvent<T>
{
	public AbstractEvent(T data)
    {
	    super(data);
    }
}
