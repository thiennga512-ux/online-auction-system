package com.auction.server.observer;
import com.auction.network.Response;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
public class AuctionBroadcaster {
    //Map: sessionId -> danh sach cac client dang xem phien dau gia do
    private final Map<String,List<ClientObserver>> sessionSubscribers= new ConcurrentHashMap<>();
    private static final AuctionBroadcaster instance = new AuctionBroadcaster();
    private AuctionBroadcaster(){}
    public static AuctionBroadcaster getInstance(){
        return instance;
    }
    public List<ClientObserver> getSubscribersForSession(String sessionId){
        return sessionSubscribers.get(sessionId);
    }
    //Client mo man hinh dau gia -> dang ki nhan thong bao
    public void subscribe(String sessionId,ClientObserver observer){
        sessionSubscribers.computeIfAbsent(sessionId,k-> new CopyOnWriteArrayList<>()).add(observer);
        System.out.println("[Broadcaster] User "+ observer.getUserId()+ " theo dõi phiên "+ sessionId.substring(0,8));
    }
    public void unsubscribe(String sessionId,ClientObserver observer){
        List<ClientObserver> list = sessionSubscribers.get(sessionId);
        if (list != null) {
            list.remove(observer);
        }
    }
    //go dang ki cua client o moi phien (khi mat ket noi dot ngot)
    public void unsubscribeAll(ClientObserver observer){
        for(String sessionId: sessionSubscribers.keySet()){
            unsubscribe(sessionId,observer);
        }
    }
    //phat di 1 goi response cho tat ca nhung nguoi dang theo doi phien
    public void broadcastToSession(String sessionId,Response eventResponse){
        List<ClientObserver> list = sessionSubscribers.get(sessionId);
        if(list != null && !list.isEmpty()){
            for(ClientObserver observer: list){
                try{
                    observer.onUpdate(eventResponse);

                }catch(Exception e){
                    System.err.println("[Broadcaster] Lỗi khi gửi update đến user "+ observer.getUserId()+ ": " + e.getMessage());
                }
            }
            System.out.println("[Broadcaster] Đã broadcast tới " + list.size() + " client cho phiên " + sessionId.substring(0, 8));
        }
    }
}
