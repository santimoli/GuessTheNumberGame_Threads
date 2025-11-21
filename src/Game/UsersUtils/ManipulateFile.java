package Game.UsersUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ManipulateFile {

    public static final String fullfilename= System.getProperty("user.dir")+"\\src\\Game\\UsersUtils\\users.txt";
    public static List<String[]> users;

    public static void initUsers() {
        users = readUsers();
        resetAllLogins();
    }

    public static List<String[]> readUsers(){
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

    public static synchronized void writeUsersToFile() {
        try {
            List<String> text = new ArrayList<>();
            for (String[] user : users) {
                text.add(String.join(";", user));
            }
            Files.write(Paths.get(fullfilename), text);
        } catch (IOException e) {
            throw new UncheckedIOException("Erro ao escrever utilizadores: " + e.getMessage(), e);
        }
    }

    public static synchronized int validateLogin(String username, String password) {

        for(String[] user : users) {
            if(user[0].equals(username) && user[1].equals(password)) {
                if(user[2].equals("1")) {
                    return 0;// Já está logado
                }
                else {
                    user[2] = "1"; // Marca como logado
                    writeUsersToFile();
                    return 1;
                }
            }
        }
        // Utilizador não encontrado,username ou password errada
        return -1;
    }


    public static synchronized void resetAllLogins() {
        for (String[] user : users) {
            user[2] = "0";
        }

        writeUsersToFile();
    }

    public static synchronized void resetLoginStatus(String username){
        for(String[] user : users){
            if(user[0].equals(username)) {
                user[2] = "0";
                break;
            }
        }

        writeUsersToFile();
    }

}
