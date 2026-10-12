package game;

import java.io.Serializable;
import java.util.Objects;

/**
 * Biểu diễn một điểm giao trên lưới bàn cờ Dots and Boxes với tọa độ (row, col).
 * @author Triệu Tuyên Kỳ
 */
public class Dot implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int row;
    private final int col;

    public Dot(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dot dot = (Dot) o;
        return row == dot.row && col == dot.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }

    @Override
    public String toString() {
        return "(" + row + ", " + col + ")";
    }
}
