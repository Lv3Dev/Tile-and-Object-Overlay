/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntUnaryOperator;
import javax.inject.Singleton;
import net.runelite.api.coords.WorldPoint;

@Singleton
public class MarkerRepository
{
	private final Map<MarkerLocation, TileMarker> markers = new LinkedHashMap<>();
	private final Map<ObjectRef, ObjectMarker> objects = new LinkedHashMap<>();
	private final Map<String, MarkerGroup> groups = new LinkedHashMap<>();
	private volatile Set<MarkerLocation> highlighted = Collections.emptySet();
	private volatile Set<ObjectRef> highlightedObjects = Collections.emptySet();

	public synchronized void replace(MarkerFile file)
	{
		markers.clear();
		objects.clear();
		groups.clear();
		if (file == null)
		{
			return;
		}
		if (file.getGroups() != null)
		{
			for (MarkerGroup group : file.getGroups())
			{
				if (group == null || group.getId() == null || group.getId().isEmpty())
				{
					continue;
				}
				if (group.getName() == null || group.getName().trim().isEmpty())
				{
					group.setName("Group");
				}
				if (group.getKind() == null)
				{
					group.setKind(GroupKind.TILE);
				}
				groups.put(group.getId(), group.copy());
			}
			normalizeActiveGroups();
		}
		if (file.getMarkers() != null)
		{
			for (TileMarker marker : file.getMarkers())
			{
				if (marker == null || marker.getStyle() == null)
				{
					continue;
				}
				if (marker.getGroupId() != null && !groups.containsKey(marker.getGroupId()))
				{
					marker.setGroupId(null);
				}
				markers.put(marker.location(), marker.copy());
			}
		}
		if (file.getObjects() != null)
		{
			for (ObjectMarker marker : file.getObjects())
			{
				if (marker == null || marker.getObjectId() < 0)
				{
					continue;
				}
				if (marker.getGroupId() != null && !groups.containsKey(marker.getGroupId()))
				{
					marker.setGroupId(null);
				}
				if (file.getVersion() < MarkerFile.HIGHLIGHT_FLAGS_VERSION)
				{
					marker.setShowOutline(true);
				}
				objects.put(marker.ref(), marker.copy());
			}
		}
	}

	public synchronized MarkerFile toFile()
	{
		MarkerFile file = new MarkerFile();
		for (MarkerGroup group : groups.values())
		{
			file.getGroups().add(group.copy());
		}
		for (TileMarker marker : markers.values())
		{
			file.getMarkers().add(marker.copy());
		}
		for (ObjectMarker marker : objects.values())
		{
			file.getObjects().add(marker.copy());
		}
		return file;
	}

	public synchronized MarkerSnapshot snapshot()
	{
		List<MarkerGroup> groupCopies = new ArrayList<>(groups.size());
		Set<String> disabled = new HashSet<>();
		for (MarkerGroup group : groups.values())
		{
			groupCopies.add(group.copy());
			if (!group.isEnabled())
			{
				disabled.add(group.getId());
			}
		}
		List<TileMarker> markerCopies = new ArrayList<>(markers.size());
		for (TileMarker marker : markers.values())
		{
			markerCopies.add(marker.copy());
		}
		List<ObjectMarker> objectCopies = new ArrayList<>(objects.size());
		for (ObjectMarker marker : objects.values())
		{
			objectCopies.add(marker.copy());
		}
		return new MarkerSnapshot(groupCopies, markerCopies, objectCopies, highlighted, highlightedObjects, disabled);
	}

	public synchronized TileMarker get(WorldPoint worldPoint)
	{
		if (worldPoint == null)
		{
			return null;
		}
		TileMarker marker = markers.get(MarkerLocation.fromWorld(worldPoint));
		return marker == null ? null : marker.copy();
	}

	public synchronized ObjectMarker getObject(ObjectRef ref)
	{
		if (ref == null)
		{
			return null;
		}
		ObjectMarker marker = objects.get(ref);
		return marker == null ? null : marker.copy();
	}

