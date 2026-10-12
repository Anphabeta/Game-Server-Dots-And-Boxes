package game;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Đối tượng kết quả trả về sau mỗi nước đi, dùng để ServerController gửi phản hồi về cho Client.
 * @author Triệu Tuyên Kỳ
 */
public class MoveResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final boolean success;
    private final String message;
    private final Edge edge;
    private final List<Box> completedBoxes;
    private final Player playedBy;
    private final Player nextTurn;
    private final boolean bonusTurn;
    private final boolean gameOver;
    private final Player winner;
    private final boolean draw;

    private MoveResult(Builder builder) {
        this.success = builder.success;
        this.message = builder.message;
        this.edge = builder.edge;
        this.completedBoxes = builder.completedBoxes != null ? builder.completedBoxes : Collections.emptyList();
        this.playedBy = builder.playedBy;
        this.nextTurn = builder.nextTurn;
        this.bonusTurn = builder.bonusTurn;
        this.gameOver = builder.gameOver;
        this.winner = builder.winner;
        this.draw = builder.draw;
    }

    public static MoveResult fail(String message) {
        return new Builder().success(false).message(message).build();
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Edge getEdge() {
        return edge;
    }

    public List<Box> getCompletedBoxes() {
        return completedBoxes;
    }

    public Player getPlayedBy() {
        return playedBy;
    }

    public Player getNextTurn() {
        return nextTurn;
    }

    public boolean hasBonusTurn() {
        return bonusTurn;
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

    public static class Builder {
        private boolean success = true;
        private String message = "Thành công";
        private Edge edge;
        private List<Box> completedBoxes;
        private Player playedBy;
        private Player nextTurn;
        private boolean bonusTurn = false;
        private boolean gameOver = false;
        private Player winner;
        private boolean draw = false;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder edge(Edge edge) {
            this.edge = edge;
            return this;
        }

        public Builder completedBoxes(List<Box> completedBoxes) {
            this.completedBoxes = completedBoxes;
            return this;
        }

        public Builder playedBy(Player playedBy) {
            this.playedBy = playedBy;
            return this;
        }

        public Builder nextTurn(Player nextTurn) {
            this.nextTurn = nextTurn;
            return this;
        }

        public Builder bonusTurn(boolean bonusTurn) {
            this.bonusTurn = bonusTurn;
            return this;
        }

        public Builder gameOver(boolean gameOver) {
            this.gameOver = gameOver;
            return this;
        }

        public Builder winner(Player winner) {
            this.winner = winner;
            return this;
        }

        public Builder draw(boolean draw) {
            this.draw = draw;
            return this;
        }

        public MoveResult build() {
            return new MoveResult(this);
        }
    }

    @Override
    public String toString() {
        return "MoveResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", edge=" + edge +
                ", completedBoxes=" + completedBoxes.size() +
                ", playedBy=" + (playedBy != null ? playedBy.getName() : "null") +
                ", nextTurn=" + (nextTurn != null ? nextTurn.getName() : "null") +
                ", bonusTurn=" + bonusTurn +
                ", gameOver=" + gameOver +
                ", winner=" + (winner != null ? winner.getName() : "null") +
                ", draw=" + draw +
                '}';
    }
}
