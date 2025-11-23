package Game.Server;

import Game.UsersUtils.ManipulateFile;
import utils.Messages;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
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
                PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))

        ) {

            String username = null, password;
            boolean loggedIn = false;
            int attempts = 3;

            while (attempts > 0 && !loggedIn) {
                System.out.println("Thread servidor " + this.threadId() + " :À espera do username do novo cliente");
                username = in.readLine();
                if (username == null) {
                    phaser.arriveAndDeregister();
                    return;
                }
                System.out.println("Thread servidor " + this.threadId() + " :À espera da password do novo cliente");
                password = in.readLine();
                if (password == null) {
                    phaser.arriveAndDeregister();
                    return;
                }

                int userLogin = ManipulateFile.validateLogin(username, password);


                if (userLogin == Server.LOGIN_ALREADY_LOGGED) {
                    System.out.println("Thread servidor " + this.threadId() + " :Login falhado, o utilizador " + username + " já efetuou login anteriormente");
                    out.println(Messages.LOGIN_ALREADY_LOGGED.getText());
                    return;
                } else if (userLogin == Server.LOGIN_SUCCESS) {
                    System.out.println("Thread servidor " + this.threadId() + ": O utilizador " + username + " efetuou login com sucesso.");
                    out.println(Messages.LOGIN_SUCCESS.getText());
                    loggedIn = true;
                } else {
                    System.out.println("Thread servidor " + this.threadId() + ": Login falhado (palavra-passe ou username errado) para o utilizador " + username);
                    out.println(Messages.LOGIN_FAILED.getText());
                    attempts--;
                }
            }
            if (!loggedIn) {
                out.println("Número de tentativas: " + attempts + ".");
                phaser.arriveAndDeregister();
                return;
            }
            out.println(Messages.GAME_START.getText());
            try {
                semaphore.acquire();
            } catch (InterruptedException ignored) {
            }
            System.out.println("Thread servidor " + this.threadId() + " :O utilizador " + username + " foi informado que o jogo vai começar");
            out.println("O numero a adivinhar está entre " + Server.MIN + " e " + Server.MAX + "." +
                    "Ganha o primeiro utilizador a adivinhar o número "
                    + "Em qualquer momento, pode introduzir \"Desisto\" para sair do jogo");

            if (Server.GAME_ENDED) {
                System.out.println("Thread servidor " + this.threadId() + ": O jogo terminou.");
                out.println("Sair");
            }


            while (!Server.GAME_ENDED && Server.WINNER_USERNAME == null) {
                try {
                    String input = in.readLine();         // <-- nova tentativa do jogador
                    if (input == null) break;

                    // Se o jogo já terminou por tempo e não há vencedor, avisar **agora** (em resposta à tentativa)
                    if (Server.GAME_ENDED && Server.WINNER_USERNAME == null) {
                        out.println("O tempo do jogo terminou"); // ou Messages.GAME_ENDED_NO_WINNER.getText()
                        break;
                    }

                    // Se já há vencedor, informar e terminar
                    if (Server.WINNER_USERNAME != null) {
                        if (!Server.WINNER_USERNAME.equals(username)) {
                            out.println(String.format("O jogador %s já acertou no número (%d). O jogo terminou.",
                                    Server.WINNER_USERNAME, Server.EXTRACTED_WINNER_NUMBER));
                        }
                        break;
                    }

                    if (input.equalsIgnoreCase("Desisto")) {
                        System.out.println("Thread servidor " + this.threadId() + ": O utilizador " + username + " desistiu do jogo");
                        System.out.println("Thread servidor " + this.threadId() + ": terminou para o utilizador " + username);
                        ManipulateFile.resetLoginStatus(username);
                        if (Server.WINNER_USERNAME != null) {
                            out.println(String.format("O jogador %s já acertou no número (%d). O jogo terminou.",
                                    Server.WINNER_USERNAME, Server.EXTRACTED_WINNER_NUMBER));
                        }
                        break;
                    }

                    int NumberPlayer;
                    try {
                        NumberPlayer = Integer.parseInt(input);
                    } catch (NumberFormatException e) {
                        out.println("Valor inválido. Tente novamente.");
                        continue;
                    }

                    System.out.println("Thread servidor " + this.threadId() + " :O utilizador " + username + " enviou " + NumberPlayer);

                    if (NumberPlayer == Server.EXTRACTED_WINNER_NUMBER) {
                        if (Server.setGameWinner(username)) {
                            out.println(Messages.GAME_WON.getText());
                            System.out.println("Thread servidor " + this.threadId() + " :O utilizador " + username + " acertou no numero");
                            System.out.println("Thread servidor " + this.threadId() + " :terminou para o utilizador " + username);
                            ManipulateFile.resetLoginStatus(username);
                            break;
                        }
                    } else if (NumberPlayer > Server.EXTRACTED_WINNER_NUMBER) {
                        System.out.println("Thread servidor " + this.threadId() + " :O utilizador " + username + " enviou o numero " + NumberPlayer
                                + " que é superior ao número a adivinhar (" + Server.EXTRACTED_WINNER_NUMBER + ")");
                        out.println(Messages.NUMBER_TOO_HIGH.format(NumberPlayer));
                    } else {
                        System.out.println("Thread servidor " + this.threadId() + " :O utilizador " + username + " enviou o numero " + NumberPlayer
                                + " que é inferior ao número a adivinhar (" + Server.EXTRACTED_WINNER_NUMBER + ")");
                        out.println(Messages.NUMBER_TOO_LOW.format(NumberPlayer));
                    }
                } catch (SocketTimeoutException ste) {
                    // Sem nova tentativa: se o jogo já terminou por tempo, fechar silenciosamente.
                    if (Server.GAME_ENDED && Server.WINNER_USERNAME == null) {
                        break; // não enviar aviso aqui; requisito: apenas em resposta a nova tentativa
                    }
                    // caso contrário, continua a esperar por input
                }
            }
            phaser.arriveAndDeregister();

        } catch (IOException e) {
            System.err.println("Thread servidor " + this.threadId() + ": Erro de criação dos buffers do socket");
            phaser.arriveAndDeregister();
        }
    }
}
