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
public class ObjectMarker
{
	private int objectId;
	private int regionId;
	private int regionX;
	private int regionY;
	private int plane;
	private String label;
	private String groupId;
	private String name;
	/**
	 * Draw the object name when there is no label. A label turns this off,
	 * and clearing the label leaves it off.
	 */
	private boolean showName;
	/**
	 * Draw the object's left-click option when there is no label. A label turns
	 * this off, and clearing the label leaves it off.
	 */
	private boolean showLeftClick;
	/**
	 * Model outline. New marks turn this on. Files saved before highlight
	 * flags existed are given the same default when they load.
	 */
	private boolean showOutline;
	private boolean showHull;
	private boolean showTile;
	private boolean showClickbox;

	public ObjectRef ref()
	{
		return new ObjectRef(objectId, regionId, regionX, regionY, plane);
	}

	public boolean hasLabel()
	{
		return label != null && !label.trim().isEmpty();
	}

	public ObjectMarker copy()
	{
		return new ObjectMarker(
			objectId, regionId, regionX, regionY, plane, label, groupId, name,
			showName, showLeftClick, showOutline, showHull, showTile, showClickbox);
	}

	public static ObjectMarker create(ObjectRef ref, String name)
	{
		return new ObjectMarker(
			ref.getObjectId(),
			ref.getRegionId(),
			ref.getRegionX(),
			ref.getRegionY(),
			ref.getPlane(),
			null,
			null,
			name,
			false,
			false,
			true,
			false,
			false,
			false);
	}
}
