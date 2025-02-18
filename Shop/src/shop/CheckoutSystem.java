package shop;

import java.sql.*;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

public class CheckoutSystem extends javax.swing.JFrame {

    private DefaultTableModel productModel;
    private DefaultTableModel cartModel;
    private double totalPrice = 0;
    private int totalPoints = 0;

    public CheckoutSystem() {
        initComponents();
        loadProducts(); // โหลดสินค้าจาก Database
        productModel = (DefaultTableModel) productTable.getModel();
        cartModel = (DefaultTableModel) cartTable.getModel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
    }

    // โหลดสินค้าจาก Database
    private void loadProducts() {
        DefaultTableModel model = (DefaultTableModel) productTable.getModel();
        model.setRowCount(0);

        try (Connection conn = DatabaseConnection.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM products")) {

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("product_id"),
                    rs.getString("name"),
                    rs.getDouble("price"),
                    rs.getInt("quantity")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error!", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    // เพิ่มสินค้าลงตะกร้า
    private void addToCart() {
        int row = productTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int productId = (int) productTable.getValueAt(row, 0);
        String name = productTable.getValueAt(row, 1).toString();
        double price = (double) productTable.getValueAt(row, 2);
        int availableQuantity = (int) productTable.getValueAt(row, 3);

        String quantityText = JOptionPane.showInputDialog(this, "Enter quantity:", "Quantity", JOptionPane.QUESTION_MESSAGE);
        if (quantityText == null || quantityText.isEmpty()) {
            return;
        }

        try {
            int quantity = Integer.parseInt(quantityText);
            if (quantity <= 0 || quantity > availableQuantity) {
                JOptionPane.showMessageDialog(this, "Invalid quantity!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            double subtotal = price * quantity;
            cartModel.addRow(new Object[]{productId, name, price, quantity, subtotal});

            totalPrice += subtotal;
            totalPoints = (int) (totalPrice / 500);
            TXT_TotalPrice.setText("Total: " + totalPrice + " ฿");
            TXT_Point1.setText("Points Earned: " + totalPoints);

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid number!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

// ยืนยันการซื้อสินค้า 
    private void confirmPurchase() {
        if (cartModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Cart is empty!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String phone = TXT_Member.getText().trim();
        boolean isMember = !phone.isEmpty();

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            // บันทึก Order
            String orderQuery = "INSERT INTO orders (phone, total_price) VALUES (?, ?)";
            PreparedStatement orderStmt = conn.prepareStatement(orderQuery, Statement.RETURN_GENERATED_KEYS);
            orderStmt.setString(1, isMember ? phone : null);
            orderStmt.setDouble(2, totalPrice);
            orderStmt.executeUpdate();

            ResultSet rs = orderStmt.getGeneratedKeys();
            if (!rs.next()) {
                throw new SQLException("Failed to get Order ID!");
            }

            int orderId = rs.getInt(1);

            // บันทึกรายการสินค้าใน order_items
            String itemQuery = "INSERT INTO order_items (order_id, product_id, quantity, subtotal) VALUES (?, ?, ?, ?)";
            PreparedStatement itemStmt = conn.prepareStatement(itemQuery);

            // อัปเดตจำนวนสินค้าใน stock
            String updateStockQuery = "UPDATE products SET quantity = quantity - ? WHERE product_id = ?";
            PreparedStatement updateStockStmt = conn.prepareStatement(updateStockQuery);

            for (int i = 0; i < cartModel.getRowCount(); i++) {
                int productId = (int) cartModel.getValueAt(i, 0);  // Product ID
                int quantity = (int) cartModel.getValueAt(i, 3);  // Quantity
                double price = (double) cartModel.getValueAt(i, 2); // ดึงราคาต่อชิ้นจาก index 2
                double subtotal = price * quantity;  // คำนวณราคาทั้งหมดเอง

                // บันทึกข้อมูลลงตาราง order_items
                itemStmt.setInt(1, orderId);
                itemStmt.setInt(2, productId);
                itemStmt.setInt(3, quantity);
                itemStmt.setDouble(4, subtotal);
                itemStmt.executeUpdate();

                // อัปเดตสต็อกสินค้า
                updateStockStmt.setInt(1, quantity);
                updateStockStmt.setInt(2, productId);
                updateStockStmt.executeUpdate();
            }

            // อัปเดตแต้มสะสมถ้าเป็นสมาชิก
            if (isMember) {
                int earnedPoints = (int) (totalPrice / 500); // คำนวณแต้ม (500 บาท = 1 แต้ม)

                // อัปเดตแต้มสะสมในตาราง members
                String pointUpdateQuery = "UPDATE members SET points = points + ? WHERE phone = ?";
                PreparedStatement pointUpdateStmt = conn.prepareStatement(pointUpdateQuery);
                pointUpdateStmt.setInt(1, earnedPoints);
                pointUpdateStmt.setString(2, phone);
                pointUpdateStmt.executeUpdate();

                // บันทึกข้อมูลแต้มสะสมลงตาราง points
                String pointsQuery = "INSERT INTO points (phone, points_earned) VALUES (?, ?)";
                PreparedStatement pointsStmt = conn.prepareStatement(pointsQuery);
                pointsStmt.setString(1, phone);
                pointsStmt.setInt(2, earnedPoints);
                pointsStmt.executeUpdate();
            }

            conn.commit();
            JOptionPane.showMessageDialog(this, "Purchase successful!", "Success", JOptionPane.INFORMATION_MESSAGE);

            // อัปเดตสินค้าใหม่
            loadProducts();
            cartModel.setRowCount(0);
            TXT_TotalPrice.setText("Total: 0 ฿");
            TXT_Point1.setText("Points Earned: 0");
            TXT_Member.setText("");

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error!", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        productTable = new javax.swing.JTable();
        TXT_TotalPrice = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        cartTable = new javax.swing.JTable();
        jLabel3 = new javax.swing.JLabel();
        TXT_Point = new javax.swing.JLabel();
        TXT_Member = new javax.swing.JTextField();
        TXT_Point1 = new javax.swing.JLabel();
        BT_OK = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        jLabel1.setFont(new java.awt.Font("Tahoma", 0, 48)); // NOI18N
        jLabel1.setText("คำนวณสินค้า");

        productTable.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        productTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "ID", "Name", "Price", "Quantity"
            }
        ));
        jScrollPane1.setViewportView(productTable);

        TXT_TotalPrice.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        TXT_TotalPrice.setText("ยอดเงินรวม: ");

        cartTable.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        cartTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "list", "Name", "Price", "Title 4"
            }
        ));
        jScrollPane2.setViewportView(cartTable);

        jLabel3.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel3.setText("ตะกร้า");

        TXT_Point.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        TXT_Point.setText("รหัสสมาชิกเบอร์");

        TXT_Member.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N

        TXT_Point1.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        TXT_Point1.setText("คะแนนแต้ม: ");

        BT_OK.setText("OK");
        BT_OK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BT_OKActionPerformed(evt);
            }
        });

        jButton1.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jButton1.setText("เพิ่มลงตะกร้า");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jLabel4.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel4.setText("รายการสินค้า");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(TXT_TotalPrice, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(TXT_Point1, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(BT_OK, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(TXT_Point, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(TXT_Member, javax.swing.GroupLayout.PREFERRED_SIZE, 177, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(29, 29, 29)
                                .addComponent(jLabel1))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(49, 49, 49)
                                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(28, 28, 28)
                                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap(27, Short.MAX_VALUE))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(39, 39, 39)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(651, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(31, 31, 31)
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(217, 217, 217))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 286, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(14, 14, 14)))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 50, Short.MAX_VALUE)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 345, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(14, 14, 14))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(70, 70, 70)
                        .addComponent(TXT_TotalPrice)
                        .addGap(29, 29, 29)
                        .addComponent(TXT_Point1)
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(TXT_Point)
                            .addComponent(TXT_Member, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(31, 31, 31)
                        .addComponent(BT_OK, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                    .addContainerGap(392, Short.MAX_VALUE)
                    .addComponent(jLabel3)
                    .addGap(367, 367, 367)))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        addToCart();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void BT_OKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BT_OKActionPerformed
        confirmPurchase();
    }//GEN-LAST:event_BT_OKActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(CheckoutSystem.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(CheckoutSystem.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(CheckoutSystem.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(CheckoutSystem.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new CheckoutSystem().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton BT_OK;
    private javax.swing.JTextField TXT_Member;
    private javax.swing.JLabel TXT_Point;
    private javax.swing.JLabel TXT_Point1;
    private javax.swing.JLabel TXT_TotalPrice;
    private javax.swing.JTable cartTable;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable productTable;
    // End of variables declaration//GEN-END:variables
}
