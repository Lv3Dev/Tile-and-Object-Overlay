/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Rectangle, perimeter, and merged-outline math for ground markers.
 * World X grows east and world Y grows north.
 */
public final class TileGeometry
{
	public static final int MAX_SPAN = 64;

	public enum Edge
	{
		SOUTH,
		EAST,
		NORTH,
		WEST
	}

	public enum Corner
	{
		SW,
		SE,
		NE,
		NW
	}

	private TileGeometry()
	{
	}

	public static long pack(int x, int y, int plane)
	{
		return ((long) (x & 0x1FFFFF) << 42)
			| ((long) (y & 0x1FFFFF) << 21)
			| (plane & 0xF);
	}

	/**
	 * Inclusive rectangle from either corner. Returns null when either side is longer than {@link #MAX_SPAN}.
	 * A border mark is only the perimeter.
	 */
	public static List<TilePoint> area(int x1, int y1, int x2, int y2, int plane, boolean perimeterOnly)
	{
		int minX = Math.min(x1, x2);
		int maxX = Math.max(x1, x2);
		int minY = Math.min(y1, y2);
		int maxY = Math.max(y1, y2);
		int width = maxX - minX + 1;
		int height = maxY - minY + 1;
		if (width > MAX_SPAN || height > MAX_SPAN)
		{
			return null;
		}

		List<TilePoint> tiles = new ArrayList<>(width * height);
		for (int x = minX; x <= maxX; x++)
		{
			for (int y = minY; y <= maxY; y++)
			{
				boolean perimeter = x == minX || x == maxX || y == minY || y == maxY;
				if (!perimeterOnly || perimeter)
				{
					tiles.add(new TilePoint(x, y, plane));
				}
			}
		}
		return tiles;
	}

	public static boolean isOuter(Set<Long> occupied, int x, int y, int plane, Edge edge)
	{
		int nx = x;
		int ny = y;
		switch (edge)
		{
			case SOUTH:
				ny--;
				break;
			case EAST:
				nx++;
				break;
			case NORTH:
				ny++;
				break;
			case WEST:
				nx--;
				break;
		}
		return !occupied.contains(pack(nx, ny, plane));
	}

	/**
	 * True when the outline turns at this corner of the tile. Straight joins between
	 * neighbouring marked tiles are not corners, so a merged row does not grow ticks
	 * along the middle of a side.
	 */
	public static boolean isTurn(Set<Long> occupied, int tx, int ty, int plane, Corner corner)
	{
		final boolean nw;
		final boolean ne;
		final boolean sw;
		final boolean se;
		switch (corner)
		{
			case SW:
				ne = has(occupied, tx, ty, plane);
				nw = has(occupied, tx - 1, ty, plane);
				se = has(occupied, tx, ty - 1, plane);
				sw = has(occupied, tx - 1, ty - 1, plane);
				break;
			case SE:
				nw = has(occupied, tx, ty, plane);
				ne = has(occupied, tx + 1, ty, plane);
				sw = has(occupied, tx, ty - 1, plane);
				se = has(occupied, tx + 1, ty - 1, plane);
				break;
			case NE:
				sw = has(occupied, tx, ty, plane);
				se = has(occupied, tx + 1, ty, plane);
				nw = has(occupied, tx, ty + 1, plane);
				ne = has(occupied, tx + 1, ty + 1, plane);
				break;
			case NW:
				se = has(occupied, tx, ty, plane);
				sw = has(occupied, tx - 1, ty, plane);
				ne = has(occupied, tx, ty + 1, plane);
				nw = has(occupied, tx - 1, ty + 1, plane);
				break;
			default:
				return false;
		}
		return turns(nw, ne, sw, se);
	}

	static boolean turns(boolean nw, boolean ne, boolean sw, boolean se)
	{
		int n = (nw ? 1 : 0) + (ne ? 1 : 0) + (sw ? 1 : 0) + (se ? 1 : 0);
		if (n == 0 || n == 4)
		{
			return false;
		}
		if (n == 1 || n == 3)
		{
			return true;
		}
		boolean horizontal = (nw && ne && !sw && !se) || (sw && se && !nw && !ne);
		boolean vertical = (nw && sw && !ne && !se) || (ne && se && !nw && !sw);
		return !horizontal && !vertical;
	}

	private static boolean has(Set<Long> occupied, int x, int y, int plane)
	{
		return occupied.contains(pack(x, y, plane));
	}
}
