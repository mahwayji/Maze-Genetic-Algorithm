import java.util.ArrayList;
import java.util.Scanner;
import javax.swing.SwingUtilities;

import maze.Maze;
import display.MazeAnimatorApp;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ArrayList<String> lines = new ArrayList<>();
        int index = 0;

        System.out.println("Please input the maze map (Start input):");

        if (sc.hasNextLine()) {
            lines.add(sc.nextLine());
        }

        do {
            if (!sc.hasNextLine())
                break;
            String line = sc.nextLine();
            lines.add(line);
            index++;
        } while (index < lines.size() && lines.get(index).compareTo(lines.get(0)) != 0);

        if (lines.size() <= 1) {
            System.out.println("Error: No maze input detected.");
            sc.close();
            return;
        }

        Maze maze = new Maze(lines);

        System.out.println("Map loaded successfully. Launching GUI...");
        SwingUtilities.invokeLater(() -> {
            new MazeAnimatorApp(maze);
        });

        sc.close();
    }
}
