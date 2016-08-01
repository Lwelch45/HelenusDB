/*
    Copyright 2015, Strategic Gains, Inc.

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

		http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
 */
package com.orangerhymelabs.helenus.cassandra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.CodecNotFoundException;
import com.datastax.driver.core.exceptions.InvalidTypeException;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.orangerhymelabs.helenus.exception.DuplicateItemException;
import com.orangerhymelabs.helenus.exception.InvalidIdentifierException;
import com.orangerhymelabs.helenus.exception.ItemNotFoundException;
import com.orangerhymelabs.helenus.exception.StorageException;
import com.orangerhymelabs.helenus.persistence.Identifier;

/**
 * @author tfredrich
 * @since Jun 8, 2015
 * @param <T> The type stored in this repository.
 */
public abstract class AbstractCassandraRepository<T>
{
	private Session session;
	private String keyspace;

	private PreparedStatement createStmt;
	private PreparedStatement updateStmt;
	private PreparedStatement readAllStmt;
	private PreparedStatement readStmt;
	private PreparedStatement deleteStmt;

	public AbstractCassandraRepository(Session session, String keyspace)
	{
		this.session = session;
		this.keyspace = keyspace;
		initializeStatements();
	}

	protected void initializeStatements()
	{
		createStmt = prepare(buildCreateStatement());
		updateStmt = prepare(buildUpdateStatement());
		readAllStmt = prepare(buildReadAllStatement());
		readStmt = prepare(buildReadStatement());
		deleteStmt = prepare(buildDeleteStatement());
	}

	protected PreparedStatement createStmt()
	{
		return createStmt;
	}

	protected PreparedStatement updateStmt()
	{
		return updateStmt;
	}

	protected PreparedStatement deleteStmt()
	{
		return deleteStmt;
	}

	public void createAsync(T entity, FutureCallback<T> callback)
	{
		ResultSetFuture future = null;
		
		try
		{
			future = _create(entity);
		}
		catch(Exception e)
		{
			callback.onFailure(e);
			return;
		}

		Futures.addCallback(future, new FutureCallback<ResultSet>()
		{
			@Override
			public void onSuccess(ResultSet result)
			{
				if (!result.wasApplied())
				{
					callback.onFailure(new DuplicateItemException(entity.toString()));
				}

				callback.onSuccess(null);
			}

			@Override
			public void onFailure(Throwable t)
			{
				callback.onFailure(t);
			}
		}, MoreExecutors.newDirectExecutorService());
	}

	public T create(T entity)
	{
		try
		{
			ResultSet rs = _create(entity).get();

			if (rs.wasApplied())
			{
				return entity;
			}

			throw new DuplicateItemException(entity.toString());
		}
		catch (InterruptedException | ExecutionException e)
		{
			throw new StorageException(e);
		}
	}

	protected ResultSetFuture _create(T entity)
	{
		BoundStatement bs = new BoundStatement(createStmt);
		bindCreate(bs, entity);
		return session.executeAsync(bs);
	}

	public void updateAsync(T entity, FutureCallback<T> callback)
	{
		ResultSetFuture future = null;
		
		try
		{
			future = _update(entity);
		}
		catch (Exception e)
		{
			callback.onFailure(e);
			return;
		}

		Futures.addCallback(future, new FutureCallback<ResultSet>()
		{
			@Override
			public void onSuccess(ResultSet result)
			{
				if (!result.wasApplied())
				{
					callback.onFailure(new ItemNotFoundException(entity.toString()));
				}

				callback.onSuccess(null);
			}

			@Override
			public void onFailure(Throwable t)
			{
				callback.onFailure(t);
			}
		}, MoreExecutors.newDirectExecutorService());
	}

	public T update(T entity)
	{
		try
		{
			ResultSet rs = _update(entity).get();

			if (rs.wasApplied())
			{
				return entity;
			}

			throw new ItemNotFoundException(entity.toString());
		}
		catch (InterruptedException | ExecutionException e)
		{
			throw new StorageException(e);
		}
	}

