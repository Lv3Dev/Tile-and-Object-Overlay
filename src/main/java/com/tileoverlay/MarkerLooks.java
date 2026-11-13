/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay;

import java.awt.Color;

/**
 * Style colours live here, not on {@link TileOverlayConfig}.
 * RuneLite proxies every config method. A method without {@code @ConfigItem}
 * logs a warning and returns null, which throws on every frame once a marker exists.
 */
final class MarkerLooks
{
	private MarkerLooks()
	{
	}

	static Color border(TileOverlayConfig config, MarkerStyle style)
	{
		switch (style)
		{
			case REGION:
				return config.regionBorderColour();
			case BORDER:
				return config.borderStyleBorderColour();
			case TILE:
			default:
				return config.tileBorderColour();
		}
	}

	static Color fill(TileOverlayConfig config, MarkerStyle style)
	{
		switch (style)
		{
			case REGION:
				return config.regionFillColour();
			case BORDER:
				return config.borderStyleFillColour();
			case TILE:
			default:
				return config.tileFillColour();
		}
	}

	static int fillOpacity(TileOverlayConfig config, MarkerStyle style)
	{
		switch (style)
		{
			case REGION:
				return config.regionFillOpacity();
			case BORDER:
				return config.borderStyleFillOpacity();
			case TILE:
			default:
				return config.tileFillOpacity();
		}
	}

	static boolean cornersOnly(TileOverlayConfig config, MarkerStyle style)
	{
		switch (style)
		{
			case REGION:
				return config.regionCornersOnly();
			case BORDER:
				return config.borderStyleCornersOnly();
			case TILE:
			default:
				return config.tileCornersOnly();
		}
	}

	static int cornerSize(TileOverlayConfig config, MarkerStyle style)
	{
		switch (style)
		{
			case REGION:
				return config.regionCornerSize();
			case BORDER:
				return config.borderStyleCornerSize();
			case TILE:
			default:
				return config.tileCornerSize();
		}
	}

	static boolean merge(TileOverlayConfig config, MarkerStyle style)
	{
		switch (style)
		{
			case REGION:
				return config.regionMergeBorders();
			case BORDER:
				return config.borderMergeBorders();
			case TILE:
			default:
				return config.tileMergeBorders();
		}
	}
}
