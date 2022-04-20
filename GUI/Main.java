package GUI;
import Network.MyProtocol;

public class Main {

    public static void main(String[] args) {
        MyProtocol protocol = new MyProtocol();
        protocol.start();
    }
}
