package pt.ulisboa.tecnico.sirs.backoffice;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.sirs.backoffice.exceptions.*;
import pt.ulisboa.tecnico.sirs.backoffice.grpc.*;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.sql.SQLException;
import java.util.List;

public class BackofficeServiceImpl extends ServerServiceGrpc.ServerServiceImplBase {
	private static Backoffice server;

	public BackofficeServiceImpl(Backoffice backofficeServer) throws SQLException, ClassNotFoundException {
		server = backofficeServer;
	}

	@Override
	public void register(RegisterRequest request, StreamObserver<AckResponse> responseObserver) {
		AckResponse.Builder builder = AckResponse.newBuilder();
		try {
			server.register(request.getUsername(), request.getPassword(), request.getRole().name());

			builder.setAck(true);

			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (SQLException e){
			responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
		} catch (AdminAlreadyExistsException e){
			responseObserver.onError(Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException());
		}
	}

	@Override
	public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
		LoginResponse.Builder builder = LoginResponse.newBuilder();
		try {
			List<String> response = server.login(request.getUsername(), request.getPassword());

			builder.setRole(RoleType.valueOf(response.get(0)));
			builder.setHashedToken(response.get(1));

			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (SQLException e){
			responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
		} catch (AdminDoesNotExistException e){
			responseObserver.onError(Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException());
		} catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException | WrongPasswordException e){
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		}
	}

	@Override
	public void logout(LogoutRequest request, StreamObserver<AckResponse> responseObserver) {
		AckResponse.Builder builder = AckResponse.newBuilder();
		try {
			boolean ack = server.logout(request.getUsername(), request.getHashedToken());

			builder.setAck(ack);

		} catch (SQLException | RuntimeException | AdminDoesNotExistException | InvalidSessionTokenException e) {
			// do nothing, admin should be able to log out
			builder.setAck(true);
		}

		responseObserver.onNext(builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void listClients(ListClientsRequest request, StreamObserver<ListClientsResponse> responseObserver) {
		ListClientsResponse.Builder builder = ListClientsResponse.newBuilder();
		try {
			List<Client> clients = server.listClients(request.getUsername(), request.getHashedToken());

			builder.addAllClients(clients);

			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (SQLException e){
			responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
		} catch (AdminDoesNotExistException e) {
			responseObserver.onError(Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException());
		} catch (InvalidSessionTokenException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		}
	}

	@Override
	public void checkPersonalInfo(CheckPersonalInfoRequest request, StreamObserver<CheckPersonalInfoResponse> responseObserver) {
		CheckPersonalInfoResponse.Builder builder = CheckPersonalInfoResponse.newBuilder();
		try {
			PersonalInfo personalInfo = server.checkPersonalInfo(request.getUsername(), request.getEmail(), request.getHashedToken());

			builder.setPersonalInfo(personalInfo);

			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (SQLException e){
			responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
		} catch (InvalidRoleException | AdminDoesNotExistException | InvalidSessionTokenException | ClientDoesNotExistException e){
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		} catch (PermissionDeniedException e) {
			responseObserver.onError(Status.PERMISSION_DENIED.withDescription(e.getMessage()).asRuntimeException());
		}
	}

	@Override
	public void checkEnergyPanel(CheckEnergyPanelRequest request, StreamObserver<CheckEnergyPanelResponse> responseObserver) {
		CheckEnergyPanelResponse.Builder builder = CheckEnergyPanelResponse.newBuilder();
		try {
			EnergyPanel energyPanel = server.checkEnergyPanel(request.getUsername(), request.getEmail(), request.getHashedToken());

			builder.setEnergyPanel(energyPanel);

			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (SQLException e){
			responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
		} catch (InvalidRoleException | AdminDoesNotExistException | InvalidSessionTokenException | ClientDoesNotExistException e){
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		} catch (PermissionDeniedException e) {
			responseObserver.onError(Status.PERMISSION_DENIED.withDescription(e.getMessage()).asRuntimeException());
		}
	}

	@Override
	public void deleteClient(DeleteClientRequest request, StreamObserver<AckResponse> responseObserver) {
		AckResponse.Builder builder = AckResponse.newBuilder();
		try {
			boolean ack = server.deleteClient(request.getUsername(), request.getEmail(), request.getHashedToken());

			builder.setAck(ack);

			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (SQLException e){
			responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
		} catch (AdminDoesNotExistException | ClientDoesNotExistException | InvalidSessionTokenException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		}
	}
}
