/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class MarkerSnapshot
{
	private final List<MarkerGroup> groups;
	private final List<TileMarker> markers;
	private final List<ObjectMarker> objects;
	private final Set<MarkerLocation> highlighted;
	private final Set<ObjectRef> highlightedObjects;
	private final Set<String> disabledGroupIds;

	public MarkerSnapshot(
		List<MarkerGroup> groups,
		List<TileMarker> markers,
		List<ObjectMarker> objects,
		Set<MarkerLocation> highlighted,
		Set<ObjectRef> highlightedObjects,
		Set<String> disabledGroupIds)
	{
		this.groups = groups;
		this.markers = markers;
		this.objects = objects;
		this.highlighted = highlighted;
		this.highlightedObjects = highlightedObjects;
		this.disabledGroupIds = disabledGroupIds;
	}

	public static MarkerSnapshot empty()
	{
		return new MarkerSnapshot(
			Collections.emptyList(),
			Collections.emptyList(),
			Collections.emptyList(),
			Collections.emptySet(),
			Collections.emptySet(),
			Collections.emptySet());
	}

	public List<MarkerGroup> getGroups()
	{
		return groups;
	}

	public List<TileMarker> getMarkers()
	{
		return markers;
	}

	public List<ObjectMarker> getObjects()
	{
		return objects;
	}

	public Set<MarkerLocation> getHighlighted()
	{
		return highlighted;
	}

	public Set<ObjectRef> getHighlightedObjects()
	{
		return highlightedObjects;
	}

	public boolean isShown(TileMarker marker)
	{
		String groupId = marker.getGroupId();
		return groupId == null || !disabledGroupIds.contains(groupId);
	}

	public boolean isShown(ObjectMarker marker)
	{
		String groupId = marker.getGroupId();
		return groupId == null || !disabledGroupIds.contains(groupId);
	}

	public enum ListFilter
	{
		UNGROUPED,
		GROUPED,
		SHOW_ALL
	}

	public List<TileMarker> tilesForList(ListFilter filter)
	{
		List<TileMarker> listed = new ArrayList<>();
		for (TileMarker marker : markers)
		{
			if (includeInList(marker.getGroupId(), filter))
			{
				listed.add(marker);
			}
		}
		return listed;
	}

	public List<ObjectMarker> objectsForList(ListFilter filter)
	{
		List<ObjectMarker> listed = new ArrayList<>();
		for (ObjectMarker marker : objects)
		{
			if (includeInList(marker.getGroupId(), filter))
			{
				listed.add(marker);
			}
		}
		return listed;
	}

	private static boolean includeInList(String groupId, ListFilter filter)
	{
		if (filter == null || filter == ListFilter.SHOW_ALL)
		{
			return true;
		}
		boolean grouped = groupId != null;
		return filter == ListFilter.GROUPED ? grouped : !grouped;
	}
}
