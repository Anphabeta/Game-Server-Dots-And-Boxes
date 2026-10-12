package game;

import java.io.Serializable;
import java.util.List;

/**
 * Lớp điều phối chính của Logic Game (Trọng tài ván đấu).
 * Quản lý Bàn cờ, 2 Người chơi, Lượt đánh, xác thực nước đi, tính điểm và phân định thắng thua.
 * @author Triệu Tuyên Kỳ
 */
public class GameEngine implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Board board;
    private final Player player1;
    private final Player player2;
    private final MoveValidator moveValidator;
    private final BoxDetector boxDetector;

    private Player currentTurn;
    private boolean gameOver;
    private Player winner;
    private boolean draw;

    public GameEngine(int boxRows, int boxCols, Player player1, Player player2) {
        if (player1 == null || player2 == null) {
            throw new IllegalArgumentException("Cần đủ 2 người chơi");
        }
        this.board = new Board(boxRows, boxCols);
        this.player1 = player1;
        this.player2 = player2;
        this.moveValidator = new MoveValidator();
        this.boxDetector = new BoxDetector();

        // Mặc định player1 đi trước
        this.currentTurn = player1;
        this.gameOver = false;
        this.winner = null;
        this.draw = false;
    }

    /**
     * Xử lý một nước đi từ người chơi.
     * @param playerId ID của người chơi thực hiện nước đi
     * @param r1 Tọa độ hàng điểm 1
     * @param c1 Tọa độ cột điểm 1
     * @param r2 Tọa độ hàng điểm 2
     * @param c2 Tọa độ cột điểm 2
     * @return MoveResult kết quả chi tiết của nước đi
     */
    public synchronized MoveResult makeMove(int playerId, int r1, int c1, int r2, int c2) {
        if (gameOver) {
            return MoveResult.fail("Ván đấu đã kết thúc");
        }

        // 1. Kiểm tra có đúng lượt của người chơi hay không
        if (currentTurn.getId() != playerId) {
            return MoveResult.fail("Chưa đến lượt của bạn");
        }

        Dot d1 = board.getDot(r1, c1);
        Dot d2 = board.getDot(r2, c2);

        // 2. Xác thực nước đi thông qua MoveValidator
        MoveValidator.ValidationResult validation = moveValidator.validate(board, d1, d2);
        if (!validation.isValid()) {
            return MoveResult.fail(validation.getMessage());
        }

        // 3. Thực hiện vẽ cạnh
        Edge edge = board.getEdge(d1, d2);
        edge.setDrawn(true);
        edge.setDrawnBy(currentTurn);

        // 4. Phát hiện ô vuông khép kín thông qua BoxDetector
        List<Box> completedBoxes = boxDetector.detectAndClaimCompletedBoxes(board, edge, currentTurn);

        Player playedPlayer = currentTurn;
        boolean hasBonusTurn = false;

        if (!completedBoxes.isEmpty()) {
            // Ăn được ô vuông -> cộng điểm và ĐƯỢC ĐÁNH TIẾP (giữ nguyên lượt)
            playedPlayer.addScore(completedBoxes.size());
            hasBonusTurn = true;
        } else {
            // Không ăn được ô nào -> chuyển lượt cho đối thủ
            switchTurn();
        }

        // 5. Kiểm tra kết thúc ván đấu khi hết cạnh
        checkGameStatus();

        return new MoveResult.Builder()
                .success(true)
                .message("Nước đi hợp lệ")
                .edge(edge)
                .completedBoxes(completedBoxes)
                .playedBy(playedPlayer)
                .nextTurn(gameOver ? null : currentTurn)
                .bonusTurn(hasBonusTurn)
                .gameOver(gameOver)
                .winner(winner)
                .draw(draw)
                .build();
    }

    private void switchTurn() {
        if (currentTurn.equals(player1)) {
            currentTurn = player2;
        } else {
            currentTurn = player1;
        }
    }

    private void checkGameStatus() {
        if (board.isFull()) {
            this.gameOver = true;
            if (player1.getScore() > player2.getScore()) {
                this.winner = player1;
                this.draw = false;
            } else if (player2.getScore() > player1.getScore()) {
                this.winner = player2;
                this.draw = false;
            } else {
                this.winner = null;
                this.draw = true; // Hòa
            }
        }
    }

    /**
     * Cho phép xử lý trường hợp một người chơi đầu hàng hoặc mất kết nối.
     */
    public synchronized void resign(int resignedPlayerId) {
        if (gameOver) return;
        this.gameOver = true;
        this.draw = false;
        if (player1.getId() == resignedPlayerId) {
            this.winner = player2;
        } else if (player2.getId() == resignedPlayerId) {
            this.winner = player1;
        }
    }

    public Board getBoard() {
        return board;
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public Player getCurrentTurn() {
        return currentTurn;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public Player getWinner() {
        return winner;
    }

    public boolean isDraw() {
        return draw;
    }

    public MoveValidator getMoveValidator() {
        return moveValidator;
    }

    public BoxDetector getBoxDetector() {
        return boxDetector;
    }
}
