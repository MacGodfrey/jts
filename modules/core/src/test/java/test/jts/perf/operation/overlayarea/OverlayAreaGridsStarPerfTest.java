package test.jts.perf.operation.overlayarea;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.util.SineStarFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.operation.overlayarea.OverlayArea;

import test.jts.perf.PerformanceTestCase;
import test.jts.perf.PerformanceTestRunner;
import test.jts.util.IOUtil;

public class OverlayAreaGridsStarPerfTest extends PerformanceTestCase
{
  public static void main(String args[]) {
    PerformanceTestRunner.run(OverlayAreaGridsStarPerfTest.class);
  }
  boolean verbose = true;
  private Geometry geom;
  private Geometry grid;
  
  public OverlayAreaGridsStarPerfTest(String name) {
    super(name);
    setRunSize(new int[] { 100, 1000, 2000, 10_000, 20_000, 40_000, 1000_000 });
    setRunIterations(1);
  }

  public void startRun(int size) throws IOException, ParseException
  {
    iter = 0;
    geom = createSineStar(10_000, 0);
    //geom = (Geometry) IOUtil.readWKTFile("D:/proj/jts/testing/intersectionarea/dvg_nw.wkt").toArray()[0];
    grid = grid(geom, size);
    
    System.out.printf("\n---  Running with Polygon size %d, grid # = %d -------------\n",
        geom.getNumPoints(), grid.getNumGeometries());
  }
 
  private int iter = 0;
  
  public void runOverlayArea()
  {
    double area = 0.0;
    OverlayArea intArea = new OverlayArea(geom);
    //System.out.println("Test 1 : Iter # " + iter++);
    for (int i = 0; i < grid.getNumGeometries(); i++) {
      Geometry cell = grid.getGeometryN(i);
      area += intArea.intersectionArea(cell);
    }
    System.out.println(">>> OverlayArea = " + area);
  }
  
  public void runFullIntersection()
  {
    double area = 0.0;
    //System.out.println("Test 1 : Iter # " + iter++);
    for (int i = 0; i < grid.getNumGeometries(); i++) {
      Geometry cell = grid.getGeometryN(i);
      area += geom.intersection(cell).getArea();
    }
    System.out.println(">>> Full Intersection area = " + area);
  }
  
  public static Geometry createSineStar(int nPts, double offset)
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
