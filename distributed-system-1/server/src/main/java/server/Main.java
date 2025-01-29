package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        System.out.println("Server running!");

        try {
            ServerSocket serverSocket = new ServerSocket(3000);

            Socket socket = serverSocket.accept();

            OutputStream outputStream = socket.getOutputStream();
            
            PrintWriter printWriter = new PrintWriter(outputStream, true);

            printWriter.println("Connected to server");


            InputStream inputStream = socket.getInputStream();

            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);

            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            String response = bufferedReader.readLine();

            System.out.println("Client say " +  response);

            printWriter.flush();

            printWriter.close();
            socket.close();
            serverSocket.close();


        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.out.println("Connection Failed");
        }
    }
}