	public synchronized void markTile(WorldPoint worldPoint)
	{
		if (worldPoint == null)
		{
			return;
		}
		MarkerLocation location = MarkerLocation.fromWorld(worldPoint);
		TileMarker existing = markers.get(location);
		if (existing != null)
		{
			return;
		}
		TileMarker created = TileMarker.create(location, MarkerStyle.TILE);
		assignActive(created);
		markers.put(location, created);
	}

	public synchronized void markObject(int objectId, WorldPoint worldPoint, String name)
	{
		if (worldPoint == null || objectId < 0)
		{
			return;
		}
		ObjectRef ref = ObjectRef.of(objectId, worldPoint);
		if (objects.containsKey(ref))
		{
			return;
		}
		ObjectMarker created = ObjectMarker.create(ref, name);
		assignActive(created);
		objects.put(ref, created);
	}

	public synchronized boolean markArea(WorldPoint start, WorldPoint end, MarkerStyle style)
	{
		if (start == null || end == null || style == null)
		{
			return false;
		}
		List<TilePoint> tiles = TileGeometry.area(
			start.getX(), start.getY(),
			end.getX(), end.getY(),
			start.getPlane(),
			style == MarkerStyle.BORDER);
		if (tiles == null)
		{
			return false;
		}
		List<MarkerLocation> created = new ArrayList<>();
		for (TilePoint tile : tiles)
		{
			WorldPoint worldPoint = new WorldPoint(tile.getX(), tile.getY(), tile.getPlane());
			MarkerLocation location = MarkerLocation.fromWorld(worldPoint);
			TileMarker existing = markers.get(location);
			if (existing == null)
			{
				markers.put(location, TileMarker.create(location, style));
				created.add(location);
			}
			else
			{
				existing.setStyle(style);
			}
		}

		if (!created.isEmpty())
		{
			String groupId = activeId(GroupKind.TILE);
			if (groupId == null)
			{
				String base = style == MarkerStyle.BORDER ? "Border" : "Region";
				groupId = addGroup(base, GroupKind.TILE).getId();
			}
			for (MarkerLocation location : created)
			{
				TileMarker marker = markers.get(location);
				if (marker != null)
				{
					marker.setGroupId(groupId);
				}
			}
		}
		return true;
	}

	public synchronized Collection<MarkerLocation> connectedOfStyle(WorldPoint start, MarkerStyle style)
	{
		if (start == null || style == null)
		{
			return Collections.emptyList();
		}
		MarkerLocation origin = MarkerLocation.fromWorld(start);
		TileMarker first = markers.get(origin);
		if (first == null || first.getStyle() != style)
		{
			return Collections.emptyList();
		}

		List<MarkerLocation> found = new ArrayList<>();
		Set<MarkerLocation> seen = new HashSet<>();
		ArrayDeque<WorldPoint> queue = new ArrayDeque<>();
		queue.add(new WorldPoint(start.getX(), start.getY(), start.getPlane()));
		seen.add(origin);
		int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		while (!queue.isEmpty())
		{
			WorldPoint point = queue.removeFirst();
			MarkerLocation location = MarkerLocation.fromWorld(point);
			TileMarker marker = markers.get(location);
			if (marker == null || marker.getStyle() != style)
			{
				continue;
			}
			found.add(location);
			for (int[] step : steps)
			{
				WorldPoint next = new WorldPoint(point.getX() + step[0], point.getY() + step[1], point.getPlane());
				MarkerLocation nextLocation = MarkerLocation.fromWorld(next);
				if (seen.add(nextLocation) && markers.containsKey(nextLocation))
				{
					queue.add(next);
				}
			}
		}
		return found;
	}

	public synchronized void unmark(Collection<MarkerLocation> locations)
	{
		if (locations == null)
		{
			return;
		}
		for (MarkerLocation location : locations)
		{
			markers.remove(location);
		}
		if (!highlighted.isEmpty())
		{
			Set<MarkerLocation> next = new HashSet<>(highlighted);
			next.removeAll(locations);
			highlighted = next.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(next);
		}
	}

