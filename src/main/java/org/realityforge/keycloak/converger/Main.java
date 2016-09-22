package org.realityforge.keycloak.converger;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
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
  private static final int ADMIN_USERNAME_OPT = 'u';
  private static final int ADMIN_PASSWORD_OPT = 'p';
  private static final int ENV_OPT = 'e';
  private static final int UNMANAGED_CLIENT_OPT = 'c';
  private static final int VERBOSE_OPT = 'v';
  private static final int DIR_OPT = 'd';

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
    new CLOptionDescriptor( "env",
                            CLOptionDescriptor.ARGUMENTS_REQUIRED_2 | CLOptionDescriptor.DUPLICATES_ALLOWED,
                            ENV_OPT,
                            "Settings that are replaced in client configurations." ),
    new CLOptionDescriptor( "unmanaged-client",
                            CLOptionDescriptor.ARGUMENTS_REQUIRED_2 | CLOptionDescriptor.DUPLICATES_ALLOWED,
                            UNMANAGED_CLIENT_OPT,
                            "Client configurations that should not be deleted." ),
    new CLOptionDescriptor( "standard-unmanaged-clients",
                            CLOptionDescriptor.ARGUMENT_DISALLOWED,
                            STANDARD_UNMANAGED_CLIENTS_OPT,
                            "Add the default set of keycloak clients to those unmanaged." ),
    new CLOptionDescriptor( "delete-unknown-clients",
                            CLOptionDescriptor.ARGUMENT_DISALLOWED,
                            DELETE_UNKNOWN_CLIENTS_OPT,
                            "Delete unknown clients." ),
    new CLOptionDescriptor( "verbose",
                            CLOptionDescriptor.ARGUMENT_DISALLOWED,
                            VERBOSE_OPT,
                            "print verbose message while operating." )
  };

  private static final int SUCCESS_EXIT_CODE = 0;
  private static final int ERROR_PARSING_ARGS_EXIT_CODE = 1;
  private static final int ERROR_PATCHING_CODE = 2;

  private static boolean c_verbose;
  private static boolean c_delete;
  private static final Map<String, String> c_envs = new HashMap<>();
  private static final List<String> c_unmanagedClients = new ArrayList<>();
  private static File c_dir;
  private static String c_adminRealmName = "master";
  private static String c_adminClient = "admin-cli";
  private static String c_adminUsername = "admin";
  private static String c_adminPassword;
  private static String c_serverURL;
  private static String c_realmName;

  public static void main( final String[] args )
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
      if ( c_delete )
      {
        removeUnmatchedClients( realm, clients );
      }

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

  private static void removeUnmatchedClients( final RealmResource realm, final Map<String, String> clients )
    throws Exception
  {
    for ( final ClientRepresentation client : realm.clients().findAll() )
    {
      final String clientId = client.getClientId();
      if ( !clients.containsKey( clientId ) && !c_unmanagedClients.contains( clientId ) )
      {

        try
        {
          info( "Deleting client configuration for clientId '" + clientId + "'" );
          realm.clients().get( client.getId() ).remove();
        }
        catch ( final Exception e )
        {
          error( "Error deleting client configuration " + clientId );
          throw e;
        }
      }
    }
  }

  private static void uploadClients( final RealmResource realm, final Map<String, String> clients )
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

  private static ClientRepresentation findExistingClient( final List<ClientRepresentation> existing,
                                                          final String clientID )
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

  private static void updateClient( final RealmResource realm,
                                    final ClientRepresentation client,
                                    final String configuration )
  {
    info( "Updating client with clientId '" + client.getClientId() + "'" );
    try
    {
      final ClientRepresentation candidate = realm.convertClientDescription( configuration );
      realm.clients().get( client.getId() ).update( candidate );
    }
    catch ( final Exception e )
    {
      error( "Error uploading client configuration " + client.getClientId() );
      throw e;
    }
  }

  private static void createClient( final RealmResource realm, final String clientID, final String configuration )
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

  private static void buildClientConfiguration( final Map<String, String> clientConfigurations, final File file )
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
    final String data = new String( byteData, "US-ASCII" );

    final Pattern pattern = Pattern.compile( "\\{\\{([^}].+)\\}\\}" );
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

  private static boolean processOptions( final String[] args )
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
          c_delete = true;
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
        case UNMANAGED_CLIENT_OPT:
        {
          c_unmanagedClients.add( option.getArgument() );
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
      info( "Delete Unknown Clients: " + c_delete );

      info( "Configuration directory: " + c_dir.getAbsolutePath() );
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

    final StringBuilder msg = new StringBuilder();

    msg.append( "java " );
    msg.append( Main.class.getName() );
    msg.append( " [options] message" );
    msg.append( lineSeparator );
    msg.append( "Options: " );
    msg.append( lineSeparator );

    msg.append( CLUtil.describeOptions( OPTIONS ).toString() );

    info( msg.toString() );
  }

  private static void info( final String message )
  {
    System.out.println( message );
  }

  private static void error( final String message )
  {
    System.out.println( "Error: " + message );
  }
}
