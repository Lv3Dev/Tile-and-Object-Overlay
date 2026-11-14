/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class MarkerFile
{
	public static final int HIGHLIGHT_FLAGS_VERSION = 3;
	public static final int CURRENT_VERSION = 4;

	private int version = CURRENT_VERSION;
	private List<MarkerGroup> groups = new ArrayList<>();
	private List<TileMarker> markers = new ArrayList<>();
	private List<ObjectMarker> objects = new ArrayList<>();

	public static MarkerFile empty()
	{
		return new MarkerFile();
	}
}
