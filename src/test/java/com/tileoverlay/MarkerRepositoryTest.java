/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay;

import java.util.Collection;
import java.util.Collections;
import net.runelite.api.coords.WorldPoint;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Assert;
import org.junit.Test;

public class MarkerRepositoryTest
{
	@Test
	public void deleteGroupWipesItsTiles()
	{
		MarkerRepository repository = new MarkerRepository(RuneLiteAPI.GSON);
		WorldPoint point = new WorldPoint(3200, 3200, 0);
		MarkerGroup group = repository.addGroup("Home");
		repository.markTile(point);
		MarkerLocation location = MarkerLocation.fromWorld(point);
		repository.assign(Collections.singleton(location), group.getId());
		repository.setHighlighted(Collections.singleton(location));

		repository.deleteGroup(group.getId());

		Assert.assertNull(repository.get(point));
		Assert.assertTrue(repository.snapshot().getGroups().isEmpty());
		Assert.assertFalse(repository.getHighlighted().contains(location));
	}

	@Test
	public void overlappingAreaKeepsLabelAndGroup()
	{
		MarkerRepository repository = new MarkerRepository(RuneLiteAPI.GSON);
		WorldPoint start = new WorldPoint(3200, 3200, 0);
		WorldPoint end = new WorldPoint(3201, 3200, 0);
		repository.markTile(start);
		repository.setLabel(start, "bank");
		MarkerGroup group = repository.addGroup("Home");
		repository.assign(Collections.singleton(MarkerLocation.fromWorld(start)), group.getId());

		Assert.assertTrue(repository.markArea(start, end, MarkerStyle.REGION));

		TileMarker marker = repository.get(start);
		Assert.assertEquals(MarkerStyle.REGION, marker.getStyle());
		Assert.assertEquals("bank", marker.getLabel());
		Assert.assertEquals(group.getId(), marker.getGroupId());
		Assert.assertNotNull(repository.get(end));
	}

	@Test
	public void singleMarkDoesNotRestyleAnExistingTile()
	{
		MarkerRepository repository = new MarkerRepository(RuneLiteAPI.GSON);
		WorldPoint point = new WorldPoint(3200, 3200, 0);
		repository.markArea(point, point, MarkerStyle.BORDER);
		repository.markTile(point);
		Assert.assertEquals(MarkerStyle.BORDER, repository.get(point).getStyle());
	}

	@Test
	public void areaLargerThanTheCapIsRejected()
	{
		MarkerRepository repository = new MarkerRepository(RuneLiteAPI.GSON);
		WorldPoint start = new WorldPoint(3200, 3200, 0);
		WorldPoint end = new WorldPoint(3200 + TileGeometry.MAX_SPAN, 3200, 0);
		Assert.assertFalse(repository.markArea(start, end, MarkerStyle.REGION));
		Assert.assertNull(repository.get(start));
	}

	@Test
	public void disabledGroupIsHiddenFromTheMergeSet()
	{
		MarkerRepository repository = new MarkerRepository(RuneLiteAPI.GSON);
		WorldPoint point = new WorldPoint(3200, 3200, 0);
		MarkerGroup group = repository.addGroup("Hide");
		repository.markTile(point);
		repository.assign(Collections.singleton(MarkerLocation.fromWorld(point)), group.getId());
		repository.setGroupEnabled(group.getId(), false);

		MarkerSnapshot snapshot = repository.snapshot();
		Assert.assertFalse(snapshot.isShown(snapshot.getMarkers().get(0)));
	}

	@Test
	public void newGroupsGetUniqueNames()
	{
		MarkerRepository repository = new MarkerRepository(RuneLiteAPI.GSON);
		Assert.assertEquals("Group 1", repository.addGroup(null).getName());
		Assert.assertEquals("Group 2", repository.addGroup("").getName());
		Assert.assertEquals("Home", repository.addGroup("Home").getName());
		Assert.assertEquals("Home 2", repository.addGroup("Home").getName());
	}