	public synchronized void unmarkObjects(Collection<ObjectRef> refs)
	{
		if (refs == null)
		{
			return;
		}
		for (ObjectRef ref : refs)
		{
			objects.remove(ref);
		}
		dropHighlightedObjects(refs);
	}

	public synchronized void setLabel(WorldPoint worldPoint, String label)
	{
		if (worldPoint == null)
		{
			return;
		}
		TileMarker marker = markers.get(MarkerLocation.fromWorld(worldPoint));
		if (marker == null)
		{
			return;
		}
		marker.setLabel(label);
	}

	public synchronized void setTileCorners(MarkerLocation location, Boolean cornersOnly)
	{
		TileMarker marker = tile(location);
		if (marker != null)
		{
			marker.setCornersOnly(cornersOnly);
		}
	}

	public synchronized void setTileMerge(MarkerLocation location, Boolean mergeBorders)
	{
		TileMarker marker = tile(location);
		if (marker != null)
		{
			marker.setMergeBorders(mergeBorders);
		}
	}

	public synchronized void setGroupTileCorners(String groupId, Boolean cornersOnly)
	{
		applyGroupTileFlag(groupId, cornersOnly, true);
	}

	public synchronized void setGroupTileMerge(String groupId, Boolean mergeBorders)
	{
		applyGroupTileFlag(groupId, mergeBorders, false);
	}

	public synchronized void setTileColour(MarkerLocation location, TileColourSlot slot, Integer rgb)
	{
		TileMarker marker = tile(location);
		if (marker == null || slot == null)
		{
			return;
		}
		switch (slot)
		{
			case TILE:
				marker.setBorderColour(rgb);
				break;
			case FILL:
				marker.setFillColour(rgb);
				break;
			case LABEL:
				marker.setLabelColour(rgb);
				break;
			default:
				break;
		}
	}

	/** Tile groups only. Null clears the border and tiles go back to their style settings. */
	public synchronized void setGroupColour(String groupId, Integer rgb)
	{
		MarkerGroup group = tileGroup(groupId);
		if (group != null)
		{
			group.setColour(rgb);
		}
	}

	public synchronized void setGroupFillColour(String groupId, Integer rgb)
	{
		MarkerGroup group = tileGroup(groupId);
		if (group != null)
		{
			group.setFillColour(rgb);
		}
	}

	private MarkerGroup tileGroup(String groupId)
	{
		MarkerGroup group = groups.get(groupId);
		if (group == null || kindOf(group) != GroupKind.TILE)
		{
			return null;
		}
		return group;
	}

	public synchronized GroupShare.Bundle shareBundle(String groupId)
	{
		MarkerGroup group = groups.get(groupId);
		if (group == null)
		{
			return null;
		}
		GroupKind kind = kindOf(group);
		List<TileMarker> tiles = new ArrayList<>();
		List<ObjectMarker> sharedObjects = new ArrayList<>();
		if (kind == GroupKind.OBJECT)
		{
			for (ObjectMarker marker : objects.values())
			{
				if (groupId.equals(marker.getGroupId()))
				{
					ObjectMarker copy = marker.copy();
					copy.setGroupId(null);
					sharedObjects.add(copy);
				}
			}
		}
		else
		{
			for (TileMarker marker : markers.values())
			{
				if (groupId.equals(marker.getGroupId()))
				{
					TileMarker copy = marker.copy();
					copy.setGroupId(null);
					tiles.add(copy);
				}
			}
		}
		int count = kind == GroupKind.OBJECT ? sharedObjects.size() : tiles.size();
		String noun = kind == GroupKind.OBJECT ? "object" : "tile";
		String counted = count == 1 ? "1 " + noun : count + " " + noun + "s";
		if (count == 0)
		{
			counted = "no " + noun + "s";
		}
		String where = kind == GroupKind.OBJECT ? "Object Groups" : "Tile Groups";
		String notice = "Copied " + noun + " group \"" + group.getName() + "\" (" + counted + "). Import it from " + where + ".";
		return new GroupShare.Bundle(GroupShare.encode(group, tiles, sharedObjects), notice);
	}

