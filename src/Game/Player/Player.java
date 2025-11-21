package Game.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Player {
    public static void main(String[] args) {
        Scanner sc=new Scanner(System.in);
        System.out.println("Introduza o valor do servidor ao qual se vai ligar: ");
        String hostname = "localhost";


        int PortNumber = 6000;
        try(
                Socket clientSocket = new Socket(hostname, PortNumber);
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(),true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                ){


            int Attempts=3;
            boolean loggedIn=false;
            while (Attempts>0){

                System.out.println("Username:> ");
                String username = sc.nextLine();
                out.println(username);
                System.out.println("Password:> ");
                String password = sc.nextLine();
                out.println(password);

                String logStatus = in.readLine();
                System.out.println(logStatus);



                if(logStatus.equalsIgnoreCase("Já efetuou Login no jogo anteriormente")){
                    return;
                }
                if(logStatus.equalsIgnoreCase("Login efetuado com sucesso")){
                    loggedIn=true;
                    break;
                }
                if (logStatus.equals("Nome de utilizador ou palavra passe errada")) {
                    Attempts--;
                    if (Attempts == 0) {
                        System.out.println("Demasiadas tentativas. Ligação encerrada.");
                        return;
                    }
                    System.out.println("Tentativas restantes: " + Attempts);
                }
            }

            String messageServer;
            messageServer=in.readLine();
            System.out.println(messageServer);

            String rulesServer= in.readLine();
            System.out.println(rulesServer);

            while(true){

                String NumberPlayer;
                System.out.println("Por favor introduza o seu palpite: ");
                NumberPlayer = sc.nextLine();
                out.println(NumberPlayer);

                String ResponseServer= in.readLine();
                if(ResponseServer == null)break;
                System.out.println(ResponseServer);

                if (ResponseServer.contains("Parabéns") ||
                        ResponseServer.contains("acertou no número") ||
                        ResponseServer.contains("Sair") ||
                        ResponseServer.equalsIgnoreCase("Desisto") ||
                        NumberPlayer.equalsIgnoreCase("Desisto") ||
                        ResponseServer.equalsIgnoreCase("O tempo do jogo terminou, não houve vencedor.")){

                    break;
                }

            }

        } catch (UnknownHostException e) {
            System.err.println("Host desconhecido: " + hostname);
            System.exit(2);
        } catch (IOException e) {
            System.err.println("Erro de IO");
            System.exit(3);
        }
        sc.close();
    }
}
