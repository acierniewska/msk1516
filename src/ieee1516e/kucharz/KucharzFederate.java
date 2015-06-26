package ieee1516e.kucharz;

import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.encoding.HLAunicodeString;
import hla.rti1516e.exceptions.*;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class KucharzFederate {
    private static final int PSTWO_PRZYGOTOWANIA_POSILKU = 5;
    public static final String READY_TO_RUN = "ReadyToRun";
    public static RTIambassador rtiamb;
    private KucharzFederateAmbassador fedamb;
    private HLAfloat64TimeFactory timeFactory;
    protected EncoderFactory encoderFactory;

    protected List<PrzekazanieZamowieniaEvent> przekazanieZamowieniaEvents = new ArrayList<>();

    public void runFederate(String federateName) throws Exception {
        if (prepareFederate(federateName)) return;

        publishAndSubscribe();
        log("Published and Subscribed");

        while (fedamb.running) {
            Iterator<PrzekazanieZamowieniaEvent> eventIterator = przekazanieZamowieniaEvents.iterator();
            int val = new Random().nextInt(10) + 1;
            if (eventIterator.hasNext() && val <= PSTWO_PRZYGOTOWANIA_POSILKU) {
                PrzekazanieZamowieniaEvent event = eventIterator.next();
                eventIterator.remove();

                sendPrzygotowaniePosilkuInteraction(event);
            }

            advanceTime(1.0);
        }
        log("Restauracja koñczy prace");

        destroyFederate();
    }

    private void sendPrzygotowaniePosilkuInteraction(PrzekazanieZamowieniaEvent event) throws NameNotFound, FederateNotExecutionMember, NotConnected, RTIinternalError, InvalidInteractionClassHandle, InvalidLogicalTime, InteractionClassNotPublished, InteractionParameterNotDefined, InteractionClassNotDefined, SaveInProgress, RestoreInProgress {
        InteractionClassHandle przygotowanieZamowieniaHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.przygotowanieZamowienia");
        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(2);

        ParameterHandle stolikIdHandle = rtiamb.getParameterHandle(przygotowanieZamowieniaHandle, "idStolika");
        HLAinteger32BE stolikId = encoderFactory.createHLAinteger32BE(event.getStolikId());
        parameters.put(stolikIdHandle, stolikId.toByteArray());

        ParameterHandle listaPosilkowHandle = rtiamb.getParameterHandle(przygotowanieZamowieniaHandle, "listaPosilkow");
        HLAunicodeString listaPosilkow = encoderFactory.createHLAunicodeString(event.getListaPosilkow());
        parameters.put(listaPosilkowHandle, listaPosilkow.toByteArray());

        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + fedamb.federateLookahead);
        rtiamb.sendInteraction(przygotowanieZamowieniaHandle, parameters, generateTag(), time);

        log("Przygotowa³em zamówienie zamowienie");
    }

    private void destroyFederate() throws RTIexception {
        //Thread.sleep(4000);
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
        fedamb = new KucharzFederateAmbassador(this);
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
                "MenedzerSaliFederateType",   // federate type
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

    private void publishAndSubscribe() throws RTIexception {
        InteractionClassHandle przekazanieZamowieniaDoKucharzaHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.przekazanieZamowieniaDoKucharza");
        rtiamb.subscribeInteractionClass(przekazanieZamowieniaDoKucharzaHandle);

        InteractionClassHandle przygotowanieZamowieniaHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.przygotowanieZamowienia");
        rtiamb.publishInteractionClass(przygotowanieZamowieniaHandle);

        InteractionClassHandle zamkniecieRestauracjiHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.zamkniecieRestauracji");
        rtiamb.subscribeInteractionClass(zamkniecieRestauracjiHandle);
    }

    private void advanceTime(double timestep) throws RTIexception {

        fedamb.isAdvancing = true;
        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + timestep);
        rtiamb.timeAdvanceRequest(time);

        while (fedamb.isAdvancing) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }
    }

    public short getTimeAsShort() {
        return (short) (fedamb != null ? fedamb.federateTime : 0);
    }

    byte[] generateTag() {
        return ("(timestamp) " + System.currentTimeMillis()).getBytes();
    }

    public static void main(String[] args) {

        String federateName = "KucharzFederate";
        if (args.length != 0) {
            federateName = args[0];
        }

        try {
            new KucharzFederate().runFederate(federateName);
        } catch (Exception rtie) {
            rtie.printStackTrace();
        }
    }

    private void log(String message) {
        System.out.println("czas: " + getTimeAsShort() + " - KucharzFederate   : " + message);
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