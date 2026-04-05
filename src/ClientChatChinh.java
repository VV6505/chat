import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;

public class ClientChatChinh extends JFrame {
    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;

    private JPanel chatPanel; // Panel chứa nội dung tin nhắn
    private JPanel clientListPanel; // Panel chứa danh sách client
    private JTextField inputField; // Ô nhập tin nhắn
    private JButton sendButton, exitButton, imageButton, fileButton; // Các nút gửi, thoát, gửi ảnh
    private JScrollPane scrollPane; // Thanh cuộn cho panel tin nhắn
    private ClientChatCon chatMoi; // Cửa sổ trò chuyện hiện tại
    private Map<String, List<String>> chatHistory = new HashMap<>(); // Lưu trữ lịch sử chat theo client
    private String currentClient;

    public ClientChatChinh() {
        setTitle("Client Chat");
        setSize(600, 450);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panel bên trái chứa danh sách client
        clientListPanel = new JPanel();
        clientListPanel.setLayout(new BoxLayout(clientListPanel, BoxLayout.Y_AXIS));
        clientListPanel.setBackground(Color.LIGHT_GRAY);
        clientListPanel.setPreferredSize(new Dimension(150, 0)); // Đặt kích thước cho panel

        // Thêm danh sách client (ví dụ Client 1, Client 2, ...)
        addClientButton("Client 1");
        addClientButton("Client 2");
        addClientButton("Client 3");
        addClientButton("Client 4");
        addClientButton("Client 5");

        add(clientListPanel, BorderLayout.WEST); // Thêm panel client vào phần bên trái

        // Panel chính chứa tin nhắn
        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(Color.WHITE);

        scrollPane = new JScrollPane(chatPanel); // Thêm thanh cuộn
        scrollPane.setBorder(BorderFactory.createTitledBorder("💬 Cuộc trò chuyện"));
        add(scrollPane, BorderLayout.CENTER);
        
        
        clientListPanel.setBorder(BorderFactory.createTitledBorder("👥 Các Client"));
        clientListPanel.setBackground(new Color(230, 240, 255));

        inputField = new JTextField(); // Ô nhập nội dung
        fileButton = new JButton("File");
        sendButton = new JButton("Gửi");
        imageButton = new JButton("Ảnh");
        exitButton = new JButton("Thoát");

        // Panel dưới cùng chứa ô nhập và các nút
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputField, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(fileButton);
        btnPanel.add(imageButton);
        btnPanel.add(sendButton);
        btnPanel.add(exitButton);
        bottomPanel.add(btnPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        // Sự kiện nút
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
        exitButton.addActionListener(e -> closeConnection());
        imageButton.addActionListener(e -> sendImage());
        fileButton.addActionListener(e -> sendFile());

        connectToServer(); // Kết nối tới server khi khởi động
        setVisible(true);
    }

    private void addClientButton(String clientName) {
        JButton clientButton = new JButton(clientName);
        clientButton.setFocusPainted(false);
        clientButton.setPreferredSize(new Dimension(160, 50));
        clientButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        clientButton.setBackground(new Color(230, 240, 255)); // màu nền xanh nhạt
        clientButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        clientButton.setForeground(new Color(33, 33, 33)); // chữ đậm vừa
        clientButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        clientButton.setHorizontalAlignment(SwingConstants.LEFT);

        // Tránh hiện trắng xóa
        clientButton.setContentAreaFilled(true);
        clientButton.setOpaque(true);
        clientButton.setBorderPainted(false);

        // Hover hiện đại
        clientButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                clientButton.setBackground(new Color(200, 225, 255)); // hover xanh hơn
                clientButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                clientButton.setBackground(new Color(230, 240, 255)); // trở lại nền cũ
                clientButton.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });

        // Khi click vào client
        clientButton.addActionListener(e -> openChat(clientName));
        clientButton.addActionListener(e -> switchToClientChat(clientName));

