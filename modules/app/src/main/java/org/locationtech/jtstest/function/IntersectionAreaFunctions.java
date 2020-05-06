package org.locationtech.jtstest.function;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.operation.intarea.GeometryArea;
import org.locationtech.jts.operation.intarea.IntersectionArea;

public class IntersectionAreaFunctions {
  public static double areaSingle(Geometry g) {
    return GeometryArea.area(g);
  }
  
  public static double area(Geometry geom0, Geometry geom1) {
    return IntersectionArea.area(geom0, geom1);
  }
  
  static IntersectionArea cacheIntArea = null;
  static Geometry cacheKey = null;
  
  public static double areaPrep(Geometry geom0, Geometry geom1) {
    if (geom0 != cacheKey) {
      cacheKey = geom0;
      cacheIntArea = new IntersectionArea(geom0);
    }
    return cacheIntArea.area(geom1);
  }
  
  public static double areaOrig(Geometry geom0, Geometry geom1) {
    double intArea = geom0.intersection(geom1).getArea();
    return intArea;
  }
  
  public static double checkArea(Geometry geom0, Geometry geom1) {
    double intArea = area(geom0, geom1);
    
    double intAreaStd = geom0.intersection(geom1).getArea();
    
    double diff = Math.abs(intArea - intAreaStd)/Math.max(intArea, intAreaStd);
    
    return diff;
  }
}
