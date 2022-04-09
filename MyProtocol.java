import Network.Node;
import client.*;

import java.util.concurrent.BlockingQueue;

/**
* This is just some example code to show you how to interact 
* with the server using the provided 'Client' class and two queues.
* Feel free to modify this code in any way you like!
*/

public class MyProtocol{

    // The host to connect to. Set this to localhost when using the audio interface tool.
    private static String SERVER_IP = "netsys.ewi.utwente.nl"; //"127.0.0.1";
    // The port to connect to. 8954 for the simulation server.
    private static int SERVER_PORT = 8954;
    // The frequency to use.
    private static int frequency = 5300;

    private BlockingQueue<Message> receivedQueue;
    private BlockingQueue<Message> sendingQueue;

    public MyProtocol(String server_ip, int server_port, int frequency){

    }

    public static void main(String args[]) {
        new Node();
    }

}

