/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.remoteum.sample;

import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.transport.http.HTTPConstants;
import org.wso2.carbon.authenticator.stub.AuthenticationAdminStub;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.entitlement.EntitlementPolicyAdminService;
import org.wso2.carbon.identity.entitlement.dto.AttributeDTO;
import org.wso2.carbon.identity.entitlement.dto.PolicyDTO;
import org.wso2.carbon.identity.entitlement.internal.EntitlementConfigHolder;
import org.wso2.carbon.identity.entitlement.internal.EntitlementServiceComponent;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.jdbc.EmbeddedRegistryService;
import org.wso2.carbon.registry.core.jdbc.InMemoryEmbeddedRegistryService;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.um.ws.api.WSAuthorizationManager;
import org.wso2.carbon.um.ws.api.WSUserStoreManager;
import org.wso2.carbon.user.core.Permission;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.claim.Claim;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * This demonstrates how to use remote user management API to add, delete and read users.
 */
public class RemoteUMClient {

    private static String serverUrl;
    private static String username;
    private static String password;

    private AuthenticationAdminStub authstub = null;
    private ConfigurationContext ctx;
    private String authCookie = null;
    private WSUserStoreManager remoteUserStoreManager = null;
    private WSAuthorizationManager remoteAuthorizationManager = null;
    private EntitlementPolicyAdminService entitlementPolicyAdminService = null;

    /**
     * Initialization of environment
     *
     * @throws Exception
     */
    public RemoteUMClient() throws Exception {
        ctx = ConfigurationContextFactory.createConfigurationContextFromFileSystem(null, null);
        String authEPR = serverUrl + "AuthenticationAdmin";
        authstub = new AuthenticationAdminStub(ctx, authEPR);
        ServiceClient client = authstub._getServiceClient();
        Options options = client.getOptions();
        options.setManageSession(true);
        options.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, authCookie);

        //set trust store properties required in SSL communication.
        System.setProperty("javax.net.ssl.trustStore", RemoteUMSampleConstants.TRUST_STORE_PATH);
        System.setProperty("javax.net.ssl.trustStorePassword", RemoteUMSampleConstants.TRUST_STORE_PASSWORD);


        //log in as admin user and obtain the cookie
        this.login(username, password);

