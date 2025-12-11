import java.util.ArrayList;
import java.util.Scanner;
import pathfinder.*;
import maze.Maze;
public class Main {
    public static void main(String[] args){
      Scanner sc =new Scanner(System.in);
      ArrayList<String> lines = new ArrayList<>();
      int index = 0;

      // get first line 
      lines.add(sc.nextLine());

      //input map
      do {
        lines.add(sc.nextLine());
        index++;
      } while(lines.get(index).compareTo(lines.get(0)) != 0);

      Maze maze = new Maze(lines);

      System.out.printf("\nDefault Map : \n");
      maze.showMaze(new ArrayList<>());

      System.out.printf("\nDijkstra Map : ");
      pathfinderContext pathfinder_DijkStra = new pathfinderContext(new Dijkstra());
      int dijkstraResult = pathfinder_DijkStra.execute(maze);
      System.out.printf("->dijkstraResult: %d",dijkstraResult);

      System.out.printf("\nA-Star Map : ");
      pathfinderContext pathfinder_A_Star = new pathfinderContext(new A_Star());
      int AStarResult = pathfinder_A_Star.execute(maze);
      System.out.printf("->AStarResult: %d",AStarResult);

      sc.close();
    }
}
