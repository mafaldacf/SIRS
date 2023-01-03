package pt.ulisboa.tecnico.sirs.rbac;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import pt.ulisboa.tecnico.sirs.security.Security;
import pt.ulisboa.tecnico.sirs.rbac.exceptions.*;
import pt.ulisboa.tecnico.sirs.rbac.grpc.*;

import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.time.format.DateTimeFormatter;  
import java.time.LocalDateTime; 

public class Rbac {
    
    private RbacServiceGrpc.RbacServiceBlockingStub rbacserver;

    Map<Role, PermissionType> PermissionsByRoles = Map.ofEntries(
        Map.entry(Role.ENERGY_MANAGER, PermissionType.ENERGY_DATA),
        Map.entry(Role.ACCOUNT_MANAGER, PermissionType.PERSONAL_DATA)
    );

    // Data compartments
	private static final String KEY_STORE_FILE = "src/main/resources/rbac.keystore";
	private static final String KEY_STORE_PASSWORD = "mypassrbac";
    private static final String KEY_STORE_ALIAS_RBAC = "rbac"; 

    public Rbac()
    {

    }

    public ValidatePermissionResponse generateResponse(String username, Role role, PermissionType permission) throws NoSuchPaddingException,
        NoSuchAlgorithmException, InvalidKeyException, KeyStoreException, IOException,
        CertificateException, UnrecoverableKeyException, SignatureException 
    {
        PrivateKey privateKey;

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(Files.newInputStream(Paths.get(KEY_STORE_FILE)), KEY_STORE_PASSWORD.toCharArray());

        privateKey = (PrivateKey) keyStore.getKey(KEY_STORE_ALIAS_RBAC, KEY_STORE_PASSWORD.toCharArray());
      
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nowPlusSeconds = now.plusSeconds(30);

        ValidatePermissionResponse.Ticket data = ValidatePermissionResponse.Ticket.newBuilder()
                .setUsername(username)
                .setRole(role)
                .setPermission(permission)
                .setRequestIssuedAt(dtf.format(now))
                .setRequestValidUntil(dtf.format(nowPlusSeconds))
                .build();

        ByteString signature = Security.signMessage(privateKey, data.toByteArray());

        ValidatePermissionResponse response = ValidatePermissionResponse.newBuilder()
                .setData(data)
                .setSignature(signature)
                .build();

        return response;
    }

    /*
    ------------------------------------------------
    ---------------- ACCESS CONTROL ----------------
    ------------------------------------------------
    */

    public ValidatePermissionResponse validatePermissions(String username, Role role, PermissionType permission) throws NoSuchPaddingException,
        NoSuchAlgorithmException, InvalidKeyException, KeyStoreException, IOException,
        CertificateException, UnrecoverableKeyException, SignatureException,
        PermissionDeniedException, InvalidRoleException
    {
        if(PermissionsByRoles.containsKey(role)) {            
            if(PermissionsByRoles.get(role).equals(permission)) {
                return generateResponse(username, role, permission);
            }
            else {
                throw new PermissionDeniedException(role.name());
            }
        }
        else {
            throw new InvalidRoleException(role.name());
        }
    }
}