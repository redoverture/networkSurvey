import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class NetworkRecruiter {
    private ServerSocket semaphoreListener;
    private ServerSocket queryListener;

    private NetworkManager manager;
    private Thread listener;
    boolean threadStop = false;

    public NetworkRecruiter(NetworkManager superior) throws IOException {
        this.manager = superior;
        this.semaphoreListener = new ServerSocket(Constants.SEMAPHORE_PORT);
        this.queryListener = new ServerSocket(Constants.QUERY_PORT);
        listen();
    }

    private void listen(){
        listener = new Thread(() ->{
            Socket clientSemaphoreSocket;
            Socket clientQuerySocket;
            ObjectInputStream semaphoreIn;
            ObjectOutputStream semaphoreOut;
            ObjectOutputStream queryOut;
            ObjectInputStream queryIn;
            Identity identity;
            while(!threadStop){
                try{
                    clientSemaphoreSocket = semaphoreListener.accept();
                    clientQuerySocket = queryListener.accept();
                } catch (IOException e) {
                    System.err.println("Recruiter encountered " + e.toString());
                    return;
                }
                try{
                    semaphoreIn = new ObjectInputStream(clientSemaphoreSocket.getInputStream());
                    semaphoreOut = new ObjectOutputStream(clientSemaphoreSocket.getOutputStream());
                    queryOut = new ObjectOutputStream(clientQuerySocket.getOutputStream());
                    queryIn = new ObjectInputStream(clientQuerySocket.getInputStream());
                    identity = (Identity)semaphoreIn.readObject();
                    semaphoreOut.writeObject(manager.host.identity);
                    semaphoreOut.flush();
                } catch (IOException|ClassNotFoundException e) {
                    System.err.println("Recruiter encountered " + e.toString());
                    return;
                }
                manager.addClient(new NetworkClient(identity, manager, clientSemaphoreSocket, clientQuerySocket, semaphoreIn, semaphoreOut, queryOut, queryIn));
            }});
        listener.start();
    }

    public void close(){
        threadStop = true;
        try {
            semaphoreListener.close();
            queryListener.close();
            listener.interrupt();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
