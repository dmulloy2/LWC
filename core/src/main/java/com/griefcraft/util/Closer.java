/**
 * SwornAPI - common API for MineSworn and Shadowvolt plugins
 * Copyright (C) 2015 dmulloy2
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.griefcraft.util;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.Validate;

/**
 * Automatically closes a collection of closeable objects.
 * 
 * @author dmulloy2
 */

public class Closer implements Closeable
{
	private final List<AutoCloseable> list = new ArrayList<>();

	/**
	 * Registers a closeable object.
	 *
	 * @param closeable Object to register
	 * @return The object
	 */
	public final <C extends AutoCloseable> C register(C closeable)
	{
		Validate.notNull(closeable, "closeable cannot be null!");

		list.add(closeable);
		return closeable;
	}

	@Override
	public final void close()
	{
		if (list.isEmpty())
			return;

		Iterator<AutoCloseable> iter = list.iterator();
		while (iter.hasNext())
		{
			closeQuietly(iter.next());
			iter.remove();
		}
	}

	/**
	 * Quietly closes a closeable object, ignoring all exceptions.
	 * 
	 * @param closeable Object to close
	 */
	public static void closeQuietly(AutoCloseable closeable)
	{
		try
		{
			if (closeable != null)
				closeable.close();
		} catch (Throwable ex) { }
	}
}
