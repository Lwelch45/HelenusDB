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
package com.orangerhymelabs.helenusdb.cassandra.database;

import com.orangerhymelabs.helenusdb.cassandra.Constants;
import com.strategicgains.syntaxe.annotation.RegexValidation;

/**
 * @author tfredrich
 * @since Jan 25, 2015
 */
public class DatabaseReference
{
	@RegexValidation(name = "Database Name", nullable = false, pattern = Constants.NAME_PATTERN, message = Constants.NAME_MESSAGE)
	private String name;

	public DatabaseReference(String name)
	{
		this.name = name;
	}

	public DatabaseReference(Database database)
    {
		this(database.name());
    }

	public String name()
	{
		return name;
	}

	public Database asObject()
    {
		return new Database(name);
    }
}