        clientListPanel.add(clientButton);
        clientListPanel.revalidate();
    }

    private void openChat(String clientName) {
        // Mở cửa sổ trò chuyện cho client mới và reset nội dung
        if (chatMoi != null) {
            chatMoi.dispose(); // Đóng cửa sổ trò chuyện trước đó
        }
        chatMoi = new ClientChatCon(clientName); // Tạo cửa sổ trò chuyện mới với client
        chatMoi.setVisible(true);
        
        currentClient = clientName; // selectedClient là tên client được chọn
        
        // Reset tin nhắn trong cửa sổ trò chuyện hiện tại
        xoaCuocTroChuyen();
        hienThiLichSu(clientName);
    }

    private void switchToClientChat(String clientName) {
        // Logic chuyển sang trò chuyện với client đó và reset tin nhắn
        System.out.println("Chuyển sang trò chuyện với " + clientName);
        // Reset nội dung tin nhắn
        if (chatMoi != null) {
            chatMoi.resetMessages(); // Gọi hàm reset tin nhắn
        }
    }
    
    private void xoaCuocTroChuyen() {
        // Xóa hết các tin nhắn cũ trong chatPanel
        chatPanel.removeAll();
        chatPanel.revalidate();  // Cập nhật lại giao diện
        chatPanel.repaint();     // Vẽ lại giao diện
    }
    
    // Kết nối đến server
    private void connectToServer() {
        try {
            socket = new Socket("192.168.1.6", 2025); // đổi IP nếu cần
            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream());

            // Thread để nhận dữ liệu từ server
            new Thread(() -> {
                try {
                    while (true) {
                        String type = inputStream.readUTF(); // Đọc loại dữ liệu: TEXT, IMAGE hoặc FILE

                        if (type.equals("TEXT")) {
                            String msg = inputStream.readUTF(); // Đọc nội dung tin nhắn
                            addMessage(msg, false); // Hiển thị tin nhắn từ người khác
                            
                            // Chỉ lưu phần nội dung tin nhắn
                            chatHistory.computeIfAbsent(currentClient, k -> new ArrayList<>()).add("Client: "+msg);
                           
                        } else if (type.equals("IMAGE")) {
                            String fileName = inputStream.readUTF(); // Đọc tên file ảnh
                            int size = inputStream.readInt(); // Đọc kích thước ảnh
                            byte[] imageData = new byte[size];
                            inputStream.readFully(imageData); // Đọc toàn bộ dữ liệu ảnh

                            ImageIcon icon = new ImageIcon(imageData);
                            addImage(icon, false); // Hiển thị ảnh nhận được
                            
                            chatHistory.computeIfAbsent(currentClient, k -> new ArrayList<>()).add("Client đã gửi ảnh: " + fileName);

                        } else if (type.equals("FILE")) {
                            String fileName = inputStream.readUTF(); // Đọc tên file
                            int size = inputStream.readInt(); // Đọc độ dài file
                            byte[] data = inputStream.readNBytes(size); // Đọc chính xác 20480 byte vào mảng data

                            addFileMessage(fileName, data,false); // gọi hàm mới để hiển thị
                            
                            chatHistory.computeIfAbsent(currentClient, k -> new ArrayList<>()).add("Client đã gửi file: " + fileName);
                        }

                    }
                } catch (IOException e) {
                    addMessage("Mất kết nối với server.", false); // Nếu lỗi hoặc server ngắt kết nối
                }
            }).start();

            addMessage("Đã kết nối đến server.", false);
        } catch (IOException e) {
            showError("Không thể kết nối đến server."); // Hiện thông báo nếu không kết nối được
        }
    }

    private void addFileMessage(String fileName, byte[] data, boolean isClient) {
        // Tạo panel chứa tin nhắn file, căn trái nếu nhận, căn phải nếu gửi
        JPanel msgPanel = new JPanel(new FlowLayout(isClient ? FlowLayout.RIGHT : FlowLayout.LEFT));

        // Tạo label hiển thị tên file như một link HTML
        JLabel fileLabel = new JLabel("<html>Tải File: <a href='#'>" + fileName + "</a></html>");
        fileLabel.setCursor(new Cursor(Cursor.HAND_CURSOR)); // Con trỏ dạng bàn tay khi hover

        // Thiết lập màu sắc và nền label tùy theo người gửi hay nhận
        fileLabel.setForeground(Color.BLACK); // Màu chữ
        fileLabel.setOpaque(true); // Cho phép hiển thị màu nền
        fileLabel.setBackground(isClient ? new Color(179, 229, 252) : new Color(200, 230, 201)); // Màu nền tùy phía gửi hay nhận
        fileLabel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12)); // Padding cho đẹp

        // Bắt sự kiện click chuột để tải file về
        fileLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                // Hỏi người dùng có muốn tải file không
                int confirm = JOptionPane.showConfirmDialog(ClientChatChinh.this,
                        "Bạn có muốn tải file: " + fileName + "?", "Tải file", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    JFileChooser chooser = new JFileChooser(); // Mở hộp thoại chọn nơi lưu file
                    chooser.setSelectedFile(new File(fileName)); // Gợi ý tên file mặc định
                    if (chooser.showSaveDialog(ClientChatChinh.this) == JFileChooser.APPROVE_OPTION) {
                        try {
                            // Ghi dữ liệu file vào ổ đĩa
                            Files.write(chooser.getSelectedFile().toPath(), data);
                            JOptionPane.showMessageDialog(ClientChatChinh.this, "Đã tải file."); // Thông báo thành công
                        } catch (IOException ex) {
                            showError("Lỗi khi tải file."); // Nếu ghi file bị lỗi
                        }
                    }
                }
            }
        });

        // Thêm label vào panel chat
        msgPanel.add(fileLabel);
        chatPanel.add(msgPanel);
        chatPanel.revalidate(); // Cập nhật lại UI để hiển thị tin nhắn mới

        // Tự động cuộn xuống dưới cùng khung chat
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = scrollPane.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    
    private void hienThiLichSu(String clientName){
        List<String> message = chatHistory.get(clientName);
        if(message != null){
            for( String mgs: message){
                addMessage(mgs, false);
            }
        }
    }

    // Gửi tin nhắn văn bản đến server
    private void sendMessage() {
        String msg = inputField.getText().trim();
        if (!msg.isEmpty()) {
            try {
                outputStream.writeUTF("TEXT"); // Gửi loại dữ liệu là TEXT
                outputStream.writeUTF(msg); // Gửi nội dung tin nhắn
                addMessage("Bạn: " + msg, true);
                
                if(chatMoi != null){
                    String clientName = chatMoi.getClientName();
                    chatHistory.computeIfAbsent(clientName, k -> new ArrayList<>()).add("Bạn: " +msg);
                }
                
                inputField.setText("");
            } catch (IOException e) {
                showError("Lỗi khi gửi tin nhắn.");
            }
        }
    }
    
    //Gửi File
    private void sendFile() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] data = fis.readAllBytes();

                outputStream.writeUTF("FILE"); // Gửi loại FILE
                outputStream.writeUTF(file.getName()); // Gửi tên file
                outputStream.writeInt(data.length); // Gửi độ dài
                outputStream.write(data); // Gửi nội dung

                addMessage("File: " + file.getName(), true);
                
                if(chatMoi != null){
                    String clientName = chatMoi.getClientName();
                    chatHistory.computeIfAbsent(clientName, k -> new ArrayList<>()).add("Bạn đã gửi file: " + file.getName());
                }
            } catch (IOException e) {
                showError("Lỗi khi gửi file.");
            }
        }
    }
    
    
    // Gửi ảnh
    private void sendImage() {
        JFileChooser chooser = new JFileChooser(); // Tạo một hộp thoại chọn file (file chooser) để người dùng có thể chọn file từ máy
        int result = chooser.showOpenDialog(this); // Hiển thị hộp thoại và đợi người dùng chọn file (sẽ mở thư mục mặc định của hệ điều hành)
        if (result == JFileChooser.APPROVE_OPTION) { // Nếu người dùng nhấn "Open" (tức là đồng ý chọn file)
            File file = chooser.getSelectedFile(); // Lấy ra file mà người dùng vừa chọn từ hộp thoại

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] data = fis.readAllBytes(); // Đọc dữ liệu ảnh

                outputStream.writeUTF("IMAGE"); // Gửi loại dữ liệu là IMAGE
                outputStream.writeUTF(file.getName()); // Gửi tên file
                outputStream.writeInt(data.length); // Gửi độ dài
                outputStream.write(data); // Gửi dữ liệu ảnh

                addImage(new ImageIcon(data), true); // Hiển thị ảnh gửi
                
                // Lưu lịch sử gửi ảnh
                if (chatMoi != null) {
                    String clientName = chatMoi.getClientName();
                    chatHistory.computeIfAbsent(clientName, k -> new ArrayList<>()).add("Bạn đã gửi ảnh: " + file.getName());
                }
                } catch (IOException e) {
                    showError("Lỗi khi gửi ảnh.");
                }
        }
    }

    // Hiển thị tin nhắn lên giao diện
    private void addMessage(String message, boolean isClient) {
        JPanel msgPanel = new JPanel(new FlowLayout(isClient ? FlowLayout.RIGHT : FlowLayout.LEFT));
        // Dùng HTML để giới hạn chiều rộng
        JLabel label = new JLabel("<html><p style='width: 250px'>" + message + "</p></html>");
        // Bật hiển thị nền màu cho JLabel
        label.setOpaque(true);
        label.setBackground(isClient ? new Color(179, 229, 252) : new Color(200, 230, 201));
        label.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        // Thêm label vào panel tin nhắn, rồi thêm vào khung chat chính
        msgPanel.add(label);
        chatPanel.add(msgPanel);
        chatPanel.revalidate(); // Cập nhật lại giao diện
        
        // Tự động cuộn xuống dòng
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = scrollPane.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }
    
    // Hiển thị ảnh lên giao diện
    private void addImage(ImageIcon icon, boolean isClient) {
        JPanel imgPanel = new JPanel(new FlowLayout(isClient ? FlowLayout.RIGHT : FlowLayout.LEFT));

        // Resize ảnh cho phù hợp giao diện
        Image scaled = icon.getImage().getScaledInstance(200, -1, Image.SCALE_SMOOTH);
        JLabel label = new JLabel(new ImageIcon(scaled));

        imgPanel.add(label);
        chatPanel.add(imgPanel);
        chatPanel.revalidate();
        
        // Tự động cuộn xuống dòng
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = scrollPane.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }
    
    // Ngắt kết nối và đóng cửa sổ
    private void closeConnection() {
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Lỗi khi đóng kết nối");
        }
        dispose();
    }
    
    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientChatChinh::new);
    }
}