	@Test
	public void deletingAnObjectGroupRemovesItsObjectsOnly()
	{
		MarkerRepository repository = new MarkerRepository(RuneLiteAPI.GSON);
		WorldPoint point = new WorldPoint(3200, 3200, 0);
		repository.markTile(point);
		repository.addGroup("Tiles");
		MarkerGroup objects = repository.addGroup("Rocks", GroupKind.OBJECT);
		ObjectRef ref = ObjectRef.of(123, point);
		repository.markObject(123, point, "Rocks");
		repository.assignObjects(Collections.singleton(ref), objects.getId());
		repository.setHighlightedObjects(Collections.singleton(ref));

		repository.deleteGroup(objects.getId());

		Assert.assertNull(repository.getObject(ref));
		Assert.assertNotNull(repository.get(point));
		Assert.assertEquals(1, repository.snapshot().getGroups().size());
		Assert.assertFalse(repository.getHighlightedObjects().contains(ref));
	}

	@Test
	public void markingAnObjectAgainKeepsItsLabel()
	{
		MarkerRepository repository = new MarkerRepository(RuneLiteAPI.GSON);
		WorldPoint point = new WorldPoint(3200, 3200, 0);
		ObjectRef ref = ObjectRef.of(123, point);
		repository.markObject(123, point, "Rocks");
		repository.setObjectLabel(ref, "bank");
		repository.markObject(123, point, "Other");
		Assert.assertEquals("bank", repository.getObject(ref).getLabel());
		Assert.assertEquals("Rocks", repository.getObject(ref).getName());
	}

	@Test
	public void aLabelLeavesNameAndLeftClickAlone()
	{
		MarkerRepository repository = new MarkerRepository(RuneLiteAPI.GSON);
		WorldPoint point = new WorldPoint(3200, 3200, 0);
		ObjectRef ref = ObjectRef.of(123, point);
		repository.markObject(123, point, "Rocks");
		repository.setObjectShowName(ref, true);
		repository.setObjectShowLeftClick(ref, true);

		repository.setObjectLabel(ref, "bank");
		ObjectMarker labelled = repository.getObject(ref);
		Assert.assertTrue(labelled.hasLabel());
		Assert.assertTrue(labelled.isShowName());
		Assert.assertTrue(labelled.isShowLeftClick());

		repository.setObjectShowName(ref, false);
		Assert.assertFalse(repository.getObject(ref).isShowName());
		Assert.assertTrue(repository.getObject(ref).hasLabel());
		Assert.assertTrue(repository.getObject(ref).isShowLeftClick());

		repository.setObjectLabel(ref, "   ");
		ObjectMarker cleared = repository.getObject(ref);
		Assert.assertFalse(cleared.hasLabel());
		Assert.assertNull(cleared.getLabel());
		Assert.assertFalse(cleared.isShowName());
		Assert.assertTrue(cleared.isShowLeftClick());
		Assert.assertTrue(cleared.isShowOutline());
	}

	@Test
	public void mountedDecorationKeepsItsLabelWhenTheConfigurationChanges()
	{
		MarkerRepository repository = new MarkerRepository(RuneLiteAPI.GSON);
		ObjectFamilies families = new ObjectFamilies();
		WorldPoint point = new WorldPoint(3200, 3200, 0);
		int stored = families.canonical(33412);
		repository.markObject(stored, point, "Xeric's talisman");
		ObjectRef ref = ObjectRef.of(stored, point);
		repository.setObjectLabel(ref, "home");
		repository.setObjectShowName(ref, true);
		repository.setObjectShowLeftClick(ref, true);
		repository.setObjectColour(ref, ObjectColourSlot.TEXT, 0x80FFFFFF);

		ObjectMarker found = repository.findMarkedObject(point, 33415, families::canonical);
		Assert.assertNotNull(found);
		Assert.assertEquals("home", found.getLabel());
		Assert.assertTrue(found.isShowName());
		Assert.assertTrue(found.isShowLeftClick());
		Assert.assertEquals(Integer.valueOf(0x80FFFFFF), found.getTextColour());
		Assert.assertNull(repository.findMarkedObject(point, 33416, families::canonical));

		families.index(29150, new int[]{33411, 33412, 33419, -1});
		Assert.assertTrue(repository.rekeyObjects(families::canonical));
		ObjectMarker after = repository.findMarkedObject(point, 33414, families::canonical);
		Assert.assertNotNull(after);
		Assert.assertEquals("home", after.getLabel());
		Assert.assertTrue(after.isShowName());
		Assert.assertTrue(after.isShowLeftClick());
		Assert.assertEquals(Integer.valueOf(0x80FFFFFF), after.getTextColour());
		Assert.assertEquals(29150, after.getObjectId());
		Assert.assertNull(repository.findMarkedObject(point, 33420, families::canonical));
	}

