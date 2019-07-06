/*
 * Copyright (c) 2019 Martin Davis.
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

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Location;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.geomgraph.Position;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.jts.util.Assert;
import org.locationtech.jts.util.Debug;

/**
 * An overlay node is a "virtual" concept, 
 * which is represented by a single {@link OverlayEdge}
 * originating at the node coordinate. 
 * 
 * @author mdavis
 *
 */
public class OverlayNode {

  /**
   * Scan around node CCW and propagate side locations
   * until the labels of incident edges are fully populated.
   * 
   * @param e node to compute labelling for
   */
  public static void computeLabelling(OverlayEdge nodeEdge) {
    propagateAreaLabels(nodeEdge, 0);
    propagateAreaLabels(nodeEdge, 1);
  }

  /**
   * Scans around a node CCW, propagating the labels
   * for a given area geometry to all edges (and their sym)
   * with unknown locations for that geometry.
   * @param e2 
   * 
   * @param geomIndex the geometry to propagate locations for
   */
  private static void propagateAreaLabels(OverlayEdge nodeEdge, int geomIndex) {
    OverlayEdge eStart = findPropagationStartEdge(nodeEdge, geomIndex);
    // no labelled edge found, so nothing to propagate
    if ( eStart == null )
      return;
    
    // initialize currLoc to location of L side
    int currLoc = eStart.getLabel().getLocation(geomIndex, Position.LEFT);
    OverlayEdge e = eStart.oNextOE();

    Debug.println("\npropagateAreaLabels geomIndex = " + geomIndex + " : " + eStart);
    Debug.print("BEFORE: " + toString(eStart));
    
    do {
      OverlayLabel label = e.getLabel();
      /**
       * If location is unknown 
       * they are all set to current location
       */
      if ( ! label.hasLocation(geomIndex) ) {
        e.setLocationAreaBoth(geomIndex, currLoc);
      }
      else {
        /**
         *  Location is known, so update curr loc
         *  (which may change moving from R to L across the edge
         */
        int locRight = e.getLabel().getLocation(geomIndex, Position.RIGHT);
        if (locRight != currLoc) {
          Debug.println("side location conflict: edge R loc " 
        + Location.toLocationSymbol(locRight) + " <>  curr loc " + Location.toLocationSymbol(currLoc) 
        + " for " + e);
          throw new TopologyException("side location conflict", e.getCoordinate());
        }
        int locLeft = e.getLabel().getLocation(geomIndex, Position.LEFT);
        if (locLeft == Location.NONE) {
          Assert.shouldNeverReachHere("found single null side at " + e);
        }
        currLoc = locLeft;
      }
      e = e.oNextOE();
    } while (e != eStart);
    Debug.print("AFTER: " + toString(eStart));
  }

  /**
   * Finds a node edge which has a labelling for this geom.
   * @param e2 
   * 
   * @param geomIndex
   * @return labelled edge, or null if no edges are labelled
   */
  private static OverlayEdge findPropagationStartEdge(OverlayEdge nodeEdge, int geomIndex) {
    OverlayEdge eStart = nodeEdge;
    do {
      OverlayLabel label = eStart.getLabel();
      if (label.hasLocation(geomIndex)) {
        return eStart;
      }
      eStart = (OverlayEdge) eStart.oNext();
    } while (eStart != nodeEdge);
    return null;
  }
  
  private static final int STATE_FIND_INCOMING = 1;
  private static final int STATE_LINK_OUTGOING = 2;

  /**
   * Traverses the star of OverlayEdges 
   * orginating at this node
   * and links result edges together.
   * To link two edges, the <code>resNext</code> pointer 
   * for an <b>incoming</b> result edge
   * is set to the next <b>outgoing</b> result edge.
   * <p>
   * Edges are linked only if:
   * <ul>
   * <li>they belong to an area (i.e. they have sides)
   * <li>they are marked as being in the result
   * </ul>
   * <p>
   * Edges are linked in CCW order (the order they are stored).
   * This means that rings have their face on the Right
   * (in other words,
   * the topological location of the face is given by the RHS label of the DirectedEdge).
   * This produces rings with CW orientation.
   * <p>
   * PRECONDITIONS: 
   * - This edge is in the result
   * - This edge is not yet linked
   * - The edge and its sym are NOT both marked as being in the result
   */
  public static void linkResultAreaEdges(OverlayEdge nodeEdge)
  {
    Assert.isTrue(nodeEdge.isInResult(), "Attempt to link non-result edge");
    Assert.isTrue(! nodeEdge.symOE().isInResult(), "Found both half-edges in result");

    /**
     * Since the node edge is an out-edge, 
     * make it the last edge to be linked
     * by starting at the next edge.
     * The node edge cannot be an in-edge as well, 
     * but the next one may be the first in-edge.
     */
    OverlayEdge endOut = nodeEdge.oNextOE();
    OverlayEdge currOut = endOut;
Debug.println("\n------  Linking node result area edges");
Debug.println("BEFORE: " + toString(nodeEdge));
    int state = STATE_FIND_INCOMING;
    OverlayEdge currResultIn = null;
    do {
      /**
       * If an edge is linked this node has already been processed
       * so can skip further processing
       */
      if (currResultIn != null && currResultIn.isResultLinked())
        return;
      
      switch (state) {
      case STATE_FIND_INCOMING:
        OverlayEdge currIn = currOut.symOE();
        if (! currIn.isInResult()) break;
        currResultIn = currIn;
        state = STATE_LINK_OUTGOING;
        Debug.println("Found result in-edge:  " + currResultIn);
        break;
      case STATE_LINK_OUTGOING:
        if (! currOut.isInResult()) break;
        // link the in edge to the out edge
        currResultIn.setResultNext(currOut);
        state = STATE_FIND_INCOMING;
        Debug.println("Linked:  " + currResultIn + " -> " + currOut);
        break;
      }
      currOut = currOut.oNextOE();
    } while (currOut != endOut);
    Debug.println("AFTER: " + toString(nodeEdge));
    if (state == STATE_LINK_OUTGOING) {
//Debug.print(firstOut == null, this);
      throw new TopologyException("no outgoing edge found", nodeEdge.getCoordinate());
    }    
  }

  public static void mergeSymLabels(OverlayEdge nodeEdge) {
    Debug.println("\nnodeMergeSymLabels-------- ");
    Debug.println("BEFORE: " + toString(nodeEdge));
    OverlayEdge e = nodeEdge;
    do {
      e.mergeSymLabels();
      e = (OverlayEdge) e.oNext();
    } while (e != nodeEdge);
    Debug.println("AFTER: " + toString(nodeEdge));
  }
  
  public static String toString(OverlayEdge nodeEdge) {
    Coordinate orig = nodeEdge.orig();
    Coordinate dest = nodeEdge.dest();
    StringBuilder sb = new StringBuilder();
    sb.append("Node( "+WKTWriter.format(orig) + " )" + "\n");
    OverlayEdge e = nodeEdge;
    do {
      sb.append("  -> " + e);
      if (e.isResultLinked()) {
        sb.append(" Link: ");
        sb.append(e.getResultNext());
      }
      sb.append("\n");
      e = e.oNextOE();
    } while (e != nodeEdge);
    return sb.toString(); 
  }

  private static String toStringNodeEdge(OverlayEdge e) {
    return "  -> (" + WKTWriter.format(e.dest()) 
    + " " + e.getLabel() + " Sym: " + e.symOE().getLabel()
    + (e.isInResult() ? " Res" : "-") + "/" + (e.symOE().isInResult() ? " Res" : "-")
    ;
  }
}