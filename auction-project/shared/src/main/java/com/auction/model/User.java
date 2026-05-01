package com.auction.model;

import java.time.LocalDateTime;
import java.util.UUID;


// Lớp trừu tượng User: Dùng làm khuôn mẫu cho các loại người dùng khác.
// Không ai được phép tạo trực tiếp 1 "User" chung chung (không thể dùng: new User())
public abstract class User extends Entity {
    private String username;
    private String email;
    private String gender;
    private String DateOfBirth;
    private String password;
    private String phoneNumber;
    private final LocalDateTime createdAt;
    private boolean active;
    

    // Constructor (Hàm khởi tạo)
    public User(String id,String username, String email,String password,String phoneNumber,String gender,String dateOfBirth,LocalDateTime createdAt,boolean active) {
        super(id);
        this.username = username;
        this.email = email;
        this.password = password;
        this.phoneNumber=phoneNumber;
        this.gender=gender;
        this.createdAt=createdAt;
        this.active=active;
    }
    //constructor tao moi- tu dong sinh ID va gan thoi gian hien tai
    //Dung khi nguoi dung dang ki tai khoan moi:
    protected User(String fullName,String username,String email,String password,String gender,String dateOfBirth){
        this(UUID.randomUUID().toString(),//Sinh Id ngau nhien
            fullName, 
            username, 
            email, 
            password, 
            null, 
            gender, 
            dateOfBirth, 
            LocalDateTime.now()l,
            true //Mac dinh Tai khoan moi la active
);
    }
    //ABSTRACT METHOD:
    public abstract UserRole getRole();
    public abstract String getDashboardView();
    public boolean checkPassword(String hashedInput){
        return this.password.equals(hasedInput);
    }
    public boolean hasPermission(int requiredLevel){
        return getRole().getLevel() >= requiredLevel;
    }
    //tra ve thong tin ngang gon de hien thi tren UI or log
    public String toString(){
        return String.format("[%s] %s (%s)", getRole().name(), username != null ? username : fullName, email);
  }
    public boolean equals(Object obj){
        if(this==obj) return true;
        if(!(obj instanceof User other)) return false;
        return this.id.equals(other.id);

    }
    public int hasCode(){
        return id.hashCode();
    }
    // Phương thức chung cho mọi User
    public void login() {
        System.out.println(username + " đã đăng nhập vào hệ thống.");
    }

    public void logout() {
        System.out.println(username + " đã đăng xuất.");
    }

    // Getter & Setter
    public String getId() { return id; }
    public String getFullName() { return fullName; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
  public String getPhoneNumber() { return phoneNumber; }
  public String getGender() { return gender; }
  public String getDateOfBirth() { return dateOfBirth; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public boolean isActive() { return active; }

  public void setFullName(String fullName) {
    if (fullName == null || fullName.isBlank()) {
      throw new IllegalArgumentException("Họ tên không được để trống");
    }
    this.fullName = fullName;
  }

  public void setEmail(String email) {
    
    if (email == null || !email.contains("@")) {
      throw new IllegalArgumentException("Email không hợp lệ: " + email);
    }
    this.email = email;
  }

  public void setPasswordHash(String passwordHash) {
    if (passwordHash == null || passwordHash.isBlank()) {
      throw new IllegalArgumentException("Password không được rỗng");
    }
    this.passwordHash = passwordHash;
  }

  public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

  public void setUsername(String username) { this.username = username; }
  
  public void setGender(String gender) { this.gender = gender; }
  
  public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

  public void setActive(boolean active) { this.active = active; }


}