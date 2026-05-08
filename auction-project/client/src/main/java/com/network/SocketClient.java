package com.network;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
public class SocketClient {
    private static final String HOST = "127.0.0.1";
    private static final int PORT=8080;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final List<Consumer<Response>> listeners= new CopyOnWriteArrayList<>();
    private static SocketClient instance;
    private boolean connected=false;
    private ExecutorService executorService;
    private SocketClient(){
        executorService = Executors.newSingleThreadExecutor();
    }
    public static synchronized SocketClient getInstance(){
        if(instance==null){
            instance= new SocketClient();
        }
        return instance;
    }
    public boolean isConnected(){
        return connected && socket !=null && !socket.isClosed();
    }
    public void addListener(Consumer<Response> listener){
        if(!listeners.contains(listener)){
            listeners.add(listener);
        }
    }
    public void removeListener(Consumer<Respone> listener){
        listeners.remove(listener);
    }
    public void setOnResponseReceived(Consumer<Response> callback){
        listeners.clear();
        addListener(callback);
    }
    public void connect() throws IOException{
        if(isConnected()){
            return;
        }
        try{
            socket = new Socket(HOST,PORT);
            out= new PrintWriter(socket.getOutputStream(),true);
            in= new BufferedReader(new InputStreamReader(socket.getInputStream())));
            connected= true;
            System.out.println("Client da ket noi den server: "+HOST+" : "+PORT);
            
        }
    }


}
