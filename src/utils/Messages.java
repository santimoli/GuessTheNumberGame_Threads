package utils;

public enum Messages {
    LOGIN_SUCCESS("Login efetuado com sucesso"),
    LOGIN_ALREADY_LOGGED("Já efetuou Login no jogo anteriormente"),
    LOGIN_FAILED("Nome de utilizador ou palavra passe errada"),
    GAME_START("Por favor, aguarde até terminar o tempo para a entrada de novos jogadores"),
    GAME_NUMBER_INFO("O numero a adivinhar está entre %d e %d. Ganha o primeiro utilizador a adivinhar o número. Em qualquer momento, pode introduzir \"Desisto\" para sair do jogo"),
    GAME_WON("Parabéns, acertou no número"),
    GAME_ENDED_NO_WINNER("O tempo do jogo terminou, não houve vencedor."),
    OTHER_PLAYER_WON("já acertou"),
    NUMBER_TOO_HIGH("O número %d é superior ao número a adivinhar"),
    NUMBER_TOO_LOW("O número %d é inferior ao número a adivinhar"),
    EXIT("Sair");


    private final String text;

    Messages(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public String format(Object... args) {
        return String.format(text, args);
    }
}