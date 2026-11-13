/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay;

import java.util.Collection;
import java.util.Collections;
import net.runelite.api.coords.WorldPoint;
import org.junit.Assert;
import org.junit.Test;

public class MarkerRepositoryTest
{
	@Test
	public void deleteGroupWipesItsTiles()
	{
		MarkerRepository repository = new MarkerRepository();
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
		MarkerRepository repository = new MarkerRepository();
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
		MarkerRepository repository = new MarkerRepository();
		WorldPoint point = new WorldPoint(3200, 3200, 0);
		repository.markArea(point, point, MarkerStyle.BORDER);
		repository.markTile(point);
		Assert.assertEquals(MarkerStyle.BORDER, repository.get(point).getStyle());
	}

	@Test
	public void areaLargerThanTheCapIsRejected()
	{
		MarkerRepository repository = new MarkerRepository();
		WorldPoint start = new WorldPoint(3200, 3200, 0);
		WorldPoint end = new WorldPoint(3200 + TileGeometry.MAX_SPAN, 3200, 0);
		Assert.assertFalse(repository.markArea(start, end, MarkerStyle.REGION));
		Assert.assertNull(repository.get(start));
	}

	@Test
	public void disabledGroupIsHiddenFromTheMergeSet()
	{
		MarkerRepository repository = new MarkerRepository();
		WorldPoint point = new WorldPoint(3200, 3200, 0);
		MarkerGroup group = repository.addGroup("Hide");
		repository.markTile(point);
		repository.assign(Collections.singleton(MarkerLocation.fromWorld(point)), group.getId());
		repository.setGroupEnabled(group.getId(), false);

		MarkerSnapshot snapshot = repository.snapshot();
		Assert.assertFalse(snapshot.isShown(snapshot.getMarkers().get(0)));
		Assert.assertTrue(snapshot.visiblePacks(point.getPlane()).isEmpty());
	}

	@Test
	public void newGroupsGetUniqueNames()
	{
		MarkerRepository repository = new MarkerRepository();
		Assert.assertEquals("Group 1", repository.addGroup(null).getName());
		Assert.assertEquals("Group 2", repository.addGroup("").getName());
		Assert.assertEquals("Home", repository.addGroup("Home").getName());
		Assert.assertEquals("Home 2", repository.addGroup("Home").getName());
	}

	@Test
	public void deletingAnObjectGroupRemovesItsObjectsOnly()
	{
		MarkerRepository repository = new MarkerRepository();
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
		MarkerRepository repository = new MarkerRepository();
		WorldPoint point = new WorldPoint(3200, 3200, 0);
		ObjectRef ref = ObjectRef.of(123, point);
		repository.markObject(123, point, "Rocks");
		repository.setObjectLabel(ref, "bank");
		repository.markObject(123, point, "Other");
		Assert.assertEquals("bank", repository.getObject(ref).getLabel());
		Assert.assertEquals("Rocks", repository.getObject(ref).getName());
	}

	@Test
	public void clearingAnObjectLabelLeavesNameAndLeftClickOff()
	{
		MarkerRepository repository = new MarkerRepository();
		WorldPoint point = new WorldPoint(3200, 3200, 0);
		ObjectRef ref = ObjectRef.of(123, point);
		repository.markObject(123, point, "Rocks");
		repository.setObjectShowName(ref, true);
		repository.setObjectShowLeftClick(ref, true);

		repository.setObjectLabel(ref, "bank");
		ObjectMarker labelled = repository.getObject(ref);
		Assert.assertTrue(labelled.hasLabel());
		Assert.assertFalse(labelled.isShowName());
		Assert.assertFalse(labelled.isShowLeftClick());

		repository.setObjectShowName(ref, true);
		repository.setObjectShowLeftClick(ref, true);
		Assert.assertFalse(repository.getObject(ref).isShowName());
		Assert.assertFalse(repository.getObject(ref).isShowLeftClick());

		repository.setObjectLabel(ref, "   ");
		ObjectMarker cleared = repository.getObject(ref);
		Assert.assertFalse(cleared.hasLabel());
		Assert.assertNull(cleared.getLabel());
		Assert.assertFalse(cleared.isShowName());
		Assert.assertFalse(cleared.isShowLeftClick());

		repository.setObjectShowName(ref, true);
		repository.setObjectShowLeftClick(ref, true);
		Assert.assertTrue(repository.getObject(ref).isShowName());
		Assert.assertTrue(repository.getObject(ref).isShowLeftClick());
		Assert.assertTrue(repository.getObject(ref).isShowOutline());
	}

	@Test
	public void newObjectMarksStartWithOutlineOnlyAndFlagsRoundTrip()
	{
		MarkerRepository repository = new MarkerRepository();
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

		MarkerRepository loaded = new MarkerRepository();
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
		MarkerRepository repository = new MarkerRepository();
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
		MarkerRepository repository = new MarkerRepository();
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
		MarkerRepository repository = new MarkerRepository();
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
		MarkerRepository repository = new MarkerRepository();
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
		MarkerRepository repository = new MarkerRepository();
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
}
