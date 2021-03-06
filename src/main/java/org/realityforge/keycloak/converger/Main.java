package org.realityforge.keycloak.converger;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.realityforge.getopt4j.CLArgsParser;
import org.realityforge.getopt4j.CLOption;
import org.realityforge.getopt4j.CLOptionDescriptor;
import org.realityforge.getopt4j.CLUtil;

/**
 * A simple commandline application for converging a keycloak realm.
 */
public class Main
{
  private static final int HELP_OPT = 1;
  private static final int SERVER_URL_OPT = 2;
  private static final int ADMIN_REALM_NAME_OPT = 3;
  private static final int ADMIN_CLIENT_NAME_OPT = 4;
  private static final int REALM_NAME_OPT = 5;
  private static final int STANDARD_UNMANAGED_CLIENTS_OPT = 6;
  private static final int DELETE_UNKNOWN_CLIENTS_OPT = 7;
  private static final int DELETE_CLIENT_OPT = 8;
  private static final int SECRETS_DIR_OPT = 9;
  private static final int ADMIN_USERNAME_OPT = 'u';
  private static final int ADMIN_PASSWORD_OPT = 'p';
  private static final int ENV_OPT = 'e';
  private static final int UNMANAGED_CLIENT_OPT = 'c';
  private static final int VERBOSE_OPT = 'v';
  private static final int DIR_OPT = 'd';
  @Nonnull
  private static final CLOptionDescriptor[] OPTIONS = new CLOptionDescriptor[]{
    new CLOptionDescriptor( "help",
                            CLOptionDescriptor.ARGUMENT_DISALLOWED,
                            HELP_OPT,
                            "print this message and exit" ),
    new CLOptionDescriptor( "server-url",
                            CLOptionDescriptor.ARGUMENT_REQUIRED,
                            SERVER_URL_OPT,
                            "the base url of keycloak server." ),
    new CLOptionDescriptor( "admin-realm-name",
                            CLOptionDescriptor.ARGUMENT_REQUIRED,
                            ADMIN_REALM_NAME_OPT,
                            "the name of the realm used to authenticate admin against. Defaults to 'master'." ),
    new CLOptionDescriptor( "admin-client",
                            CLOptionDescriptor.ARGUMENT_REQUIRED,
                            ADMIN_CLIENT_NAME_OPT,
                            "the name of client in admin realm to authenticate using. Defaults to 'admin-cli'." ),
    new CLOptionDescriptor( "admin-username",
                            CLOptionDescriptor.ARGUMENT_REQUIRED,
                            ADMIN_USERNAME_OPT,
                            "the username to authenticate with. Defaults to 'admin'." ),
    new CLOptionDescriptor( "admin-password",
                            CLOptionDescriptor.ARGUMENT_REQUIRED,
                            ADMIN_PASSWORD_OPT,
                            "the password to authenticate with." ),
    new CLOptionDescriptor( "realm-name",
                            CLOptionDescriptor.ARGUMENT_REQUIRED,
                            REALM_NAME_OPT,
                            "the name of the realm to update." ),
    new CLOptionDescriptor( "dir",
                            CLOptionDescriptor.ARGUMENT_REQUIRED,
                            DIR_OPT,
                            "the directory of client configurations." ),
    new CLOptionDescriptor( "secrets-dir",
                            CLOptionDescriptor.ARGUMENT_REQUIRED,
                            SECRETS_DIR_OPT,
                            "the directory where client secrets are downloaded to." ),
    new CLOptionDescriptor( "env",
                            CLOptionDescriptor.ARGUMENTS_REQUIRED_2 | CLOptionDescriptor.DUPLICATES_ALLOWED,
                            ENV_OPT,
                            "Settings that are replaced in client configurations." ),
    new CLOptionDescriptor( "unmanaged-client",
                            CLOptionDescriptor.ARGUMENTS_REQUIRED_2 | CLOptionDescriptor.DUPLICATES_ALLOWED,
                            UNMANAGED_CLIENT_OPT,
                            "Client configurations that should not be deleted and should have clients downloaded." ),
    new CLOptionDescriptor( "standard-unmanaged-clients",
                            CLOptionDescriptor.ARGUMENT_DISALLOWED,
                            STANDARD_UNMANAGED_CLIENTS_OPT,
                            "Add the default set of keycloak clients to those unmanaged." ),
    new CLOptionDescriptor( "delete-unknown-clients",
                            CLOptionDescriptor.ARGUMENT_DISALLOWED,
                            DELETE_UNKNOWN_CLIENTS_OPT,
                            "Delete unknown clients." ),
    new CLOptionDescriptor( "delete-client",
                            CLOptionDescriptor.ARGUMENT_REQUIRED | CLOptionDescriptor.DUPLICATES_ALLOWED,
                            DELETE_CLIENT_OPT,
                            "Delete specific client." ),
    new CLOptionDescriptor( "verbose",
                            CLOptionDescriptor.ARGUMENT_DISALLOWED,
                            VERBOSE_OPT,
                            "print verbose message while operating." )
  };
  private static final int SUCCESS_EXIT_CODE = 0;
  private static final int ERROR_PARSING_ARGS_EXIT_CODE = 1;
  private static final int ERROR_PATCHING_CODE = 2;
  private static final int ERROR_WRITING_CLIENT_SECRET_CODE = 3;
  private static boolean c_verbose;
  private static boolean c_deleteUnmatchedClients;
  @Nonnull
  private static final Map<String, String> c_envs = new HashMap<>();
  @Nonnull
  private static final List<String> c_unmanagedClients = new ArrayList<>();
  @Nonnull
  private static final List<String> c_clientsToDelete = new ArrayList<>();
  private static File c_dir;
  @Nonnull
  private static String c_adminRealmName = "master";
  @Nonnull
  private static String c_adminClient = "admin-cli";
  @Nonnull
  private static String c_adminUsername = "admin";
  @Nullable
  private static String c_adminPassword;
  @Nullable
  private static String c_serverURL;
  @Nullable
  private static String c_realmName;
  private static File c_secretsDir;

