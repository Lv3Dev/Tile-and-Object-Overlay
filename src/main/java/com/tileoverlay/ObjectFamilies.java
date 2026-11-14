/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ObjectComposition;

@Singleton
public class ObjectFamilies
{
	static final int CHUNK = 2000;
	static final int CAP = 90000;

	/**
	 * Mounted Xeric's talisman configurations. The scene id changes with the
	 * left-click destination, and the cache does not keep them on one impostor list.
	 */
	private static final int[] MOUNTED_XERICS = {33411, 33412, 33413, 33414, 33415, 33419};
	private static final int[] MOUNTED_DIGSITE = {33416, 33417, 33418, 33420};
	private static final int[][] KNOWN = {MOUNTED_XERICS, MOUNTED_DIGSITE};

	private final Client client;
	private final Map<Integer, Integer> toBase = new HashMap<>();
	private int nextId;
	private boolean complete;

	@Inject
	ObjectFamilies(Client client)
	{
		this.client = client;
		for (int[] family : KNOWN)
		{
			link(family[0], family);
		}
	}

	ObjectFamilies()
	{
		this(null);
	}

	public boolean scanChunk()
	{
		if (complete || client == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return false;
		}
		int end = Math.min(nextId + CHUNK, CAP);
		for (int id = nextId; id < end; id++)
		{
			try
			{
				ObjectComposition definition = client.getObjectDefinition(id);
				// Empty ids reuse one definition whose id does not match the request.
				if (definition != null && definition.getId() == id)
				{
					index(id, definition.getImpostorIds());
				}
			}
			catch (RuntimeException ex)
			{
				// Some ids are unused. The next ones are still worth reading.
			}
		}
		nextId = end;
		if (nextId >= CAP)
		{
			complete = true;
		}
		return complete;
	}

	public boolean isComplete()
	{
		return complete;
	}

	public int canonical(int id)
	{
		Integer base = toBase.get(id);
		return base == null ? id : base;
	}

	public boolean same(int left, int right)
	{
		if (left < 0 || right < 0)
		{
			return false;
		}
		return left == right || canonical(left) == canonical(right);
	}

	void index(int baseId, int[] impostorIds)
	{
		if (baseId < 0 || impostorIds == null || impostorIds.length == 0)
		{
			return;
		}
		int count = 0;
		for (int child : impostorIds)
		{
			if (child >= 0)
			{
				count++;
			}
		}
		if (count == 0)
		{
			return;
		}
		int[] listed = new int[count + 1];
		listed[0] = baseId;
		int cursor = 1;
		for (int child : impostorIds)
		{
			if (child >= 0)
			{
				listed[cursor++] = child;
			}
		}
		link(baseId, listed);
		if (crossesFamilies(listed))
		{
			for (int[] family : KNOWN)
			{
				if (intersects(family, listed))
				{
					link(family[0], family);
				}
			}
			return;
		}
		for (int[] family : KNOWN)
		{
			if (intersects(family, listed))
			{
				link(baseId, family);
			}
		}
	}

	/** One impostor list must not join the Xeric's mount to the digsite mount. */
	private static boolean crossesFamilies(int[] ids)
	{
		int hits = 0;
		for (int[] family : KNOWN)
		{
			if (intersects(family, ids))
			{
				hits++;
			}
		}
		return hits > 1;
	}

	private static boolean intersects(int[] family, int[] ids)
	{
		for (int id : ids)
		{
			for (int member : family)
			{
				if (id == member)
				{
					return true;
				}
			}
		}
		return false;
	}

	private void link(int baseId, int[] ids)
	{
		if (baseId < 0 || ids == null)
		{
			return;
		}
		toBase.put(baseId, baseId);
		for (int id : ids)
		{
			if (id >= 0)
			{
				toBase.put(id, baseId);
			}
		}
	}
}
