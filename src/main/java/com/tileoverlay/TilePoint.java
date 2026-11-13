/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay;

import java.util.Objects;

/**
 * A world-coordinate tile. Used by geometry that does not need a RuneLite world point.
 */
public final class TilePoint
{
	private final int x;
	private final int y;
	private final int plane;

	public TilePoint(int x, int y, int plane)
	{
		this.x = x;
		this.y = y;
		this.plane = plane;
	}

	public int getX()
	{
		return x;
	}

	public int getY()
	{
		return y;
	}

	public int getPlane()
	{
		return plane;
	}

	public long pack()
	{
		return TileGeometry.pack(x, y, plane);
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		if (!(other instanceof TilePoint))
		{
			return false;
		}
		TilePoint tilePoint = (TilePoint) other;
		return x == tilePoint.x && y == tilePoint.y && plane == tilePoint.plane;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(x, y, plane);
	}
}
