/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay;

import java.awt.Color;
import org.junit.Assert;
import org.junit.Test;

public class MarkerLooksTest
{
	@Test
	public void eachMarkerStyleUsesItsOwnColours()
	{
		TileOverlayConfig config = config();
		Assert.assertEquals(new Color(1, 0, 0), MarkerLooks.border(config, MarkerStyle.TILE));
		Assert.assertEquals(new Color(0, 1, 0), MarkerLooks.border(config, MarkerStyle.REGION));
		Assert.assertEquals(new Color(0, 0, 1), MarkerLooks.border(config, MarkerStyle.BORDER));
		Assert.assertEquals(10, MarkerLooks.fillOpacity(config, MarkerStyle.TILE));
		Assert.assertEquals(20, MarkerLooks.fillOpacity(config, MarkerStyle.REGION));
		Assert.assertEquals(30, MarkerLooks.fillOpacity(config, MarkerStyle.BORDER));
		Assert.assertFalse(MarkerLooks.cornersOnly(config, MarkerStyle.TILE));
		Assert.assertTrue(MarkerLooks.cornersOnly(config, MarkerStyle.REGION));
		Assert.assertEquals(22, MarkerLooks.cornerSize(config, MarkerStyle.TILE));
		Assert.assertEquals(30, MarkerLooks.cornerSize(config, MarkerStyle.BORDER));
		Assert.assertTrue(MarkerLooks.merge(config, MarkerStyle.TILE));
		Assert.assertFalse(MarkerLooks.merge(config, MarkerStyle.REGION));
		Assert.assertTrue(MarkerLooks.merge(config, MarkerStyle.BORDER));
	}

	@Test
	public void markerOverrideReplacesTheStyleSetting()
	{
		TileOverlayConfig config = config();
		Assert.assertFalse(MarkerLooks.cornersOnly(config, MarkerStyle.TILE, null));
		Assert.assertTrue(MarkerLooks.cornersOnly(config, MarkerStyle.TILE, Boolean.TRUE));
		Assert.assertFalse(MarkerLooks.cornersOnly(config, MarkerStyle.REGION, Boolean.FALSE));
		Assert.assertFalse(MarkerLooks.merge(config, MarkerStyle.REGION, null));
		Assert.assertTrue(MarkerLooks.merge(config, MarkerStyle.REGION, Boolean.TRUE));
		Assert.assertFalse(MarkerLooks.merge(config, MarkerStyle.TILE, Boolean.FALSE));
		Assert.assertTrue(MarkerLooks.merge(config, MarkerStyle.BORDER, null));
	}

	@Test
	public void tileColourUsesTheTileThenTheGroupThenTheStyle()
	{
		TileOverlayConfig config = config();
		Assert.assertEquals(new Color(1, 0, 0), MarkerLooks.tileBorder(config, MarkerStyle.TILE, null, null));
		Assert.assertEquals(new Color(9, 8, 7), MarkerLooks.tileBorder(config, MarkerStyle.TILE, null, 0xAA090807));
		Assert.assertEquals(new Color(4, 5, 6, 7), MarkerLooks.tileBorder(config, MarkerStyle.REGION, 0x07040506, 0xFF010203));

		Assert.assertEquals(new Color(9, 8, 7, 10), MarkerLooks.tileFill(config, MarkerStyle.TILE, null, 0xFF090807));
		Assert.assertEquals(new Color(9, 8, 7, 40), MarkerLooks.tileFill(config, MarkerStyle.TILE, null, 0x28090807));
		Assert.assertEquals(new Color(1, 2, 3, 4), MarkerLooks.tileFill(config, MarkerStyle.BORDER, 0x04010203, 0xFF090807));
		Assert.assertEquals(new Color(5, 6, 7, 8), MarkerLooks.tileFill(config, MarkerStyle.TILE, null, 0xFF090807, 0x08050607));
		Assert.assertEquals(new Color(1, 2, 3, 4), MarkerLooks.tileFill(config, MarkerStyle.TILE, 0x04010203, 0xFF090807, 0x08050607));
		Assert.assertEquals(new Color(3, 2, 1), MarkerLooks.tileLabel(config, 0xFF030201));
		Assert.assertNull(MarkerLooks.tileLabel(config, null));
	}

	private static TileOverlayConfig config()
	{
		return new TileOverlayConfig()
		{
			@Override
			public Color tileBorderColour()
			{
				return new Color(1, 0, 0);
			}

			@Override
			public Color regionBorderColour()
			{
				return new Color(0, 1, 0);
			}

			@Override
			public Color borderStyleBorderColour()
			{
				return new Color(0, 0, 1);
			}

			@Override
			public int tileFillOpacity()
			{
				return 10;
			}

			@Override
			public int regionFillOpacity()
			{
				return 20;
			}

			@Override
			public int borderStyleFillOpacity()
			{
				return 30;
			}

			@Override
			public boolean regionCornersOnly()
			{
				return true;
			}

			@Override
			public int borderStyleCornerSize()
			{
				return 30;
			}

			@Override
			public boolean regionMergeBorders()
			{
				return false;
			}

			@Override
			public Color labelTextColour()
			{
				return null;
			}
		};
	}
}
