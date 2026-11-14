/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay;

import com.google.gson.Gson;
import net.runelite.api.coords.WorldPoint;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Assert;
import org.junit.Test;

public class MarkerFileTest
{
	@Test
	public void markersGroupsAndLabelsRoundTrip()
	{
		MarkerFile file = new MarkerFile();
		file.getGroups().add(new MarkerGroup("id", "Home", false, true));
		TileMarker marker = TileMarker.create(
			MarkerLocation.fromWorld(new WorldPoint(3200, 3200, 0)),
			MarkerStyle.BORDER);
		marker.setLabel("altar");
		marker.setGroupId("id");
		file.getMarkers().add(marker);

		Gson gson = RuneLiteAPI.GSON.newBuilder().setPrettyPrinting().create();
		MarkerFile loaded = gson.fromJson(gson.toJson(file), MarkerFile.class);

		Assert.assertEquals(MarkerFile.CURRENT_VERSION, loaded.getVersion());
		Assert.assertEquals("Home", loaded.getGroups().get(0).getName());
		Assert.assertEquals(GroupKind.TILE, loaded.getGroups().get(0).getKind());
		Assert.assertFalse(loaded.getGroups().get(0).isEnabled());
		Assert.assertTrue(loaded.getGroups().get(0).isCollapsed());
		Assert.assertEquals("altar", loaded.getMarkers().get(0).getLabel());
		Assert.assertEquals(MarkerStyle.BORDER, loaded.getMarkers().get(0).getStyle());
		Assert.assertEquals("id", loaded.getMarkers().get(0).getGroupId());
		Assert.assertEquals(marker.location(), loaded.getMarkers().get(0).location());
	}

	@Test
	public void objectsAndObjectGroupsRoundTrip()
	{
		MarkerFile file = new MarkerFile();
		file.getGroups().add(new MarkerGroup("obj", "Rocks", true, false, GroupKind.OBJECT));
		ObjectMarker marker = ObjectMarker.create(ObjectRef.of(1234, new WorldPoint(3200, 3200, 0)), "Rocks");
		marker.setLabel("bank");
		marker.setShowName(true);
		marker.setShowLeftClick(true);
		marker.setShowHull(true);
		marker.setShowClickbox(true);
		marker.setOutlineColour(0xFF102030);
		marker.setTextColour(0x80FFFFFF);
		marker.setGroupId("obj");
		file.getObjects().add(marker);
		Assert.assertTrue(marker.isShowOutline());
		Assert.assertFalse(marker.isShowTile());

		Gson gson = RuneLiteAPI.GSON.newBuilder().setPrettyPrinting().create();
		MarkerFile loaded = gson.fromJson(gson.toJson(file), MarkerFile.class);

		Assert.assertEquals(GroupKind.OBJECT, loaded.getGroups().get(0).getKind());
		Assert.assertEquals("bank", loaded.getObjects().get(0).getLabel());
		Assert.assertTrue(loaded.getObjects().get(0).hasLabel());
		Assert.assertTrue(loaded.getObjects().get(0).isShowName());
		Assert.assertTrue(loaded.getObjects().get(0).isShowLeftClick());
		Assert.assertTrue(loaded.getObjects().get(0).isShowOutline());
		Assert.assertTrue(loaded.getObjects().get(0).isShowHull());
		Assert.assertFalse(loaded.getObjects().get(0).isShowTile());
		Assert.assertTrue(loaded.getObjects().get(0).isShowClickbox());
		Assert.assertEquals(Integer.valueOf(0xFF102030), loaded.getObjects().get(0).getOutlineColour());
		Assert.assertEquals(Integer.valueOf(0x80FFFFFF), loaded.getObjects().get(0).getTextColour());
		Assert.assertNull(loaded.getObjects().get(0).getHullColour());
		Assert.assertEquals("Rocks", loaded.getObjects().get(0).getName());
		Assert.assertEquals(1234, loaded.getObjects().get(0).getObjectId());
		Assert.assertEquals(marker.ref(), loaded.getObjects().get(0).ref());
	}

	@Test
	public void versionOneFileHasNoObjectsAndNoGroupKind()
	{
		String json = "{\"version\":1,\"groups\":[{\"id\":\"id\",\"name\":\"Home\",\"enabled\":true,\"collapsed\":false}],\"markers\":[]}";
		MarkerFile loaded = RuneLiteAPI.GSON.fromJson(json, MarkerFile.class);

		Assert.assertEquals(1, loaded.getVersion());
		Assert.assertTrue(loaded.getObjects() == null || loaded.getObjects().isEmpty());
		Assert.assertNull(loaded.getGroups().get(0).getKind());

		MarkerRepository repository = new MarkerRepository(RuneLiteAPI.GSON);
		repository.replace(loaded);
		Assert.assertEquals(GroupKind.TILE, repository.snapshot().getGroups().get(0).getKind());
		Assert.assertTrue(repository.snapshot().getObjects().isEmpty());
	}

