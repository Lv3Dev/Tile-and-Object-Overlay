/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay;

import com.google.inject.Provides;
import com.tileoverlay.ui.TileOverlayPanel;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.ObjectComposition;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.PostMenuSort;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Filepath;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Tile & Object Markers",
	description = "Tile indicators and persistent tile and object markers",
	tags = {"tile", "object", "marker", "overlay", "kasi"},
	enabledByDefault = false,
	internalName = "tile-and-object-overlay"
)
public class TileObjectMarkersPlugin extends Plugin implements TileOverlayPanel.Listener
{
	private static final String CHAT_PREFIX = "Tile and Object Markers: ";

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private TileOverlayConfig config;

	@Inject
	private TileOverlayOverlay overlay;

	@Inject
	private TileMinimapOverlay minimapOverlay;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MarkerRepository repository;

	@Inject
	private MarkerStore store;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private ObjectFamilies families;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ChatboxPanelManager chatboxPanelManager;

	@Inject
	private TileOverlayPanel panel;

	@Inject
	private ObjectLeftClick leftClick;

	private final AtomicBoolean loaded = new AtomicBoolean();
	private final AtomicInteger edits = new AtomicInteger();
	private final AtomicInteger session = new AtomicInteger();
	private volatile boolean stopping;
	private volatile boolean hotkeyDown;
	private boolean familiesRekeyed;

	private NavigationButton navButton;
	private BufferedImage icon;

	private volatile ExecutorService io;

	private final HotkeyListener hotkeyListener = new HotkeyListener(() -> config.markHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			hotkeyDown = true;
		}