	public synchronized GroupShare.Result importShare(GroupKind expected, String text)
	{
		GroupKind wanted = expected == null ? GroupKind.TILE : expected;
		String trimmed = text == null ? "" : text.trim();
		if (trimmed.isEmpty())
		{
			return GroupShare.Result.message("Paste a copied group. Use the copy icon on a group, then import it here.");
		}
		GroupShare.Decoded decoded = GroupShare.decode(trimmed);
		if (decoded.isNewer())
		{
			return GroupShare.Result.message("That group was copied by a newer version of this plugin.");
		}
		GroupShare share = decoded.getShare();
		if (share == null)
		{
			return GroupShare.Result.message("That text is not a copied group. Use the copy icon on a group, then import it here.");
		}
		if (share.getKind() != wanted)
		{
			if (share.getKind() == GroupKind.OBJECT)
			{
				return GroupShare.Result.message("That is an object group. Open Object Groups and import it there.");
			}
			return GroupShare.Result.message("That is a tile group. Open Tile Groups and import it there.");
		}
		if (wanted == GroupKind.OBJECT)
		{
			return importObjects(share);
		}
		return importTiles(share);
	}

	private GroupShare.Result importTiles(GroupShare share)
	{
		List<TileMarker> incoming = share.getTiles();
		if (incoming.isEmpty())
		{
			MarkerGroup created = storeNewGroup(share.getName(), GroupKind.TILE, share.getColour(), share.getFillColour());
			return GroupShare.Result.imported("Imported \"" + created.getName() + "\" with no tiles.");
		}
		int skipped = 0;
		int usable = 0;
		Set<MarkerLocation> seen = new HashSet<>();
		List<TileMarker> accepted = new ArrayList<>();
		for (TileMarker marker : incoming)
		{
			if (marker == null || marker.getStyle() == null)
			{
				continue;
			}
			MarkerLocation location = marker.location();
			if (!seen.add(location))
			{
				continue;
			}
			usable++;
			if (markers.containsKey(location))
			{
				skipped++;
				continue;
			}
			accepted.add(marker);
		}
		if (accepted.isEmpty())
		{
			if (skipped > 0 && skipped == usable)
			{
				return GroupShare.Result.message("Every tile in that group is already marked, so nothing was imported.");
			}
			return GroupShare.Result.message("That group had nothing that could be imported.");
		}
		MarkerGroup created = storeNewGroup(share.getName(), GroupKind.TILE, share.getColour(), share.getFillColour());
		for (TileMarker marker : accepted)
		{
			TileMarker copy = marker.copy();
			copy.setGroupId(created.getId());
			markers.put(copy.location(), copy);
		}
		return GroupShare.Result.imported(importedMessage(created.getName(), accepted.size(), skipped, "tile"));
	}

	private GroupShare.Result importObjects(GroupShare share)
	{
		List<ObjectMarker> incoming = share.getObjects();
		if (incoming.isEmpty())
		{
			MarkerGroup created = storeNewGroup(share.getName(), GroupKind.OBJECT, null, null);
			return GroupShare.Result.imported("Imported \"" + created.getName() + "\" with no objects.");
		}
		int skipped = 0;
		int usable = 0;
		Set<ObjectRef> seen = new HashSet<>();
		List<ObjectMarker> accepted = new ArrayList<>();
		for (ObjectMarker marker : incoming)
		{
			if (marker == null || marker.getObjectId() < 0)
			{
				continue;
			}
			ObjectRef ref = marker.ref();
			if (!seen.add(ref))
			{
				continue;
			}
			usable++;
			if (objects.containsKey(ref))
			{
				skipped++;
				continue;
			}
			accepted.add(marker);
		}
		if (accepted.isEmpty())
		{
			if (skipped > 0 && skipped == usable)
			{
				return GroupShare.Result.message("Every object in that group is already marked, so nothing was imported.");
			}
			return GroupShare.Result.message("That group had nothing that could be imported.");
		}
		MarkerGroup created = storeNewGroup(share.getName(), GroupKind.OBJECT, null, null);
		for (ObjectMarker marker : accepted)
		{
			ObjectMarker copy = marker.copy();
			copy.setGroupId(created.getId());
			objects.put(copy.ref(), copy);
		}
		return GroupShare.Result.imported(importedMessage(created.getName(), accepted.size(), skipped, "object"));
	}

