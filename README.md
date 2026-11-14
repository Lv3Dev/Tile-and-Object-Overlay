# Tile and Object Markers

Tile indicators, persistent ground markers, and persistent object markers for RuneLite.

## What it does

- Highlights the true tile you are standing on, the tile you are walking to, and the tile under the mouse.
- The destination highlight has its own on/off toggle. It starts on.
- Current-tile, destination, and hovered highlights can be drawn as corners, with their own colours and border widths.
- The true tile can fade out after you arrive and stop moving.
- Ground markers are placed by holding the mark key and right-clicking a tile. The mark key is at the top of General and stays Ctrl, so it does not clash with the Shift hotkey used by other marker plugins. The mark menu is only added while the key is held.
  - **Mark Tile** marks the clicked tile.
  - **Mark Region** uses that tile as one corner. Move the mouse and right-click the opposite corner to fill the rectangle.
  - **Mark Border** does the same and keeps only the perimeter.
  - **Unmark Region** removes the whole region you right-clicked. **Unmark Border** removes the whole border. Tiles belong to that area when they share an edge and use the same style. **Unmark tile** still removes only the one tile under the cursor.
- **Merge borders** is in Tile Style, Region Style, and Border Style. Each one starts on. Touching marks of a style that is on are drawn as one shape. Turn a style off to draw each of its tiles in full, or as corners when Corners only is on. Those settings are the default. A tile, or a tile group, can override them.
- **Label tile** opens the in-game chatbox. The text is drawn on that tile. An empty label clears it. A labelled tile can also be edited from its row in the side panel.
- Tile labels use the Ground Markers font, size, and colour. **Label outline** is on by default and has its own colour. Object labels use the font, size, and colour in the Objects section. Both fonts default to the RuneScape font.
- **Minimap markers** is in General. It starts off. Turn it on to draw every marked tile, region, and border on the minimap. It is one switch for all of them.
- Object markers use the same mark key. Right-click an object while it is held.
  - **Mark Object** and **Unmark Object** add and remove that object.
  - **Label object** opens the in-game chatbox. An empty label clears it. A labelled object can also be edited from its row. Clearing the label does not turn the name or left-click text back on.
- Tile rows and object rows start collapsed. The arrow on the right opens that row.
- An open tile row has Label, then the corner icon, then the merge icon. Label opens the text field underneath. Tile, Fill, and Label squares sit below that. An empty square uses the Tile, Region, or Border settings, or the group's tile or fill colour when the group has one. Right-click a square to go back to that default. Object rows still use their own squares the same way.
- Click an object in the side panel to show Outline, Hull, Tile, and Clickbox under Highlight. Name, Left-click, and Label sit under Name display and stay centred on the row. Label opens a text field. The right-click label options still work. A new mark starts with the outline on. Widths stay in the Objects section. A square on the row overrides that colour. Those styles can be combined.
- The side panel opens on Tiles or Objects. Each has a list and a groups tab. Select rows to create a named group or add them to one. Click a group to make it active so new marks of that kind join it. A tile group and an object group can both be active. Items added to a group are unselected afterwards.
- Tile groups have an eyedropper under the copy icon. It opens one window for that group's tile colour and fill. Tiles that already have their own colour keep it. Right-click a square in the window to clear that one, or right-click the eyedropper to clear both. The corner icon under the eye and the merge icon under the trash set those for every tile in the group.
- Each group has a copy icon. Copy, then use Import beside Create group on the matching groups tab. A copied tile group only imports on Tile Groups, and a copied object group only imports on Object Groups. Tiles or objects that are already marked are left as they are.
- The list filter cycles **Ungrouped**, **Grouped**, and **Show all**. It starts on Ungrouped. Tiles and objects each keep their own filter. Ungrouped lists markers that are not in a group. Grouped lists markers that belong to a group. Show all lists every marker.
- **Quick delete** shows an X on each tile and object. While it is off, removal stays on the right-click menu.
- Hiding the sidebar icon leaves marking and overlays running.
- Markers, labels, and groups are saved locally and loaded again after a logout, world hop, or client restart. Older tile-only files still load.

Drawing overlays underneath the player or NPCs needs the GPU plugin.

## Sideload

From this directory:

```
./gradlew jar
```

The plugin jar is `build/libs/tile-and-object-overlay-1.0.0.jar`. `runelite-plugin.properties` is packaged from `src/main/resources`.

Copy that jar to `%USERPROFILE%\.runelite\sideloaded-plugins\tile-and-object-overlay.jar`, then fully quit and relaunch RuneLite. Remove a leftover `tile-overlay.jar` from that folder so RuneLite does not load both copies.

## Saved markers

Markers are stored at:

`.runelite/plugin-data/tile-and-object-overlay/tile-and-object-overlay-markers.json`

On Windows that is `%USERPROFILE%\.runelite\plugin-data\tile-and-object-overlay\tile-and-object-overlay-markers.json`.

Object markers and object groups are stored in the same file.

A marker file that is not valid JSON is left in place and copied to `tile-and-object-overlay-markers.bak` beside it.

## Develop

```
./gradlew test
./gradlew run
```

`./gradlew run` starts a developer client with this plugin loaded. To log in with a Jagex account, follow [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).