  public static void main( @Nonnull final String[] args )
  {
    if ( !processOptions( args ) )
    {
      System.exit( ERROR_PARSING_ARGS_EXIT_CODE );
      return;
    }

    try
    {
      if ( c_verbose )
      {
        info( "Converging realm " + c_realmName );
      }
      final Keycloak keycloak =
        Keycloak.getInstance( c_serverURL,
                              c_adminRealmName,
                              c_adminUsername,
                              c_adminPassword,
                              c_adminClient,
                              c_adminPassword );

      final RealmResource realm = keycloak.realm( c_realmName );

      final Map<String, String> clients = buildClientConfigurations();
      uploadClients( realm, clients );
      if ( c_deleteUnmatchedClients )
      {
        removeUnmatchedClients( realm, clients );
      }
      if ( !c_clientsToDelete.isEmpty() )
      {
        removeClients( realm, c_clientsToDelete::contains );
      }
      collectClientSecrets( realm, clients );

      System.exit( SUCCESS_EXIT_CODE );
    }
    catch ( final Exception e )
    {
      error( "Error converging keycloak realm " + c_realmName + ". Error: " + e );
      if ( c_verbose )
      {
        e.printStackTrace( System.out );
      }
      System.exit( ERROR_PATCHING_CODE );
    }
  }

  private static void removeUnmatchedClients( @Nonnull final RealmResource realm,
                                              @Nonnull final Map<String, String> clients )
  {
    removeClients( realm, clientId -> !clients.containsKey( clientId ) && !c_unmanagedClients.contains( clientId ) );
  }

  private static void removeClients( @Nonnull final RealmResource realm, @Nonnull final Predicate<String> shouldDelete )
  {
    for ( final ClientRepresentation client : realm.clients().findAll() )
    {
      final String clientId = client.getClientId();
      if ( shouldDelete.test( clientId ) )
      {
        deleteClient( realm, client.getId(), clientId );
      }
    }
  }

  private static void deleteClient( @Nonnull final RealmResource realm,
                                    @Nonnull final String id,
                                    @Nonnull final String clientId )
  {
    try
    {
      info( "Deleting client configuration for clientId '" + clientId + "'" );
      realm.clients().get( id ).remove();
    }
    catch ( final Exception e )
    {
      error( "Error deleting client configuration " + clientId );
      throw e;
    }
  }

