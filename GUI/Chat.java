package GUI;

import java.awt.*;
import java.awt.event.*;
import java.nio.charset.StandardCharsets;
import javax.swing.*;

public class Chat extends JPanel implements ActionListener {
    protected JTextField inputMessage;
    protected JTextField destination;
    protected JButton send;

    protected JTextArea receivedMessage;
    private final static String newline = "\n";

    public Chat() {
        super(new GridBagLayout());
        inputMessage = new JTextField(20);
        destination = new JTextField(20);
        send = new JButton("Send Message");
        send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
//        inputMessage.addActionListener(this);



        receivedMessage = new JTextArea(5, 20);
        receivedMessage.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(receivedMessage);

        //Add Components to this panel.
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;

        c.fill = GridBagConstraints.HORIZONTAL;
        add(inputMessage, c);

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        add(scrollPane, c);
    }


//    public ActionListener pressSend() {
//        byte[] dest = inputMessage.getText().getBytes();
//        String msg = inputMessage.getText();
//    }

    public void actionPerformed(ActionEvent evt) {
        String text = inputMessage.getText();



        receivedMessage.append(text + newline);
        inputMessage.selectAll();

        //Make sure the new text is visible, even if there
        //was a selection in the text area.
        receivedMessage.setCaretPosition(receivedMessage.getDocument().getLength());
    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event dispatch thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("TextDemo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800,500);

        //Add contents to the window.
        frame.add(new Chat());

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        //Schedule a job for the event dispatch thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}