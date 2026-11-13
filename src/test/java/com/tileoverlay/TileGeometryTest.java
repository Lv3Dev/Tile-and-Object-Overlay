/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public class TileGeometryTest
{
	@Test
	public void rectangleIsInclusive()
	{
		List<TilePoint> tiles = TileGeometry.area(1, 1, 2, 3, 0, false);
		Assert.assertNotNull(tiles);
		Assert.assertEquals(6, tiles.size());
		Assert.assertTrue(tiles.contains(new TilePoint(1, 1, 0)));
		Assert.assertTrue(tiles.contains(new TilePoint(2, 3, 0)));
	}

	@Test
	public void borderOfThreeByThreeIsThePerimeter()
	{
		List<TilePoint> tiles = TileGeometry.area(0, 0, 2, 2, 1, true);
		Assert.assertNotNull(tiles);
		Assert.assertEquals(8, tiles.size());
		Assert.assertFalse(tiles.contains(new TilePoint(1, 1, 1)));
	}

	@Test
	public void areaRejectsASideLongerThanTheCap()
	{
		Assert.assertNull(TileGeometry.area(0, 0, TileGeometry.MAX_SPAN, 0, 0, false));
		List<TilePoint> allowed = TileGeometry.area(0, 0, TileGeometry.MAX_SPAN - 1, 0, 0, false);
		Assert.assertNotNull(allowed);
		Assert.assertEquals(TileGeometry.MAX_SPAN, allowed.size());
	}

	@Test
	public void southWestTileOfATwoByTwoOnlyHasOuterSouthAndWest()
	{
		Set<Long> occupied = new HashSet<>();
		occupied.add(TileGeometry.pack(0, 0, 0));
		occupied.add(TileGeometry.pack(1, 0, 0));
		occupied.add(TileGeometry.pack(0, 1, 0));
		occupied.add(TileGeometry.pack(1, 1, 0));

		Assert.assertTrue(TileGeometry.isOuter(occupied, 0, 0, 0, TileGeometry.Edge.SOUTH));
		Assert.assertTrue(TileGeometry.isOuter(occupied, 0, 0, 0, TileGeometry.Edge.WEST));
		Assert.assertFalse(TileGeometry.isOuter(occupied, 0, 0, 0, TileGeometry.Edge.NORTH));
		Assert.assertFalse(TileGeometry.isOuter(occupied, 0, 0, 0, TileGeometry.Edge.EAST));
	}

	@Test
	public void straightJoinsAreNotTurns()
	{
		Assert.assertFalse(TileGeometry.turns(true, true, false, false));
		Assert.assertFalse(TileGeometry.turns(false, false, true, true));
		Assert.assertFalse(TileGeometry.turns(true, false, true, false));
		Assert.assertFalse(TileGeometry.turns(false, true, false, true));
		Assert.assertTrue(TileGeometry.turns(true, false, false, true));
		Assert.assertTrue(TileGeometry.turns(false, true, true, false));
		Assert.assertTrue(TileGeometry.turns(true, false, false, false));
		Assert.assertTrue(TileGeometry.turns(true, true, true, false));
		Assert.assertFalse(TileGeometry.turns(false, false, false, false));
		Assert.assertFalse(TileGeometry.turns(true, true, true, true));
	}
}
