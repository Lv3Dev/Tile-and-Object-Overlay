/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.Model;
import net.runelite.api.NPC;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.api.WorldView;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.ColorUtil;

@Slf4j
public class TileOverlayOverlay extends Overlay
{
	private static final int MAX_NPCS = 15;
	private static final int VIEW_RANGE = 128;
	private static final float MARKER_STROKE = 2f;
	private static final int HOVERED_STROKE = 2;
	private static final Color PREVIEW_BORDER = new Color(255, 255, 255, 210);
	private static final Color PREVIEW_FILL = new Color(255, 255, 255, 35);
	private static final Color SELECT_BORDER = new Color(255, 255, 255, 230);
	private static final Color SELECT_FILL = new Color(255, 255, 255, 28);

	private final Client client;
	private final TileOverlayConfig config;
	private final MarkerRepository repository;
	private final ObjectFamilies families;
	private final ModelOutlineRenderer modelOutlineRenderer;
	private final ObjectLeftClick leftClick;

	private WorldPoint pendingStart;
	private MarkerStyle pendingStyle;

	private WorldPoint lastPlayerPosition = new WorldPoint(0, 0, 0);
	private int lastTickPlayerMoved;
	private long lastTimePlayerStoppedMoving;

	private Font tileLabelFont;
	private int tileLabelFontSize = -1;
	private LabelFont tileLabelFontStyle;
	private Font objectLabelFont;
	private int objectLabelFontSize = -1;
	private LabelFont objectLabelFontStyle;

	private int[] projectedX = new int[256];
	private int[] projectedY = new int[256];
	private boolean punchFailed;
	private boolean objectRenderFailed;

	@Inject
	TileOverlayOverlay(Client client, TileOverlayConfig config, MarkerRepository repository, ObjectFamilies families, ModelOutlineRenderer modelOutlineRenderer, ObjectLeftClick leftClick)
	{
		this.client = client;
		this.config = config;
		this.repository = repository;
		this.families = families;
		this.modelOutlineRenderer = modelOutlineRenderer;
		this.leftClick = leftClick;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(PRIORITY_MED);
	}

	void setPending(WorldPoint start, MarkerStyle style)
	{
		pendingStart = start;
		pendingStyle = style;
	}

	WorldPoint getPendingStart()
	{
		return pendingStart;
	}

	MarkerStyle getPendingStyle()
	{
		return pendingStyle;
	}

	WorldPoint tileAtCursor()
	{
		Player player = client.getLocalPlayer();
		return player == null ? null : hoveredWorld(player);
	}

	/**
	 * World point for a Walk here menu entry. Scene x and y are the entry's params.
	 */
	WorldPoint tileFromMenu(int worldViewId, int sceneX, int sceneY)
	{
		WorldView view = client.getWorldView(worldViewId);
		if (view == null)
		{
			view = client.getTopLevelWorldView();
		}
		if (view == null)
		{
			return null;
		}
		return worldFromScene(view, sceneX, sceneY);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}

