package game;

import java.io.Serializable;
import java.util.Objects;

/**
 * Biểu diễn một cạnh nối 2 điểm kề nhau trên lưới bàn cờ.
 * Không cho phép nối chéo.
 * @author Triệu Tuyên Kỳ
 */
public class Edge implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Dot dot1;
    private final Dot dot2;
    private boolean drawn;
    private Player drawnBy;

    public Edge(Dot d1, Dot d2) {
        if (d1 == null || d2 == null) {
            throw new IllegalArgumentException("Dot không được null");
        }
        // Chuẩn hóa thứ tự dot1, dot2 để tránh trùng lặp đảo chiều
        if (isBefore(d1, d2)) {
            this.dot1 = d1;
            this.dot2 = d2;
        } else {
            this.dot1 = d2;
            this.dot2 = d1;
        }
        this.drawn = false;
        this.drawnBy = null;
    }

    private static boolean isBefore(Dot a, Dot b) {
        if (a.getRow() != b.getRow()) {
            return a.getRow() < b.getRow();
        }
        return a.getCol() < b.getCol();
    }

    /**
     * Kiểm tra xem 2 điểm có thực sự kề nhau (khoảng cách 1 theo chiều ngang hoặc dọc) và không bị chéo hay không.
     */
    public boolean isAdjacent() {
        int dRow = Math.abs(dot1.getRow() - dot2.getRow());
        int dCol = Math.abs(dot1.getCol() - dot2.getCol());
        return (dRow == 1 && dCol == 0) || (dRow == 0 && dCol == 1);
    }

    public boolean isHorizontal() {
        return dot1.getRow() == dot2.getRow() && Math.abs(dot1.getCol() - dot2.getCol()) == 1;
    }

    public boolean isVertical() {
        return dot1.getCol() == dot2.getCol() && Math.abs(dot1.getRow() - dot2.getRow()) == 1;
    }

    public Dot getDot1() {
        return dot1;
    }

    public Dot getDot2() {
        return dot2;
    }

    public boolean isDrawn() {
        return drawn;
    }

    public void setDrawn(boolean drawn) {
        this.drawn = drawn;
    }

    public Player getDrawnBy() {
        return drawnBy;
    }

    public void setDrawnBy(Player drawnBy) {
        this.drawnBy = drawnBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Edge edge = (Edge) o;
        return Objects.equals(dot1, edge.dot1) && Objects.equals(dot2, edge.dot2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dot1, dot2);
    }

    @Override
    public String toString() {
        return "Edge[" + dot1 + " - " + dot2 + ", drawn=" + drawn + "]";
    }
}
