/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.EntityOps;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.ObjectComposition;
import net.runelite.api.TileObject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.util.Text;

@Singleton
class ObjectLeftClick
{
	static final String SWAP_GROUP = "menuentryswapper";
	static final String SWAP_PREFIX = "object_";
	private static final String SHIFT_PREFIX = "object_shift_";
	private static final String WALK_HERE = "Walk here";

	private final Client client;
	private final ConfigManager configManager;
	private final Map<Integer, String> seen = new ConcurrentHashMap<>();
	private final Map<Integer, String> swaps = new ConcurrentHashMap<>();
	private final Set<Integer> noSwap = ConcurrentHashMap.newKeySet();

	@Inject
	ObjectLeftClick(Client client, ConfigManager configManager)
	{
		this.client = client;
		this.configManager = configManager;
	}

	/**
	 * Remember the left-click option currently built for each object in the menu.
	 * Call this after menu entry swapper and other plugins have reordered entries.
	 * Shift is ignored so a shift-click option does not replace the normal one.
	 */
	void observe(MenuEntry[] entries)
	{
		if (entries == null || entries.length == 0)
		{
			return;
		}
		List<Hit> hits = new ArrayList<>(entries.length);
		for (MenuEntry entry : entries)
		{
			if (entry == null)
			{
				hits.add(Hit.skip());
				continue;
			}
			MenuAction type = entry.getType();
			if (type == MenuAction.WALK)
			{
				hits.add(Hit.walk(entry.getParam0(), entry.getParam1()));
				continue;
			}
			if (!isObjectOption(type))
			{
				hits.add(Hit.skip());
				continue;
			}
			int rawId = entry.getIdentifier();
			int altId = -1;
			int objectId = -1;
			// A normal object option's identifier is the id. A cloned sub-option
			// keeps the submenu identifier, whose high bits are the sub-index, so
			// it must not be looked up as its own object. The option text is kept
			// and later attached to the real object on the same tile.
			if ((rawId >>> 16) == 0)
			{
				ObjectComposition base = definition(rawId);
				if (base != null && clean(base.getName()) != null)
				{
					objectId = rawId;
					ObjectComposition resolved = resolve(base);
					if (resolved != null && resolved.getId() != rawId)
					{
						altId = resolved.getId();
					}
				}
			}
			hits.add(Hit.object(entry.getParam0(), entry.getParam1(), objectId, altId, clean(entry.getOption())));
		}
		seen.putAll(choose(hits));
	}

	String option(TileObject object, ObjectComposition resolved)
	{
		if (object == null)
		{
			return null;
		}
		int sceneId = object.getId();
		int resolvedId = resolved == null ? sceneId : resolved.getId();
		String observed = seen.get(sceneId);
		if (observed == null && resolvedId != sceneId)
		{
			observed = seen.get(resolvedId);
		}
		if (observed != null)
		{
			return observed;
		}
		String swapped = swapName(resolvedId, resolved);
		if (swapped == null && resolvedId != sceneId)
		{
			swapped = swapName(sceneId, resolved);
		}
		if (swapped != null)
		{
			return swapped;
		}
		return firstOption(topOps(resolved));
	}

	/**
	 * Drop remembered options after a custom left-click changes, so the new
	 * config is used until the menu is built again.
	 */
	void forgetSwap(String key)
	{
		if (key == null || key.startsWith(SHIFT_PREFIX) || !key.startsWith(SWAP_PREFIX))
		{
			return;
		}
		swaps.clear();
		noSwap.clear();
		seen.clear();
	}

	private String swapName(int objectId, ObjectComposition resolved)
	{
		if (objectId < 0)
		{
			return null;
		}
		String cached = swaps.get(objectId);
		if (cached != null)
		{
			return cached;
		}
		if (noSwap.contains(objectId))
		{
			return null;
		}
		String raw;
		try
		{
			raw = configManager.getConfiguration(SWAP_GROUP, SWAP_PREFIX + objectId);
		}
		catch (RuntimeException ex)
		{
			noSwap.add(objectId);
			return null;
		}
		if (raw == null || raw.isEmpty())
		{
			noSwap.add(objectId);
			return null;
		}
		int index;
		try
		{
			index = Integer.parseInt(raw.trim());
		}
		catch (NumberFormatException ex)
		{
			noSwap.add(objectId);
			return null;
		}
		String name = nameForIndex(topOps(resolved), subOps(resolved), index);
		if (name == null)
		{
			noSwap.add(objectId);
			return null;
		}
		swaps.put(objectId, name);
		return name;
	}

	private ObjectComposition definition(int objectId)
	{
		try
		{
			return client.getObjectDefinition(objectId);
		}
		catch (RuntimeException ex)
		{
			return null;
		}
	}

	private static ObjectComposition resolve(ObjectComposition definition)
	{
		try
		{
			if (definition.getImpostorIds() != null)
			{
				ObjectComposition impostor = definition.getImpostor();
				if (impostor != null)
				{
					return impostor;
				}
			}
		}
		catch (RuntimeException ex)
		{
			return definition;
		}
		return definition;
	}

	private static boolean isObjectOption(MenuAction type)
	{
		return type == MenuAction.GAME_OBJECT_FIRST_OPTION
			|| type == MenuAction.GAME_OBJECT_SECOND_OPTION
			|| type == MenuAction.GAME_OBJECT_THIRD_OPTION
			|| type == MenuAction.GAME_OBJECT_FOURTH_OPTION
			|| type == MenuAction.GAME_OBJECT_FIFTH_OPTION;
	}

