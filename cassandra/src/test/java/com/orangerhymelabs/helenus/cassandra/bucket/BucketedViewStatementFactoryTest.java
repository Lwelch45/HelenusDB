package com.orangerhymelabs.helenus.cassandra.bucket;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Date;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.datastax.driver.core.BoundStatement;
import com.orangerhymelabs.helenus.cassandra.CassandraManager;
import com.orangerhymelabs.helenus.cassandra.SchemaRegistry;
import com.orangerhymelabs.helenus.cassandra.bucket.BucketedViewStatementFactory;
import com.orangerhymelabs.helenus.cassandra.document.Document;
import com.orangerhymelabs.helenus.cassandra.index.BucketedViewIndex;
import com.orangerhymelabs.helenus.cassandra.index.IndexRepository;
import com.orangerhymelabs.helenus.cassandra.table.Table;
import com.orangerhymelabs.helenus.cassandra.table.TableRepository;

public class BucketedViewStatementFactoryTest
{
	private static BucketedViewStatementFactory factory;
	private static BucketedViewIndex abcIndex;

	@BeforeClass
	public static void beforeClass()
	throws Exception
	{
		CassandraManager.start();
		SchemaRegistry.instance().createAll(CassandraManager.session(), CassandraManager.keyspace());
		TableRepository tables = new TableRepository(CassandraManager.session(), CassandraManager.keyspace());
		IndexRepository indexes = new IndexRepository(CassandraManager.session(), CassandraManager.keyspace());

		Table table = new Table();
		table.database("dbItable1");
		table.name("indexed1");
		table.description("A sample indexed table");
		table = tables.create(table);

		abcIndex = new BucketedViewIndex();
		abcIndex.name("index_abc");
		abcIndex.fields(Arrays.asList("a:text", "b:integer", "c:text"));
		abcIndex.table(table);
		abcIndex = (BucketedViewIndex) indexes.create(abcIndex);

		factory = new BucketedViewStatementFactory(CassandraManager.session(), CassandraManager.keyspace(), table);
	}

	@AfterClass
	public static void afterClass()
	{
		SchemaRegistry.instance().dropAll(CassandraManager.session(), CassandraManager.keyspace());
	}

	@Test
	public void shouldCreateMultiKeyStatement()
	{
		BSONObject bson = new BasicBSONObject();
		bson.put("a", "textA");
		bson.put("b", 42);
		bson.put("c", "textC");
		Document d = new Document();
		d.object(bson);
		Date now = new Date();
		d.createdAt(now);
		d.updatedAt(now);

		BoundStatement bs = factory.createIndexEntryCreateStatement(d, abcIndex);
		assertNotNull(bs);
		// TODO: verify bs is fully populated.
	}
}