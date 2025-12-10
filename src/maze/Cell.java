package maze;
public class Cell {
    CellType type;
    int value;

    public Cell(CellType type){
        this.type = type;
    }

    public Cell(CellType type, int value){
        this.type = type;
        this.value = value;
    }
}

enum CellType {
    WALL,
    NUMBER,
    START,
    GOAL
}