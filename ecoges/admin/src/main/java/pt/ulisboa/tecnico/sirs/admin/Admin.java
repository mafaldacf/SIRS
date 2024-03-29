package pt.ulisboa.tecnico.sirs.admin;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import pt.ulisboa.tecnico.sirs.backoffice.grpc.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class Admin {

	private static ManagedChannel channel;
	private static BackofficeServiceGrpc.BackofficeServiceBlockingStub server;

	// TLS
	private static final String TRUST_STORE_FILE = "src/main/resources/admin.truststore";
	private static final String TRUST_STORE_PASSWORD = "mypassadmin";
	private static final String TRUST_STORE_ALIAS_CA = "ca";
	private static final String TRUST_STORE_ALIAS_BACKOFFICE = "backoffice";

	public static void init(String host, int port) throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {
		String target = host + ":" + port;

		KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
		trustStore.load(Files.newInputStream(Paths.get(TRUST_STORE_FILE)), TRUST_STORE_PASSWORD.toCharArray());
		X509Certificate CACertificate = (X509Certificate) trustStore.getCertificate(TRUST_STORE_ALIAS_CA);
		X509Certificate webserverCertificate = (X509Certificate) trustStore.getCertificate(TRUST_STORE_ALIAS_BACKOFFICE);

		// Setup ssl context
		SslContext sslContext = GrpcSslContexts.configure(SslContextBuilder.forClient().trustManager(webserverCertificate, CACertificate)).build();

		channel = NettyChannelBuilder.forTarget(target).sslContext(sslContext).build();
		server = BackofficeServiceGrpc.newBlockingStub(channel);
	}

	public static void close(){
		channel.shutdown();
	}

	public static boolean register(String username, String password, int role) {
		try {
			RegisterRequest request = RegisterRequest.newBuilder()
					.setUsername(username)
					.setPassword(password)
					.setRole(RoleType.forNumber(role-1))
					.build();
			AckResponse response = server.register(request);
			return(response.getAck());
		} catch (StatusRuntimeException e) {
			System.out.println(e.getMessage());
		}
		return false;
	}

	public static List<String> login(String username, String password) {
		List<String> cred = new ArrayList<>();
		try {
			LoginRequest request = LoginRequest.newBuilder()
					.setUsername(username)
					.setPassword(password)
					.build();
			LoginResponse response = server.login(request);
			cred.add(response.getRole().name());
			cred.add(response.getHashedToken());
			return cred;
		} catch (StatusRuntimeException e) {
			System.out.println(e.getMessage());
		}
		return null;
	}

	public static boolean logout(String username, String hashedToken) {
		try {
			LogoutRequest request = LogoutRequest.newBuilder()
					.setUsername(username)
					.setHashedToken(hashedToken)
					.build();
			AckResponse response = server.logout(request);
			return(response.getAck());
		} catch (StatusRuntimeException e) {
			System.out.println(e.getMessage());
		}
		return false;
	}

	public static String listClients(String username, String hashedToken) {
		String clients = "";
		try {
			ListClientsRequest request = ListClientsRequest.newBuilder()
					.setUsername(username)
					.setHashedToken(hashedToken)
					.build();
			ListClientsResponse response = server.listClients(request);

			for (Client client: response.getClientsList()) {
				clients += "Name: " + client.getName()
						+ ", Email: " + client.getEmail()
						+ "\n";
			}
		} catch (StatusRuntimeException e) {
			System.out.println(e.getMessage());
		}
		return clients;
	}

	public static String checkClientPersonalInfo(String username, String email, String hashedToken) {
		String result = "";
		try {
			CheckPersonalInfoRequest request = CheckPersonalInfoRequest.newBuilder()
					.setUsername(username)
					.setEmail(email)
					.setHashedToken(hashedToken)
					.build();
			CheckPersonalInfoResponse response = server.checkPersonalInfo(request);
			PersonalInfo personalInfo = response.getPersonalInfo();
			result += "Name: " + personalInfo.getName() + "\n"
					+ "Email: " + personalInfo.getEmail() + "\n"
					+ "Address: " + personalInfo.getAddress() + "\n"
					+ "IBAN: " + personalInfo.getIBAN() + "\n"
					+ "Energy plan: " + personalInfo.getPlan().name();
		} catch (StatusRuntimeException e) {
			System.out.println(e.getMessage());
		}
		return result;
	}

	public static String checkClientEnergyPanel(String username, String email, String hashedToken) {
		String result = "";
		try {
			CheckEnergyPanelRequest request = CheckEnergyPanelRequest.newBuilder()
					.setUsername(username)
					.setEmail(email)
					.setHashedToken(hashedToken)
					.build();
			CheckEnergyPanelResponse response = server.checkEnergyPanel(request);

			EnergyPanel energyPanel = response.getEnergyPanel();
			result += "Total Energy Consumed: " + energyPanel.getEnergyConsumed() + " kWh\n"
					+ "Total Energy Consumed Daytime: " + energyPanel.getEnergyConsumedDaytime() + " kWh \n"
					+ "Total Energy Consumed Night: " + energyPanel.getEnergyConsumedNight() + " kWh \n"
					+ "Total Energy Produced: " + energyPanel.getEnergyProduced() + " kWh \n";

			if (energyPanel.getAppliancesCount() > 0) {
				result += "Appliances Consumption:\n";
				for (Appliance appliance : energyPanel.getAppliancesList()) {
					result += "\t" + appliance.getName() + " (" + appliance.getBrand() + ")" + " > Total: "
							+ appliance.getEnergyConsumed() + " kWh, Daytime: " + appliance.getEnergyConsumedDaytime()
							+ " kWh, Night: " + appliance.getEnergyConsumedNight() + "kWh \n";
				}
			}

			if (energyPanel.getSolarPanelsCount() > 0) {
				result += "Solar Panels Production:\n";
				for (SolarPanel solarPanel : energyPanel.getSolarPanelsList()) {
					result += "\t" + solarPanel.getName() + " (" + solarPanel.getBrand() + ")" + " > Total: "
							+ solarPanel.getEnergyProduced() + " kWh \n";
				}
			}

		} catch (StatusRuntimeException e) {
			System.out.println(e.getMessage());
		}
		return result;
	}
}
