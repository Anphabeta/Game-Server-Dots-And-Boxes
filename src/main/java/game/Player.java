package game;

import java.io.Serializable;
import java.util.Objects;

/**
 * Lưu thông tin người chơi trong ván cờ (ID, biểu tượng/tên, điểm số hiện tại).
 * @author Triệu Tuyên Kỳ
 */
public class Player implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final String name;
    private final String symbol; // "X" hoặc "O"
    private int score;

    public Player(int id, String name, String symbol) {
        this.id = id;
        this.name = name;
        this.symbol = symbol;
        this.score = 0;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getScore() {
        return score;
    }

    public void addScore(int points) {
        this.score += points;
    }

    public void resetScore() {
        this.score = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return id == player.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Player{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", symbol='" + symbol + '\'' +
                ", score=" + score +
                '}';
    }
}
