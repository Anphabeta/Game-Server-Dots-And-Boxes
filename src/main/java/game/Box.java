package game;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Biểu diễn một ô vuông được tạo bởi 4 cạnh (top, bottom, left, right) và 4 điểm.
 * @author Triệu Tuyên Kỳ
 */
public class Box implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int row;
    private final int col;
    private final Edge top;
    private final Edge bottom;
    private final Edge left;
    private final Edge right;
    private Player owner;

    public Box(int row, int col, Edge top, Edge bottom, Edge left, Edge right) {
        this.row = row;
        this.col = col;
        this.top = top;
        this.bottom = bottom;
        this.left = left;
        this.right = right;
        this.owner = null;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public Edge getTop() {
        return top;
    }

    public Edge getBottom() {
        return bottom;
    }

    public Edge getLeft() {
        return left;
    }

    public Edge getRight() {
        return right;
    }

    public Player getOwner() {
        return owner;
    }

    public void setOwner(Player owner) {
        this.owner = owner;
    }

    public boolean hasOwner() {
        return this.owner != null;
    }

    /**
     * Kiểm tra điều kiện ô vuông đã được khép kín đủ 4 cạnh hay chưa.
     */
    public boolean isCompleted() {
        return top.isDrawn() && bottom.isDrawn() && left.isDrawn() && right.isDrawn();
    }

    /**
     * Kiểm tra xem ô vuông này có chứa cạnh được truyền vào hay không.
     */
    public boolean containsEdge(Edge edge) {
        return top.equals(edge) || bottom.equals(edge) || left.equals(edge) || right.equals(edge);
    }

    public List<Edge> getEdges() {
        return Arrays.asList(top, bottom, left, right);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Box box = (Box) o;
        return row == box.row && col == box.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }

    @Override
    public String toString() {
        return "Box[(" + row + ", " + col + "), completed=" + isCompleted() + ", owner=" + (owner != null ? owner.getSymbol() : "none") + "]";
    }
}
