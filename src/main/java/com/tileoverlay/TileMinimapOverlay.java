/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * One minimap marker for every marked tile. Regions and borders are stored as
 * tiles, so the same switch covers them. The minimap is part of the viewport
 * interface, which is drawn above the scene, so this overlay is separate.
 */
public class TileMinimapOverlay extends Overlay
{
	private final Client client;
	private final TileOverlayConfig config;
	private final MarkerRepository repository;

	@Inject
	TileMinimapOverlay(Client client, TileOverlayConfig config, MarkerRepository repository)
	{
		this.client = client;
		this.config = config;
		this.repository = repository;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_LOW);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.drawMinimapMarkers())
		{
			return null;
		}
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}
		WorldView top = client.getTopLevelWorldView();
		if (top == null)
		{
			return null;
		}
		int plane = playerPlane(player);
		MarkerSnapshot snapshot = repository.snapshot();
		Map<String, Integer> groupColours = new HashMap<>();
		for (MarkerGroup group : snapshot.getGroups())
		{
			if (group != null && group.getId() != null && group.getColour() != null)
			{
				groupColours.put(group.getId(), group.getColour());
			}
		}
		WorldView playerView = player.getWorldView();
		for (TileMarker marker : snapshot.getMarkers())
		{
			if (!snapshot.isShown(marker))
			{
				continue;
			}
			WorldPoint template = marker.location().toWorldPoint();
			if (template == null || template.getPlane() != plane)
			{
				continue;
			}
			MarkerStyle style = marker.getStyle() == null ? MarkerStyle.TILE : marker.getStyle();
			Integer groupColour = marker.getGroupId() == null ? null : groupColours.get(marker.getGroupId());
			Color colour = MarkerLooks.tileBorder(config, style, marker.getBorderColour(), groupColour);
			if (colour == null)
			{
				continue;
			}
			if (colour.getAlpha() < 180)
			{
				colour = new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), 180);
			}
			Collection<WorldPoint> worlds = WorldPoint.toLocalInstance(top, template);
			if (worlds == null || worlds.isEmpty())
			{
				worlds = Collections.singletonList(template);
			}
			for (WorldPoint world : worlds)
			{
				if (world == null)
				{
					continue;
				}
				LocalPoint local = LocalPoint.fromWorld(top, world);
				if (local == null && playerView != null && playerView != top)
				{
					local = LocalPoint.fromWorld(playerView, world);
				}
				if (local != null)
				{
					drawTile(graphics, local, colour);
				}
			}
		}
		return null;
	}

	private int playerPlane(Player player)
	{
		LocalPoint local = player.getLocalLocation();
		if (local != null)
		{
			WorldPoint template = WorldPoint.fromLocalInstance(client, local);
			if (template != null)
			{
				return template.getPlane();
			}
		}
		WorldPoint world = player.getWorldLocation();
		return world == null ? 0 : world.getPlane();
	}

	/**
	 * The tile outline is the same shape Ground Markers uses. A dot is drawn as
	 * well so a single tile still reads when it sits on the edge of the minimap.
	 */
	private void drawTile(Graphics2D graphics, LocalPoint local, Color colour)
	{
		int x = local.getX() & -Perspective.LOCAL_TILE_SIZE;
		int y = local.getY() & -Perspective.LOCAL_TILE_SIZE;
		int viewId = local.getWorldView();
		Point southWest = Perspective.localToMinimap(client, new LocalPoint(x, y, viewId));
		Point northWest = Perspective.localToMinimap(client, new LocalPoint(x, y + Perspective.LOCAL_TILE_SIZE, viewId));
		Point northEast = Perspective.localToMinimap(client, new LocalPoint(x + Perspective.LOCAL_TILE_SIZE, y + Perspective.LOCAL_TILE_SIZE, viewId));
		Point southEast = Perspective.localToMinimap(client, new LocalPoint(x + Perspective.LOCAL_TILE_SIZE, y, viewId));
		if (southWest != null && northWest != null && northEast != null && southEast != null)
		{
			Polygon polygon = new Polygon();
			polygon.addPoint(southWest.getX(), southWest.getY());
			polygon.addPoint(northWest.getX(), northWest.getY());
			polygon.addPoint(northEast.getX(), northEast.getY());
			polygon.addPoint(southEast.getX(), southEast.getY());
			graphics.setStroke(new BasicStroke(1f));
			graphics.setColor(colour);
			graphics.drawPolygon(polygon);
		}
		Point center = Perspective.localToMinimap(client, local);
		if (center != null)
		{
			OverlayUtil.renderMinimapLocation(graphics, center, colour);
		}
	}
}
