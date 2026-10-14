package game;

import controller.UserRepresentative;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Quản lý toàn bộ các phòng chơi (Room) đang hoạt động trên Server.
 * Cung cấp các thao tác tạo phòng, tra cứu theo roomId hoặc userId, và hủy phòng.
 */
public class RoomManager {
    private static final RoomManager INSTANCE = new RoomManager();

    private final ConcurrentHashMap<Integer, Room> rooms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Integer> userToRoomMap = new ConcurrentHashMap<>();
    private final AtomicInteger roomIdGenerator = new AtomicInteger(1);

    public RoomManager() {
    }

    public static RoomManager getInstance() {
        return INSTANCE;
    }

    /**
     * Tạo một phòng chơi mới giữa 2 người chơi và kích thước bàn cờ.
     */
    public Room createRoom(UserRepresentative p1, UserRepresentative p2, int boxRows, int boxCols) {
        int roomId = roomIdGenerator.getAndIncrement();
        Room room = new Room(roomId, p1, p2, boxRows, boxCols);

        rooms.put(roomId, room);
        userToRoomMap.put(p1.user.getUserId(), roomId);
        userToRoomMap.put(p2.user.getUserId(), roomId);

        System.out.println(String.format("RoomManager: Created Room %d (%s vs %s, Grid: %dx%d)",
                roomId, p1.user.getUsername(), p2.user.getUsername(), boxRows, boxCols));
        return room;
    }

    /**
     * Lấy phòng theo roomId.
     */
    public Room getRoom(int roomId) {
        return rooms.get(roomId);
    }

    /**
     * Lấy phòng theo userId của người chơi.
     */
    public Room getRoomByUserId(int userId) {
        Integer roomId = userToRoomMap.get(userId);
        if (roomId == null) {
            return null;
        }
        return rooms.get(roomId);
    }

    /**
     * Hủy và xóa phòng sau khi trận đấu kết thúc hoặc bị hủy.
     */
    public Room removeRoom(int roomId) {
        Room removed = rooms.remove(roomId);
        if (removed != null) {
            userToRoomMap.remove(removed.getPlayer1Rep().user.getUserId());
            userToRoomMap.remove(removed.getPlayer2Rep().user.getUserId());
            System.out.println(String.format("RoomManager: Removed Room %d. Remaining active rooms: %d",
                    roomId, rooms.size()));
        }
        return removed;
    }

    /**
     * Lấy danh sách tất cả các phòng đang hoạt động (read-only).
     */
    public Collection<Room> getAllRooms() {
        return Collections.unmodifiableCollection(rooms.values());
    }

    /**
     * Số lượng phòng đang hoạt động.
     */
    public int getActiveRoomCount() {
        return rooms.size();
    }
}
