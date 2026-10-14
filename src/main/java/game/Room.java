package game;

import controller.UserRepresentative;
import dao.MatchDAO;
import dao.UserDAO;
import model.Match;
import model.User;
import model.UserHistoryMatch;
import request.Encoder;
import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Đại diện cho một phòng chơi ván cờ Dots and Boxes.
 * Đóng vai trò cầu nối giữa kết nối mạng (UserRepresentative) và logic ván cờ (GameEngine).
 */
public class Room implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int roomId;
    private final UserRepresentative player1Rep;
    private final UserRepresentative player2Rep;
    private final Player player1;
    private final Player player2;
    private final GameEngine gameEngine;
    private final int boxRows;
    private final int boxCols;
    private final long startTime;
    private volatile boolean finished;

    private transient UserDAO userDAO;
    private transient MatchDAO matchDAO;
    private transient Encoder encoder;
    private transient RoomManager roomManager;
    private int matchId = -1;

    public Room(int roomId, UserRepresentative player1Rep, UserRepresentative player2Rep, int boxRows, int boxCols) {
        if (player1Rep == null || player2Rep == null) {
            throw new IllegalArgumentException("Phòng chơi cần có đủ 2 người chơi");
        }
        this.roomId = roomId;
        this.player1Rep = player1Rep;
        this.player2Rep = player2Rep;
        this.boxRows = boxRows;
        this.boxCols = boxCols;
        this.startTime = System.currentTimeMillis();
        this.finished = false;

        // Khởi tạo Player logic tương ứng với 2 tài khoản người chơi
        this.player1 = new Player(player1Rep.user.getUserId(), player1Rep.user.getDisplayName(), "X");
        this.player2 = new Player(player2Rep.user.getUserId(), player2Rep.user.getDisplayName(), "O");

        this.gameEngine = new GameEngine(boxRows, boxCols, this.player1, this.player2);
        System.out.println("Room created: Room " + roomId);
    }

    public void setDAO(UserDAO userDAO, MatchDAO matchDAO) {
        this.userDAO = userDAO;
        this.matchDAO = matchDAO;
    }

    public void setEncoder(Encoder encoder) {
        this.encoder = encoder;
    }

    public void setRoomManager(RoomManager roomManager) {
        this.roomManager = roomManager;
    }

    public void setMatchId(int matchId) {
        this.matchId = matchId;
    }

    public int getMatchId() {
        return matchId;
    }

    /**
     * Xử lý một nước đi từ người chơi theo userId.
     */
    public synchronized MoveResult handleMove(int userId, int r1, int c1, int r2, int c2) {
        if (finished) {
            return MoveResult.fail("MATCH_ENDED");
        }

        if (userId != player1.getId() && userId != player2.getId()) {
            return MoveResult.fail("ROOM_NOT_BELONG");
        }

        MoveResult result = gameEngine.makeMove(userId, r1, c1, r2, c2);
        if (result.isGameOver()) {
            this.finished = true;
            finalizeMatch("GAME_FINISHED");
        }
        return result;
    }

    /**
     * Xử lý trường hợp một người chơi xin đầu hàng (Surrender).
     */
    public synchronized void handleResign(int userId) {
        if (finished) return;
        gameEngine.resign(userId);
        this.finished = true;
        finalizeMatch("SURRENDER");
    }

    /**
     * Xử lý khi một người chơi bị ngắt kết nối trong lúc trận đấu đang diễn ra.
     */
    public synchronized void handleDisconnect(int disconnectedUserId) {
        if (finished) return;
        gameEngine.resign(disconnectedUserId);
        this.finished = true;
        finalizeMatch("DISCONNECT");
    }

    /**
     * Kết thúc ván đấu: tính toán Elo/Ranking, ghi vào Database và dọn dẹp phòng.
     */
    private void finalizeMatch(String reason) {
        int p1Score = player1.getScore();
        int p2Score = player2.getScore();
        boolean isDraw = gameEngine.isDraw();
        Integer winnerId = gameEngine.getWinner() != null ? gameEngine.getWinner().getId() : null;

        // 1. Cập nhật Match trong Database (hoặc tạo mới nếu chưa có)
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (matchDAO != null) {
            if (matchId > 0) {
                Match match = matchDAO.findById(matchId);
                if (match != null) {
                    match.setPlayer1Score(p1Score);
                    match.setPlayer2Score(p2Score);
                    match.setWinnerId(winnerId);
                    match.setMatchStatus("finished");
                    match.setEndTime(now);
                    matchDAO.updateMatch(match);
                }
            } else {
                Match match = new Match(player1.getId(), player2.getId());
                match.setPlayer1Score(p1Score);
                match.setPlayer2Score(p2Score);
                match.setWinnerId(winnerId);
                match.setMatchStatus("finished");
                match.setStartTime(new Timestamp(startTime));
                match.setEndTime(now);
                this.matchId = matchDAO.createMatch(match);
            }

            // Ghi nhận lịch sử đấu (UserHistoryMatch) cho cả 2 người chơi
            if (this.matchId > 0) {
                String p1Result = isDraw ? "draw" : (winnerId != null && winnerId == player1.getId() ? "win" : "lose");
                String p2Result = isDraw ? "draw" : (winnerId != null && winnerId == player2.getId() ? "win" : "lose");

                matchDAO.addHistory(new UserHistoryMatch(player1.getId(), this.matchId, p1Result));
                matchDAO.addHistory(new UserHistoryMatch(player2.getId(), this.matchId, p2Result));
            }
        }

        // 2. Tính toán điểm xếp hạng (Elo Ranking) và cập nhật qua UserDAO
        updateRankings(winnerId, isDraw);

        // 3. Đưa trạng thái 2 người chơi trở lại ONLINE
        player1Rep.user.setStatus(User.STATUS.ONLINE);
        player2Rep.user.setStatus(User.STATUS.ONLINE);
        if (userDAO != null) {
            userDAO.updateUserStatus(player1.getId(), User.STATUS.ONLINE);
            userDAO.updateUserStatus(player2.getId(), User.STATUS.ONLINE);
        }

        // 4. Gửi frame GAME_OVER cho các client còn kết nối
        int winnerIdVal = winnerId != null ? winnerId : (isDraw ? 0 : -1);
        if (encoder != null) {
            encoder.notifyGameOver(player1Rep.state, winnerIdVal, p1Score, p2Score, isDraw, reason);
            encoder.notifyGameOver(player2Rep.state, winnerIdVal, p1Score, p2Score, isDraw, reason);
        }

        // 5. Dọn dẹp phòng khỏi RoomManager và giải phóng tham chiếu ở 2 UserRepresentative
        player1Rep.setCurrentRoom(null);
        player2Rep.setCurrentRoom(null);
        if (roomManager != null) {
            roomManager.removeRoom(roomId);
        }
        System.out.println(String.format("Match ended in Room %d. Reason: %s, Winner: %s",
                roomId, reason, winnerIdVal == 0 ? "DRAW" : String.valueOf(winnerIdVal)));
    }

    /**
     * Cập nhật điểm Elo ranking cho 2 người chơi (+25 cho thắng, -20 cho thua, 0 cho hòa).
     */
    private void updateRankings(Integer winnerId, boolean isDraw) {
        if (isDraw) return;
        if (winnerId == null) return;

        User u1 = player1Rep.user;
        User u2 = player2Rep.user;

        if (winnerId == u1.getUserId()) {
            u1.setRanking(u1.getRanking() + 25);
            u2.setRanking(Math.max(0, u2.getRanking() - 20));
        } else if (winnerId == u2.getUserId()) {
            u2.setRanking(u2.getRanking() + 25);
            u1.setRanking(Math.max(0, u1.getRanking() - 20));
        }

        if (userDAO != null) {
            userDAO.updateUserRanking(u1.getUserId(), u1.getRanking());
            userDAO.updateUserRanking(u2.getUserId(), u2.getRanking());
        }
    }

    /**
     * Gửi bản tin tới cả 2 người chơi trong phòng.
     */
    public void broadcast(String frame) {
        sendTo(player1Rep, frame);
        sendTo(player2Rep, frame);
    }

    /**
     * Gửi bản tin tới 1 người chơi cụ thể.
     */
    public void sendTo(UserRepresentative rep, String frame) {
        if (rep != null && rep.state != null && rep.state.isValid()) {
            rep.state.writeFrame(String.format("INGAME_MESSAGE,,%s" ,frame));
        }
    }

    /**
     * Lấy đại diện người chơi theo userId.
     */
    public UserRepresentative getRepByUserId(int userId) {
        if (player1Rep.user.getUserId() == userId) return player1Rep;
        if (player2Rep.user.getUserId() == userId) return player2Rep;
        return null;
    }

    /**
     * Lấy đối thủ của người chơi theo userId.
     */
    public UserRepresentative getOpponentRepByUserId(int userId) {
        if (player1Rep.user.getUserId() == userId) return player2Rep;
        if (player2Rep.user.getUserId() == userId) return player1Rep;
        return null;
    }

    public int getRoomId() {
        return roomId;
    }

    public UserRepresentative getPlayer1Rep() {
        return player1Rep;
    }

    public UserRepresentative getPlayer2Rep() {
        return player2Rep;
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public GameEngine getGameEngine() {
        return gameEngine;
    }

    public int getBoxRows() {
        return boxRows;
    }

    public int getBoxCols() {
        return boxCols;
    }

    public long getStartTime() {
        return startTime;
    }

    public boolean isFinished() {
        return finished;
    }
}