	private MarkerGroup storeNewGroup(String name, GroupKind kind, Integer colour, Integer fillColour)
	{
		MarkerGroup created = addGroup(name, kind);
		MarkerGroup stored = groups.get(created.getId());
		if (kind == GroupKind.TILE)
		{
			stored.setColour(colour);
			stored.setFillColour(fillColour);
		}
		return stored;
	}

	private static String importedMessage(String name, int added, int skipped, String noun)
	{
		String addedText = added == 1 ? "1 " + noun : added + " " + noun + "s";
		String message = "Imported " + addedText + " into \"" + name + "\".";
		if (skipped > 0)
		{
			String skippedText = skipped == 1 ? "1 " + noun + " was" : skipped + " " + noun + "s were";
			message += " " + skippedText + " already marked and left as they are.";
		}
		return message;
	}

	private void applyGroupTileFlag(String groupId, Boolean value, boolean corners)
	{
		if (groupId == null)
		{
			return;
		}
		for (TileMarker marker : markers.values())
		{
			if (!groupId.equals(marker.getGroupId()))
			{
				continue;
			}
			if (corners)
			{
				marker.setCornersOnly(value);
			}
			else
			{
				marker.setMergeBorders(value);
			}
		}
	}

	private TileMarker tile(MarkerLocation location)
	{
		if (location == null)
		{
			return null;
		}
		return markers.get(location);
	}

	public synchronized void setObjectLabel(ObjectRef ref, String label)
	{
		if (ref == null)
		{
			return;
		}
		ObjectMarker marker = objects.get(ref);
		if (marker == null)
		{
			return;
		}
		String resolved = label == null ? null : label.trim();
		if (resolved != null && resolved.isEmpty())
		{
			resolved = null;
		}
		marker.setLabel(resolved);
	}

	public synchronized void setObjectShowName(ObjectRef ref, boolean show)
	{
		ObjectMarker marker = object(ref);
		if (marker != null)
		{
			marker.setShowName(show);
		}
	}

	public synchronized void setObjectShowLeftClick(ObjectRef ref, boolean show)
	{
		ObjectMarker marker = object(ref);
		if (marker != null)
		{
			marker.setShowLeftClick(show);
		}
	}

	public synchronized void setObjectShowOutline(ObjectRef ref, boolean show)
	{
		ObjectMarker marker = object(ref);
		if (marker != null)
		{
			marker.setShowOutline(show);
		}
	}

	public synchronized void setObjectShowHull(ObjectRef ref, boolean show)
	{
		ObjectMarker marker = object(ref);
		if (marker != null)
		{
			marker.setShowHull(show);
		}
	}

	public synchronized void setObjectShowTile(ObjectRef ref, boolean show)
	{
		ObjectMarker marker = object(ref);
		if (marker != null)
		{
			marker.setShowTile(show);
		}
	}

	public synchronized void setObjectShowClickbox(ObjectRef ref, boolean show)
	{
		ObjectMarker marker = object(ref);
		if (marker != null)
		{
			marker.setShowClickbox(show);
		}
	}

	public synchronized void setObjectColour(ObjectRef ref, ObjectColourSlot slot, Integer rgb)
	{
		ObjectMarker marker = object(ref);
		if (marker == null || slot == null)
		{
			return;
		}
		switch (slot)
		{
			case TEXT:
				marker.setTextColour(rgb);
				break;
			case OUTLINE:
				marker.setOutlineColour(rgb);
				break;
			case HULL:
				marker.setHullColour(rgb);
				break;
			case TILE:
				marker.setTileColour(rgb);
				break;
			case CLICKBOX:
				marker.setClickboxColour(rgb);
				break;
			default:
				break;
		}
	}

	private ObjectMarker object(ObjectRef ref)
	{
		if (ref == null)
		{
			return null;
		}
		return objects.get(ref);
	}

	public synchronized MarkerGroup addGroup(String name)
	{
		return addGroup(name, GroupKind.TILE);
	}

