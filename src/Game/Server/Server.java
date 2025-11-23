package Game.Server;

import Game.UsersUtils.ManipulateFile;
import utils.InputValidation;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.*;


public class Server {
    private final static int N_THREADS = 10;
    private final static int SOCKET_TIMEOUT = 30000;
    private final static Phaser Phaser = new Phaser();
    private static final Semaphore Semaphore = new Semaphore(0);
    public static volatile int EXTRACTED_WINNER_NUMBER;
    public static int MIN;
    public static int MAX;
    private static final int PORT = 6000;
    private final static int MAX_TIME_GAME = 120;
    public static volatile boolean GAME_ENDED = false;
    public static volatile String WINNER_USERNAME = null;
    public static final int LOGIN_SUCCESS = 1;
    public static final int LOGIN_ALREADY_LOGGED = 0;


    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        ManipulateFile.initUsers();

        int n_Players = 0;

        MIN = InputValidation.validateIntGE0(sc, "Main: Por favor, introduza o valor mínimo a ser indicado aos utilizadores:> ");
        MAX = InputValidation.validateIntGTN(sc, "Main: Por favor, introduza o valor máximo a ser indicado aos utilizadores:> ", MIN);


        try (
                ServerSocket serverSocket = new ServerSocket(PORT);
                ExecutorService executor = Executors.newFixedThreadPool(N_THREADS);
        ) {
            serverSocket.setSoTimeout(SOCKET_TIMEOUT);
            while (true) {
                System.out.println("Main: À espera de ligações....");
                try {
                    Socket clientSocket = serverSocket.accept();
                    n_Players++;
                    System.out.println("Main: Nova ligação");
                    executor.execute(new ServerThread(clientSocket, Phaser, Semaphore));

                } catch (SocketTimeoutException e) {
                    System.out.println("Main: Acabou o tempo para entrar no jogo");
                    EXTRACTED_WINNER_NUMBER = new Random().nextInt(MAX - MIN + 1) + MIN;
                    //EXTRACTED_WINNER_NUMBER= new Random().nextInt(MIN,MAX);
                    System.out.println("Main: número a adivinhar gerado");
                    Semaphore.release(n_Players);

                    // Necessário para funcionar o awaitTermination abaixo
                    //Para controlar que todas as threads terminam, qualquer dos dois funcionam. Porque? duvida!
                    //executor.shutdown();
                    executor.shutdownNow();
                    break;
                }

            }
            if (n_Players == 0) {
                System.out.println("Main: Nenhum jogador entrou. A encerrar.");
                executor.shutdownNow();
                return;
            }
            //Estava aqui a extrair o número;

            if (!executor.awaitTermination(MAX_TIME_GAME, TimeUnit.SECONDS)) {
                GAME_ENDED = true;
                System.out.println("Main: O tempo do jogo terminou");
                if (WINNER_USERNAME == null) {
                    System.out.println("Main: Não houve vencedores");
                }
                executor.shutdownNow();
                return;
            }
            if (WINNER_USERNAME != null) {
                System.out.println("Main: O utilizador " + WINNER_USERNAME + " ganhou");
            }


        } catch (IOException e) {
            System.err.println("Main: Ocorreu um erro de I/O ao tentar criar o socket no porto " + PORT);
            System.exit(2);
        } catch (InterruptedException e) {
            System.err.println("Main: Ocorreu um erro em awaitTermination");
            System.exit(3);
        }


        sc.close();

    }

    public static synchronized boolean setGameWinner(String username) {
        if (WINNER_USERNAME == null && !GAME_ENDED) {
            WINNER_USERNAME = username;
            GAME_ENDED = true;
            return true;
        }
        return false;
    }
}
