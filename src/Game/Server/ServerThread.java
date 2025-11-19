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
                    System.out.println("Thread servidor " + this.threadId() + ": Login falhado, o utilizador " + username + " já efetuou login anteriormente");
                    out.println("Já efetuou Login no jogo anteriormente");
                    return;
                }
                if(Server.validateLogin(username, password)){
                    System.out.println("Thread servidor " + this.threadId() + ": O utilizador " + username + " efetuou login com sucesso.");
                    out.println("Login efetuado com sucesso");
                    Server.setUserLoggedIn(username,true);
                    loggedIn=true;
                }else {
                    System.out.println("Thread servidor " + this.threadId() + ": Login falhado (palavra-passe ou username errado) para o utilizador " + username);
                    out.println("Nome de utilizador ou palavra passe errada");
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

            if (Server.GAME_ENDED) {
                System.out.println("Thread servidor " + this.threadId() + ": O jogo terminou.");
                out.println("Sair");
            }

            while(!Server.GAME_ENDED && Server.WINNER_USERNAME==null){

                String text = in.readLine();
                if(text==null) break;

                if(Server.GAME_ENDED){
                    if(Server.WINNER_USERNAME==null){
                        out.println("O tempo do jogo terminou, não houve vencedor.");
                    }
                    break;
                }

                if(text.equalsIgnoreCase("Desisto")){
                    System.out.println("Thread servidor " + this.threadId() + ": O utilizador " + username + " desistiu do jogo");
                    System.out.println("Thread servidor " + this.threadId() + ": terminou para o utilizador " + username);
                    break;
                }

                if(Server.WINNER_USERNAME!=null){
                    out.println("O jogador " + Server.WINNER_USERNAME +
                            " já acertou no número (" + Server.EXTRACTED_WINNER_NUMBER + ")");
                    break;
                }


                int NumberPlayer;
                try {
                    NumberPlayer = Integer.parseInt(text);
                } catch (NumberFormatException e) {
                    out.println("Valor inválido. Tente novamente.");
                    continue;
                }
                System.out.println("Thread servidor " + this.threadId() + " :O utilizador " + username + " enviou " + NumberPlayer);


                if(NumberPlayer==Server.EXTRACTED_WINNER_NUMBER){

                    if(Server.setGameWinner(username)){
                        out.println("Parabéns, acertou no número");
                        System.out.println("Thread servidor " + this.threadId() + " :O utilizador " + username + " acertou no numero");
                        System.out.println("Thread servidor " + this.threadId() + " :terminou para o utilizador " + username);
                        //phaser.arriveAndDeregister();
                        break;
                    }

                }else if( NumberPlayer > Server.EXTRACTED_WINNER_NUMBER){
                    System.out.println("Thread servidor " + this.threadId() + " :O utilizador " + username + " enviou o numero " + NumberPlayer +
                            " que é superior ao número a adivinhar (" + Server.EXTRACTED_WINNER_NUMBER + ")");
                    out.println("O número " + NumberPlayer + " é superior ao número a adivinhar");
                }else{
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
}
