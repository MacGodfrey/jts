package org.locationtech.jts.operation.overlayarea;

import java.util.List;

import org.locationtech.jts.algorithm.LineIntersector;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.algorithm.RobustLineIntersector;
import org.locationtech.jts.algorithm.locate.IndexedPointInAreaLocator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.Location;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.ItemVisitor;
import org.locationtech.jts.index.kdtree.KdNode;
import org.locationtech.jts.index.kdtree.KdTree;
import org.locationtech.jts.index.strtree.STRtree;

public class OverlayArea {
  
  public static double intersectionArea(Geometry geom0, Geometry geom1) {
    OverlayArea area = new OverlayArea(geom0);
    return area.intersectionArea(geom1);
  }
  
  private static LineIntersector li = new RobustLineIntersector();
  
  private Geometry geom0;
  private IndexedPointInAreaLocator locator0;
  private STRtree indexSegs;
  private KdTree vertexIndex;

  public OverlayArea(Geometry geom) {
    this.geom0 = geom;
    locator0 = new IndexedPointInAreaLocator(geom);
    indexSegs = buildSegmentIndex(geom);
    vertexIndex = buildVertexIndex(geom);
  }
  
  public double intersectionArea(Geometry geom) {
    // TODO: for now assume poly is CW and has no holes
    
    double area = 0.0;
    
    area += areaForIntersections(geom);
    
    /**
     * If area is still zero then no segments intersect.
     * This means that either the geometries are disjoint, 
     * or one is inside the other.
     * This allows computing area more efficiently
     */
    if (area == 0.0) {
      area = areaForContained(geom, geom0.getEnvelopeInternal(), locator0);
      if (area != 0.0) return area;
      IndexedPointInAreaLocator locator1 = new IndexedPointInAreaLocator(geom);
      area = areaForContained(geom0, geom.getEnvelopeInternal(), locator1);
    }
    
    /**
     * geometries intersect, so add area for interior vertices
     */
    area += areaForInteriorVertices(geom, geom0.getEnvelopeInternal(), locator0);
    
    IndexedPointInAreaLocator locator1 = new IndexedPointInAreaLocator(geom);
    area += areaForInteriorVerticesIndexed(geom0, vertexIndex, geom.getEnvelopeInternal(), locator1);
    //area += areaForInteriorVertices(geom0, geom.getEnvelopeInternal(), locator1);
    
    return area;
  }

  private double areaForContained(Geometry geom, Envelope env, IndexedPointInAreaLocator locator) {
    Coordinate pt = geom.getCoordinate();
    if (! env.covers(pt)) return 0.0;
    if (Location.INTERIOR != locator.locate(pt)) return 0.0;
    return geom.getArea();
  }

  private double areaForIntersections(Geometry geomB) {
    double area = 0.0;
    CoordinateSequence seqB = getVertices(geomB);
    
    boolean isCCWB = Orientation.isCCW(seqB);
    
    // Compute rays for all intersections   
    for (int j = 0; j < seqB.size() - 1; j++) {
      Coordinate b0 = seqB.getCoordinate(j);
      Coordinate b1 = seqB.getCoordinate(j+1);
      if (isCCWB) {
        // flip segment orientation
        Coordinate temp = b0; b0 = b1; b1 = temp;
      }
      
      Envelope env = new Envelope(b0, b1);
      IntersectionVisitor intVisitor = new IntersectionVisitor(b0, b1);
      indexSegs.query(env, intVisitor);
      area += intVisitor.getArea();
    }
    return area;
  }

  class IntersectionVisitor implements ItemVisitor {
    double area = 0.0;
    private Coordinate b0;
    private Coordinate b1;
    
    IntersectionVisitor(Coordinate b0, Coordinate b1) {
      this.b0 = b0;
      this.b1 = b1;
    }
    
    double getArea() {
      return area;
    }
    
    public void visitItem(Object item) {
      LineSegment seg = (LineSegment) item;
      area += areaForIntersection(b0, b1, seg.p0, seg.p1);
    }
  }
  
