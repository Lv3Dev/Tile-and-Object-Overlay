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
	private boolean showName;
	private boolean showLeftClick;
	private boolean showOutline;
	private boolean showHull;
	private boolean showTile;
	private boolean showClickbox;
	private Integer textColour;
	private Integer outlineColour;
	private Integer hullColour;
	private Integer tileColour;
	private Integer clickboxColour;

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
			showName, showLeftClick, showOutline, showHull, showTile, showClickbox,
			textColour, outlineColour, hullColour, tileColour, clickboxColour);
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
			false,
			null,
			null,
			null,
			null,
			null);
	}
}
