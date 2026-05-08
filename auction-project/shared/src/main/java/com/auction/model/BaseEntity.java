package com.auction.model;
import java.util.Objects;
import java.util.UUID;
import java.time.LocalDateTime;
public abstract class BaseEntity {
    private String id;
    protected LocalDateTime createdAt;
    private LocalDateTime updateAt;
    protected BaseEntity(){
        this.id=UUID.randomUUID().toString();
        this.createdAt=LocalDateTime.now();
        this.updateAt=LocalDateTime.now();

    }
    protected BaseEntity(String id,LocalDateTime createdAt,LocalDateTime updateAt){
        this.id=id;
        this.createdAt=createdAt;
        this.updateAt=updateAt;
    }
    public abstract void printInfo();
    public String getId(){
        return id;
    }
    public LocalDateTime getcreatedAt(){
        return createdAt;
    }
    public LocalDateTime getupdateAt(){
        return updateAt;
    }
    public void touch(){
        this.updateAt=LocalDateTime.now();
    }
    public boolean equals(Object obj){
        if(this==obj) return true;
        if(obj==null|| getClass()!=obj.getClass()) return false;
        BaseEntity that= (BaseEntity) obj;
        return Objects.equals(id,that.id);
    }
    public int hashCode(){
        return Objects.hash(id);
    }
}


