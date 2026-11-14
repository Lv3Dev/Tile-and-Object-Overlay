/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay.ui;

import com.tileoverlay.GroupKind;
import com.tileoverlay.GroupShare;
import com.tileoverlay.TileColourSlot;
import com.tileoverlay.MarkerGroup;
import com.tileoverlay.MarkerLocation;
import com.tileoverlay.MarkerLooks;
import com.tileoverlay.MarkerRepository;
import com.tileoverlay.MarkerSnapshot;
import com.tileoverlay.MarkerStyle;
import com.tileoverlay.ObjectColourSlot;
import com.tileoverlay.ObjectMarker;
import com.tileoverlay.ObjectRef;
import com.tileoverlay.MarkerSnapshot.ListFilter;
import com.tileoverlay.TileMarker;
import com.tileoverlay.TileObjectMarkersPlugin;
import com.tileoverlay.TileOverlayConfig;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;
import net.runelite.client.util.ImageUtil;

@Singleton
public class TileOverlayPanel extends PluginPanel
{
	public interface Listener
	{
		void renameGroup(String id, String name);

		void setGroupEnabled(String id, boolean enabled);

		void setGroupCollapsed(String id, boolean collapsed);

		void deleteGroup(String id);

		void assign(Collection<MarkerLocation> locations, String groupId);

		void unassign(Collection<MarkerLocation> locations);

		void unmark(Collection<MarkerLocation> locations);

		void assignObjects(Collection<ObjectRef> refs, String groupId);

		void unassignObjects(Collection<ObjectRef> refs);

		void unmarkObjects(Collection<ObjectRef> refs);

		void setObjectShowName(ObjectRef ref, boolean show);

		void setObjectShowLeftClick(ObjectRef ref, boolean show);

		void setTileLabel(MarkerLocation location, String label);

		/** Updates the drawn tile label without rebuilding the panel, so typing can show as it is entered. */
		void previewTileLabel(MarkerLocation location, String label);

		/** Null follows that style's Corners setting. */
		void setTileCorners(MarkerLocation location, Boolean cornersOnly);

		/** Null follows that style's Merge setting. */
		void setTileMerge(MarkerLocation location, Boolean mergeBorders);

		/** Null clears the override on every tile in the group. */
		void setGroupTileCorners(String groupId, Boolean cornersOnly);

		/** Null clears the override on every tile in the group. */
		void setGroupTileMerge(String groupId, Boolean mergeBorders);

		/** Null clears that slot and the tile follows the group colour, or the style setting. */
		void setTileColour(MarkerLocation location, TileColourSlot slot, Color colour);

		/** Tile groups only. Null clears the group border colour. */
		void setGroupColour(String groupId, Color colour);

		/** Tile groups only. Null clears the group fill colour. */
		void setGroupFillColour(String groupId, Color colour);

		/** Copies the group. Returns the clipboard text, or null when the group is gone. */
		String copyGroup(String id);

		void importGroup(GroupKind kind, String text, Consumer<String> done);

		void setObjectLabel(ObjectRef ref, String label);

		/** Updates the drawn label without rebuilding the panel, so typing can show as it is entered. */
		void previewObjectLabel(ObjectRef ref, String label);

		void setObjectShowOutline(ObjectRef ref, boolean show);

		void setObjectShowHull(ObjectRef ref, boolean show);

		void setObjectShowTile(ObjectRef ref, boolean show);

		void setObjectShowClickbox(ObjectRef ref, boolean show);

		void setObjectColour(ObjectRef ref, ObjectColourSlot slot, Color colour);

		void addNamedGroup(GroupKind kind, String name);

		void createNamedTileGroup(String name, Collection<MarkerLocation> locations);

		void createNamedObjectGroup(String name, Collection<ObjectRef> refs);

		void setActiveGroup(String id, boolean active);
	}

