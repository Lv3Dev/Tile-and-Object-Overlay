/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay;

import lombok.Value;
import net.runelite.api.coords.WorldPoint;

@Value
public class ObjectRef
{
	int objectId;
	int regionId;
	int regionX;
	int regionY;
	int plane;

	public static ObjectRef of(int objectId, WorldPoint worldPoint)
	{
		MarkerLocation location = MarkerLocation.fromWorld(worldPoint);
		return new ObjectRef(
			objectId,
			location.getRegionId(),
			location.getRegionX(),
			location.getRegionY(),
			location.getPlane());
	}

	public MarkerLocation location()
	{
		return new MarkerLocation(regionId, regionX, regionY, plane);
	}

	public WorldPoint toWorldPoint()
	{
		return WorldPoint.fromRegion(regionId, regionX, regionY, plane);
	}
}