	@Test
	public void newObjectMarksStartWithOutlineOnlyAndFlagsRoundTrip()
	{
		MarkerRepository repository = new MarkerRepository(RuneLiteAPI.GSON);
		WorldPoint point = new WorldPoint(3200, 3200, 0);
		ObjectRef ref = ObjectRef.of(123, point);
		repository.markObject(123, point, "Rocks");
		Assert.assertTrue(repository.getObject(ref).isShowOutline());
		Assert.assertFalse(repository.getObject(ref).isShowHull());
		Assert.assertFalse(repository.getObject(ref).isShowTile());
		Assert.assertFalse(repository.getObject(ref).isShowClickbox());

		repository.setObjectShowOutline(ref, false);
		repository.setObjectShowHull(ref, true);
		repository.setObjectShowTile(ref, true);
		repository.setObjectShowClickbox(ref, true);
		repository.setObjectLabel(ref, "bank");

		MarkerRepository loaded = new MarkerRepository(RuneLiteAPI.GSON);
		loaded.replace(repository.toFile());
		ObjectMarker marker = loaded.getObject(ref);
		Assert.assertEquals(MarkerFile.CURRENT_VERSION, repository.toFile().getVersion());
		Assert.assertFalse(marker.isShowOutline());
		Assert.assertTrue(marker.isShowHull());
		Assert.assertTrue(marker.isShowTile());
		Assert.assertTrue(marker.isShowClickbox());
		Assert.assertFalse(marker.isShowName());
		Assert.assertFalse(marker.isShowLeftClick());
	}

	@Test
	public void groupedMarkersStayOffTheListUntilAsked()
	{
		MarkerRepository repository = new MarkerRepository(RuneLiteAPI.GSON);
		WorldPoint grouped = new WorldPoint(3200, 3200, 0);
		WorldPoint loose = new WorldPoint(3201, 3200, 0);
		MarkerGroup group = repository.addGroup("Home");
		repository.markTile(grouped);
		repository.markTile(loose);
		repository.assign(Collections.singleton(MarkerLocation.fromWorld(grouped)), group.getId());

		WorldPoint objectPoint = new WorldPoint(3202, 3200, 0);
		MarkerGroup objectGroup = repository.addGroup("Rocks", GroupKind.OBJECT);
		repository.markObject(10, objectPoint, "Rocks");
		repository.markObject(11, loose, "Tree");
		repository.assignObjects(Collections.singleton(ObjectRef.of(10, objectPoint)), objectGroup.getId());

		MarkerSnapshot snapshot = repository.snapshot();
		Assert.assertEquals(1, snapshot.tilesForList(MarkerSnapshot.ListFilter.UNGROUPED).size());
		Assert.assertNull(snapshot.tilesForList(MarkerSnapshot.ListFilter.UNGROUPED).get(0).getGroupId());
		Assert.assertEquals(1, snapshot.tilesForList(MarkerSnapshot.ListFilter.GROUPED).size());
		Assert.assertEquals(group.getId(), snapshot.tilesForList(MarkerSnapshot.ListFilter.GROUPED).get(0).getGroupId());
		Assert.assertEquals(2, snapshot.tilesForList(MarkerSnapshot.ListFilter.SHOW_ALL).size());
		Assert.assertEquals(1, snapshot.objectsForList(MarkerSnapshot.ListFilter.UNGROUPED).size());
		Assert.assertEquals(11, snapshot.objectsForList(MarkerSnapshot.ListFilter.UNGROUPED).get(0).getObjectId());
		Assert.assertEquals(1, snapshot.objectsForList(MarkerSnapshot.ListFilter.GROUPED).size());
		Assert.assertEquals(10, snapshot.objectsForList(MarkerSnapshot.ListFilter.GROUPED).get(0).getObjectId());
		Assert.assertEquals(2, snapshot.objectsForList(MarkerSnapshot.ListFilter.SHOW_ALL).size());
	}

