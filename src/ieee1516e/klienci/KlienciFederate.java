package ieee1516e.klienci;

import hla.rti1516e.*;
import hla.rti1516e.encoding.*;
import hla.rti1516e.exceptions.*;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class KlienciFederate {
    private static final int MIN_CZAS_MIEDZY_KLIENTAMI = 5;
    private static final int MAX_CZAS_MIEDZY_KLIENTAMI = 15;
    private static final int MAKSYMALNA_LICZBA_KLIENTOW_W_GRUPIE = 10;

    private static final int PSTWO_NIECIERPLIWOSCI = 4;
    private static final int PSTWO_WYJSCIA_Z_KOLEJKI = 4;
    private static final int PSTWO_DODATKOWEGO_ZAMOWIENIA = 2;
    private static final int PSTWO_ZAKONCZENIA_POSILKU = 4;

    public static final String READY_TO_RUN = "ReadyToRun";
    public static RTIambassador rtiamb;
    private KlienciFederateAmbassador fedamb;
    private HLAfloat64TimeFactory timeFactory;
    protected EncoderFactory encoderFactory;
    protected Map<ObjectInstanceHandle, Klienci> clients = new LinkedHashMap<>();

    protected List<InterakcjaZMenedzeremEvent> interakcjaZMenedzeremEvents = new ArrayList<>();
    protected List<DostarczenieZamowieniaEvent> dostarczenieZamowieniaEvents = new ArrayList<>();

    public void runFederate(String federateName) throws Exception {
        if (prepareFederate(federateName)) return;

        publishAndSubscribe();
        log("Published and Subscribed");

        while (fedamb.running) {

            handleInterakcjaZMenedzeremEvent();
            handleDostarczenieZamowieniaEvent();
            updateNiecierpliwosc();
            updateJedzenie();

            ObjectInstanceHandle objectHandle = registerObject();
            Klienci klienci = updateAttributeValues(objectHandle);
            clients.put(objectHandle, klienci);
            log("Utworzono grupe " + klienci.getLiczbaKlientow() + " klientow, " + (klienci.isCzyNiecierpliwi() ? "niecierpliwi" : "cierpliwi") + " id=" + objectHandle);

            advanceTime(new Random().nextInt(MAX_CZAS_MIEDZY_KLIENTAMI - MIN_CZAS_MIEDZY_KLIENTAMI) + MIN_CZAS_MIEDZY_KLIENTAMI + 1);
            // log("Time Advanced to " + fedamb.federateTime);
        }
        log("Restauracja juz nie przyjmuje klientow");

        destroyFederate(clients);
    }

    private void handleDostarczenieZamowieniaEvent() throws NotConnected, FederateNotExecutionMember, NameNotFound, RTIinternalError, InvalidObjectClassHandle, ObjectInstanceNotKnown, RestoreInProgress, AttributeNotOwned, AttributeNotDefined, InvalidLogicalTime, SaveInProgress {
        Iterator<DostarczenieZamowieniaEvent> eventIterator = dostarczenieZamowieniaEvents.iterator();
        while (eventIterator.hasNext()) {
            DostarczenieZamowieniaEvent event = eventIterator.next();
            log("Dano nam jeść");
            eventIterator.remove();

            ObjectInstanceHandle klientHandle = getObjectInstanceHandleFromStolikId(event.getStolikId());
            Klienci klienci = clients.get(klientHandle);

            ObjectClassHandle klienciHandle = rtiamb.getObjectClassHandle("HLAobjectRoot.klienci");
            AttributeHandleValueMap attributes = rtiamb.getAttributeHandleValueMapFactory().create(3);

            AttributeHandle czasOczekiwaniaNaPosilekHandle = rtiamb.getAttributeHandle(klienciHandle, "czasOczekiwaniaNaPosilek");
            long czasOczekiwaniaNaPosilek = event.getCzas() - klienci.getCzasOczekiwaniaNaPosilek();
            HLAinteger64BE czasOczekiwaniaNaPosilekVal = encoderFactory.createHLAinteger64BE(czasOczekiwaniaNaPosilek);
            attributes.put(czasOczekiwaniaNaPosilekHandle, czasOczekiwaniaNaPosilekVal.toByteArray());

            AttributeHandle czasSpozywaniaPosilkukHandle = rtiamb.getAttributeHandle(klienciHandle, "czasSpozywaniaPosilku");
            HLAinteger64BE czasSpozywaniaPosilku = encoderFactory.createHLAinteger64BE(getTimeAsShort());
            attributes.put(czasSpozywaniaPosilkukHandle, czasSpozywaniaPosilku.toByteArray());
            klienci.setCzasSpozywaniaPosilku(getTimeAsShort());

            HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + fedamb.federateLookahead);
            rtiamb.updateAttributeValues(klientHandle, attributes, generateTag(), time);
        }
    }

    private void handleInterakcjaZMenedzeremEvent() throws RTIexception {
        Iterator<InterakcjaZMenedzeremEvent> eventIterator = interakcjaZMenedzeremEvents.iterator();
        while (eventIterator.hasNext()) {
            InterakcjaZMenedzeremEvent event = eventIterator.next();
            ObjectInstanceHandle objectInstanceHandle = getObjectInstanceHandleFromInt(event.getIdKlientow());
            Klienci klienci = clients.get(objectInstanceHandle);

            if (event.getIdStolika() != -1) {
                ObjectClassHandle klienciHandle = rtiamb.getObjectClassHandle("HLAobjectRoot.klienci");
                AttributeHandleValueMap attributes = rtiamb.getAttributeHandleValueMapFactory().create(3);

                AttributeHandle czasStaniaWKolejceHandle = rtiamb.getAttributeHandle(klienciHandle, "czasStaniaWKolejce");
                long czasStaniaVal = event.getCzas() - klienci.getCzasStaniaWKolejce();
                HLAinteger64BE czasStaniaWKolejce = encoderFactory.createHLAinteger64BE(czasStaniaVal);
                attributes.put(czasStaniaWKolejceHandle, czasStaniaWKolejce.toByteArray());
                klienci.setCzasStaniaWKolejce((short) czasStaniaVal);

                AttributeHandle czasOczekiwaniaNaPosilekHandle = rtiamb.getAttributeHandle(klienciHandle, "czasOczekiwaniaNaPosilek");
                HLAinteger64BE czasOczekiwaniaNaPosilek = encoderFactory.createHLAinteger64BE(getTimeAsShort());
                attributes.put(czasOczekiwaniaNaPosilekHandle, czasOczekiwaniaNaPosilek.toByteArray());
                klienci.setCzasOczekiwaniaNaPosilek(getTimeAsShort());

                AttributeHandle stolikHandle = rtiamb.getAttributeHandle(klienciHandle, "idStolika");
                HLAinteger32BE stolikId = encoderFactory.createHLAinteger32BE(event.getIdStolika());
                attributes.put(stolikHandle, stolikId.toByteArray());
                klienci.setIdStolika(event.getIdStolika());

                HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + fedamb.federateLookahead);
                rtiamb.updateAttributeValues(objectInstanceHandle, attributes, generateTag(), time);

                log("Klienci id:" + event.getIdKlientow() + " usiedli do stolika id:" + event.getIdStolika());

                sendZlozenieZamowieniaInteraction(klienci);
            } else {
                deleteObject(objectInstanceHandle);
                clients.remove(objectInstanceHandle);
                log("Klienci odeszli, id:" + event.getIdKlientow());
            }
            eventIterator.remove();
        }
    }

    private void sendZlozenieZamowieniaInteraction(Klienci klienci) throws NameNotFound, NotConnected, RTIinternalError, FederateNotExecutionMember, InvalidInteractionClassHandle, SaveInProgress, RestoreInProgress, InteractionClassNotPublished, InteractionClassNotDefined, InvalidLogicalTime, InteractionParameterNotDefined {
        InteractionClassHandle zlozenieZamowieniaHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.zlozenieZamowienia");
        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(2);

        ParameterHandle stolikIdHandle = rtiamb.getParameterHandle(zlozenieZamowieniaHandle, "idStolika");
        HLAinteger32BE stolikId = encoderFactory.createHLAinteger32BE(klienci.getIdStolika());
        parameters.put(stolikIdHandle, stolikId.toByteArray());

        ParameterHandle listaPosilkowHandle = rtiamb.getParameterHandle(zlozenieZamowieniaHandle, "listaPosilkow");
        HLAunicodeString listaPosilkow = encoderFactory.createHLAunicodeString("Posilki");
        parameters.put(listaPosilkowHandle, listaPosilkow.toByteArray());

        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + fedamb.federateLookahead);
        rtiamb.sendInteraction(zlozenieZamowieniaHandle, parameters, generateTag(), time);
    }

    public ObjectInstanceHandle getObjectInstanceHandleFromStolikId(int id) {
        for (Map.Entry<ObjectInstanceHandle, Klienci> entry : clients.entrySet()) {
            if (entry.getValue().getIdStolika() == id) {
                return entry.getKey();
            }
        }

        return null;
    }


    public ObjectInstanceHandle getObjectInstanceHandleFromInt(int id) {
        for (ObjectInstanceHandle obj : clients.keySet()) {
            if (obj.toString().equals(String.valueOf(id))) {
                return obj;
            }
        }

        return null;
    }

    private void updateNiecierpliwosc() throws RTIexception {
        Iterator<Map.Entry<ObjectInstanceHandle, Klienci>> iter = clients.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = iter.next();
            int val = new Random().nextInt(10) + 1;
            Klienci klienci = (Klienci) entry.getValue();
            if (klienci.isCzyNiecierpliwi() && klienci.getCzasOczekiwaniaNaPosilek() == -1 && val <= PSTWO_WYJSCIA_Z_KOLEJKI) {
                log("usuwam niecierpliwego klienta " + entry.getKey());
                ObjectInstanceHandle key = (ObjectInstanceHandle) entry.getKey();
                deleteObject(key);

                iter.remove();
            }
        }
    }

    private void updateJedzenie() throws RTIexception {
        Iterator<Map.Entry<ObjectInstanceHandle, Klienci>> iter = clients.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = iter.next();
            int val = new Random().nextInt(10) + 1;
            Klienci klienci = (Klienci) entry.getValue();
            if (klienci.getCzasSpozywaniaPosilku() != -1 && val <= PSTWO_ZAKONCZENIA_POSILKU) {
                if (klienci.isPierwszyPosilek() && new Random().nextInt(10) + 1 <= PSTWO_DODATKOWEGO_ZAMOWIENIA) {
                    klienci.setPierwszyPosilek(false);
                    log("Klienci zamawiaja drugi posilek");

                    sendZlozenieZamowieniaInteraction(klienci);
                } else {
                    ObjectInstanceHandle key = (ObjectInstanceHandle) entry.getKey();
                    deleteObject(key);
                    log("Klient sobie idzie " + entry.getKey());
                    iter.remove();
                }

            }
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

        rtiamb.publishObjectClassAttributes(klienciHandle, attributes);

        //SUBSCRIBE UsadowienieKlientow
        InteractionClassHandle usadawianieKlientowHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.usadowienieKlientow");
        rtiamb.subscribeInteractionClass(usadawianieKlientowHandle);

        //PUBLISH Złożenie zamówienia
        InteractionClassHandle zlozenieZamowieniaHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.zlozenieZamowienia");
        rtiamb.publishInteractionClass(zlozenieZamowieniaHandle);

        InteractionClassHandle dostarczenieZamowieniaHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.dostarczenieZamowienia");
        rtiamb.subscribeInteractionClass(dostarczenieZamowieniaHandle);

        InteractionClassHandle zamkniecieRestauracjiHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.zamkniecieRestauracji");
        rtiamb.subscribeInteractionClass(zamkniecieRestauracjiHandle);
    }

    private ObjectInstanceHandle registerObject() throws RTIexception {
        ObjectClassHandle klienciHandle = rtiamb.getObjectClassHandle("HLAobjectRoot.klienci");
        return rtiamb.registerObjectInstance(klienciHandle);
    }

    private Klienci updateAttributeValues(ObjectInstanceHandle objectHandle) throws RTIexception {
        ObjectClassHandle klienciHandle = rtiamb.getObjectClassHandle("HLAobjectRoot.klienci");
        AttributeHandleValueMap attributes = rtiamb.getAttributeHandleValueMapFactory().create(3);
        Klienci klienci = new Klienci();

        AttributeHandle liczbaKlientowHandle = rtiamb.getAttributeHandle(klienciHandle, "liczbaKlientow");

        int liczbaKlientowVal = (new Random().nextInt(MAKSYMALNA_LICZBA_KLIENTOW_W_GRUPIE)) + 1;
        HLAinteger32BE liczbaKlientow = encoderFactory.createHLAinteger32BE(liczbaKlientowVal);
        attributes.put(liczbaKlientowHandle, liczbaKlientow.toByteArray());
        klienci.setLiczbaKlientow(liczbaKlientowVal);

        AttributeHandle czasStaniaWKolejceHandle = rtiamb.getAttributeHandle(klienciHandle, "czasStaniaWKolejce");
        HLAinteger64BE czasStaniaWKolejce = encoderFactory.createHLAinteger64BE(getTimeAsShort());
        attributes.put(czasStaniaWKolejceHandle, czasStaniaWKolejce.toByteArray());
        klienci.setCzasStaniaWKolejce(getTimeAsShort());

        AttributeHandle czyNiecierpliwiHandle = rtiamb.getAttributeHandle(klienciHandle, "czyNiecierpliwi");
        boolean czyNiecierpliwiValue = (new Random().nextInt(10) + 1) <= PSTWO_NIECIERPLIWOSCI;
        HLAboolean czyNiecierpliwi = encoderFactory.createHLAboolean(czyNiecierpliwiValue);
        attributes.put(czyNiecierpliwiHandle, czyNiecierpliwi.toByteArray());
        klienci.setCzyNiecierpliwi(czyNiecierpliwiValue);

        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + fedamb.federateLookahead);
        rtiamb.updateAttributeValues(objectHandle, attributes, generateTag(), time);

        return klienci;
    }

    private void advanceTime(double timestep) throws RTIexception {

        fedamb.isAdvancing = true;
        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + timestep);
        rtiamb.timeAdvanceRequest(time);

        while (fedamb.isAdvancing) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }
    }

    private void destroyFederate(Map<ObjectInstanceHandle, Klienci> clients) throws RTIexception {
        for (ObjectInstanceHandle oh : clients.keySet()) {
            deleteObject(oh);
            log("Deleted Object, handle=" + oh);
        }
        clients.clear();

        rtiamb.resignFederationExecution(ResignAction.DELETE_OBJECTS);
        log("Resigned from Federation");

        try {
            rtiamb.destroyFederationExecution("ExampleFederation");
            log("Destroyed Federation");
        } catch (FederationExecutionDoesNotExist dne) {
            log("No need to destroy federation, it doesn't exist");
        } catch (FederatesCurrentlyJoined fcj) {
            log("Didn't destroy federation, federates still joined");
        }
    }

    private boolean prepareFederate(String federateName) throws Exception {
        log("Creating RTIambassador");
        rtiamb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
        encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();

        log("Connecting...");
        fedamb = new KlienciFederateAmbassador(this);
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
                "KlienciFederateType",   // federate type
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
        this.rtiamb.enableTimeRegulation(lookahead);
        while (!fedamb.isRegulating) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }
        this.rtiamb.enableTimeConstrained();

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

    byte[] generateTag() {
        return ("(timestamp) " + System.currentTimeMillis()).getBytes();
    }

    public static void main(String[] args) {

        String federateName = "klienciFederate";
        if (args.length != 0) {
            federateName = args[0];
        }
        try {
            new KlienciFederate().runFederate(federateName);
        } catch (Exception rtie) {
            rtie.printStackTrace();
        }
    }

    private void log(String message) {
        System.out.println("czas: " + getTimeAsShort() + " - KlienciFederate   : " + message);
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