		@Override
		public void hotkeyReleased()
		{
			hotkeyDown = false;
		}
	};

	@Provides
	TileOverlayConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TileOverlayConfig.class);
	}

	@Override
	protected void startUp()
	{
		stopping = false;
		loaded.set(false);
		familiesRekeyed = false;
		edits.set(0);
		int sessionId = session.incrementAndGet();
		overlayManager.add(overlay);
		overlayManager.add(minimapOverlay);
		keyManager.registerKeyListener(hotkeyListener);
		panel.setListener(this);
		icon = ImageUtil.loadImageResource(TileObjectMarkersPlugin.class, "icon.png");
		SwingUtilities.invokeLater(panel::install);
		refreshSidebar();
		ExecutorService previous = io;
		ExecutorService next = newIo();
		io = next;
		next.execute(() ->
		{
			if (!finishPrevious(previous) || stopping || session.get() != sessionId)
			{
				return;
			}
			loadFromDisk(sessionId);
		});
	}

	@Override
	protected void shutDown()
	{
		stopping = true;
		hotkeyDown = false;
		overlay.setPending(null, null);
		repository.setHighlighted(Collections.emptySet());
		repository.setHighlightedObjects(Collections.emptySet());
		keyManager.unregisterKeyListener(hotkeyListener);
		overlayManager.remove(overlay);
		overlayManager.remove(minimapOverlay);
		panel.setListener(null);
		removeSidebar();
		if (loaded.get())
		{
			enqueueSave(repository.toFile());
		}

		ExecutorService current = io;
		if (current != null)
		{
			current.shutdown();
		}
	}

	private static ExecutorService newIo()
	{
		return Executors.newSingleThreadExecutor(runnable ->
		{
			Thread thread = new Thread(runnable, "Tile and Object Markers IO");
			thread.setDaemon(false);
			return thread;
		});
	}

	private boolean finishPrevious(ExecutorService previous)
	{
		if (previous == null)
		{
			return true;
		}
		previous.shutdown();
		try
		{
			if (!previous.awaitTermination(30, TimeUnit.SECONDS))
			{
				log.error("Marker save did not finish before the next load");
			}
		}
		catch (InterruptedException ex)
		{
			Thread.currentThread().interrupt();
			return false;
		}
		return true;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (ObjectLeftClick.SWAP_GROUP.equals(event.getGroup()))
		{
			leftClick.forgetSwap(event.getKey());
			return;
		}
		if (!TileOverlayConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		if ("showSidePanel".equals(event.getKey()))
		{
			refreshSidebar();
		}
		if (isPanelColour(event.getKey()) || isObjectDefaultColour(event.getKey()))
		{
			SwingUtilities.invokeLater(panel::refreshColours);
		}
	}

	private static boolean isPanelColour(String key)
	{
		return "groupBorderColour".equals(key)
			|| "tileListTextColour".equals(key)
			|| "objectListTextColour".equals(key);
	}

	private static boolean isObjectDefaultColour(String key)
	{
		return "objectOutlineColour".equals(key)
			|| "objectHullColour".equals(key)
			|| "objectTileColour".equals(key)
			|| "objectClickboxColour".equals(key)
			|| "objectTextColour".equals(key);
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		families.scanChunk();
		if (!families.isComplete() || familiesRekeyed || !loaded.get())
		{
			return;
		}
		familiesRekeyed = true;
		if (repository.rekeyObjects(families::canonical))
		{
			changed();
		}
	}

	@Subscribe(priority = -1000f)
	public void onPostMenuSort(PostMenuSort event)
	{
		if (client.isMenuOpen() || client.isKeyPressed(KeyCode.KC_SHIFT))
		{
			return;
		}
		leftClick.observe(client.getMenu().getMenuEntries());
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!hotkeyDown)
		{
			return;
		}
		MenuEntry entry = event.getMenuEntry();
		if (entry == null)
		{
			return;
		}
		if (isObjectAction(entry.getType()))
		{
			addObjectEntries(entry);
		}
		else if (entry.getType() == MenuAction.WALK)
		{
			addTileEntries(entry);
		}
	}

	private void addTileEntries(MenuEntry walk)
	{
		if (tileOptionPresent(client.getMenu().getMenuEntries()))
		{
			return;
		}

		WorldPoint hovered = overlay.tileAtCursor();
		final WorldPoint worldPoint = hovered != null
			? hovered
			: overlay.tileFromMenu(walk.getWorldViewId(), walk.getParam0(), walk.getParam1());
		if (worldPoint == null)
		{
			return;
		}

		if (overlay.getPendingStart() != null)
		{
			addEntry("Cancel", menuEntry -> cancelPending());
			String fill = overlay.getPendingStyle() == MarkerStyle.BORDER ? "Fill border" : "Fill region";
			addEntry(fill, menuEntry -> finishArea(worldPoint));
			return;
		}

		TileMarker existing = repository.get(worldPoint);
		if (existing != null)
		{
			addEntry("Label tile", menuEntry -> promptLabel(worldPoint));
		}
		addEntry("Mark Border", menuEntry -> beginArea(worldPoint, MarkerStyle.BORDER));
		addEntry("Mark Region", menuEntry -> beginArea(worldPoint, MarkerStyle.REGION));
		if (existing != null)
		{
			if (existing.getStyle() == MarkerStyle.BORDER)
			{
				addEntry("Unmark Border", menuEntry -> unmarkConnected(worldPoint, MarkerStyle.BORDER));
			}
			else if (existing.getStyle() == MarkerStyle.REGION)
			{
				addEntry("Unmark Region", menuEntry -> unmarkConnected(worldPoint, MarkerStyle.REGION));
			}
			MarkerLocation location = existing.location();
			addEntry("Unmark tile", menuEntry ->
			{
				repository.unmark(Collections.singleton(location));
				changed();
			});
		}
		else
		{
			addEntry("Mark Tile", menuEntry ->
			{
				repository.markTile(worldPoint);
				changed();
			});
		}
	}

	private void addObjectEntries(MenuEntry entry)
	{
		TileOverlayOverlay.LocatedObject located = overlay.locateMenuObject(
			entry.getWorldViewId(), entry.getParam0(), entry.getParam1(), entry.getIdentifier());
		if (located == null || objectOptionPresent(located))
		{
			return;
		}
		int sceneId = located.getObjectId();
		WorldPoint worldPoint = located.getWorldPoint();
		String name = objectName(sceneId);
		String target = name == null ? "" : name;
		ObjectMarker existing = repository.findMarkedObject(worldPoint, sceneId, families::canonical);
		if (existing != null)
		{
			ObjectRef ref = existing.ref();
			String labelOption = existing.hasLabel() ? "Edit label" : "Label object";
			addObjectEntry(labelOption, target, located, menuEntry -> promptObjectLabel(ref));
			addObjectEntry("Unmark Object", target, located, menuEntry ->
			{
				repository.unmarkObjects(Collections.singleton(ref));
				changed();
			});
			return;
		}
		int storedId = families.canonical(sceneId);
		addObjectEntry("Mark Object", target, located, menuEntry ->
		{
			repository.markObject(storedId, worldPoint, name);
			changed();
		});
	}

	private boolean objectOptionPresent(TileOverlayOverlay.LocatedObject located)
	{
		for (MenuEntry existing : client.getMenu().getMenuEntries())
		{
			String option = existing.getOption();
			if (!"Mark Object".equals(option) && !"Unmark Object".equals(option)
				&& !"Label object".equals(option) && !"Edit label".equals(option))
			{
				continue;
			}
			if (existing.getIdentifier() == located.getObjectId()
				&& existing.getParam0() == located.getSceneX()
				&& existing.getParam1() == located.getSceneY())
			{
				return true;
			}
		}
		return false;
	}

	private static boolean isObjectAction(MenuAction type)
	{
		return type == MenuAction.GAME_OBJECT_FIRST_OPTION
			|| type == MenuAction.GAME_OBJECT_SECOND_OPTION
			|| type == MenuAction.GAME_OBJECT_THIRD_OPTION
			|| type == MenuAction.GAME_OBJECT_FOURTH_OPTION
			|| type == MenuAction.GAME_OBJECT_FIFTH_OPTION
			|| type == MenuAction.EXAMINE_OBJECT
			|| type == MenuAction.WIDGET_TARGET_ON_GAME_OBJECT;
	}

	@Override
	public void renameGroup(String id, String name)
	{
		clientThread.invoke(() ->
		{
			repository.renameGroup(id, name);
			changed();
		});
	}

	@Override
	public void setGroupEnabled(String id, boolean enabled)
	{
		clientThread.invoke(() ->
		{
			repository.setGroupEnabled(id, enabled);
			changed();
		});
	}

	@Override
	public void setGroupCollapsed(String id, boolean collapsed)
	{
		clientThread.invoke(() ->
		{
			repository.setGroupCollapsed(id, collapsed);
			changed();
		});
	}

	@Override
	public void deleteGroup(String id)
	{
		clientThread.invoke(() ->
		{
			repository.deleteGroup(id);
			changed();
		});
	}

	@Override
	public void assign(Collection<MarkerLocation> locations, String groupId)
	{
		Collection<MarkerLocation> copy = new ArrayList<>(locations);
		clientThread.invoke(() ->
		{
			repository.assign(copy, groupId);
			changed();
		});
	}

	@Override
	public void unassign(Collection<MarkerLocation> locations)
	{
		Collection<MarkerLocation> copy = new ArrayList<>(locations);
		clientThread.invoke(() ->
		{
			repository.unassign(copy);
			changed();
		});
	}

	@Override
	public void unmark(Collection<MarkerLocation> locations)
	{
		Collection<MarkerLocation> copy = new ArrayList<>(locations);
		clientThread.invoke(() ->
		{
			repository.unmark(copy);
			changed();
		});
	}

	@Override
	public void assignObjects(Collection<ObjectRef> refs, String groupId)
	{
		Collection<ObjectRef> copy = new ArrayList<>(refs);
		clientThread.invoke(() ->
		{
			repository.assignObjects(copy, groupId);
			changed();
		});
	}

	@Override
	public void addNamedGroup(GroupKind kind, String name)
	{
		String trimmed = name == null ? "" : name.trim();
		if (trimmed.isEmpty())
		{
			return;
		}
		GroupKind resolved = kind == null ? GroupKind.TILE : kind;
		clientThread.invoke(() ->
		{
			repository.addGroup(trimmed, resolved);
			changed();
		});
	}

	@Override
	public void createNamedTileGroup(String name, Collection<MarkerLocation> locations)
	{
		String trimmed = name == null ? "" : name.trim();
		if (trimmed.isEmpty() || locations == null || locations.isEmpty())
		{
			return;
		}
		Collection<MarkerLocation> copy = new ArrayList<>(locations);
		clientThread.invoke(() ->
		{
			if (repository.createNamedGroupFor(trimmed, copy) != null)
			{
				changed();
			}
		});
	}

	@Override
	public void createNamedObjectGroup(String name, Collection<ObjectRef> refs)
	{
		String trimmed = name == null ? "" : name.trim();
		if (trimmed.isEmpty() || refs == null || refs.isEmpty())
		{
			return;
		}
		Collection<ObjectRef> copy = new ArrayList<>(refs);
		clientThread.invoke(() ->
		{
			if (repository.createNamedObjectGroupFor(trimmed, copy) != null)
			{
				changed();
			}
		});
	}

	@Override
	public void setActiveGroup(String id, boolean active)
	{
		clientThread.invoke(() ->
		{
			repository.setActiveGroup(id, active);
			changed();
		});
	}

	@Override
	public void openSettings()
	{
		SwingUtilities.invokeLater(this::openPluginSettings);
	}

	private void openPluginSettings()
	{
		try
		{
			NavigationButton configButton = configNavigation();
			if (configButton != null)
			{
				clientToolbar.openPanel(configButton);
			}
			SwingUtilities.invokeLater(this::openConfigurationPage);
		}
		catch (RuntimeException ex)
		{
			log.warn("Could not open settings", ex);
		}
	}

	private void openConfigurationPage()
	{
		try
		{
			Class<?> panelClass = Class.forName("net.runelite.client.plugins.config.PluginListPanel");
			Object listPanel = getInjector().getInstance(panelClass);
			Method method = panelClass.getDeclaredMethod("openConfigurationPanel", Plugin.class);
			method.setAccessible(true);
			method.invoke(listPanel, this);
		}
		catch (ReflectiveOperationException ex)
		{
			log.warn("Could not open settings", ex);
		}
	}

	private NavigationButton configNavigation()
	{
		if (pluginManager == null)
		{
			return null;
		}
		for (Plugin plugin : pluginManager.getPlugins())
		{
			if (plugin == null || !"net.runelite.client.plugins.config.ConfigPlugin".equals(plugin.getClass().getName()))
			{
				continue;
			}
			try
			{
				Field field = plugin.getClass().getDeclaredField("navButton");
				field.setAccessible(true);
				Object value = field.get(plugin);
				return value instanceof NavigationButton ? (NavigationButton) value : null;
			}
			catch (ReflectiveOperationException ex)
			{
				log.warn("Could not open settings", ex);
				return null;
			}
		}
		return null;
	}

	@Override
	public void unassignObjects(Collection<ObjectRef> refs)
	{
		Collection<ObjectRef> copy = new ArrayList<>(refs);
		clientThread.invoke(() ->
		{
			repository.unassignObjects(copy);
			changed();
		});
	}

	@Override
	public void unmarkObjects(Collection<ObjectRef> refs)
	{
		Collection<ObjectRef> copy = new ArrayList<>(refs);
		clientThread.invoke(() ->
		{
			repository.unmarkObjects(copy);
			changed();
		});
	}

	@Override
	public void setObjectShowName(ObjectRef ref, boolean show)
	{
		clientThread.invoke(() ->
		{
			repository.setObjectShowName(ref, show);
			changed();
		});
	}

	@Override
	public void setObjectShowLeftClick(ObjectRef ref, boolean show)
	{
		clientThread.invoke(() ->
		{
			repository.setObjectShowLeftClick(ref, show);
			changed();
		});
	}

	@Override
	public void setTileLabel(MarkerLocation location, String label)
	{
		if (location == null)
		{
			return;
		}
		clientThread.invoke(() ->
		{
			repository.setLabel(location.toWorldPoint(), tileLabelText(label));
			changed();
		});
	}

	@Override
	public void previewTileLabel(MarkerLocation location, String label)
	{
		if (location == null)
		{
			return;
		}
		clientThread.invoke(() -> repository.setLabel(location.toWorldPoint(), tileLabelText(label)));
	}

	private static String tileLabelText(String label)
	{
		String resolved = label == null ? "" : label.trim();
		return resolved.isEmpty() ? null : resolved;
	}

	@Override
	public void setTileCorners(MarkerLocation location, Boolean cornersOnly)
	{
		if (location == null)
		{
			return;
		}
		clientThread.invoke(() ->
		{
			repository.setTileCorners(location, cornersOnly);
			changed();
		});
	}

	@Override
	public void setTileMerge(MarkerLocation location, Boolean mergeBorders)
	{
		if (location == null)
		{
			return;
		}
		clientThread.invoke(() ->
		{
			repository.setTileMerge(location, mergeBorders);
			changed();
		});
	}

	@Override
	public void setGroupTileCorners(String groupId, Boolean cornersOnly)
	{
		if (groupId == null)
		{
			return;
		}
		clientThread.invoke(() ->
		{
			repository.setGroupTileCorners(groupId, cornersOnly);
			changed();
		});
	}

	@Override
	public void setGroupTileMerge(String groupId, Boolean mergeBorders)
	{
		if (groupId == null)
		{
			return;
		}
		clientThread.invoke(() ->
		{
			repository.setGroupTileMerge(groupId, mergeBorders);
			changed();
		});
	}

	@Override
	public void setTileColour(MarkerLocation location, TileColourSlot slot, Color colour)
	{
		if (location == null || slot == null)
		{
			return;
		}
		clientThread.invoke(() ->
		{
			repository.setTileColour(location, slot, colour == null ? null : colour.getRGB());
			changed();
		});
	}

	@Override
	public void setGroupColour(String groupId, Color colour)
	{
		if (groupId == null)
		{
			return;
		}
		clientThread.invoke(() ->
		{
			repository.setGroupColour(groupId, colour == null ? null : colour.getRGB());
			changed();
		});
	}

	@Override
	public void setGroupFillColour(String groupId, Color colour)
	{
		if (groupId == null)
		{
			return;
		}
		clientThread.invoke(() ->
		{
			repository.setGroupFillColour(groupId, colour == null ? null : colour.getRGB());
			changed();
		});
	}

	@Override
	public String copyGroup(String id)
	{
		GroupShare.Bundle bundle = repository.shareBundle(id);
		if (bundle == null)
		{
			return null;
		}
		clientThread.invoke(() -> chat(bundle.getNotice()));
		return bundle.getText();
	}

	@Override
	public void importGroup(GroupKind kind, String text, Consumer<String> done)
	{
		clientThread.invoke(() ->
		{
			GroupShare.Result result = repository.importShare(kind, text);
			if (result.isChanged())
			{
				changed();
			}
			if (done != null)
			{
				SwingUtilities.invokeLater(() -> done.accept(result.getMessage()));
			}
		});
	}

	@Override
	public void setObjectLabel(ObjectRef ref, String label)
	{
		clientThread.invoke(() ->
		{
			repository.setObjectLabel(ref, label);
			changed();
		});
	}

	@Override
	public void previewObjectLabel(ObjectRef ref, String label)
	{
		clientThread.invoke(() -> repository.setObjectLabel(ref, label));
	}

	@Override
	public void setObjectShowOutline(ObjectRef ref, boolean show)
	{
		clientThread.invoke(() ->
		{
			repository.setObjectShowOutline(ref, show);
			changed();
		});
	}

	@Override
	public void setObjectShowHull(ObjectRef ref, boolean show)
	{
		clientThread.invoke(() ->
		{
			repository.setObjectShowHull(ref, show);
			changed();
		});
	}

	@Override
	public void setObjectShowTile(ObjectRef ref, boolean show)
	{
		clientThread.invoke(() ->
		{
			repository.setObjectShowTile(ref, show);
			changed();
		});
	}

	@Override
	public void setObjectShowClickbox(ObjectRef ref, boolean show)
	{
		clientThread.invoke(() ->
		{
			repository.setObjectShowClickbox(ref, show);
			changed();
		});
	}

	@Override
	public void setObjectColour(ObjectRef ref, ObjectColourSlot slot, Color colour)
	{
		clientThread.invoke(() ->
		{
			repository.setObjectColour(ref, slot, colour == null ? null : colour.getRGB());
			changed();
		});
	}

	private void addEntry(String option, Consumer<MenuEntry> onClick)
	{
		client.getMenu().createMenuEntry(-1)
			.setOption(option)
			.setTarget("")
			.setType(MenuAction.RUNELITE)
			.onClick(onClick);
	}

	private void addObjectEntry(String option, String target, TileOverlayOverlay.LocatedObject located, Consumer<MenuEntry> onClick)
	{
		client.getMenu().createMenuEntry(-1)
			.setOption(option)
			.setTarget(target == null ? "" : target)
			.setIdentifier(located.getObjectId())
			.setParam0(located.getSceneX())
			.setParam1(located.getSceneY())
			.setType(MenuAction.RUNELITE)
			.onClick(onClick);
	}

	private static boolean tileOptionPresent(MenuEntry[] entries)
	{
		if (entries == null)
		{
			return false;
		}
		for (MenuEntry entry : entries)
		{
			if (entry != null && entry.getType() == MenuAction.RUNELITE && isTileOption(entry.getOption()))
			{
				return true;
			}
		}
		return false;
	}

	private static boolean isTileOption(String option)
	{
		if (option == null)
		{
			return false;
		}
		switch (option)
		{
			case "Mark Tile":
			case "Unmark tile":
			case "Mark Region":
			case "Mark Border":
			case "Label tile":
			case "Fill region":
			case "Fill border":
			case "Cancel":
			case "Unmark Region":
			case "Unmark Border":
				return true;
			default:
				return false;
		}
	}

	private void beginArea(WorldPoint start, MarkerStyle style)
	{
		overlay.setPending(start, style);
		String kind = style == MarkerStyle.BORDER ? "border" : "region";
		chat("Right-click the opposite corner to fill the " + kind + ".");
	}

	private void finishArea(WorldPoint end)
	{
		WorldPoint start = overlay.getPendingStart();
		MarkerStyle style = overlay.getPendingStyle();
		overlay.setPending(null, null);
		if (start == null || end == null || style == null)
		{
			return;
		}
		if (!repository.markArea(start, end, style))
		{
			chat("That area is larger than 64 tiles on a side.");
			return;
		}
		changed();
	}

	private void unmarkConnected(WorldPoint worldPoint, MarkerStyle style)
	{
		repository.unmark(repository.connectedOfStyle(worldPoint, style));
		changed();
	}

	private void cancelPending()
	{
		overlay.setPending(null, null);
		chat("Mark cancelled.");
	}

	private void promptLabel(WorldPoint worldPoint)
	{
		TileMarker marker = repository.get(worldPoint);
		String current = marker == null || marker.getLabel() == null ? "" : marker.getLabel();
		chatboxPanelManager.openTextInput("Tile label")
			.value(current)
			.onDone((Consumer<String>) text -> clientThread.invoke(() -> applyLabel(worldPoint, text)))
			.build();
	}

	private void promptObjectLabel(ObjectRef ref)
	{
		ObjectMarker marker = repository.getObject(ref);
		String current = marker == null || !marker.hasLabel() ? "" : marker.getLabel();
		String prompt = marker != null && marker.hasLabel() ? "Edit label" : "Object label";
		chatboxPanelManager.openTextInput(prompt)
			.value(current)
			.onDone((Consumer<String>) text -> clientThread.invoke(() -> applyObjectLabel(ref, text)))
			.build();
	}

	private void applyLabel(WorldPoint worldPoint, String text)
	{
		String label = text == null ? "" : text.trim();
		repository.setLabel(worldPoint, label.isEmpty() ? null : label);
		changed();
	}

	private void applyObjectLabel(ObjectRef ref, String text)
	{
		String label = text == null ? "" : text.trim();
		repository.setObjectLabel(ref, label.isEmpty() ? null : label);
		changed();
	}

	private String objectName(int objectId)
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
			String name = definition.getName();
			if (name == null)
			{
				return null;
			}
			String trimmed = name.trim();
			if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed))
			{
				return null;
			}
			return trimmed;
		}
		catch (RuntimeException ex)
		{
			return null;
		}
	}

	private void chat(String message)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", CHAT_PREFIX + message, null);
	}

	private void changed()
	{
		edits.incrementAndGet();
		publish();
		if (loaded.get())
		{
			enqueueSave(repository.toFile());
		}
	}

	private void publish()
	{
		if (stopping)
		{
			return;
		}
		MarkerSnapshot snapshot = repository.snapshot();
		SwingUtilities.invokeLater(() -> panel.setSnapshot(snapshot));
	}

	private void loadFromDisk(int sessionId)
	{
		MarkerFile file = null;
		boolean failed = false;
		try
		{
			file = store.read(markerFile());
		}
		catch (IOException ex)
		{
			failed = true;
			log.error("Could not load markers", ex);
		}
		MarkerFile loadedFile = file;
		boolean loadFailed = failed;
		clientThread.invoke(() ->
		{
			if (stopping || session.get() != sessionId)
			{
				return;
			}
			applyLoad(loadedFile, loadFailed);
		});
	}

	private void applyLoad(MarkerFile file, boolean failed)
	{
		if (stopping)
		{
			return;
		}
		if (!failed && file != null && edits.get() == 0)
		{
			repository.replace(file);
			int tiles = file.getMarkers() == null ? 0 : file.getMarkers().size();
			int objects = file.getObjects() == null ? 0 : file.getObjects().size();
			log.info("Loaded {} tile markers and {} object markers", tiles, objects);
		}
		loaded.set(true);
		if (edits.get() > 0)
		{
			enqueueSave(repository.toFile());
		}
		publish();
	}

	private void enqueueSave(MarkerFile file)
	{
		ExecutorService current = io;
		if (current == null || current.isShutdown())
		{
			writeOnFreshThread(file);
			return;
		}
		try
		{
			current.execute(() -> writeQuiet(file));
		}
		catch (RejectedExecutionException ex)
		{
			writeOnFreshThread(file);
		}
	}

	private void writeOnFreshThread(MarkerFile file)
	{
		Thread thread = new Thread(() -> writeQuiet(file), "Tile and Object Markers IO");
		thread.setDaemon(false);
		thread.start();
	}

	private void writeQuiet(MarkerFile file)
	{
		try
		{
			store.write(markerFile(), file);
		}
		catch (IOException ex)
		{
			log.error("Could not save markers", ex);
		}
	}

	private Filepath markerFile() throws IOException
	{
		return getPluginDirectory().joinSegment(MarkerStore.FILE_NAME);
	}

	private void refreshSidebar()
	{
		if (config.showSidePanel())
		{
			if (navButton != null || icon == null)
			{
				return;
			}
			navButton = NavigationButton.builder()
				.tooltip("Tile & Object Markers")
				.icon(icon)
				.priority(7)
				.panel(panel)
				.build();
			clientToolbar.addNavigation(navButton);
			return;
		}
		removeSidebar();
	}

	private void removeSidebar()
	{
		if (navButton == null)
		{
			return;
		}
		clientToolbar.removeNavigation(navButton);
		navButton = null;
	}
}
