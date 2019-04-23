package org.locationtech.jts.index.rtree;

import java.util.ArrayList;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.strtree.Boundable;
import org.locationtech.jts.util.Assert;

public class InternalNode implements Node {

  private ArrayList children;
  private Envelope bounds = null;
  private int level;

  /**
   * Returns 0 if this node is a leaf, 1 if a parent of a leaf, and so on; the
   * root node will have the highest level
   * 
   * @return the node level
   */
  public int getLevel() {
    return level;
  }

  /**
   * Gets the count of the {@link Boundable}s at this node.
   * 
   * @return the count of boundables at this node
   */
  public int size()
  {
    return 1;
  }

  /**
   * Tests whether there are any {@link Boundable}s at this node.
   * 
   * @return true if there are boundables at this node
   */
  public boolean isEmpty()
  {
    return item != null;
  }
  
  /**
   * Adds either an AbstractNode, or if this is a leaf node, a data object
   * (wrapped in an ItemBoundable)
   * 
   * @param childBoundable the child to add
   */
  public void addChild(Node child) {
    children.add(child);
  }

}
