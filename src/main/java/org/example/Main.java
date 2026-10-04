package org.example;

import org.example.io.Reader;
import org.example.io.Writer;

import java.net.ServerSocket;
import java.net.Socket;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    static void main(String[] args) {
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.
        ServerSocket ss;
        try {
            ss = new ServerSocket(Integer.parseInt(args[0]));
            Socket s;
            while(true) {
                s = ss.accept();
                Server server = new Server();
                Reader reader = new Reader(s.getInputStream());
                Writer rw = new Writer(s.getInputStream(), s.getOutputStream());
                try {
                    rw.send(server.respond(reader.receive()));
                } catch(Exception e) {
                    e.printStackTrace();
                }
                finally {
                    s.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