  private static void uploadClients( @Nonnull final RealmResource realm, @Nonnull final Map<String, String> clients )
  {
    final List<ClientRepresentation> existing = realm.clients().findAll();

    for ( final Map.Entry<String, String> entry : clients.entrySet() )
    {
      final String clientID = entry.getKey();
      final String configuration = entry.getValue();

      final ClientRepresentation client = findExistingClient( existing, clientID );
      if ( null != client )
      {
        updateClient( realm, client, configuration );
      }
      else
      {
        createClient( realm, clientID, configuration );
      }
    }
  }

  private static void collectClientSecrets( @Nonnull final RealmResource realm,
                                            @Nonnull final Map<String, String> clients )
  {
    final List<ClientRepresentation> existing = realm.clients().findAll();
    for ( final Map.Entry<String, String> entry : clients.entrySet() )
    {
      final ClientRepresentation client = findExistingClient( existing, entry.getKey() );
      assert null != client;
      collectClientSecret( realm, client );
    }
    for ( final String clientID : c_unmanagedClients )
    {
      final ClientRepresentation client = findExistingClient( existing, clientID );
      if ( null != client )
      {
        collectClientSecret( realm, client );
      }
    }
  }

  private static void collectClientSecret( @Nonnull final RealmResource realm,
                                           @Nonnull final ClientRepresentation client )
  {
    final Boolean publicClient = client.isPublicClient();
    if ( null != publicClient && !publicClient )
    {
      info( "Retrieving client secret for confidential client with clientId '" + client.getClientId() + "'" );
      final ClientResource clientResource = realm.clients().get( client.getId() );
      final CredentialRepresentation secret = clientResource.getSecret();
      if ( null != secret )
      {
        final String value = secret.getValue();
        if ( null != value )
        {
          final Path dir = c_secretsDir.toPath();
          final Path secretFile = dir.resolve( client.getClientId() );
          try
          {
            if ( !Files.exists( dir ) )
            {
              Files.createDirectories( dir );
            }
            Files.write( secretFile, value.getBytes( StandardCharsets.US_ASCII ) );
          }
          catch ( final IOException ioe )
          {
            error( "Error writing keycloak secret for client " + client.getClientId() + " in " +
                   "the realm " + c_realmName + ". Error: " + ioe );
            System.exit( ERROR_WRITING_CLIENT_SECRET_CODE );
          }
        }
      }
    }
  }

  @Nullable
  private static ClientRepresentation findExistingClient( @Nonnull final List<ClientRepresentation> existing,
                                                          @Nonnull final String clientID )
  {
    ClientRepresentation client = null;
    for ( final ClientRepresentation candidate : existing )
    {
      if ( candidate.getClientId().equals( clientID ) )
      {
        client = candidate;
        break;
      }
    }
    return client;
  }

  private static void updateClient( @Nonnull final RealmResource realm,
                                    @Nonnull final ClientRepresentation client,
                                    @Nonnull final String configuration )
  {
    info( "Updating client with clientId '" + client.getClientId() + "'" );
    try
    {
      final ClientRepresentation candidate = realm.convertClientDescription( configuration );
      final ClientResource clientResource = realm.clients().get( client.getId() );
      clientResource.update( candidate );
    }
    catch ( final Exception e )
    {
      error( "Error uploading client configuration " + client.getClientId() );
      throw e;
    }
  }

  private static void createClient( @Nonnull final RealmResource realm,
                                    @Nonnull final String clientID,
                                    @Nonnull final String configuration )
  {
    info( "Creating client with clientId '" + clientID + "'" );
    try
    {
      final Response response = realm.clients().create( realm.convertClientDescription( configuration ) );
      if ( response.getStatus() != Response.Status.CREATED.getStatusCode() )
      {
        final String message =
          "Failed to create client '" + clientID + "' due to " +
          response.getStatusInfo().getStatusCode() + ":" + response.getStatusInfo().getReasonPhrase();
        throw new IllegalStateException( message );
      }
    }
    catch ( final Exception e )
    {
      error( "Error uploading client configuration " + clientID );
      throw e;
    }
  }

