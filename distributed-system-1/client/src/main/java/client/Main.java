package client;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hi, I am a client ");


        try {
            Socket socket = new Socket("localhost", 3000);

            InputStream inputStream = socket.getInputStream();

           InputStreamReader inputStreamReader = new InputStreamReader(inputStream);

           BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

           String response = bufferedReader.readLine();

           System.out.println("Sever say " + response);

           OutputStream outputStream = socket.getOutputStream();
           
           PrintWriter printWrite = new PrintWriter(outputStream);

           printWrite.println("Hi, I am Connected to server");

           printWrite.flush();

           printWrite.close();
           socket.close();

            
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
            System.out.println("Failed connection");
        }

    }
}