	protected ResultSetFuture _update(T entity)
	{
		BoundStatement bs = new BoundStatement(updateStmt);
		bindUpdate(bs, entity);
		return session.executeAsync(bs);
	}

	public void deleteAsync(Identifier id, FutureCallback<T> callback)
	{
		ResultSetFuture future = _delete(id);
		Futures.addCallback(future, new FutureCallback<ResultSet>()
		{
			@Override
			public void onSuccess(ResultSet result)
			{
				if (!result.wasApplied())
				{
					callback.onFailure(new ItemNotFoundException(id.toString()));
				}

				callback.onSuccess(null);
			}

			@Override
			public void onFailure(Throwable t)
			{
				callback.onFailure(t);
			}
		}, MoreExecutors.newDirectExecutorService());
	}

	public void delete(Identifier id)
	{
		try
		{
			_delete(id).get();
		}
		catch (InterruptedException | ExecutionException e)
		{
			throw new StorageException(e);
		}
	}

	protected ResultSetFuture _delete(Identifier id)
	{
		BoundStatement bs = new BoundStatement(deleteStmt);
		bindIdentity(bs, id);
		return session.executeAsync(bs);
	}

	public void readAsync(Identifier id, FutureCallback<T> callback)
	{
		ResultSetFuture future;

		try
		{
			future = _read(id);
		}
		catch(Exception e)
		{
			callback.onFailure(e);
			return;
		}

		Futures.addCallback(future, new FutureCallback<ResultSet>()
		{
			@Override
			public void onSuccess(ResultSet result)
			{
				if (result.isExhausted())
				{
					callback.onFailure(new ItemNotFoundException(id.toString()));
				}

				T entity = marshalRow(result.one());
				callback.onSuccess(entity);
			}

			@Override
			public void onFailure(Throwable t)
			{
				callback.onFailure(t);
			}
		}, MoreExecutors.newDirectExecutorService());
	}

	public T read(Identifier id)
	{
		try
		{
			ResultSet rs = _read(id).get();

			if (rs.isExhausted())
			{
				throw new ItemNotFoundException(id.toString());
			}

			T entity = marshalRow(rs.one());
			return entity;
		}
		catch (InterruptedException | ExecutionException e)
		{
			throw new StorageException(e);
		}
	}

	private ResultSetFuture _read(Identifier id)
	{
		BoundStatement bs = new BoundStatement(readStmt);
		bindIdentity(bs, id);
		return session.executeAsync(bs);
	}

	public void readAllAsync(FutureCallback<List<T>> callback, Object... parms)
	{
		ResultSetFuture future = _readAll(parms);
		Futures.addCallback(future, new FutureCallback<ResultSet>()
		{
			@Override
			public void onSuccess(ResultSet result)
			{
				callback.onSuccess(marshalAll(result));
			}

			@Override
			public void onFailure(Throwable t)
			{
				callback.onFailure(t);
			}
		}, MoreExecutors.newDirectExecutorService());
	}

	public List<T> readAll(Object... parms)
	{
		try
		{
			ResultSet rs = _readAll(parms).get();
			return marshalAll(rs);
		}
		catch (InterruptedException | ExecutionException e)
		{
			throw new StorageException(e);
		}
	}

	private ResultSetFuture _readAll(Object... parms)
	{
		BoundStatement bs = new BoundStatement(readAllStmt);

		if (parms != null)
		{
			bs.bind(parms);
		}

		return session.executeAsync(bs);
	}