  @Nonnull
  private static Map<String, String> buildClientConfigurations()
    throws Exception
  {
    final Map<String, String> clientConfigurations = new HashMap<>();
    final File[] files = c_dir.listFiles();
    assert null != files;
    for ( final File file : files )
    {
      if ( !file.isDirectory() && file.getName().endsWith( ".json" ) )
      {
        try
        {
          buildClientConfiguration( clientConfigurations, file );
        }
        catch ( final Exception e )
        {
          error( "Error building client configuration from file " + file );
          throw e;
        }
      }
    }
    return clientConfigurations;
  }

  private static void buildClientConfiguration( @Nonnull final Map<String, String> clientConfigurations,
                                                @Nonnull final File file )
    throws IOException
  {
    final String data = loadAndTransformClient( file );
    final JsonObject clientJson = Json.createReader( new StringReader( data ) ).readObject();
    final String clientID = clientJson.getString( "clientId" );
    if ( clientConfigurations.containsKey( clientID ) )
    {
      final String message =
        "Client with clientId '" + clientID + "' defined multiple times in client directory";
      throw new IllegalStateException( message );
    }
    clientConfigurations.put( clientID, data );
  }

  /**
   * Load the file from file system and process using mustache replacement
   */
  private static String loadAndTransformClient( final File file )
    throws IOException
  {
    final byte[] byteData = Files.readAllBytes( file.toPath() );
    final String data = new String( byteData, StandardCharsets.US_ASCII );

    return replaceUUIDs( replaceVars( data ) );
  }

  /**
   * Need to replace UUIDs as the database uses them to uniquely distinguish elements.
   */
  @Nonnull
  private static String replaceUUIDs( @Nonnull final String data )
  {
    final Pattern pattern =
      Pattern.compile( "[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}" );
    final Matcher matcher = pattern.matcher( data );

    boolean result = matcher.find();
    if ( result )
    {
      final StringBuffer sb = new StringBuffer();
      do
      {
        final String replacement = UUID.randomUUID().toString();
        matcher.appendReplacement( sb, replacement );
        result = matcher.find();
      } while ( result );
      matcher.appendTail( sb );

      return sb.toString();
    }
    else
    {
      return data;
    }
  }

  @Nonnull
  private static String replaceVars( @Nonnull final String data )
  {
    final Pattern pattern = Pattern.compile( "\\{\\{([^}].+)}}" );
    final Matcher matcher = pattern.matcher( data );

    boolean result = matcher.find();
    if ( result )
    {
      final StringBuffer sb = new StringBuffer();
      do
      {
        final String var = matcher.group( 1 );
        final String replacement = c_envs.get( var );
        if ( null == replacement )
        {
          throw new IllegalStateException( "Unable to replace variable '" + var + "'" );
        }
        matcher.appendReplacement( sb, replacement );
        result = matcher.find();
      } while ( result );
      matcher.appendTail( sb );

      return sb.toString();
    }
    else
    {
      return data;
    }
  }

