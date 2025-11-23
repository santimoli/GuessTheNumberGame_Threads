package Game.Player;

import utils.Messages;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Player {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String hostname = "localhost";
        int PortNumber = 6000;

        try (
                Socket clientSocket = new Socket(hostname, PortNumber);
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
        ) {

            int Attempts = 3;
            while (Attempts > 0) {

                System.out.println("Username:> ");
                String username = sc.nextLine();
                out.println(username);

                System.out.println("Password:> ");
                String password = sc.nextLine();
                out.println(password);

                String logStatus = in.readLine();
                if (logStatus == null) {
                    System.out.println("Servidor terminou ligação.");
                    return;
                }

                System.out.println(logStatus);

                if (logStatus.equalsIgnoreCase(Messages.LOGIN_ALREADY_LOGGED.getText())) {
                    return;
                }

                if (logStatus.equalsIgnoreCase(Messages.LOGIN_SUCCESS.getText())) {
                    break;
                }

                if (logStatus.equalsIgnoreCase(Messages.LOGIN_FAILED.getText())) {
                    Attempts--;
                    System.out.println("Tentativas restantes: " + Attempts);
                    if (Attempts == 0) {
                        System.out.println("Demasiadas tentativas. Ligação encerrada.");
                        return;
                    }
                }
            }

            // Ler mensagens iniciais do servidor (podem ser 1 ou mais)
            while (true) {
                String initialMsg = in.readLine();
                if (initialMsg == null) {
                    System.out.println("Servidor terminou a ligação.");
                    return;
                }

                System.out.println(initialMsg);

                // Parar se já for mensagem de fim de jogo
                if (initialMsg.contains("já acertou") ||
                        initialMsg.equalsIgnoreCase("Sair") ||
                        initialMsg.equalsIgnoreCase(Messages.GAME_ENDED_NO_WINNER.getText())) {
                    return;
                }

                // Quando recebe a descrição do intervalo, parar de ler mensagens iniciais
                if (initialMsg.startsWith("O numero a adivinhar está entre"))
                    break;
            }

            // Loop principal do jogo
            while (true) {

                System.out.println("Por favor introduza o seu palpite: ");
                String NumberPlayer = sc.nextLine();
                try {
                    out.println(NumberPlayer);
                } catch (Exception e) {
                    System.out.println("Não foi possível enviar o palpite. Encerrando cliente.");
                    break;
                }

                // Se o jogador desistiu, basta ler UMA resposta
                if (NumberPlayer.equalsIgnoreCase("Desisto")) {
                    String response = in.readLine();
                    if (response != null) {
                        System.out.println(response);
                    }
                    break;
                }

                String ResponseServer = in.readLine();
                if (ResponseServer == null) {
                    System.out.println("Servidor terminou a ligação.");
                    break;
                }

                System.out.println(ResponseServer);

                // Condições de fim de jogo
                if (ResponseServer.contains("já acertou") ||
                        ResponseServer.equalsIgnoreCase(Messages.GAME_WON.getText()) ||
                        ResponseServer.equalsIgnoreCase(Messages.EXIT.getText()) ||
                        ResponseServer.equalsIgnoreCase(Messages.GAME_ENDED_NO_WINNER.getText()) ||
                        ResponseServer.contains("tempo do jogo terminou")) {  // Adicionar esta verificação
                    //ResponseServer.equalsIgnoreCase("O tempo do jogo terminou")) Sera necessaria esta linha?
                    break;
                }
            }

        } catch (UnknownHostException e) {
            System.err.println("Host desconhecido: " + hostname);
            System.exit(2);
        } catch (IOException e) {
            if(e instanceof java.net.ConnectException) {
                System.out.println("Não foi possível ligar ao servidor: já não estão a ser aceites novos jogadores.");
            }else{
                System.err.println("Erro de IO");
            }
            System.exit(3);
        }

        sc.close();
    }
}
