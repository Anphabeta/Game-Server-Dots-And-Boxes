package game;

import java.io.Serializable;
import java.util.*;

/**
 * Lớp trung tâm quản lý lưới bàn cờ Dots and Boxes.
 * Lưu danh sách điểm (Dot), cạnh (Edge) và ô (Box).
 * Cung cấp danh sách cạnh còn trống để hợp lệ hoá nước đi.
 * @author Triệu Tuyên Kỳ
 */
public class Board implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int boxRows;
    private final int boxCols;
    private final int dotRows;
    private final int dotCols;

    private final Dot[][] dots;
    private final Map<String, Edge> edgesMap; // Key: "r1,c1-r2,c2" chuẩn hóa
    private final Box[][] boxes;

    public Board(int boxRows, int boxCols) {
        if (boxRows <= 0 || boxCols <= 0) {
            throw new IllegalArgumentException("Kích thước bàn cờ phải lớn hơn 0");
        }
        this.boxRows = boxRows;
        this.boxCols = boxCols;
        this.dotRows = boxRows + 1;
        this.dotCols = boxCols + 1;

        this.dots = new Dot[dotRows][dotCols];
        this.edgesMap = new LinkedHashMap<>();
        this.boxes = new Box[boxRows][boxCols];

        initializeBoard();
    }

    private void initializeBoard() {
        // 1. Khởi tạo tất cả các điểm Dot
        for (int r = 0; r < dotRows; r++) {
            for (int c = 0; c < dotCols; c++) {
                dots[r][c] = new Dot(r, c);
            }
        }

        // 2. Khởi tạo tất cả các cạnh Edge ngang và dọc
        // Cạnh ngang
        for (int r = 0; r < dotRows; r++) {
            for (int c = 0; c < dotCols - 1; c++) {
                Edge hEdge = new Edge(dots[r][c], dots[r][c + 1]);
                edgesMap.put(getEdgeKey(dots[r][c], dots[r][c + 1]), hEdge);
            }
        }
        // Cạnh dọc
        for (int r = 0; r < dotRows - 1; r++) {
            for (int c = 0; c < dotCols; c++) {
                Edge vEdge = new Edge(dots[r][c], dots[r + 1][c]);
                edgesMap.put(getEdgeKey(dots[r][c], dots[r + 1][c]), vEdge);
            }
        }

        // 3. Khởi tạo các ô vuông Box liên kết với 4 cạnh
        for (int r = 0; r < boxRows; r++) {
            for (int c = 0; c < boxCols; c++) {
                Edge top = getEdge(dots[r][c], dots[r][c + 1]);
                Edge bottom = getEdge(dots[r + 1][c], dots[r + 1][c + 1]);
                Edge left = getEdge(dots[r][c], dots[r + 1][c]);
                Edge right = getEdge(dots[r][c + 1], dots[r + 1][c + 1]);

                boxes[r][c] = new Box(r, c, top, bottom, left, right);
            }
        }
    }

    public static String getEdgeKey(Dot d1, Dot d2) {
        Edge temp = new Edge(d1, d2);
        return temp.getDot1().getRow() + "," + temp.getDot1().getCol() + "-" +
               temp.getDot2().getRow() + "," + temp.getDot2().getCol();
    }

    public Dot getDot(int row, int col) {
        if (row < 0 || row >= dotRows || col < 0 || col >= dotCols) {
            return null;
        }
        return dots[row][col];
    }

    public Edge getEdge(Dot d1, Dot d2) {
        if (d1 == null || d2 == null) return null;
        return edgesMap.get(getEdgeKey(d1, d2));
    }

    public Edge getEdge(int r1, int c1, int r2, int c2) {
        Dot d1 = getDot(r1, c1);
        Dot d2 = getDot(r2, c2);
        return getEdge(d1, d2);
    }

    public Box getBox(int row, int col) {
        if (row < 0 || row >= boxRows || col < 0 || col >= boxCols) {
            return null;
        }
        return boxes[row][col];
    }

    /**
     * Lấy danh sách tất cả các cạnh còn trống (chưa được vẽ).
     */
    public List<Edge> getRemainingEdges() {
        List<Edge> remaining = new ArrayList<>();
        for (Edge edge : edgesMap.values()) {
            if (!edge.isDrawn()) {
                remaining.add(edge);
            }
        }
        return Collections.unmodifiableList(remaining);
    }

    /**
     * Tìm các ô vuông có chứa cạnh edge này (tối đa 2 ô).
     */
    public List<Box> getBoxesAdjacentTo(Edge edge) {
        List<Box> adjacent = new ArrayList<>();
        for (int r = 0; r < boxRows; r++) {
            for (int c = 0; c < boxCols; c++) {
                if (boxes[r][c].containsEdge(edge)) {
                    adjacent.add(boxes[r][c]);
                }
            }
        }
        return adjacent;
    }

    public boolean isFull() {
        return getRemainingEdges().isEmpty();
    }

    public int getBoxRows() {
        return boxRows;
    }

    public int getBoxCols() {
        return boxCols;
    }

    public int getDotRows() {
        return dotRows;
    }

    public int getDotCols() {
        return dotCols;
    }

    public int getTotalBoxes() {
        return boxRows * boxCols;
    }

    public int getTotalEdges() {
        return edgesMap.size();
    }

    public Collection<Edge> getAllEdges() {
        return edgesMap.values();
    }
}