  private static boolean processOptions( @Nonnull final String[] args )
  {
    // Parse the arguments
    final CLArgsParser parser = new CLArgsParser( args, OPTIONS );

    //Make sure that there was no errors parsing arguments
    if ( null != parser.getErrorString() )
    {
      error( parser.getErrorString() );
      return false;
    }

    // Get a list of parsed options
    final List<CLOption> options = parser.getArguments();
    for ( final CLOption option : options )
    {
      switch ( option.getId() )
      {
        case CLOption.TEXT_ARGUMENT:
        {
          error( "Invalid text argument supplied: " + option.getArgument() );
          return false;
        }
        case SERVER_URL_OPT:
        {
          c_serverURL = option.getArgument();
          break;
        }
        case REALM_NAME_OPT:
        {
          c_realmName = option.getArgument();
          break;
        }
        case ADMIN_USERNAME_OPT:
        {
          c_adminUsername = option.getArgument();
          break;
        }
        case ADMIN_PASSWORD_OPT:
        {
          c_adminPassword = option.getArgument();
          break;
        }
        case ADMIN_REALM_NAME_OPT:
        {
          c_adminRealmName = option.getArgument();
          break;
        }
        case ADMIN_CLIENT_NAME_OPT:
        {
          c_adminClient = option.getArgument();
          break;
        }
        case ENV_OPT:
        {
          c_envs.put( option.getArgument(), option.getArgument( 1 ) );
          break;
        }
        case DELETE_UNKNOWN_CLIENTS_OPT:
        {
          c_deleteUnmatchedClients = true;
          break;
        }
        case STANDARD_UNMANAGED_CLIENTS_OPT:
        {
          c_unmanagedClients.add( "admin-cli" );
          c_unmanagedClients.add( "account" );
          c_unmanagedClients.add( "broker" );
          c_unmanagedClients.add( "realm-management" );
          c_unmanagedClients.add( "security-admin-console" );
          break;
        }
        case DELETE_CLIENT_OPT:
        {
          c_clientsToDelete.add( option.getArgument() );
          break;
        }
        case UNMANAGED_CLIENT_OPT:
        {
          c_unmanagedClients.add( option.getArgument() );
          break;
        }
        case SECRETS_DIR_OPT:
        {
          c_secretsDir = new File( option.getArgument() );
          break;
        }
        case DIR_OPT:
        {
          c_dir = new File( option.getArgument() );
          break;
        }
        case VERBOSE_OPT:
        {
          c_verbose = true;
          break;
        }
        case HELP_OPT:
        {
          printUsage();
          System.exit( SUCCESS_EXIT_CODE );
          return false;
        }
      }
    }
    if ( null == c_dir )
    {
      error( "No configuration directory specified." );
      return false;
    }
    if ( !c_dir.exists() )
    {
      error( "Configuration directory specified " + c_dir.getAbsolutePath() + " does not exist." );
      return false;
    }
    if ( !c_dir.canRead() )
    {
      error( "Configuration directory specified " + c_dir.getAbsolutePath() + " is not readable." );
      return false;
    }
    if ( !c_dir.isDirectory() )
    {
      error( "Configuration directory specified " + c_dir.getAbsolutePath() + " is not a directory." );
      return false;
    }
    if ( null != c_secretsDir )
    {
      if ( c_secretsDir.exists() && !c_secretsDir.canRead() )
      {
        error( "Secret directory specified " + c_secretsDir.getAbsolutePath() + " is not readable." );
        return false;
      }
      if ( c_secretsDir.exists() && !c_secretsDir.isDirectory() )
      {
        error( "Secret directory specified " + c_secretsDir.getAbsolutePath() + " is not a directory." );
        return false;
      }
    }
    else
    {
      c_secretsDir = c_dir;
    }
    if ( null == c_realmName )
    {
      error( "No realm specified to update." );
      return false;
    }
    if ( null == c_serverURL )
    {
      error( "No server url specified." );
      return false;
    }
    if ( null == c_adminPassword )
    {
      error( "No admin password specified." );
      return false;
    }
    if ( c_verbose )
    {
      info( "Server URL: " + c_serverURL );
      info( "Admin Realm Name: " + c_adminRealmName );
      info( "Admin Client Name: " + c_adminClient );
      info( "Admin Username: " + c_adminUsername );
      info( "Realm: " + c_realmName );
      info( "Delete Unknown Clients: " + c_deleteUnmatchedClients );

      info( "Configuration directory: " + c_dir.getAbsolutePath() );
      info( "Secrets directory: " + c_secretsDir.getAbsolutePath() );
      if ( !c_envs.isEmpty() )
      {
        info( "Env vars:" );
        for ( final Map.Entry<String, String> entry : c_envs.entrySet() )
        {
          info( "\t" + entry.getKey() + " = " + entry.getValue() );
        }
      }
      if ( !c_unmanagedClients.isEmpty() )
      {
        info( "Unmanaged clients:" );
        for ( final String unmanagedClient : c_unmanagedClients )
        {
          info( "\t" + unmanagedClient );
        }
      }
    }

    return true;
  }

  /**
   * Print out a usage statement
   */
  private static void printUsage()
  {
    final String lineSeparator = System.getProperty( "line.separator" );
    info( "java " +
          Main.class.getName() +
          " [options] message" +
          lineSeparator +
          "Options: " +
          lineSeparator +
          CLUtil.describeOptions( OPTIONS ) );
  }

  private static void info( @Nonnull final String message )
  {
    System.out.println( message );
  }

  private static void error( @Nonnull final String message )
  {
    System.out.println( "Error: " + message );
  }
}