	static String nameForIndex(String[] ops, String[][] subs, int index)
	{
		if (index == -1)
		{
			return WALK_HERE;
		}
		if (index < 0)
		{
			return null;
		}
		if ((index >>> 8) == 0)
		{
			return opAt(ops, index);
		}
		int opIndex = index & 0xFF;
		int subIndex = (index >>> 8) - 1;
		if (subs == null || opIndex >= subs.length)
		{
			return null;
		}
		return opAt(subs[opIndex], subIndex);
	}

	static String firstOption(String[] ops)
	{
		if (ops == null)
		{
			return null;
		}
		for (String op : ops)
		{
			String cleaned = clean(op);
			if (cleaned != null)
			{
				return cleaned;
			}
		}
		return null;
	}

	/**
	 * Highest object option in the menu. Entries are bottom to top, so the
	 * last option for an object is its left-click. Walk here wins only when
	 * it sits above that object's own options.
	 */
	static Map<Integer, String> choose(List<Hit> hits)
	{
		Map<Integer, String> chosen = new HashMap<>();
		if (hits == null || hits.isEmpty())
		{
			return chosen;
		}
		int walkAt = -1;
		Map<Long, Group> groups = new HashMap<>();
		for (int i = 0; i < hits.size(); i++)
		{
			Hit hit = hits.get(i);
			if (hit == null)
			{
				continue;
			}
			if (hit.walk)
			{
				walkAt = i;
				continue;
			}
			if (!hit.objectAction)
			{
				continue;
			}
			long key = tileKey(hit.sceneX, hit.sceneY);
			Group group = groups.get(key);
			if (group == null)
			{
				group = new Group();
				groups.put(key, group);
			}
			if (hit.objectId >= 0 && i >= group.idAt)
			{
				group.id = hit.objectId;
				group.altId = hit.altId;
				group.idAt = i;
			}
			if (hit.option != null && i >= group.best)
			{
				group.best = i;
				group.option = hit.option;
			}
		}
		for (Group group : groups.values())
		{
			if (group.id < 0 || group.option == null)
			{
				continue;
			}
			String option = walkAt > group.best ? WALK_HERE : group.option;
			chosen.put(group.id, option);
			if (group.altId >= 0)
			{
				chosen.put(group.altId, option);
			}
		}
		return chosen;
	}

	private static long tileKey(int sceneX, int sceneY)
	{
		return ((long) sceneX << 32) | (sceneY & 0xFFFFFFFFL);
	}

	private static String[] topOps(ObjectComposition definition)
	{
		if (definition == null)
		{
			return null;
		}
		String[] fromOps = readOps(definition.getOps());
		if (firstOption(fromOps) != null)
		{
			return fromOps;
		}
		return definition.getActions();
	}

	private static String[] readOps(EntityOps ops)
	{
		if (ops == null)
		{
			return null;
		}
		String[] actions = new String[EntityOps.MAX_OPS];
		int count = Math.min(EntityOps.MAX_OPS, Math.max(0, ops.getNumOps()));
		for (int i = 0; i < count; i++)
		{
			try
			{
				actions[i] = ops.getOp(i);
			}
			catch (RuntimeException ex)
			{
				break;
			}
		}
		return actions;
	}

	private static String[][] subOps(ObjectComposition definition)
	{
		if (definition == null)
		{
			return null;
		}
		EntityOps ops = definition.getOps();
		if (ops == null)
		{
			return null;
		}
		int count = Math.min(EntityOps.MAX_OPS, Math.max(0, ops.getNumOps()));
		String[][] subs = new String[EntityOps.MAX_OPS][];
		for (int i = 0; i < count; i++)
		{
			EntityOps sub;
			try
			{
				sub = ops.getSubOps(i);
			}
			catch (RuntimeException ex)
			{
				continue;
			}
			if (sub == null || sub.getNumOps() <= 0)
			{
				continue;
			}
			int n = sub.getNumOps();
			String[] names = new String[n];
			for (int s = 0; s < n; s++)
			{
				try
				{
					names[s] = sub.getOp(s);
				}
				catch (RuntimeException ex)
				{
					break;
				}
			}
			subs[i] = names;
		}
		return subs;
	}

	private static String opAt(String[] ops, int index)
	{
		if (ops == null || index < 0 || index >= ops.length)
		{
			return null;
		}
		return clean(ops[index]);
	}

	private static String clean(String value)
	{
		if (value == null)
		{
			return null;
		}
		String stripped = Text.removeTags(value);
		if (stripped == null)
		{
			return null;
		}
		String trimmed = stripped.trim();
		if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed))
		{
			return null;
		}
		return trimmed;
	}

	static final class Hit
	{
		final int sceneX;
		final int sceneY;
		final int objectId;
		final int altId;
		final String option;
		final boolean walk;
		final boolean objectAction;

		private Hit(int sceneX, int sceneY, int objectId, int altId, String option, boolean walk, boolean objectAction)
		{
			this.sceneX = sceneX;
			this.sceneY = sceneY;
			this.objectId = objectId;
			this.altId = altId;
			this.option = option;
			this.walk = walk;
			this.objectAction = objectAction;
		}

		static Hit walk(int sceneX, int sceneY)
		{
			return new Hit(sceneX, sceneY, -1, -1, null, true, false);
		}

		static Hit object(int sceneX, int sceneY, int objectId, int altId, String option)
		{
			return new Hit(sceneX, sceneY, objectId, altId, option, false, true);
		}

		static Hit skip()
		{
			return new Hit(0, 0, -1, -1, null, false, false);
		}
	}

	private static final class Group
	{
		private int best = -1;
		private int idAt = -1;
		private int id = -1;
		private int altId = -1;
		private String option;
	}
}
