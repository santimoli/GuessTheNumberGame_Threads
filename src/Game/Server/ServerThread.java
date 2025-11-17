package Game.Server;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.Phaser;
import java.util.concurrent.Semaphore;

public class ServerThread extends Thread {
    private final Socket clientSocket;
    private final Phaser phaser;
    private final Semaphore semaphore;


    public ServerThread(Socket clientSocket, Phaser phaser, Semaphore semaphore) {
        this.clientSocket = clientSocket;
        this.phaser = phaser;
        this.semaphore = semaphore;

        phaser.register();
    }

    public void run() {
        try (
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(),true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))

                ) {

            String username = null, password;
            boolean loggedIn= false;
            int attempts = 3;

            while (attempts>0 && !loggedIn) {
                System.out.println("Thread servidor " + this.threadId() + " À espera do username do novo cliente");
                username = in.readLine();
                if(username==null) return;
                System.out.println("Thread servidor "+ this.threadId() + " À espera da password do novo cliente");
                password= in.readLine();
                if(password==null) return;

                if(Server.isAlreadyLoggedIn(username)){
                    out.println("Já efetuou Login no jogo anteriormente");
                    Server.ALREADY_LOGGED_IN=true;
                    return;
                }
                if(Server.validateLogin(username, password)){
                    out.println("Login efetuado com sucesso");
                    Server.SUCCESS_LOGIN=true;
                    loggedIn=true;
                }else {
                    out.println("Nome de utilizador ou palavra passe errada");
                    Server.ERROR_LOGIN=true;
                    attempts--;
                }
            }
            if(!loggedIn){
                out.println("Número de tentativas: " + attempts + ".");
                phaser.arriveAndDeregister();
                return;
            }
            out.println("Por favor, aguarde até terminar o tempo para a entrada de novos jogadores");
            try {
                semaphore.acquire();
            }catch (InterruptedException ignored){
            }
            System.out.println("Thread servidor " + this.threadId() + " :O utilizador " + username + " foi informado que o jogo vai começar");
            out.println("O numero a adivinhar está entre " + Server.MIN + " e "+ Server.MAX + "." +
                    "Ganha o primeiro utilizador a adivinhar o número "
                    + "Em qualquer momento, pode introduzir \"Desisto\" para sair do jogo");


            while(Server.WINNER_USERNAME == null){

                if (Server.GAME_ENDED) {
                    System.out.println("Thread servidor " + this.threadId() + ": O jogo terminou.");
                    out.println("Sair");
                    break;
                }
                String text = in.readLine();
                if(text.equalsIgnoreCase("Desisto")){
                    System.out.println("Thread servidor " + this.threadId() + ": O utilizador " + username + " desistiu do jogo");
                    System.out.println("Thread servidor " + this.threadId() + ": terminou para o utilizador " + username);
                    break;
                }

                int NumberPlayer;
                try {
                    NumberPlayer = Integer.parseInt(text);
                } catch (NumberFormatException e) {
                    out.println("[SERVER] Valor inválido. Tente novamente.");
                    continue;
                }
                System.out.println("Thread servidor " + this.threadId() + " :O utilizador " + username + " enviou " + NumberPlayer);

                if(NumberPlayer==Server.EXTRACTED_WINNER_NUMBER){
                    out.println("Parabéns, acertou no número");
                    System.out.println("O utilizador " + username + " acertou no numero");
                    Server.setGameWinner(username);
                    System.out.println("Thread servidor: " + this.threadId() + " terminou para o utilizador " + username);
                    phaser.arriveAndDeregister();
                    break;
                }else if( NumberPlayer > Server.EXTRACTED_WINNER_NUMBER){
                    System.out.println("Thread servidor " + this.threadId() + " :O utilizador " + username + " enviou o numero " + NumberPlayer +
                            " que é superior ao número a adivinhar (" + Server.EXTRACTED_WINNER_NUMBER + ")");
                    out.println("O número " + NumberPlayer + " é superior ao número a adivinhar");
                } else if (NumberPlayer < Server.EXTRACTED_WINNER_NUMBER) {
                    System.out.println("Thread servidor " + this.threadId() + " :O utilizador " + username + " enviou o numero " + NumberPlayer +
                            " que é inferior ao número a adivinhar (" + Server.EXTRACTED_WINNER_NUMBER + ")");
                    out.println("O número " + NumberPlayer + " é inferior ao número a adivinhar");
                }
            }
            //phaser.arriveAndAwaitAdvance();
            phaser.arriveAndDeregister();

        } catch (IOException e) {
            System.err.println("Thread servidor " + this.threadId() + ": Erro de criação dos buffers do socket");
            phaser.arriveAndDeregister();
        }
    }
    private boolean IsLogIn(String username, String password){
        for(String[] user : Server.users){
            if(user[0].equals(username) && user[1].equals(password)){
                if(user[2].equals("1")){
                    return true;
                }
                if(user[2].equals("0")){
                    return false;
                }
            }
        }
        return false;
    }

}
