package com.orangerhymelabs.orangedb.cassandra.document;

import com.orangerhymelabs.orangedb.cassandra.event.AbstractEvent;

/**
 * @author toddf
 * @since Nov 19, 2014
 */
public class DocumentUpdatedEvent
extends AbstractEvent<Document>
{
	public DocumentUpdatedEvent(Document document)
	{
		super(document);
	}
}