	@Test
	public void activeGroupsStayOnTheirOwnKind()
	{
		MarkerRepository repository = new MarkerRepository(RuneLiteAPI.GSON);
		MarkerGroup tiles = repository.addGroup("Tiles");
		MarkerGroup objects = repository.addGroup("Rocks", GroupKind.OBJECT);
		repository.setActiveGroup(objects.getId(), true);
		repository.setActiveGroup(tiles.getId(), true);

		WorldPoint point = new WorldPoint(3200, 3200, 0);
		repository.markTile(point);
		Assert.assertEquals(tiles.getId(), repository.get(point).getGroupId());

		repository.markObject(11, point, "Rock");
		Assert.assertEquals(objects.getId(), repository.getObject(ObjectRef.of(11, point)).getGroupId());

		repository.setActiveGroup(tiles.getId(), false);
		WorldPoint other = new WorldPoint(3201, 3200, 0);
		repository.markTile(other);
		Assert.assertNull(repository.get(other).getGroupId());

		WorldPoint end = new WorldPoint(3202, 3200, 0);
		repository.setActiveGroup(tiles.getId(), true);
		Assert.assertTrue(repository.markArea(other, end, MarkerStyle.REGION));
		Assert.assertNull(repository.get(other).getGroupId());
		Assert.assertEquals(tiles.getId(), repository.get(end).getGroupId());
	}

	@Test
	public void namedGroupRequiresANameAndRekeyCollapsesDecorations()
	{
		MarkerRepository repository = new MarkerRepository(RuneLiteAPI.GSON);
		WorldPoint point = new WorldPoint(3200, 3200, 0);
		Assert.assertNull(repository.createNamedGroupFor("  ", Collections.singleton(MarkerLocation.fromWorld(point))));

		repository.markTile(point);
		MarkerGroup named = repository.createNamedGroupFor("Home", Collections.singleton(MarkerLocation.fromWorld(point)));
		Assert.assertEquals("Home", named.getName());
		Assert.assertEquals(named.getId(), repository.get(point).getGroupId());

		repository.markObject(33411, point, "Xeric's talisman");
		repository.markObject(33412, point, "Xeric's talisman");
		Assert.assertTrue(repository.rekeyObjects(id -> id == 33411 || id == 33412 ? 29150 : id));
		Assert.assertNotNull(repository.getObject(ObjectRef.of(29150, point)));
		Assert.assertNull(repository.getObject(ObjectRef.of(33411, point)));
		Assert.assertNull(repository.getObject(ObjectRef.of(33412, point)));
	}

	@Test
	public void unmarkRegionRemovesTheConnectedAreaOnly()
	{
		MarkerRepository repository = new MarkerRepository(RuneLiteAPI.GSON);
		WorldPoint start = new WorldPoint(3200, 3200, 0);
		WorldPoint end = new WorldPoint(3202, 3201, 0);
		Assert.assertTrue(repository.markArea(start, end, MarkerStyle.REGION));

		WorldPoint far = new WorldPoint(3300, 3300, 0);
		Assert.assertTrue(repository.markArea(far, far, MarkerStyle.REGION));

		WorldPoint above = new WorldPoint(3201, 3200, 1);
		Assert.assertTrue(repository.markArea(above, above, MarkerStyle.REGION));

		WorldPoint beside = new WorldPoint(3203, 3200, 0);
		repository.markTile(beside);

		WorldPoint diagonal = new WorldPoint(3199, 3202, 0);
		Assert.assertTrue(repository.markArea(diagonal, diagonal, MarkerStyle.REGION));

		WorldPoint touching = new WorldPoint(3203, 3201, 0);
		Assert.assertTrue(repository.markArea(touching, touching, MarkerStyle.REGION));

		WorldPoint inside = new WorldPoint(3201, 3200, 0);
		Collection<MarkerLocation> connected = repository.connectedOfStyle(inside, MarkerStyle.REGION);
		Assert.assertEquals(7, connected.size());

		repository.unmark(connected);
		Assert.assertNull(repository.get(start));
		Assert.assertNull(repository.get(end));
		Assert.assertNull(repository.get(inside));
		Assert.assertNull(repository.get(touching));
		Assert.assertNotNull(repository.get(far));
		Assert.assertNotNull(repository.get(above));
		Assert.assertEquals(MarkerStyle.TILE, repository.get(beside).getStyle());
		Assert.assertNotNull(repository.get(diagonal));
	}

