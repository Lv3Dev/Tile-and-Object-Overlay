/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay;

/**
 * A colour a tile can override. Null on the marker means the style setting,
 * or the tile group's colour when that group has one.
 */
public enum TileColourSlot
{
	/** The tile border. */
	TILE,
	FILL,
	LABEL
}
