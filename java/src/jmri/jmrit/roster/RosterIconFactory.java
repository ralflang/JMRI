package jmri.jmrit.roster;

import java.awt.Graphics;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import javax.swing.ImageIcon;

import jmri.InstanceManagerAutoDefault;

/**
 * Generate and cache icons at a given height. A managed instance will generate
 * icons for a default height, while unmanaged instances can be created to
 * generate icons at different heights.
 * <hr>
 * This file is part of JMRI.
 * <p>
 * JMRI is free software; you can redistribute it and/or modify it under the
 * terms of version 2 of the GNU General Public License as published by the Free
 * Software Foundation. See the "COPYING" file for a copy of this license.
 * <p>
 * JMRI is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * @author Lionel Jeanson Copyright (C) 2009
 */
public class RosterIconFactory implements InstanceManagerAutoDefault {

    private final int iconHeight;
    HashMap<String, ImageIcon> icons = new HashMap<>();

    public RosterIconFactory(int h) {
        iconHeight = h;
    }

    public RosterIconFactory() {
        iconHeight = 19; // OS X, because of Apple look'n feel constraints, ComboBox cannot be higher than this 19pixels
    }

    public ImageIcon getIcon(String id) {
        if (id == null) {
            return null;
        }
        RosterEntry re = Roster.getDefault().entryFromTitle(id);
        if (re == null) {
            return null;
        }
        return getIcon(re);
    }
    
    public ImageIcon getReversedIcon(String id) {
        if (id == null) {
            return null;
        }
        RosterEntry re = Roster.getDefault().entryFromTitle(id);
        if (re == null) {
            return null;
        }
        return getReversedIcon(re);
    }

    public ImageIcon getIcon(RosterEntry re) {
        if ((re == null) || (re.getIconPath() == null)) {
            return null;
        }

        ImageIcon icon = icons.get(re.getIconPath());
        if (icon == null) {
            icon = new ImageIcon(re.getIconPath(), re.getId());
            /* icon can not be null
             if (icon==null)
             return null;
             */
            icon.setImage(icon.getImage().getScaledInstance(-1, iconHeight, java.awt.Image.SCALE_FAST));
            icons.put(re.getIconPath(), icon);
        }
        return icon;
    }
    
    public ImageIcon getReversedIcon(RosterEntry re) {
        if ((re == null) || (re.getIconPath() == null)) {
            return null;
        }

        ImageIcon revicon = icons.get("rev_"+re.getIconPath());
        if (revicon == null) {
            ImageIcon icon = getIcon(re);
            if (icon==null) {
                return null;
            }
            // Flip the image horizontally
            AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
            tx.translate(-icon.getImage().getWidth(null), 0);
            AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
            BufferedImage bi = new BufferedImage( icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics g = bi.createGraphics();
            icon.paintIcon(null, g, 0,0);
            g.dispose();            
            revicon = new ImageIcon();
            revicon.setImage(op.filter( bi, null).getScaledInstance(-1, iconHeight, java.awt.Image.SCALE_FAST));
            icons.put("rev_"+re.getIconPath(), revicon);
        }
        return revicon;
    }
}