	@Test
	public void unmarkBorderRemovesTheWholePerimeter()
	{
		MarkerRepository repository = new MarkerRepository(RuneLiteAPI.GSON);
		WorldPoint start = new WorldPoint(3200, 3200, 0);
		WorldPoint end = new WorldPoint(3202, 3202, 0);
		Assert.assertTrue(repository.markArea(start, end, MarkerStyle.BORDER));
		WorldPoint interior = new WorldPoint(3201, 3201, 0);
		repository.markTile(interior);

		Collection<MarkerLocation> connected = repository.connectedOfStyle(start, MarkerStyle.BORDER);
		Assert.assertEquals(8, connected.size());

		repository.unmark(connected);
		Assert.assertNull(repository.get(start));
		Assert.assertNull(repository.get(new WorldPoint(3202, 3201, 0)));
		Assert.assertNull(repository.get(end));
		Assert.assertEquals(MarkerStyle.TILE, repository.get(interior).getStyle());
	}

	@Test
	public void regionWithoutAnActiveGroupCreatesOne()
	{
		MarkerRepository repository = new MarkerRepository(RuneLiteAPI.GSON);
		WorldPoint start = new WorldPoint(3200, 3200, 0);
		WorldPoint end = new WorldPoint(3201, 3200, 0);
		Assert.assertTrue(repository.markArea(start, end, MarkerStyle.REGION));

		String groupId = repository.get(start).getGroupId();
		Assert.assertNotNull(groupId);
		Assert.assertEquals(groupId, repository.get(end).getGroupId());
		Assert.assertEquals("Region", repository.snapshot().getGroups().get(0).getName());
		Assert.assertEquals(GroupKind.TILE, repository.snapshot().getGroups().get(0).getKind());
	}

	@Test
	public void borderWithoutAnActiveGroupCreatesOne()
	{
		MarkerRepository repository = new MarkerRepository(RuneLiteAPI.GSON);
		WorldPoint point = new WorldPoint(3200, 3200, 0);
		Assert.assertTrue(repository.markArea(point, point, MarkerStyle.BORDER));
		Assert.assertEquals("Border", repository.snapshot().getGroups().get(0).getName());
		Assert.assertEquals(repository.snapshot().getGroups().get(0).getId(), repository.get(point).getGroupId());
	}

	@Test
	public void regionUsesTheActiveTileGroup()
	{
		MarkerRepository repository = new MarkerRepository(RuneLiteAPI.GSON);
		MarkerGroup active = repository.addGroup("Home");
		repository.setActiveGroup(active.getId(), true);
		WorldPoint start = new WorldPoint(3200, 3200, 0);
		WorldPoint end = new WorldPoint(3201, 3200, 0);
		Assert.assertTrue(repository.markArea(start, end, MarkerStyle.REGION));

		Assert.assertEquals(active.getId(), repository.get(start).getGroupId());
		Assert.assertEquals(active.getId(), repository.get(end).getGroupId());
		Assert.assertEquals(1, repository.snapshot().getGroups().size());
	}

	@Test
	public void markedTileStaysUngroupedWithoutAnActiveGroup()
	{
		MarkerRepository repository = new MarkerRepository(RuneLiteAPI.GSON);
		WorldPoint point = new WorldPoint(3200, 3200, 0);
		repository.markTile(point);
		Assert.assertNull(repository.get(point).getGroupId());
		Assert.assertTrue(repository.snapshot().getGroups().isEmpty());
	}

	@Test
	public void objectColourOverrideCanBeCleared()
	{
		MarkerRepository repository = new MarkerRepository(RuneLiteAPI.GSON);
		WorldPoint point = new WorldPoint(3200, 3200, 0);
		repository.markObject(5, point, "Pool");
		ObjectRef ref = ObjectRef.of(5, point);
		repository.setObjectColour(ref, ObjectColourSlot.OUTLINE, 0xFF112233);
		repository.setObjectColour(ref, ObjectColourSlot.TEXT, 0xFFFFFFFF);

		ObjectMarker marker = repository.getObject(ref);
		Assert.assertEquals(Integer.valueOf(0xFF112233), marker.getOutlineColour());
		Assert.assertEquals(Integer.valueOf(0xFFFFFFFF), marker.getTextColour());
		Assert.assertNull(marker.getHullColour());
		Assert.assertNull(marker.getTileColour());
		Assert.assertNull(marker.getClickboxColour());

		repository.setObjectColour(ref, ObjectColourSlot.OUTLINE, null);
		Assert.assertNull(repository.getObject(ref).getOutlineColour());
	}

