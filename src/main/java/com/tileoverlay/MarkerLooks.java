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
public final class MarkerLooks
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

	/** {@code override} null uses the style setting. */
	public static boolean cornersOnly(TileOverlayConfig config, MarkerStyle style, Boolean override)
	{
		if (override != null)
		{
			return override;
		}
		return cornersOnly(config, style == null ? MarkerStyle.TILE : style);
	}

	/** {@code override} null uses the style setting. */
	public static boolean merge(TileOverlayConfig config, MarkerStyle style, Boolean override)
	{
		if (override != null)
		{
			return override;
		}
		return merge(config, style == null ? MarkerStyle.TILE : style);
	}

	/**
	 * Tile border. The tile colour wins, then the group colour as a solid border,
	 * then that style's border colour.
	 */
	public static Color tileBorder(TileOverlayConfig config, MarkerStyle style, Integer tileColour, Integer groupColour)
	{
		if (tileColour != null)
		{
			return new Color(tileColour, true);
		}
		if (groupColour != null)
		{
			Color colour = new Color(groupColour, true);
			return new Color(colour.getRed(), colour.getGreen(), colour.getBlue());
		}
		return border(config, style);
	}

	/**
	 * Tile fill. The tile colour wins, then the group's fill, then a group
	 * colour's transparency. A fully solid group colour keeps that style's fill strength.
	 */
	public static Color tileFill(TileOverlayConfig config, MarkerStyle style, Integer tileColour, Integer groupColour)
	{
		return tileFill(config, style, tileColour, groupColour, null);
	}

	public static Color tileFill(TileOverlayConfig config, MarkerStyle style, Integer tileColour, Integer groupColour, Integer groupFill)
	{
		if (tileColour != null)
		{
			return new Color(tileColour, true);
		}
		if (groupFill != null)
		{
			return new Color(groupFill, true);
		}
		if (groupColour != null)
		{
			Color colour = new Color(groupColour, true);
			int alpha = colour.getAlpha();
			if (alpha >= 255)
			{
				alpha = fillOpacity(config, style);
			}
			return new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), Math.max(0, Math.min(255, alpha)));
		}
		return withOpacity(fill(config, style), fillOpacity(config, style));
	}

	/** Label text. Null uses the Ground Markers label colour. */
	public static Color tileLabel(TileOverlayConfig config, Integer tileColour)
	{
		if (tileColour != null)
		{
			return new Color(tileColour, true);
		}
		return config.labelTextColour();
	}

	private static Color withOpacity(Color colour, int opacity)
	{
		if (colour == null)
		{
			return null;
		}
		int alpha = Math.max(0, Math.min(255, opacity));
		return new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), alpha);
	}
}
