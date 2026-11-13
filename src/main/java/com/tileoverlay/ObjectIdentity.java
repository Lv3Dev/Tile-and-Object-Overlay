/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay;

/**
 * Menu actions and scene objects do not always carry the same id.
 * A game-object option uses the base id. Examine, and some wall decorations,
 * use a transformed id from the object's impostor list.
 */
final class ObjectIdentity
{
	private ObjectIdentity()
	{
	}

	static boolean same(int sceneId, int otherId, int[] sceneImpostors, int[] otherImpostors)
	{
		if (sceneId < 0 || otherId < 0)
		{
			return false;
		}
		if (sceneId == otherId)
		{
			return true;
		}
		return contains(sceneImpostors, otherId) || contains(otherImpostors, sceneId);
	}

	private static boolean contains(int[] ids, int id)
	{
		if (ids == null)
		{
			return false;
		}
		for (int candidate : ids)
		{
			if (candidate == id)
			{
				return true;
			}
		}
		return false;
	}
}
