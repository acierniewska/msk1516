/*
 *   Copyright 2012 The Portico Project
 *
 *   This file is part of portico.
 *
 *   portico is free software; you can redistribute it and/or modify
 *   it under the terms of the Common Developer and Distribution License (CDDL) 
 *   as published by Sun Microsystems. For more information see the LICENSE file.
 *   
 *   Use of this software is strictly AT YOUR OWN RISK!!!
 *   If something bad happens you do not have permission to come crying to me.
 *   (that goes for your lawyer as well)
 *
 */
package ieee1516e.statystyka;

import hla.rti1516e.*;
import hla.rti1516e.exceptions.*;
import hla.rti1516e.time.HLAfloat64Time;
import ieee1516e.DecoderUtils;

public class StatystykaFederateAmbassador extends NullFederateAmbassador {
    private StatystykaFederate federate;

    protected double federateTime = 0.0;
    protected double federateLookahead = 1.0;

    protected boolean isRegulating = false;
    protected boolean isConstrained = false;
    protected boolean isAdvancing = false;

    protected boolean isAnnounced = false;
    protected boolean isReadyToRun = false;
    protected boolean running = true;


    public StatystykaFederateAmbassador(StatystykaFederate federate) {
        this.federate = federate;
    }

    private void log(String message) {
        System.out.println("czas: " + federate.getTimeAsShort() + " - FederateAmbassador: " + message);
    }

    @Override
    public void synchronizationPointRegistrationFailed(String label, SynchronizationPointFailureReason reason) {
        log("Failed to register sync point: " + label + ", reason=" + reason);
    }

    @Override
    public void synchronizationPointRegistrationSucceeded(String label) {
        log("Successfully registered sync point: " + label);
    }

    @Override
    public void announceSynchronizationPoint(String label, byte[] tag) {
        log("Synchronization point announced: " + label);
        if (label.equals(StatystykaFederate.READY_TO_RUN))
            this.isAnnounced = true;
    }

    @Override
    public void federationSynchronized(String label, FederateHandleSet failed) {
        log("Federation Synchronized: " + label);
        if (label.equals(StatystykaFederate.READY_TO_RUN))
            this.isReadyToRun = true;
    }

    @Override
    public void timeRegulationEnabled(LogicalTime time) {
        this.federateTime = ((HLAfloat64Time) time).getValue();
        this.isRegulating = true;
    }

    @Override
    public void timeConstrainedEnabled(LogicalTime time) {
        this.federateTime = ((HLAfloat64Time) time).getValue();
        this.isConstrained = true;
    }

    @Override
    public void timeAdvanceGrant(LogicalTime time) {
        this.federateTime = ((HLAfloat64Time) time).getValue();
        this.isAdvancing = false;
    }

    @Override
    public void discoverObjectInstance(ObjectInstanceHandle theObject, ObjectClassHandle theObjectClass, String objectName) throws FederateInternalError {
    }

    @Override
    public void reflectAttributeValues(ObjectInstanceHandle theObject, AttributeHandleValueMap theAttributes, byte[] tag, OrderType sentOrder, TransportationTypeHandle transport,
                                       SupplementalReflectInfo reflectInfo) throws FederateInternalError {

        reflectAttributeValues(theObject, theAttributes, tag, sentOrder, transport, null, sentOrder, reflectInfo);
    }

