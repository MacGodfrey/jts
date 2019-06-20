/*
 * Copyright (c) 2016 Vivid Solutions.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.locationtech.jts.operation.overlaysr;

import java.util.Comparator;

import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geomgraph.Quadrant;
import org.locationtech.jts.util.Assert;

/**
 * Represents a directed component of an edge in an {@link EdgeGraph}.
 * HalfEdges link vertices whose locations are defined by {@link Coordinate}s.
 * HalfEdges start at an <b>origin</b> vertex,
 * and terminate at a <b>destination</b> vertex.
 * HalfEdges always occur in symmetric pairs, with the {@link #sym()} method
 * giving access to the oppositely-oriented component.
 * HalfEdges and the methods on them form an edge algebra,
 * which can be used to traverse and query the topology
 * of the graph formed by the edges.
 * <p>
 * By design HalfEdges carry minimal information
 * about the actual usage of the graph they represent.
 * They can be subclassed to carry more information if required.
 * <p>
 * HalfEdges form a complete and consistent data structure by themselves,
 * but an {@link EdgeGraph} is useful to allow retrieving edges
 * by vertex and edge location, as well as ensuring 
 * edges are created and linked appropriately.
 * 
 * @author Martin Davis
 *
 */
public class HalfEdge {
  
  public static Comparator<HalfEdge> comparator() {
    return new Comparator<HalfEdge>() {
      @Override
      public int compare(HalfEdge e1, HalfEdge e2) {
        return e1.compareTo(e2);
      }
    };
  }
  
  /**
   * The node where this edge originates
   */
  private Coordinate orig;
  
  /**
   * The half-edge in the opposite direction
   */
  private HalfEdge sym;
  
  /**
   * The next edge CCW around the origin node, 
   * originating at the node.
   */
  private HalfEdge next;

  /**
   * Creates an edge originating from a given coordinate.
   * 
   * @param orig the origin coordinate
   */
  public HalfEdge(Coordinate orig) {
    this.orig = orig;
  }

  /**
   * Initialize a symmetric pair of halfedges.
   * Intended for use by {@link EdgeGraph} subclasses.
   * The edges are initialized to have each other 
   * as the {@link sym} edge, and to have {@link next} pointers
   * which point to edge other.
   * This effectively creates a graph containing a single edge.
   * 
   * @param e a halfedge
   * @return the initialized edge
   */
  protected void init(HalfEdge e)
  {
    // ensure only newly created edges can be initialized, to prevent information loss
    if (this.sym != null || e.sym != null
        || this.next != null || e.next != null)
      throw new IllegalStateException("Edges are already initialized");
    
    setSym(e);
    e.setSym(this);
    // set next ptrs for a single segment
    setNext(e);
    e.setNext(this);
  }
  
  /**
   * Gets the origin coordinate of this edge.
   * 
   * @return the origin coordinate
   */
  public Coordinate orig() { return orig; }
  
  /**
   * Gets the destination coordinate of this edge.
   * 
   * @return the destination coordinate
   */
  public Coordinate dest() { return sym.orig; }

  /**
   * Gets the symmetric pair edge of this edge.
   * 
   * @return the symmetric pair edge
   */
  public HalfEdge sym()
  { 
    return sym;
  }
  
  /**
   * Sets the sym edge.
   * 
   * @param e the sym edge to set
   */
  private void setSym(HalfEdge e) {
    sym = e;
  }

  /**
   * Gets the next edge CCW around the 
   * destination vertex of this edge,
   * with that vertex as origin.
   * If the vertex has degree 1 then this is the <b>sym</b> edge.
   * 
   * @return the next edge CCW around the dest vertex
   */
  public HalfEdge next()
  {
    return next;
  }
  
  /**
   * Returns the edge previous to this one
   * (with dest being the same as this orig).
   * 
   * @return the previous edge to this one
   */
  public HalfEdge prev() {
    return sym.next().sym;
  }

  public void setNext(HalfEdge e)
  {
    next = e;
  }
  
  /**
   * Gets the next edge CCW around the 
   * origin vertex of this edge,
   * with that vertex as origin.
   * If the vertex has degree 1 then this is the <b>sym</b> edge.
   * 
   * @return the next edge CCW around the origin vertex
   */
  public HalfEdge oNext() {
    return sym.next;
  }

  /**
   * Finds the edge starting at the origin of this edge
   * with the given dest vertex,
   * if any.
   * 
   * @param dest the dest vertex to search for
   * @return the edge with the required dest vertex, if it exists,
   * or null
   */
  public HalfEdge find(Coordinate dest) {
    HalfEdge oNext = this;
    do {
      if (oNext == null) return null;
      if (oNext.dest().equals2D(dest)) 
        return oNext;
      oNext = oNext.oNext();
    } while (oNext != this);
    return null;
  }

