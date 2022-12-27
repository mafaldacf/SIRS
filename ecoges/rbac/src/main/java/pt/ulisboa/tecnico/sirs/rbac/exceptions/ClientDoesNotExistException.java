package pt.ulisboa.tecnico.sirs.rbac.exceptions;

public class ClientDoesNotExistException extends Exception {

    private static final long serialVersionUID = 1L;

    public ClientDoesNotExistException(String email) {
        super("Client with email '" + email + "' does not exist.");
    }
}
