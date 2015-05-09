package com.orangerhymelabs.orangedb.cassandra.document;

import java.util.UUID;

import com.orangerhymelabs.orangedb.cassandra.table.Table;
import com.orangerhymelabs.orangedb.cassandra.table.TableReference;
import com.orangerhymelabs.orangedb.persistence.AbstractEntity;
import com.orangerhymelabs.orangedb.persistence.Identifier;
import com.strategicgains.syntaxe.annotation.ChildValidation;
import com.strategicgains.syntaxe.annotation.Required;

public class Document
extends AbstractEntity
{
	// TODO: allow something other than UUID as object id.
	// TODO: add any necessary metadata regarding a document.
	// TODO: documents are versioned per transaction via updateAt timestamp.
	private UUID id;

	// need a separate version (as opposed to updatedAt)?
	// private long version;
	@Required("Table")
	@ChildValidation
	private TableReference table;

	// The JSON document.
	private String object;

	public Document()
	{
	}

	@Override
	public Identifier getId()
	{
		return new Identifier(databaseName(), tableName(), id, updatedAt());
	}

	public UUID getUuid()
	{
		return id;
	}

	public void setUuid(UUID id)
	{
		this.id = id;
	}

	public boolean hasTable()
	{
		return (table != null);
	}

	public Table table()
	{
		return (hasTable() ? table.asObject() : null);
	}

	public void table(String database, String table)
	{
		this.table = new TableReference(database, table);
	}

	public void table(Table table)
	{
		this.table = (table != null ? new TableReference(table) : null);
	}

	public String tableName()
	{
		return (hasTable() ? table.name() : null);
	}

	public String databaseName()
	{
		return (hasTable() ? table.database() : null);
	}

	public String object()
	{
		return object;
	}

	public void object(String json)
	{
		this.object = json;
	}

	@Override
	public String toString()
	{
		return "Document{" + "id=" + id + ", table=" + table + ", object=" + object + '}';
	}

}