  /**
   * Tests whether this edge has the given orig and dest vertices.
   * 
   * @param p0 the origin vertex to test
   * @param p1 the destination vertex to test
   * @return true if the vertices are equal to the ones of this edge
   */
  public boolean equals(Coordinate p0, Coordinate p1) {
    return orig.equals2D(p0) && sym.orig.equals(p1);
  }
  
  /**
   * Inserts an edge
   * into the ring of edges around the origin vertex of this edge.
   * The inserted edge must have the same origin as this edge.
   * 
   * @param e the edge to insert
   */
  public void insert(HalfEdge e) {
    // if no other edge around origin
    if (oNext() == this) {
      // set linkage so ring is correct
      insertAfter(e);
      return;
    }
    
    // otherwise, find edge to insert after
    int ecmp = compareTo(e);
    HalfEdge ePrev = this;
    do {
      HalfEdge oNext = ePrev.oNext();
      int cmp = oNext.compareTo(e);
      if (cmp != ecmp || oNext == this) {
        ePrev.insertAfter(e);
        return;
      }
      ePrev = oNext;
    } while (ePrev != this);
    Assert.shouldNeverReachHere();
  }
  
  /**
   * Insert an edge with the same origin after this one.
   * Assumes that the inserted edge is in the correct
   * position around the ring.
   * 
   * @param e the edge to insert (with same origin)
   */
  private void insertAfter(HalfEdge e) {
    Assert.equals(orig, e.orig());
    HalfEdge save = oNext();
    sym.setNext(e);
    e.sym().setNext(save);
  }

  /**
   * Compares edges which originate at the same vertex
   * based on the angle they make at their origin vertex with the positive X-axis.
   * This allows sorting edges around their origin vertex in CCW order.
   */
  public int compareTo(Object obj)
  {
    HalfEdge e = (HalfEdge) obj;
    int comp = compareAngularDirection(e);
    return comp;
  }

  /**
   * Implements the total order relation:
   * <p>
   *    The angle of edge a is greater than the angle of edge b,
   *    where the angle of an edge is the angle made by 
   *    the first segment of the edge with the positive x-axis
   * <p>
   * When applied to a list of edges originating at the same point,
   * this produces a CCW ordering of the edges around the point.
   * <p>
   * Using the obvious algorithm of computing the angle is not robust,
   * since the angle calculation is susceptible to roundoff error.
   * A robust algorithm is:
   * <ul>
   * <li>First, compare the quadrants the edge vectors lie in.  
   * If the quadrants are different, 
   * it is trivial to determine which edge has a greater angle.
   * 
   * <li>if the vectors lie in the same quadrant, the 
   * {@link Orientation#computeOrientation(Coordinate, Coordinate, Coordinate)} function
   * can be used to determine the relative orientation of the vectors.
   * </ul>
   */
  public int compareAngularDirection(HalfEdge e)
  {
    Coordinate dir1 = directionPt();
    Coordinate dir2 = e.directionPt();
    
    // same vector
    if (dir1.equals2D(dir2))
      return 0;
    
    double quadrant = Quadrant.quadrant(orig, dir1);
    double quadrant2 = Quadrant.quadrant(orig, dir2);
    
    // if the vectors are in different quadrants, determining the ordering is trivial
    if (quadrant > quadrant2) return 1;
    if (quadrant < quadrant2) return -1;
    // vectors are in the same quadrant
    // Check relative orientation of direction vectors
    // this is > e if it is CCW of e
    return Orientation.index(e.orig, dir2, dir1);
  }

  protected Coordinate directionPt() {
    // default is to assume edges have only 2 vertices
    return dest();
  }
  
  /**
   * Computes a string representation of a HalfEdge.
   * 
   * @return a string representation
   */
  public String toString()
  {
    return "HE("+orig.x + " " + orig.y
        + ", "
        + sym.orig.x + " " + sym.orig.y
        + ")";
  }

  /**
   * Computes the degree of the origin vertex.
   * The degree is the number of edges
   * originating from the vertex.
   * 
   * @return the degree of the origin vertex
   */
  public int degree() {
    int degree = 0;
    HalfEdge e = this;
    do {
      degree++;
      e = e.oNext();
    } while (e != this);
    return degree;
  }

  /**
   * Finds the first node previous to this edge, if any.
   * If no such node exists (i.e. the edge is part of a ring)
   * then null is returned.
   * 
   * @return an edge originating at the node prior to this edge, if any,
   *   or null if no node exists
   */
  public HalfEdge prevNode() {
    HalfEdge e = this;
    while (e.degree() == 2) {
      e = e.prev();
      if (e == this)
        return null;
    }
    return e;
  }

}