  private static double areaForIntersection(Coordinate b0, Coordinate b1, Coordinate a0, Coordinate a1) {
    // TODO: can the intersection computation be optimized?
    li.computeIntersection(a0, a1, b0, b1);
    if (! li.hasIntersection()) return 0.0;
    
    /**
     * With both rings oriented CW (effectively)
     * There are two situations for segment intersections:
     * 
     * 1) A entering B, B exiting A => rays are IP-A1:R, IP-B0:L
     * 2) A exiting B, B entering A => rays are IP-A0:L, IP-B1:R
     * (where :L/R indicates result is to the Left or Right).
     * 
     * Use full edge to compute direction, for accuracy.
     */
    Coordinate intPt = li.getIntersection(0);
    
    boolean isAenteringB = Orientation.COUNTERCLOCKWISE == Orientation.index(a0, a1, b1);
    
    if ( isAenteringB ) {
      return EdgeVector.areaTerm(intPt, a0, a1, true)
        + EdgeVector.areaTerm(intPt, b1, b0, false);
    }
    else {
      return EdgeVector.areaTerm(intPt, a1, a0, false)
       + EdgeVector.areaTerm(intPt, b0, b1, true);
    }
  }
    
  private double areaForInteriorVertices(Geometry geom, Envelope env, IndexedPointInAreaLocator locator) {
    /**
     * Compute rays originating at vertices inside the intersection result
     * (i.e. A vertices inside B, and B vertices inside A)
     */
    double area = 0.0;
    CoordinateSequence seq = getVertices(geom);
    boolean isCW = ! Orientation.isCCW(seq);
    
    for (int i = 0; i < seq.size()-1; i++) {
      Coordinate v = seq.getCoordinate(i);
      // quick bounda check
      if (! env.contains(v)) continue;
      // is this vertex in interior of intersection result?
      if (Location.INTERIOR == locator.locate(v)) {
        Coordinate vPrev = i == 0 ? seq.getCoordinate(seq.size()-2) : seq.getCoordinate(i-1);
        Coordinate vNext = seq.getCoordinate(i+1);
        area += EdgeVector.areaTerm(v, vPrev, ! isCW)
            + EdgeVector.areaTerm(v, vNext, isCW);
      }
    }
    return area;
  }
  
  private double areaForInteriorVerticesIndexed(Geometry geom, KdTree vertexIndex, Envelope env, IndexedPointInAreaLocator locator) {
    /**
     * Compute rays originating at vertices inside the intersection result
     * (i.e. A vertices inside B, and B vertices inside A)
     */
    double area = 0.0;
    CoordinateSequence seq = getVertices(geom);
    boolean isCW = ! Orientation.isCCW(seq);
    
    List verts = vertexIndex.query(env);
    for (Object node : verts) {
      KdNode kdNode = (KdNode) node;
      int i = (Integer) kdNode.getData();
      
      Coordinate v = seq.getCoordinate(i);
      // quick bounda check
      //if (! env.contains(v)) continue;
      // is this vertex in interior of intersection result?
      if (Location.INTERIOR == locator.locate(v)) {
        Coordinate vPrev = i == 0 ? seq.getCoordinate(seq.size()-2) : seq.getCoordinate(i-1);
        Coordinate vNext = seq.getCoordinate(i+1);
        area += EdgeVector.areaTerm(v, vPrev, ! isCW)
            + EdgeVector.areaTerm(v, vNext, isCW);
      }
    }
    return area;
  }
  
  
  private CoordinateSequence getVertices(Geometry geom) {
    Polygon poly = (Polygon) geom;
    CoordinateSequence seq = poly.getExteriorRing().getCoordinateSequence();
    return seq;
  }
  
  private STRtree buildSegmentIndex(Geometry geom) {
    Coordinate[] coords = geom.getCoordinates();
    
    boolean isCCWA = Orientation.isCCW(coords);
    STRtree index = new STRtree();
    for (int i = 0; i < coords.length - 1; i++) {
      Coordinate a0 = coords[i];
      Coordinate a1 = coords[i+1];
      LineSegment seg = new LineSegment(a0, a1);
      if (isCCWA) {
        seg = new LineSegment(a1, a0);
      }
      Envelope env = new Envelope(a0, a1);
      index.insert(env, seg);
    }
    return index;
  }

  private KdTree buildVertexIndex(Geometry geom) {
    Coordinate[] coords = geom.getCoordinates();
    KdTree index = new KdTree();
    for (int i = 0; i < coords.length - 1; i++) {
      Coordinate p = coords[i];
      index.insert(p, i);
    }
    return index;
  }

}
