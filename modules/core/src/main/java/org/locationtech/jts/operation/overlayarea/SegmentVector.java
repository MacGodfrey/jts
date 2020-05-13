/*
 * Copyright (c) 2020 Martin Davis
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.operation.overlayarea;

import org.locationtech.jts.geom.Coordinate;

/**
 * Functions to compute the partial area for a segment vector
 * starting from an intersection vertex or a contained vertex.
 * 
 * @author Martin Davis
 *
 */
class SegmentVector {

  public static double area2Term(Coordinate p0, Coordinate p1, boolean isNormalToRight) {
    return area2Term(p0.x, p0.y, p1.x, p1.y, isNormalToRight);
  }
  
  public static double area2Term(Coordinate v, Coordinate p0, Coordinate p1, boolean isNormalToRight) {
    return area2Term(v.x, v.y, p0.x, p0.y, p1.x, p1.y, isNormalToRight);
  }
  
  public static double area2Term(
      double x0, double y0, double x1, double y1, boolean isNormalToRight) {
    return area2Term(x0, y0, x0, y0, x1, y1, isNormalToRight);
  }

  public static double area2Term(
      double vx, double vy, double x0, double y0, double x1, double y1, boolean isNormalToRight) {

    double dx = x1 - x0;
    double dy = y1 - y0;
    double len2 = dx*dx + dy*dy;
    if (len2 <= 0) return 0;
    
    // unit vector in direction of edge
    double len = Math.sqrt(len2);
    double ux = dx / len;
    double uy = dy / len;
    
    // normal vector to edge
    double nx, ny;
    if (isNormalToRight) {
      nx = uy;
      ny = -ux;
    }
    else {
      nx = -uy;
      ny = ux;
    }
    
    double area2Term = (vx*ux + vy*uy) * (vx*nx + vy*ny); 
    //System.out.println(areaTerm);
    return area2Term;
  }

  
}