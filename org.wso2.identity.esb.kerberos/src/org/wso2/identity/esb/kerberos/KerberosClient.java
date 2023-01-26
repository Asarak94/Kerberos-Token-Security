package org.wso2.identity.esb.kerberos;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyEngine;
import org.apache.rampart.RampartMessageData;

/**
 * A client program which access "echo" web service over Kerberos authentication.
 */
public class KerberosClient {

    final static String ECHO_SERVICE_EPR = "http://asara:8290/services/kerberosSampleProxy";

    private static ConfigurationContext confContext = null;
    private static Policy servicePolicy = null;


    public static void main(String[] args) throws Exception {

        // If you are accessing the echo service over HTTPS, you need to set following properties
        String trustStore = System.getProperty("user.dir") + "/src/wso2carbon.jks";
        System.setProperty("javax.net.ssl.trustStore", trustStore);
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");

        // Create configuration context - you will have Rampart module engaged in the
        // client.axis2.xml
        confContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem("repo",
                "repo/conf/client.axis2.xml");

        servicePolicy = loadServicePolicy("repo/conf/policy.xml");

        run();

        System.exit(0);
    }

    /**
     * Loads the policy.
     * @param xmlPath Policy file path.
     * @return A Policy object
     * @throws Exception If an error occurred while creating the Policy object.
     */
    private static Policy loadServicePolicy(String xmlPath) throws Exception {
        StAXOMBuilder builder;
        Policy policy;

        builder = new StAXOMBuilder(xmlPath);
        policy = PolicyEngine.getPolicy(builder.getDocumentElement());

        return policy;
    }

    /**
     * Constructs the payload.
     * @param value The value to be echoed.
     * @return Axiom representation of the payload.
     */
    private static OMElement getPayload(String value) {

        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMNamespace ns = factory.createOMNamespace("http://echo.services.core.carbon.wso2.org", "ns1");
        OMElement elem = factory.createOMElement("echoString", ns);
        OMElement childElem = factory.createOMElement("in", null);
        childElem.setText(value);
        elem.addChild(childElem);

        return elem;
    }


    /**
     * This method will invoke the service.
     */
    public static void run() {


     

        ServiceClient client = null;
        try {

            client = new ServiceClient(confContext, null);

            Options options = new Options();

            // Set the action
            options.setAction("urn:echoString");

            // Set the endpoint
            options.setTo(new EndpointReference(ECHO_SERVICE_EPR));
            // Set the service policy
            options.setProperty(RampartMessageData.KEY_RAMPART_POLICY, servicePolicy);

            client.setOptions(options);

            // Engage modules
            //client.engageModule("rampart");

            // We are calling the service with following parameter.
            String value = "Hello WORLD";

            System.out.println("Calling kerberosSampleProxy service with parameter - " + value);

            OMElement response = client.sendReceive(getPayload(value));
		

            // Output from the service invocation
            System.out.println("Response  : " + response);
	

        } catch (AxisFault axisFault) {

            axisFault.printStackTrace();
            System.err.println("Could not create service client");
        } finally {

            if (client != null) {
                try {
                    client.cleanup();
                } catch (AxisFault axisFault) {
                    System.err.println("Error cleaning client after invocation");
                }
            }

        }
    }
}
