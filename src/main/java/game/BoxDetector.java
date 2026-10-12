package game;

import java.util.ArrayList;
import java.util.List;

/**
 * Phát hiện ô vuông khép kín.
 * Sau mỗi nước đi hợp lệ (vẽ 1 cạnh), kiểm tra xem có ô nào (tối đa 2 ô kề cạnh đó) vừa được khép kín hay không và gán chủ sở hữu.
 * @author Triệu Tuyên Kỳ
 */
public class BoxDetector {

    /**
     * Kiểm tra và phát hiện các ô vuông vừa được khép kín sau khi cạnh `edge` được vẽ.
     * Nếu ô vuông khép kín và chưa có chủ sở hữu, gán ô vuông đó cho `player`.
     * @param board Bàn cờ hiện tại
     * @param edge Cạnh vừa được đánh
     * @param player Người chơi thực hiện nước đi
     * @return Danh sách các ô vuông vừa được hoàn thành bởi nước đi này
     */
    public List<Box> detectAndClaimCompletedBoxes(Board board, Edge edge, Player player) {
        List<Box> completedBoxes = new ArrayList<>();
        if (board == null || edge == null || player == null) {
            return completedBoxes;
        }

        List<Box> adjacentBoxes = board.getBoxesAdjacentTo(edge);
        for (Box box : adjacentBoxes) {
            if (!box.hasOwner() && box.isCompleted()) {
                box.setOwner(player);
                completedBoxes.add(box);
            }
        }

        return completedBoxes;
    }
}
