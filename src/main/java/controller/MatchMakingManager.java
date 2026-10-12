package controller;

import java.util.concurrent.ConcurrentLinkedQueue;
import model.User;
import request.Encoder;

public class MatchMakingManager {
    private final ConcurrentLinkedQueue<UserRepresentative> queue = new ConcurrentLinkedQueue<>();
    private Encoder encoder;
    private static int roomCounter = 1;

    public void setEncoder(Encoder encoder) {
        this.encoder = encoder;
    }

    /**
     * Thêm người chơi vào hàng đợi tìm trận nhanh.
     * @param player Đại diện của người chơi
     * @return true nếu thêm thành công, false nếu đã có trong hàng đợi
     */
    public synchronized boolean addPlayer(UserRepresentative player) {
        if (player == null) {
            return false;
        }

        if (queue.contains(player)) {
            return false;
        }

        queue.add(player);
        System.out.println(String.format("Player %s (Session %d) joined quick match queue. Current queue size: %d",
                player.user.getUsername(), player.sessionId, queue.size()));

        tryMatch();
        return true;
    }

    /**
     * Rút người chơi khỏi hàng đợi tìm trận (do hủy tìm trận hoặc disconnect).
     * @param player Đại diện của người chơi
     * @return true nếu người chơi có trong hàng đợi và đã được xóa
     */
    public synchronized boolean removePlayer(UserRepresentative player) {
        if (player == null) {
            return false;
        }

        boolean removed = queue.remove(player);
        if (removed) {
            System.out.println(String.format("Player %s (Session %d) removed from quick match queue. Remaining: %d",
                    player.user.getUsername(), player.sessionId, queue.size()));
            if (User.STATUS.QUEUING.equalsIgnoreCase(player.user.getStatus())) {
                player.user.setStatus(User.STATUS.ONLINE);
            }
        }
        return removed;
    }

    /**
     * Thử ghép cặp nếu có ít nhất 2 người trong hàng đợi.
     */
    private synchronized void tryMatch() {
        while (queue.size() >= 2) {
            UserRepresentative player1 = queue.poll();
            UserRepresentative player2 = queue.poll();

            // Kiểm tra tính hợp lệ (còn kết nối) của player 1
            if (player1 == null || !isPlayerActive(player1)) {
                if (player2 != null && isPlayerActive(player2)) {
                    queue.add(player2); // Đưa player 2 trở lại queue
                }
                continue;
            }

            // Kiểm tra tính hợp lệ của player 2
            if (player2 == null || !isPlayerActive(player2)) {
                if (isPlayerActive(player1)) {
                    queue.add(player1); // Đưa player 1 trở lại queue
                }
                continue;
            }

            // Cả hai người đều hợp lệ -> Khởi tạo trận đấu
            createMatch(player1, player2);
        }
    }

    /**
     * Kiểm tra kết nối của người chơi còn hoạt động hay không.
     */
    private boolean isPlayerActive(UserRepresentative player) {
        return player.state != null && player.state.isValid();
    }

    /**
     * Xử lý khi ghép thành công 2 người chơi.
     */
    private void createMatch(UserRepresentative p1, UserRepresentative p2) {
        int roomId = roomCounter++;
        p1.user.setStatus(User.STATUS.INGAME);
        p2.user.setStatus(User.STATUS.INGAME);

        System.out.println(String.format("Match created! Room: %d, Player 1: %s vs Player 2: %s",
                roomId, p1.user.getUsername(), p2.user.getUsername()));

        if (encoder != null) {
            // Định dạng matchInfo: roomId;opponentUsername;opponentDisplayName;opponentRanking;role
            // role = 1: Đi trước (màu đỏ), role = 2: Đi sau (màu xanh)
            String matchInfoForP1 = String.format("%d;%s;%s;%d;1",
                    roomId,
                    p2.user.getUsername(),
                    p2.user.getDisplayName(),
                    p2.user.getRanking());

            String matchInfoForP2 = String.format("%d;%s;%s;%d;2",
                    roomId,
                    p1.user.getUsername(),
                    p1.user.getDisplayName(),
                    p1.user.getRanking());

            encoder.notifyMatchFound(p1.state, matchInfoForP1);
            encoder.notifyMatchFound(p2.state, matchInfoForP2);
        }
    }

    public synchronized int getQueueSize() {
        return queue.size();
    }
}
