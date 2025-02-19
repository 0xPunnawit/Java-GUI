# 🛒 FurniturePOS - A Simple Furniture Store Management System  

**FurniturePOS** is a **Java-based** Point of Sale (**POS**) system for managing furniture sales, built with **Java JDK 17, NetBeans, and MySQL (XAMPP)**.  
This system supports **member registration, product management, checkout processing, and a reward points system** for customer retention. 🎉  

---

## 🚀 Features
### 🔹 1. Member Registration System
- Customers can **register as members** by providing **first name, last name, and phone number**.
- The **phone number serves as the unique identifier** (Primary Key).
- Members **earn points** (1 point per **500 THB** spent).
- Points are **updated automatically** after each purchase.

### 🔹 2. Product Management System
- Add **new products** (name, price, quantity).
- **Edit existing products**.
- **Delete products** from the system.
- View **all products in real-time** from the database.

### 🔹 3. Checkout & Sales System
- Displays **all available products** from the database.
- Customers can **select products, specify quantity, and add them to the cart**.
- The system **calculates total price and earned points automatically**.
- If the customer is a **registered member**, they can enter their phone number to **update points in the database**.
- Purchased **products are deducted from inventory** automatically.

---

## 🛠 Technology Stack
- **Java JDK 17**  
- **NetBeans IDE** (Swing GUI)  
- **MySQL (XAMPP)**  
- **JDBC Connector (`mysql-connector-j-9.2.0.jar`)**  

---

## 📥 Installation & Setup
### ✅ 1. Install & Set Up MySQL Database
1. Start **XAMPP** and enable **MySQL**.  
2. Open **phpMyAdmin** and **import `furniture_shop_db.sql`**.  
3. Verify that the following tables exist:
   - `members` → Stores member data  
   - `products` → Stores product details  
   - `orders` → Stores sales transactions  
   - `order_items` → Stores purchased items  
   - `points` → Stores reward points transactions  

### ✅ 2. Configure Database Connection
Edit the `DatabaseConnection.java` file to match your MySQL credentials:
```java
private static final String URL = "jdbc:mysql://localhost:3306/furniture_shop_db?useSSL=false&serverTimezone=UTC";
private static final String USERNAME = "root";
private static final String PASSWORD = ""; // Change this if you set a MySQL password
