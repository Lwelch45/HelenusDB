package com.orangerhymelabs.orangedb.cassandra.itable;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Date;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.datastax.driver.core.BoundStatement;
import com.orangerhymelabs.orangedb.cassandra.CassandraManager;
import com.orangerhymelabs.orangedb.cassandra.SchemaRegistry;
import com.orangerhymelabs.orangedb.cassandra.document.Document;
import com.orangerhymelabs.orangedb.cassandra.index.Index;
import com.orangerhymelabs.orangedb.cassandra.table.Table;
import com.orangerhymelabs.orangedb.cassandra.table.TableRepository;

public class ItableStatementFactoryTest
{
	private static ItableStatementFactory factory;

	@BeforeClass
	public static void beforeClass()
	throws Exception
	{
		CassandraManager.start();
		SchemaRegistry.instance().createAll(CassandraManager.session(), CassandraManager.keyspace());
		TableRepository tables = new TableRepository(CassandraManager.session(), CassandraManager.keyspace());

		Table table = new Table();
		table.database("dbItable1");
		table.name("indexed1");
		table.description("A sample indexed table");
		table = tables.create(table);

		factory = new ItableStatementFactory(CassandraManager.session(), CassandraManager.keyspace(), table);
	}

	@AfterClass
	public static void afterClass()
	{
		SchemaRegistry.instance().dropAll(CassandraManager.session(), CassandraManager.keyspace());
	}

	@Test
	public void shouldCreateMultiKeyStatement()
	{
		Index i = new Index();
		i.name("index_abc");
		i.fields(Arrays.asList("a:text", "b:text", "c:text"));
		i.table(factory.table());

		BSONObject bson = new BasicBSONObject();
		bson.put("a", "textA");
		bson.put("b", "textB");
		bson.put("c", "textC");
		Document d = new Document();
		d.object(bson);
		Date now = new Date();
		d.createdAt(now);
		d.updatedAt(now);

		BoundStatement bs = factory.createIndexEntryCreateStatement(d, i);
		assertNotNull(bs);
	}
}
