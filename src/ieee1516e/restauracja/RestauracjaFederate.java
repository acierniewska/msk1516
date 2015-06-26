package ieee1516e.restauracja;

import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.FederationExecutionDoesNotExist;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class RestauracjaFederate {
    private static final int[] STOLIKI = {6, 2, 2, 4, 5, 1};

    public static final String READY_TO_RUN = "ReadyToRun";
    public static RTIambassador rtiamb;
    private RestauracjaFederateAmbassador fedamb;
    private HLAfloat64TimeFactory timeFactory;
    protected EncoderFactory encoderFactory;

    public void runFederate(String federateName) throws Exception {
        if (prepareFederate(federateName)) return;

        publishAndSubscribe();
        log("Published and Subscribed");

        List<ObjectInstanceHandle> stolics = new ArrayList<>();

        for (int stolik : STOLIKI) {
            ObjectInstanceHandle objectHandle = registerObject();
            stolics.add(objectHandle);
            updateAttributeValues(objectHandle, stolik);
            log("Utworzono stolik o liczbie miejsc " + stolik + ", id=" + objectHandle);
        }

        while (fedamb.running) {
            //sendInteraction();
            advanceTime(50.0);
            // log("Time Advanced to " + fedamb.federateTime);
        }
        log("Restauracja koñczy prace");

        destroyFederate(stolics);
    }

    private void publishAndSubscribe() throws RTIexception {
        ObjectClassHandle stolikHandle = rtiamb.getObjectClassHandle("HLAobjectRoot.Stolik");
        AttributeHandle idHandle = rtiamb.getAttributeHandle(stolikHandle, "id");
        AttributeHandle liczbaMiejscHandle = rtiamb.getAttributeHandle(stolikHandle, "liczbaMiejsc");

        AttributeHandleSet attributes = rtiamb.getAttributeHandleSetFactory().create();
        attributes.add(idHandle);
        attributes.add(liczbaMiejscHandle);

        rtiamb.publishObjectClassAttributes(stolikHandle, attributes);

        InteractionClassHandle zamkniecieRestauracjiHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.zamkniecieRestauracji");
        rtiamb.subscribeInteractionClass(zamkniecieRestauracjiHandle);
    }

    private ObjectInstanceHandle registerObject() throws RTIexception {
        ObjectClassHandle stolikHandle = rtiamb.getObjectClassHandle("HLAobjectRoot.Stolik");
        return rtiamb.registerObjectInstance(stolikHandle);
    }

    private void updateAttributeValues(ObjectInstanceHandle objectHandle, int miejsca) throws RTIexception {
        ObjectClassHandle stolikHandle = rtiamb.getObjectClassHandle("HLAobjectRoot.Stolik");
        AttributeHandleValueMap attributes = rtiamb.getAttributeHandleValueMapFactory().create(1);

        AttributeHandle liczbaMiejscHandle = rtiamb.getAttributeHandle(stolikHandle, "liczbaMiejsc");
        HLAinteger32BE liczbaMiejsc = encoderFactory.createHLAinteger32BE(miejsca);
        attributes.put(liczbaMiejscHandle, liczbaMiejsc.toByteArray());

        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + fedamb.federateLookahead);
        rtiamb.updateAttributeValues(objectHandle, attributes, generateTag(), time);
    }

    private void advanceTime(double timestep) throws RTIexception {

        fedamb.isAdvancing = true;
        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + timestep);
        rtiamb.timeAdvanceRequest(time);

        while (fedamb.isAdvancing) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }
    }

    private void destroyFederate(List<ObjectInstanceHandle> clients) throws RTIexception {
        //Thread.sleep(1000);
        for (ObjectInstanceHandle oh : clients) {
            deleteObject(oh);
            log("Deleted Object, handle=" + oh);
        }

        try {
            rtiamb.resignFederationExecution(ResignAction.DELETE_OBJECTS);
            log("Resigned from Federation");
        }catch (Exception ex){
            log("Timeout");
        }

        try {
            rtiamb.destroyFederationExecution("ExampleFederation");
            log("Destroyed Federation");
        } catch (FederationExecutionDoesNotExist dne) {
            log("No need to destroy federation, it doesn't exist");
        } catch (FederatesCurrentlyJoined fcj) {
            log("Didn't destroy federation, federates still joined");
        } catch (Exception ex){
            log("Some other error");
        }
    }

    private boolean prepareFederate(String federateName) throws Exception {
        log("Creating RTIambassador");
        rtiamb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
        encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();

        log("Connecting...");
        fedamb = new RestauracjaFederateAmbassador(this);
        rtiamb.connect(fedamb, CallbackModel.HLA_EVOKED);

        log("Creating Federation...");
        try {
            URL[] modules = new URL[]{
                    (new File("foms/client.xml")).toURI().toURL()
            };

            rtiamb.createFederationExecution("ExampleFederation", modules);
            log("Created Federation");
        } catch (FederationExecutionAlreadyExists exists) {
            log("Didn't create federation, it already existed");
        } catch (MalformedURLException urle) {
            log("Exception loading one of the FOM modules from disk: " + urle.getMessage());
            urle.printStackTrace();
            return true;
        }

        URL[] joinModules = new URL[]{
        };

        rtiamb.joinFederationExecution(federateName,            // name for the federate
                "RestauracjaFederateType",   // federate type
                "ExampleFederation",     // name of federation
                joinModules);           // modules we want to add

        log("Joined Federation as " + federateName);

        this.timeFactory = (HLAfloat64TimeFactory) rtiamb.getTimeFactory();


        rtiamb.registerFederationSynchronizationPoint(READY_TO_RUN, null);
        // wait until the point is announced
        while (!fedamb.isAnnounced) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }

        waitForUser();

        rtiamb.synchronizationPointAchieved(READY_TO_RUN);
        log("Achieved sync point: " + READY_TO_RUN + ", waiting for federation...");
        while (!fedamb.isReadyToRun) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }

        enableTimePolicy();
        log("Time Policy Enabled");
        return false;
    }

    private void enableTimePolicy() throws Exception {
        HLAfloat64Interval lookahead = timeFactory.makeInterval(fedamb.federateLookahead);
        rtiamb.enableTimeRegulation(lookahead);
        while (!fedamb.isRegulating) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }
        rtiamb.enableTimeConstrained();

        while (!fedamb.isConstrained) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }
    }

    private void deleteObject(ObjectInstanceHandle handle) throws RTIexception {
        rtiamb.deleteObjectInstance(handle, generateTag());
    }

    public short getTimeAsShort() {
        return (short) (fedamb != null ? fedamb.federateTime : 0);
    }

    private byte[] generateTag() {
        return ("(timestamp) " + System.currentTimeMillis()).getBytes();
    }

    public static void main(String[] args) {

        String federateName = "RestauracjaFederate";
        if (args.length != 0) {
            federateName = args[0];
        }
        try {
            new RestauracjaFederate().runFederate(federateName);
        } catch (Exception rtie) {
            rtie.printStackTrace();
        }
    }

    private void log(String message) {
        System.out.println("czas: " + getTimeAsShort() + " - RestauracjaFederate   : " + message);
    }

    private void waitForUser() {
        log(" >>>>>>>>>> Press Enter to Continue <<<<<<<<<<");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            reader.readLine();
        } catch (Exception e) {
            log("Error while waiting for user input: " + e.getMessage());
            e.printStackTrace();
        }
    }
}