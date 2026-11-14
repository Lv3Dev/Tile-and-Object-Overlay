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
 * Version 4 stores optional tile colours and a tile group's border and fill.
 * Corner, merge, and colour overrides are optional. A missing value follows the setting.
 * Older files still load. Their objects keep the outline on.
 */
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
