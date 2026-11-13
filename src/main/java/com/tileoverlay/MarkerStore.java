/*
 * Copyright (c) 2026 tileoverlay
 * BSD-2-Clause. See LICENSE.
 */
package com.tileoverlay;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.Filepath;

@Slf4j
@Singleton
public class MarkerStore
{
	static final String FILE_NAME = "tile-and-object-overlay-markers.json";
	private static final String BACKUP_NAME = "tile-and-object-overlay-markers.bak";

	private final Gson gson;

	@Inject
	public MarkerStore(Gson gson)
	{
		this.gson = gson.newBuilder().setPrettyPrinting().create();
	}

	public MarkerFile read(Filepath file) throws IOException
	{
		if (file == null || !file.exists())
		{
			return MarkerFile.empty();
		}
		try (BufferedReader reader = file.openBufferedReader())
		{
			MarkerFile fileData = gson.fromJson(reader, MarkerFile.class);
			if (fileData == null)
			{
				return MarkerFile.empty();
			}
			if (fileData.getGroups() == null)
			{
				fileData.setGroups(new java.util.ArrayList<>());
			}
			if (fileData.getMarkers() == null)
			{
				fileData.setMarkers(new java.util.ArrayList<>());
			}
			if (fileData.getObjects() == null)
			{
				fileData.setObjects(new java.util.ArrayList<>());
			}
			return fileData;
		}
		catch (JsonSyntaxException ex)
		{
			log.error("Marker file is not valid JSON. The file was left in place and a backup was written.", ex);
			backup(file);
			return null;
		}
	}

	public void write(Filepath file, MarkerFile data) throws IOException
	{
		Filepath directory = file.getParent();
		directory.createDirectories();
		Filepath temp = directory.createTempFile("markers-", ".tmp");
		temp.write(gson.toJson(data));
		try
		{
			temp.moveTo(file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		}
		catch (AtomicMoveNotSupportedException ex)
		{
			temp.moveTo(file, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private void backup(Filepath file)
	{
		try
		{
			Filepath backup = file.getParent().joinSegment(BACKUP_NAME);
			file.copyTo(backup, StandardCopyOption.REPLACE_EXISTING);
		}
		catch (IOException ex)
		{
			log.error("Could not back up the marker file", ex);
		}
	}
}
