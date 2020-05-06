package org.locationtech.jtstest.function;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.operation.overlayarea.GeometryArea;
import org.locationtech.jts.operation.overlayarea.OverlayArea;

public class OverlayAreaFunctions {
  public static double areaSingle(Geometry g) {
    return GeometryArea.area(g);
  }
  
  public static double intersectionArea(Geometry geom0, Geometry geom1) {
    return OverlayArea.intersectionArea(geom0, geom1);
  }
  
  static OverlayArea cacheIntArea = null;
  static Geometry cacheKey = null;
  
  public static double intersectionAreaPrep(Geometry geom0, Geometry geom1) {
    if (geom0 != cacheKey) {
      cacheKey = geom0;
      cacheIntArea = new OverlayArea(geom0);
    }
    return cacheIntArea.intersectionArea(geom1);
  }
  
  public static double intAreaOrig(Geometry geom0, Geometry geom1) {
    double intArea = geom0.intersection(geom1).getArea();
    return intArea;
  }
  
  public static double checkIntArea(Geometry geom0, Geometry geom1) {
    double intArea = intersectionArea(geom0, geom1);
    
    double intAreaStd = geom0.intersection(geom1).getArea();
    
    double diff = Math.abs(intArea - intAreaStd)/Math.max(intArea, intAreaStd);
    
    return diff;
  }
}
