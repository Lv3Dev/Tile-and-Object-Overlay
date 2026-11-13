/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Version 1 is tiles only. A group with no kind is a tile group.
 * Version 2 adds object markers.
 * Version 3 stores each object's outline, hull, tile, and clickbox flags.
 * Older files still load. Their objects keep the outline on.
 */
@Data
public class MarkerFile
{
	public static final int HIGHLIGHT_FLAGS_VERSION = 3;
	public static final int CURRENT_VERSION = HIGHLIGHT_FLAGS_VERSION;

	private int version = CURRENT_VERSION;
	private List<MarkerGroup> groups = new ArrayList<>();
	private List<TileMarker> markers = new ArrayList<>();
	private List<ObjectMarker> objects = new ArrayList<>();

	public static MarkerFile empty()
	{
		return new MarkerFile();
	}
}