	private static final String[] CARD_KEYS = {"tileGroups", "tiles", "objectGroups", "objects"};
	private static final Color CARD = new Color(32, 34, 38);
	private static final Color CARD_DIM = new Color(28, 29, 32);
	private static final Color CARD_SELECTED = new Color(62, 66, 74);
	private static final Color BUTTON_FILL = new Color(78, 78, 82);
	private static final Color BUTTON_TEXT = new Color(235, 235, 235);
	private static final Color TAB_IDLE = new Color(54, 54, 58);
	private static final Color TAB_SELECTED = new Color(96, 96, 100);
	private static final Color TAB_MUTED = new Color(168, 168, 172);
	private static final Color FIELD = new Color(24, 25, 28);
	private static final Color FIELD_BORDER = new Color(90, 92, 96);
	private static final int CORNER = 4;
	private static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 12);
	private static final Font UI_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
	private static final Font SMALL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
	private static final String LABEL_TOGGLE = "labelToggle";
	// Eye icons from Skretzo's Radius Markers plugin, BSD-2-Clause.
	private static final ImageIcon VISIBLE_ICON;
	private static final ImageIcon VISIBLE_HOVER_ICON;
	private static final ImageIcon INVISIBLE_ICON;
	private static final ImageIcon INVISIBLE_HOVER_ICON;

	static
	{
		BufferedImage visible = ImageUtil.loadImageResource(TileOverlayPanel.class, "visible_icon.png");
		BufferedImage invisible = ImageUtil.loadImageResource(TileOverlayPanel.class, "invisible_icon.png");
		VISIBLE_ICON = new ImageIcon(visible);
		VISIBLE_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(visible, -100));
		INVISIBLE_ICON = new ImageIcon(invisible);
		INVISIBLE_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(invisible, -100));
	}

	private final MarkerRepository repository;
	private final TileOverlayConfig config;
	private final ColorPickerManager colorPickerManager;
	private Listener listener;
	private MarkerSnapshot snapshot = MarkerSnapshot.empty();
	private final LinkedHashSet<MarkerLocation> selected = new LinkedHashSet<>();
	private final LinkedHashSet<ObjectRef> selectedObjects = new LinkedHashSet<>();
	private final Set<ObjectRef> labelEditors = new HashSet<>();
	private final Set<ObjectRef> expandedObjects = new HashSet<>();
	private final Set<MarkerLocation> expandedTiles = new HashSet<>();
	private final Set<MarkerLocation> tileLabelEditors = new HashSet<>();
	private final Map<MarkerLocation, String> tileLabelDrafts = new HashMap<>();
	private final Map<ObjectRef, String> objectLabelDrafts = new HashMap<>();
	private final List<TileMarker> tileOrder = new ArrayList<>();
	private final List<ObjectMarker> objectOrder = new ArrayList<>();
	private MarkerLocation tileAnchorLocation;
	private ObjectRef objectAnchorLocation;
	private ObjectRef focusLabel;
	private MarkerLocation focusTileLabel;
	private int primaryIndex;
	private int secondaryIndex;
	private ListFilter tileFilter = ListFilter.UNGROUPED;
	private ListFilter objectFilter = ListFilter.UNGROUPED;
	private boolean quickDelete;
	private boolean built;
	private boolean namingEmpty;
	private Collection<MarkerLocation> namingTiles;
	private Collection<ObjectRef> namingObjects;
	private String activeTileGroupId;
	private String activeObjectGroupId;

	private final TabButton[] primaryButtons = new TabButton[2];
	private final TabButton[] secondaryButtons = new TabButton[2];
	private JPanel actionRow;
	private JPanel selectionActions;
	private JPanel nameRow;
	private JTextField groupNameField;
	private PillButton createGroupButton;
	private PillButton addToGroupButton;
	private PillButton newGroupButton;
	private PillButton importGroupButton;
	private JPanel groupBar;
	private JPanel toggles;
	private TogglePill listFilterToggle;
	private TogglePill quickDeleteToggle;
	private JLabel statusLabel;
	private JPanel statusRow;
	private JButton collapseAll;
	private final CardLayout cards = new CardLayout();
	private final JPanel cardHost = new JPanel(cards);
	private JPanel tileGroupsBody;
	private JPanel tilesBody;
	private JPanel objectGroupsBody;
	private JPanel objectsBody;
	private JScrollPane tileGroupsScroll;
	private JScrollPane tilesScroll;
	private JScrollPane objectGroupsScroll;
	private JScrollPane objectsScroll;

	@Inject
	TileOverlayPanel(MarkerRepository repository, TileOverlayConfig config, ColorPickerManager colorPickerManager)
	{
		super(false);
		this.repository = repository;
		this.config = config;
		this.colorPickerManager = colorPickerManager;
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(new EmptyBorder(0, 0, 0, 0));
	}

	public void setListener(Listener listener)
	{
		this.listener = listener;
	}

	public void install()
	{
		if (built)
		{
			return;
		}
		built = true;

		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		header.setOpaque(false);
		header.setBorder(new EmptyBorder(12, 6, 8, 6));

		BufferedImage rawIcon = ImageUtil.loadImageResource(TileObjectMarkersPlugin.class, "icon.png");
		JLabel iconLabel = null;
		if (rawIcon != null)
		{
			iconLabel = new JLabel(new ImageIcon(ImageUtil.resizeImage(rawIcon, 16, 16)));
		}
		JLabel title = new JLabel("Tile & Object Markers");
		title.setFont(TITLE_FONT);
		title.setForeground(Color.WHITE);
		header.add(new TitleRow(iconLabel, title));
		header.add(Box.createVerticalStrut(8));

		JPanel primaryRow = new JPanel(new GridLayout(1, 2, 6, 0));
		primaryRow.setOpaque(false);
		primaryRow.setAlignmentX(Component.CENTER_ALIGNMENT);
		primaryRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
		primaryButtons[0] = new TabButton("Tiles", TabGlyph.TILE, true);
		primaryButtons[1] = new TabButton("Objects", TabGlyph.OBJECT, true);
		primaryButtons[0].addActionListener(event -> showPrimary(0));
		primaryButtons[1].addActionListener(event -> showPrimary(1));
		primaryRow.add(primaryButtons[0]);
		primaryRow.add(primaryButtons[1]);
		header.add(primaryRow);
		header.add(Box.createVerticalStrut(6));

		secondaryButtons[0] = new TabButton("Tiles", TabGlyph.TILE, false);
		secondaryButtons[1] = new TabButton("Tile Groups", TabGlyph.GROUP, false);
		secondaryButtons[0].addActionListener(event -> showSecondary(0));
		secondaryButtons[1].addActionListener(event -> showSecondary(1));
		header.add(new SplitTabs(secondaryButtons[0], new TabSeparator(), secondaryButtons[1]));
		header.add(Box.createVerticalStrut(8));

		toggles = new JPanel(new GridLayout(1, 2, 6, 0));
		toggles.setOpaque(false);
		toggles.setAlignmentX(Component.CENTER_ALIGNMENT);
		toggles.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
		listFilterToggle = new TogglePill("Ungrouped");
		listFilterToggle.setToolTipText("Cycles Ungrouped, Grouped, and Show all. Tiles and objects keep their own filter.");
		listFilterToggle.addActionListener(event ->
		{
			if (activeKind() == GroupKind.OBJECT)
			{
				objectFilter = nextFilter(objectFilter);
			}
			else
			{
				tileFilter = nextFilter(tileFilter);
			}
			refreshListFilterButton();
			rebuildLists();
		});
		quickDeleteToggle = new TogglePill("Quick delete");
		quickDeleteToggle.addActionListener(event ->
		{
			quickDelete = !quickDelete;
			quickDeleteToggle.setOn(quickDelete);
			rebuildLists();
		});
		toggles.add(listFilterToggle);
		toggles.add(quickDeleteToggle);
		toggles.setVisible(false);
		header.add(toggles);

		statusLabel = new JLabel(" ", SwingConstants.CENTER);
		statusLabel.setFont(SMALL_FONT);
		statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		statusRow = new JPanel(new BorderLayout(4, 0));
		statusRow.setOpaque(false);
		statusRow.setAlignmentX(Component.CENTER_ALIGNMENT);
		statusRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
		statusRow.setVisible(false);
		collapseAll = new CollapseAllButton();
		collapseAll.addActionListener(event -> collapseExpanded());
		statusRow.add(statusLabel, BorderLayout.CENTER);
		statusRow.add(collapseAll, BorderLayout.EAST);
		header.add(statusRow);
		header.add(Box.createVerticalStrut(6));

		nameRow = new JPanel(new BorderLayout(6, 0));
		nameRow.setOpaque(false);
		nameRow.setAlignmentX(Component.CENTER_ALIGNMENT);
		nameRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
		nameRow.setVisible(false);
		groupNameField = new JTextField();
		groupNameField.setFont(UI_FONT);
		groupNameField.setForeground(Color.WHITE);
		groupNameField.setCaretColor(Color.WHITE);
		groupNameField.setBackground(FIELD);
		groupNameField.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(FIELD_BORDER),
			new EmptyBorder(3, 6, 3, 6)));
		groupNameField.setToolTipText("Group name");
		groupNameField.addActionListener(event -> saveGroupName());
		JPanel nameButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
		nameButtons.setOpaque(false);
		PillButton saveName = new PillButton("Save");
		saveName.addActionListener(event -> saveGroupName());
		PillButton cancelName = new PillButton("Cancel");
		cancelName.addActionListener(event -> hideGroupName());
		nameButtons.add(saveName);
		nameButtons.add(cancelName);
		nameRow.add(groupNameField, BorderLayout.CENTER);
		nameRow.add(nameButtons, BorderLayout.EAST);
		header.add(nameRow);
		header.add(Box.createVerticalStrut(6));

		actionRow = new JPanel();
		actionRow.setLayout(new BoxLayout(actionRow, BoxLayout.Y_AXIS));
		actionRow.setOpaque(false);
		actionRow.setAlignmentX(Component.CENTER_ALIGNMENT);

		PillButton clearSelectionButton = new PillButton("Clear selection", true);
		clearSelectionButton.addActionListener(event -> clearActiveSelection());
		PillButton deleteSelectionButton = new PillButton("Delete selection", true);
		deleteSelectionButton.setToolTipText("Delete the selected items");
		deleteSelectionButton.addActionListener(event -> deleteActiveSelection());
		createGroupButton = new PillButton("Create group", true);
		createGroupButton.setToolTipText("Name a new group for the selected items");
		createGroupButton.addActionListener(event -> beginNamingSelection());
		addToGroupButton = new PillButton("Add to group", true);
		addToGroupButton.setToolTipText("Add the selected items to a group");
		addToGroupButton.addActionListener(event -> showAddToGroupMenu(addToGroupButton));
		newGroupButton = new PillButton("Create group");
		newGroupButton.setToolTipText("Name an empty group");
		newGroupButton.addActionListener(event -> beginNaming(true, null, null));
		importGroupButton = new PillButton("Import");
		importGroupButton.setToolTipText("Paste a copied group into a new group");
		importGroupButton.addActionListener(event -> beginImport());

		JPanel selectionTop = buttonRow();
		selectionTop.add(clearSelectionButton);
		selectionTop.add(deleteSelectionButton);
		JPanel selectionBottom = buttonRow();
		selectionBottom.add(createGroupButton);
		selectionBottom.add(addToGroupButton);
		selectionActions = new JPanel();
		selectionActions.setLayout(new BoxLayout(selectionActions, BoxLayout.Y_AXIS));
		selectionActions.setOpaque(false);
		selectionActions.setAlignmentX(Component.CENTER_ALIGNMENT);
		selectionActions.add(selectionTop);
		selectionActions.add(Box.createVerticalStrut(8));
		selectionActions.add(selectionBottom);
		fitBar(selectionActions);
		actionRow.add(selectionActions);
		groupBar = buttonRow();
		groupBar.add(newGroupButton);
		groupBar.add(importGroupButton);
		actionRow.add(groupBar);
		fitBar(groupBar);
		header.add(actionRow);

		tileGroupsBody = body();
		tilesBody = body();
		objectGroupsBody = body();
		objectsBody = body();
		tileGroupsScroll = scroll(tileGroupsBody);
		tilesScroll = scroll(tilesBody);
		objectGroupsScroll = scroll(objectGroupsBody);
		objectsScroll = scroll(objectsBody);
		cardHost.setOpaque(false);
		cardHost.add(tileGroupsScroll, CARD_KEYS[0]);
		cardHost.add(tilesScroll, CARD_KEYS[1]);
		cardHost.add(objectGroupsScroll, CARD_KEYS[2]);
		cardHost.add(objectsScroll, CARD_KEYS[3]);

		add(header, BorderLayout.NORTH);
		add(cardHost, BorderLayout.CENTER);
		showCurrent();
		rebuild();
	}

	public void refreshColours()
	{
		if (!built)
		{
			return;
		}
		if (listFilterToggle != null)
		{
			listFilterToggle.refreshFace();
		}
		if (quickDeleteToggle != null)
		{
			quickDeleteToggle.refreshFace();
		}
		rebuild();
	}

	public void setSnapshot(MarkerSnapshot snapshot)
	{
		install();
		this.snapshot = snapshot == null ? MarkerSnapshot.empty() : snapshot;
		Set<MarkerLocation> alive = new HashSet<>();
		for (TileMarker marker : this.snapshot.getMarkers())
		{
			alive.add(marker.location());
		}
		selected.retainAll(alive);
		tileLabelEditors.retainAll(alive);
		expandedTiles.retainAll(alive);
		tileLabelDrafts.keySet().retainAll(alive);
		if (focusTileLabel != null && !alive.contains(focusTileLabel))
		{
			focusTileLabel = null;
		}
		repository.setHighlighted(new HashSet<>(selected));

		Set<ObjectRef> aliveObjects = new HashSet<>();
		for (ObjectMarker marker : this.snapshot.getObjects())
		{
			aliveObjects.add(marker.ref());
		}
		selectedObjects.retainAll(aliveObjects);
		labelEditors.retainAll(aliveObjects);
		expandedObjects.retainAll(aliveObjects);
		objectLabelDrafts.keySet().retainAll(aliveObjects);
		if (focusLabel != null && !aliveObjects.contains(focusLabel))
		{
			focusLabel = null;
		}
		repository.setHighlightedObjects(new HashSet<>(selectedObjects));
		rememberActiveGroups();
		rebuild();
		focusLabel = null;
		focusTileLabel = null;
	}

	private void showPrimary(int index)
	{
		if (index == primaryIndex)
		{
			return;
		}
		disarmQuickDelete();
		hideGroupName();
		primaryIndex = index;
		showCurrent();
	}

	private void showSecondary(int index)
	{
		if (index == secondaryIndex)
		{
			return;
		}
		disarmQuickDelete();
		hideGroupName();
		secondaryIndex = index;
		showCurrent();
	}

	private void disarmQuickDelete()
	{
		if (!quickDelete)
		{
			return;
		}
		quickDelete = false;
		if (quickDeleteToggle != null)
		{
			quickDeleteToggle.setOn(false);
		}
		if (built)
		{
			rebuildLists();
		}
	}

	private void showCurrent()
	{
		for (int i = 0; i < primaryButtons.length; i++)
		{
			if (primaryButtons[i] != null)
			{
				primaryButtons[i].setActive(i == primaryIndex);
			}
		}
		refreshSecondaryLabels();
		for (int i = 0; i < secondaryButtons.length; i++)
		{
			if (secondaryButtons[i] != null)
			{
				secondaryButtons[i].setActive(i == secondaryIndex);
			}
		}
		boolean list = listView();
		refreshListFilterButton();
		if (toggles != null)
		{
			toggles.setVisible(list);
		}
		if (statusRow != null)
		{
			statusRow.setVisible(list);
		}
		if (collapseAll != null)
		{
			collapseAll.setVisible(list);
		}
		cards.show(cardHost, CARD_KEYS[cardIndex()]);
		if (built && list)
		{
			updateStatus();
		}
		else
		{
			updateActions();
		}
		revalidate();
		repaint();
	}

	private void refreshSecondaryLabels()
	{
		if (secondaryButtons[0] == null || secondaryButtons[1] == null)
		{
			return;
		}
		boolean tiles = primaryIndex == 0;
		secondaryButtons[0].setText(tiles ? "Tiles" : "Objects");
		secondaryButtons[1].setText(tiles ? "Tile Groups" : "Object Groups");
		secondaryButtons[0].setGlyph(tiles ? TabGlyph.TILE : TabGlyph.OBJECT);
		secondaryButtons[1].setGlyph(TabGlyph.GROUP);
	}

	private int cardIndex()
	{
		if (primaryIndex == 0)
		{
			return secondaryIndex == 0 ? 1 : 0;
		}
		return secondaryIndex == 0 ? 3 : 2;
	}

	private boolean listView()
	{
		return secondaryIndex == 0;
	}

	private GroupKind activeKind()
	{
		return primaryIndex == 1 ? GroupKind.OBJECT : GroupKind.TILE;
	}

	private void rebuild()
	{
		int tileGroupScroll = tileGroupsScroll.getVerticalScrollBar().getValue();
		int tileScroll = tilesScroll.getVerticalScrollBar().getValue();
		int objectGroupScroll = objectGroupsScroll.getVerticalScrollBar().getValue();
		int objectScroll = objectsScroll.getVerticalScrollBar().getValue();
		rebuildGroupList(tileGroupsBody, GroupKind.TILE, "No tile groups yet. Add one to organise marked tiles.");
		rebuildTiles();
		rebuildGroupList(objectGroupsBody, GroupKind.OBJECT, "No object groups yet. Add one to organise marked objects.");
		rebuildObjects();
		SwingUtilities.invokeLater(() ->
		{
			tileGroupsScroll.getVerticalScrollBar().setValue(tileGroupScroll);
			tilesScroll.getVerticalScrollBar().setValue(tileScroll);
			objectGroupsScroll.getVerticalScrollBar().setValue(objectGroupScroll);
			objectsScroll.getVerticalScrollBar().setValue(objectScroll);
		});
		revalidate();
		repaint();
	}

	private void rebuildLists()
	{
		int tileScroll = tilesScroll.getVerticalScrollBar().getValue();
		int objectScroll = objectsScroll.getVerticalScrollBar().getValue();
		rebuildTiles();
		rebuildObjects();
		SwingUtilities.invokeLater(() ->
		{
			tilesScroll.getVerticalScrollBar().setValue(tileScroll);
			objectsScroll.getVerticalScrollBar().setValue(objectScroll);
		});
	}

	private void rebuildGroupList(JPanel body, GroupKind kind, String emptyHint)
	{
		body.removeAll();
		boolean any = false;
		for (MarkerGroup group : groupsOf(kind))
		{
			any = true;
			body.add(groupCard(group, kind));
			body.add(Box.createVerticalStrut(6));
		}
		if (!any)
		{
			body.add(hint(emptyHint));
		}
		body.revalidate();
		body.repaint();
	}

	private void rebuildTiles()
	{
		List<TileMarker> listed = snapshot.tilesForList(tileFilter);
		tileOrder.clear();
		tileOrder.addAll(listed);
		Map<String, String> names = groupNames();

		tilesBody.removeAll();
		if (snapshot.getMarkers().isEmpty())
		{
			tilesBody.add(hint("No marked tiles. Hold the mark key and right-click a tile."));
		}
		else if (listed.isEmpty())
		{
			tilesBody.add(hint(tileFilterHint()));
		}
		else
		{
			for (int i = 0; i < tileOrder.size(); i++)
			{
				tilesBody.add(tileRow(tileOrder.get(i), i, tileOrder, names, false));
				tilesBody.add(Box.createVerticalStrut(4));
			}
		}
		tilesBody.revalidate();
		tilesBody.repaint();
		updateStatus();
	}

	private void rebuildObjects()
	{
		List<ObjectMarker> listed = snapshot.objectsForList(objectFilter);
		objectOrder.clear();
		objectOrder.addAll(listed);
		Map<String, String> names = groupNames();

		objectsBody.removeAll();
		if (snapshot.getObjects().isEmpty())
		{
			objectsBody.add(hint("No marked objects. Hold the mark key and right-click an object."));
		}
		else if (listed.isEmpty())
		{
			objectsBody.add(hint(objectFilterHint()));
		}
		else
		{
			for (int i = 0; i < objectOrder.size(); i++)
			{
				objectsBody.add(objectRow(objectOrder.get(i), i, objectOrder, names, false));
				objectsBody.add(Box.createVerticalStrut(4));
			}
		}
		objectsBody.revalidate();
		objectsBody.repaint();
		updateStatus();
	}

	private String tileFilterHint()
	{
		if (tileFilter == ListFilter.GROUPED)
		{
			return "No grouped tiles.";
		}
		return "No ungrouped tiles.";
	}

	private String objectFilterHint()
	{
		if (objectFilter == ListFilter.GROUPED)
		{
			return "No grouped objects.";
		}
		return "No ungrouped objects.";
	}

	private void refreshListFilterButton()
	{
		if (listFilterToggle == null)
		{
			return;
		}
		ListFilter filter = activeKind() == GroupKind.OBJECT ? objectFilter : tileFilter;
		listFilterToggle.setText(filterLabel(filter));
		listFilterToggle.setOn(filter != ListFilter.UNGROUPED);
	}

	private static ListFilter nextFilter(ListFilter filter)
	{
		if (filter == ListFilter.UNGROUPED)
		{
			return ListFilter.GROUPED;
		}
		if (filter == ListFilter.GROUPED)
		{
			return ListFilter.SHOW_ALL;
		}
		return ListFilter.UNGROUPED;
	}

	private static String filterLabel(ListFilter filter)
	{
		if (filter == ListFilter.GROUPED)
		{
			return "Grouped";
		}
		if (filter == ListFilter.SHOW_ALL)
		{
			return "Show all";
		}
		return "Ungrouped";
	}

	private void updateStatus()
	{
		if (statusLabel == null)
		{
			return;
		}
		if (!listView())
		{
			updateActions();
			return;
		}
		if (activeKind() == GroupKind.OBJECT)
		{
			statusLabel.setText(selectedObjects.isEmpty()
				? countLabel(objectOrder.size(), "object")
				: selectedObjects.size() + " selected");
		}
		else
		{
			statusLabel.setText(selected.isEmpty()
				? countLabel(tileOrder.size(), "tile")
				: selected.size() + " selected");
		}
		updateActions();
	}

	private static String countLabel(int count, String noun)
	{
		return count == 1 ? "1 " + noun : count + " " + noun + "s";
	}

	private Map<String, String> groupNames()
	{
		Map<String, String> names = new HashMap<>();
		for (MarkerGroup group : snapshot.getGroups())
		{
			names.put(group.getId(), group.getName());
		}
		return names;
	}

	private List<MarkerGroup> groupsOf(GroupKind kind)
	{
		List<MarkerGroup> list = new ArrayList<>();
		for (MarkerGroup group : snapshot.getGroups())
		{
			GroupKind groupKind = group.getKind() == null ? GroupKind.TILE : group.getKind();
			if (groupKind == kind)
			{
				list.add(group);
			}
		}
		return list;
	}

	private JPanel groupCard(MarkerGroup group, GroupKind kind)
	{
		int count = 0;
		boolean allCorners = true;
		boolean allMerge = true;
		if (kind == GroupKind.OBJECT)
		{
			for (ObjectMarker marker : snapshot.getObjects())
			{
				if (group.getId().equals(marker.getGroupId()))
				{
					count++;
				}
			}
		}
		else
		{
			for (TileMarker marker : snapshot.getMarkers())
			{
				if (group.getId().equals(marker.getGroupId()))
				{
					count++;
					if (!Boolean.TRUE.equals(marker.getCornersOnly()))
					{
						allCorners = false;
					}
					if (!Boolean.TRUE.equals(marker.getMergeBorders()))
					{
						allMerge = false;
					}
				}
			}
			if (count == 0)
			{
				allCorners = false;
				allMerge = false;
			}
		}

		RoundPanel card = new RoundPanel(group.isEnabled() ? CARD : CARD_DIM);
		card.setLayout(new BorderLayout(0, 6));
		card.setBorder(new EmptyBorder(8, 8, 8, 8));

		JPanel header = new JPanel(new BorderLayout(6, 0));
		header.setOpaque(false);

		ChevronButton chevron = new ChevronButton(group.isCollapsed());
		chevron.addActionListener(event ->
		{
			if (listener != null)
			{
				listener.setGroupCollapsed(group.getId(), !group.isCollapsed());
			}
		});

		JTextField name = new JTextField(group.getName());
		name.setFont(UI_FONT);
		name.setForeground(group.isEnabled() ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR);
		name.setCaretColor(Color.WHITE);
		name.setOpaque(false);
		name.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		// A long name must not set the card's minimum width, or the eye and delete sit past the sidebar.
		Dimension nameSize = name.getPreferredSize();
		name.setPreferredSize(new Dimension(48, Math.max(22, nameSize.height)));
		name.setMinimumSize(new Dimension(24, Math.max(22, nameSize.height)));
		name.addActionListener(event -> commitName(group, name));
		name.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent event)
			{
				commitName(group, name);
			}
		});

		boolean cornersMarked = allCorners;
		boolean mergeMarked = allMerge;
		int members = count;
		int rows = kind == GroupKind.OBJECT ? 1 : 2;
		JPanel actions = new JPanel(new GridLayout(rows, 3, 2, 2));
		actions.setOpaque(false);
		actions.add(fixedSlot(copyButton(group)));
		EyeButton eye = new EyeButton(group.isEnabled());
		eye.addActionListener(event ->
		{
			if (listener != null)
			{
				listener.setGroupEnabled(group.getId(), !group.isEnabled());
			}
		});
		TrashButton trash = new TrashButton();
		trash.addActionListener(event -> confirmDelete(group, kind));
		actions.add(fixedSlot(eye));
		actions.add(fixedSlot(trash));
		if (kind != GroupKind.OBJECT)
		{
			actions.add(fixedSlot(groupBrush(group)));
			actions.add(fixedSlot(groupMarkButton(true, cornersMarked, () ->
			{
				if (listener != null && members > 0)
				{
					listener.setGroupTileCorners(group.getId(), cornersMarked ? null : Boolean.TRUE);
				}
			})));
			actions.add(fixedSlot(groupMarkButton(false, mergeMarked, () ->
			{
				if (listener != null && members > 0)
				{
					listener.setGroupTileMerge(group.getId(), mergeMarked ? null : Boolean.TRUE);
				}
			})));
		}

		JPanel chevronWrap = new JPanel(new GridBagLayout());
		chevronWrap.setOpaque(false);
		chevronWrap.add(chevron);
		JPanel nameWrap = new JPanel(new GridBagLayout());
		nameWrap.setOpaque(false);
		GridBagConstraints nameConstraints = new GridBagConstraints();
		nameConstraints.weightx = 1;
		nameConstraints.fill = GridBagConstraints.HORIZONTAL;
		nameConstraints.anchor = GridBagConstraints.WEST;
		nameWrap.add(name, nameConstraints);

		// BorderLayout stretches the east strip, and GridLayout would open the row gap with it.
		int cell = 22;
		int gap = 2;
		Dimension grid = new Dimension(cell * 3 + gap * 2, cell * rows + gap * (rows - 1));
		actions.setPreferredSize(grid);
		actions.setMinimumSize(grid);
		actions.setMaximumSize(grid);
		JPanel actionsAnchor = new JPanel(new GridBagLayout());
		actionsAnchor.setOpaque(false);
		actionsAnchor.add(actions);

		header.add(chevronWrap, BorderLayout.WEST);
		header.add(nameWrap, BorderLayout.CENTER);
		header.add(actionsAnchor, BorderLayout.EAST);
		card.add(header, BorderLayout.NORTH);
		card.setOutline(groupBorderColour());

		if (!group.isCollapsed())
		{
			card.add(groupMembers(group, kind), BorderLayout.CENTER);
		}

		boolean active = isActiveGroup(group, kind);
		String noun = kind == GroupKind.OBJECT ? "object" : "tile";
		String metaText = countLabel(count, noun);
		metaText = active ? metaText + "  ·  Active" : metaText + "  ·  click to set active";
		JLabel meta = new JLabel(metaText);
		meta.setFont(SMALL_FONT);
		meta.setForeground(active ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR);
		card.add(meta, BorderLayout.SOUTH);

		armGroupClick(header, group, kind);
		armGroupClick(meta, group, kind);
		stretch(card);
		return card;
	}

	private JComponent groupMembers(MarkerGroup group, GroupKind kind)
	{
		JPanel members = new JPanel()
		{
			@Override
			public Dimension getPreferredSize()
			{
				Dimension pref = super.getPreferredSize();
				return new Dimension(1, pref.height);
			}

			@Override
			public Dimension getMinimumSize()
			{
				Dimension min = super.getMinimumSize();
				return new Dimension(1, min.height);
			}

			@Override
			public Dimension getMaximumSize()
			{
				Dimension pref = getPreferredSize();
				return new Dimension(Integer.MAX_VALUE, pref.height);
			}
		};
		members.setLayout(new BoxLayout(members, BoxLayout.Y_AXIS));
		members.setOpaque(false);
		members.setBorder(new EmptyBorder(6, 4, 0, 0));

		Map<String, String> names = groupNames();
		if (kind == GroupKind.OBJECT)
		{
			List<ObjectMarker> listed = new ArrayList<>();
			for (ObjectMarker marker : snapshot.getObjects())
			{
				if (group.getId().equals(marker.getGroupId()))
				{
					listed.add(marker);
				}
			}
			if (listed.isEmpty())
			{
				JLabel empty = groupEmpty("No objects in this group.");
				armGroupClick(empty, group, kind);
				members.add(empty);
				return members;
			}
			for (int i = 0; i < listed.size(); i++)
			{
				members.add(objectRow(listed.get(i), i, listed, names, true));
				if (i < listed.size() - 1)
				{
					members.add(Box.createVerticalStrut(2));
				}
			}
			return members;
		}

		List<TileMarker> listed = new ArrayList<>();
		for (TileMarker marker : snapshot.getMarkers())
		{
			if (group.getId().equals(marker.getGroupId()))
			{
				listed.add(marker);
			}
		}
		if (listed.isEmpty())
		{
			JLabel empty = groupEmpty("No tiles in this group.");
			armGroupClick(empty, group, kind);
			members.add(empty);
			return members;
		}
		for (int i = 0; i < listed.size(); i++)
		{
			members.add(tileRow(listed.get(i), i, listed, names, true));
			if (i < listed.size() - 1)
			{
				members.add(Box.createVerticalStrut(4));
			}
		}
		return members;
	}

	private void armGroupClick(Component component, MarkerGroup group, GroupKind kind)
	{
		if (component instanceof JTextField || component instanceof JButton)
		{
			return;
		}
		boolean active = isActiveGroup(group, kind);
		String noun = kind == GroupKind.OBJECT ? "objects" : "tiles";
		String tip = active
			? "Active " + noun + " group. New marks are added here. Click to turn it off."
			: "Make this the active " + noun + " group. New marks are added here.";
		component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		if (component instanceof JComponent)
		{
			((JComponent) component).setToolTipText(tip);
		}
		component.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent event)
			{
				if (SwingUtilities.isLeftMouseButton(event))
				{
					toggleActive(group, kind);
				}
			}
		});
		if (component instanceof Container)
		{
			for (Component child : ((Container) component).getComponents())
			{
				armGroupClick(child, group, kind);
			}
		}
	}

	private boolean isActiveGroup(MarkerGroup group, GroupKind kind)
	{
		if (group == null || group.getId() == null)
		{
			return false;
		}
		String current = kind == GroupKind.OBJECT ? activeObjectGroupId : activeTileGroupId;
		return group.getId().equals(current);
	}

	private void rememberActiveGroups()
	{
		activeTileGroupId = null;
		activeObjectGroupId = null;
		for (MarkerGroup group : snapshot.getGroups())
		{
			if (group == null || !group.isActive() || group.getId() == null)
			{
				continue;
			}
			if (group.getKind() == GroupKind.OBJECT)
			{
				activeObjectGroupId = group.getId();
			}
			else
			{
				activeTileGroupId = group.getId();
			}
		}
	}

	private void toggleActive(MarkerGroup group, GroupKind kind)
	{
		if (listener == null || group == null || group.getId() == null)
		{
			return;
		}
		boolean turnOn = !isActiveGroup(group, kind);
		if (kind == GroupKind.OBJECT)
		{
			activeObjectGroupId = turnOn ? group.getId() : null;
		}
		else
		{
			activeTileGroupId = turnOn ? group.getId() : null;
		}
		listener.setActiveGroup(group.getId(), turnOn);
		syncGroupHints();
	}

	private void commitName(MarkerGroup group, JTextField name)
	{
		String text = name.getText() == null ? "" : name.getText().trim();
		if (text.isEmpty())
		{
			name.setText(group.getName());
			return;
		}
		if (text.equals(group.getName()) || listener == null)
		{
			return;
		}
		listener.renameGroup(group.getId(), text);
	}

	private void confirmDelete(MarkerGroup group, GroupKind kind)
	{
		String noun = kind == GroupKind.OBJECT ? "object" : "tile";
		int result = JOptionPane.showOptionDialog(
			SwingUtilities.getWindowAncestor(this),
			"Delete \"" + group.getName() + "\" and every " + noun + " in that group?",
			"Delete group",
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.WARNING_MESSAGE,
			null,
			new Object[]{"Delete", "Cancel"},
			"Cancel");
		if (result == 0 && listener != null)
		{
			listener.deleteGroup(group.getId());
		}
	}

	private JPanel tileRow(TileMarker marker, int index, List<TileMarker> order, Map<String, String> names, boolean inGroup)
	{
		boolean on = selected.contains(marker.location());
		WorldPoint world = marker.location().toWorldPoint();
		String title = world.getX() + ", " + world.getY();
		String detail = styleName(marker.getStyle()) + "  ·  plane " + world.getPlane();
		if (!inGroup)
		{
			String groupName = marker.getGroupId() == null ? "Ungrouped" : names.getOrDefault(marker.getGroupId(), "Ungrouped");
			detail = detail + "  ·  " + groupName;
		}
		boolean expanded = expandedTiles.contains(marker.location());
		JPanel options = expanded ? tileOptions(marker) : null;
		Set<Component> skip = new HashSet<>();
		if (options != null)
		{
			skip.add(options);
		}
		JButton remove = quickDelete ? crossButton(() -> removeTiles(Collections.singleton(marker.location()))) : null;
		JComponent detailLine = tileDetail(detail, marker, tileTextColour());
		RoundPanel row = row(on, title, detail, detailLine, options, remove, tileTextColour());
		MouseAdapter adapter = new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent event)
			{
				if (SwingUtilities.isRightMouseButton(event))
				{
					Collection<MarkerLocation> targets = targetsFor(marker);
					showMemberMenu(row, event, GroupKind.TILE, targets, Collections.emptyList());
					return;
				}
				if (SwingUtilities.isLeftMouseButton(event))
				{
					selectTile(marker, index, event.isShiftDown(), order);
				}
			}
		};
		armRowPress(row, adapter, skip);
		return row;
	}

	private JPanel tileOptions(TileMarker marker)
	{
		JPanel block = optionsBlock();
		CenteredFlow flags = new CenteredFlow();
		flags.add(tileLabelPill(marker));
		flags.add(tileFlagButton(marker, true));
		flags.add(tileFlagButton(marker, false));
		block.add(flags);
		if (tileLabelFieldOpen(marker))
		{
			boolean focus = focusTileLabel != null && focusTileLabel.equals(marker.location());
			block.add(Box.createVerticalStrut(4));
			block.add(centeredField(labelEditor(
				tileLabelFieldText(marker),
				text -> commitTileLabel(marker, text),
				text -> previewTileLabel(marker, text),
				focus)));
		}
		block.add(Box.createVerticalStrut(4));
		CenteredFlow colours = new CenteredFlow();
		colours.add(labelledSwatch("Tile", tileSwatch(marker, TileColourSlot.TILE)));
		colours.add(labelledSwatch("Fill", tileSwatch(marker, TileColourSlot.FILL)));
		colours.add(labelledSwatch("Label", tileSwatch(marker, TileColourSlot.LABEL)));
		block.add(colours);
		return block;
	}

	private JPanel objectRow(ObjectMarker marker, int index, List<ObjectMarker> order, Map<String, String> names, boolean inGroup)
	{
		boolean on = selectedObjects.contains(marker.ref());
		WorldPoint world = marker.ref().toWorldPoint();
		String title = objectNameTitle(marker);
		String detail = world.getX() + ", " + world.getY() + "  ·  plane " + world.getPlane();
		if (!inGroup)
		{
			String groupName = marker.getGroupId() == null ? "Ungrouped" : names.getOrDefault(marker.getGroupId(), "Ungrouped");
			detail = detail + "  ·  " + groupName;
		}
		boolean expanded = expandedObjects.contains(marker.ref());
		JPanel options = expanded ? objectOptions(marker) : null;
		Set<Component> skip = new HashSet<>();
		if (options != null)
		{
			skip.add(options);
		}

		JButton remove = quickDelete ? crossButton(() -> removeObjects(Collections.singleton(marker.ref()))) : null;
		JComponent detailLine = objectDetail(detail, marker, objectTextColour());
		RoundPanel row = row(on, title, detail, detailLine, options, remove, objectTextColour());
		if (inGroup)
		{
			row.setBorder(new EmptyBorder(3, on ? 8 : 10, 2, 8));
		}
		row.setToolTipText(title + "  ·  id " + marker.getObjectId());
		MouseAdapter adapter = new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent event)
			{
				if (SwingUtilities.isRightMouseButton(event))
				{
					Collection<ObjectRef> targets = targetsFor(marker);
					showMemberMenu(row, event, GroupKind.OBJECT, Collections.emptyList(), targets);
					return;
				}
				if (SwingUtilities.isLeftMouseButton(event))
				{
					selectObject(marker, index, event.isShiftDown(), order);
				}
			}
		};
		armRowPress(row, adapter, skip);
		return row;
	}

	private JPanel objectOptions(ObjectMarker marker)
	{
		JPanel block = optionsBlock();

		block.add(sectionTitle("Name display"));
		CenteredFlow names = new CenteredFlow();
		names.add(displayToggle(
			"Name",
			marker.isShowName(),
			"Draw the object name",
			() ->
			{
				if (listener != null)
				{
					listener.setObjectShowName(marker.ref(), !marker.isShowName());
				}
			}));
		names.add(displayToggle(
			"Left-click",
			marker.isShowLeftClick(),
			"Draw the left-click option",
			() ->
			{
				if (listener != null)
				{
					listener.setObjectShowLeftClick(marker.ref(), !marker.isShowLeftClick());
				}
			}));
		names.add(labelPill(marker));
		names.add(colourSwatch(marker, ObjectColourSlot.TEXT, "Text colour"));
		block.add(names);

		if (labelEditors.contains(marker.ref()))
		{
			block.add(labelFieldRow(marker));
		}

		block.add(Box.createVerticalStrut(4));
		block.add(sectionTitle("Highlight"));
		CenteredFlow pills = new CenteredFlow();
		pills.add(highlightChoice(marker, "Outline", marker.isShowOutline(), "Draw the model outline", ObjectColourSlot.OUTLINE, () ->
		{
			if (listener != null)
			{
				listener.setObjectShowOutline(marker.ref(), !marker.isShowOutline());
			}
		}));
		pills.add(highlightChoice(marker, "Hull", marker.isShowHull(), "Draw the convex hull", ObjectColourSlot.HULL, () ->
		{
			if (listener != null)
			{
				listener.setObjectShowHull(marker.ref(), !marker.isShowHull());
			}
		}));
		pills.add(highlightChoice(marker, "Tile", marker.isShowTile(), "Highlight the tile under the object", ObjectColourSlot.TILE, () ->
		{
			if (listener != null)
			{
				listener.setObjectShowTile(marker.ref(), !marker.isShowTile());
			}
		}));
		pills.add(highlightChoice(marker, "Clickbox", marker.isShowClickbox(), "Draw the clickbox", ObjectColourSlot.CLICKBOX, () ->
		{
			if (listener != null)
			{
				listener.setObjectShowClickbox(marker.ref(), !marker.isShowClickbox());
			}
		}));
		block.add(pills);
		return block;
	}

	private static JPanel optionsBlock()
	{
		JPanel block = new JPanel()
		{
			@Override
			public Dimension getMaximumSize()
			{
				Dimension pref = getPreferredSize();
				return new Dimension(Integer.MAX_VALUE, pref.height);
			}
		};
		block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
		block.setOpaque(false);
		block.setAlignmentX(Component.LEFT_ALIGNMENT);
		return block;
	}

	private JComponent highlightChoice(ObjectMarker marker, String text, boolean on, String tip, ObjectColourSlot slot, Runnable toggle)
	{
		return beside(displayToggle(text, on, tip, toggle), colourSwatch(marker, slot, text + " colour"));
	}

	private JComponent labelledSwatch(String name, JComponent square)
	{
		JPanel pair = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
		pair.setOpaque(false);
		JLabel caption = new JLabel(name);
		caption.setFont(SMALL_FONT);
		caption.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		pair.add(caption);
		pair.add(square);
		return pair;
	}

	private JComponent tileSwatch(TileMarker marker, TileColourSlot slot)
	{
		Integer custom = tileColourValue(marker, slot);
		Integer groupColour = groupColourOf(marker);
		Integer groupFill = groupFillOf(marker);
		Color shown = shownTileColour(marker, slot, custom, groupColour, groupFill);
		boolean own = custom != null;
		String title = tileColourTitle(slot);
		String idle = tileColourIdle(marker, slot, groupColour, groupFill);
		String ownTip = title + " for this tile. Right-click to use the default.";
		return colourSquare(shown, own, ownTip, idle, () ->
		{
			if (own && listener != null)
			{
				listener.setTileColour(marker.location(), slot, null);
			}
		}, () -> chooseTileColour(marker, slot, shown, title));
	}

	private void chooseTileColour(TileMarker marker, TileColourSlot slot, Color current, String title)
	{
		openPicker(current, title, colour ->
		{
			Color fallback = shownTileColour(marker, slot, null, groupColourOf(marker), groupFillOf(marker));
			if (fallback != null && colour.equals(fallback))
			{
				listener.setTileColour(marker.location(), slot, null);
			}
			else
			{
				listener.setTileColour(marker.location(), slot, colour);
			}
		});
	}

	private void chooseGroupColour(MarkerGroup group)
	{
		if (colorPickerManager == null || listener == null || group.getId() == null)
		{
			return;
		}
		String groupId = group.getId();
		Window owner = SwingUtilities.getWindowAncestor(this);
		JDialog dialog = new JDialog(owner, "Group colour", Dialog.ModalityType.APPLICATION_MODAL);
		Integer[] border = {group.getColour()};
		Integer[] fill = {group.getFillColour()};
		GroupColourSquare tileSquare = new GroupColourSquare();
		GroupColourSquare fillSquare = new GroupColourSquare();
		Runnable refresh = () ->
		{
			boolean tileOwn = border[0] != null;
			tileSquare.show(
				groupBorderShown(border[0]),
				tileOwn,
				tileOwn
					? "Tile colour for this group. Right-click to use the style colour."
					: "Using the style colour. Click to set a tile colour for this group.");
			boolean fillOwn = fill[0] != null;
			String fillTip;
			if (fillOwn)
			{
				fillTip = "Fill colour for this group. Right-click to use the style fill.";
			}
			else if (border[0] != null)
			{
				fillTip = "Using the group colour for the fill. Click to set a fill.";
			}
			else
			{
				fillTip = "Using the style fill. Click to set a fill for this group.";
			}
			fillSquare.show(
				MarkerLooks.tileFill(config, MarkerStyle.TILE, null, border[0], fill[0]),
				fillOwn,
				fillTip);
		};
		tileSquare.onPress(() ->
		{
			if (border[0] != null)
			{
				border[0] = null;
				listener.setGroupColour(groupId, null);
				refresh.run();
			}
		}, () -> openPicker(dialog, groupBorderShown(border[0]), "Tile colour", true, colour ->
		{
			border[0] = colour.getRGB();
			listener.setGroupColour(groupId, colour);
			refresh.run();
		}));
		fillSquare.onPress(() ->
		{
			if (fill[0] != null)
			{
				fill[0] = null;
				listener.setGroupFillColour(groupId, null);
				refresh.run();
			}
		}, () -> openPicker(dialog, MarkerLooks.tileFill(config, MarkerStyle.TILE, null, border[0], fill[0]), "Fill colour", true, colour ->
		{
			fill[0] = colour.getRGB();
			listener.setGroupFillColour(groupId, colour);
			refresh.run();
		}));
		refresh.run();

		JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setBorder(new EmptyBorder(10, 12, 10, 12));
		body.add(groupColourRow("Tile", tileSquare));
		body.add(Box.createVerticalStrut(6));
		body.add(groupColourRow("Fill", fillSquare));
		body.add(Box.createVerticalStrut(8));
		JLabel hint = new JLabel("A tile with its own colour keeps it.");
		hint.setFont(SMALL_FONT);
		hint.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		body.add(hint);
		body.add(Box.createVerticalStrut(8));
		JButton close = new JButton("Close");
		close.addActionListener(event -> dialog.dispose());
		JPanel closeRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		closeRow.setOpaque(false);
		closeRow.add(close);
		body.add(closeRow);
		dialog.setContentPane(body);
		dialog.pack();
		dialog.setResizable(false);
		dialog.setLocationRelativeTo(owner != null ? owner : this);
		dialog.setVisible(true);
	}

	private Color groupBorderShown(Integer stored)
	{
		if (stored != null)
		{
			Color colour = new Color(stored, true);
			return new Color(colour.getRed(), colour.getGreen(), colour.getBlue());
		}
		Color border = config.tileBorderColour();
		return border == null ? Color.WHITE : border;
	}

	private static JComponent groupColourRow(String name, JComponent square)
	{
		JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		row.setOpaque(false);
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		JLabel label = new JLabel(name);
		label.setFont(UI_FONT);
		label.setPreferredSize(new Dimension(36, 22));
		row.add(label);
		row.add(square);
		return row;
	}

	private Integer groupColourOf(TileMarker marker)
	{
		if (marker.getGroupId() == null)
		{
			return null;
		}
		for (MarkerGroup group : snapshot.getGroups())
		{
			if (group != null && marker.getGroupId().equals(group.getId()))
			{
				return group.getColour();
			}
		}
		return null;
	}

	private Integer groupFillOf(TileMarker marker)
	{
		if (marker.getGroupId() == null)
		{
			return null;
		}
		for (MarkerGroup group : snapshot.getGroups())
		{
			if (group != null && marker.getGroupId().equals(group.getId()))
			{
				return group.getFillColour();
			}
		}
		return null;
	}

	private Color shownTileColour(TileMarker marker, TileColourSlot slot, Integer custom, Integer groupColour, Integer groupFill)
	{
		MarkerStyle style = marker.getStyle() == null ? MarkerStyle.TILE : marker.getStyle();
		if (slot == TileColourSlot.LABEL)
		{
			return shownColour(custom, config.labelTextColour());
		}
		if (slot == TileColourSlot.FILL)
		{
			return MarkerLooks.tileFill(config, style, custom, groupColour, groupFill);
		}
		return MarkerLooks.tileBorder(config, style, custom, groupColour);
	}

	private static Integer tileColourValue(TileMarker marker, TileColourSlot slot)
	{
		if (slot == TileColourSlot.FILL)
		{
			return marker.getFillColour();
		}
		if (slot == TileColourSlot.LABEL)
		{
			return marker.getLabelColour();
		}
		return marker.getBorderColour();
	}

	private static String tileColourTitle(TileColourSlot slot)
	{
		if (slot == TileColourSlot.FILL)
		{
			return "Fill colour";
		}
		if (slot == TileColourSlot.LABEL)
		{
			return "Label colour";
		}
		return "Tile colour";
	}

	private static String tileColourIdle(TileMarker marker, TileColourSlot slot, Integer groupColour, Integer groupFill)
	{
		if (slot == TileColourSlot.FILL && (groupFill != null || groupColour != null))
		{
			return "Using the group colour. Click to set this tile.";
		}
		if (slot == TileColourSlot.TILE && groupColour != null)
		{
			return "Using the group colour. Click to set this tile.";
		}
		String style = styleName(marker.getStyle());
		if (slot == TileColourSlot.LABEL)
		{
			return "Using the label colour from settings. Click to set this tile.";
		}
		if (slot == TileColourSlot.FILL)
		{
			return "Using the " + style + " fill from settings. Click to set this tile.";
		}
		return "Using the " + style + " colour from settings. Click to set this tile.";
	}

	private JComponent colourSwatch(ObjectMarker marker, ObjectColourSlot slot, String title)
	{
		Integer custom = colourValue(marker, slot);
		Color shown = shownColour(custom, defaultColour(slot));
		boolean own = custom != null;
		return colourSquare(
			shown,
			own,
			title + " for this object. Right-click to use the default.",
			"Using the default " + title.toLowerCase() + ". Click to choose one for this object.",
			() ->
			{
				if (own && listener != null)
				{
					listener.setObjectColour(marker.ref(), slot, null);
				}
			},
			() -> chooseObjectColour(marker, slot, shown, title));
	}

	private JComponent colourSquare(Color shown, boolean own, String ownTip, String idleTip, Runnable clear, Runnable choose)
	{
		Color paint = shown == null ? Color.WHITE : shown;
		JPanel square = new JPanel()
		{
			@Override
			protected void paintComponent(Graphics graphics)
			{
				Graphics2D g2 = (Graphics2D) graphics.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(paint);
				g2.fillRoundRect(0, 0, Math.max(0, getWidth() - 1), Math.max(0, getHeight() - 1), 3, 3);
				g2.setColor(own ? Color.WHITE : new Color(255, 255, 255, 70));
				g2.drawRoundRect(0, 0, Math.max(0, getWidth() - 1), Math.max(0, getHeight() - 1), 3, 3);
				g2.dispose();
			}
		};
		square.setOpaque(false);
		square.setPreferredSize(new Dimension(12, 12));
		square.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		square.setToolTipText(own ? ownTip : idleTip);
		square.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent event)
			{
				if (SwingUtilities.isRightMouseButton(event))
				{
					clear.run();
					return;
				}
				if (SwingUtilities.isLeftMouseButton(event))
				{
					choose.run();
				}
			}
		});

		JPanel box = new JPanel(new GridBagLayout());
		box.setOpaque(false);
		box.setPreferredSize(new Dimension(14, 22));
		box.add(square);
		return box;
	}

	private void openPicker(Color current, String title, Consumer<Color> apply)
	{
		openPicker(SwingUtilities.getWindowAncestor(this), current, title, false, apply);
	}

	private void openPicker(Window owner, Color current, String title, boolean keepSame, Consumer<Color> apply)
	{
		if (colorPickerManager == null || listener == null)
		{
			return;
		}
		Color start = current == null ? Color.WHITE : current;
		RuneliteColorPicker picker = colorPickerManager.create(owner, start, title, false);
		picker.setOnClose(colour -> SwingUtilities.invokeLater(() ->
		{
			if (listener == null || colour == null || (!keepSame && colour.equals(start)))
			{
				return;
			}
			apply.accept(colour);
		}));
		// A dialog with no location stays at the monitor origin. Place it on the client.
		picker.setLocationRelativeTo(owner != null ? owner : this);
		picker.setVisible(true);
	}

	private void chooseObjectColour(ObjectMarker marker, ObjectColourSlot slot, Color current, String title)
	{
		openPicker(current, title, colour ->
		{
			Color fallback = defaultColour(slot);
			if (fallback != null && colour.equals(fallback))
			{
				listener.setObjectColour(marker.ref(), slot, null);
			}
			else
			{
				listener.setObjectColour(marker.ref(), slot, colour);
			}
		});
	}

	private static Integer colourValue(ObjectMarker marker, ObjectColourSlot slot)
	{
		switch (slot)
		{
			case TEXT:
				return marker.getTextColour();
			case OUTLINE:
				return marker.getOutlineColour();
			case HULL:
				return marker.getHullColour();
			case TILE:
				return marker.getTileColour();
			case CLICKBOX:
				return marker.getClickboxColour();
			default:
				return null;
		}
	}

	private Color defaultColour(ObjectColourSlot slot)
	{
		switch (slot)
		{
			case TEXT:
				return config.objectTextColour();
			case OUTLINE:
				return config.objectOutlineColour();
			case HULL:
				return config.objectHullColour();
			case TILE:
				return config.objectTileColour();
			case CLICKBOX:
				return config.objectClickboxColour();
			default:
				return null;
		}
	}

	private static Color shownColour(Integer custom, Color fallback)
	{
		if (custom != null)
		{
			return new Color(custom, true);
		}
		return fallback == null ? Color.WHITE : fallback;
	}

	private static JComponent beside(JComponent pill, JComponent swatch)
	{
		JPanel pair = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
		pair.setOpaque(false);
		pair.add(pill);
		pair.add(swatch);
		return pair;
	}

	private static JLabel sectionTitle(String text)
	{
		JLabel caption = new JLabel(text, SwingConstants.CENTER);
		caption.setFont(SMALL_FONT);
		caption.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		caption.setAlignmentX(Component.CENTER_ALIGNMENT);
		return caption;
	}

	private TogglePill labelPill(ObjectMarker marker)
	{
		boolean editor = labelEditors.contains(marker.ref());
		boolean shown = marker.hasLabel();
		String draft = objectLabelDrafts.get(marker.ref());
		boolean kept = draft != null && !draft.isEmpty();
		TogglePill pill = new TogglePill("Label");
		pill.putClientProperty(LABEL_TOGGLE, Boolean.TRUE);
		pill.setOn(shown || editor);
		if (editor || shown)
		{
			pill.setToolTipText("Hide the label");
		}
		else if (kept)
		{
			pill.setToolTipText("Show the label");
		}
		else
		{
			pill.setToolTipText("Add a label");
		}
		pill.addActionListener(event -> toggleObjectLabel(marker, pill));
		return pill;
	}

	private JComponent labelFieldRow(ObjectMarker marker)
	{
		boolean focus = focusLabel != null && focusLabel.equals(marker.ref());
		JTextField field = labelEditor(
			objectLabelFieldText(marker),
			text -> commitObjectLabel(marker, text),
			text -> previewObjectLabel(marker, text),
			focus);
		return centeredField(field);
	}

	private String objectLabelFieldText(ObjectMarker marker)
	{
		if (marker.hasLabel())
		{
			return marker.getLabel().trim();
		}
		String draft = objectLabelDrafts.get(marker.ref());
		return draft == null ? "" : draft;
	}

	private TogglePill tileLabelPill(TileMarker marker)
	{
		boolean editor = tileLabelEditors.contains(marker.location());
		boolean shown = labelled(marker.getLabel());
		String draft = tileLabelDrafts.get(marker.location());
		boolean kept = draft != null && !draft.isEmpty();
		TogglePill pill = new TogglePill("Label");
		pill.putClientProperty(LABEL_TOGGLE, Boolean.TRUE);
		pill.setOn(shown || editor);
		if (editor || shown)
		{
			pill.setToolTipText("Hide the label");
		}
		else if (kept)
		{
			pill.setToolTipText("Show the label");
		}
		else
		{
			pill.setToolTipText("Add a label");
		}
		pill.addActionListener(event -> toggleTileLabel(marker, pill));
		return pill;
	}

	private JButton tileFlagButton(TileMarker marker, boolean corners)
	{
		MarkerStyle style = marker.getStyle() == null ? MarkerStyle.TILE : marker.getStyle();
		Boolean override = corners ? marker.getCornersOnly() : marker.getMergeBorders();
		boolean effective = corners
			? MarkerLooks.cornersOnly(config, style, override)
			: MarkerLooks.merge(config, style, override);
		JButton button = corners ? new CornerMarkButton(effective) : new MergeMarkButton(effective);
		button.setToolTipText(tileFlagTip(corners, effective, override != null));
		button.addActionListener(event ->
		{
			if (listener == null)
			{
				return;
			}
			Boolean next = override == null ? !effective : null;
			if (corners)
			{
				listener.setTileCorners(marker.location(), next);
			}
			else
			{
				listener.setTileMerge(marker.location(), next);
			}
		});
		return button;
	}

	private static String tileFlagTip(boolean corners, boolean effective, boolean pinned)
	{
		String name = corners ? "Corners" : "Merge";
		if (!pinned)
		{
			if (effective)
			{
				return corners
					? "Following Corners (on). Click to turn corners off for this tile."
					: "Following Merge (on). Click to stop merging this tile.";
			}
			return corners
				? "Following Corners (off). Click to draw corners on this tile."
				: "Following Merge (off). Click to merge this tile.";
		}
		if (effective)
		{
			return name + " on for this tile. Click to follow the " + name + " setting.";
		}
		return name + " off for this tile. Click to follow the " + name + " setting.";
	}

	private JButton groupMarkButton(boolean corners, boolean marked, Runnable apply)
	{
		JButton button = corners ? new CornerMarkButton(marked) : new MergeMarkButton(marked);
		if (marked)
		{
			button.setToolTipText(corners
				? "Follow the Corners setting for this group"
				: "Follow the Merge setting for this group");
		}
		else
		{
			button.setToolTipText(corners
				? "Draw corners for every tile in this group"
				: "Merge borders for every tile in this group");
		}
		button.addActionListener(event -> apply.run());
		return button;
	}

	private JButton groupBrush(MarkerGroup group)
	{
		boolean marked = group.getColour() != null || group.getFillColour() != null;
		BrushButton button = new BrushButton(marked);
		button.setToolTipText(marked
			? "Group colour is set. Click to change the tile and fill. Right-click to use the style colours."
			: "Set the tile and fill colours for this group. A tile with its own colour keeps it.");
		button.addActionListener(event -> chooseGroupColour(group));
		button.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent event)
			{
				if (SwingUtilities.isRightMouseButton(event) && marked && listener != null)
				{
					listener.setGroupColour(group.getId(), null);
					listener.setGroupFillColour(group.getId(), null);
				}
			}
		});
		return button;
	}

	private JButton copyButton(MarkerGroup group)
	{
		CopyButton button = new CopyButton();
		String noun = group.getKind() == GroupKind.OBJECT ? "objects" : "tiles";
		button.setToolTipText("Copy this group's " + noun + ". Import them from the groups tab.");
		button.addActionListener(event -> copyGroup(group));
		return button;
	}

	private void copyGroup(MarkerGroup group)
	{
		if (listener == null || group.getId() == null)
		{
			return;
		}
		String text = listener.copyGroup(group.getId());
		if (text == null)
		{
			return;
		}
		try
		{
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
		}
		catch (RuntimeException ex)
		{
			JTextArea area = new JTextArea(text, 8, 28);
			area.setLineWrap(true);
			area.setEditable(false);
			JOptionPane.showMessageDialog(
				SwingUtilities.getWindowAncestor(this),
				new JScrollPane(area),
				"Copy this group",
				JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private void beginImport()
	{
		if (listener == null)
		{
			return;
		}
		GroupKind kind = activeKind();
		JTextArea area = new JTextArea(8, 28);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setFont(UI_FONT);
		String clip = clipboardText();
		if (GroupShare.looksLike(clip))
		{
			area.setText(clip);
			area.setCaretPosition(0);
		}
		JPanel form = new JPanel(new BorderLayout(0, 6));
		form.setOpaque(false);
		JLabel help = new JLabel("<html>Paste a copied group. A tile group only imports on Tile Groups,<br>and an object group only imports on Object Groups.</html>");
		help.setFont(SMALL_FONT);
		form.add(help, BorderLayout.NORTH);
		form.add(new JScrollPane(area), BorderLayout.CENTER);
		String title = kind == GroupKind.OBJECT ? "Import object group" : "Import tile group";
		int result = JOptionPane.showOptionDialog(
			SwingUtilities.getWindowAncestor(this),
			form,
			title,
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.PLAIN_MESSAGE,
			null,
			new Object[]{"Import", "Cancel"},
			"Import");
		if (result != 0)
		{
			return;
		}
		listener.importGroup(kind, area.getText(), message -> JOptionPane.showMessageDialog(
			SwingUtilities.getWindowAncestor(this),
			message,
			"Import group",
			JOptionPane.INFORMATION_MESSAGE));
	}

	private static String clipboardText()
	{
		try
		{
			Object data = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
			return data instanceof String ? (String) data : "";
		}
		catch (Exception ex)
		{
			return "";
		}
	}

	private boolean tileLabelFieldOpen(TileMarker marker)
	{
		return tileLabelEditors.contains(marker.location());
	}

	private String tileLabelFieldText(TileMarker marker)
	{
		if (labelled(marker.getLabel()))
		{
			return marker.getLabel();
		}
		String draft = tileLabelDrafts.get(marker.location());
		return draft == null ? "" : draft;
	}

	private void toggleTileLabel(TileMarker marker, Component source)
	{
		MarkerLocation location = marker.location();
		String typed = fieldText(source);
		if (typed == null)
		{
			if (labelled(marker.getLabel()))
			{
				typed = marker.getLabel().trim();
			}
			else
			{
				String draft = tileLabelDrafts.get(location);
				typed = draft == null ? "" : draft;
			}
		}
		typed = typed.trim();
		boolean on = tileLabelEditors.contains(location) || labelled(marker.getLabel());
		if (on)
		{
			tileLabelEditors.remove(location);
			if (typed.isEmpty())
			{
				tileLabelDrafts.remove(location);
			}
			else
			{
				tileLabelDrafts.put(location, typed);
			}
			if (listener != null)
			{
				listener.setTileLabel(location, "");
				return;
			}
			rebuild();
			syncGroupHints();
			return;
		}
		tileLabelEditors.add(location);
		focusTileLabel = location;
		if (listener != null)
		{
			listener.setTileLabel(location, typed);
			return;
		}
		rebuild();
		syncGroupHints();
		focusTileLabel = null;
	}

	private static String fieldText(Component source)
	{
		JTextField field = findField(source);
		if (field == null)
		{
			return null;
		}
		String text = field.getText();
		return text == null ? "" : text;
	}

	/** The label field sits in the options block under the pill, not beside it. */
	private static JTextField findField(Component source)
	{
		Container container = source == null ? null : source.getParent();
		while (container != null)
		{
			if (container.getLayout() instanceof BoxLayout)
			{
				return firstField(container);
			}
			container = container.getParent();
		}
		return null;
	}

	private static JTextField firstField(Container container)
	{
		for (Component child : container.getComponents())
		{
			if (child instanceof JTextField)
			{
				return (JTextField) child;
			}
			if (child instanceof Container)
			{
				JTextField nested = firstField((Container) child);
				if (nested != null)
				{
					return nested;
				}
			}
		}
		return null;
	}

	private static JComponent centeredField(JTextField field)
	{
		JPanel holder = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		holder.setOpaque(false);
		holder.setAlignmentX(Component.CENTER_ALIGNMENT);
		holder.add(field);
		Dimension pref = holder.getPreferredSize();
		holder.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));
		return holder;
	}

	private void toggleObjectLabel(ObjectMarker marker, Component source)
	{
		ObjectRef ref = marker.ref();
		String typed = fieldText(source);
		if (typed == null)
		{
			if (marker.hasLabel())
			{
				typed = marker.getLabel().trim();
			}
			else
			{
				String draft = objectLabelDrafts.get(ref);
				typed = draft == null ? "" : draft;
			}
		}
		typed = typed.trim();
		boolean on = labelEditors.contains(ref) || marker.hasLabel();
		if (on)
		{
			labelEditors.remove(ref);
			if (typed.isEmpty())
			{
				objectLabelDrafts.remove(ref);
			}
			else
			{
				objectLabelDrafts.put(ref, typed);
			}
			if (listener != null)
			{
				listener.setObjectLabel(ref, "");
				return;
			}
			rebuild();
			syncGroupHints();
			return;
		}
		labelEditors.add(ref);
		focusLabel = ref;
		if (listener != null)
		{
			listener.setObjectLabel(ref, typed);
			return;
		}
		rebuild();
		syncGroupHints();
		focusLabel = null;
	}

	private void toggleTileExpanded(MarkerLocation location)
	{
		if (!expandedTiles.add(location))
		{
			expandedTiles.remove(location);
		}
		rebuild();
		syncGroupHints();
	}

	private JComponent tileDetail(String detail, TileMarker marker, Color colour)
	{
		boolean expanded = expandedTiles.contains(marker.location());
		JLabel detailLabel = new JLabel(detail);
		detailLabel.setFont(SMALL_FONT);
		detailLabel.setForeground(colour == null ? Color.WHITE : colour);
		Dimension labelSize = detailLabel.getPreferredSize();
		detailLabel.setMinimumSize(new Dimension(0, labelSize.height));
		JButton chevron = new ChevronButton(!expanded, 16);
		chevron.addActionListener(event -> toggleTileExpanded(marker.location()));
		return detailLine(detailLabel, chevron);
	}

	private void toggleObjectExpanded(ObjectRef ref)
	{
		if (!expandedObjects.add(ref))
		{
			expandedObjects.remove(ref);
		}
		rebuild();
		syncGroupHints();
	}

	private void collapseExpanded()
	{
		if (activeKind() == GroupKind.OBJECT)
		{
			if (expandedObjects.isEmpty())
			{
				return;
			}
			expandedObjects.clear();
		}
		else if (expandedTiles.isEmpty())
		{
			return;
		}
		else
		{
			expandedTiles.clear();
		}
		rebuild();
		syncGroupHints();
	}

	private JComponent objectDetail(String detail, ObjectMarker marker, Color colour)
	{
		boolean expanded = expandedObjects.contains(marker.ref());
		JLabel detailLabel = new JLabel(detail);
		detailLabel.setFont(SMALL_FONT);
		detailLabel.setForeground(colour == null ? Color.WHITE : colour);
		Dimension labelSize = detailLabel.getPreferredSize();
		detailLabel.setMinimumSize(new Dimension(0, labelSize.height));
		JButton chevron = new ChevronButton(!expanded, 16);
		chevron.addActionListener(event -> toggleObjectExpanded(marker.ref()));
		return detailLine(detailLabel, chevron);
	}

	private static JComponent detailLine(JLabel detailLabel, JButton chevron)
	{
		JPanel line = new JPanel(new BorderLayout(2, 0))
		{
			@Override
			public Dimension getPreferredSize()
			{
				return new Dimension(1, Math.max(16, super.getPreferredSize().height));
			}

			@Override
			public Dimension getMinimumSize()
			{
				return new Dimension(1, getPreferredSize().height);
			}

			@Override
			public Dimension getMaximumSize()
			{
				return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
			}
		};
		line.setOpaque(false);
		line.setAlignmentX(Component.LEFT_ALIGNMENT);
		line.add(detailLabel, BorderLayout.CENTER);
		line.add(chevron, BorderLayout.EAST);
		return line;
	}

	private static String objectNameTitle(ObjectMarker marker)
	{
		if (marker.getName() != null && !marker.getName().isEmpty())
		{
			return marker.getName();
		}
		return "Object " + marker.getObjectId();
	}

	private static boolean labelled(String label)
	{
		return label != null && !label.isEmpty();
	}

	private JTextField labelEditor(String text, Consumer<String> commit, Consumer<String> live, boolean focus)
	{
		JTextField field = new JTextField(text == null ? "" : text);
		field.setFont(UI_FONT);
		field.setForeground(Color.WHITE);
		field.setCaretColor(Color.WHITE);
		field.setBackground(FIELD);
		field.setOpaque(true);
		field.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(FIELD_BORDER),
			new EmptyBorder(2, 4, 2, 4)));
		field.setAlignmentX(Component.LEFT_ALIGNMENT);
		field.setPreferredSize(new Dimension(160, 22));
		field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
		field.setToolTipText("Label");
		if (live != null)
		{
			String[] sent = {text == null ? "" : text.trim()};
			field.getDocument().addDocumentListener(new DocumentListener()
			{
				@Override
				public void insertUpdate(DocumentEvent event)
				{
					send();
				}

				@Override
				public void removeUpdate(DocumentEvent event)
				{
					send();
				}

				@Override
				public void changedUpdate(DocumentEvent event)
				{
					send();
				}

				private void send()
				{
					String next = field.getText() == null ? "" : field.getText().trim();
					if (next.equals(sent[0]))
					{
						return;
					}
					sent[0] = next;
					live.accept(next);
				}
			});
		}
		field.addActionListener(event -> commit.accept(field.getText()));
		field.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent event)
			{
				// The label button reads the field in its own action. Saving first would
				// apply or clear the label before that click can decide.
				if (isLabelToggle(event.getOppositeComponent()))
				{
					return;
				}
				commit.accept(field.getText());
			}
		});
		if (focus)
		{
			SwingUtilities.invokeLater(() ->
			{
				if (field.isShowing())
				{
					field.requestFocusInWindow();
				}
			});
		}
		return field;
	}

	private void previewTileLabel(TileMarker marker, String text)
	{
		if (listener == null || !tileLabelEditors.contains(marker.location()))
		{
			return;
		}
		listener.previewTileLabel(marker.location(), text);
	}

	private void commitTileLabel(TileMarker marker, String text)
	{
		if (listener == null || !tileLabelEditors.contains(marker.location()))
		{
			return;
		}
		String next = text == null ? "" : text.trim();
		String current = marker.getLabel() == null ? "" : marker.getLabel().trim();
		if (next.equals(current))
		{
			return;
		}
		listener.setTileLabel(marker.location(), next);
	}

	private void previewObjectLabel(ObjectMarker marker, String text)
	{
		if (listener == null || !labelEditors.contains(marker.ref()))
		{
			return;
		}
		listener.previewObjectLabel(marker.ref(), text);
	}

	private void commitObjectLabel(ObjectMarker marker, String text)
	{
		if (listener == null || !labelEditors.contains(marker.ref()))
		{
			return;
		}
		String next = text == null ? "" : text.trim();
		String current = marker.hasLabel() ? marker.getLabel().trim() : "";
		if (next.equals(current))
		{
			return;
		}
		listener.setObjectLabel(marker.ref(), next);
	}

	private static boolean isLabelToggle(Component component)
	{
		return component instanceof JComponent
			&& Boolean.TRUE.equals(((JComponent) component).getClientProperty(LABEL_TOGGLE));
	}

	private void armRowPress(Component component, MouseAdapter adapter, Set<Component> skip)
	{
		if (component instanceof JTextField || component instanceof JButton || (skip != null && skip.contains(component)))
		{
			return;
		}
		component.addMouseListener(adapter);
		if (component instanceof Container)
		{
			for (Component child : ((Container) component).getComponents())
			{
				armRowPress(child, adapter, skip);
			}
		}
	}

	private TogglePill displayToggle(String text, boolean on, String tip, Runnable toggle)
	{
		TogglePill pill = new TogglePill(text);
		pill.setOn(on);
		pill.setToolTipText(tip);
		pill.addActionListener(event -> toggle.run());
		return pill;
	}

	private RoundPanel row(boolean on, String title, String detail, JComponent detailLine, JComponent extra, JComponent remove, Color titleColour)
	{
		RoundPanel row = new RoundPanel(on ? CARD_SELECTED : CARD);
		row.setLayout(new BorderLayout(6, 0));
		row.setBorder(new EmptyBorder(7, on ? 8 : 10, 7, 8));

		Color textColour = titleColour == null ? Color.WHITE : titleColour;
		JLabel titleLabel = new JLabel(title);
		titleLabel.setFont(UI_FONT);
		titleLabel.setForeground(textColour);

		JComponent detailComponent = detailLine;
		if (detailComponent == null)
		{
			JLabel detailLabel = new JLabel(detail);
			detailLabel.setFont(SMALL_FONT);
			detailLabel.setForeground(textColour);
			detailComponent = detailLabel;
		}

		JPanel text = new JPanel();
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		text.setOpaque(false);
		text.add(titleLabel);
		text.add(Box.createVerticalStrut(2));
		text.add(detailComponent);
		if (extra != null)
		{
			text.add(Box.createVerticalStrut(4));
			extra.setAlignmentX(Component.LEFT_ALIGNMENT);
			text.add(extra);
		}
		row.add(text, BorderLayout.CENTER);
		if (remove != null)
		{
			row.add(remove, BorderLayout.EAST);
		}
		row.setToolTipText(title + "  " + detail);
		stretch(row);
		return row;
	}

	private JButton crossButton(Runnable action)
	{
		JButton button = new CrossButton();
		button.addActionListener(event -> action.run());
		return button;
	}

	private Collection<MarkerLocation> targetsFor(TileMarker marker)
	{
		if (selected.contains(marker.location()))
		{
			return new ArrayList<>(selected);
		}
		return Collections.singletonList(marker.location());
	}

	private Collection<ObjectRef> targetsFor(ObjectMarker marker)
	{
		if (selectedObjects.contains(marker.ref()))
		{
			return new ArrayList<>(selectedObjects);
		}
		return Collections.singletonList(marker.ref());
	}

	private void selectTile(TileMarker marker, int index, boolean shift, List<TileMarker> order)
	{
		MarkerLocation location = marker.location();
		if (shift)
		{
			int anchor = indexOfTile(order, tileAnchorLocation);
			if (anchor >= 0)
			{
				int from = Math.min(anchor, index);
				int to = Math.max(anchor, index);
				for (int i = from; i <= to; i++)
				{
					selected.add(order.get(i).location());
				}
				repository.setHighlighted(new HashSet<>(selected));
				rebuildTiles();
				syncGroupHints();
				return;
			}
		}
		if (!selected.remove(location))
		{
			selected.add(location);
		}
		tileAnchorLocation = location;
		repository.setHighlighted(new HashSet<>(selected));
		rebuildTiles();
		syncGroupHints();
	}

	private void selectObject(ObjectMarker marker, int index, boolean shift, List<ObjectMarker> order)
	{
		ObjectRef ref = marker.ref();
		if (shift)
		{
			int anchor = indexOfObject(order, objectAnchorLocation);
			if (anchor >= 0)
			{
				int from = Math.min(anchor, index);
				int to = Math.max(anchor, index);
				for (int i = from; i <= to; i++)
				{
					selectedObjects.add(order.get(i).ref());
				}
				repository.setHighlightedObjects(new HashSet<>(selectedObjects));
				rebuildObjects();
				syncGroupHints();
				return;
			}
		}
		if (!selectedObjects.remove(ref))
		{
			selectedObjects.add(ref);
		}
		objectAnchorLocation = ref;
		repository.setHighlightedObjects(new HashSet<>(selectedObjects));
		rebuildObjects();
		syncGroupHints();
	}

	private static int indexOfTile(List<TileMarker> order, MarkerLocation location)
	{
		if (location == null || order == null)
		{
			return -1;
		}
		for (int i = 0; i < order.size(); i++)
		{
			if (location.equals(order.get(i).location()))
			{
				return i;
			}
		}
		return -1;
	}

	private static int indexOfObject(List<ObjectMarker> order, ObjectRef ref)
	{
		if (ref == null || order == null)
		{
			return -1;
		}
		for (int i = 0; i < order.size(); i++)
		{
			if (ref.equals(order.get(i).ref()))
			{
				return i;
			}
		}
		return -1;
	}

	private void showMemberMenu(
		Component row,
		MouseEvent event,
		GroupKind kind,
		Collection<MarkerLocation> tileTargets,
		Collection<ObjectRef> objectTargets)
	{
		JPopupMenu menu = new JPopupMenu();
		JMenu add = new JMenu("Add to group");
		List<MarkerGroup> groups = groupsOf(kind);
		if (groups.isEmpty())
		{
			add.setEnabled(false);
		}
		for (MarkerGroup group : groups)
		{
			JMenuItem item = new JMenuItem(group.getName());
			item.addActionListener(action ->
			{
				if (kind == GroupKind.OBJECT)
				{
					assignObjects(objectTargets, group.getId());
				}
				else
				{
					assignTiles(tileTargets, group.getId());
				}
			});
			add.add(item);
		}
		menu.add(add);

		JMenuItem create = new JMenuItem("New group");
		create.addActionListener(action ->
		{
			if (kind == GroupKind.OBJECT)
			{
				beginNaming(false, null, new ArrayList<>(objectTargets));
			}
			else
			{
				beginNaming(false, new ArrayList<>(tileTargets), null);
			}
		});
		menu.add(create);

		if (targetsGrouped(kind, tileTargets, objectTargets))
		{
			JMenuItem remove = new JMenuItem("Remove from group");
			remove.addActionListener(action ->
			{
				if (listener == null)
				{
					return;
				}
				if (kind == GroupKind.OBJECT)
				{
					listener.unassignObjects(objectTargets);
				}
				else
				{
					listener.unassign(tileTargets);
				}
			});
			menu.add(remove);
		}
		menu.addSeparator();

		JMenuItem unmark = new JMenuItem("Unmark");
		unmark.addActionListener(action ->
		{
			if (kind == GroupKind.OBJECT)
			{
				removeObjects(objectTargets);
			}
			else
			{
				removeTiles(tileTargets);
			}
		});
		menu.add(unmark);
		menu.show(row, event.getX(), event.getY());
	}

	private void assignTiles(Collection<MarkerLocation> targets, String groupId)
	{
		selected.removeAll(targets);
		tileAnchorLocation = null;
		repository.setHighlighted(new HashSet<>(selected));
		if (listener != null)
		{
			listener.assign(targets, groupId);
		}
		rebuildTiles();
		syncGroupHints();
	}

	private void removeTiles(Collection<MarkerLocation> targets)
	{
		selected.removeAll(targets);
		tileAnchorLocation = null;
		repository.setHighlighted(new HashSet<>(selected));
		if (listener != null)
		{
			listener.unmark(targets);
		}
		rebuildTiles();
		syncGroupHints();
	}

	private void assignObjects(Collection<ObjectRef> targets, String groupId)
	{
		selectedObjects.removeAll(targets);
		objectAnchorLocation = null;
		repository.setHighlightedObjects(new HashSet<>(selectedObjects));
		if (listener != null)
		{
			listener.assignObjects(targets, groupId);
		}
		rebuildObjects();
		syncGroupHints();
	}

	private void removeObjects(Collection<ObjectRef> targets)
	{
		selectedObjects.removeAll(targets);
		objectAnchorLocation = null;
		repository.setHighlightedObjects(new HashSet<>(selectedObjects));
		if (listener != null)
		{
			listener.unmarkObjects(targets);
		}
		rebuildObjects();
		syncGroupHints();
	}

	private void clearActiveSelection()
	{
		if (activeKind() == GroupKind.OBJECT)
		{
			if (selectedObjects.isEmpty())
			{
				return;
			}
			selectedObjects.clear();
			objectAnchorLocation = null;
			repository.setHighlightedObjects(Collections.emptySet());
			rebuildObjects();
		}
		else
		{
			if (selected.isEmpty())
			{
				return;
			}
			selected.clear();
			tileAnchorLocation = null;
			repository.setHighlighted(Collections.emptySet());
			rebuildTiles();
		}
		syncGroupHints();
	}

	private void syncGroupHints()
	{
		if (!built)
		{
			return;
		}
		int tileScroll = tileGroupsScroll.getVerticalScrollBar().getValue();
		int objectScroll = objectGroupsScroll.getVerticalScrollBar().getValue();
		rebuildGroupList(tileGroupsBody, GroupKind.TILE, "No tile groups yet. Add one to organise marked tiles.");
		rebuildGroupList(objectGroupsBody, GroupKind.OBJECT, "No object groups yet. Add one to organise marked objects.");
		SwingUtilities.invokeLater(() ->
		{
			tileGroupsScroll.getVerticalScrollBar().setValue(tileScroll);
			objectGroupsScroll.getVerticalScrollBar().setValue(objectScroll);
		});
	}

	private void updateActions()
	{
		if (actionRow == null || selectionActions == null)
		{
			return;
		}
		boolean list = listView();
		boolean pending = activeKind() == GroupKind.OBJECT ? !selectedObjects.isEmpty() : !selected.isEmpty();
		boolean showNew = !list && !pending;
		boolean showImport = !list;
		boolean show = pending || showNew;
		boolean moved = placeGroupActions(list, pending);
		boolean layout = actionRow.isVisible() != show
			|| selectionActions.isVisible() != pending
			|| groupBar.isVisible() != showNew
			|| moved;
		selectionActions.setVisible(pending);
		groupBar.setVisible(showNew);
		importGroupButton.setVisible(showImport);
		actionRow.setVisible(show);
		if (show)
		{
			fitBar(actionRow);
		}
		if (layout)
		{
			revalidate();
			repaint();
		}
	}

	/**
	 * Import sits beside Create group on the groups tab. On the list it stays off that row.
	 * A component can only have one parent, so the button moves when the tab or selection changes.
	 */
	private boolean placeGroupActions(boolean list, boolean pending)
	{
		JPanel bottom = selectionBottomParent();
		if (bottom == null)
		{
			return false;
		}
		boolean importInSelection = !list && pending;
		if (importInSelection)
		{
			if (importGroupButton.getParent() == bottom)
			{
				return false;
			}
			bottom.removeAll();
			bottom.add(createGroupButton);
			bottom.add(importGroupButton);
			bottom.add(addToGroupButton);
			groupBar.removeAll();
			groupBar.add(newGroupButton);
			bottom.revalidate();
			groupBar.revalidate();
			return true;
		}
		if (importGroupButton.getParent() == groupBar)
		{
			return false;
		}
		bottom.removeAll();
		bottom.add(createGroupButton);
		bottom.add(addToGroupButton);
		groupBar.removeAll();
		groupBar.add(newGroupButton);
		groupBar.add(importGroupButton);
		bottom.revalidate();
		groupBar.revalidate();
		return true;
	}

	private JPanel selectionBottomParent()
	{
		Container parent = createGroupButton.getParent();
		return parent instanceof JPanel ? (JPanel) parent : null;
	}

	private void deleteActiveSelection()
	{
		if (activeKind() == GroupKind.OBJECT)
		{
			if (selectedObjects.isEmpty())
			{
				return;
			}
			removeObjects(new ArrayList<>(selectedObjects));
			return;
		}
		if (selected.isEmpty())
		{
			return;
		}
		removeTiles(new ArrayList<>(selected));
	}

	private void beginNamingSelection()
	{
		if (activeKind() == GroupKind.OBJECT)
		{
			if (selectedObjects.isEmpty())
			{
				return;
			}
			beginNaming(false, null, new ArrayList<>(selectedObjects));
			return;
		}
		if (selected.isEmpty())
		{
			return;
		}
		beginNaming(false, new ArrayList<>(selected), null);
	}

	private void beginNaming(boolean empty, Collection<MarkerLocation> tiles, Collection<ObjectRef> objects)
	{
		namingEmpty = empty;
		namingTiles = tiles;
		namingObjects = objects;
		if (groupNameField != null)
		{
			groupNameField.setText("");
		}
		if (nameRow != null)
		{
			nameRow.setVisible(true);
			revalidate();
			repaint();
		}
		if (groupNameField != null)
		{
			groupNameField.requestFocusInWindow();
		}
	}

	private void hideGroupName()
	{
		namingEmpty = false;
		namingTiles = null;
		namingObjects = null;
		if (groupNameField != null)
		{
			groupNameField.setText("");
		}
		if (nameRow != null && nameRow.isVisible())
		{
			nameRow.setVisible(false);
			revalidate();
			repaint();
		}
	}

	private void saveGroupName()
	{
		if (listener == null || groupNameField == null)
		{
			return;
		}
		String name = groupNameField.getText() == null ? "" : groupNameField.getText().trim();
		if (name.isEmpty())
		{
			return;
		}
		if (namingEmpty)
		{
			listener.addNamedGroup(activeKind(), name);
		}
		else if (activeKind() == GroupKind.OBJECT)
		{
			Collection<ObjectRef> targets = namingObjects == null ? new ArrayList<>(selectedObjects) : namingObjects;
			if (targets.isEmpty())
			{
				return;
			}
			selectedObjects.removeAll(targets);
			objectAnchorLocation = null;
			repository.setHighlightedObjects(new HashSet<>(selectedObjects));
			listener.createNamedObjectGroup(name, targets);
		}
		else
		{
			Collection<MarkerLocation> targets = namingTiles == null ? new ArrayList<>(selected) : namingTiles;
			if (targets.isEmpty())
			{
				return;
			}
			selected.removeAll(targets);
			tileAnchorLocation = null;
			repository.setHighlighted(new HashSet<>(selected));
			listener.createNamedTileGroup(name, targets);
		}
		hideGroupName();
		rebuildLists();
		syncGroupHints();
	}

	private void showAddToGroupMenu(Component source)
	{
		if (activeKind() == GroupKind.OBJECT)
		{
			showGroupPopup(source, GroupKind.OBJECT, Collections.emptyList(), new ArrayList<>(selectedObjects));
			return;
		}
		showGroupPopup(source, GroupKind.TILE, new ArrayList<>(selected), Collections.emptyList());
	}

	private void showGroupPopup(Component source, GroupKind kind, Collection<MarkerLocation> tiles, Collection<ObjectRef> objects)
	{
		JPopupMenu menu = new JPopupMenu();
		List<MarkerGroup> groups = groupsOf(kind);
		if (groups.isEmpty())
		{
			JMenuItem none = new JMenuItem("No groups");
			none.setEnabled(false);
			menu.add(none);
		}
		for (MarkerGroup group : groups)
		{
			JMenuItem item = new JMenuItem(group.getName());
			item.addActionListener(action ->
			{
				if (kind == GroupKind.OBJECT)
				{
					assignObjects(objects, group.getId());
				}
				else
				{
					assignTiles(tiles, group.getId());
				}
			});
			menu.add(item);
		}
		menu.show(source, 0, source.getHeight());
	}

	private boolean targetsGrouped(GroupKind kind, Collection<MarkerLocation> tiles, Collection<ObjectRef> objects)
	{
		if (kind == GroupKind.OBJECT)
		{
			if (objects == null || objects.isEmpty())
			{
				return false;
			}
			Set<ObjectRef> wanted = new HashSet<>(objects);
			for (ObjectMarker marker : snapshot.getObjects())
			{
				if (wanted.contains(marker.ref()) && marker.getGroupId() != null)
				{
					return true;
				}
			}
			return false;
		}
		if (tiles == null || tiles.isEmpty())
		{
			return false;
		}
		Set<MarkerLocation> wanted = new HashSet<>(tiles);
		for (TileMarker marker : snapshot.getMarkers())
		{
			if (wanted.contains(marker.location()) && marker.getGroupId() != null)
			{
				return true;
			}
		}
		return false;
	}

	private Color groupBorderColour()
	{
		Color colour = config.groupBorderColour();
		return colour == null ? CARD_SELECTED : colour;
	}

	private static JPanel buttonRow()
	{
		JPanel row = new JPanel(new GridLayout(1, 0, 4, 0))
		{
			@Override
			public Dimension getMaximumSize()
			{
				Dimension pref = getPreferredSize();
				return new Dimension(Integer.MAX_VALUE, pref.height);
			}
		};
		row.setOpaque(false);
		row.setAlignmentX(Component.CENTER_ALIGNMENT);
		return row;
	}

	private static void fitBar(JComponent component)
	{
		component.setAlignmentX(Component.CENTER_ALIGNMENT);
		Dimension pref = component.getPreferredSize();
		component.setMaximumSize(new Dimension(Integer.MAX_VALUE, Math.max(1, pref.height)));
	}

	private Color tileTextColour()
	{
		Color colour = config.tileListTextColour();
		return colour == null ? Color.WHITE : colour;
	}

	private Color objectTextColour()
	{
		Color colour = config.objectListTextColour();
		return colour == null ? Color.WHITE : colour;
	}

	private static String styleName(MarkerStyle style)
	{
		if (style == MarkerStyle.REGION)
		{
			return "Region";
		}
		if (style == MarkerStyle.BORDER)
		{
			return "Border";
		}
		return "Tile";
	}

	private static JPanel body()
	{
		return new ListBody();
	}

	private static JScrollPane scroll(JPanel body)
	{
		JScrollPane scroll = new JScrollPane(body);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		scroll.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
		scroll.setBackground(ColorScheme.DARK_GRAY_COLOR);
		return scroll;
	}

	private static JComponent hint(String text)
	{
		return new HintText(text);
	}

	/**
	 * Wraps to the real row width. Preferred width stays 1 so the hint cannot
	 * widen the list, and the right inset keeps the last glyph inside the sidebar.
	 */
	private static final class HintText extends JComponent
	{
		private final String text;
		private int laidOutWidth = -1;

		private HintText(String text)
		{
			this.text = text == null ? "" : text;
			setFont(SMALL_FONT);
			setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			setOpaque(false);
			setAlignmentX(Component.LEFT_ALIGNMENT);
			setBorder(new EmptyBorder(8, 4, 8, 12));
		}

		@Override
		public Dimension getPreferredSize()
		{
			return new Dimension(1, heightFor(textWidth(availableWidth())));
		}

		@Override
		public Dimension getMinimumSize()
		{
			return new Dimension(1, getPreferredSize().height);
		}

		@Override
		public Dimension getMaximumSize()
		{
			return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
		}

		@Override
		public void setBounds(int x, int y, int width, int height)
		{
			super.setBounds(x, y, width, height);
			if (width <= 0 || width == laidOutWidth)
			{
				return;
			}
			int wanted = heightFor(textWidth(width));
			laidOutWidth = width;
			if (wanted != height)
			{
				revalidate();
			}
		}

		private int availableWidth()
		{
			if (getWidth() > 1)
			{
				return getWidth();
			}
			for (Container parent = getParent(); parent != null; parent = parent.getParent())
			{
				if (parent.getWidth() > 1)
				{
					Insets insets = parent.getInsets();
					return Math.max(1, parent.getWidth() - insets.left - insets.right);
				}
			}
			return PANEL_WIDTH;
		}

		private int textWidth(int componentWidth)
		{
			Insets insets = getInsets();
			return Math.max(1, componentWidth - insets.left - insets.right);
		}

		private int heightFor(int width)
		{
			Insets insets = getInsets();
			int lines = wrap(getFontMetrics(getFont()), width).size();
			return insets.top + insets.bottom + Math.max(1, lines) * getFontMetrics(getFont()).getHeight();
		}

		private List<String> wrap(FontMetrics metrics, int width)
		{
			List<String> lines = new ArrayList<>();
			if (text.isEmpty())
			{
				lines.add("");
				return lines;
			}
			String[] words = text.split(" ", -1);
			StringBuilder line = new StringBuilder();
			for (String word : words)
			{
				if (line.length() == 0)
				{
					line.append(word);
					continue;
				}
				String candidate = line + " " + word;
				if (metrics.stringWidth(candidate) <= width)
				{
					line.append(' ');
					line.append(word);
				}
				else
				{
					lines.add(line.toString());
					line.setLength(0);
					line.append(word);
				}
			}
			lines.add(line.toString());
			return lines;
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g2 = (Graphics2D) graphics.create();
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2.setFont(getFont());
			g2.setColor(getForeground());
			Insets insets = getInsets();
			FontMetrics metrics = g2.getFontMetrics();
			List<String> lines = wrap(metrics, textWidth(getWidth()));
			int y = insets.top + metrics.getAscent();
			for (String line : lines)
			{
				g2.drawString(line, insets.left, y);
				y += metrics.getHeight();
			}
			g2.dispose();
		}
	}

	/** Short empty-group line. A fixed HTML width makes the card wider than the panel and clips delete. */
	private static JLabel groupEmpty(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(SMALL_FONT);
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setBorder(new EmptyBorder(2, 4, 2, 4));
		stretch(label);
		return label;
	}

	private static void stretch(JComponent component)
	{
		component.setAlignmentX(Component.LEFT_ALIGNMENT);
		Dimension pref = component.getPreferredSize();
		component.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));
	}

	private static void paintPill(Graphics2D graphics, int width, int height, Color fill)
	{
		graphics.setColor(fill);
		graphics.fillRoundRect(0, 0, width, height, CORNER, CORNER);
	}

	private static final class RoundPanel extends JPanel
	{
		private Color outline;

		private RoundPanel(Color background)
		{
			setBackground(background);
			setOpaque(false);
		}

		private void setOutline(Color outline)
		{
			this.outline = outline;
		}

		@Override
		public Dimension getMaximumSize()
		{
			Dimension pref = getPreferredSize();
			return new Dimension(Integer.MAX_VALUE, pref.height);
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g2 = (Graphics2D) graphics.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(getBackground());
			g2.fillRoundRect(0, 0, getWidth(), getHeight(), CORNER, CORNER);
			if (outline != null)
			{
				g2.setColor(outline);
				g2.setStroke(new BasicStroke(2f));
				g2.drawRoundRect(1, 1, Math.max(0, getWidth() - 3), Math.max(0, getHeight() - 3), CORNER, CORNER);
			}
			g2.dispose();
			super.paintComponent(graphics);
		}
	}

	private enum TabGlyph
	{
		TILE,
		OBJECT,
		GROUP
	}

	private static final class TitleRow extends JPanel
	{
		private final JLabel iconLabel;
		private final JLabel title;

		private TitleRow(JLabel iconLabel, JLabel title)
		{
			this.iconLabel = iconLabel;
			this.title = title;
			setOpaque(false);
			setLayout(null);
			setAlignmentX(Component.CENTER_ALIGNMENT);
			if (iconLabel != null)
			{
				add(iconLabel);
			}
			add(title);
		}

		@Override
		public Dimension getPreferredSize()
		{
			return new Dimension(1, 22);
		}

		@Override
		public Dimension getMaximumSize()
		{
			return new Dimension(Integer.MAX_VALUE, 22);
		}

		@Override
		public void doLayout()
		{
			int width = getWidth();
			int height = getHeight();
			int leftLimit = 0;
			if (iconLabel != null)
			{
				Dimension iconSize = iconLabel.getPreferredSize();
				int iconY = Math.max(0, (height - iconSize.height) / 2);
				iconLabel.setBounds(0, iconY, iconSize.width, iconSize.height);
				leftLimit = iconSize.width + 4;
			}
			int rightLimit = width;
			Dimension titleSize = title.getPreferredSize();
			int titleX = (width - titleSize.width) / 2;
			if (titleX < leftLimit)
			{
				titleX = leftLimit;
			}
			if (titleX + titleSize.width > rightLimit)
			{
				titleX = Math.max(leftLimit, rightLimit - titleSize.width);
			}
			int titleY = Math.max(0, (height - titleSize.height) / 2);
			int titleWidth = Math.min(titleSize.width, Math.max(0, rightLimit - titleX));
			title.setBounds(titleX, titleY, titleWidth, titleSize.height);
		}
	}

	private static final class SplitTabs extends JPanel
	{
		private static final int HEIGHT = 28;

		private SplitTabs(JComponent left, JComponent rule, JComponent right)
		{
			setOpaque(false);
			setLayout(null);
			setAlignmentX(Component.CENTER_ALIGNMENT);
			add(left);
			add(rule);
			add(right);
		}

		@Override
		public Dimension getPreferredSize()
		{
			return new Dimension(1, HEIGHT);
		}

		@Override
		public Dimension getMaximumSize()
		{
			return new Dimension(Integer.MAX_VALUE, HEIGHT);
		}

		@Override
		public void doLayout()
		{
			int ruleWidth = 9;
			int height = getHeight();
			int each = Math.max(0, (getWidth() - ruleWidth) / 2);
			getComponent(0).setBounds(0, 0, each, height);
			getComponent(1).setBounds(each, 0, ruleWidth, height);
			getComponent(2).setBounds(each + ruleWidth, 0, Math.max(0, getWidth() - each - ruleWidth), height);
		}
	}

	private static final class TabSeparator extends JComponent
	{
		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g2 = (Graphics2D) graphics.create();
			g2.setColor(new Color(118, 122, 130));
			int x = getWidth() / 2;
			int top = 6;
			int bottom = Math.max(top, getHeight() - 6);
			g2.fillRect(x, top, 1, bottom - top);
			g2.dispose();
		}
	}

	private static final class TabGlyphs
	{
		private static ImageIcon icon(TabGlyph glyph, Color color, int size)
		{
			BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2 = image.createGraphics();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(color);
			if (glyph == TabGlyph.GROUP)
			{
				drawGroup(g2, size);
			}
			else if (glyph == TabGlyph.TILE)
			{
				drawTile(g2, size);
			}
			else
			{
				drawObject(g2, size);
			}
			g2.dispose();
			return new ImageIcon(image);
		}

		private static void drawGroup(Graphics2D g2, int size)
		{
			int bar = 2;
			int gap = 2;
			int total = bar * 3 + gap * 2;
			int y = Math.max(0, (size - total) / 2);
			int inset = 1;
			for (int i = 0; i < 3; i++)
			{
				g2.fillRoundRect(inset, y, size - inset * 2, bar, bar, bar);
				y += bar + gap;
			}
		}

		private static void drawTile(Graphics2D g2, int size)
		{
			// The 12px tab uses the same corner square as the large one. A long arm
			// and a round stroke close the gap and read as a solid square.
			boolean small = size <= 12;
			if (small)
			{
				g2.setStroke(new BasicStroke(1.15f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
			}
			else
			{
				g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			}
			int inset = 1;
			int last = size - 1 - inset;
			int span = Math.max(1, last - inset);
			int arm = small ? Math.max(2, span / 3) : Math.max(3, size / 3);
			g2.drawPolyline(new int[] {inset + arm, inset, inset}, new int[] {inset, inset, inset + arm}, 3);
			g2.drawPolyline(new int[] {last - arm, last, last}, new int[] {inset, inset, inset + arm}, 3);
			g2.drawPolyline(new int[] {inset, inset, inset + arm}, new int[] {last - arm, last, last}, 3);
			g2.drawPolyline(new int[] {last, last, last - arm}, new int[] {last - arm, last, last}, 3);
		}

		private static void drawObject(Graphics2D g2, int size)
		{
			Color ink = g2.getColor();
			int left = 1;
			int right = size - 2;
			int midX = size / 2;
			int top = 1;
			int ridge = Math.max(top + 2, size / 3);
			int midY = ridge + (ridge - top);
			int bottom = size - 2;
			int sideBottom = bottom - (midY - ridge);
			Polygon topFace = new Polygon(
				new int[] {midX, right, midX, left},
				new int[] {top, ridge, midY, ridge},
				4);
			Polygon leftFace = new Polygon(
				new int[] {left, midX, midX, left},
				new int[] {ridge, midY, bottom, sideBottom},
				4);
			Polygon rightFace = new Polygon(
				new int[] {midX, right, right, midX},
				new int[] {midY, ridge, sideBottom, bottom},
				4);
			g2.setColor(fade(ink, 145));
			g2.fillPolygon(leftFace);
			g2.setColor(fade(ink, 205));
			g2.fillPolygon(rightFace);
			g2.setColor(ink);
			g2.fillPolygon(topFace);
			g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g2.drawPolygon(topFace);
			g2.drawPolygon(leftFace);
			g2.drawPolygon(rightFace);
		}

		private static Color fade(Color color, int alpha)
		{
			return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
		}
	}

	private static final class TabButton extends JButton
	{
		private final boolean prominent;
		private boolean active;
		private TabGlyph glyph;

		private TabButton(String text, TabGlyph glyph, boolean prominent)
		{
			super(text);
			this.glyph = glyph;
			this.prominent = prominent;
			setFont(prominent ? UI_FONT : SMALL_FONT);
			setForeground(prominent ? new Color(232, 234, 238) : TAB_MUTED);
			setFocusPainted(false);
			setBorderPainted(false);
			setContentAreaFilled(false);
			setOpaque(false);
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setBorder(new EmptyBorder(prominent ? 8 : 4, prominent ? 6 : 3, prominent ? 8 : 4, prominent ? 6 : 3));
			setHorizontalAlignment(SwingConstants.CENTER);
			setHorizontalTextPosition(SwingConstants.RIGHT);
			setIconTextGap(prominent ? 6 : 4);
			refreshIcon();
		}

		private void setGlyph(TabGlyph glyph)
		{
			this.glyph = glyph;
			refreshIcon();
			repaint();
		}

		private void setActive(boolean active)
		{
			this.active = active;
			setForeground(active ? Color.WHITE : prominent ? new Color(232, 234, 238) : TAB_MUTED);
			refreshIcon();
			repaint();
		}

		private void refreshIcon()
		{
			setIcon(TabGlyphs.icon(glyph, getForeground(), prominent ? 14 : 12));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g2 = (Graphics2D) graphics.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Color fill = prominent
				? (active ? new Color(126, 132, 144) : new Color(74, 78, 86))
				: (active ? TAB_SELECTED : TAB_IDLE);
			if (!active && getModel().isRollover())
			{
				fill = fill.brighter();
			}
			g2.setColor(fill);
			g2.fillRoundRect(0, 0, getWidth(), getHeight(), CORNER, CORNER);
			if (prominent && active)
			{
				g2.setColor(new Color(232, 234, 238));
				int inset = 12;
				g2.fillRoundRect(inset, getHeight() - 4, Math.max(0, getWidth() - inset * 2), 2, 2, 2);
			}
			g2.dispose();
			super.paintComponent(graphics);
		}
	}

	private final class TogglePill extends JButton
	{
		private boolean on;

		private TogglePill(String text)
		{
			super(text);
			setFont(SMALL_FONT);
			setForeground(TAB_MUTED);
			setFocusPainted(false);
			setBorderPainted(false);
			setContentAreaFilled(false);
			setOpaque(false);
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setBorder(new EmptyBorder(5, 6, 5, 6));
		}

		private void setOn(boolean on)
		{
			this.on = on;
			refreshFace();
		}

		@Override
		public void setEnabled(boolean enabled)
		{
			super.setEnabled(enabled);
			refreshFace();
		}

		private void refreshFace()
		{
			if (!isEnabled())
			{
				setForeground(new Color(120, 120, 124));
				setCursor(Cursor.getDefaultCursor());
			}
			else
			{
				setForeground(on ? BUTTON_TEXT : TAB_MUTED);
				setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			}
			repaint();
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g2 = (Graphics2D) graphics.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Color fill = !isEnabled() ? CARD_DIM : on ? CARD_SELECTED : TAB_IDLE;
			paintPill(g2, getWidth(), getHeight(), fill);
			g2.dispose();
			super.paintComponent(graphics);
		}
	}

	private static final class PillButton extends JButton
	{
		private PillButton(String text)
		{
			this(text, false);
		}

		private PillButton(String text, boolean compact)
		{
			super(text);
			setFont(compact ? SMALL_FONT : UI_FONT);
			setForeground(BUTTON_TEXT);
			setFocusPainted(false);
			setBorderPainted(false);
			setContentAreaFilled(false);
			setOpaque(false);
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setBorder(new EmptyBorder(compact ? 4 : 5, compact ? 4 : 12, compact ? 4 : 5, compact ? 4 : 12));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g2 = (Graphics2D) graphics.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Color fill = BUTTON_FILL;
			if (getModel().isPressed())
			{
				fill = fill.darker();
			}
			else if (getModel().isRollover())
			{
				fill = fill.brighter();
			}
			paintPill(g2, getWidth(), getHeight(), fill);
			g2.setColor(new Color(255, 255, 255, 36));
			g2.drawRoundRect(0, 0, Math.max(0, getWidth() - 1), Math.max(0, getHeight() - 1), CORNER, CORNER);
			g2.dispose();
			super.paintComponent(graphics);
		}
	}

	/**
	 * Centres pills and wraps them to the row width. Preferred width stays 1.
	 * Reporting the ancestor width here makes the scroll view grow by the row insets
	 * on every rebuild, which walks the pills to the right and clips expanded groups.
	 */
	private static final class CenteredFlow extends JPanel
	{
		private int laidOutWidth = -1;

		private CenteredFlow()
		{
			super(new FlowLayout(FlowLayout.CENTER, 4, 2));
			setOpaque(false);
			setAlignmentX(Component.CENTER_ALIGNMENT);
		}

		@Override
		public Dimension getPreferredSize()
		{
			return new Dimension(1, wrappedSize(availableWidth()).height);
		}

		@Override
		public Dimension getMinimumSize()
		{
			return new Dimension(1, getPreferredSize().height);
		}

		@Override
		public Dimension getMaximumSize()
		{
			return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
		}

		@Override
		public void setBounds(int x, int y, int width, int height)
		{
			super.setBounds(x, y, width, height);
			if (width <= 0 || width == laidOutWidth)
			{
				return;
			}
			int wanted = wrappedSize(width).height;
			laidOutWidth = width;
			if (wanted != height)
			{
				revalidate();
			}
		}

		private int availableWidth()
		{
			if (getWidth() > 0)
			{
				return getWidth();
			}
			for (Container parent = getParent(); parent != null; parent = parent.getParent())
			{
				if (parent.getWidth() > 0)
				{
					Insets insets = parent.getInsets();
					return Math.max(1, parent.getWidth() - insets.left - insets.right);
				}
			}
			return 220;
		}

		private Dimension wrappedSize(int width)
		{
			int hgap = 4;
			int vgap = 2;
			int x = 0;
			int rowHeight = 0;
			int height = vgap;
			boolean any = false;
			for (Component child : getComponents())
			{
				if (!child.isVisible())
				{
					continue;
				}
				Dimension size = child.getPreferredSize();
				if (any && x > 0 && x + size.width > width)
				{
					height += rowHeight + vgap;
					x = 0;
					rowHeight = 0;
				}
				if (x > 0)
				{
					x += hgap;
				}
				x += size.width;
				rowHeight = Math.max(rowHeight, size.height);
				any = true;
			}
			if (any)
			{
				height += rowHeight + vgap;
			}
			else
			{
				height = 0;
			}
			return new Dimension(Math.max(1, width), Math.max(1, height));
		}
	}

	/**
	 * Locks each list to the sidebar. Without this, a wide preferred size becomes the
	 * scroll view and the next rebuild measures that wider view and grows again.
	 */
	private static final class ListBody extends JPanel implements Scrollable
	{
		private ListBody()
		{
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			setBackground(ColorScheme.DARK_GRAY_COLOR);
			setBorder(new EmptyBorder(0, 8, 12, 8));
		}

		@Override
		public Dimension getPreferredSize()
		{
			Dimension pref = super.getPreferredSize();
			return new Dimension(1, pref.height);
		}

		@Override
		public Dimension getMinimumSize()
		{
			Dimension min = super.getMinimumSize();
			return new Dimension(1, min.height);
		}

		@Override
		public Dimension getPreferredScrollableViewportSize()
		{
			Dimension pref = super.getPreferredSize();
			return new Dimension(PANEL_WIDTH, pref.height);
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			if (orientation == SwingConstants.VERTICAL)
			{
				return Math.max(16, visibleRect.height - 16);
			}
			return Math.max(16, visibleRect.width - 16);
		}

		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight()
		{
			return false;
		}
	}

	private static final class EyeButton extends JButton
	{
		private EyeButton(boolean shown)
		{
			prepareIcon(this);
			setIcon(shown ? VISIBLE_ICON : INVISIBLE_ICON);
			setRolloverIcon(shown ? VISIBLE_HOVER_ICON : INVISIBLE_HOVER_ICON);
			setRolloverEnabled(true);
			setToolTipText(shown ? "Hide group" : "Show group");
		}
	}

	private static final class ChevronButton extends JButton
	{
		private final boolean collapsed;

		private ChevronButton(boolean collapsed)
		{
			this(collapsed, 22);
		}

		private ChevronButton(boolean collapsed, int size)
		{
			this.collapsed = collapsed;
			Dimension box = new Dimension(size, size);
			setPreferredSize(box);
			setMinimumSize(box);
			setMaximumSize(box);
			setFocusPainted(false);
			setBorderPainted(false);
			setContentAreaFilled(false);
			setOpaque(false);
			setBorder(new EmptyBorder(0, 0, 0, 0));
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setToolTipText(collapsed ? "Expand" : "Collapse");
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g2 = (Graphics2D) graphics.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(ColorScheme.LIGHT_GRAY_COLOR);
			int cx = getWidth() / 2;
			int cy = getHeight() / 2;
			if (collapsed)
			{
				g2.fillPolygon(new int[]{cx - 3, cx + 4, cx - 3}, new int[]{cy - 5, cy, cy + 5}, 3);
			}
			else
			{
				g2.fillPolygon(new int[]{cx - 5, cx + 5, cx}, new int[]{cy - 3, cy - 3, cy + 4}, 3);
			}
			g2.dispose();
		}
	}

	private static final class CollapseAllButton extends JButton
	{
		private CollapseAllButton()
		{
			Dimension box = new Dimension(16, 16);
			setPreferredSize(box);
			setMinimumSize(box);
			setMaximumSize(box);
			setFocusPainted(false);
			setBorderPainted(false);
			setContentAreaFilled(false);
			setOpaque(false);
			setBorder(new EmptyBorder(0, 0, 0, 0));
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setToolTipText("Collapse all");
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g2 = (Graphics2D) graphics.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(getModel().isRollover() ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR);
			int cx = getWidth() / 2;
			int top = Math.max(2, getHeight() / 2 - 5);
			g2.fillPolygon(new int[]{cx - 4, cx + 4, cx}, new int[]{top + 4, top + 4, top}, 3);
			g2.fillPolygon(new int[]{cx - 4, cx + 4, cx}, new int[]{top + 9, top + 9, top + 5}, 3);
			g2.dispose();
		}
	}

	private static void prepareIcon(JButton button)
	{
		Dimension box = new Dimension(22, 22);
		button.setPreferredSize(box);
		button.setMinimumSize(box);
		button.setMaximumSize(box);
		button.setFocusPainted(false);
		button.setBorderPainted(false);
		button.setContentAreaFilled(false);
		button.setOpaque(false);
		button.setBorder(new EmptyBorder(0, 0, 0, 0));
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	/** Keeps each group icon in the same 22px cell. */
	private static JComponent fixedSlot(JComponent child)
	{
		JPanel slot = new JPanel(new GridBagLayout());
		slot.setOpaque(false);
		Dimension box = new Dimension(22, 22);
		slot.setPreferredSize(box);
		slot.setMinimumSize(box);
		slot.setMaximumSize(box);
		slot.add(child);
		return slot;
	}

	private static int[] markSquare(JButton button)
	{
		int side = 11;
		int left = Math.max(0, (button.getWidth() - side) / 2);
		int top = Math.max(0, (button.getHeight() - side) / 2);
		return new int[]{left, top, left + side - 1, top + side - 1};
	}

	private static void paintMarkBackdrop(Graphics2D g2, JButton button, boolean marked)
	{
		if (!marked)
		{
			return;
		}
		g2.setColor(new Color(255, 255, 255, 36));
		g2.fillRoundRect(1, 1, Math.max(0, button.getWidth() - 2), Math.max(0, button.getHeight() - 2), 4, 4);
	}

	private static Color markColour(JButton button, boolean marked)
	{
		if (marked || button.getModel().isRollover())
		{
			return Color.WHITE;
		}
		return ColorScheme.LIGHT_GRAY_COLOR;
	}

	private static final class GroupColourSquare extends JPanel
	{
		private Color paint = Color.WHITE;
		private boolean own;
		private Runnable clear = () -> { };
		private Runnable choose = () -> { };

		private GroupColourSquare()
		{
			setOpaque(false);
			setPreferredSize(new Dimension(14, 22));
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			addMouseListener(new MouseAdapter()
			{
				@Override
				public void mousePressed(MouseEvent event)
				{
					if (SwingUtilities.isRightMouseButton(event))
					{
						clear.run();
					}
					else if (SwingUtilities.isLeftMouseButton(event))
					{
						choose.run();
					}
				}
			});
		}

		private void show(Color colour, boolean own, String tip)
		{
			paint = colour == null ? Color.WHITE : colour;
			this.own = own;
			setToolTipText(tip);
			repaint();
		}

		private void onPress(Runnable clear, Runnable choose)
		{
			this.clear = clear == null ? () -> { } : clear;
			this.choose = choose == null ? () -> { } : choose;
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			super.paintComponent(graphics);
			Graphics2D g2 = (Graphics2D) graphics.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int size = 11;
			int x = Math.max(0, (getWidth() - size) / 2);
			int y = Math.max(0, (getHeight() - size) / 2);
			g2.setColor(paint);
			g2.fillRoundRect(x, y, size, size, 3, 3);
			g2.setColor(own ? Color.WHITE : new Color(255, 255, 255, 70));
			g2.drawRoundRect(x, y, size, size, 3, 3);
			g2.dispose();
		}
	}

	private static final class BrushButton extends JButton
	{
		private final boolean marked;

		private BrushButton(boolean marked)
		{
			this.marked = marked;
			prepareIcon(this);
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g2 = (Graphics2D) graphics.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			paintMarkBackdrop(g2, this, marked);
			g2.setColor(markColour(this, marked));
			int[] square = markSquare(this);
			int left = square[0];
			int top = square[1];
			int right = square[2];
			int bottom = square[3];
			g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g2.drawOval(left + 5, top, 5, 5);
			g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
			g2.drawPolyline(new int[]{left + 6, left + 3, left + 1}, new int[]{top + 5, top + 8, bottom}, 3);
			g2.drawPolyline(new int[]{right - 1, left + 6, left + 1}, new int[]{top + 5, top + 8, bottom}, 3);
			g2.dispose();
		}
	}

	private static final class CopyButton extends JButton
	{
		private CopyButton()
		{
			prepareIcon(this);
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g2 = (Graphics2D) graphics.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g2.setColor(markColour(this, false));
			g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g2.drawRoundRect(8, 4, 8, 9, 2, 2);
			g2.setColor(new Color(32, 34, 38));
			g2.fillRoundRect(5, 8, 8, 9, 2, 2);
			g2.setColor(markColour(this, false));
			g2.drawRoundRect(5, 8, 8, 9, 2, 2);
			g2.dispose();
		}
	}

	private static final class CornerMarkButton extends JButton
	{
		private final boolean marked;

		private CornerMarkButton(boolean marked)
		{
			this.marked = marked;
			prepareIcon(this);
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g2 = (Graphics2D) graphics.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			paintMarkBackdrop(g2, this, marked);
			g2.setColor(markColour(this, marked));
			g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			int[] square = markSquare(this);
			int left = square[0];
			int top = square[1];
			int right = square[2];
			int bottom = square[3];
			int arm = 3;
			g2.drawLine(left, top, left + arm, top);
			g2.drawLine(left, top, left, top + arm);
			g2.drawLine(right, top, right - arm, top);
			g2.drawLine(right, top, right, top + arm);
			g2.drawLine(left, bottom, left + arm, bottom);
			g2.drawLine(left, bottom, left, bottom - arm);
			g2.drawLine(right, bottom, right - arm, bottom);
			g2.drawLine(right, bottom, right, bottom - arm);
			g2.dispose();
		}
	}

	private static final class MergeMarkButton extends JButton
	{
		private final boolean marked;

		private MergeMarkButton(boolean marked)
		{
			this.marked = marked;
			prepareIcon(this);
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g2 = (Graphics2D) graphics.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			paintMarkBackdrop(g2, this, marked);
			g2.setColor(markColour(this, marked));
			g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			int[] square = markSquare(this);
			int left = square[0];
			int top = square[1];
			int right = square[2];
			int bottom = square[3];
			int mid = (left + right) / 2;
			int tick = 3;
			g2.drawLine(left, top, right, top);
			g2.drawLine(left, bottom, right, bottom);
			g2.drawLine(left, top, left, bottom);
			g2.drawLine(right, top, right, bottom);
			g2.drawLine(mid, top, mid, top + tick);
			g2.drawLine(mid, bottom - tick, mid, bottom);
			g2.dispose();
		}
	}

	private static final class TrashButton extends JButton
	{
		private TrashButton()
		{
			prepareIcon(this);
			setToolTipText("Delete group");
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g2 = (Graphics2D) graphics.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Color colour = getModel().isRollover() ? new Color(230, 90, 90) : ColorScheme.LIGHT_GRAY_COLOR;
			g2.setColor(colour);
			int x = 6;
			int y = 7;
			g2.drawRect(x, y, 10, 10);
			g2.drawLine(x - 1, y, x + 11, y);
			g2.drawLine(x + 3, y - 2, x + 7, y - 2);
			g2.drawLine(x + 3, y + 2, x + 3, y + 8);
			g2.drawLine(x + 7, y + 2, x + 7, y + 8);
			g2.dispose();
		}
	}

	private static final class CrossButton extends JButton
	{
		private CrossButton()
		{
			setPreferredSize(new Dimension(18, 18));
			setFocusPainted(false);
			setBorderPainted(false);
			setContentAreaFilled(false);
			setOpaque(false);
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setToolTipText("Remove");
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g2 = (Graphics2D) graphics.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(getModel().isRollover() ? new Color(230, 90, 90) : TAB_MUTED);
			int cx = getWidth() / 2;
			int cy = getHeight() / 2;
			g2.drawLine(cx - 4, cy - 4, cx + 4, cy + 4);
			g2.drawLine(cx + 4, cy - 4, cx - 4, cy + 4);
			g2.dispose();
		}
	}
}
