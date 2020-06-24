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
 * Functions to compute the partial area term for an edge vector
 * starting at an intersection vertex or a contained vertex.
 * <p>
 * Uses mathematics derived from the work of William R. Franklin.
 * 
 * @author Martin Davis
 *
 */
class EdgeVector {

  public static double area2Term(Coordinate p0, Coordinate p1, boolean isInteriorToRight) {
    return area2Term(p0.x, p0.y, p0.x, p0.y, p1.x, p1.y, isInteriorToRight);
  }
  
  public static double area2Term(Coordinate v, Coordinate p0, Coordinate p1, boolean isInteriorToRight) {
    return area2Term(v.x, v.y, p0.x, p0.y, p1.x, p1.y, isInteriorToRight);
  }
  
  public static double area2Term(
      double x0, double y0, double x1, double y1, boolean isInteriorToRight) {
    return area2Term(x0, y0, x0, y0, x1, y1, isInteriorToRight);
  }

  /**
   * Computes the partial area (doubled) for the edge vector.
   * The partial area terms can be summed to determine twice the total
   * area of a geometry or an overlay.
   * <p>
   * The edge vector has origin (vx, vy), and direction vector (x0,y0)->(x1,y1).
   * The area term sign depends on whether the polygon interior lies to the right or left
   * of the vector. 
   * <p>
   * The value returned is twice the actual area term, to reduce arithmetic operations
   * over many evaluations.
   * 
   * @param vx the x ordinate of the edge origin point
   * @param vy the y ordinate of the edge origin point
   * @param x0 the x ordinate of the vector origin
   * @param y0 the y ordinate of the vector origin
   * @param x1 the x ordinate of the vector terminus
   * @param y1 the y ordinate of the vector terminus
   * @param isInteriorToRight whether the polygon interior lies to the right of the vector
   * @return the area term
   */
  public static double area2Term(
      double vx, double vy, double x0, double y0, double x1, double y1, boolean isInteriorToRight) {

    double dx = x1 - x0;
    double dy = y1 - y0;
    double len2 = dx*dx + dy*dy;
    if (len2 <= 0) return 0;
    
    // unit vector in direction of edge
    double len = Math.sqrt(len2);
    double ux = dx / len;
    double uy = dy / len;
    
    // normal vector to edge, pointing into polygon
    double nx, ny;
    if (isInteriorToRight) {
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