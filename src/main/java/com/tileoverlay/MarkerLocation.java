/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay;

import lombok.Value;
import net.runelite.api.coords.WorldPoint;

@Value
public class MarkerLocation
{
	int regionId;
	int regionX;
	int regionY;
	int plane;

	public static MarkerLocation fromWorld(WorldPoint worldPoint)
	{
		return new MarkerLocation(
			worldPoint.getRegionID(),
			worldPoint.getRegionX(),
			worldPoint.getRegionY(),
			worldPoint.getPlane());
	}

	public WorldPoint toWorldPoint()
	{
		return WorldPoint.fromRegion(regionId, regionX, regionY, plane);
	}

	public TilePoint toTilePoint()
	{
		WorldPoint worldPoint = toWorldPoint();
		return new TilePoint(worldPoint.getX(), worldPoint.getY(), worldPoint.getPlane());
	}
}
