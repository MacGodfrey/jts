package org.locationtech.jts.operation.overlayarea;

import org.locationtech.jts.geom.Coordinate;

class EdgeVector {

  public static double areaTerm(Coordinate p0, Coordinate p1, boolean isNormalToRight) {
    return areaTerm(p0.x, p0.y, p1.x, p1.y, isNormalToRight);
  }
  
  public static double areaTerm(Coordinate v, Coordinate p0, Coordinate p1, boolean isNormalToRight) {
    return areaTerm(v.x, v.y, p0.x, p0.y, p1.x, p1.y, isNormalToRight);
  }
  
  public static double areaTerm(
      double x0, double y0, double x1, double y1, boolean isNormalToRight) {
    return areaTerm(x0, y0, x0, y0, x1, y1, isNormalToRight);
  }

  public static double areaTerm(
      double vx, double vy, double x0, double y0, double x1, double y1, boolean isNormalToRight) {

    double dx = x1 - x0;
    double dy = y1 - y0;
    double len2 = dx*dx + dy*dy;
    if (len2 <= 0) return 0;
    
    double len = Math.sqrt(len2);
    double ux = dx / len;
    double uy = dy / len;
    // normal vector pointing to R of unit
    // (assumes CW ring)
    double nx, ny;
    if (isNormalToRight) {
      nx = uy;
      ny = -ux;
    }
    else {
      nx = -uy;
      ny = ux;
    }
    
    double areaTerm = 0.5 * (vx*ux + vy*uy) * (vx*nx + vy*ny); 
    //System.out.println(areaTerm);
    return areaTerm;
  }

  
}