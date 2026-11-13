/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MarkerGroup
{
	private String id;
	private String name;
	private boolean enabled;
	private boolean collapsed;
	private GroupKind kind;
	/**
	 * New marks of this group's kind are added here. Tiles and objects each
	 * keep their own active group.
	 */
	private boolean active;

	public MarkerGroup(String id, String name, boolean enabled, boolean collapsed)
	{
		this(id, name, enabled, collapsed, GroupKind.TILE);
	}

	public MarkerGroup(String id, String name, boolean enabled, boolean collapsed, GroupKind kind)
	{
		this.id = id;
		this.name = name;
		this.enabled = enabled;
		this.collapsed = collapsed;
		this.kind = kind;
	}

	public MarkerGroup copy()
	{
		MarkerGroup copy = new MarkerGroup(id, name, enabled, collapsed, kind);
		copy.setActive(active);
		return copy;
	}
}
