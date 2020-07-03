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
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.wso2.carbon.identity.entitlement.common.EntitlementConstants;
import org.wso2.carbon.identity.entitlement.stub.EntitlementPolicyAdminServiceStub;
import org.wso2.carbon.identity.entitlement.stub.dto.PaginatedStatusHolder;
import org.wso2.carbon.identity.entitlement.stub.dto.PolicyDTO;
import org.wso2.carbon.identity.entitlement.stub.dto.StatusHolder;

import java.io.File;

/**
 *  This is sample code that shows how you can automate the EntitlementPolicyAdminService web service API.
 *  This API can be used to upload, update, delete and publish PAP policies.
 *
 */
public class PolicyAdminClient {

    public static final String SERVER_URL = "https://localhost:9443/services/EntitlementPolicyAdminService";
    public static final String SERVER_USER_NAME = "admin";
    public static final String SERVER_PASSWORD = "admin";

    public static void main (String[] args) throws Exception {


        /**
         * Call to https://localhost:9443/services/   uses HTTPS protocol.
         * Therefore we to validate the server certificate. The server certificate is looked up in the
         * trust store. Following code sets what trust-store to look for and its JKs password.
         */

        System.setProperty("javax.net.ssl.trustStore",  "resources" + File.separator+ "wso2carbon.jks" );

        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");

        /**
         * Axis2 configuration context
         */
        ConfigurationContext configContext;

        try {

            /**
             * Create a configuration context. A configuration context contains information for
             * axis2 environment. This is needed to create an axis2 client
             */
            configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem( null, null);

            String serviceEndPoint = SERVER_URL;

            EntitlementPolicyAdminServiceStub policyAdminStub =
                            new EntitlementPolicyAdminServiceStub(configContext, serviceEndPoint);
            ServiceClient client = policyAdminStub._getServiceClient();
            Options option = client.getOptions();

            /**
             * Setting basic auth headers for authentication for user admin
             */
            HttpTransportProperties.Authenticator auth = new HttpTransportProperties.Authenticator();
            auth.setUsername(SERVER_USER_NAME);
            auth.setPassword(SERVER_PASSWORD);
            auth.setPreemptiveAuthentication(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.AUTHENTICATE, auth);
            option.setManageSession(true);

            /**
             *  Do any thing with entitlement policy admin API.
             *  Here as an example just have implemented add, update, publish and delete policy
             */


            ///////////////////////////////////////////////////////////////////////////////////////////////////////////

            //////// Following method shows how you can use web service operation /////////////////////////////////////

            ///////////////////////////////////////////////////////////////////////////////////////////////////////////

            // Read policy as String

            String samplePolicyName = "SamplePolicy";
            String policy =  readPolicy();

            /**
             * Add sample policy in to PAP.
             *
             * policyDTO :  only need to be set XACML policy as String value
             */

            PolicyDTO policyDTO = new PolicyDTO();
            policyDTO.setPolicy(policy);
            try{
                policyAdminStub.addPolicy(policyDTO);
            } catch (Exception e){
                e.printStackTrace();
            }

            System.out.println("INFO : Policy is uploaded successfully");


            /**
             *  Retrieve policy from PAP
             *
             *  policy Id : TestPolicy
             *  whether policy is retrieved from internal PDP : false
             */

            PolicyDTO samplePolicyDTO = policyAdminStub.getPolicy(samplePolicyName, false);

            System.out.println("INFO : Policy is retrieved successfully");

            /**
             *  Update sample policy in PAP.
             */

            policy = samplePolicyDTO.getPolicy();
            // update policy with new value
            policy = policy.replace("asela", "admin");
            policyDTO.setPolicy(policy);
            policyAdminStub.updatePolicy(policyDTO);

            System.out.println("INFO : Policy is updated successfully");

            /**
             *  Publish policy into internal PDP
             *
             *  policyIds :  TestPolicy
             *  version :  use current version. so NULL
             *  action : polisher action. policy is going to add in to the PDP
             *  enabled :  whether policy must be enabled in PDP
             *  order : default order. so Zero
             */

            policyAdminStub.publishToPDP(new String[]{samplePolicyName}, EntitlementConstants.PolicyPublish.ACTION_CREATE, null, false, 0);

            //Policy publish is happened in separate thread. So retrieve policy status and verify.
            Thread.sleep(2000);
            PaginatedStatusHolder paginatedStatusHolder = policyAdminStub.
                    getStatusData(EntitlementConstants.Status.ABOUT_POLICY, samplePolicyName, EntitlementConstants.StatusTypes.PUBLISH_POLICY, "*", 1);
            StatusHolder statusHolder = paginatedStatusHolder.getStatusHolders()[0];
            if(statusHolder.getSuccess() && EntitlementConstants.PolicyPublish.ACTION_CREATE.equals(statusHolder.getTargetAction())){
                System.out.println("INFO : Policy is published successfully");
            } else {
                throw new Exception("Policy is failed to publish");
            }


            /**
             * Enable policy in PDP runtime.
             *
             *  policy Id : TestPolicy
             *  whether policy must be enabled in internal PDP : true
             */

            policyAdminStub.enableDisablePolicy(samplePolicyName, true);
            //Policy publish is happened in separate thread. So retrieve policy status and verify.
            Thread.sleep(2000);
            paginatedStatusHolder = policyAdminStub.
                    getStatusData(EntitlementConstants.Status.ABOUT_POLICY, samplePolicyName, EntitlementConstants.StatusTypes.PUBLISH_POLICY, "*", 1);
            statusHolder = paginatedStatusHolder.getStatusHolders()[0];
            if(statusHolder.getSuccess() && EntitlementConstants.PolicyPublish.ACTION_ENABLE.equals(statusHolder.getTargetAction())){
                System.out.println("INFO : Policy is enabled successfully");
            } else {
                throw new Exception("Policy is failed to enable");
            }

            /**
             * Re-Order policy in PDP runtime.
             *
             *  policy Id : TestPolicy
             *  order id : 100
             *   TODO there is an issue in IS 5.0.0. You need to apply the fix in  https://wso2.org/jira/browse/IDENTITY-2899
             */
//            policyAdminStub.orderPolicy(samplePolicyName, 100);
//
//            //Policy publish is happened in separate thread. So retrieve policy status and verify.
//            Thread.sleep(2000);
//            paginatedStatusHolder = policyAdminStub.
//                    getStatusData(EntitlementConstants.Status.ABOUT_POLICY, samplePolicyName, EntitlementConstants.StatusTypes.PUBLISH_POLICY, "*", 1);
//            statusHolder = paginatedStatusHolder.getStatusHolders()[0];
//            if(statusHolder.getSuccess() && EntitlementConstants.PolicyPublish.ACTION_ORDER.equals(statusHolder.getTargetAction())){
//                System.out.println("INFO : Policy is re-ordered successfully");
//            } else {
//                throw new Exception("Policy is failed to re-order");
//            }

            /**
             * Disable policy from PDP
             *
             *  policy Id : TestPolicy
             *  whether policy must be enabled in internal PDP : false
             */

            policyAdminStub.enableDisablePolicy(samplePolicyName, false);
            //Policy publish is happened in separate thread. So retrieve policy status and verify.
            Thread.sleep(2000);
            paginatedStatusHolder = policyAdminStub.
                    getStatusData(EntitlementConstants.Status.ABOUT_POLICY, samplePolicyName, EntitlementConstants.StatusTypes.PUBLISH_POLICY, "*", 1);
            statusHolder = paginatedStatusHolder.getStatusHolders()[0];
            if(statusHolder.getSuccess() && EntitlementConstants.PolicyPublish.ACTION_DISABLE.equals(statusHolder.getTargetAction())){
                System.out.println("INFO : Policy is disabled successfully");
            } else {
                throw new Exception("Policy is failed to disable");
            }

           /**
            * Delete policy from PDP
            *
            *  policy Id : TestPolicy
            */

            policyAdminStub.dePromotePolicy(samplePolicyName);

            // delete policy from both PDP and PAP
            // policyAdminStub.removePolicy(samplePolicyName, true);

            // Policy publish is happened in separate thread. So retrieve policy status and verify.
            Thread.sleep(2000);
            paginatedStatusHolder = policyAdminStub.
                    getStatusData(EntitlementConstants.Status.ABOUT_POLICY, samplePolicyName, EntitlementConstants.StatusTypes.PUBLISH_POLICY, "*", 1);
            statusHolder = paginatedStatusHolder.getStatusHolders()[0];
            if(statusHolder.getSuccess() && EntitlementConstants.PolicyPublish.ACTION_DELETE.equals(statusHolder.getTargetAction())){
                System.out.println("INFO : Policy is deleted successfully");
            } else {
                throw new Exception("Policy is failed to delete");
            }

            /**
             * Delete policy from PAP
             *
             *  policy Id : TestPolicy
             *  whether policy must be deleted from internal PDP : false
             *
             */
            policyAdminStub.removePolicy(samplePolicyName, false);
            System.out.println("INFO : Policy is deleted successfully from PAP");


            ///////////////////////////////////////////////////////////////////////////////////////////////////////////

            //////// Following method shows how you can use web service operations in advance manner //////////////////

            ///////////////////////////////////////////////////////////////////////////////////////////////////////////

            /**
             *  Add sample policy in to PAP and also Publish it in to internal PDP policy Store and Enable it.
             *
             * policyDTO :  XACML policy as String, ask to promote policy and enable it.
             *
             * TODO there is an issue in IS 5.0.0. You need to apply the fix in  https://wso2.org/jira/browse/IDENTITY-2899
             *
             */
            policyDTO = new PolicyDTO();
            policyDTO.setPolicy(policy);

            // publish policy in to PDP
            policyDTO.setPromote(true);

            // enable policy in to PDP
            policyDTO.setActive(true);
            try{
                policyAdminStub.addPolicy(policyDTO);
            } catch (Exception e){
                e.printStackTrace();
            }

            //Policy publish is happened in separate thread. So retrieve policy status and verify.
            Thread.sleep(2000);
            paginatedStatusHolder = policyAdminStub.
                    getStatusData(EntitlementConstants.Status.ABOUT_POLICY, samplePolicyName, EntitlementConstants.StatusTypes.PUBLISH_POLICY, "*", 1);
            statusHolder = paginatedStatusHolder.getStatusHolders()[0];
            if(statusHolder.getSuccess() && EntitlementConstants.PolicyPublish.ACTION_CREATE.equals(statusHolder.getTargetAction())){
                System.out.println("INFO : Policy is uploaded to PAP and Policy is published in to internal PDP successfully as enabled Policy");
            }


            /**
             * Delete policy from PAP and PDP
             *
             *  policy Id : TestPolicy
             *  whether policy must be deleted from internal PDP : true
             *
             */
            policyAdminStub.removePolicy(samplePolicyName, true);

            // Policy publish is happened in separate thread. So retrieve policy status and verify.
            Thread.sleep(2000);
            paginatedStatusHolder = policyAdminStub.
                    getStatusData(EntitlementConstants.Status.ABOUT_POLICY, samplePolicyName, EntitlementConstants.StatusTypes.PUBLISH_POLICY, "*", 1);
            statusHolder = paginatedStatusHolder.getStatusHolders()[0];
            if(statusHolder.getSuccess() && EntitlementConstants.PolicyPublish.ACTION_DELETE.equals(statusHolder.getTargetAction())){
                System.out.println("INFO : Policy is deleted from PAP and PDP successfully");
            } else {
                throw new Exception("Policy is failed to delete");
            }


            /////////////////////////////////////////////////////////////////////////////////////////////////////////
            /////////////////////////  You are DONE                                     ////////////////////////////
            ////////////////////////////////////////////////////////////////////////////////////////////////////////

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }

    }

