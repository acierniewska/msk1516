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
package ieee1516e.menedzerSali;

import hla.rti1516e.*;
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Time;
import ieee1516e.DecoderUtils;

public class MenedzerSaliFederateAmbassador extends NullFederateAmbassador {
    private MenedzerSaliFederate federate;

    protected double federateTime = 0.0;
    protected double federateLookahead = 1.0;

    protected boolean isRegulating = false;
    protected boolean isConstrained = false;
    protected boolean isAdvancing = false;

    protected boolean isAnnounced = false;
    protected boolean isReadyToRun = false;
/*    protected boolean running = true;*/


    public MenedzerSaliFederateAmbassador(MenedzerSaliFederate federate) {
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
        if (label.equals(MenedzerSaliFederate.READY_TO_RUN))
            this.isAnnounced = true;
    }

    @Override
    public void federationSynchronized(String label, FederateHandleSet failed) {
        log("Federation Synchronized: " + label);
        if (label.equals(MenedzerSaliFederate.READY_TO_RUN))
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
            ObjectClassHandle klienciHandle = MenedzerSaliFederate.rtiamb.getObjectClassHandle("HLAobjectRoot.Klienci");
            ObjectClassHandle stolikiHandle = MenedzerSaliFederate.rtiamb.getObjectClassHandle("HLAobjectRoot.Stolik");

            for (AttributeHandle attributeHandle : theAttributes.keySet()) {
                if (attributeHandle.equals(MenedzerSaliFederate.rtiamb.getAttributeHandle(klienciHandle, "liczbaKlientow"))) {

                    Integer liczbaKlientowWGrupie = DecoderUtils.decodeInteger(theAttributes.get(attributeHandle));
                    if(liczbaKlientowWGrupie <= federate.najwiekszyStolik) {
                        federate.klienciWKolejce.put(theObject, liczbaKlientowWGrupie);
                        log("Klienci w kolejce, liczba klientow " + federate.klienciWKolejce.get(theObject) + ", id " + theObject + " , czas: " + ((HLAfloat64Time) time).getValue());
                    } else {
                        federate.klienciZbytLiczni.add(theObject);
                        log("Klienci zbyt liczni (" + liczbaKlientowWGrupie + " osob), id:" + theObject + " , czas: " + ((HLAfloat64Time) time).getValue());
                    }
                } else if (attributeHandle.equals(MenedzerSaliFederate.rtiamb.getAttributeHandle(stolikiHandle, "liczbaMiejsc"))) {
                    Integer liczbaMiejsc = DecoderUtils.decodeInteger(theAttributes.get(attributeHandle));
                    Stolik stolik = new Stolik(liczbaMiejsc, false);
                    federate.stoliki.put(theObject, stolik);
                    if (federate.najwiekszyStolik < liczbaMiejsc) {
                        federate.najwiekszyStolik = liczbaMiejsc;
                    }
                    log("LiczbaMiejsc " + liczbaMiejsc + ", czas " + ((HLAfloat64Time) time).getValue());
                }
            }
        } catch (RTIexception rtIexception) {
            rtIexception.printStackTrace();
        }
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
        StringBuilder builder = new StringBuilder("Interaction Received:");

        // print the handle
        builder.append(" handle=" + interactionClass);
        /*
         * if( interactionClass.equals(federate.usadawianieKlientowHandle) ){
		 * builder.append( " (DrinkServed)" ); }
		 */

        builder.append(", tag=" + new String(tag));

        if (time != null) {
            builder.append(", time=" + ((HLAfloat64Time) time).getValue());
        }

        builder.append(", parameterCount=" + theParameters.size());
        builder.append("\n");
        for (ParameterHandle parameter : theParameters.keySet()) {

            builder.append("\tparamHandle=");
            builder.append(parameter);
            builder.append(", paramValue=");
            builder.append(theParameters.get(parameter).length);
            builder.append(" bytes");
            builder.append("\n");
        }

        log(builder.toString());
    }

    @Override
    public void removeObjectInstance(ObjectInstanceHandle theObject,
                                     byte[] tag,
                                     OrderType sentOrdering,
                                     SupplementalRemoveInfo removeInfo)
            throws FederateInternalError {


        if(federate.klienciWKolejce.containsKey(theObject)) {
            federate.klienciWKolejce.remove(theObject);
            log("Object Removed: handle=" + theObject);
        } else if(federate.klienciWRestauracji.containsKey(theObject)){
            Stolik s = federate.getStolikByKlientId(Integer.valueOf(theObject.toString()));
            s.setZajety(false);
            federate.klienciWRestauracji.remove(theObject);
            log("Zwalniam stolik " + s.getLiczbaMiejsc());
        }
    }
}