    @Override
    public void reflectAttributeValues(ObjectInstanceHandle theObject,
                                       AttributeHandleValueMap theAttributes,
                                       byte[] tag,
                                       OrderType sentOrdering,
                                       TransportationTypeHandle theTransport,
                                       LogicalTime time,
                                       OrderType receivedOrdering,
                                       SupplementalReflectInfo reflectInfo)
            throws FederateInternalError {
        try {
            ObjectClassHandle klienciHandle = StatystykaFederate.rtiamb.getObjectClassHandle("HLAobjectRoot.Klienci");

            for (AttributeHandle attributeHandle : theAttributes.keySet()) {
                if (attributeHandle.equals(StatystykaFederate.rtiamb.getAttributeHandle(klienciHandle, "czasStaniaWKolejce"))) {
                    Klienci klienci = getKlienci(theObject);

                    Long czasStaniaWKolejce = DecoderUtils.decodeLong(theAttributes.get(attributeHandle));
                    if(klienci.getCzasStaniaWKolejce() != -1L){
                        klienci.setCzyUsiadl(true);
                        log("Czas w kolejce zaktualizowany, id:" + theObject + ", czas:" + czasStaniaWKolejce);
                    }
                    klienci.setCzasStaniaWKolejce(czasStaniaWKolejce);
                } else if (attributeHandle.equals(StatystykaFederate.rtiamb.getAttributeHandle(klienciHandle, "czasOczekiwaniaNaPosilek"))) {
                    Klienci klienci = getKlienci(theObject);

                    Long czasOczekiwaniaNaPosilek = DecoderUtils.decodeLong(theAttributes.get(attributeHandle));
                    if(klienci.getCzasOczekiwaniaNaPosilek() != -1L){
                        klienci.setCzyDostalPosilek(true);
                        log("Czas oczekiwania na posilek zaktualizowany, id:" + theObject + ", czas:" + czasOczekiwaniaNaPosilek);
                    }
                    klienci.setCzasOczekiwaniaNaPosilek(czasOczekiwaniaNaPosilek);
                }
            }
        } catch (RTIexception rtIexception) {
            rtIexception.printStackTrace();
        }
    }

    private Klienci getKlienci(ObjectInstanceHandle theObject) {
        if (!federate.klienci.containsKey(theObject)) {
            Klienci klienci = new Klienci();
            federate.klienci.put(theObject, klienci);
        }
        return federate.klienci.get(theObject);
    }

    @Override
    public void receiveInteraction(InteractionClassHandle interactionClass,
                                   ParameterHandleValueMap theParameters,
                                   byte[] tag,
                                   OrderType sentOrdering,
                                   TransportationTypeHandle theTransport,
                                   SupplementalReceiveInfo receiveInfo)
            throws FederateInternalError {

        this.receiveInteraction(interactionClass,
                theParameters,
                tag,
                sentOrdering,
                theTransport,
                null,
                sentOrdering,
                receiveInfo);
    }

    @Override
    public void receiveInteraction(InteractionClassHandle interactionClass,
                                   ParameterHandleValueMap theParameters,
                                   byte[] tag,
                                   OrderType sentOrdering,
                                   TransportationTypeHandle theTransport,
                                   LogicalTime time,
                                   OrderType receivedOrdering,
                                   SupplementalReceiveInfo receiveInfo)
            throws FederateInternalError {
        try {
            if (interactionClass.equals(StatystykaFederate.rtiamb.getInteractionClassHandle("HLAinteractionRoot.zamkniecieRestauracji"))){
                for (ParameterHandle parameter : theParameters.keySet()) {
                    if (parameter.equals(StatystykaFederate.rtiamb.getParameterHandle(interactionClass, "typKomunikatu"))) {
                        int typKomunikatu = DecoderUtils.decodeInteger(theParameters.get(parameter));
                        if (typKomunikatu == 2) {
                            log("KONIEC!");
                            running = false;
                        }
                    }
                }
            }
        } catch (NameNotFound | FederateNotExecutionMember | NotConnected | RTIinternalError | InvalidInteractionClassHandle nameNotFound) {
            nameNotFound.printStackTrace();
        }
    }

    @Override
    public void removeObjectInstance(ObjectInstanceHandle theObject,
                                     byte[] tag,
                                     OrderType sentOrdering,
                                     SupplementalRemoveInfo removeInfo)
            throws FederateInternalError {


        log("Usunieto obiekt klienta, sredni czas w kolejce:" +federate.obliczSredniCzasWKolejce() + ", sredni czas oczekiwania na posilek:" + federate.obliczSredniCzasOczekiwania());
        if (!federate.klienci.get(theObject).isCzyUsiadl()){
            federate.utraceniKlienci++;
        }
    }
}