    private static String readPolicy(){

        return "<Policy xmlns=\"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17\" PolicyId=\"SamplePolicy\" RuleCombiningAlgId=\"urn:oasis:names:tc:xacml:1.0:rule-combining-algorithm:first-applicable\" Version=\"1.0\">\n" +
                "   <Target>\n" +
                "      <AnyOf>\n" +
                "         <AllOf>\n" +
                "            <Match MatchId=\"urn:oasis:names:tc:xacml:1.0:function:string-equal\">\n" +
                "               <AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">echo</AttributeValue>\n" +
                "               <AttributeDesignator AttributeId=\"urn:oasis:names:tc:xacml:1.0:resource:resource-id\" Category=\"urn:oasis:names:tc:xacml:3.0:attribute-category:resource\" DataType=\"http://www.w3.org/2001/XMLSchema#string\" MustBePresent=\"true\"/>\n" +
                "            </Match>\n" +
                "         </AllOf>\n" +
                "      </AnyOf>\n" +
                "   </Target>\n" +
                "   <Rule Effect=\"Permit\" RuleId=\"Rule-1\">\n" +
                "      <Target>\n" +
                "         <AnyOf>\n" +
                "            <AllOf>\n" +
                "               <Match MatchId=\"urn:oasis:names:tc:xacml:1.0:function:string-equal\">\n" +
                "                  <AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">read</AttributeValue>\n" +
                "                  <AttributeDesignator AttributeId=\"urn:oasis:names:tc:xacml:1.0:action:action-id\" Category=\"urn:oasis:names:tc:xacml:3.0:attribute-category:action\" DataType=\"http://www.w3.org/2001/XMLSchema#string\" MustBePresent=\"true\"/>\n" +
                "               </Match>\n" +
                "            </AllOf>\n" +
                "         </AnyOf>\n" +
                "      </Target>\n" +
                "      <Condition>\n" +
                "         <Apply FunctionId=\"urn:oasis:names:tc:xacml:1.0:function:any-of\">\n" +
                "            <Function FunctionId=\"urn:oasis:names:tc:xacml:1.0:function:string-equal\"/>\n" +
                "            <AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">asela</AttributeValue>\n" +
                "            <AttributeDesignator AttributeId=\"urn:oasis:names:tc:xacml:1.0:subject:subject-id\" Category=\"urn:oasis:names:tc:xacml:1.0:subject-category:access-subject\" DataType=\"http://www.w3.org/2001/XMLSchema#string\" MustBePresent=\"true\"/>\n" +
                "         </Apply>\n" +
                "      </Condition>\n" +
                "   </Rule>\n" +
                "   <Rule Effect=\"Deny\" RuleId=\"Deny-Rule\"/>\n" +
                "</Policy> ";
    }
}