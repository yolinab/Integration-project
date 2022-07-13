import Network.MyProtocol;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        MyProtocol protocol = new MyProtocol();
        protocol.start();
    }
}
