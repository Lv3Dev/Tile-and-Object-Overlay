/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Text copied from a group and pasted into Import on the matching groups tab.
 */
public final class GroupShare
{
	static final String TYPE = "tile-and-object-overlay-group";
	static final int VERSION = 1;

	private static final Gson GSON = new Gson();

	private String type;
	private int version;
	private GroupKind kind;
	private String name;
	private Integer colour;
	private Integer fillColour;
	private List<TileMarker> tiles;
	private List<ObjectMarker> objects;

	private GroupShare()
	{
	}

	public static boolean looksLike(String text)
	{
		return text != null && text.contains(TYPE);
	}

	public static String encode(MarkerGroup group, List<TileMarker> tiles, List<ObjectMarker> objects)
	{
		GroupShare share = new GroupShare();
		share.type = TYPE;
		share.version = VERSION;
		share.kind = group.getKind() == null ? GroupKind.TILE : group.getKind();
		share.name = group.getName();
		if (share.kind == GroupKind.TILE)
		{
			share.colour = group.getColour();
			share.fillColour = group.getFillColour();
			share.tiles = tiles == null ? new ArrayList<>() : tiles;
			share.objects = new ArrayList<>();
		}
		else
		{
			share.tiles = new ArrayList<>();
			share.objects = objects == null ? new ArrayList<>() : objects;
		}
		return GSON.toJson(share);
	}

	public static Decoded decode(String text)
	{
		if (text == null)
		{
			return Decoded.invalid();
		}
		String trimmed = text.trim();
		int start = trimmed.indexOf('{');
		int end = trimmed.lastIndexOf('}');
		if (start < 0 || end <= start)
		{
			return Decoded.invalid();
		}
		try
		{
			GroupShare share = GSON.fromJson(trimmed.substring(start, end + 1), GroupShare.class);
			if (share == null || !TYPE.equals(share.type) || share.kind == null)
			{
				return Decoded.invalid();
			}
			if (share.version > VERSION)
			{
				return Decoded.newer();
			}
			return Decoded.ok(share);
		}
		catch (JsonSyntaxException ex)
		{
			return Decoded.invalid();
		}
	}

	GroupKind getKind()
	{
		return kind;
	}

	String getName()
	{
		return name;
	}

	Integer getColour()
	{
		return colour;
	}

	Integer getFillColour()
	{
		return fillColour;
	}

	List<TileMarker> getTiles()
	{
		return tiles == null ? new ArrayList<>() : tiles;
	}

	List<ObjectMarker> getObjects()
	{
		return objects == null ? new ArrayList<>() : objects;
	}

	public static final class Decoded
	{
		private final GroupShare share;
		private final boolean newer;

		private Decoded(GroupShare share, boolean newer)
		{
			this.share = share;
			this.newer = newer;
		}

		static Decoded ok(GroupShare share)
		{
			return new Decoded(share, false);
		}

		static Decoded newer()
		{
			return new Decoded(null, true);
		}

		static Decoded invalid()
		{
			return new Decoded(null, false);
		}

		GroupShare getShare()
		{
			return share;
		}

		boolean isNewer()
		{
			return newer;
		}
	}

	public static final class Result
	{
		private final boolean changed;
		private final String message;

		private Result(boolean changed, String message)
		{
			this.changed = changed;
			this.message = message;
		}

		public static Result message(String message)
		{
			return new Result(false, message);
		}

		public static Result imported(String message)
		{
			return new Result(true, message);
		}

		public boolean isChanged()
		{
			return changed;
		}

		public String getMessage()
		{
			return message;
		}
	}

	public static final class Bundle
	{
		private final String text;
		private final String notice;

		Bundle(String text, String notice)
		{
			this.text = text;
			this.notice = notice;
		}

		public String getText()
		{
			return text;
		}

		public String getNotice()
		{
			return notice;
		}
	}
}
