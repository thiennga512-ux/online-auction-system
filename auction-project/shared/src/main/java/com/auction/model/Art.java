package com.auction.model;
import java.time.LocalDateTime;
public class Art extends Item {

    private String artistName; // tên họa sĩ
    private int creationYear; // năm sáng tác(0 nếu ko rõ)
    private String medium; //chất liệu sáng tác:Sơn dầu, ...
    private boolean authenticated; //đã có chứng nhận kiểm định của chuyên gia chưa
    private String certificatedId// mã số chứng chỉ kiểm định
    private String dimensions // thông số vật lí ,kích thước

    public Art(String id, String name, String desc, double price, String artist,int creationYear) {
        super(id, name, desc, price);
        this.artistName = artist;
        this.creationYear=creationYear;
    }

    @Override
    public void printInfo() {
        System.out.println("Art: " + name
                + " | Artist: " + artistName
                + " | Price: " + startingPrice);
    }
    public String get
}