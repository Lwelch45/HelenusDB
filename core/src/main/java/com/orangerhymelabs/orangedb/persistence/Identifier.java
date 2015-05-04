package com.orangerhymelabs.orangedb.persistence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Supports the concept of a compound identifier. An Identifier is made up of components, which
 * are Object instances. The components are kept in order of which they are added.
 * 
 * @author toddf
 * @since Aug 29, 2013
 */
public class Identifier
implements Comparable<Identifier>
{
	private static final String SEPARATOR = ", ";

	private List<Object> components = new ArrayList<Object>();

	/**
	 * Create an empty identifier.
	 */
	public Identifier()
	{
		super();
	}
	
	public Identifier(Identifier that)
	{
		this();
		
		if (that == null || that.isEmpty()) return;
		
		add(that.components.toArray());
	}

	/**
	 * Create an identifier with the given components. Duplicate instances
	 * are not added--only one instance of a component will exist in the identifier.
	 * 
	 * @param components
	 */
	public Identifier(Object... components)
	{
		this();
		add(components);
	}

	/**
	 * Add the given components, in order, to the identifier. Duplicate instances
	 * are not added--only one instance of a component will exist in the identifier.
	 * 
	 * @param components
	 */
	public void add(Object... components)
    {
		if (components == null) return;

		for (Object component : components)
		{
			add(component);
		}
    }

	/**
	 * Add a single component to the identifier. The given component is added to
	 * the end of the identifier. Duplicate instances are not added--only one instance
	 * of a component will exist in the identifier.
	 * 
	 * @param component
	 */
	public void add(Object component)
    {
		if (component == null) return;

		components.add(component);
    }

	/**
	 * Get an unmodifiable list of the components that make up this identifier.
	 * 
	 * @return an unmodifiable list of components.
	 */
	public List<Object> components()
	{
		return Collections.unmodifiableList(components);
	}

	/**
	 * Iterate the components of this identifier. Modifications to the underlying components
	 * are not possible via this iterator.
	 * 
	 * @return an iterator over the components of this identifier
	 */
	public Iterator<Object> iterator()
	{
		return components().iterator();
	}

	/**
	 * Indicates the number of components making up this identifier.
	 * 
	 * @return the number of components in this identifier.
	 */
	public int size()
	{
		return components.size();
	}

	/**
	 * Check for equality between identifiers. Returns true if the identifiers
	 * contain equal components. Otherwise, returns false.
	 * 
	 * @return true if the identifiers are equivalent.
	 */
	@Override
	public boolean equals(Object that)
	{
		return (compareTo((Identifier) that) == 0);
	}

	/**
	 * Returns a hash code for this identifier.
	 * 
	 * @return an integer hashcode
	 */
	@Override
	public int hashCode()
	{
		return 1 + components.hashCode();
	}

	/**
	 * Compares this identifer to another, returning -1, 0, or 1 depending
	 * on whether this identifier is less-than, equal-to, or greater-than
	 * the other identifier, respectively.
	 * 
	 * @return -1, 0, 1 to indicate less-than, equal-to, or greater-than, respectively.
	 */
    @SuppressWarnings({
        "unchecked", "rawtypes"
    })
    @Override
    public int compareTo(Identifier that)
    {
		if (that == null) return 1;
		if (this.size() < that.size()) return -1;
		if (this.size() > that.size()) return 1;

		int i = 0;
		int result = 0;

		while (result == 0 && i < size())
		{
			Object cThis = this.components.get(i);
			Object cThat = that.components.get(i);

			if (ObjectUtils.areComparable(cThis, cThat))
			{
				result = ((Comparable) cThis).compareTo(((Comparable) cThat));
			}
			else
			{
				result = (cThis.toString().compareTo(cThat.toString()));
			}
			
			++i;
		}

	    return result;
    }

    /**
     * Returns a string representation of this identifier.
     * 
     * @return a string representation of the identifier.
     */
	@Override
	public String toString()
	{
		if (components.isEmpty()) return "";

		return (components.size() == 1 ? primaryKey().toString() : "(" + StringUtils.join(SEPARATOR, components) + ")");
	}

	/**
	 * Returns the first component of the identifier. Return null if the identifier is empty.
	 * Equivalent to components().get(0).
	 * 
	 * @return the first component or null.
	 */
	public Object primaryKey()
	{
		return (isEmpty() ? null : components.get(0));
	}

	/**
	 * Return true if the identifier has no components.
	 * 
	 * @return true if the identifier is empty.
	 */
	public boolean isEmpty()
    {
	    return components.isEmpty();
    }
}