        //create web service client
        this.createRemoteUserStoreManager();
    }

    /**
     * Authenticate to carbon as admin user and obtain the authentication cookie
     *
     * @param username
     * @param password
     * @return
     * @throws Exception
     */
    public String login(String username, String password) throws Exception {
        //String cookie = null;
        boolean loggedIn = authstub.login(username, password, "localhost");
        if (loggedIn) {
            System.out.println("The user " + username + " logged in successfully.");
            System.out.println();
            authCookie = (String) authstub._getServiceClient().getServiceContext().getProperty(
                    HTTPConstants.COOKIE_STRING);
        } else {
            System.out.println("Error logging in " + username);
        }
        return authCookie;
    }

    /**
     * create web service client for RemoteUserStoreManager service from the wrapper api provided
     * in carbon - remote-usermgt component
     *
     * @throws UserStoreException
     */
    public void createRemoteUserStoreManager() throws UserStoreException {

        remoteUserStoreManager = new WSUserStoreManager(serverUrl, authCookie, ctx);
    }

    public void createEntitlementPolicyAdminService() throws UserStoreException {

        entitlementPolicyAdminService = new EntitlementPolicyAdminService();
    }
    /**
     * create web service client for RemoteAuthorizationManager service.
     *
     * @throws UserStoreException
     */
    public void createRemoteAuthorizationManager() throws UserStoreException {

        remoteAuthorizationManager = new WSAuthorizationManager(serverUrl, authCookie, ctx);
    }

    /**
     * Add a user store to the system.
     *
     * @throws UserStoreException
     */
    public void addUser(String userName, String password) throws UserStoreException {

        remoteUserStoreManager.addUser(userName, password, null, null, null);
        System.out.println("Added user: " + userName);
        System.out.println();
    }

    /**
     * Add a role to the system
     *
     * @throws Exception
     */
    public void addRole(String roleName) throws UserStoreException {
        remoteUserStoreManager.addRole(roleName, null, null);
        System.out.println("Added role: " + roleName);
        System.out.println();
    }

    /**
     * Add a new user by assigning him to a new role
     *
     * @throws Exception
     */
    public void addUserWithRole(String userName, String password, String roleName)
            throws UserStoreException {
        remoteUserStoreManager.addUser(userName, password, new String[]{roleName}, null, null);
        System.out.println("Added user: " + userName + " with role: " + roleName);
        System.out.println();
    }

    /**
     * Retrieve all the users in the system
     *
     * @throws Exception
     */
    public String[] listUsers() throws UserStoreException {
        return remoteUserStoreManager.listUsers("*", -1);
    }

    /**
     * Delete an exisitng user from the system
     *
     * @throws Exception
     */
    public void deleteUser(String userName) throws UserStoreException {
        remoteUserStoreManager.deleteUser(userName);
        System.out.println("Deleted user:" + userName);
        System.out.println();
    }

    /**
     * Authorize a role with given permission.
     *
     * @param roleName
     * @param resourceId
     * @param action
     * @throws UserStoreException
     */
    public void authorizeRole(String roleName, String resourceId, String action)
            throws UserStoreException {
        remoteAuthorizationManager.authorizeRole(roleName, resourceId, action);
    }

    /**
     * Check whether a given user has a given permission.
     *
     * @param userName
     * @param resourceId
     * @param action
     * @return
     * @throws UserStoreException
     */
    public boolean isUserAuthorized(String userName, String resourceId, String action)
            throws UserStoreException {
        return remoteAuthorizationManager.isUserAuthorized(userName, resourceId, action);
    }

    public static void main(String[] args) throws Exception {
        loadConfiguration();
        /*Create client for RemoteUserStoreManagerService and perform user management operations*/
        RemoteUMClient remoteUMClient = new RemoteUMClient();

        Permission permission1 = new Permission("/permission/admin/configure/test", "ui.execute");
        Permission permission2 = new Permission("/permission/admin/login", "ui.execute");

        //create web service client
        remoteUMClient.createRemoteUserStoreManager();


       // remoteUMClient.remoteUserStoreManager.addRole("testRole", null, new Permission[]{permission1, permission2});

        remoteUMClient.createRemoteAuthorizationManager();

        String[] res = remoteUMClient.remoteAuthorizationManager.getAllowedRolesForResource("/permission/admin/login","ui.execute");

        System.out.println();
        System.out.println("resorces");
        for(String d : res){
            System.out.println(d);
        }
        System.out.println();

        PolicyDTO policyDTO = new PolicyDTO();
        policyDTO.setPolicyId("testSample");

        AttributeDTO attributeDTO = new AttributeDTO();
        attributeDTO.setAttributeDataType("string");
        attributeDTO.setAttributeValue("testSampleValue");
        attributeDTO.setAttributeId("http://wso2.org/claims/role");
        policyDTO.setAttributeDTOs(new AttributeDTO[]{attributeDTO});

        remoteUMClient.createEntitlementPolicyAdminService();
        Properties engineProperties = new Properties();
        engineProperties.put( "PAP.Policy.Id.Regexp.Pattern", "[a-zA-Z0-9._:-]{3,100}$");

        EntitlementConfigHolder.getInstance().setEngineProperties(engineProperties);
        System.setProperty("carbon.config.dir.path", "");
        CarbonContext ct = CarbonContext.getThreadLocalCarbonContext();

        RegistryService registryService = new RegistryService() {
            @Override
            public UserRegistry getUserRegistry() throws RegistryException {
                return null;
            }

            @Override
            public UserRegistry getSystemRegistry() throws RegistryException {
                return null;
            }

            @Override
            public UserRegistry getSystemRegistry(int i) throws RegistryException {
                return null;
            }

            @Override
            public UserRegistry getSystemRegistry(int i, String s) throws RegistryException {
                return null;
            }

            @Override
            public UserRegistry getUserRegistry(String s, String s1) throws RegistryException {
                return null;
            }

            @Override
            public UserRegistry getUserRegistry(String s) throws RegistryException {
                return null;
            }

            @Override
            public UserRegistry getUserRegistry(String s, String s1, int i) throws RegistryException {
                return null;
            }

            @Override
            public UserRegistry getUserRegistry(String s, String s1, int i, String s2) throws RegistryException {
                return null;
            }

            @Override
            public UserRegistry getUserRegistry(String s, int i) throws RegistryException {
                return null;
            }

            @Override
            public UserRegistry getUserRegistry(String s, int i, String s1) throws RegistryException {
                return null;
            }

            @Override
            public UserRealm getUserRealm(int i) throws RegistryException {
                return null;
            }

            @Override
            public UserRegistry getRegistry() throws RegistryException {
                return null;
            }

            @Override
            public UserRegistry getRegistry(String s, String s1) throws RegistryException {
                return null;
            }

            @Override
            public UserRegistry getRegistry(String s) throws RegistryException {
                return null;
            }

            @Override
            public UserRegistry getRegistry(String s, String s1, int i) throws RegistryException {
                return null;
            }

            @Override
            public UserRegistry getRegistry(String s, String s1, int i, String s2) throws RegistryException {
                return null;
            }

            @Override
            public UserRegistry getRegistry(String s, int i) throws RegistryException {
                return null;
            }

            @Override
            public UserRegistry getRegistry(String s, int i, String s1) throws RegistryException {
                return null;
            }

            @Override
            public UserRegistry getLocalRepository() throws RegistryException {
                return null;
            }

            @Override
            public UserRegistry getLocalRepository(int i) throws RegistryException {
                return null;
            }

            @Override
            public UserRegistry getConfigSystemRegistry() throws RegistryException {
                return null;
            }

            @Override
            public UserRegistry getConfigSystemRegistry(int i) throws RegistryException {
                return null;
            }

            @Override
            public UserRegistry getConfigUserRegistry() throws RegistryException {
                return null;
            }

            @Override
            public UserRegistry getConfigUserRegistry(String s, String s1) throws RegistryException {
                return null;
            }

            @Override
            public UserRegistry getConfigUserRegistry(String s) throws RegistryException {
                return null;
            }

            @Override
            public UserRegistry getConfigUserRegistry(String s, int i) throws RegistryException {
                return null;
            }

            @Override
            public UserRegistry getConfigUserRegistry(String s, String s1, int i) throws RegistryException {
                return null;
            }

            @Override
            public UserRegistry getGovernanceSystemRegistry() throws RegistryException {
                return null;
            }

            @Override
            public UserRegistry getGovernanceSystemRegistry(int i) throws RegistryException {
                return new UserRegistry("", -1, null, null, "");
            }

            @Override
            public UserRegistry getGovernanceUserRegistry() throws RegistryException {
                return null;
            }

            @Override
            public UserRegistry getGovernanceUserRegistry(String s, String s1) throws RegistryException {
                return null;
            }

            @Override
            public UserRegistry getGovernanceUserRegistry(String s) throws RegistryException {
                return null;
            }

            @Override
            public UserRegistry getGovernanceUserRegistry(String s, String s1, int i) throws RegistryException {
                return new UserRegistry("", -1, null, null, "");
            }

            @Override
            public UserRegistry getGovernanceUserRegistry(String s, int i) throws RegistryException {
                return null;
            }
        };
        EntitlementServiceComponent myObj = new EntitlementServiceComponent();
        Class myClass = myObj.getClass();
        Field myField = myClass.getDeclaredField("registryService");
        myField.setAccessible(true);
        myField.set(myObj, registryService);

        remoteUMClient.entitlementPolicyAdminService.addPolicies(new PolicyDTO[]{policyDTO});

        //add a new user to the system
        remoteUMClient.addUser("kamal", "kamal");
        //add a role to the system
        remoteUMClient.addRole("eng");
        //add a new user with a role
        remoteUMClient.addUserWithRole("saman", "saman", "eng");
        //print a list of all the users in the system
        String[] users = remoteUMClient.listUsers();
        System.out.println("List of users in the system:");
        for (String user : users) {
            System.out.println(user);
        }
        System.out.println();
        //delete an existing user
        remoteUMClient.deleteUser("kamal");
        //print the current list of users
        String[] userList = remoteUMClient.listUsers();
        System.out.println("List of users in the system currently:");
        for (String user : userList) {
            System.out.println(user);
        }
        System.out.println();
        remoteUMClient.addUser("dinuka", "dinuka");
        remoteUMClient.getUserClaims("admin", "null");
        remoteUMClient.updateLastName("dinuka", "malalanayake");
        remoteUMClient.updateEmail("dinuka", "dinukam@wso2.com");

        //create remote authorization manager
        remoteUMClient.createRemoteAuthorizationManager();
        //authorize role
        remoteUMClient.authorizeRole("eng", "foo/bar", "read");
        //check authorization of a user belongs to that role
        if (remoteUMClient.isUserAuthorized("saman", "foo/bar", "read")) {
            System.out.println("User saman is authorized to read foo/bar.");
        }

    }

    /**
     * print the user claims of given user
     *
     * @param username
     * @param profile
     * @throws Exception
     */
    public void getUserClaims(String username, String profile) throws Exception {
        System.out.println("================Print All Claims of " + username + "================");
        //list down the user claims
        for (Claim claims : remoteUserStoreManager.getUserClaimValues(username, profile)) {
            System.out.println("-----------------------------------");
            System.out.println(claims.getClaimUri() + " -- " + claims.getValue());
        }

        System.out.println("================================================================");
    }

    /**
     * update the Last name of given user
     *
     * @param username
     * @param value
     * @throws Exception
     */
    public void updateLastName(String username, String value) throws Exception {
        remoteUserStoreManager.setUserClaimValue(username, "http://wso2.org/claims/lastname", value, null);
        System.out.println("lastname :" + value + " updated successful for" + " User :" + username);
        System.out.println();
    }

    /**
     * update the email address  of given user
     *
     * @param username
     * @param value
     * @throws Exception
     */
    public void updateEmail(String username, String value) throws Exception {
        remoteUserStoreManager.setUserClaimValue(username, "email", value, null);
        System.out.println("email :" + value + " updated successful for" + " User :" + username);
        System.out.println();
    }

    private static void loadConfiguration() throws IOException {
        Properties properties = new Properties();
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();

        InputStream is = classloader.getResourceAsStream(RemoteUMSampleConstants.PROPERTIES_FILE_NAME);
		properties.load(is);

        serverUrl = properties.getProperty(RemoteUMSampleConstants.REMOTE_SERVER_URL);
        username = properties.getProperty(RemoteUMSampleConstants.USER_NAME);
        password = properties.getProperty(RemoteUMSampleConstants.PASSWORD);
    }

}
