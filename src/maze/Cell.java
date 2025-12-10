package maze;

public class Cell {
    public CellType type;
    public int value;

    public Cell(CellType type) {
        this.type = type;
    }

    public Cell(CellType type, int value) {
        this.type = type;
        this.value = value;
    }
}