
import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    // Danh sách các client đang kết nối, dùng synchronized để đảm bảo an toàn luồng
    private static final Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(2025)) { // Mở server socket trên cổng 2025
            System.out.println("Server đang chạy trên cổng 2025...");

            while (true) {
                Socket clientSocket = serverSocket.accept(); // Chờ và nhận kết nối từ client
                System.out.println("Client kết nối: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket); // Tạo handler cho client mới
                clients.add(handler); // Thêm client vào danh sách
                new Thread(handler).start(); // Chạy handler trên luồng mới
            }

        } catch (IOException e) {
            System.out.println("Lỗi server: " + e.getMessage());
        }
    }
    
    // Gửi tin nhắn văn bản đến tất cả client
    public static void broadcastText(String message, ClientHandler sender) {
        synchronized (clients) { //Đảm bảo đồng bộ hóa (thread-safe) khi duyệt danh sách clients
            for (ClientHandler client : clients) { // Duyệt qua tất cả client đang kết nối.
                if (client != sender) { // Nếu client đang xét không phải là người gửi (sender) → mới gửi cho họ.
                    client.sendText(message);
                }
            }
        }
    }
    
    // Gửi ảnh đến tất cả client
    public static void broadcastImage(String fileName, byte[] imageData, ClientHandler sender) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client != sender) {
                    client.sendImage(fileName, imageData);
                }
            }
        }
    }
    
    // Gửi file đến tất cả client
    public static void broadcastFile(String fileName, byte[] data, ClientHandler sender) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client != sender) {
                    client.sendFile(fileName, data);
                }
            }
        }
    }
    
    // Xóa client khi ngắt kết nối
    public static void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("Client ngắt kết nối");
    }
}

class ClientHandler implements Runnable {
    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            input = new DataInputStream(socket.getInputStream()); // Lấy luồng input từ socket
            output = new DataOutputStream(socket.getOutputStream()); // Lấy luồng output từ socket
        } catch (IOException e) {
            System.out.println("Không thể tạo luồng IO cho client.");
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            while (true) {
                String type = input.readUTF(); // Đọc loại dữ liệu gửi đến

                if (type.equals("TEXT")) {
                    String message = input.readUTF(); // Đọc nội dung tin nhắn
                    System.out.println("Tin nhắn: " + message);
                    ChatServer.broadcastText(message, this); // Gửi tin nhắn đến các client khác
                
                }else if (type.equals("FILE")) {
                    String fileName = input.readUTF();
                    int size = input.readInt();
                    byte[] fileData = new byte[size];
                    input.readFully(fileData);
                    System.out.println("Nhận được file: "+fileName + " (" +size + " bytes)");
                    ChatServer.broadcastFile(fileName, fileData, this);

                } else if (type.equals("IMAGE")) {
                    String fileName = input.readUTF(); // Đọc tên file ảnh
                    int size = input.readInt(); // Đọc kích thước ảnh
                    byte[] imageData = new byte[size];
                    input.readFully(imageData); // Đọc toàn bộ dữ liệu ảnh

                    System.out.println("Ảnh được gửi: " + fileName + " (" + size + " bytes)");
                    ChatServer.broadcastImage(fileName, imageData, this); // Gửi ảnh đến các client khác
                }
            }
        } catch (IOException e) {
//            System.out.println("Client đã rời đi hoặc mất kết nối.");
        } finally {
            ChatServer.removeClient(this); // Xóa client khỏi danh sách
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Lỗi đóng socket.");
            }
        }
    }
    
    // Gửi tin nhắn văn bản
    public void sendText(String message) {
        try {
            output.writeUTF("TEXT");
            output.writeUTF(message);
            output.flush();
        } catch (IOException e) {
            System.out.println("Lỗi gửi tin nhắn văn bản.");
        }
    }
    
    // Gửi ảnh
    public void sendImage(String fileName, byte[] data) {
        try {
            output.writeUTF("IMAGE");
            output.writeUTF(fileName);
            output.writeInt(data.length);
            output.write(data);
            output.flush();
        } catch (IOException e) {
            System.out.println("Lỗi gửi ảnh.");
        }
    }
    
    // Gửi file
    public void sendFile(String fileName, byte [] data){
        try {
            output.writeUTF("FILE");
            output.writeUTF(fileName);
            output.writeInt(data.length);
            output.write(data);
            output.flush();
        } catch (IOException e) {
            System.out.println("Lỗi gửi file");
        }
    }
}
