package com.auction.model;
import java.time.LocalDateTime;

import com.auction.enums.UserRole;
public abstract class User extends BaseEntity {
  private String fullName;
  private String email;
  private String passwordHash;
  private String phoneNumber;
  private boolean active;
  private UserRole role;
  private String username;
  private String gender;
  private String dateOfBirth;

  protected User(String username,String passwordHash,String email,String fullName,UserRole role,String gender,String dateOfBirth){
    super();
    this.username=username;
    this.passwordHash=passwordHash;
    this.email=email;
    this.fullName=fullName;
    this.role=role;
    this.active=true;
    this.gender=gender;
    this.dateOfBirth=dateOfBirth;
  }
  protected User(String id,LocalDateTime createdAt,LocalDateTime updateAt,String username,String passwordHash,String email,String fullName,UserRole role,boolean active,String gender,String dateOfBirth){
    super(id,createdAt,updateAt);
    this.username=username;
    this.passwordHash=passwordHash;
    this.email=email;
    this.fullName=fullName;
    this.role=role;
    this.active=active;
    this.gender=gender;
    this.dateOfBirth=dateOfBirth;
  }
  public abstract UserRole getRole();
  public abstract String getDashboardView();

  @Override
  public void printInfo() {
      System.out.printf("[%s] ID=%s | %s (%s) | Email: %s | Giới tính: %s | Ngày sinh: %s | Trạng thái: %s%n",
              role, getId(), fullName, username, email, gender, dateOfBirth,
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
  public String getUsername() { return username; }
  public String getGender() { return gender; }
  public void setGender(String gender) { this.gender = gender; }
  public String getDateOfBirth() { return dateOfBirth; }
  public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
}