		Stroke previousStroke = graphics.getStroke();
		Font previousFont = graphics.getFont();
		Composite previousComposite = graphics.getComposite();
		Object previousAa = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		try
		{
			MarkerSnapshot snapshot = repository.snapshot();
			int plane = viewPlane(player);
			Set<Long> merged = mergedPacks(snapshot, plane);
			Map<String, Integer> groupColours = groupColours(snapshot);
			Map<String, Integer> groupFills = groupFills(snapshot);

			for (TileMarker marker : snapshot.getMarkers())
			{
				if (marker.getPlane() != plane || !snapshot.isShown(marker))
				{
					continue;
				}
				renderMarker(graphics, marker, merged, false, groupColours, groupFills);
			}
			for (TileMarker marker : snapshot.getMarkers())
			{
				if (marker.getPlane() != plane || !snapshot.isShown(marker))
				{
					continue;
				}
				renderLabels(graphics, marker);
			}

			renderObjects(graphics, snapshot, plane, false);
			renderPreview(graphics, player);
			renderHovered(graphics, player);
			renderDestination(graphics);
			renderCurrent(graphics, player);

			for (TileMarker marker : snapshot.getMarkers())
			{
				if (marker.getPlane() != plane
					|| !snapshot.isShown(marker)
					|| !snapshot.getHighlighted().contains(marker.location()))
				{
					continue;
				}
				renderMarker(graphics, marker, merged, true, groupColours, groupFills);
			}

			if (config.objectSelected())
			{
				renderObjects(graphics, snapshot, plane, true);
			}

			if ((config.drawBelowPlayer() || config.drawBelowNpcs()) && client.isGpu())
			{
				punchActors(graphics, player);
			}
		}
		finally
		{
			graphics.setStroke(previousStroke);
			graphics.setFont(previousFont);
			graphics.setComposite(previousComposite);
			if (previousAa != null)
			{
				graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, previousAa);
			}
		}
		return null;
	}

	private Set<Long> mergedPacks(MarkerSnapshot snapshot, int plane)
	{
		Set<Long> packs = new HashSet<>();
		for (TileMarker marker : snapshot.getMarkers())
		{
			if (marker.getPlane() != plane || !snapshot.isShown(marker))
			{
				continue;
			}
			MarkerStyle style = marker.getStyle() == null ? MarkerStyle.TILE : marker.getStyle();
			if (!MarkerLooks.merge(config, style, marker.getMergeBorders()))
			{
				continue;
			}
			packs.add(marker.location().toTilePoint().pack());
		}
		return packs;
	}

	private void renderMarker(Graphics2D graphics, TileMarker marker, Set<Long> merged, boolean selection, Map<String, Integer> groupColours, Map<String, Integer> groupFills)
	{
		WorldPoint template = marker.location().toWorldPoint();
		if (!inView(template))
		{
			return;
		}

		TilePoint tile = new TilePoint(template.getX(), template.getY(), template.getPlane());
		MarkerStyle style = marker.getStyle() == null ? MarkerStyle.TILE : marker.getStyle();
		boolean merge = !selection && MarkerLooks.merge(config, style, marker.getMergeBorders());
		Set<Long> occupied = merge ? merged : Collections.singleton(tile.pack());
		boolean south = TileGeometry.isOuter(occupied, tile.getX(), tile.getY(), tile.getPlane(), TileGeometry.Edge.SOUTH);
		boolean east = TileGeometry.isOuter(occupied, tile.getX(), tile.getY(), tile.getPlane(), TileGeometry.Edge.EAST);
		boolean north = TileGeometry.isOuter(occupied, tile.getX(), tile.getY(), tile.getPlane(), TileGeometry.Edge.NORTH);
		boolean west = TileGeometry.isOuter(occupied, tile.getX(), tile.getY(), tile.getPlane(), TileGeometry.Edge.WEST);

		Color border;
		Color fill;
		float stroke;
		boolean corners;
		int cornerSize;
		if (selection)
		{
			border = SELECT_BORDER;
			fill = SELECT_FILL;
			stroke = MARKER_STROKE;
			corners = false;
			cornerSize = 0;
			south = east = north = west = true;
		}
		else
		{
			Integer groupColour = marker.getGroupId() == null ? null : groupColours.get(marker.getGroupId());
			Integer groupFill = marker.getGroupId() == null ? null : groupFills.get(marker.getGroupId());
			border = MarkerLooks.tileBorder(config, style, marker.getBorderColour(), groupColour);
			fill = MarkerLooks.tileFill(config, style, marker.getFillColour(), groupColour, groupFill);
			stroke = MARKER_STROKE;
			corners = MarkerLooks.cornersOnly(config, style, marker.getCornersOnly());
			cornerSize = MarkerLooks.cornerSize(config, style);
		}

		boolean turnSw = corners && TileGeometry.isTurn(occupied, tile.getX(), tile.getY(), tile.getPlane(), TileGeometry.Corner.SW);
		boolean turnSe = corners && TileGeometry.isTurn(occupied, tile.getX(), tile.getY(), tile.getPlane(), TileGeometry.Corner.SE);
		boolean turnNe = corners && TileGeometry.isTurn(occupied, tile.getX(), tile.getY(), tile.getPlane(), TileGeometry.Corner.NE);
		boolean turnNw = corners && TileGeometry.isTurn(occupied, tile.getX(), tile.getY(), tile.getPlane(), TileGeometry.Corner.NW);

		for (LocalPoint local : localsFor(template))
		{
			Polygon poly = Perspective.getCanvasTilePoly(client, local);
			if (poly == null)
			{
				continue;
			}
			paintShape(graphics, poly, border, fill, stroke, corners, cornerSize, south, east, north, west, turnSw, turnSe, turnNe, turnNw);
		}
	}

	private void renderLabels(Graphics2D graphics, TileMarker marker)
	{
		String label = marker.getLabel();
		if (label == null || label.isEmpty() || !inView(marker.location().toWorldPoint()))
		{
			return;
		}
		graphics.setFont(tileLabelFont(config.labelTextSize()));
		Color colour = MarkerLooks.tileLabel(config, marker.getLabelColour());
		for (LocalPoint local : localsFor(marker.location().toWorldPoint()))
		{
			Point point = Perspective.getCanvasTextLocation(client, graphics, local, label, 0);
			if (point != null)
			{
				drawTileLabel(graphics, point, label, colour);
			}
		}
	}

	private void drawTileLabel(Graphics2D graphics, Point point, String label, Color colour)
	{
		if (colour == null)
		{
			return;
		}
		if (!config.labelOutline())
		{
			OverlayUtil.renderTextLocation(graphics, point, label, colour);
			return;
		}
		Color edge = config.labelOutlineColour();
		int x = point.getX();
		int y = point.getY();
		if (edge != null)
		{
			graphics.setColor(edge);
			for (int dx = -1; dx <= 1; dx++)
			{
				for (int dy = -1; dy <= 1; dy++)
				{
					if (dx != 0 || dy != 0)
					{
						graphics.drawString(label, x + dx, y + dy);
					}
				}
			}
		}
		graphics.setColor(colour);
		graphics.drawString(label, x, y);
	}

	private static Map<String, Integer> groupColours(MarkerSnapshot snapshot)
	{
		Map<String, Integer> colours = new HashMap<>();
		for (MarkerGroup group : snapshot.getGroups())
		{
			if (group != null && group.getId() != null && group.getColour() != null)
			{
				colours.put(group.getId(), group.getColour());
			}
		}
		return colours;
	}

	private static Map<String, Integer> groupFills(MarkerSnapshot snapshot)
	{
		Map<String, Integer> colours = new HashMap<>();
		for (MarkerGroup group : snapshot.getGroups())
		{
			if (group != null && group.getId() != null && group.getFillColour() != null)
			{
				colours.put(group.getId(), group.getFillColour());
			}
		}
		return colours;
	}

	private void renderPreview(Graphics2D graphics, Player player)
	{
		if (pendingStart == null || pendingStyle == null)
		{
			return;
		}
		WorldPoint end = hoveredWorld(player);
		if (end == null)
		{
			end = pendingStart;
		}
		boolean perimeter = pendingStyle == MarkerStyle.BORDER;
		List<TilePoint> tiles = TileGeometry.area(
			pendingStart.getX(), pendingStart.getY(),
			end.getX(), end.getY(),
			pendingStart.getPlane(),
			perimeter);
		if (tiles == null)
		{
			tiles = Collections.singletonList(new TilePoint(pendingStart.getX(), pendingStart.getY(), pendingStart.getPlane()));
		}
		Set<Long> occupied = new HashSet<>();
		for (TilePoint tile : tiles)
		{
			occupied.add(tile.pack());
		}
		for (TilePoint tile : tiles)
		{
			WorldPoint world = new WorldPoint(tile.getX(), tile.getY(), tile.getPlane());
			boolean south = TileGeometry.isOuter(occupied, tile.getX(), tile.getY(), tile.getPlane(), TileGeometry.Edge.SOUTH);
			boolean east = TileGeometry.isOuter(occupied, tile.getX(), tile.getY(), tile.getPlane(), TileGeometry.Edge.EAST);
			boolean north = TileGeometry.isOuter(occupied, tile.getX(), tile.getY(), tile.getPlane(), TileGeometry.Edge.NORTH);
			boolean west = TileGeometry.isOuter(occupied, tile.getX(), tile.getY(), tile.getPlane(), TileGeometry.Edge.WEST);
			for (LocalPoint local : localsFor(world))
			{
				Polygon poly = Perspective.getCanvasTilePoly(client, local);
				if (poly != null)
				{
					paintShape(graphics, poly, PREVIEW_BORDER, PREVIEW_FILL, MARKER_STROKE, false, 0,
						south, east, north, west, false, false, false, false);
				}
			}
		}
	}

	private void renderHovered(Graphics2D graphics, Player player)
	{
		if (!config.highlightHoveredTile())
		{
			return;
		}
		WorldView worldView = player.getWorldView();
		Tile tile = worldView == null ? null : worldView.getSelectedSceneTile();
		if (tile == null)
		{
			return;
		}
		renderIndicator(
			graphics,
			tile.getLocalLocation(),
			config.hoveredBorderColour(),
			config.hoveredFillColour(),
			HOVERED_STROKE,
			config.hoveredCornersOnly(),
			config.hoveredCornerSize());
	}

	private void renderDestination(Graphics2D graphics)
	{
		if (!config.highlightDestinationTile())
		{
			return;
		}
		renderIndicator(
			graphics,
			client.getLocalDestinationLocation(),
			config.destinationBorderColour(),
			config.destinationFillColour(),
			config.destinationBorderWidth(),
			config.destinationCornersOnly(),
			config.destinationCornerSize());
	}

	private void renderCurrent(Graphics2D graphics, Player player)
	{
		if (!config.highlightCurrentTile())
		{
			return;
		}

		WorldPoint playerPos = player.getWorldLocation();
		if (playerPos == null)
		{
			return;
		}
		if (!playerPos.equals(lastPlayerPosition))
		{
			lastTickPlayerMoved = client.getTickCount();
		}
		else if (lastTickPlayerMoved + 1 == client.getTickCount())
		{
			lastTimePlayerStoppedMoving = System.currentTimeMillis();
		}
		lastPlayerPosition = playerPos;

		LocalPoint local = LocalPoint.fromWorld(player.getWorldView(), playerPos);
		if (local == null)
		{
			return;
		}

		Color border = config.currentBorderColour();
		Color fill = config.currentFillColour();
		if (!config.currentFadeout() || client.getTickCount() - lastTickPlayerMoved <= 1)
		{
			renderIndicator(graphics, local, border, fill, config.currentBorderWidth(), config.currentCornersOnly(), config.currentCornerSize());
			return;
		}

		long sinceStop = System.currentTimeMillis() - lastTimePlayerStoppedMoving;
		int fadeoutTime = config.currentFadeoutTime() - Constants.GAME_TICK_LENGTH;
		if (fadeoutTime <= 0 || sinceStop >= fadeoutTime)
		{
			return;
		}
		double opacity = 1.0d - Math.pow(sinceStop / (double) fadeoutTime, 2);
		renderIndicator(
			graphics,
			local,
			ColorUtil.colorWithAlpha(border, (int) (opacity * border.getAlpha())),
			ColorUtil.colorWithAlpha(fill, (int) (opacity * fill.getAlpha())),
			config.currentBorderWidth(),
			config.currentCornersOnly(),
			config.currentCornerSize());
	}

	private void renderIndicator(Graphics2D graphics, LocalPoint local, Color border, Color fill, float stroke, boolean cornersOnly, int cornerSize)
	{
		if (local == null)
		{
			return;
		}
		Polygon poly = Perspective.getCanvasTilePoly(client, local);
		if (poly == null)
		{
			return;
		}
		paintShape(graphics, poly, border, fill, stroke, cornersOnly, cornerSize,
			true, true, true, true, true, true, true, true);
	}

	private void paintShape(
		Graphics2D graphics,
		Polygon poly,
		Color border,
		Color fill,
		float strokeWidth,
		boolean cornersOnly,
		int cornerPercent,
		boolean south,
		boolean east,
		boolean north,
		boolean west,
		boolean turnSw,
		boolean turnSe,
		boolean turnNe,
		boolean turnNw)
	{
		if (fill != null && fill.getAlpha() > 0)
		{
			graphics.setColor(fill);
			graphics.fill(poly);
		}
		if (border == null || border.getAlpha() <= 0 || strokeWidth <= 0)
		{
			return;
		}
		graphics.setColor(border);
		graphics.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		if (poly.npoints != 4)
		{
			graphics.draw(poly);
			return;
		}

		int[] xs = poly.xpoints;
		int[] ys = poly.ypoints;
		if (!cornersOnly)
		{
			if (south)
			{
				graphics.drawLine(xs[0], ys[0], xs[1], ys[1]);
			}
			if (east)
			{
				graphics.drawLine(xs[1], ys[1], xs[2], ys[2]);
			}
			if (north)
			{
				graphics.drawLine(xs[2], ys[2], xs[3], ys[3]);
			}
			if (west)
			{
				graphics.drawLine(xs[3], ys[3], xs[0], ys[0]);
			}
			return;
		}

		float fraction = Math.max(0.08f, Math.min(0.45f, cornerPercent / 100f));
		if (south)
		{
			if (turnSw)
			{
				arm(graphics, xs[0], ys[0], xs[1], ys[1], fraction);
			}
			if (turnSe)
			{
				arm(graphics, xs[1], ys[1], xs[0], ys[0], fraction);
			}
		}
		if (east)
		{
			if (turnSe)
			{
				arm(graphics, xs[1], ys[1], xs[2], ys[2], fraction);
			}
			if (turnNe)
			{
				arm(graphics, xs[2], ys[2], xs[1], ys[1], fraction);
			}
		}
		if (north)
		{
			if (turnNe)
			{
				arm(graphics, xs[2], ys[2], xs[3], ys[3], fraction);
			}
			if (turnNw)
			{
				arm(graphics, xs[3], ys[3], xs[2], ys[2], fraction);
			}
		}
		if (west)
		{
			if (turnNw)
			{
				arm(graphics, xs[3], ys[3], xs[0], ys[0], fraction);
			}
			if (turnSw)
			{
				arm(graphics, xs[0], ys[0], xs[3], ys[3], fraction);
			}
		}
	}

	private static void arm(Graphics2D graphics, int x1, int y1, int x2, int y2, float fraction)
	{
		int x = x1 + Math.round((x2 - x1) * fraction);
		int y = y1 + Math.round((y2 - y1) * fraction);
		graphics.drawLine(x1, y1, x, y);
	}

	int viewPlane(Player player)
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

	boolean inView(WorldPoint template)
	{
		Player player = client.getLocalPlayer();
		if (player == null || template == null)
		{
			return false;
		}
		LocalPoint local = player.getLocalLocation();
		if (local == null)
		{
			return false;
		}
		WorldPoint playerPoint = WorldPoint.fromLocalInstance(client, local);
		if (playerPoint == null || template.getPlane() != playerPoint.getPlane())
		{
			return false;
		}
		WorldView worldView = player.getWorldView();
		if (worldView != null && worldView.isInstance())
		{
			return true;
		}
		return Math.abs(template.getX() - playerPoint.getX()) <= VIEW_RANGE
			&& Math.abs(template.getY() - playerPoint.getY()) <= VIEW_RANGE;
	}

	List<LocalPoint> localsFor(WorldPoint template)
	{
		List<LocalPoint> locals = new ArrayList<>(1);
		WorldView top = client.getTopLevelWorldView();
		Collection<WorldPoint> worlds = WorldPoint.toLocalInstance(top, template);
		if (worlds == null || worlds.isEmpty())
		{
			worlds = Collections.singletonList(template);
		}
		Player player = client.getLocalPlayer();
		WorldView playerView = player == null ? top : player.getWorldView();
		for (WorldPoint world : worlds)
		{
			LocalPoint local = LocalPoint.fromWorld(top, world);
			if (local == null && playerView != null && playerView != top)
			{
				local = LocalPoint.fromWorld(playerView, world);
			}
			if (local != null)
			{
				locals.add(local);
			}
		}
		return locals;
	}

	private WorldPoint hoveredWorld(Player player)
	{
		WorldView worldView = player.getWorldView();
		if (worldView == null)
		{
			return null;
		}
		Tile tile = worldView.getSelectedSceneTile();
		if (tile == null)
		{
			return null;
		}
		LocalPoint local = tile.getLocalLocation();
		if (local == null)
		{
			return null;
		}
		WorldPoint world = WorldPoint.fromLocalInstance(client, local);
		if (world != null)
		{
			return world;
		}
		return WorldPoint.fromLocal(worldView, local.getX(), local.getY(), worldView.getPlane());
	}

	private void punchActors(Graphics2D graphics, Player player)
	{
		try
		{
			if (config.drawBelowPlayer())
			{
				clearActor(graphics, player);
			}
			if (!config.drawBelowNpcs() || player.getWorldView() == null)
			{
				return;
			}
			List<NPC> npcs = new ArrayList<>();
			for (NPC npc : player.getWorldView().npcs())
			{
				if (npc != null && npc.getModel() != null && npc.getLocalLocation() != null)
				{
					npcs.add(npc);
				}
			}
			LocalPoint playerLocal = player.getLocalLocation();
			npcs.sort(Comparator.comparingInt(npc -> npc.getLocalLocation().distanceTo(playerLocal)));
			int limit = Math.min(MAX_NPCS, npcs.size());
			for (int i = 0; i < limit; i++)
			{
				clearActor(graphics, npcs.get(i));
			}
		}
		catch (RuntimeException ex)
		{
			if (!punchFailed)
			{
				punchFailed = true;
				log.debug("Unable to draw overlays below actors", ex);
			}
		}
	}

	private void clearActor(Graphics2D graphics, Actor actor)
	{
		Model model = actor.getModel();
		if (model == null || actor.getLocalLocation() == null || actor.getWorldView() == null)
		{
			return;
		}
		int count = model.getVerticesCount();
		float[] xs = model.getVerticesX();
		float[] ys = model.getVerticesY();
		float[] zs = model.getVerticesZ();
		if (count <= 0 || xs == null || ys == null || zs == null)
		{
			return;
		}
		if (projectedX.length < count)
		{
			projectedX = new int[count];
			projectedY = new int[count];
		}

		LocalPoint local = actor.getLocalLocation();
		int height = Perspective.getFootprintTileHeight(
			client,
			local,
			actor.getWorldView().getPlane(),
			actor.getFootprintSize()) - actor.getAnimationHeightOffset();
		Perspective.modelToCanvas(
			client,
			actor.getWorldView(),
			count,
			local.getX(),
			local.getY(),
			height,
			actor.getCurrentOrientation(),
			xs,
			zs,
			ys,
			projectedX,
			projectedY);

		int faces = model.getFaceCount();
		int[] faceA = model.getFaceIndices1();
		int[] faceB = model.getFaceIndices2();
		int[] faceC = model.getFaceIndices3();
		if (faces <= 0 || faceA == null || faceB == null || faceC == null)
		{
			return;
		}
		byte[] transparency = model.getFaceTransparencies();
		int clipX1 = client.getViewportXOffset();
		int clipY1 = client.getViewportYOffset();
		int clipX2 = clipX1 + client.getViewportWidth();
		int clipY2 = clipY1 + client.getViewportHeight();

		Composite previous = graphics.getComposite();
		Object previousAa = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		graphics.setComposite(AlphaComposite.Clear);
		graphics.setColor(Color.WHITE);
		for (int i = 0; i < faces; i++)
		{
			int i1 = faceA[i];
			int i2 = faceB[i];
			int i3 = faceC[i];
			if (i1 < 0 || i2 < 0 || i3 < 0 || i1 >= count || i2 >= count || i3 >= count)
			{
				continue;
			}
			int x1 = projectedX[i1];
			int y1 = projectedY[i1];
			int x2 = projectedX[i2];
			int y2 = projectedY[i2];
			int x3 = projectedX[i3];
			int y3 = projectedY[i3];
			if (x1 == Integer.MIN_VALUE || x2 == Integer.MIN_VALUE || x3 == Integer.MIN_VALUE)
			{
				continue;
			}
			if (direction(x1, y1, x2, y2, x3, y3) >= 0)
			{
				continue;
			}
			if (transparency != null && i < transparency.length && (transparency[i] & 255) >= 254)
			{
				continue;
			}
			if (!inside(x1, y1, clipX1, clipY1, clipX2, clipY2)
				&& !inside(x2, y2, clipX1, clipY1, clipX2, clipY2)
				&& !inside(x3, y3, clipX1, clipY1, clipX2, clipY2))
			{
				continue;
			}
			graphics.fill(new Polygon(new int[]{x1, x2, x3}, new int[]{y1, y2, y3}, 3));
		}
		graphics.setComposite(previous);
		if (previousAa != null)
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, previousAa);
		}
	}

	private static boolean inside(int x, int y, int x1, int y1, int x2, int y2)
	{
		return x >= x1 && x < x2 && y >= y1 && y < y2;
	}

	private static int direction(int x1, int y1, int x2, int y2, int x3, int y3)
	{
		return (x2 - x1) * (y3 - y1) - (y2 - y1) * (x3 - x1);
	}

	private Font tileLabelFont(int size)
	{
		return cachedFont(config.labelFont(), size, false);
	}

	private Font objectLabelFont(int size)
	{
		return cachedFont(config.objectLabelFont(), size, true);
	}

	private Font cachedFont(LabelFont style, int size, boolean object)
	{
		if (style == null)
		{
			style = LabelFont.RUNESCAPE;
		}
		int clamped = Math.max(8, size);
		if (object)
		{
			if (objectLabelFont == null || objectLabelFontSize != clamped || objectLabelFontStyle != style)
			{
				objectLabelFontSize = clamped;
				objectLabelFontStyle = style;
				objectLabelFont = style.create(clamped);
			}
			return objectLabelFont;
		}
		if (tileLabelFont == null || tileLabelFontSize != clamped || tileLabelFontStyle != style)
		{
			tileLabelFontSize = clamped;
			tileLabelFontStyle = style;
			tileLabelFont = style.create(clamped);
		}
		return tileLabelFont;
	}

	private void renderObjects(Graphics2D graphics, MarkerSnapshot snapshot, int plane, boolean selectionPass)
	{
		for (ObjectMarker marker : snapshot.getObjects())
		{
			if (marker.getPlane() != plane)
			{
				continue;
			}
			boolean selected = snapshot.getHighlightedObjects().contains(marker.ref());
			if (selectionPass)
			{
				if (!selected || !snapshot.isShown(marker))
				{
					continue;
				}
			}
			else if (!snapshot.isShown(marker))
			{
				continue;
			}
			try
			{
				TileObject object = resolveObject(marker);
				if (object == null)
				{
					continue;
				}
				if (selectionPass)
				{
					paintObjectSelection(graphics, object);
				}
				else
				{
					paintObject(graphics, object, marker);
				}
			}
			catch (RuntimeException ex)
			{
				if (!objectRenderFailed)
				{
					objectRenderFailed = true;
					log.debug("Unable to draw an object marker", ex);
				}
			}
		}
	}

	private void paintObject(Graphics2D graphics, TileObject object, ObjectMarker marker)
	{
		if (marker.isShowTile())
		{
			Polygon poly = object.getCanvasTilePoly();
			Color colour = chosenColour(marker.getTileColour(), config.objectTileColour());
			if (poly != null && colour != null)
			{
				graphics.setColor(colour);
				graphics.fillPolygon(poly);
				graphics.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				graphics.drawPolygon(poly);
			}
		}
		if (marker.isShowClickbox())
		{
			strokeShape(graphics, object.getClickbox(), chosenColour(marker.getClickboxColour(), config.objectClickboxColour()), config.objectClickboxWidth());
		}
		if (marker.isShowHull())
		{
			strokeShape(graphics, hull(object), chosenColour(marker.getHullColour(), config.objectHullColour()), config.objectHullWidth());
		}
		if (marker.isShowOutline())
		{
			Color colour = chosenColour(marker.getOutlineColour(), config.objectOutlineColour());
			if (colour != null)
			{
				modelOutlineRenderer.drawOutline(object, config.objectOutlineWidth(), colour, 0);
			}
		}
		List<String> lines = objectLines(object, marker);
		if (!lines.isEmpty())
		{
			graphics.setFont(objectLabelFont(config.objectTextSize()));
			drawObjectLines(graphics, object, lines, chosenColour(marker.getTextColour(), config.objectTextColour()));
		}
	}

	private List<String> objectLines(TileObject object, ObjectMarker marker)
	{
		List<String> lines = new ArrayList<>(3);
		String action = marker.isShowLeftClick() ? leftClickOption(object) : null;
		String name = marker.isShowName() ? objectDisplayName(marker, object.getId()) : null;
		if (name != null && action != null
			&& (action.equalsIgnoreCase(name) || action.toLowerCase().endsWith(" " + name.toLowerCase())))
		{
			lines.add(action);
		}
		else
		{
			if (name != null)
			{
				lines.add(name);
			}
			if (action != null)
			{
				lines.add(action);
			}
		}
		if (marker.hasLabel())
		{
			lines.add(marker.getLabel().trim());
		}
		return lines;
	}

	private String objectDisplayName(ObjectMarker marker, int objectId)
	{
		String stored = cleanName(marker.getName());
		if (stored != null)
		{
			return stored;
		}
		ObjectComposition definition = composition(objectId);
		return definition == null ? null : cleanName(definition.getName());
	}

	private String leftClickOption(TileObject object)
	{
		return leftClick.option(object, composition(object.getId()));
	}

	private ObjectComposition composition(int objectId)
	{
		try
		{
			ObjectComposition definition = client.getObjectDefinition(objectId);
			if (definition == null)
			{
				return null;
			}
			if (definition.getImpostorIds() != null)
			{
				ObjectComposition impostor = definition.getImpostor();
				if (impostor != null)
				{
					definition = impostor;
				}
			}
			return definition;
		}
		catch (RuntimeException ex)
		{
			return null;
		}
	}

	private static String cleanName(String value)
	{
		if (value == null)
		{
			return null;
		}
		String trimmed = value.trim();
		if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed))
		{
			return null;
		}
		return trimmed;
	}

	private void paintObjectSelection(Graphics2D graphics, TileObject object)
	{
		Shape shape = object.getClickbox();
		if (shape == null)
		{
			shape = object.getCanvasTilePoly();
		}
		strokeShape(graphics, shape, config.objectSelectedColour(), 2);
	}

	private static Color chosenColour(Integer custom, Color fallback)
	{
		if (custom != null)
		{
			return new Color(custom, true);
		}
		return fallback;
	}

	private void drawObjectLines(Graphics2D graphics, TileObject object, List<String> lines, Color colour)
	{
		if (colour == null || lines == null || lines.isEmpty())
		{
			return;
		}
		int offset = config.objectTextOffset();
		ObjectTextPosition position = config.objectTextPosition();
		if (position == null)
		{
			position = ObjectTextPosition.MIDDLE;
		}
		FontMetrics metrics = graphics.getFontMetrics();
		int lineHeight = Math.max(1, metrics.getHeight());
		if (position == ObjectTextPosition.ABOVE)
		{
			Point point = object.getCanvasTextLocation(graphics, lines.get(0), 0);
			if (point == null)
			{
				return;
			}
			int firstWidth = metrics.stringWidth(lines.get(0));
			int y = point.getY() + offset;
			for (int i = 0; i < lines.size(); i++)
			{
				String line = lines.get(i);
				int x = point.getX();
				if (i > 0)
				{
					x += (firstWidth - metrics.stringWidth(line)) / 2;
				}
				OverlayUtil.renderTextLocation(graphics, new Point(x, y + i * lineHeight), line, colour);
			}
			return;
		}

		Rectangle bounds = objectBounds(object);
		int centerX;
		int baseline;
		if (bounds == null)
		{
			Point canvas = object.getCanvasLocation();
			if (canvas == null)
			{
				return;
			}
			centerX = canvas.getX();
			baseline = canvas.getY() + offset;
		}
		else
		{
			centerX = bounds.x + bounds.width / 2;
			if (position == ObjectTextPosition.TOP)
			{
				baseline = bounds.y;
			}
			else if (position == ObjectTextPosition.BOTTOM)
			{
				baseline = bounds.y + bounds.height;
			}
			else if (position == ObjectTextPosition.BELOW)
			{
				baseline = bounds.y + bounds.height + metrics.getAscent();
			}
			else
			{
				baseline = bounds.y + bounds.height / 2 + (metrics.getAscent() - metrics.getDescent()) / 2;
			}
			baseline += offset;
		}
		for (int i = 0; i < lines.size(); i++)
		{
			String line = lines.get(i);
			int x = centerX - metrics.stringWidth(line) / 2;
			OverlayUtil.renderTextLocation(graphics, new Point(x, baseline + i * lineHeight), line, colour);
		}
	}

	private Rectangle objectBounds(TileObject object)
	{
		Shape shape = object.getClickbox();
		if (shape == null)
		{
			shape = object.getCanvasTilePoly();
		}
		if (shape == null)
		{
			return null;
		}
		return shape.getBounds();
	}

	private TileObject resolveObject(ObjectMarker marker)
	{
		WorldPoint template = marker.ref().toWorldPoint();
		if (!inView(template))
		{
			return null;
		}
		for (LocalPoint local : localsFor(template))
		{
			TileObject object = objectAt(local, marker.getObjectId());
			if (object != null)
			{
				return object;
			}
		}
		return null;
	}

	private TileObject objectAt(LocalPoint local, int objectId)
	{
		if (local == null)
		{
			return null;
		}
		WorldView top = client.getTopLevelWorldView();
		TileObject found = objectAt(top, local, objectId);
		if (found != null)
		{
			return found;
		}
		Player player = client.getLocalPlayer();
		WorldView playerView = player == null ? null : player.getWorldView();
		if (playerView != null && playerView != top)
		{
			return objectAt(playerView, local, objectId);
		}
		return null;
	}

	/**
	 * Scene object for a menu entry. The id is the scene object's own id,
	 * which stays put when the entry carries a transformed id instead.
	 */
	public LocatedObject locateMenuObject(int worldViewId, int sceneX, int sceneY, int menuObjectId)
	{
		if (menuObjectId < 0)
		{
			return null;
		}
		WorldView view = client.getWorldView(worldViewId);
		if (view == null)
		{
			view = client.getTopLevelWorldView();
		}
		if (view == null)
		{
			return null;
		}
		TileHit hit = findHit(view, sceneX, sceneY, menuObjectId, true);
		if (hit == null)
		{
			hit = findAdjacentDecoration(view, sceneX, sceneY, menuObjectId);
		}
		if (hit == null)
		{
			return null;
		}
		WorldPoint worldPoint = worldFromScene(view, hit.sceneX, hit.sceneY);
		if (worldPoint == null)
		{
			return null;
		}
		return new LocatedObject(hit.object.getId(), worldPoint, hit.sceneX, hit.sceneY);
	}

	private TileObject objectAt(WorldView view, LocalPoint local, int objectId)
	{
		if (local == null)
		{
			return null;
		}
		Tile tile = tileAt(view, local.getSceneX(), local.getSceneY());
		if (tile == null)
		{
			return null;
		}
		TileObject found = scanTile(tile, objectId, true);
		if (found == null && tile.getBridge() != null)
		{
			found = scanTile(tile.getBridge(), objectId, true);
		}
		return found;
	}

	private TileHit findHit(WorldView view, int sceneX, int sceneY, int objectId, boolean includeGameObjects)
	{
		Tile tile = tileAt(view, sceneX, sceneY);
		if (tile == null)
		{
			return null;
		}
		TileObject found = scanTile(tile, objectId, includeGameObjects);
		if (found == null && tile.getBridge() != null)
		{
			found = scanTile(tile.getBridge(), objectId, includeGameObjects);
		}
		if (found == null)
		{
			return null;
		}
		return new TileHit(found, sceneX, sceneY);
	}

	/**
	 * A wall decoration is sometimes activated from the neighbouring tile.
	 * Only wall, decorative, and ground objects are accepted from that tile,
	 * so a nearby scenery object is left alone.
	 */
	private TileHit findAdjacentDecoration(WorldView view, int sceneX, int sceneY, int objectId)
	{
		int[][] neighbours = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		for (int[] neighbour : neighbours)
		{
			TileHit hit = findHit(view, sceneX + neighbour[0], sceneY + neighbour[1], objectId, false);
			if (hit != null)
			{
				return hit;
			}
		}
		return null;
	}

	private Tile tileAt(WorldView view, int sceneX, int sceneY)
	{
		if (view == null || view.getScene() == null)
		{
			return null;
		}
		Tile[][][] tiles = view.getScene().getTiles();
		if (tiles == null)
		{
			return null;
		}
		int plane = view.getPlane();
		if (plane < 0 || plane >= tiles.length || sceneX < 0 || sceneY < 0
			|| sceneX >= tiles[plane].length || tiles[plane][sceneX] == null
			|| sceneY >= tiles[plane][sceneX].length)
		{
			return null;
		}
		return tiles[plane][sceneX][sceneY];
	}

	private WorldPoint worldFromScene(WorldView view, int sceneX, int sceneY)
	{
		LocalPoint local = LocalPoint.fromScene(sceneX, sceneY, view);
		WorldPoint template = WorldPoint.fromLocalInstance(client, local);
		if (template != null)
		{
			return template;
		}
		return WorldPoint.fromLocal(view, local.getX(), local.getY(), view.getPlane());
	}

	private TileObject scanTile(Tile tile, int objectId, boolean includeGameObjects)
	{
		if (tile == null || objectId < 0)
		{
			return null;
		}
		TileObject exact = firstMatch(tile, objectId, includeGameObjects, false);
		if (exact != null)
		{
			return exact;
		}
		return firstMatch(tile, objectId, includeGameObjects, true);
	}

	private TileObject firstMatch(Tile tile, int objectId, boolean includeGameObjects, boolean transformed)
	{
		if (includeGameObjects)
		{
			GameObject[] gameObjects = tile.getGameObjects();
			if (gameObjects != null)
			{
				for (GameObject gameObject : gameObjects)
				{
					if (matches(gameObject, objectId, transformed))
					{
						return gameObject;
					}
				}
			}
		}
		if (matches(tile.getWallObject(), objectId, transformed))
		{
			return tile.getWallObject();
		}
		if (matches(tile.getDecorativeObject(), objectId, transformed))
		{
			return tile.getDecorativeObject();
		}
		if (matches(tile.getGroundObject(), objectId, transformed))
		{
			return tile.getGroundObject();
		}
		return null;
	}

	private boolean matches(TileObject object, int objectId, boolean transformed)
	{
		if (object == null)
		{
			return false;
		}
		if (!transformed)
		{
			return object.getId() == objectId;
		}
		if (object.getId() == objectId)
		{
			return false;
		}
		if (families != null && families.same(object.getId(), objectId))
		{
			return true;
		}
		return ObjectIdentity.same(object.getId(), objectId, impostorIds(object.getId()), impostorIds(objectId));
	}

	private int[] impostorIds(int objectId)
	{
		try
		{
			ObjectComposition definition = client.getObjectDefinition(objectId);
			if (definition == null)
			{
				return null;
			}
			return definition.getImpostorIds();
		}
		catch (RuntimeException ex)
		{
			return null;
		}
	}

	private static final class TileHit
	{
		private final TileObject object;
		private final int sceneX;
		private final int sceneY;

		private TileHit(TileObject object, int sceneX, int sceneY)
		{
			this.object = object;
			this.sceneX = sceneX;
			this.sceneY = sceneY;
		}
	}

	public static final class LocatedObject
	{
		private final int objectId;
		private final WorldPoint worldPoint;
		private final int sceneX;
		private final int sceneY;

		private LocatedObject(int objectId, WorldPoint worldPoint, int sceneX, int sceneY)
		{
			this.objectId = objectId;
			this.worldPoint = worldPoint;
			this.sceneX = sceneX;
			this.sceneY = sceneY;
		}

		public int getObjectId()
		{
			return objectId;
		}

		public WorldPoint getWorldPoint()
		{
			return worldPoint;
		}

		public int getSceneX()
		{
			return sceneX;
		}

		public int getSceneY()
		{
			return sceneY;
		}
	}

	private static Shape hull(TileObject object)
	{
		if (object instanceof GameObject)
		{
			return ((GameObject) object).getConvexHull();
		}
		if (object instanceof GroundObject)
		{
			return ((GroundObject) object).getConvexHull();
		}
		if (object instanceof WallObject)
		{
			WallObject wall = (WallObject) object;
			return combine(wall.getConvexHull(), wall.getConvexHull2());
		}
		if (object instanceof DecorativeObject)
		{
			DecorativeObject decorative = (DecorativeObject) object;
			return combine(decorative.getConvexHull(), decorative.getConvexHull2());
		}
		return null;
	}

	private static Shape combine(Shape first, Shape second)
	{
		if (first == null)
		{
			return second;
		}
		if (second == null)
		{
			return first;
		}
		Area area = new Area(first);
		area.add(new Area(second));
		return area;
	}

	private static void strokeShape(Graphics2D graphics, Shape shape, Color colour, int width)
	{
		if (shape == null || colour == null || width <= 0)
		{
			return;
		}
		graphics.setColor(colour);
		graphics.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.draw(shape);
	}
}
