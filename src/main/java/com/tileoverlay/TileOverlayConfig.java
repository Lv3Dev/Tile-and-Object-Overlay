/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay;

import java.awt.Color;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(TileOverlayConfig.GROUP)
public interface TileOverlayConfig extends Config
{
	String GROUP = "tileoverlay";

	@ConfigSection(
		name = "General",
		description = "How overlays are drawn and whether the side panel is shown",
		position = 0
	)
	String generalSection = "general";

	@ConfigItem(
		keyName = "markHotkey",
		name = "Mark hotkey",
		description = "Hold this key and right-click a tile. The mark menu only appears while the key is held.",
		position = 0,
		section = generalSection
	)
	default Keybind markHotkey()
	{
		return new Keybind(KeyEvent.VK_UNDEFINED, InputEvent.CTRL_DOWN_MASK);
	}

	@ConfigSection(
		name = "Objects",
		description = "Colours, widths, and text for marked objects. Outline, hull, tile, and clickbox are toggled on each object in the side panel.",
		position = 1
	)
	String objectsSection = "objects";

	@ConfigSection(
		name = "Current Tile",
		description = "The true tile the player is standing on",
		position = 2
	)
	String currentSection = "current";

	@ConfigSection(
		name = "Destination Tile",
		description = "The tile the player is walking to",
		position = 3
	)
	String destinationSection = "destination";

	@ConfigSection(
		name = "Hovered Tile",
		description = "The tile under the mouse",
		position = 4
	)
	String hoveredSection = "hovered";

	@ConfigSection(
		name = "Ground Markers",
		description = "Text drawn on marked tiles",
		position = 5
	)
	String markerSection = "markers";

	@ConfigSection(
		name = "Tile Style",
		description = "Look of tiles marked with Mark Tile",
		position = 6
	)
	String tileStyleSection = "tileStyle";

	@ConfigSection(
		name = "Region Style",
		description = "Look of tiles marked with Mark Region",
		position = 7
	)
	String regionStyleSection = "regionStyle";

	@ConfigSection(
		name = "Border Style",
		description = "Look of tiles marked with Mark Border",
		position = 8
	)
	String borderStyleSection = "borderStyle";

	@ConfigItem(
		keyName = "drawBelowPlayer",
		name = "Draw overlays below player",
		description = "Draws tile overlays underneath the local player. Requires the GPU plugin.",
		position = 2,
		section = generalSection
	)
	default boolean drawBelowPlayer()
	{
		return false;
	}