	@Test
	public void cornerAndMergeOverridesFollowTheTileAndTheGroup()
	{
		MarkerRepository repository = new MarkerRepository(RuneLiteAPI.GSON);
		WorldPoint first = new WorldPoint(3200, 3200, 0);
		WorldPoint second = new WorldPoint(3201, 3200, 0);
		WorldPoint other = new WorldPoint(3205, 3200, 0);
		repository.markTile(first);
		repository.markTile(second);
		repository.markTile(other);
		Assert.assertNull(repository.get(first).getCornersOnly());
		Assert.assertNull(repository.get(first).getMergeBorders());

		MarkerGroup group = repository.addGroup("Home");
		repository.assign(Collections.singleton(MarkerLocation.fromWorld(first)), group.getId());
		repository.assign(Collections.singleton(MarkerLocation.fromWorld(second)), group.getId());

		repository.setTileCorners(MarkerLocation.fromWorld(first), Boolean.TRUE);
		repository.setTileMerge(MarkerLocation.fromWorld(first), Boolean.FALSE);
		Assert.assertEquals(Boolean.TRUE, repository.get(first).getCornersOnly());
		Assert.assertEquals(Boolean.FALSE, repository.get(first).getMergeBorders());
		Assert.assertNull(repository.get(second).getCornersOnly());
		Assert.assertNull(repository.get(second).getMergeBorders());

		repository.setGroupTileCorners(group.getId(), Boolean.TRUE);
		repository.setGroupTileMerge(group.getId(), Boolean.TRUE);
		Assert.assertEquals(Boolean.TRUE, repository.get(first).getCornersOnly());
		Assert.assertEquals(Boolean.TRUE, repository.get(first).getMergeBorders());
		Assert.assertEquals(Boolean.TRUE, repository.get(second).getCornersOnly());
		Assert.assertEquals(Boolean.TRUE, repository.get(second).getMergeBorders());
		Assert.assertNull(repository.get(other).getCornersOnly());
		Assert.assertNull(repository.get(other).getMergeBorders());

		repository.setGroupTileCorners(group.getId(), null);
		Assert.assertNull(repository.get(first).getCornersOnly());
		Assert.assertNull(repository.get(second).getCornersOnly());
		Assert.assertEquals(Boolean.TRUE, repository.get(first).getMergeBorders());

		MarkerRepository loaded = new MarkerRepository(RuneLiteAPI.GSON);
		loaded.replace(repository.toFile());
		Assert.assertEquals(MarkerFile.CURRENT_VERSION, loaded.toFile().getVersion());
		Assert.assertNull(loaded.get(second).getCornersOnly());
		Assert.assertEquals(Boolean.TRUE, loaded.get(second).getMergeBorders());
		Assert.assertNull(loaded.get(other).getMergeBorders());
	}

	@Test
	public void groupColourIsOnlyForTileGroups()
	{
		MarkerRepository repository = new MarkerRepository(RuneLiteAPI.GSON);
		MarkerGroup tiles = repository.addGroup("Tiles", GroupKind.TILE);
		MarkerGroup objects = repository.addGroup("Objects", GroupKind.OBJECT);
		repository.setGroupColour(tiles.getId(), 0xFF010203);
		repository.setGroupColour(objects.getId(), 0xFF040506);
		repository.setGroupFillColour(tiles.getId(), 0x40010203);
		repository.setGroupFillColour(objects.getId(), 0xFF040506);

		MarkerRepository loaded = new MarkerRepository(RuneLiteAPI.GSON);
		loaded.replace(repository.toFile());
		Assert.assertEquals(Integer.valueOf(0xFF010203), group(loaded, "Tiles").getColour());
		Assert.assertEquals(Integer.valueOf(0x40010203), group(loaded, "Tiles").getFillColour());
		Assert.assertNull(group(loaded, "Objects").getColour());
		Assert.assertNull(group(loaded, "Objects").getFillColour());
	}

