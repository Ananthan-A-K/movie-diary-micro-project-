# 🎬 Movie Review Management System (Java + MySQL)

A console-based **Java** application that demonstrates **Object-Oriented Programming (OOP)** principles integrated with a **MySQL database** using **JDBC**.  
It enables users to create accounts, browse movies, post reviews, and manage their watchlist — all through a simple command-line interface.

---

## 📘 Overview

This microproject showcases core OOP concepts such as **classes, objects, inheritance, polymorphism, encapsulation,** and **abstraction**.  
The project demonstrates how Java applications can interact with databases to store and manage persistent data effectively.

---

## 🧠 Features

- 👤 User registration and login  
- 🎞️ Add and view movie details  
- 🌟 Add and view movie reviews  
- 📋 Maintain a personal watchlist  
- 💾 Persistent MySQL data storage  

---

## ⚙️ System Requirements

- **Java JDK 17+**  
- **MySQL Server 8+**  
- **MySQL Connector/J (mysql-connector-j-9.4.0.jar)**  
- **IDE:** VS Code / IntelliJ / Eclipse  

---

## 🧩 Setup Instructions

1. **Create Database**
   ```sql
   CREATE DATABASE moviereviewdb;
   USE moviereviewdb;
Place JDBC Driver

Copy mysql-connector-j-9.4.0.jar into your project folder.

Compile the Program

bash
Copy code
javac -cp ".;mysql-connector-j-9.4.0.jar" MovieReviewApp.java
Run the Program

bash
Copy code
java -cp ".;mysql-connector-j-9.4.0.jar" MovieReviewApp
(Use : instead of ; on macOS/Linux)

🧱 System Design
Classes

MovieReviewApp – Main application logic

User – Manages user data and authentication

Movie – Handles movie details

Review – Stores ratings and comments

Database Tables

users(id, username, password)

movies(id, title, year, starring, description)

reviews(id, user_id, movie_id, rating, comment)

watchlist(id, user_id, movie_id)

💡 OOP Concepts Demonstrated
Concept	Description
Encapsulation	Data and behavior grouped within classes
Abstraction	Database details hidden behind methods
Inheritance	Extensible design for future admin/user roles
Polymorphism	Supports method overloading/overriding

🖥️ Example Output
text
Copy code
===============================
 MOVIE REVIEW SYSTEM
===============================
1. Register
2. Login
3. Exit
Enter choice: 1
Enter username: anant
Enter password: ****
✅ User registered successfully!
🏁 Learning Outcomes
Applied OOP principles in a real-world scenario

Gained hands-on experience with JDBC and MySQL

Implemented a modular Java program with persistent data

Improved understanding of system design and database integration

📚 References
E. Balagurusamy, Object-Oriented Programming with Java

Oracle Java Documentation

MySQL Connector/J Documentation

TutorialsPoint / GeeksforGeeks – JDBC and OOPs Concepts