	public synchronized MarkerGroup addGroup(String name, GroupKind kind)
	{
		String resolved = uniqueName(name);
		GroupKind resolvedKind = kind == null ? GroupKind.TILE : kind;
		MarkerGroup group = new MarkerGroup(UUID.randomUUID().toString(), resolved, true, false, resolvedKind);
		groups.put(group.getId(), group);
		return group.copy();
	}

	public synchronized MarkerGroup createNamedGroupFor(String name, Collection<MarkerLocation> locations)
	{
		String trimmed = name == null ? "" : name.trim();
		if (trimmed.isEmpty() || locations == null || locations.isEmpty())
		{
			return null;
		}
		MarkerGroup group = addGroup(trimmed, GroupKind.TILE);
		assign(locations, group.getId());
		return group;
	}

	public synchronized MarkerGroup createNamedObjectGroupFor(String name, Collection<ObjectRef> refs)
	{
		String trimmed = name == null ? "" : name.trim();
		if (trimmed.isEmpty() || refs == null || refs.isEmpty())
		{
			return null;
		}
		MarkerGroup group = addGroup(trimmed, GroupKind.OBJECT);
		assignObjects(refs, group.getId());
		return group;
	}

	public synchronized void setActiveGroup(String id, boolean active)
	{
		MarkerGroup target = groups.get(id);
		if (target == null)
		{
			return;
		}
		GroupKind kind = kindOf(target);
		for (MarkerGroup group : groups.values())
		{
			if (kindOf(group) == kind)
			{
				group.setActive(false);
			}
		}
		target.setActive(active);
	}

	public synchronized boolean rekeyObjects(IntUnaryOperator canonical)
	{
		if (canonical == null || objects.isEmpty())
		{
			return false;
		}
		Map<ObjectRef, ObjectMarker> next = new LinkedHashMap<>();
		boolean changed = false;
		for (ObjectMarker marker : objects.values())
		{
			int id = canonical.applyAsInt(marker.getObjectId());
			if (id < 0)
			{
				id = marker.getObjectId();
			}
			if (id != marker.getObjectId())
			{
				marker.setObjectId(id);
				changed = true;
			}
			ObjectRef ref = marker.ref();
			ObjectMarker kept = next.get(ref);
			if (kept == null)
			{
				next.put(ref, marker);
			}
			else
			{
				mergeObject(kept, marker);
				changed = true;
			}
		}
		if (!changed)
		{
			return false;
		}
		objects.clear();
		objects.putAll(next);
		if (!highlightedObjects.isEmpty())
		{
			Set<ObjectRef> remapped = new HashSet<>();
			for (ObjectRef ref : highlightedObjects)
			{
				int id = canonical.applyAsInt(ref.getObjectId());
				if (id < 0)
				{
					id = ref.getObjectId();
				}
				remapped.add(ObjectRef.of(id, ref.toWorldPoint()));
			}
			highlightedObjects = Collections.unmodifiableSet(remapped);
		}
		return true;
	}

	public synchronized ObjectMarker findMarkedObject(WorldPoint worldPoint, int sceneId, IntUnaryOperator canonical)
	{
		if (worldPoint == null || sceneId < 0)
		{
			return null;
		}
		ObjectMarker direct = objects.get(ObjectRef.of(sceneId, worldPoint));
		if (direct != null)
		{
			return direct.copy();
		}
		int base = canonical == null ? sceneId : canonical.applyAsInt(sceneId);
		if (base >= 0 && base != sceneId)
		{
			ObjectMarker based = objects.get(ObjectRef.of(base, worldPoint));
			if (based != null)
			{
				return based.copy();
			}
		}
		MarkerLocation location = MarkerLocation.fromWorld(worldPoint);
		for (ObjectMarker marker : objects.values())
		{
			if (marker.getRegionId() != location.getRegionId()
				|| marker.getRegionX() != location.getRegionX()
				|| marker.getRegionY() != location.getRegionY()
				|| marker.getPlane() != location.getPlane())
			{
				continue;
			}
			int markerBase = canonical == null ? marker.getObjectId() : canonical.applyAsInt(marker.getObjectId());
			if (marker.getObjectId() == sceneId || marker.getObjectId() == base || markerBase == base)
			{
				return marker.copy();
			}
		}
		return null;
	}

