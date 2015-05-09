package com.orangerhymelabs.orangedb.cassandra.event;


public interface EventFactory<T>
{
	Object newCreatedEvent(T object);
	Object newUpdatedEvent(T object);
	Object newDeletedEvent(T object);
}