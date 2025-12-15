package pathfinder;

import java.awt.Point;
import java.util.List;
import java.util.Set;


public interface AnimatablePathfinder {
    

    boolean isFinished();

    void nextStep();

    Set<Point> getOpenSet();   

    Set<Point> getClosedSet(); 

   
    List<Point> getPath();

    
    int getTotalCost();
    
   
    String getName();
}