	@Test
	public void copiedGroupImportsColoursLabelsAndStyles()
	{
		MarkerRepository repository = new MarkerRepository(RuneLiteAPI.GSON);
		WorldPoint point = new WorldPoint(3200, 3200, 1);
		repository.markTile(point);
		MarkerGroup group = repository.addGroup("Route");
		repository.assign(Collections.singleton(MarkerLocation.fromWorld(point)), group.getId());
		repository.setGroupColour(group.getId(), 0xC0112233);
		repository.setGroupFillColour(group.getId(), 0x180A0B0C);
		MarkerLocation location = MarkerLocation.fromWorld(point);
		repository.setTileCorners(location, Boolean.TRUE);
		repository.setTileMerge(location, Boolean.FALSE);
		repository.setLabel(point, "step");
		repository.setTileColour(location, TileColourSlot.LABEL, 0xFFABCDEF);
		repository.setTileColour(location, TileColourSlot.FILL, 0x40010203);

		String text = repository.shareBundle(group.getId()).getText();
		MarkerRepository other = new MarkerRepository(RuneLiteAPI.GSON);
		GroupShare.Result result = other.importShare(GroupKind.TILE, text);

		Assert.assertTrue(result.isChanged());
		Assert.assertTrue(result.getMessage().contains("Route"));
		TileMarker imported = other.snapshot().getMarkers().get(0);
		Assert.assertEquals(MarkerStyle.TILE, imported.getStyle());
		Assert.assertEquals("step", imported.getLabel());
		Assert.assertEquals(Boolean.TRUE, imported.getCornersOnly());
		Assert.assertEquals(Boolean.FALSE, imported.getMergeBorders());
		Assert.assertEquals(Integer.valueOf(0xFFABCDEF), imported.getLabelColour());
		Assert.assertEquals(Integer.valueOf(0x40010203), imported.getFillColour());
		Assert.assertNull(imported.getBorderColour());
		Assert.assertEquals(Integer.valueOf(0xC0112233), group(other, "Route").getColour());
		Assert.assertEquals(Integer.valueOf(0x180A0B0C), group(other, "Route").getFillColour());
		Assert.assertEquals(group(other, "Route").getId(), imported.getGroupId());
	}

	@Test
	public void importRejectsTheOtherKindAndLeavesExistingMarks()
	{
		MarkerRepository tiles = new MarkerRepository(RuneLiteAPI.GSON);
		WorldPoint point = new WorldPoint(3200, 3200, 0);
		tiles.markTile(point);
		MarkerGroup tileGroup = tiles.addGroup("Route");
		tiles.assign(Collections.singleton(MarkerLocation.fromWorld(point)), tileGroup.getId());
		String tileText = tiles.shareBundle(tileGroup.getId()).getText();

		MarkerRepository objects = new MarkerRepository(RuneLiteAPI.GSON);
		objects.markObject(5, point, "Bank");
		MarkerGroup objectGroup = objects.addGroup("Bank", GroupKind.OBJECT);
		objects.assignObjects(Collections.singleton(ObjectRef.of(5, point)), objectGroup.getId());
		objects.setObjectLabel(ObjectRef.of(5, point), "chest");
		String objectText = objects.shareBundle(objectGroup.getId()).getText();

		GroupShare.Result wrongTiles = tiles.importShare(GroupKind.TILE, objectText);
		Assert.assertFalse(wrongTiles.isChanged());
		Assert.assertTrue(wrongTiles.getMessage().contains("Object Groups"));
		Assert.assertEquals(1, tiles.snapshot().getGroups().size());

		GroupShare.Result wrongObjects = objects.importShare(GroupKind.OBJECT, tileText);
		Assert.assertFalse(wrongObjects.isChanged());
		Assert.assertTrue(wrongObjects.getMessage().contains("Tile Groups"));
		Assert.assertEquals(1, objects.snapshot().getObjects().size());

		GroupShare.Result again = new MarkerRepository(RuneLiteAPI.GSON).importShare(GroupKind.TILE, tileText);
		Assert.assertTrue(again.isChanged());
		GroupShare.Result duplicate = tiles.importShare(GroupKind.TILE, tileText);
		Assert.assertFalse(duplicate.isChanged());
		Assert.assertTrue(duplicate.getMessage().contains("already marked"));
		Assert.assertEquals(1, tiles.snapshot().getMarkers().size());

		MarkerRepository fresh = new MarkerRepository(RuneLiteAPI.GSON);
		fresh.importShare(GroupKind.OBJECT, objectText);
		ObjectMarker imported = fresh.snapshot().getObjects().get(0);
		Assert.assertEquals("chest", imported.getLabel());
		Assert.assertEquals("Bank", imported.getName());
		Assert.assertEquals(5, imported.getObjectId());
	}

	private static MarkerGroup group(MarkerRepository repository, String name)
	{
		for (MarkerGroup group : repository.snapshot().getGroups())
		{
			if (name.equals(group.getName()))
			{
				return group;
			}
		}
		return null;
	}
}
