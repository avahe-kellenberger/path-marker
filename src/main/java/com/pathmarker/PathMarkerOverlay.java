package com.pathmarker;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.*;

@Slf4j
public class PathMarkerOverlay extends Overlay
{
    private final Client client;
    private final PathMarkerPlugin plugin;

    @Inject
    private PathMarkerConfig config;

    @Inject
    private PathMarkerOverlay(Client client, PathMarkerConfig config, PathMarkerPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.LOW);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        /*
        Scene scene = client.getScene();
        Tile[][][] tiles = scene.getTiles();
        int xOffset = 64 - client.getLocalPlayer().getLocalLocation().getSceneX();
        int yOffset = 64 - client.getLocalPlayer().getLocalLocation().getSceneY();
        if (client.getMenuEntries()[client.getMenuEntries().length - 1].getType() == MenuAction.GAME_OBJECT_FIRST_OPTION)
        {
            GameObject object = (GameObject) plugin.findTileObject(client.getMenuEntries()[client.getMenuEntries().length - 1].getParam0(),client.getMenuEntries()[client.getMenuEntries().length - 1].getParam1(),client.getMenuEntries()[client.getMenuEntries().length - 1].getIdentifier());
            for (Tile[] x : tiles[client.getPlane()])
            {
                for  (Tile y : x)
                {
                    if (y != null)
                    {
                        for (GameObject gameObject : y.getGameObjects())
                        {
                            //if (plugin.method984(client.getSelectedSceneTile().getLocalLocation().getSceneY() + yOffset, y.getLocalLocation().getSceneX() + xOffset, client.getSelectedSceneTile().getLocalLocation().getSceneX() + xOffset, plugin.getBlocking(client.getMenuEntries()[client.getMenuEntries().length - 1].getIdentifier()),3, y.getLocalLocation().getSceneY() + yOffset, 3, client.getCollisionMaps()[client.getPlane()].getFlags()))
                            if (plugin.reachRectangularBoundary(client.getCollisionMaps()[client.getPlane()].getFlags(), y.getLocalLocation().getSceneX() + xOffset, y.getLocalLocation().getSceneY() + yOffset, client.getSelectedSceneTile().getLocalLocation().getSceneX() + xOffset,client.getSelectedSceneTile().getLocalLocation().getSceneY() + yOffset, object.sizeX(), object.sizeY(), plugin.getBlocking(client.getMenuEntries()[client.getMenuEntries().length - 1].getIdentifier(),object.getConfig() >>> 6 & 3)))
                            {
                                final Polygon poly = Perspective.getCanvasTilePoly(client, y.getLocalLocation());
                                if (poly == null)
                                {
                                    return null;
                                }
                                OverlayUtil.renderPolygon(graphics, poly, Color.RED);
                            }
                        }
                    }
                }
            }
        }*/
        Color fillColour1;
        Color fillColour2;
        if (config.hoverPathMode() != PathMarkerConfig.pathMode.NEITHER)
        {
            fillColour1 = new Color(config.hoverPathColor1().getRed(),config.hoverPathColor1().getGreen(),config.hoverPathColor1().getBlue(),config.hoverPathFillOpacity());
            fillColour2 = new Color(config.hoverPathColor2().getRed(),config.hoverPathColor2().getGreen(),config.hoverPathColor2().getBlue(),config.hoverPathFillOpacity());
            for (WorldPoint worldPoint : plugin.getHoverPathTiles())
            {
                if (config.hoverPathMode() == PathMarkerConfig.pathMode.BOTH || config.hoverPathMode() == PathMarkerConfig.pathMode.GAME_WORLD)
                {
                    renderTile(graphics, worldPoint, config.hoverPathColor1(), fillColour1);
                }
            }
            for (WorldPoint worldPoint : plugin.getHoverMiddlePathTiles())
            {
                if (config.hoverPathMode() == PathMarkerConfig.pathMode.BOTH || config.hoverPathMode() == PathMarkerConfig.pathMode.GAME_WORLD)
                {
                    renderTile(graphics, worldPoint, config.hoverPathColor2(), fillColour2);
                }
            }
        }
        if (config.activePathDrawLocations() != PathMarkerConfig.pathMode.NEITHER && plugin.isPathActive())
        {
            fillColour1 = new Color(config.activePathColor1().getRed(),config.activePathColor1().getGreen(),config.activePathColor1().getBlue(),config.activePathFillOpacity());
            fillColour2 = new Color(config.activePathColor2().getRed(),config.activePathColor2().getGreen(),config.activePathColor2().getBlue(),config.activePathFillOpacity());
            for (WorldPoint worldPoint : plugin.getActivePathTiles())
            {
                if (config.activePathDrawLocations() == PathMarkerConfig.pathMode.BOTH || config.activePathDrawLocations() == PathMarkerConfig.pathMode.GAME_WORLD)
                {
                    renderTile(graphics, worldPoint, config.activePathColor1(), fillColour1);
                }
            }
            for (WorldPoint worldPoint : plugin.getActiveMiddlePathTiles())
            {
                if (config.activePathDrawLocations() == PathMarkerConfig.pathMode.BOTH || config.activePathDrawLocations() == PathMarkerConfig.pathMode.GAME_WORLD)
                {
                    renderTile(graphics, worldPoint, config.activePathColor2(), fillColour2);
                }
            }
        }
        /*for (WorldPoint worldPoint : plugin.getActiveCheckpointWPs())
        {
            renderTile(graphics, worldPoint, Color.GREEN);
        }*/
        return null;
    }

    private void renderTile(Graphics2D graphics, WorldPoint worldPoint, Color color, Color fillOpacity)
    {
        Stroke stroke = new BasicStroke(1);
        LocalPoint lp = LocalPoint.fromWorld(client, worldPoint);
        if (lp == null)
        {
            return;
        }
        final Polygon poly = Perspective.getCanvasTilePoly(client, lp);
        if (poly == null)
        {
            return;
        }
        OverlayUtil.renderPolygon(graphics, poly, color, fillOpacity, stroke);
    }
}
