import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import javax.swing.*;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

public class ClientChatCon extends JFrame {
    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private String clientName;

    private JPanel chatPanel; // Panel chứa nội dung tin nhắn
    private JTextField inputField; // Ô nhập tin nhắn
    private JButton sendButton, exitButton, imageButton,fileButton; // Các nút gửi, thoát, gửi ảnh
    private JScrollPane scrollPane; // Thanh cuộn cho panel tin nhắn

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public ClientChatCon(String clientName) {
        this.clientName = clientName;
        setTitle("Client Chat");
        setSize(600, 450);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Panel chính chứa tin nhắn
        chatPanel = new JPanel(); 
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(Color.WHITE);

        scrollPane = new JScrollPane(chatPanel); // Thêm thanh cuộn
        scrollPane.setBorder(BorderFactory.createTitledBorder("💬 Cuộc trò chuyện"));
        add(scrollPane, BorderLayout.CENTER);

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
    
    // Hàm reset lại tin nhắn trong cửa sổ chat
    public void resetMessages() {
        chatPanel.removeAll(); // Xóa toàn bộ tin nhắn hiện tại
        chatPanel.revalidate(); // Cập nhật lại giao diện
        chatPanel.repaint(); // Vẽ lại giao diện sau khi xóa
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

                        } else if (type.equals("IMAGE")) {
                            String fileName = inputStream.readUTF(); // Đọc tên file ảnh
                            int size = inputStream.readInt(); // Đọc kích thước ảnh
                            byte[] imageData = new byte[size];
                            inputStream.readFully(imageData); // Đọc toàn bộ dữ liệu ảnh

                            ImageIcon icon = new ImageIcon(imageData);
                            addImage(icon, false); // Hiển thị ảnh nhận được

                        } else if (type.equals("FILE")) {
                            String fileName = inputStream.readUTF();
                            int size = inputStream.readInt();
                            byte[] data = inputStream.readNBytes(size);

                            addFileMessage(fileName, data,false); //gọi hàm mới để hiển thị
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
        JPanel msgPanel = new JPanel(new FlowLayout(isClient ? FlowLayout.RIGHT : FlowLayout.LEFT));

        JLabel fileLabel = new JLabel("<html>Tải File: <a href='#'>" + fileName + "</a></html>");
        fileLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        fileLabel.setForeground(Color.BLACK);
        fileLabel.setOpaque(true);
        fileLabel.setBackground(isClient ? new Color(179, 229, 252) : new Color(200, 230, 201));
        fileLabel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        // Bắt sự kiện click để lưu file
        fileLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int confirm = JOptionPane.showConfirmDialog(ClientChatCon.this,
                        "Bạn có muốn tải file: " + fileName + "?", "Tải file", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setSelectedFile(new File(fileName));
                    if (chooser.showSaveDialog(ClientChatCon.this) == JFileChooser.APPROVE_OPTION) {
                        try {
                            Files.write(chooser.getSelectedFile().toPath(), data);
                            JOptionPane.showMessageDialog(ClientChatCon.this, "Đã tải file.");
                        } catch (IOException ex) {
                            showError("Lỗi khi tải file.");
                        }
                    }
                }
            }
        });

        msgPanel.add(fileLabel);
        chatPanel.add(msgPanel);
        chatPanel.revalidate();

        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = scrollPane.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }


    // Gửi tin nhắn văn bản đến server
    private void sendMessage() {
        String msg = inputField.getText().trim();
        if (!msg.isEmpty()) {
            try {
                outputStream.writeUTF("TEXT"); // Gửi loại dữ liệu là TEXT
                outputStream.writeUTF(msg); // Gửi nội dung tin nhắn
                addMessage("Bạn: " + msg, true);
                inputField.setText("");
            } catch (IOException e) {
                showError("Lỗi khi gửi tin nhắn.");
            }
        }
    }
    
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
            } catch (IOException e) {
                showError("Lỗi khi gửi file.");
            }
        }
    }
    
    // Gửi ảnh đến server
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
            } catch (IOException e) {
                showError("Lỗi khi gửi ảnh.");
            }
        }
    }

    // Hiển thị tin nhắn lên giao diện
    private void addMessage(String message, boolean isClient) {
        JPanel msgPanel = new JPanel(new FlowLayout(isClient ? FlowLayout.RIGHT : FlowLayout.LEFT));
        JLabel label = new JLabel("<html><p style='width: 250px'>" + message + "</p></html>");
        label.setOpaque(true);
        label.setBackground(isClient ? new Color(179, 229, 252) : new Color(200, 230, 201));
        label.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        msgPanel.add(label);
        chatPanel.add(msgPanel);
        chatPanel.revalidate();
        
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
