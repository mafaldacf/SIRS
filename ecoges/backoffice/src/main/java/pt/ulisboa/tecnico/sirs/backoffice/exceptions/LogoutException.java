package pt.ulisboa.tecnico.sirs.backoffice.exceptions;

public class LogoutException extends Exception {

    private static final long serialVersionUID = 1L;

    public LogoutException() {
        super("Could not logout.");
    }
}
