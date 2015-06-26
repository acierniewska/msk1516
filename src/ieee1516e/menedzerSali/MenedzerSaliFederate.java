package ieee1516e.menedzerSali;

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
import java.util.*;

public class MenedzerSaliFederate {
    private static final Integer SIMULATION_TIME = 500;

    public static final String READY_TO_RUN = "ReadyToRun";
    public static RTIambassador rtiamb;
    private MenedzerSaliFederateAmbassador fedamb;
    private HLAfloat64TimeFactory timeFactory;
    protected EncoderFactory encoderFactory;

    Map<ObjectInstanceHandle, Integer> klienciWKolejce = new LinkedHashMap<>();
    Map<ObjectInstanceHandle, Integer> klienciWRestauracji = new LinkedHashMap<>();
    Map<ObjectInstanceHandle, Stolik> stoliki = new HashMap<>();
    List<ObjectInstanceHandle> klienciZbytLiczni = new LinkedList<>();
    int najwiekszyStolik = 0;

    public void runFederate(String federateName) throws Exception {
        if (prepareFederate(federateName)) return;
        publishAndSubscribe();
        log("Published and Subscribed");
        List<ObjectInstanceHandle> clients = new ArrayList<>();
        boolean send = false;
        while (getTimeAsShort() <= SIMULATION_TIME || !klienciWRestauracji.isEmpty()) {
            if (getTimeAsShort()<= SIMULATION_TIME) {
                wyprosZbytLicznych();
                sprobujPosadzicKlientow();
            } else if (!send){
                sendZamkniecieRestauracjiInteraction(1);
                send = true;
            }

            advanceTime(1.0);
        }
        sendZamkniecieRestauracjiInteraction(2);
        destroyFederate(clients);
    }

    private void wyprosZbytLicznych() throws RTIexception {
        Iterator<ObjectInstanceHandle> iter = klienciZbytLiczni.iterator();
        while(iter.hasNext()){
            ObjectInstanceHandle klienci = iter.next();
            sendWyproszenieInteraction(klienci);
            log("Wyprosilem zbyt liczna grupe id:" + klienci);
            iter.remove();
        }
    }

