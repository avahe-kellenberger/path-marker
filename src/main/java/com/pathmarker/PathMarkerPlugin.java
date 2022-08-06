package com.pathmarker;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.TileObject;
import net.runelite.api.Varbits;
import net.runelite.api.WallObject;
import net.runelite.api.World;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import org.apache.commons.lang3.tuple.Pair;

import javax.inject.Inject;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@PluginDescriptor(
	name = "Path Marker"
)
public class PathMarkerPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private PathMarkerConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private PathMarkerOverlay overlay;

	public Pathfinder pathfinder;

	private Tile lastSelectedSceneTile;

	@Setter
	private boolean ctrlHeld;

	private WorldPoint lastTickWorldLocation;

	private MenuEntry lastSelectedMenuEntry;

	@Getter
	private List<WorldPoint> activeCheckpointWPs;

	private Tile oldSelectedSceneTile;

	private boolean isRunning;

	private boolean activePathFound;

	private boolean hoverPathFound;

	private boolean activePathStartedLastTick;

	private boolean activePathMismatchLastTick;

	private boolean calcTilePathOnNextClientTick;

	@Getter
	private boolean pathActive;

	@Getter
	private List<WorldPoint> hoverPathTiles;

	@Getter
	private List<WorldPoint> hoverMiddlePathTiles;

	@Getter
	private List<WorldPoint> activePathTiles;

	@Getter
	private List<WorldPoint> activeMiddlePathTiles;

	private List<WorldPoint> hoverCheckpointWPs;

	private List<WorldArea> npcBlockWAs;

	private MenuEntry[] oldMenuEntries;

	@Inject
	private KeyManager keyManager;

	@Inject
	private CtrlListener ctrlListener;

	private static Map<Integer, Integer> objectBlocking;

	private static Map<Integer, Integer> npcBlocking;

	/*private final int[][] directions = new int[128][128];

	private final int[][] distances = new int[128][128];

	private final int[] bufferX = new int[4096];

	private final int[] bufferY = new int[4096];*/

	static class PathDestination
	{
		private WorldPoint worldPoint;
		private int sizeX;
		private int sizeY;
		private int objConfig;
		private int objID;
		private Actor actor;

		public PathDestination(WorldPoint worldPoint, int sizeX, int sizeY, int objConfig, int objID)
		{
			this.worldPoint = worldPoint;
			this.sizeX = sizeX;
			this.sizeY = sizeY;
			this.objConfig = objConfig;
			this.objID = objID;
			this.actor = null;
		}
		public PathDestination(WorldPoint worldPoint, int sizeX, int sizeY, int objConfig, int objID, Actor actor)
		{
			this.worldPoint = worldPoint;
			this.sizeX = sizeX;
			this.sizeY = sizeY;
			this.objConfig = objConfig;
			this.objID = objID;
			this.actor = actor;
		}
	}

	private PathDestination activePathDestination;

	@Override
	protected void startUp() throws Exception
	{
		hoverPathTiles = new ArrayList<>();
		hoverMiddlePathTiles = new ArrayList<>();
		hoverCheckpointWPs = new ArrayList<>();
		activePathTiles = new ArrayList<>();
		activeMiddlePathTiles = new ArrayList<>();
		activeCheckpointWPs = new ArrayList<>();
		npcBlockWAs = new ArrayList<>();
		ctrlHeld = false;
		pathActive = false;
		activePathStartedLastTick = false;
		activePathMismatchLastTick = false;
		isRunning = willRunOnClick();
		objectBlocking = readFile("loc_blocking.txt");
		npcBlocking = readFile("npc_blocking.txt");
		overlayManager.add(overlay);
		keyManager.registerKeyListener(ctrlListener);
		pathfinder = new Pathfinder(client, config, this);
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
		keyManager.registerKeyListener(ctrlListener);
	}

	@Provides
	PathMarkerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PathMarkerConfig.class);
	}

	/*private Pair<List<WorldPoint>, Boolean> pathTo(int approxDestinationX, int approxDestinationY, int sizeX, int sizeY, int objConfig, int objID)
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}
		int z = client.getPlane();

		CollisionData[] collisionData = client.getCollisionMaps();
		if (collisionData == null)
		{
			return null;
		}

		// Initialise directions and distances
		for (int i = 0; i < 128; ++i)
		{
			for (int j = 0; j < 128; ++j)
			{
				directions[i][j] = 0;
				distances[i][j] = Integer.MAX_VALUE;
			}
		}
		LocalPoint playerTrueTileLocalPoint = LocalPoint.fromWorld(client, player.getWorldLocation());
		if (playerTrueTileLocalPoint == null)
		{
			return null;
		}
		int middleX = playerTrueTileLocalPoint.getSceneX();
		int middleY = playerTrueTileLocalPoint.getSceneY();
		int currentX = middleX;
		int currentY = middleY;
		int offsetX = 64;
		int offsetY = 64;
		// Initialise directions and dist
		directions[offsetX][offsetY] = 99;
		distances[offsetX][offsetY] = 0;
		int index1 = 0;
		bufferX[0] = currentX;
		int index2 = 1;
		bufferY[0] = currentY;
		int[][] collisionDataFlags = collisionData[z].getFlags();

		boolean isReachable = false;

		while (index1 != index2)
		{
			currentX = bufferX[index1];
			currentY = bufferY[index1];
			index1 = index1 + 1 & 4095;
			// currentX is for the local coordinate while currentMapX is for the index in the directions and distances arrays
			int currentMapX = currentX - middleX + offsetX;
			int currentMapY = currentY - middleY + offsetY;

			if (hasArrived(currentX, currentY, approxDestinationX, approxDestinationY, sizeX, sizeY, objConfig, objID, collisionDataFlags))
			{
				isReachable = true;
				break;
			}

			int currentDistance = distances[currentMapX][currentMapY] + 1;
			if (currentMapX > 0 && directions[currentMapX - 1][currentMapY] == 0 && (collisionDataFlags[currentX - 1][currentY] & 19136776) == 0)
			{
				// Able to move 1 tile west
				bufferX[index2] = currentX - 1;
				bufferY[index2] = currentY;
				index2 = index2 + 1 & 4095;
				directions[currentMapX - 1][currentMapY] = 2;
				distances[currentMapX - 1][currentMapY] = currentDistance;
			}

			if (currentMapX < 127 && directions[currentMapX + 1][currentMapY] == 0 && (collisionDataFlags[currentX + 1][currentY] & 19136896) == 0)
			{
				// Able to move 1 tile east
				bufferX[index2] = currentX + 1;
				bufferY[index2] = currentY;
				index2 = index2 + 1 & 4095;
				directions[currentMapX + 1][currentMapY] = 8;
				distances[currentMapX + 1][currentMapY] = currentDistance;
			}

			if (currentMapY > 0 && directions[currentMapX][currentMapY - 1] == 0 && (collisionDataFlags[currentX][currentY - 1] & 19136770) == 0)
			{
				// Able to move 1 tile south
				bufferX[index2] = currentX;
				bufferY[index2] = currentY - 1;
				index2 = index2 + 1 & 4095;
				directions[currentMapX][currentMapY - 1] = 1;
				distances[currentMapX][currentMapY - 1] = currentDistance;
			}

			if (currentMapY < 127 && directions[currentMapX][currentMapY + 1] == 0 && (collisionDataFlags[currentX][currentY + 1] & 19136800) == 0)
			{
				// Able to move 1 tile north
				bufferX[index2] = currentX;
				bufferY[index2] = currentY + 1;
				index2 = index2 + 1 & 4095;
				directions[currentMapX][currentMapY + 1] = 4;
				distances[currentMapX][currentMapY + 1] = currentDistance;
			}

			if (currentMapX > 0 && currentMapY > 0 && directions[currentMapX - 1][currentMapY - 1] == 0 && (collisionDataFlags[currentX - 1][currentY - 1] & 19136782) == 0 && (collisionDataFlags[currentX - 1][currentY] & 19136776) == 0 && (collisionDataFlags[currentX][currentY - 1] & 19136770) == 0)
			{
				// Able to move 1 tile south-west
				bufferX[index2] = currentX - 1;
				bufferY[index2] = currentY - 1;
				index2 = index2 + 1 & 4095;
				directions[currentMapX - 1][currentMapY - 1] = 3;
				distances[currentMapX - 1][currentMapY - 1] = currentDistance;
			}

			if (currentMapX < 127 && currentMapY > 0 && directions[currentMapX + 1][currentMapY - 1] == 0 && (collisionDataFlags[currentX + 1][currentY - 1] & 19136899) == 0 && (collisionDataFlags[currentX + 1][currentY] & 19136896) == 0 && (collisionDataFlags[currentX][currentY - 1] & 19136770) == 0)
			{
				// Able to move 1 tile south-east
				bufferX[index2] = currentX + 1;
				bufferY[index2] = currentY - 1;
				index2 = index2 + 1 & 4095;
				directions[currentMapX + 1][currentMapY - 1] = 9;
				distances[currentMapX + 1][currentMapY - 1] = currentDistance;
			}

			if (currentMapX > 0 && currentMapY < 127 && directions[currentMapX - 1][currentMapY + 1] == 0 && (collisionDataFlags[currentX - 1][currentY + 1] & 19136824) == 0 && (collisionDataFlags[currentX - 1][currentY] & 19136776) == 0 && (collisionDataFlags[currentX][currentY + 1] & 19136800) == 0)
			{
				// Able to move 1 tile north-west
				bufferX[index2] = currentX - 1;
				bufferY[index2] = currentY + 1;
				index2 = index2 + 1 & 4095;
				directions[currentMapX - 1][currentMapY + 1] = 6;
				distances[currentMapX - 1][currentMapY + 1] = currentDistance;
			}

			if (currentMapX < 127 && currentMapY < 127 && directions[currentMapX + 1][currentMapY + 1] == 0 && (collisionDataFlags[currentX + 1][currentY + 1] & 19136992) == 0 && (collisionDataFlags[currentX + 1][currentY] & 19136896) == 0 && (collisionDataFlags[currentX][currentY + 1] & 19136800) == 0)
			{
				// Able to move 1 tile north-east
				bufferX[index2] = currentX + 1;
				bufferY[index2] = currentY + 1;
				index2 = index2 + 1 & 4095;
				directions[currentMapX + 1][currentMapY + 1] = 12;
				distances[currentMapX + 1][currentMapY + 1] = currentDistance;
			}
		}
		if (!isReachable)
		{
			// Try find a different reachable tile in the 21x21 area around the target tile, as close as possible to the target tile
			int upperboundDistance = Integer.MAX_VALUE;
			int pathLength = Integer.MAX_VALUE;
			int checkRange = 10;
			for (int i = approxDestinationX - checkRange; i <= checkRange + approxDestinationX; ++i)
			{
				for (int j = approxDestinationY - checkRange; j <= checkRange + approxDestinationY; ++j)
				{
					int currentMapX = i - middleX + offsetX;
					int currentMapY = j - middleY + offsetY;
					if (currentMapX >= 0 && currentMapY >= 0 && currentMapX < 128 && currentMapY < 128 && distances[currentMapX][currentMapY] < 100)
					{
						int deltaX = 0;
						if (i < approxDestinationX)
						{
							deltaX = approxDestinationX - i;
						}
						else if (i > approxDestinationX + sizeX - 1)
						{
							deltaX = i - (approxDestinationX + sizeX - 1);
						}

						int deltaY = 0;
						if (j < approxDestinationY)
						{
							deltaY = approxDestinationY - j;
						}
						else if (j > approxDestinationY + sizeY - 1)
						{
							deltaY = j - (approxDestinationY + sizeY - 1);
						}

						int distanceSquared = deltaX * deltaX + deltaY * deltaY;
						if (distanceSquared < upperboundDistance || distanceSquared == upperboundDistance && distances[currentMapX][currentMapY] < pathLength)
						{
							upperboundDistance = distanceSquared;
							pathLength = distances[currentMapX][currentMapY];
							currentX = i;
							currentY = j;
						}
					}
				}
			}
			if (upperboundDistance == Integer.MAX_VALUE)
			{
				// No path found
				List<WorldPoint> checkpointWPs = new ArrayList<>();
				checkpointWPs.add(activeCheckpointWPs.get(0));
				return Pair.of(checkpointWPs,false);
			}
		}

		// Getting path from directions and distances
		bufferX[0] = currentX;
		bufferY[0] = currentY;
		int index = 1;
		int directionNew;
		int directionOld;
		for (directionNew = directionOld = directions[currentX - middleX + offsetX][currentY - middleY + offsetY]; middleX != currentX || middleY != currentY; directionNew = directions[currentX - middleX + offsetX][currentY - middleY + offsetY])
		{
			if (directionNew != directionOld)
			{
				// "Corner" of the path --> new checkpoint tile
				directionOld = directionNew;
				bufferX[index] = currentX;
				bufferY[index++] = currentY;
			}

			if ((directionNew & 2) != 0)
			{
				++currentX;
			}
			else if ((directionNew & 8) != 0)
			{
				--currentX;
			}

			if ((directionNew & 1) != 0)
			{
				++currentY;
			}
			else if ((directionNew & 4) != 0)
			{
				--currentY;
			}
		}

		int checkpointTileNumber = 1;
		Tile[][][] tiles = client.getScene().getTiles();
		List<WorldPoint> checkpointWPs = new ArrayList<>();
		while (index-- > 0)
		{
			checkpointWPs.add(tiles[z][bufferX[index]][bufferY[index]].getWorldLocation());
			if (checkpointTileNumber == 25)
			{
				// Pathfinding only supports up to the 25 first checkpoint tiles
				break;
			}
			checkpointTileNumber++;
		}
		if (checkpointWPs.size() == 0)
		{
			checkpointWPs.add(player.getWorldLocation());
			return Pair.of(checkpointWPs,true);
		}
		return Pair.of(checkpointWPs,true);
	}

	private Pair<List<WorldPoint>, Boolean> pathTo(Tile other)
	{
		return pathTo(other.getSceneLocation().getX(), other.getSceneLocation().getY(), 1, 1, -1, -1);
	}*/

	private Pair<List<WorldPoint>, Boolean> pathToHover()
	{
		MenuEntry[] menuEntries = client.getMenuEntries();
		if (menuEntries.length == 0)
		{
			return null;
		}
		MenuEntry menuEntry;
		if (!client.isMenuOpen())
		{
			int i = 1;
			menuEntry = menuEntries[menuEntries.length - 1];
			MenuAction type = menuEntry.getType();
			while (type == MenuAction.EXAMINE_ITEM_GROUND || type == MenuAction.EXAMINE_NPC || type == MenuAction.EXAMINE_OBJECT)
			{
				// For some reason, RuneLite considers the "Examine" options to be the first menuEntryOptions when no right-click menu is open.
				// It's impossible to have "Examine" as left-click option, a far as I'm aware.
				// The first non-Examine option is the real left-click option.
				i += 1;
				menuEntry = menuEntries[menuEntries.length - i];
				type = menuEntry.getType();
			}
		}
		else
		{
			menuEntry = hoveredMenuEntry(menuEntries);
		}
		switch (menuEntry.getType())
		{
			case EXAMINE_ITEM_GROUND:
			case EXAMINE_NPC:
			case EXAMINE_OBJECT:
			case CANCEL:
			case CC_OP:
			case CC_OP_LOW_PRIORITY:
			case PLAYER_EIGTH_OPTION:
			case WIDGET_CLOSE:
			case WIDGET_CONTINUE:
			case WIDGET_FIRST_OPTION:
			case WIDGET_SECOND_OPTION:
			case WIDGET_THIRD_OPTION:
			case WIDGET_FOURTH_OPTION:
			case WIDGET_FIFTH_OPTION:
			case WIDGET_TARGET:
			case WIDGET_TARGET_ON_WIDGET:
			case WIDGET_TYPE_1:
			case WIDGET_TYPE_4:
			case WIDGET_TYPE_5:
			case WIDGET_USE_ON_ITEM:
			case RUNELITE:
			case RUNELITE_HIGH_PRIORITY:
			case RUNELITE_INFOBOX:
			case RUNELITE_OVERLAY:
			case RUNELITE_OVERLAY_CONFIG:
			case RUNELITE_PLAYER:
			{
				hoverCheckpointWPs.clear();
				return null;
			}
			case GAME_OBJECT_FIRST_OPTION:
			case GAME_OBJECT_SECOND_OPTION:
			case GAME_OBJECT_THIRD_OPTION:
			case GAME_OBJECT_FOURTH_OPTION:
			case GAME_OBJECT_FIFTH_OPTION:
			case WIDGET_TARGET_ON_GAME_OBJECT:
			case GROUND_ITEM_FIRST_OPTION:
			case GROUND_ITEM_SECOND_OPTION:
			case GROUND_ITEM_THIRD_OPTION:
			case GROUND_ITEM_FOURTH_OPTION:
			case GROUND_ITEM_FIFTH_OPTION:
			case WIDGET_TARGET_ON_GROUND_ITEM:
			{
				int x = menuEntry.getParam0();
				int y = menuEntry.getParam1();
				int id = menuEntry.getIdentifier();
				int objConfig = -1;
				int sizeX = 1;
				int sizeY = 1;
				TileObject tileObject = findTileObject(x, y, id);
				TileItem tileItem = findTileItem(x, y, id);
				if (tileObject == null && tileItem == null)
				{
					return null;
				}
				if (tileObject != null)
				{
					if (tileObject instanceof GameObject)
					{
						GameObject gameObject = (GameObject) tileObject;
						objConfig = gameObject.getConfig();
						sizeX = gameObject.sizeX();
						sizeY = gameObject.sizeY();
					}
					if (tileObject instanceof WallObject)
					{
						WallObject wallObject = (WallObject) tileObject;
						objConfig = wallObject.getConfig();
					}
					if (tileObject instanceof DecorativeObject)
					{
						DecorativeObject decorativeObject = (DecorativeObject) tileObject;
						objConfig = decorativeObject.getConfig();
					}
					if (tileObject instanceof GroundObject)
					{
						GroundObject groundObject = (GroundObject) tileObject;
						objConfig = groundObject.getConfig();
					}
				}
				return pathfinder.pathTo(x, y, sizeX, sizeY, objConfig, id);
			}
			case NPC_FIRST_OPTION:
			case NPC_SECOND_OPTION:
			case NPC_THIRD_OPTION:
			case NPC_FOURTH_OPTION:
			case NPC_FIFTH_OPTION:
			case WIDGET_TARGET_ON_NPC:
			case PLAYER_FIRST_OPTION:
			case PLAYER_SECOND_OPTION:
			case PLAYER_THIRD_OPTION:
			case PLAYER_FOURTH_OPTION:
			case PLAYER_FIFTH_OPTION:
			case PLAYER_SIXTH_OPTION:
			case PLAYER_SEVENTH_OPTION:
			case WIDGET_TARGET_ON_PLAYER:
			{
				Actor actor = menuEntry.getActor();
				if (actor == null)
				{
					return null;
				}
				int x = actor.getLocalLocation().getSceneX();
				int y = actor.getLocalLocation().getSceneY();
				int size = 1;
				if (actor instanceof NPC)
				{
					size = ((NPC) actor).getComposition().getSize();
				}
				return pathfinder.pathTo(x, y, size, size, -2, -1);
			}
			case WALK:
			default:
			{
				Tile selectedSceneTile = client.getSelectedSceneTile();
				if (selectedSceneTile == null)
				{
					return null;
				}
				return pathfinder.pathTo(client.getSelectedSceneTile());
			}
		}
	}
	/*
	private boolean hasArrived(int baseX, int baseY, int targetX, int targetY, int sizeX, int sizeY, int objConfig, int objID, int[][] flags)
	{
		int objShape = -1;
		int objRot = 0;
		switch (objConfig)
		{
			case -2:
				objShape = -2;
				break;
			case -1:
				break;
			default:
			{
				objShape = objConfig & 0x1F;
				objRot = objConfig >>> 6 & 3;
			}
		}
		if (objShape != -2)
		{
			// Not pathing to an Actor
			if (targetX <= baseX && baseX <= targetX + sizeX - 1 && targetY <= baseY && baseY <= targetY + sizeY - 1)
			{
				// Inside the object or on the target tile
				return true;
			}
		}
		switch (objShape)
		{
			case 0: // Pathing to straight wall
				return reachStraightWall(flags, baseX, baseY, targetX, targetY, objRot);
			case 2:	// Pathing to L wall
				return reachLWall(flags, baseX, baseY, targetX, targetY, objRot);
			case 6: // Diagonal wall decoration, diagonal offset
				return reachDiagonalWallDecoration(flags, baseX, baseY, targetX, targetY, objRot);
			case 7: // Diagonal wall decoration, no offset
				return reachDiagonalWallDecoration(flags, baseX, baseY, targetX, targetY, objRot + 2 & 0x3);
			case -2: // Pathing to Actor, not an official flag
			case 8: // Diagonal wall decoration, both sides of the wall
			case 9: // Pathing to diagonal wall
			case 10: // Pathing to straight centrepiece
			case 11: // Pathing to diagonal centrepiece
			case 22: // Pathing to ground decor
			{
				int objFlags = 0;
				if (Arrays.asList(10,11,22).contains(objShape))
				{
					objFlags = getObjectBlocking(objID, objRot);
				}
				return reachRectangularBoundary(flags, baseX, baseY, targetX, targetY, sizeX, sizeY, objFlags);
			}
		}
		return false;
	}

	private boolean reachRectangularBoundary(int[][] flags, int x, int y, int destX, int destY, int destWidth, int destHeight, int objectflags)
	{
		int east = destX + destWidth - 1;
		int north = destY + destHeight - 1;
		if (x == destX - 1 && y >= destY && y <= north &&
				(flags[x][y] & 0x8) == 0 &&
				(objectflags & 0x8) == 0)
		{
			//Valid destination tile to the west of the rectangularBoundary
			return true;
		}
		if (x == east + 1 && y >= destY && y <= north &&
				(flags[x][y] & 0x80) == 0 &&
				(objectflags & 0x2) == 0)
		{
			//Valid destination tile to the east of the rectangularBoundary
			return true;
		}
		if (y + 1 == destY && x >= destX && x <= east &&
				(flags[x][y] & 0x2) == 0 &&
				(objectflags & 0x4) == 0)
		{
			//Valid destination tile to the south of the rectangularBoundary
			return true;
		}
		return y == north + 1 && x >= destX && x <= east &&
				(flags[x][y] & 0x20) == 0 &&
				(objectflags & 0x1) == 0;
		//Test for valid destination tile to the north of the rectangularBoundary
	}

	private boolean reachStraightWall(int[][] flags, int x, int y, int destX, int destY, int rot)
	{
		switch (rot)
		{
			case 0:
			{
				if (x == destX - 1 && y == destY)
					return true;
				if (x == destX && y == destY + 1 && (flags[x][y] & 0x12c0120) == 0)
					return true;
				if (x == destX && y == destY - 1 && (flags[x][y] & 0x12c0102) == 0)
					return true;
				break;
			}
			case 1:
			{
				if (x == destX && y == destY + 1)
					return true;
				if (x == destX - 1 && y == destY && (flags[x][y] & 0x12c0108) == 0)
					return true;
				if (x == destX + 1 && y == destY && (flags[x][y] & 0x12c0180) == 0)
					return true;
				break;
			}
			case 2:
			{
				if (x == destX + 1 && y == destY)
					return true;
				if (x == destX && y == destY + 1 && (flags[x][y] & 0x12c0120) == 0)
					return true;
				if (x == destX && y == destY - 1 && (flags[x][y] & 0x12c0102) == 0)
					return true;
				break;
			}
			case 3:
			{
				if (x == destX && y == destY - 1)
					return true;
				if (x == destX - 1 && y == destY && (flags[x][y] & 0x12c0108) == 0)
					return true;
				if (x == destX + 1 && y == destY && (flags[x][y] & 0x12c0180) == 0)
					return true;
			}
		}
		return false;
	}

	private boolean reachLWall(int[][] flags, int x, int y, int destX, int destY, int rot)
	{
		int WESTWALLFLAGS = 0x12c0108;
		int NORTHWALLFLAGS = 0x12c0120;
		int EASTWALLFLAGS = 0x12c0180;
		int SOUTHWALLFLAGS = 0x12c0102;
		switch (rot)
		{
			case 0:
			{
				WESTWALLFLAGS = 0;
				NORTHWALLFLAGS = 0;
				break;
			}
			case 1:
			{
				NORTHWALLFLAGS = 0;
				EASTWALLFLAGS = 0;
				break;
			}
			case 2:
			{
				EASTWALLFLAGS = 0;
				SOUTHWALLFLAGS = 0;
				break;
			}
			case 3:
			{
				SOUTHWALLFLAGS = 0;
				WESTWALLFLAGS = 0;
			}
		}
		if (x == destX - 1 && y == destY && (flags[x][y] & WESTWALLFLAGS) == 0)
			return true;
		if (x == destX && y == destY + 1 && (flags[x][y] & NORTHWALLFLAGS) == 0)
			return true;
		if (x == destX + 1 && y == destY && (flags[x][y] & EASTWALLFLAGS) == 0)
			return true;
		return x == destX && y == destY - 1 && (flags[x][y] & SOUTHWALLFLAGS) == 0;
	}

	private boolean reachDiagonalWallDecoration(int[][] flags, int x, int y, int destX, int destY, int rot)
	{
		switch (rot)
		{
			case 0:
			{
				if (x == destX + 1 && y == destY && (flags[x][y] & 0x80) == 0)
					return true;
				if (x == destX && y == destY - 1 && (flags[x][y] & 0x2) == 0)
					return true;
				break;
			}
			case 1:
			{
				if (x == destX - 1 && y == destY && (flags[x][y] & 0x4) == 0)
					return true;
				if (x == destX && y == destY - 1 && (flags[x][y] & 0x2) == 0)
					return true;
				break;
			}
			case 2:
			{
				if (x == destX - 1 && y == destY && (flags[x][y] & 0x4) == 0)
					return true;
				if (x == destX && y == destY + 1 && (flags[x][y] & 0x20) == 0)
					return true;
				break;
			}
			case 3:
			{
				if (x == destX + 1 && y == destY && (flags[x][y] & 0x80) == 0)
					return true;
				if (x == destX && y == destY + 1 && (flags[x][y] & 0x20) == 0)
					return true;
			}
		}
		return false;
	}*/

	private void pathFromCheckpointTiles(List<WorldPoint> checkpointWPs, boolean running, List<WorldPoint> middlePathTiles, List<WorldPoint> pathTiles, boolean pathFound)
	{
		pathTiles.clear();
		middlePathTiles.clear();
		WorldArea currentWA = client.getLocalPlayer().getWorldArea();
		if (currentWA == null || checkpointWPs == null || checkpointWPs.size() == 0)
		{
			return;
		}
		if ((currentWA.getPlane() != checkpointWPs.get(0).getPlane()) && pathFound)
		{
			return;
		}
		boolean runSkip = true;
		int cpTileIndex = 0;
		while (currentWA.toWorldPoint().getX() != checkpointWPs.get(checkpointWPs.size() - 1).getX()
				|| currentWA.toWorldPoint().getY() != checkpointWPs.get(checkpointWPs.size() - 1).getY())
		{
			//log.info("checked tile: {}",currentWA.toWorldPoint());
			//log.info("last cpt: {}",checkpointWPs.get(checkpointWPs.size() - 1));
			WorldPoint cpTileWP = checkpointWPs.get(cpTileIndex);
			if (currentWA.toWorldPoint().equals(cpTileWP))
			{
				cpTileIndex += 1;
				cpTileWP = checkpointWPs.get(cpTileIndex);
			}
			int dx = Integer.signum(cpTileWP.getX() - currentWA.getX());
			int dy = Integer.signum(cpTileWP.getY() - currentWA.getY());
			WorldArea finalCurrentWA = currentWA;
			boolean movementCheck = currentWA.canTravelInDirection(client, dx, dy, (worldPoint -> {
				WorldPoint worldPoint1 = new WorldPoint(finalCurrentWA.getX() + dx, finalCurrentWA.getY(), client.getPlane());
				WorldPoint worldPoint2 = new WorldPoint(finalCurrentWA.getX(), finalCurrentWA.getY() + dy, client.getPlane());
				WorldPoint worldPoint3 = new WorldPoint(finalCurrentWA.getX() + dx, finalCurrentWA.getY() + dy, client.getPlane());
				for (WorldArea worldArea : npcBlockWAs)
				{
					if (worldArea.contains(worldPoint1) || worldArea.contains(worldPoint2) || worldArea.contains(worldPoint3))
					{
						return false;
					}
				}
				return true;
			}));
			if (movementCheck)
			{
				currentWA = new WorldArea(currentWA.getX() + dx, currentWA.getY() + dy, 1, 1, client.getPlane());
				if (currentWA.toWorldPoint().equals(checkpointWPs.get(checkpointWPs.size() - 1)) || !pathFound)
				{
					pathTiles.add(currentWA.toWorldPoint());
				}
				else if (runSkip && running)
				{
					middlePathTiles.add(currentWA.toWorldPoint());
				}
				else
				{
					pathTiles.add(currentWA.toWorldPoint());
				}
				runSkip = !runSkip;
				continue;
			}
			movementCheck = currentWA.canTravelInDirection(client, dx, 0, (worldPoint -> {
				for (WorldArea worldArea : npcBlockWAs)
				{
					WorldPoint worldPoint1 = new WorldPoint(finalCurrentWA.getX() + dx, finalCurrentWA.getY(), client.getPlane());
					if (worldArea.contains(worldPoint1))
					{
						return false;
					}
				}
				return true;
			}));
			if (dx != 0 && movementCheck)
			{
				currentWA = new WorldArea(currentWA.getX() + dx, currentWA.getY(), 1, 1, client.getPlane());
				if (currentWA.toWorldPoint().equals(checkpointWPs.get(checkpointWPs.size() - 1)) || !pathFound)
				{
					pathTiles.add(currentWA.toWorldPoint());
				}
				else if (runSkip && running)
				{
					middlePathTiles.add(currentWA.toWorldPoint());
				}
				else
				{
					pathTiles.add(currentWA.toWorldPoint());
				}
				runSkip = !runSkip;
				continue;
			}
			movementCheck = currentWA.canTravelInDirection(client, 0, 1, (worldPoint -> {
				for (WorldArea worldArea : npcBlockWAs)
				{
					WorldPoint worldPoint1 = new WorldPoint(finalCurrentWA.getX(), finalCurrentWA.getY() + dy, client.getPlane());
					if (worldArea.contains(worldPoint1))
					{
						return false;
					}
				}
				return true;
			}));
			if (dy != 0 && movementCheck)
			{
				currentWA = new WorldArea(currentWA.getX(), currentWA.getY() + dy, 1, 1, client.getPlane());
				if (currentWA.toWorldPoint().equals(checkpointWPs.get(checkpointWPs.size() - 1)) || !pathFound)
				{
					pathTiles.add(currentWA.toWorldPoint());
				}
				else if (runSkip && running)
				{
					middlePathTiles.add(currentWA.toWorldPoint());
				}
				else
				{
					pathTiles.add(currentWA.toWorldPoint());
				}
				runSkip = !runSkip;
				continue;
			}
			return;
		}
	}

	private void updateCheckpointTiles()
	{
		if (lastTickWorldLocation == null)
		{
			return;
		}
		WorldArea currentWA = new WorldArea(lastTickWorldLocation.getX(), lastTickWorldLocation.getY(), 1,1, client.getPlane());
		if (activeCheckpointWPs == null)
		{
			return;
		}
		if ((lastTickWorldLocation.getPlane() != activeCheckpointWPs.get(0).getPlane()) && activePathFound)
		{
			WorldPoint lastActiveCPTile = activeCheckpointWPs.get(0);
			activeCheckpointWPs.clear();
			activeCheckpointWPs.add(lastActiveCPTile);
			pathActive = false;
			return;
		}
		int cpTileIndex = 0;
		int steps = 0;
		while (currentWA.toWorldPoint().getX() != activeCheckpointWPs.get(activeCheckpointWPs.size() - 1).getX()
		|| currentWA.toWorldPoint().getY() != activeCheckpointWPs.get(activeCheckpointWPs.size() - 1).getY())
		{
			WorldPoint cpTileWP = activeCheckpointWPs.get(cpTileIndex);
			if (currentWA.toWorldPoint().equals(cpTileWP))
			{
				cpTileIndex += 1;
				cpTileWP = activeCheckpointWPs.get(cpTileIndex);
			}
			int dx = Integer.signum(cpTileWP.getX() - currentWA.getX());
			int dy = Integer.signum(cpTileWP.getY() - currentWA.getY());
			WorldArea finalCurrentWA = currentWA;
			boolean movementCheck = currentWA.canTravelInDirection(client, dx, dy, (worldPoint -> {
				WorldPoint worldPoint1 = new WorldPoint(finalCurrentWA.getX() + dx, finalCurrentWA.getY(), client.getPlane());
				WorldPoint worldPoint2 = new WorldPoint(finalCurrentWA.getX(), finalCurrentWA.getY() + dy, client.getPlane());
				WorldPoint worldPoint3 = new WorldPoint(finalCurrentWA.getX() + dx, finalCurrentWA.getY() + dy, client.getPlane());
				for (WorldArea worldArea : npcBlockWAs)
				{
					if (worldArea.contains(worldPoint1) || worldArea.contains(worldPoint2) || worldArea.contains(worldPoint3))
					{
						return false;
					}
				}
				return true;
			}));
			if (movementCheck)
			{
				currentWA = new WorldArea(currentWA.getX() + dx, currentWA.getY() + dy, 1, 1, client.getPlane());
			}
			else
			{
				movementCheck = currentWA.canTravelInDirection(client, dx, 0, (worldPoint -> {
					WorldPoint worldPoint1 = new WorldPoint(finalCurrentWA.getX() + dx, finalCurrentWA.getY(), client.getPlane());
					for (WorldArea worldArea : npcBlockWAs)
					{
						if (worldArea.contains(worldPoint1))
						{
							return false;
						}
					}
					return true;
				}));
				if (dx != 0 && movementCheck)
				{
					currentWA = new WorldArea(currentWA.getX() + dx, currentWA.getY(), 1, 1, client.getPlane());
				}
				else
				{
					movementCheck = currentWA.canTravelInDirection(client, 0, dy, (worldPoint -> {
						WorldPoint worldPoint1 = new WorldPoint(finalCurrentWA.getX(), finalCurrentWA.getY() + dy, client.getPlane());
						for (WorldArea worldArea : npcBlockWAs)
						{
							if (worldArea.contains(worldPoint1))
							{
								return false;
							}
						}
						return true;
					}));
					if (dy != 0 && movementCheck)
					{
						currentWA = new WorldArea(currentWA.getX(), currentWA.getY() + dy, 1, 1, client.getPlane());
					}
				}
			}
			steps += 1;
			if (steps == 2 || !isRunning || !activePathFound)
			{
				break;
			}
		}
		if (steps == 0)
		{
			WorldPoint lastActiveCPTile = activeCheckpointWPs.get(0);
			activeCheckpointWPs.clear();
			activeCheckpointWPs.add(lastActiveCPTile);
			pathActive = false;
			activePathDestination.objConfig = -1;
			activePathMismatchLastTick = true;
			return;
		}
		if (!currentWA.toWorldPoint().equals(client.getLocalPlayer().getWorldLocation()))
		{
			/*log.info("after {}",currentWA.toWorldPoint());
			log.info("actual {}",client.getLocalPlayer().getWorldLocation());
			log.info("size {}", activeCheckpointWPs.size());*/
			if (activePathStartedLastTick)
			{
				LocalPoint localPoint = LocalPoint.fromWorld(client, activePathDestination.worldPoint);
				if (localPoint == null)
				{
					return;
				}
				Pair<List<WorldPoint>, Boolean> pathResult = pathfinder.pathTo(localPoint.getSceneX(), localPoint.getSceneY(), activePathDestination.sizeX, activePathDestination.sizeY, activePathDestination.objConfig, activePathDestination.objID);
				if (pathResult == null)
				{
					return;
				}
				lastTickWorldLocation = client.getLocalPlayer().getWorldLocation();
				pathActive = true;
				activeCheckpointWPs = pathResult.getLeft();
				activePathFound = pathResult.getRight();
				pathFromCheckpointTiles(activeCheckpointWPs, isRunning, activeMiddlePathTiles, activePathTiles, activePathFound);
				activePathStartedLastTick = false;
			}
			else if (activePathMismatchLastTick)
			{
				WorldPoint lastActiveCPTile = activeCheckpointWPs.get(0);
				activeCheckpointWPs.clear();
				activeCheckpointWPs.add(lastActiveCPTile);
				pathActive = false;
				activePathStartedLastTick = false;
			}
			activePathMismatchLastTick = true;
		}
		else
		{
			activePathMismatchLastTick = false;
		}
		for (int i = 0; i < cpTileIndex; i++)
		{
			if (activeCheckpointWPs.size()>1)
			{
				activeCheckpointWPs.remove(0);
			}
		}
	}

	private void updateNpcBlockings()
	{
		List<NPC> npcs = client.getNpcs();
		npcBlockWAs.clear();
		for (NPC npc : npcs)
		{
			NPCComposition npcComposition = npc.getTransformedComposition();
			if (npcComposition == null)
			{
				continue;
			}
			if (getNpcBlocking(npcComposition.getId()))
			{
				npcBlockWAs.add(npc.getWorldArea());
			}
		}
	}

	private boolean willRunOnClick()
	{
		boolean willRun = (client.getVarpValue(173) == 1); //run toggled on
		if (!ctrlHeld)
		{
			return willRun;
		}
		int ctrlSetting = client.getVarbitValue(13132);
		switch (ctrlSetting)
		{
			case 0: //never
				return willRun;
			case 1: //walk --> run only
				return true;
			case 2: //Run --> walk only
				return false;
			case 3: //Always
				return !willRun;
			default:
				return willRun;
		}
	}

	TileItem findTileItem(int x, int y, int id)
	{
		Scene scene = client.getScene();
		Tile[][][] tiles = scene.getTiles();
		Tile tile = tiles[client.getPlane()][x][y];
		if (tile == null)
		{
			return null;
		}
		List<TileItem> tileItems = tile.getGroundItems();
		if (tileItems == null)
		{
			return null;
		}
		for (TileItem tileItem : tileItems)
		{
			if (tileItem != null && tileItem.getId() == id)
			{
				return tileItem;
			}
		}
		return null;
	}

	TileObject findTileObject(int x, int y, int id)
	{
		Scene scene = client.getScene();
		Tile[][][] tiles = scene.getTiles();
		Tile tile = tiles[client.getPlane()][x][y];
		if (tile != null)
		{
			for (GameObject gameObject : tile.getGameObjects())
			{
				if (gameObject != null && gameObject.getId() == id)
				{
					return gameObject;
				}
			}

			WallObject wallObject = tile.getWallObject();
			if (wallObject != null && wallObject.getId() == id)
			{
				return wallObject;
			}

			DecorativeObject decorativeObject = tile.getDecorativeObject();
			if (decorativeObject != null && decorativeObject.getId() == id)
			{
				return decorativeObject;
			}

			GroundObject groundObject = tile.getGroundObject();
			if (groundObject != null && groundObject.getId() == id)
			{
				return groundObject;
			}
		}
		return null;
	}

	public static int getObjectBlocking(final int objectId, final int rotation)
	{
		if (objectBlocking == null)
		{
			return 0;
		}
		int blockingValue = objectBlocking.getOrDefault(objectId, 0);
		return rotation == 0 ? blockingValue : (((blockingValue << rotation) & 0xF) + (blockingValue >> (4 - rotation)));
	}

	public static boolean getNpcBlocking(final int npcCompId)
	{
		if (npcBlocking == null)
		{
			return false;
		}
		return npcBlocking.getOrDefault(npcCompId, 0) == 1;
	}

	private static Map<Integer, Integer> readFile(String name) {
		try {
			InputStream inputStream = PathMarkerPlugin.class.getResourceAsStream(name);
			if (inputStream == null)
			{
				return null;
			}
			InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
			BufferedReader reader = new BufferedReader(streamReader);
			final Map<Integer, Integer> map = new LinkedHashMap<>();
			for (String line; (line = reader.readLine()) != null;) {
				String[] split = line.split("=");
				int id = Integer.parseInt(split[0]);
				int blocking = Integer.parseInt(split[1].split(" ")[0]);
				map.put(id, blocking);
			}
			reader.close();
			return map;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private Point minimapToWorldPoint()
	{
		if (client.getMenuEntries().length != 1)
		{
			// Minimap hovering doesn't add menu entries other than the default "cancel"
			return null;
		}
		Widget minimapDrawWidget;
		if (client.isResized())
		{
			if (client.getVarbitValue(Varbits.SIDE_PANELS) == 1)
			{
				minimapDrawWidget = client.getWidget(WidgetInfo.RESIZABLE_MINIMAP_DRAW_AREA);
			}
			else
			{
				minimapDrawWidget = client.getWidget(WidgetInfo.RESIZABLE_MINIMAP_STONES_DRAW_AREA);
			}
		}
		else
		{
			minimapDrawWidget = client.getWidget(WidgetInfo.FIXED_VIEWPORT_MINIMAP_DRAW_AREA);
		}

		if (minimapDrawWidget == null || minimapDrawWidget.isHidden())
		{
			return null;
		}
		Point mouseCanvasPosition = client.getMouseCanvasPosition();
		if (!minimapDrawWidget.contains(mouseCanvasPosition))
		{
			return null;
		}
		int widgetX = mouseCanvasPosition.getX() - minimapDrawWidget.getCanvasLocation().getX() - minimapDrawWidget.getWidth()/2;
		int widgetY = mouseCanvasPosition.getY() - minimapDrawWidget.getCanvasLocation().getY() - minimapDrawWidget.getHeight()/2;
		int angle = client.getMapAngle() & 0x7FF;
		int sine = (int) (65536.0D * Math.sin((double) angle * Math.PI / 1024d));
		int cosine = (int) (65536.0D * Math.cos((double) angle * Math.PI / 1024d));
		int xx = cosine * widgetX + widgetY * sine >> 11;
		int yy = widgetY * cosine - widgetX * sine >> 11;
		int deltaX = xx + client.getLocalPlayer().getLocalLocation().getSceneX() >> 7;
		int deltaY = client.getLocalPlayer().getLocalLocation().getSceneY() - yy >> 7;
		LocalPoint localPoint = client.getLocalPlayer().getLocalLocation();
		if (localPoint == null)
		{
			return null;
		}
		log.info("delta X, {}",deltaX);
		return new Point(localPoint.getSceneX() + deltaX, localPoint.getSceneY() + deltaY);
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getIndex() == 173)
		{
			// Run toggled
			int[] varps = client.getVarps();
			isRunning = varps[173] == 1;
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		log.info("Menu option clicked, type: {}", event.getMenuAction());
		log.info("Menu option: {}", event.getMenuEntry());
		switch (event.getMenuAction())
		{
			case EXAMINE_ITEM_GROUND:
			case EXAMINE_NPC:
			case EXAMINE_OBJECT:
			case CANCEL:
			case CC_OP:
			case CC_OP_LOW_PRIORITY:
			case PLAYER_EIGTH_OPTION:
			case WIDGET_CLOSE:
			case WIDGET_CONTINUE:
			case WIDGET_FIRST_OPTION:
			case WIDGET_SECOND_OPTION:
			case WIDGET_THIRD_OPTION:
			case WIDGET_FOURTH_OPTION:
			case WIDGET_FIFTH_OPTION:
			case WIDGET_TARGET:
			case WIDGET_TARGET_ON_WIDGET:
			case WIDGET_TYPE_1:
			case WIDGET_TYPE_4:
			case WIDGET_TYPE_5:
			case WIDGET_USE_ON_ITEM:
			case RUNELITE:
			case RUNELITE_HIGH_PRIORITY:
			case RUNELITE_INFOBOX:
			case RUNELITE_OVERLAY:
			case RUNELITE_OVERLAY_CONFIG:
			case RUNELITE_PLAYER:
				return;
			case GAME_OBJECT_FIRST_OPTION:
			case GAME_OBJECT_SECOND_OPTION:
			case GAME_OBJECT_THIRD_OPTION:
			case GAME_OBJECT_FOURTH_OPTION:
			case GAME_OBJECT_FIFTH_OPTION:
			case WIDGET_TARGET_ON_GAME_OBJECT:
			case GROUND_ITEM_FIRST_OPTION:
			case GROUND_ITEM_SECOND_OPTION:
			case GROUND_ITEM_THIRD_OPTION:
			case GROUND_ITEM_FOURTH_OPTION:
			case GROUND_ITEM_FIFTH_OPTION:
			case WIDGET_TARGET_ON_GROUND_ITEM:
			{
				int x = event.getParam0();
				int y = event.getParam1();
				int id = event.getId();
				int config = -1;
				int sizeX = 1;
				int sizeY = 1;
				TileObject tileObject = findTileObject(x, y, id);
				TileItem tileItem = findTileItem(x, y, id);
				if (tileObject == null && tileItem == null)
				{
					return;
				}
				isRunning = willRunOnClick();
				if (tileObject != null)
				{
					if (tileObject instanceof GameObject)
					{
						GameObject gameObject = (GameObject) tileObject;
						config = gameObject.getConfig();
						sizeX = gameObject.sizeX();
						sizeY = gameObject.sizeY();
					}
					if (tileObject instanceof WallObject)
					{
						WallObject wallObject = (WallObject) tileObject;
						config = wallObject.getConfig();
					}
					if (tileObject instanceof DecorativeObject)
					{
						DecorativeObject decorativeObject = (DecorativeObject) tileObject;
						config = decorativeObject.getConfig();
					}
					if (tileObject instanceof GroundObject)
					{
						GroundObject groundObject = (GroundObject) tileObject;
						config = groundObject.getConfig();
					}
				}
				WorldPoint worldPoint = WorldPoint.fromScene(client, x, y, client.getPlane());
				Pair<List<WorldPoint>, Boolean> pathResult = pathfinder.pathTo(x, y, sizeX, sizeY, config, id);
				activePathDestination = new PathDestination(worldPoint, sizeX, sizeY, config, id);
				if (pathResult == null)
				{
					return;
				}
				lastTickWorldLocation = client.getLocalPlayer().getWorldLocation();
				pathActive = true;
				activeCheckpointWPs = pathResult.getLeft();
				activePathFound = pathResult.getRight();
				pathFromCheckpointTiles(activeCheckpointWPs, isRunning, activeMiddlePathTiles, activePathTiles, activePathFound);
				activePathStartedLastTick = true;
				calcTilePathOnNextClientTick = false;
				return;
			}
			case NPC_FIRST_OPTION:
			case NPC_SECOND_OPTION:
			case NPC_THIRD_OPTION:
			case NPC_FOURTH_OPTION:
			case NPC_FIFTH_OPTION:
			case WIDGET_TARGET_ON_NPC:
			case PLAYER_FIRST_OPTION:
			case PLAYER_SECOND_OPTION:
			case PLAYER_THIRD_OPTION:
			case PLAYER_FOURTH_OPTION:
			case PLAYER_FIFTH_OPTION:
			case PLAYER_SIXTH_OPTION:
			case PLAYER_SEVENTH_OPTION:
			case WIDGET_TARGET_ON_PLAYER:
			{
				Actor actor = event.getMenuEntry().getActor();
				if (actor == null)
				{
					return;
				}
				isRunning = willRunOnClick();
				LocalPoint localPoint = LocalPoint.fromWorld(client, actor.getWorldLocation());
				if (localPoint == null)
				{
					return;
				}
				int x = localPoint.getSceneX();
				int y = localPoint.getSceneY();
				int size = 1;
				if (actor instanceof NPC)
				{
					size = ((NPC) actor).getComposition().getSize();
				}
				WorldPoint worldPoint = WorldPoint.fromScene(client, x, y, client.getPlane());
				Pair<List<WorldPoint>, Boolean> pathResult = pathfinder.pathTo(x, y, size, size, -2, -1);
				activePathDestination = new PathDestination(worldPoint, size, size, -2, -1, actor);
				if (pathResult == null)
				{
					return;
				}
				lastTickWorldLocation = client.getLocalPlayer().getWorldLocation();
				pathActive = true;
				activeCheckpointWPs = pathResult.getLeft();
				activePathFound = pathResult.getRight();
				pathFromCheckpointTiles(activeCheckpointWPs, isRunning, activeMiddlePathTiles, activePathTiles, activePathFound);
				activePathStartedLastTick = true;
				calcTilePathOnNextClientTick = false;
				return;
			}
			case WALK:
			default:
			{
				if (!client.isMenuOpen())
				{
					calcTilePathOnNextClientTick = true;
					return;
				}
				if (oldSelectedSceneTile == null)
				{
					return;
				}
				isRunning = willRunOnClick();
				WorldPoint worldPoint = WorldPoint.fromScene(client, oldSelectedSceneTile.getSceneLocation().getX(), oldSelectedSceneTile.getSceneLocation().getY(), client.getPlane());
				Pair<List<WorldPoint>, Boolean> pathResult = pathfinder.pathTo(oldSelectedSceneTile);
				activePathDestination = new PathDestination(worldPoint, 1, 1, -1, -1);
				if (pathResult == null)
				{
					return;
				}
				lastTickWorldLocation = client.getLocalPlayer().getWorldLocation();
				pathActive = true;
				activeCheckpointWPs = pathResult.getLeft();
				activePathFound = pathResult.getRight();
				pathFromCheckpointTiles(activeCheckpointWPs, isRunning, activeMiddlePathTiles, activePathTiles, activePathFound);
				activePathStartedLastTick = true;
				calcTilePathOnNextClientTick = (event.getMenuAction() == MenuAction.WALK && !client.isMenuOpen());
			}
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case HOPPING:
			case LOGGING_IN:
			{
				activeCheckpointWPs.clear();
				activeCheckpointWPs.add(new WorldPoint(0,0,client.getPlane()));
				pathActive = false;
			}
		}
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (calcTilePathOnNextClientTick)
		{
			Tile selectedSceneTile = client.getSelectedSceneTile();
			if (selectedSceneTile != null)
			{
				isRunning = willRunOnClick();
				WorldPoint worldPoint = WorldPoint.fromScene(client, selectedSceneTile.getSceneLocation().getX(), selectedSceneTile.getSceneLocation().getY(), client.getPlane());
				Pair<List<WorldPoint>, Boolean> pathResult = pathfinder.pathTo(selectedSceneTile);
				activePathDestination = new PathDestination(worldPoint, 1, 1, -1, -1);
				if (pathResult != null)
				{
					lastTickWorldLocation = client.getLocalPlayer().getWorldLocation();
					pathActive = true;
					activeCheckpointWPs = pathResult.getLeft();
					activePathFound = pathResult.getRight();
					pathFromCheckpointTiles(activeCheckpointWPs, isRunning, activeMiddlePathTiles, activePathTiles, activePathFound);
					activePathStartedLastTick = true;
					calcTilePathOnNextClientTick = false;
				}
			}
		}
		Tile selectedSceneTile = client.getSelectedSceneTile();
		MenuEntry[] menuEntries = client.getMenuEntries();
		if (menuEntries.length == 1 && !client.isMenuOpen())
		{
			Point point = minimapToWorldPoint();
			if (point != null)
			{
				Pair<List<WorldPoint>, Boolean> pathResult = pathfinder.pathTo(point.getX(), point.getY(), 1,1,-1,-1);
				if (pathResult != null)
				{
					hoverCheckpointWPs = pathResult.getLeft();
					hoverPathFound = pathResult.getRight();
					if (hoverCheckpointWPs != null)
					{
						lastSelectedSceneTile = selectedSceneTile;
					}
				}
			}
		}
		int i = 0;
		if (lastSelectedSceneTile==null || lastSelectedSceneTile!=selectedSceneTile
				|| (client.isMenuOpen() && hoveredMenuEntry(menuEntries) != lastSelectedMenuEntry)
				|| !Arrays.equals(oldMenuEntries, menuEntries))
		{
			if (client.isMenuOpen())
			{
				lastSelectedMenuEntry = hoveredMenuEntry(menuEntries);
			}
			if (selectedSceneTile != null)
			{
				Pair<List<WorldPoint>, Boolean> pathResult = pathToHover();
				if (pathResult != null)
				{
					hoverCheckpointWPs = pathResult.getLeft();
					hoverPathFound = pathResult.getRight();
					if (hoverCheckpointWPs != null)
					{
						lastSelectedSceneTile = selectedSceneTile;
					}
				}
			}
		}
		oldSelectedSceneTile = client.getSelectedSceneTile();
		oldMenuEntries = menuEntries;
		pathFromCheckpointTiles(hoverCheckpointWPs, willRunOnClick(), hoverMiddlePathTiles, hoverPathTiles, hoverPathFound);
	}

	private MenuEntry hoveredMenuEntry(final MenuEntry[] menuEntries)
	{
		final int menuX = client.getMenuX();
		final int menuY = client.getMenuY();
		final int menuWidth = client.getMenuWidth();
		final Point mousePosition = client.getMouseCanvasPosition();

		int dy = mousePosition.getY() - menuY;
		dy -= 19; // Height of Choose Option
		if (dy < 0)
		{
			return menuEntries[0];
		}

		int idx = dy / 15; // Height of each menu option
		idx = menuEntries.length - 1 - idx;

		if (mousePosition.getX() > menuX && mousePosition.getX() < menuX + menuWidth
				&& idx >= 0 && idx < menuEntries.length)
		{
			return menuEntries[idx];
		}
		return menuEntries[0];
	}

	/*
	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		Pair<List<WorldPoint>, Boolean> pathResult = pathToHover();
		if (pathResult == null)
		{
			return;
		}
		hoverCheckpointWPs = pathResult.getLeft();
		hoverPathFound = pathResult.getRight();
	}*/

	@Subscribe
	public void onGameTick(GameTick event)
	{
		//log.info("Active cpts length: {}",activeCheckpointWPs.size());
		updateNpcBlockings();
		WorldPoint currentWorldLocation = client.getLocalPlayer().getWorldLocation();
		if (lastTickWorldLocation == null || lastTickWorldLocation != currentWorldLocation)
		{
			Pair<List<WorldPoint>, Boolean> pathResult = pathToHover();
			if (pathResult != null)
			{
				hoverCheckpointWPs = pathResult.getLeft();
				hoverPathFound = pathResult.getRight();
			}
		}
		if (hoverCheckpointWPs !=null && hoverCheckpointWPs.size()>0)
		{
			pathFromCheckpointTiles(hoverCheckpointWPs, willRunOnClick(), hoverMiddlePathTiles, hoverPathTiles, hoverPathFound);
		}
		if (currentWorldLocation.equals(activeCheckpointWPs.get(activeCheckpointWPs.size() - 1))
				|| (lastTickWorldLocation != null && currentWorldLocation.distanceTo(lastTickWorldLocation) > 2))
		{
			pathActive = false;
		}
		updateCheckpointTiles();
		if (pathActive && activePathDestination.objConfig == -2 && activeCheckpointWPs.size()<2)
		{
			// Path is recalculated when there's <2 checkpoint tiles remaining when pathing to a NPC/player
			LocalPoint localPoint = LocalPoint.fromWorld(client, activePathDestination.actor.getWorldLocation());
			if (localPoint != null)
			{
				Pair<List<WorldPoint>, Boolean> pathResult = pathfinder.pathTo(localPoint.getSceneX(), localPoint.getSceneY(), activePathDestination.sizeX, activePathDestination.sizeY, activePathDestination.objConfig, activePathDestination.objID);
				if (pathResult != null)
				{
					pathActive = true;
					activeCheckpointWPs = pathResult.getLeft();
					activePathFound = pathResult.getRight();
					activePathStartedLastTick = false;
				}
			}
		}
		pathFromCheckpointTiles(activeCheckpointWPs, isRunning, activeMiddlePathTiles, activePathTiles, activePathFound);
		lastTickWorldLocation = currentWorldLocation;
	}
}
