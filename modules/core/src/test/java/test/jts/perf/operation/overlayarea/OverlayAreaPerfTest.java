package test.jts.perf.operation.overlayarea;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.util.SineStarFactory;
import org.locationtech.jts.operation.overlayarea.OverlayArea;

import test.jts.perf.PerformanceTestCase;
import test.jts.perf.PerformanceTestRunner;

public class OverlayAreaPerfTest extends PerformanceTestCase
{
  public static void main(String args[]) {
    PerformanceTestRunner.run(OverlayAreaPerfTest.class);
  }
  boolean verbose = true;
  private Geometry star1;
  private Geometry star2;
  
  public OverlayAreaPerfTest(String name) {
    super(name);
    setRunSize(new int[] { 100, 1000, 2000, 10000 });
    setRunIterations(1);
  }

  public void startRun(int size)
  {
    System.out.println("\n---  Running with size " + size + "  -----------");
    iter = 0;
    star1 = createSineStar(size, 0);
    star2 = createSineStar(size, 10);
  }
  
  private int iter = 0;
  
  public void runIntersectionArea()
  {
    //System.out.println("Test 1 : Iter # " + iter++);
    double area = OverlayArea.intersectionArea(star1, star2);
    System.out.println(">>> IntersectionArea = " + area);
  }
  
  public void runOverlayArea()
  {
    double area = star1.intersection(star2).getArea();
    System.out.println(">>> Overlay area = " + area);
  }
  
  Geometry createSineStar(int nPts, double offset)
  {
    SineStarFactory gsf = new SineStarFactory();
    gsf.setCentre(new Coordinate(0, offset));
    gsf.setSize(100);
    gsf.setNumPoints(nPts);
    
    Geometry g = gsf.createSineStar();
    
    return g;
  }
  
  public static Geometry grid(Geometry g, int nCells)
  {
    Envelope env = g.getEnvelopeInternal();
    GeometryFactory geomFact = g.getFactory();
    
    int nCellsOnSideY = (int) Math.sqrt(nCells);
    int nCellsOnSideX = nCells / nCellsOnSideY;
    
    // alternate: make square cells, with varying grid width/height
    //double extent = env.minExtent();
    //double nCellsOnSide = Math.max(nCellsOnSideY, nCellsOnSideX);
    
    double cellSizeX = env.getWidth() / nCellsOnSideX;
    double cellSizeY = env.getHeight() / nCellsOnSideY;
    
    List geoms = new ArrayList(); 

    for (int i = 0; i < nCellsOnSideX; i++) {
      for (int j = 0; j < nCellsOnSideY; j++) {
        double x = env.getMinX() + i * cellSizeX;
        double y = env.getMinY() + j * cellSizeY;
        double x2 = env.getMinX() + (i + 1) * cellSizeX;
        double y2 = env.getMinY() + (j + 1) * cellSizeY;
      
        Envelope cellEnv = new Envelope(x, x2, y, y2);
        geoms.add(geomFact.toGeometry(cellEnv));
      }
    }
    return geomFact.buildGeometry(geoms);
  }
}
