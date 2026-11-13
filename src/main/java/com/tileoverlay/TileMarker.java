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

	public MarkerLocation location()
	{
		return new MarkerLocation(regionId, regionX, regionY, plane);
	}

	public TileMarker copy()
	{
		return new TileMarker(regionId, regionX, regionY, plane, style, label, groupId);
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
			null);
	}
}
