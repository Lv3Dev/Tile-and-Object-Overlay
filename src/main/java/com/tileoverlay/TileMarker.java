/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TileMarker
{
	private int regionId;
	private int regionX;
	private int regionY;
	private int plane;
	private MarkerStyle style;
	private String label;
	private String groupId;
	/** Null follows that style's Corners setting. */
	private Boolean cornersOnly;
	/** Null follows that style's Merge setting. */
	private Boolean mergeBorders;
	/**
	 * Packed ARGB. Null uses the tile group's colour when it has one,
	 * and otherwise the style's border colour.
	 */
	private Integer borderColour;
	/** Packed ARGB. Null uses the group colour, then the style fill. */
	private Integer fillColour;
	/** Packed ARGB. Null uses the label text colour setting. */
	private Integer labelColour;

	public MarkerLocation location()
	{
		return new MarkerLocation(regionId, regionX, regionY, plane);
	}

	public TileMarker copy()
	{
		return new TileMarker(
			regionId, regionX, regionY, plane, style, label, groupId,
			cornersOnly, mergeBorders, borderColour, fillColour, labelColour);
	}

	public static TileMarker create(MarkerLocation location, MarkerStyle style)
	{
		return new TileMarker(
			location.getRegionId(),
			location.getRegionX(),
			location.getRegionY(),
			location.getPlane(),
			style,
			null,
			null,
			null,
			null,
			null,
			null,
			null);
	}
}
