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

import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

/** 
 * Computes the area of a geometry using the {@link SegmentVector} summing
 * approach. 
 * This provides a validation of the correctness of the OverlayArea approach.
 * 
 * @author Martin Davis
 *
 */
public class GeometryArea {
  
  public static double area(Geometry geom) {
    GeometryArea area = new GeometryArea(geom);
    return area.getArea();
  }
  
  private Geometry geom;

  public GeometryArea(Geometry geom) {
    this.geom = geom;
  }
  
  public double getArea() {
    Polygon poly = (Polygon) geom;
    // TODO: handle hole rings, multiPolygons
    CoordinateSequence seq = poly.getExteriorRing().getCoordinateSequence();
    boolean isCW = ! Orientation.isCCW(seq);
    
    // scan every segment
    double area = 0;
    for (int i = 1; i < seq.size(); i++) {
      int i0 = i - 1;
      int i1 = i;
      area += SegmentVector.area2Term(seq.getX(i0), seq.getY(i0),
          seq.getX(i1), seq.getY(i1), isCW)
          + SegmentVector.area2Term(seq.getX(i1), seq.getY(i1),
          seq.getX(i0), seq.getY(i0), ! isCW);
    }
    return area / 2;
  }
}