	/**
	 * Read all given identifiers.
	 * 
	 * Leverages the token-awareness of the driver to optimally query each node directly instead of invoking a
	 * coordinator node. Sends an individual query for each partition key, so reaches the appropriate replica
	 * directly and collates the results client-side.
	 * 
	 * Note that the callback is not called with a single List of results. Instead it is called once for each
	 * Identifier provided in the call, whether successful or failed. 
	 * 
	 * @param callback a FutureCallback to notify for each ID in the ids array.
	 * @param ids the partition keys (identifiers) to select.
	 */
	public void readInAsync(FutureCallback<T> callback, Identifier... ids)
	{
		List<ListenableFuture<ResultSet>> futures = _readIn(ids);

		for (ListenableFuture<ResultSet> future : futures)
		{
			Futures.addCallback(future,  new FutureCallback<ResultSet>()
				{
					@Override
					public void onSuccess(ResultSet result)
					{
						if (!result.isExhausted())
						{
							callback.onSuccess(marshalRow(result.one()));
						}
					}

					@Override
					public void onFailure(Throwable t)
					{
						callback.onFailure(t);
					}
				}, MoreExecutors.newDirectExecutorService()
			);
		}
	}

	/**
	 * Read all given identifiers, returning them as a List. No order is guaranteed in the resulting list.
	 * 
	 * Leverages the token-awareness of the driver to optimally query each node directly instead of invoking a
	 * coordinator node. Sends an individual query for each partition key, so reaches the appropriate replica
	 * directly and collates the results client-side.
	 * 
	 * @param ids the partition keys (identifiers) to select.
	 * @return a list of entities identified by the identifiers given in the 'ids' parameter.
	 */
	public List<T> readIn(Identifier... ids)
	{
		if (ids == null) return Collections.emptyList();

		List<ListenableFuture<ResultSet>> futures = _readIn(ids);
		List<T> results = new ArrayList<T>(futures.size());

		try
		{
			for (ListenableFuture<ResultSet> future : futures)
			{
				ResultSet rs = future.get();

				if (!rs.isExhausted())
				{
					results.add(marshalRow(rs.one()));
				}
			}

			return results;
		}
		catch (InterruptedException | ExecutionException e)
		{
			throw new StorageException(e);
		}
	}

	/**
	 * Leverages the token-awareness of the driver to optimally query each node directly instead of invoking a
	 * coordinator node. Sends an individual query for each partition key, so reaches the appropriate replica
	 * directly and collates the results client-side.
	 * 
	 * @param ids the partition keys (identifiers) to select.
	 * @return a List of ListenableFuture instances for each underlying ResultSet--one for each ID.
	 */
	private List<ListenableFuture<ResultSet>> _readIn(Identifier... ids)
	{
		if (ids == null) return null;

		List<ResultSetFuture> futures = new ArrayList<ResultSetFuture>(ids.length);

		for (Identifier id : ids)
		{
			BoundStatement bs = new BoundStatement(readStmt);
			bindIdentity(bs, id);
			futures.add(session.executeAsync(bs));
		}

		return Futures.inCompletionOrder(futures);
	}

	public Session session()
	{
		return session;
	}

	protected String keyspace()
	{
		return keyspace;
	}

	protected void bindIdentity(BoundStatement bs, Identifier id)
	{
		try
		{
			bs.bind(id.components().toArray());
		}
		catch(InvalidTypeException | CodecNotFoundException e)
		{
			throw new InvalidIdentifierException(e);
		}
	}

	protected List<T> marshalAll(ResultSet rs)
	{
		List<T> results = new ArrayList<T>();
		Iterator<Row> i = rs.iterator();

		while (i.hasNext())
		{
			results.add(marshalRow(i.next()));
		}

		return results;
	}

	protected abstract void bindCreate(BoundStatement bs, T entity);
	protected abstract void bindUpdate(BoundStatement bs, T entity);
	protected abstract T marshalRow(Row row);
	protected abstract String buildCreateStatement();
	protected abstract String buildUpdateStatement();
	protected abstract String buildReadStatement();
	protected abstract String buildReadAllStatement();
	protected abstract String buildDeleteStatement();

	protected PreparedStatement prepare(String statement)
	{
		if (statement == null || statement.trim().isEmpty())
		{
			return null;
		}

		return session().prepare(statement);
	}
}
