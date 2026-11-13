/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay;

public enum ObjectTextPosition
{
	MIDDLE("Middle"),
	TOP("Top"),
	BOTTOM("Bottom"),
	ABOVE("Above"),
	BELOW("Below");

	private final String label;

	ObjectTextPosition(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
