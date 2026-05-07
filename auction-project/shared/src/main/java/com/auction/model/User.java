package com.auction.model;
import java.time.LocalDateTime;

<<<<<<< HEAD
import model.enums.UserRole;
public abstract class User extends BaseEntity {
  private String fullName;
  private String email;
  private String passwordHash;
  private String phoneNumber;
  private boolean active;
  private UserRole role;
  private String username;

  protected User(String username,String passwordHash,String email,String fullName,UserRole role){
    super();
    this.username=username;
    this.passwordHash=passwordHash;
    this.email=email;
    this.fullName=fullName;
    this.role=role;
    this.active=true;
  }
  protected User(String id,LocalDateTime createdAt,LocalDateTime updateAt,String username,String passwordHash,String email,String fullName,UserRole role,boolean active){
    super(id,createdAt,updateAt);
    this.username=username;
    this.passwordHash=passwordHash;
    this.email=email;
    this.fullName=fullName;
    this.role=role;
    this.active=active;
  }
  public abstract UserRole getRole();
  public abstract String getDashboardView();
  @Override
   public void printInfo() {
        System.out.printf("[%s] ID=%d | %s (%s) | Email: %s | Trạng thái: %s%n",
                role, getId(), fullName, username, email,
                active ? "Hoạt động" : "Bị khóa");
    }
  public String getFullName() { return fullName; }
  public String getEmail() { return email; }
  public String getPasswordHash() { return passwordHash; }
  public String getPhoneNumber() { return phoneNumber; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public boolean isActive() { return active; }
  public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
  public void setActive(boolean active) { this.active = active; }
=======
/**
 * Lớp trừu tượng User: Dùng làm khuôn mẫu cho các loại người dùng khác.
 * Không ai được phép tạo trực tiếp 1 "User" chung chung (không thể dùng: new User())
 */
public abstract class User extends Entity {
    private String fullName;
    private String username;
    private String email;
    private String gender;
    private String dateOfBirth;
    private String password;
    private String phoneNumber;
    private final LocalDateTime createdAt;
    private boolean active;

    // Constructor đầy đủ
    public User(String id, String fullName, String username, String email, String password, String phoneNumber, String gender, String dateOfBirth, LocalDateTime createdAt, boolean active) {
        super(id);
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
        this.active = active;
    }

    // Constructor tạo mới - tự động sinh ID và gán thời gian hiện tại
    protected User(String fullName, String username, String email, String password, String gender, String dateOfBirth) {
        this(UUID.randomUUID().toString(), fullName, username, email, password, null, gender, dateOfBirth, LocalDateTime.now(), true);
    }

    // ABSTRACT METHODS
    public abstract UserRole getRole();
    public abstract String getDashboardView();

    public boolean checkPassword(String inputPassword) {
        return this.password != null && this.password.equals(inputPassword);
    }

    public boolean hasPermission(int requiredLevel) {
        return getRole().getLevel() >= requiredLevel;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s)", getRole().name(), username != null ? username : fullName, email);
    }

    public void login() {
        System.out.println(username + " đã đăng nhập vào hệ thống.");
    }

    public void logout() {
        System.out.println(username + " đã đăng xuất.");
    }

    // Getter & Setter
    public String getFullName() { return fullName; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
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

    public void setPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password không được rỗng");
        }
        this.password = password;
    }

    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setUsername(String username) { this.username = username; }
    public void setGender(String gender) { this.gender = gender; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public void setActive(boolean active) { this.active = active; }
>>>>>>> e819ca10d6124354447960e56f54514b86f497ff
}