	@ConfigItem(
		keyName = "drawBelowNpcs",
		name = "Draw overlays below NPCs",
		description = "Draws tile overlays underneath nearby NPCs. Requires the GPU plugin. Limited to the nearest NPCs so busy areas stay responsive.",
		position = 3,
		section = generalSection
	)
	default boolean drawBelowNpcs()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showSidePanel",
		name = "Show side panel icon",
		description = "Shows the Tile and Object Markers icon on the sidebar. Marking and overlays keep working when the panel is hidden.",
		position = 5,
		section = generalSection
	)
	default boolean showSidePanel()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightCurrentTile",
		name = "Highlight true tile",
		description = "Highlights the true tile the player is on, as seen by the server.",
		position = 0,
		section = currentSection
	)
	default boolean highlightCurrentTile()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "currentBorderColour",
		name = "Highlight colour",
		description = "Border colour of the true tile.",
		position = 1,
		section = currentSection
	)
	default Color currentBorderColour()
	{
		return Color.CYAN;
	}

	@Alpha
	@ConfigItem(
		keyName = "currentFillColour",
		name = "Fill colour",
		description = "Fill colour of the true tile.",
		position = 2,
		section = currentSection
	)
	default Color currentFillColour()
	{
		return new Color(0, 255, 255, 50);
	}

	@Range(min = 1, max = 8)
	@ConfigItem(
		keyName = "currentBorderWidth",
		name = "Border width",
		description = "Width of the true tile border.",
		position = 3,
		section = currentSection
	)
	default int currentBorderWidth()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "currentCornersOnly",
		name = "Corners only",
		description = "Draw only the corners of the true tile.",
		position = 5,
		section = currentSection
	)
	default boolean currentCornersOnly()
	{
		return false;
	}

	@Range(min = 8, max = 45)
	@ConfigItem(
		keyName = "currentCornerSize",
		name = "Corner size",
		description = "Length of each corner as a percentage of the tile edge.",
		position = 6,
		section = currentSection
	)
	default int currentCornerSize()
	{
		return 22;
	}

	@ConfigItem(
		keyName = "currentFadeout",
		name = "Fadeout",
		description = "Fade the true tile out after the player reaches it and stops moving.",
		position = 8,
		section = currentSection
	)
	default boolean currentFadeout()
	{
		return false;
	}

	@Range(min = 600, max = 5000)
	@Units(Units.MILLISECONDS)
	@ConfigItem(
		keyName = "currentFadeoutTime",
		name = "Fadeout time",
		description = "How long the true tile stays visible after the player stops. Minimum is one game tick (600ms).",
		position = 9,
		section = currentSection
	)
	default int currentFadeoutTime()
	{
		return 800;
	}

	@ConfigItem(
		keyName = "highlightDestinationTile",
		name = "Highlight destination tile",
		description = "Highlights the tile the player is walking to.",
		position = 0,
		section = destinationSection
	)
	default boolean highlightDestinationTile()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "destinationBorderColour",
		name = "Highlight colour",
		description = "Border colour of the destination tile.",
		position = 1,
		section = destinationSection
	)
	default Color destinationBorderColour()
	{
		return new Color(179, 176, 63);
	}

	@Alpha
	@ConfigItem(
		keyName = "destinationFillColour",
		name = "Fill colour",
		description = "Fill colour of the destination tile.",
		position = 2,
		section = destinationSection
	)
	default Color destinationFillColour()
	{
		return new Color(179, 176, 63, 50);
	}

	@Range(min = 1, max = 8)
	@ConfigItem(
		keyName = "destinationBorderWidth",
		name = "Border width",
		description = "Width of the destination tile border.",
		position = 3,
		section = destinationSection
	)
	default int destinationBorderWidth()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "destinationCornersOnly",
		name = "Corners only",
		description = "Draw only the corners of the destination tile.",
		position = 5,
		section = destinationSection
	)
	default boolean destinationCornersOnly()
	{
		return false;
	}

	@Range(min = 8, max = 45)
	@ConfigItem(
		keyName = "destinationCornerSize",
		name = "Corner size",
		description = "Length of each corner as a percentage of the tile edge.",
		position = 6,
		section = destinationSection
	)
	default int destinationCornerSize()
	{
		return 22;
	}

	@ConfigItem(
		keyName = "highlightHoveredTile",
		name = "Highlight hovered tile",
		description = "Highlights the tile under the mouse.",
		position = 0,
		section = hoveredSection
	)
	default boolean highlightHoveredTile()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "hoveredBorderColour",
		name = "Highlight colour",
		description = "Border colour of the hovered tile.",
		position = 1,
		section = hoveredSection
	)
	default Color hoveredBorderColour()
	{
		return Color.WHITE;
	}

	@Alpha
	@ConfigItem(
		keyName = "hoveredFillColour",
		name = "Fill colour",
		description = "Fill colour of the hovered tile.",
		position = 2,
		section = hoveredSection
	)
	default Color hoveredFillColour()
	{
		return new Color(255, 255, 255, 40);
	}

	@ConfigItem(
		keyName = "hoveredCornersOnly",
		name = "Corners only",
		description = "Draw only the corners of the hovered tile.",
		position = 4,
		section = hoveredSection
	)
	default boolean hoveredCornersOnly()
	{
		return false;
	}

	@Range(min = 8, max = 45)
	@ConfigItem(
		keyName = "hoveredCornerSize",
		name = "Corner size",
		description = "Length of each corner as a percentage of the tile edge.",
		position = 5,
		section = hoveredSection
	)
	default int hoveredCornerSize()
	{
		return 22;
	}

	@ConfigItem(
		keyName = "labelFont",
		name = "Label font",
		description = "Font used for tile labels.",
		position = 0,
		section = markerSection
	)
	default LabelFont labelFont()
	{
		return LabelFont.RUNESCAPE;
	}

	@Range(min = 10, max = 28)
	@ConfigItem(
		keyName = "labelTextSize",
		name = "Label text size",
		description = "Size of text drawn on a labelled tile.",
		position = 1,
		section = markerSection
	)
	default int labelTextSize()
	{
		return 14;
	}

	@Alpha
	@ConfigItem(
		keyName = "labelTextColour",
		name = "Label text colour",
		description = "Colour of text drawn on a labelled tile.",
		position = 2,
		section = markerSection
	)
	default Color labelTextColour()
	{
		return Color.WHITE;
	}

	@Alpha
	@ConfigItem(
		keyName = "tileBorderColour",
		name = "Border colour",
		description = "Border colour of single marked tiles.",
		position = 0,
		section = tileStyleSection
	)
	default Color tileBorderColour()
	{
		return new Color(0, 200, 180);
	}

	@ConfigItem(
		keyName = "tileFillColour",
		name = "Fill colour",
		description = "Fill colour of single marked tiles.",
		position = 1,
		section = tileStyleSection
	)
	default Color tileFillColour()
	{
		return new Color(0, 200, 180);
	}

	@Range(min = 0, max = 255)
	@ConfigItem(
		keyName = "tileFillOpacity",
		name = "Fill opacity",
		description = "How solid the tile fill is. 0 is invisible and 255 is solid.",
		position = 2,
		section = tileStyleSection
	)
	default int tileFillOpacity()
	{
		return 50;
	}

	@ConfigItem(
		keyName = "tileMergeBorders",
		name = "Merge borders",
		description = "Drops shared edges between touching marked tiles so they draw as one shape. Turn this off to draw each tile's full border, or its corners when Corners only is on.",
		position = 4,
		section = tileStyleSection
	)
	default boolean tileMergeBorders()
	{
		return true;
	}

	@ConfigItem(
		keyName = "tileCornersOnly",
		name = "Corners only",
		description = "Draw only the corners of single marked tiles.",
		position = 5,
		section = tileStyleSection
	)
	default boolean tileCornersOnly()
	{
		return false;
	}

	@Range(min = 8, max = 45)
	@ConfigItem(
		keyName = "tileCornerSize",
		name = "Corner size",
		description = "Length of each corner as a percentage of the tile edge.",
		position = 6,
		section = tileStyleSection
	)
	default int tileCornerSize()
	{
		return 22;
	}

	@Alpha
	@ConfigItem(
		keyName = "regionBorderColour",
		name = "Border colour",
		description = "Border colour of region marks.",
		position = 0,
		section = regionStyleSection
	)
	default Color regionBorderColour()
	{
		return new Color(230, 140, 40);
	}

	@ConfigItem(
		keyName = "regionFillColour",
		name = "Fill colour",
		description = "Fill colour of region marks.",
		position = 1,
		section = regionStyleSection
	)
	default Color regionFillColour()
	{
		return new Color(230, 140, 40);
	}

	@Range(min = 0, max = 255)
	@ConfigItem(
		keyName = "regionFillOpacity",
		name = "Fill opacity",
		description = "How solid the region fill is. 0 is invisible and 255 is solid.",
		position = 2,
		section = regionStyleSection
	)
	default int regionFillOpacity()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "regionMergeBorders",
		name = "Merge borders",
		description = "Drops shared edges between touching region tiles so they draw as one shape. Turn this off to draw each tile's full border, or its corners when Corners only is on.",
		position = 4,
		section = regionStyleSection
	)
	default boolean regionMergeBorders()
	{
		return true;
	}

	@ConfigItem(
		keyName = "regionCornersOnly",
		name = "Corners only",
		description = "Draw only the corners of region marks.",
		position = 5,
		section = regionStyleSection
	)
	default boolean regionCornersOnly()
	{
		return false;
	}

	@Range(min = 8, max = 45)
	@ConfigItem(
		keyName = "regionCornerSize",
		name = "Corner size",
		description = "Length of each corner as a percentage of the tile edge.",
		position = 6,
		section = regionStyleSection
	)
	default int regionCornerSize()
	{
		return 22;
	}

	@Alpha
	@ConfigItem(
		keyName = "borderStyleBorderColour",
		name = "Border colour",
		description = "Border colour of border marks.",
		position = 0,
		section = borderStyleSection
	)
	default Color borderStyleBorderColour()
	{
		return new Color(220, 70, 70);
	}

	@ConfigItem(
		keyName = "borderStyleFillColour",
		name = "Fill colour",
		description = "Fill colour of border marks.",
		position = 1,
		section = borderStyleSection
	)
	default Color borderStyleFillColour()
	{
		return new Color(220, 70, 70);
	}

	@Range(min = 0, max = 255)
	@ConfigItem(
		keyName = "borderStyleFillOpacity",
		name = "Fill opacity",
		description = "How solid the border-mark fill is. 0 is invisible and 255 is solid.",
		position = 2,
		section = borderStyleSection
	)
	default int borderStyleFillOpacity()
	{
		return 30;
	}

	@ConfigItem(
		keyName = "borderMergeBorders",
		name = "Merge borders",
		description = "Drops shared edges between touching border tiles so they draw as one shape. Turn this off to draw each tile's full border, or its corners when Corners only is on.",
		position = 4,
		section = borderStyleSection
	)
	default boolean borderMergeBorders()
	{
		return true;
	}

	@ConfigItem(
		keyName = "borderStyleCornersOnly",
		name = "Corners only",
		description = "Draw only the corners of border marks.",
		position = 5,
		section = borderStyleSection
	)
	default boolean borderStyleCornersOnly()
	{
		return false;
	}

	@Range(min = 8, max = 45)
	@ConfigItem(
		keyName = "borderStyleCornerSize",
		name = "Corner size",
		description = "Length of each corner as a percentage of the tile edge.",
		position = 6,
		section = borderStyleSection
	)
	default int borderStyleCornerSize()
	{
		return 22;
	}

	@Alpha
	@ConfigItem(
		keyName = "objectOutlineColour",
		name = "Outline colour",
		description = "Colour of the model outline.",
		position = 0,
		section = objectsSection
	)
	default Color objectOutlineColour()
	{
		return new Color(255, 160, 64, 255);
	}

	@Range(min = 1, max = 8)
	@ConfigItem(
		keyName = "objectOutlineWidth",
		name = "Outline width",
		description = "Width of the model outline.",
		position = 1,
		section = objectsSection
	)
	default int objectOutlineWidth()
	{
		return 2;
	}

	@Alpha
	@ConfigItem(
		keyName = "objectHullColour",
		name = "Hull colour",
		description = "Colour of the convex hull.",
		position = 3,
		section = objectsSection
	)
	default Color objectHullColour()
	{
		return new Color(90, 190, 255, 255);
	}

	@Range(min = 1, max = 8)
	@ConfigItem(
		keyName = "objectHullWidth",
		name = "Hull width",
		description = "Border width of the convex hull.",
		position = 4,
		section = objectsSection
	)
	default int objectHullWidth()
	{
		return 2;
	}

	@Alpha
	@ConfigItem(
		keyName = "objectClickboxColour",
		name = "Clickbox colour",
		description = "Colour of the clickbox.",
		position = 6,
		section = objectsSection
	)
	default Color objectClickboxColour()
	{
		return new Color(180, 120, 255, 255);
	}

	@Range(min = 1, max = 8)
	@ConfigItem(
		keyName = "objectClickboxWidth",
		name = "Clickbox width",
		description = "Border width of the clickbox.",
		position = 7,
		section = objectsSection
	)
	default int objectClickboxWidth()
	{
		return 2;
	}

	@Alpha
	@ConfigItem(
		keyName = "objectTileColour",
		name = "Tile colour",
		description = "Colour of the tile under the object.",
		position = 9,
		section = objectsSection
	)
	default Color objectTileColour()
	{
		return new Color(255, 196, 80, 70);
	}

	@ConfigItem(
		keyName = "objectLabelFont",
		name = "Label font",
		description = "Font used for text drawn on a marked object.",
		position = 11,
		section = objectsSection
	)
	default LabelFont objectLabelFont()
	{
		return LabelFont.RUNESCAPE;
	}

	@Range(min = 8, max = 32)
	@ConfigItem(
		keyName = "objectTextSize",
		name = "Label text size",
		description = "Size of text drawn on a marked object.",
		position = 12,
		section = objectsSection
	)
	default int objectTextSize()
	{
		return 14;
	}

	@Alpha
	@ConfigItem(
		keyName = "objectTextColour",
		name = "Label text colour",
		description = "Colour of text drawn on a marked object.",
		position = 13,
		section = objectsSection
	)
	default Color objectTextColour()
	{
		return Color.WHITE;
	}

	@ConfigItem(
		keyName = "objectTextPosition",
		name = "Text position",
		description = "Where the label sits on the object.",
		position = 14,
		section = objectsSection
	)
	default ObjectTextPosition objectTextPosition()
	{
		return ObjectTextPosition.MIDDLE;
	}

	@Range(min = -80, max = 80)
	@Units(Units.PIXELS)
	@ConfigItem(
		keyName = "objectTextOffset",
		name = "Text offset",
		description = "Moves the label down when positive and up when negative.",
		position = 15,
		section = objectsSection
	)
	default int objectTextOffset()
	{
		return 0;
	}

	@ConfigSection(
		name = "Panel Settings",
		description = "Colours used by the side panel",
		position = 9
	)
	String panelSection = "panel";

	@Alpha
	@ConfigItem(
		keyName = "groupBorderColour",
		name = "Group border colour",
		description = "Border colour of groups in the side panel.",
		position = 0,
		section = panelSection
	)
	default Color groupBorderColour()
	{
		return new Color(62, 66, 74);
	}

	@Alpha
	@ConfigItem(
		keyName = "tileListTextColour",
		name = "Tile text colour",
		description = "Text colour of tile rows in the side panel.",
		position = 2,
		section = panelSection
	)
	default Color tileListTextColour()
	{
		return Color.WHITE;
	}

	@Alpha
	@ConfigItem(
		keyName = "objectListTextColour",
		name = "Object text colour",
		description = "Text colour of object rows in the side panel.",
		position = 3,
		section = panelSection
	)
	default Color objectListTextColour()
	{
		return Color.WHITE;
	}
}
