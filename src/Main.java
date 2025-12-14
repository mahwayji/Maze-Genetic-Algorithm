import java.util.ArrayList;
import java.util.Scanner;
import pathfinder.*;
import maze.Maze;

public class Main {
  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    ArrayList<String> lines = new ArrayList<>();
    int index = 0;

    // get first line
    lines.add(sc.nextLine());

    // input map
    do {
      lines.add(sc.nextLine());
      index++;
    } while (lines.get(index).compareTo(lines.get(0)) != 0);

    Maze maze = new Maze(lines);

    System.out.printf("\nDefault Map : \n");
    maze.showMaze(new ArrayList<>());

  pathfinderContext solver = new pathfinderContext(null);

    System.out.printf("\nGenetic Algorithm Map: ");
    solver.setStretegy(new GeneticAlgorithm());
    int GAResult = solver.execute(maze);
    System.out.printf("->Genetic Algorithm Result: %d\n", GAResult);


    System.out.printf("\nGreedy Algorithm Map : ");
    solver.setStretegy(new GreedyBestFirst());
    int GreedyResult = solver.execute(maze);
    System.out.printf("->GreedyResult: %d\n", GreedyResult);

    System.out.printf("\nDijkstra Map : ");
    solver.setStretegy(new Dijkstra());
    int dijkstraResult = solver.execute(maze);
    System.out.printf("->dijkstraResult: %d\n", dijkstraResult);

    System.out.printf("\nA-Star Map : ");
    solver.setStretegy(new A_Star());
    int AStarResult = solver.execute(maze);
    System.out.printf("->AStarResult: %d\n", AStarResult);

    sc.close();
  }
}