	public synchronized void renameGroup(String id, String name)
	{
		MarkerGroup group = groups.get(id);
		if (group == null)
		{
			return;
		}
		String trimmed = name == null ? "" : name.trim();
		if (trimmed.isEmpty())
		{
			return;
		}
		group.setName(trimmed);
	}

	public synchronized void setGroupEnabled(String id, boolean enabled)
	{
		MarkerGroup group = groups.get(id);
		if (group != null)
		{
			group.setEnabled(enabled);
		}
	}

	public synchronized void setGroupCollapsed(String id, boolean collapsed)
	{
		MarkerGroup group = groups.get(id);
		if (group != null)
		{
			group.setCollapsed(collapsed);
		}
	}

	public synchronized void deleteGroup(String id)
	{
		if (id == null || groups.remove(id) == null)
		{
			return;
		}
		List<MarkerLocation> removedTiles = new ArrayList<>();
		for (TileMarker marker : markers.values())
		{
			if (id.equals(marker.getGroupId()))
			{
				removedTiles.add(marker.location());
			}
		}
		for (MarkerLocation location : removedTiles)
		{
			markers.remove(location);
		}
		if (!highlighted.isEmpty())
		{
			Set<MarkerLocation> next = new HashSet<>(highlighted);
			next.removeAll(removedTiles);
			highlighted = next.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(next);
		}

		List<ObjectRef> removedObjects = new ArrayList<>();
		for (ObjectMarker marker : objects.values())
		{
			if (id.equals(marker.getGroupId()))
			{
				removedObjects.add(marker.ref());
			}
		}
		for (ObjectRef ref : removedObjects)
		{
			objects.remove(ref);
		}
		dropHighlightedObjects(removedObjects);
	}

	public synchronized void assign(Collection<MarkerLocation> locations, String groupId)
	{
		if (locations == null || !isKind(groupId, GroupKind.TILE))
		{
			return;
		}
		for (MarkerLocation location : locations)
		{
			TileMarker marker = markers.get(location);
			if (marker != null)
			{
				marker.setGroupId(groupId);
			}
		}
	}

	public synchronized void assignObjects(Collection<ObjectRef> refs, String groupId)
	{
		if (refs == null || !isKind(groupId, GroupKind.OBJECT))
		{
			return;
		}
		for (ObjectRef ref : refs)
		{
			ObjectMarker marker = objects.get(ref);
			if (marker != null)
			{
				marker.setGroupId(groupId);
			}
		}
	}

	public synchronized void unassign(Collection<MarkerLocation> locations)
	{
		if (locations == null)
		{
			return;
		}
		for (MarkerLocation location : locations)
		{
			TileMarker marker = markers.get(location);
			if (marker != null)
			{
				marker.setGroupId(null);
			}
		}
	}

	public synchronized void unassignObjects(Collection<ObjectRef> refs)
	{
		if (refs == null)
		{
			return;
		}
		for (ObjectRef ref : refs)
		{
			ObjectMarker marker = objects.get(ref);
			if (marker != null)
			{
				marker.setGroupId(null);
			}
		}
	}

	public void setHighlighted(Set<MarkerLocation> locations)
	{
		if (locations == null || locations.isEmpty())
		{
			highlighted = Collections.emptySet();
			return;
		}
		highlighted = Collections.unmodifiableSet(new HashSet<>(locations));
	}

	public Set<MarkerLocation> getHighlighted()
	{
		return highlighted;
	}

	public void setHighlightedObjects(Set<ObjectRef> refs)
	{
		if (refs == null || refs.isEmpty())
		{
			highlightedObjects = Collections.emptySet();
			return;
		}
		highlightedObjects = Collections.unmodifiableSet(new HashSet<>(refs));
	}

	public Set<ObjectRef> getHighlightedObjects()
	{
		return highlightedObjects;
	}