	@Test
	public void objectDisplayTogglesDefaultOffWhenMissing()
	{
		String json = "{\"version\":2,\"objects\":[{\"objectId\":1,\"regionId\":12850,\"regionX\":32,\"regionY\":32,\"plane\":0,\"name\":\"Tree\",\"label\":\"  \"}]}";
		ObjectMarker loaded = RuneLiteAPI.GSON.fromJson(json, MarkerFile.class).getObjects().get(0);

		Assert.assertFalse(loaded.isShowName());
		Assert.assertFalse(loaded.isShowLeftClick());
		Assert.assertFalse(loaded.hasLabel());
		Assert.assertFalse(loaded.isShowOutline());
	}

	@Test
	public void objectsFromBeforeHighlightFlagsKeepTheOutline()
	{
		String json = "{\"version\":2,\"objects\":[{\"objectId\":1,\"regionId\":12850,\"regionX\":32,\"regionY\":32,\"plane\":0,\"name\":\"Tree\"}]}";
		MarkerFile file = RuneLiteAPI.GSON.fromJson(json, MarkerFile.class);
		Assert.assertFalse(file.getObjects().get(0).isShowOutline());

		MarkerRepository repository = new MarkerRepository(RuneLiteAPI.GSON);
		repository.replace(file);
		ObjectMarker marker = repository.snapshot().getObjects().get(0);
		Assert.assertTrue(marker.isShowOutline());
		Assert.assertFalse(marker.isShowHull());
		Assert.assertFalse(marker.isShowTile());
		Assert.assertFalse(marker.isShowClickbox());
	}

	@Test
	public void cornerAndMergeOverridesAreOptional()
	{
		String json = "{\"version\":3,\"markers\":[{\"regionId\":12850,\"regionX\":0,\"regionY\":0,\"plane\":0,\"style\":\"TILE\"}]}";
		MarkerFile loaded = RuneLiteAPI.GSON.fromJson(json, MarkerFile.class);
		Assert.assertEquals(3, loaded.getVersion());
		Assert.assertNull(loaded.getMarkers().get(0).getCornersOnly());
		Assert.assertNull(loaded.getMarkers().get(0).getMergeBorders());
		Assert.assertNull(loaded.getMarkers().get(0).getBorderColour());
		Assert.assertNull(loaded.getMarkers().get(0).getFillColour());
		Assert.assertNull(loaded.getMarkers().get(0).getLabelColour());

		loaded.getMarkers().get(0).setCornersOnly(Boolean.FALSE);
		loaded.getMarkers().get(0).setMergeBorders(Boolean.TRUE);
		loaded.getMarkers().get(0).setBorderColour(0xFF112233);
		loaded.getMarkers().get(0).setLabelColour(0xFFABCDEF);
		loaded.getGroups().add(new MarkerGroup("id", "Home", true, false));
		loaded.getGroups().get(0).setColour(0x80FEDCBA);
		loaded.getGroups().get(0).setFillColour(0x40ABCDEF);
		Gson gson = RuneLiteAPI.GSON.newBuilder().setPrettyPrinting().create();
		MarkerFile round = gson.fromJson(gson.toJson(loaded), MarkerFile.class);
		Assert.assertEquals(3, round.getVersion());
		Assert.assertEquals(MarkerFile.CURRENT_VERSION, new MarkerFile().getVersion());
		Assert.assertEquals(Boolean.FALSE, round.getMarkers().get(0).getCornersOnly());
		Assert.assertEquals(Boolean.TRUE, round.getMarkers().get(0).getMergeBorders());
		Assert.assertEquals(Integer.valueOf(0xFF112233), round.getMarkers().get(0).getBorderColour());
		Assert.assertNull(round.getMarkers().get(0).getFillColour());
		Assert.assertEquals(Integer.valueOf(0xFFABCDEF), round.getMarkers().get(0).getLabelColour());
		Assert.assertEquals(Integer.valueOf(0x80FEDCBA), round.getGroups().get(0).getColour());
		Assert.assertEquals(Integer.valueOf(0x40ABCDEF), round.getGroups().get(0).getFillColour());
	}
}
