package Game.Server;

import utils.InputValidation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.*;


public class Server {
    private final static int N_THREADS = 10;
    private final static int SOCKET_TIMEOUT = 20000;
    private final static Phaser Phaser = new Phaser();
    private static final Semaphore Semaphore = new Semaphore(0);
    public static volatile int EXTRACTED_WINNER_NUMBER;
    public static int MIN;
    public static int MAX;
    private static final int PORT = 6000;
    public static boolean ALREADY_LOGGED_IN =false;
    public static boolean SUCCESS_LOGIN =false;
    public static boolean ERROR_LOGIN =false;
    public static boolean NO_ATTEMPTS =false;
    private final static int MAX_TIME_GAME=120;
    public static volatile boolean GAME_ENDED=false;
    public static volatile String WINNER_USERNAME = null;
    public static final String fullfilename= System.getProperty("user.dir")+"\\src\\Game\\Users\\users.txt";
    public static List<String[]> users;


    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        users = ReadUsers();

        int n_Players = 0;

        MIN=InputValidation.validateIntGE0(sc,"Main: Por favor, introduza o valor mínimo a ser indicado aos utilizadores:> ");
        MAX=InputValidation.validateIntGTN(sc,"Main: Por favor, introduza o valor máximo a ser indicado aos utilizadores:> ",MIN);



        try(
                ServerSocket serverSocket = new ServerSocket(PORT);
                ExecutorService executor = Executors.newFixedThreadPool(N_THREADS);
                )
        {
            serverSocket.setSoTimeout(SOCKET_TIMEOUT);
            while(true) {
                System.out.println("Main: À espera de ligações....");
                try{
                    Socket clientSocket = serverSocket.accept();
                    n_Players++;
                    System.out.println("Nova ligação");
                    executor.execute(new ServerThread(clientSocket,Phaser,Semaphore));


                }catch(SocketTimeoutException e){
                    System.out.println("Main: Acabou o tempo para entrar no jogo");
                    Semaphore.release(n_Players);

                    // Necessário para funcionar o awaitTermination abaixo
                    executor.shutdownNow();
                    break;
                }

            }
            if(n_Players==0){
                System.out.println("Main: Nenhum jogador entrou. A encerrar.");
                executor.shutdownNow();
                return;
            }
            EXTRACTED_WINNER_NUMBER= new Random().nextInt(MIN,MAX);
            System.out.println("Main: número a adivinhar gerado");
            // EXTRACTED_WINNER_NUMBER = new Random().nextInt(max - min + 1) + min;

            if(!executor.awaitTermination(MAX_TIME_GAME, TimeUnit.SECONDS)){
                GAME_ENDED=true;
                System.out.println("Main: O tempo do jogo terminou");
                executor.shutdownNow();
                return;
            }
            if (WINNER_USERNAME != null) {
                System.out.println("Main: O utilizador " + WINNER_USERNAME + " ganhou");
            }


        }catch (IOException e) {
            System.err.println("Main: Ocorreu um erro de I/O ao tentar criar o socket no porto " + PORT);
            System.exit(2);
        } catch (InterruptedException e) {
            System.err.println("Main: Ocorreu um erro em awaitTermination");
            System.exit(3);
        }


        sc.close();

    }
    private void setUserLoggedIn(String username){
        synchronized(Server.users){
            for(String[] user : Server.users){
                if (user[0].equals(username)) {
                    user[2] = "1"; // marca como logado
                    break;
                }
            }
            try {
                List<String> text=new ArrayList<>();
                for(String[] user : Server.users){
                    text.add(String.join(";", user));
                }
                Files.write(Paths.get(Server.fullfilename), text);
            }catch (IOException e){
                System.out.println("Não foi possível atualizar o login do user: " + e.getMessage());
            }
        }
    }
    private static List<String[]> ReadUsers(){
        try{
            List<String> usersFile = Files.readAllLines(Paths.get(fullfilename));
            List <String[]> users = new ArrayList<>();

            for(String line : usersFile){
                String[] user = line.split(";");
                if(user.length==3){
                    if(!user[0].isEmpty() && !user[1].isEmpty() && !user[2].isEmpty()){
                        if(user[2].equals("0") || user[2].equals("1")){
                            users.add(user);
                        }
                    }
                }
            }
            System.out.println("Carregados " + users.size() + " utilizadores");
            return users;

        } catch (IOException e) {
            throw new RuntimeException("Erro ao carregar utilizadores: " + e.getMessage(), e);
        }
    }
    public static synchronized boolean validateLogin(String username, String password){
        for(String[] user : users){
            if(user[0].equals(username) && user[1].equals(password)){
                return !user[2].equals("1");
            }
        }
        return false;
    }
    public static synchronized boolean isAlreadyLoggedIn(String username){
        for(String[] user : users){
            return (user[0].equals(username) && user[2].equals("1"));
        }
        return false;
    }

    public static synchronized void setUserLoggedIn(String username,boolean islog){
        boolean found=false;
        for(String[] user : users){
            if(user[0].equals(username)){
                user[2] = islog?"1":"0";
                found=true;
                break;
            }

        }
        if(!found)return;

        try {
            List<String> text = new ArrayList<>();
            for (String[] user : users) {
                text.add(String.join(";", user));
            }
            Files.write(Paths.get(fullfilename), text);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível atualizar o login do user: " + e.getMessage(), e);
        }


    }
}