	private boolean isKind(String groupId, GroupKind kind)
	{
		if (groupId == null)
		{
			return false;
		}
		MarkerGroup group = groups.get(groupId);
		return group != null && kindOf(group) == kind;
	}

	private void assignActive(TileMarker marker)
	{
		if (marker == null || marker.getGroupId() != null)
		{
			return;
		}
		String id = activeId(GroupKind.TILE);
		if (id != null)
		{
			marker.setGroupId(id);
		}
	}

	private void assignActive(ObjectMarker marker)
	{
		if (marker == null || marker.getGroupId() != null)
		{
			return;
		}
		String id = activeId(GroupKind.OBJECT);
		if (id != null)
		{
			marker.setGroupId(id);
		}
	}

	private String activeId(GroupKind kind)
	{
		for (MarkerGroup group : groups.values())
		{
			if (group.isActive() && kindOf(group) == kind)
			{
				return group.getId();
			}
		}
		return null;
	}

	private void normalizeActiveGroups()
	{
		boolean tileActive = false;
		boolean objectActive = false;
		for (MarkerGroup group : groups.values())
		{
			if (!group.isActive())
			{
				continue;
			}
			if (kindOf(group) == GroupKind.OBJECT)
			{
				if (objectActive)
				{
					group.setActive(false);
				}
				else
				{
					objectActive = true;
				}
			}
			else if (tileActive)
			{
				group.setActive(false);
			}
			else
			{
				tileActive = true;
			}
		}
	}

	private static GroupKind kindOf(MarkerGroup group)
	{
		return group.getKind() == null ? GroupKind.TILE : group.getKind();
	}

	private static void mergeObject(ObjectMarker keep, ObjectMarker extra)
	{
		if (!keep.hasLabel() && extra.hasLabel())
		{
			keep.setLabel(extra.getLabel());
		}
		if ((keep.getName() == null || keep.getName().trim().isEmpty()) && extra.getName() != null)
		{
			keep.setName(extra.getName());
		}
		if (keep.getGroupId() == null)
		{
			keep.setGroupId(extra.getGroupId());
		}
		keep.setShowOutline(keep.isShowOutline() || extra.isShowOutline());
		keep.setShowHull(keep.isShowHull() || extra.isShowHull());
		keep.setShowTile(keep.isShowTile() || extra.isShowTile());
		keep.setShowClickbox(keep.isShowClickbox() || extra.isShowClickbox());
		keep.setShowName(keep.isShowName() || extra.isShowName());
		keep.setShowLeftClick(keep.isShowLeftClick() || extra.isShowLeftClick());
		if (keep.getTextColour() == null)
		{
			keep.setTextColour(extra.getTextColour());
		}
		if (keep.getOutlineColour() == null)
		{
			keep.setOutlineColour(extra.getOutlineColour());
		}
		if (keep.getHullColour() == null)
		{
			keep.setHullColour(extra.getHullColour());
		}
		if (keep.getTileColour() == null)
		{
			keep.setTileColour(extra.getTileColour());
		}
		if (keep.getClickboxColour() == null)
		{
			keep.setClickboxColour(extra.getClickboxColour());
		}
	}

	private void dropHighlightedObjects(Collection<ObjectRef> refs)
	{
		if (highlightedObjects.isEmpty() || refs == null)
		{
			return;
		}
		Set<ObjectRef> next = new HashSet<>(highlightedObjects);
		next.removeAll(refs);
		highlightedObjects = next.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(next);
	}

	private String uniqueName(String requested)
	{
		String base = requested == null ? "" : requested.trim();
		if (base.isEmpty())
		{
			int index = 1;
			while (nameTaken("Group " + index))
			{
				index++;
			}
			return "Group " + index;
		}
		if (!nameTaken(base))
		{
			return base;
		}
		int index = 2;
		while (nameTaken(base + " " + index))
		{
			index++;
		}
		return base + " " + index;
	}

	private boolean nameTaken(String name)
	{
		for (MarkerGroup group : groups.values())
		{
			if (name.equalsIgnoreCase(group.getName()))
			{
				return true;
			}
		}
		return false;
	}
}
