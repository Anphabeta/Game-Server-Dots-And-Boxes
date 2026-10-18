package game;

/**
 * Kiểm tra tính hợp lệ của nước đi.
 * Yêu cầu: 2 điểm nằm trong bàn chơi, kề nhau theo hàng/cột (không nối chéo), và cạnh chưa từng được vẽ.
 * @author Triệu Tuyên Kỳ
 */
public class MoveValidator {

    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Xác thực nước đi giữa 2 điểm d1 và d2 trên bàn cờ.
     */
    public ValidationResult validate(Board board, Dot d1, Dot d2) {
        if (board == null) {
            return new ValidationResult(false, "BOARD_NOT_EXIST");
        }
        if (d1 == null || d2 == null) {
            return new ValidationResult(false, "INVALID_DOT");
        }
        if (d1.equals(d2)) {
            return new ValidationResult(false, "OVERLAP_DOT");
        }

        // Kiểm tra xem 2 điểm có nằm trong phạm vi bàn cờ không
        if (d1.getRow() < 0 || d1.getRow() >= board.getDotRows() ||
            d1.getCol() < 0 || d1.getCol() >= board.getDotCols() ||
            d2.getRow() < 0 || d2.getRow() >= board.getDotRows() ||
            d2.getCol() < 0 || d2.getCol() >= board.getDotCols()) {
            return new ValidationResult(false, "INVALID_DOT");
        }

        Edge tempEdge = new Edge(d1, d2);
        if (!tempEdge.isAdjacent()) {
            return new ValidationResult(false, "INVALID_DOT");
        }

        Edge boardEdge = board.getEdge(d1, d2);
        if (boardEdge == null) {
            return new ValidationResult(false, "EDGE_NOT_EXIST");
        }

        if (boardEdge.isDrawn()) {
            return new ValidationResult(false, "EDGE_EXISTED");
        }

        return new ValidationResult(true, "VALID_MOVE");
    }

    public boolean isValidMove(Board board, Dot d1, Dot d2) {
        return validate(board, d1, d2).isValid();
    }
}
