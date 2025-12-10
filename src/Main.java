import java.util.ArrayList;
import java.util.Scanner;

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

      maze.showMaze();

      sc.close();
    }
}
