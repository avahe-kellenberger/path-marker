package com.pathmarker;

import net.runelite.client.input.KeyListener;

import javax.inject.Inject;
import java.awt.event.KeyEvent;

public class CtrlListener implements KeyListener
{
    @Inject
    PathMarkerPlugin plugin;

    @Override
    public void keyTyped(KeyEvent event)
    {
    }

    @Override
    public void keyPressed(KeyEvent event)
    {
        if (KeyEvent.VK_CONTROL == event.getKeyCode())
        {
            plugin.setCtrlHeld(true);
        }
    }

    @Override
    public void keyReleased(KeyEvent event)
    {
        if (KeyEvent.VK_CONTROL == event.getKeyCode())
        {
            plugin.setCtrlHeld(false);
        }
    }
}
