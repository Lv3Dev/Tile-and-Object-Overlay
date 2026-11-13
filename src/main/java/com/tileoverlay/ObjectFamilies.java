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

/**
 * Transformed decorations, such as a mounted Xeric's talisman, keep a different
 * id for each configuration. The base object lists those ids. One mark on the
 * base covers every configuration.
 */
@Singleton
public class ObjectFamilies
{
	static final int CHUNK = 2000;
	static final int CAP = 90000;

	private final Client client;
	private final Map<Integer, Integer> toBase = new HashMap<>();
	private int nextId;
	private boolean complete;

	@Inject
	ObjectFamilies(Client client)
	{
		this.client = client;
	}

	ObjectFamilies()
	{
		this(null);
	}

	/**
	 * Reads the next slice of object definitions. Safe to call every game tick.
	 * Returns true on the tick the index finishes.
	 */
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
		if (left == right)
		{
			return true;
		}
		Integer leftBase = toBase.get(left);
		Integer rightBase = toBase.get(right);
		return leftBase != null && leftBase.equals(rightBase);
	}

	void index(int baseId, int[] impostorIds)
	{
		if (baseId < 0 || impostorIds == null || impostorIds.length == 0)
		{
			return;
		}
		boolean any = false;
		for (int child : impostorIds)
		{
			if (child >= 0)
			{
				any = true;
				break;
			}
		}
		if (!any)
		{
			return;
		}
		toBase.put(baseId, baseId);
		for (int child : impostorIds)
		{
			if (child >= 0)
			{
				toBase.put(child, baseId);
			}
		}
	}
}
