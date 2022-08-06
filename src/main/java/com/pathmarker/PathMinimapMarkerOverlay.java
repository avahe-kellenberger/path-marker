package com.pathmarker;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.*;

public class PathMinimapMarkerOverlay extends Overlay
{
    private final Client client;
    private final PathMarkerPlugin plugin;

    @Inject
    private PathMarkerConfig config;

    private double angle;

    @Inject
    private PathMinimapMarkerOverlay(Client client, PathMarkerConfig config, PathMarkerPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.LOW);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        angle = client.getMapAngle() * 0.0030679615D;
        if (config.hoverPathMode() != PathMarkerConfig.pathMode.NEITHER)
        {
            for (WorldPoint worldPoint : plugin.getHoverPathTiles())
            {
                if (config.hoverPathMode() == PathMarkerConfig.pathMode.BOTH || config.hoverPathMode() == PathMarkerConfig.pathMode.MINIMAP)
                {
                    renderMinimapTile(graphics, worldPoint, config.hoverPathColor1());
                }
            }
            for (WorldPoint worldPoint : plugin.getHoverMiddlePathTiles())
            {
                if (config.hoverPathMode() == PathMarkerConfig.pathMode.BOTH || config.hoverPathMode() == PathMarkerConfig.pathMode.MINIMAP)
                {
                    renderMinimapTile(graphics, worldPoint, config.hoverPathColor2());
                }
            }
        }
        if (config.activePathDrawLocations() != PathMarkerConfig.pathMode.NEITHER && plugin.isPathActive())
        {
            for (WorldPoint worldPoint : plugin.getActivePathTiles())
            {
                if (config.activePathDrawLocations() == PathMarkerConfig.pathMode.BOTH || config.activePathDrawLocations() == PathMarkerConfig.pathMode.MINIMAP)
                {
                    renderMinimapTile(graphics, worldPoint, config.activePathColor1());
                }
            }
            for (WorldPoint worldPoint : plugin.getActiveMiddlePathTiles())
            {
                if (config.activePathDrawLocations() == PathMarkerConfig.pathMode.BOTH || config.activePathDrawLocations() == PathMarkerConfig.pathMode.MINIMAP)
                {
                    renderMinimapTile(graphics, worldPoint, config.activePathColor2());
                }
            }
        }
        return null;
    }

    private void renderMinimapTile(Graphics2D graphics, WorldPoint worldPoint, Color color)
    {
        WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
        if (worldPoint.distanceTo(playerLocation) >= 50) {
            return;
        }
        LocalPoint lp = LocalPoint.fromWorld(client, worldPoint);
        if (lp == null) {
            return;
        }
        Point miniMapPoint = Perspective.localToMinimap(client, lp);
        if (miniMapPoint == null) {
            return;
        }
        graphics.setColor(color);
        graphics.rotate(angle, miniMapPoint.getX(), miniMapPoint.getY());
        graphics.fillRect(miniMapPoint.getX() - 2, miniMapPoint.getY() - 2, 4, 4);
        graphics.rotate(-angle, miniMapPoint.getX(), miniMapPoint.getY());
    }
}
