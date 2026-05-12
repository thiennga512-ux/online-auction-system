import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
public class ClientHandler implements Runnable{
    private finalSocket socket;
    private final BufferedReader in;
    private final BufferedWriter out;
    private final object writeLock= new Object();
    
}