    private void sprobujPosadzicKlientow() throws RTIexception {
        Iterator<Map.Entry<ObjectInstanceHandle, Integer>> iterator = klienciWKolejce.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry klient = iterator.next();
            Map.Entry stolik = znajdzStolik((int) klient.getValue());
            if (stolik != null) {
                log("Sadzam klienta " + klient.getKey() + " do stolika " + stolik.getKey());
                sendUsadowienieInteraction((ObjectInstanceHandle) klient.getKey(), (ObjectInstanceHandle) stolik.getKey());
                klienciWKolejce.remove(klient.getKey());
                klienciWRestauracji.put((ObjectInstanceHandle) klient.getKey(), (int) klient.getValue());

                Stolik stolikVal = ((Stolik)stolik.getValue());
                stolikVal.setZajety(true);
                stolikVal.setIdKlienta(Integer.valueOf(klient.getKey().toString()));
                stoliki.put((ObjectInstanceHandle) stolik.getKey(), stolikVal);

                break;
            }
        }
    }

    public Stolik getStolikByKlientId(int klientId){
        for(Stolik stolik : stoliki.values()){
            if (stolik.getIdKlienta() == klientId)
                return  stolik;
        }

        return null;
    }

    private Map.Entry znajdzStolik(int liczbaKlientow) {
        for (Map.Entry<ObjectInstanceHandle, Stolik> stolik : stoliki.entrySet()) {
            Stolik stolikValue = stolik.getValue();
            if (!stolikValue.isZajety() && stolikValue.getLiczbaMiejsc() >= liczbaKlientow) {
                return stolik;
            }
        }

        return null;
    }


    private void destroyFederate(List<ObjectInstanceHandle> clients) throws RTIexception {
        //Thread.sleep(5000);
        for (ObjectInstanceHandle objectHandle : clients) {
            deleteObject(objectHandle);
            log("Deleted Object, handle=" + objectHandle);
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
        fedamb = new MenedzerSaliFederateAmbassador(this);
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
        ObjectClassHandle klienciHandle = rtiamb.getObjectClassHandle("HLAobjectRoot.klienci");
        AttributeHandle idHandle = rtiamb.getAttributeHandle(klienciHandle, "id");
        AttributeHandle liczbaKlientowHandle = rtiamb.getAttributeHandle(klienciHandle, "liczbaKlientow");
        AttributeHandle czasStaniaWKolejceHandle = rtiamb.getAttributeHandle(klienciHandle, "czasStaniaWKolejce");
        AttributeHandle idStolikaHandle = rtiamb.getAttributeHandle(klienciHandle, "idStolika");
        AttributeHandle czasOczekiwaniaNaPosilekHandle = rtiamb.getAttributeHandle(klienciHandle, "czasOczekiwaniaNaPosilek");
        AttributeHandle czasSpozywaniaPosilkuHandle = rtiamb.getAttributeHandle(klienciHandle, "czasSpozywaniaPosilku");
        AttributeHandle czyNiecierpliwiHandle = rtiamb.getAttributeHandle(klienciHandle, "czyNiecierpliwi");

        AttributeHandleSet attributes = rtiamb.getAttributeHandleSetFactory().create();
        attributes.add(idHandle);
        attributes.add(liczbaKlientowHandle);
        attributes.add(czasStaniaWKolejceHandle);
        attributes.add(idStolikaHandle);
        attributes.add(czasOczekiwaniaNaPosilekHandle);
        attributes.add(czasSpozywaniaPosilkuHandle);
        attributes.add(czyNiecierpliwiHandle);

        rtiamb.subscribeObjectClassAttributes(klienciHandle, attributes);


        ObjectClassHandle stolikHandle = rtiamb.getObjectClassHandle("HLAobjectRoot.Stolik");
        AttributeHandle idStolikHandle = rtiamb.getAttributeHandle(stolikHandle, "id");
        AttributeHandle liczbaMiejscHandle = rtiamb.getAttributeHandle(stolikHandle, "liczbaMiejsc");

        AttributeHandleSet stolikAttributes = rtiamb.getAttributeHandleSetFactory().create();
        stolikAttributes.add(idStolikHandle);
        stolikAttributes.add(liczbaMiejscHandle);

        rtiamb.subscribeObjectClassAttributes(stolikHandle, stolikAttributes);

        InteractionClassHandle usadawianieKlientowHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.usadowienieKlientow");
        rtiamb.publishInteractionClass(usadawianieKlientowHandle);

        InteractionClassHandle zamkniecieRestauracjiHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.zamkniecieRestauracji");
        rtiamb.publishInteractionClass(zamkniecieRestauracjiHandle);
    }

    private void sendUsadowienieInteraction(ObjectInstanceHandle klientHandle, ObjectInstanceHandle stolikHandle) throws RTIexception {
        InteractionClassHandle usadawianieKlientowHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.usadowienieKlientow");
        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(2);

        ParameterHandle klientIdHandle = rtiamb.getParameterHandle(usadawianieKlientowHandle, "idKlientow");
        HLAinteger32BE klientId = encoderFactory.createHLAinteger32BE(Integer.valueOf(klientHandle.toString()));
        parameters.put(klientIdHandle, klientId.toByteArray());

        ParameterHandle stolikIdHandle = rtiamb.getParameterHandle(usadawianieKlientowHandle, "idStolika");
        HLAinteger32BE stolikId = encoderFactory.createHLAinteger32BE(Integer.valueOf(stolikHandle.toString()));
        parameters.put(stolikIdHandle, stolikId.toByteArray());

        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + fedamb.federateLookahead);

        rtiamb.sendInteraction(usadawianieKlientowHandle, parameters, generateTag(), time);
    }

    private void sendWyproszenieInteraction(ObjectInstanceHandle klientHandle) throws RTIexception {
        InteractionClassHandle usadawianieKlientowHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.usadowienieKlientow");
        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(2);

        ParameterHandle klientIdHandle = rtiamb.getParameterHandle(usadawianieKlientowHandle, "idKlientow");
        HLAinteger32BE klientId = encoderFactory.createHLAinteger32BE(Integer.valueOf(klientHandle.toString()));
        parameters.put(klientIdHandle, klientId.toByteArray());

        ParameterHandle stolikIdHandle = rtiamb.getParameterHandle(usadawianieKlientowHandle, "idStolika");
        HLAinteger32BE stolikId = encoderFactory.createHLAinteger32BE(-1);
        parameters.put(stolikIdHandle, stolikId.toByteArray());

        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + fedamb.federateLookahead);

        rtiamb.sendInteraction(usadawianieKlientowHandle, parameters, generateTag(), time);
    }

    private void sendZamkniecieRestauracjiInteraction(int typKomunikatu) throws RTIexception {
        InteractionClassHandle zamkniecieRestauracjiHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.zamkniecieRestauracji");
        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(1);

        ParameterHandle typKomunikatuHandle = rtiamb.getParameterHandle(zamkniecieRestauracjiHandle, "typKomunikatu");
        HLAinteger32BE typKomunikatuVal = encoderFactory.createHLAinteger32BE(typKomunikatu);
        parameters.put(typKomunikatuHandle, typKomunikatuVal.toByteArray());

        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + fedamb.federateLookahead);
        rtiamb.sendInteraction(zamkniecieRestauracjiHandle, parameters, generateTag(), time);

        klienciWKolejce.clear();
    }

    private void advanceTime(double timestep) throws RTIexception {

        fedamb.isAdvancing = true;
        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + timestep);
        rtiamb.timeAdvanceRequest(time);

        while (fedamb.isAdvancing) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }
    }

    void deleteObject(ObjectInstanceHandle handle) throws RTIexception {
        rtiamb.deleteObjectInstance(handle, generateTag());
    }

    public short getTimeAsShort() {
        return (short) (fedamb != null ? fedamb.federateTime : 0);
    }

    byte[] generateTag() {
        return ("(timestamp) " + System.currentTimeMillis()).getBytes();
    }

    public static void main(String[] args) {

        String federateName = "MenedzerSaliFederate";
        if (args.length != 0) {
            federateName = args[0];
        }

        try {
            new MenedzerSaliFederate().runFederate(federateName);
        } catch (Exception rtie) {
            rtie.printStackTrace();
        }
    }

    private void log(String message) {
        System.out.println("czas: " + getTimeAsShort() + " - MenedzerSaliFederate   : " + message);
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