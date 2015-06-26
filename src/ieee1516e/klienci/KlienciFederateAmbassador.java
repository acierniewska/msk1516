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
package ieee1516e.klienci;

import hla.rti1516e.*;
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.HLAinteger16BE;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.exceptions.*;
import hla.rti1516e.time.HLAfloat64Time;
import ieee1516e.DecoderUtils;

public class KlienciFederateAmbassador extends NullFederateAmbassador {
    private KlienciFederate federate;

    protected double federateTime = 0.0;
    protected double federateLookahead = 1.0;

    protected boolean isRegulating = false;
    protected boolean isConstrained = false;
    protected boolean isAdvancing = false;

    protected boolean isAnnounced = false;
    protected boolean isReadyToRun = false;
    protected boolean running = true;


    public KlienciFederateAmbassador(KlienciFederate federate) {
        this.federate = federate;
    }

    private void log(String message) {
        System.out.println("czas: " + federate.getTimeAsShort() + " - FederateAmbassador: " + message);
    }

    private String decodeFlavor(byte[] bytes) {
        HLAinteger32BE value = federate.encoderFactory.createHLAinteger32BE();
        try {
            value.decode(bytes);
        } catch (DecoderException de) {
            return "Decoder Exception: " + de.getMessage();
        }

        switch (value.getValue()) {
            case 101:
                return "Cola";
            case 102:
                return "Orange";
            case 103:
                return "RootBeer";
            case 104:
                return "Cream";
            default:
                return "Unknown";
        }
    }

    private short decodeNumCupups(byte[] bytes) {
        HLAinteger16BE value = federate.encoderFactory.createHLAinteger16BE();
        try {
            value.decode(bytes);
            return value.getValue();
        } catch (DecoderException de) {
            de.printStackTrace();
            return 0;
        }
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
        if (label.equals(KlienciFederate.READY_TO_RUN))
            this.isAnnounced = true;
    }

    @Override
    public void federationSynchronized(String label, FederateHandleSet failed) {
        log("Federation Synchronized: " + label);
        if (label.equals(KlienciFederate.READY_TO_RUN))
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
        log("Discoverd Object: handle=" + theObject + ", classHandle=" +
                theObjectClass + ", name=" + objectName);
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
        StringBuilder builder = new StringBuilder("Reflection for object:");

        builder.append(" handle=" + theObject);
        builder.append(", tag=" + new String(tag));
        if (time != null) {
            builder.append(", time=" + ((HLAfloat64Time) time).getValue());
        }

        builder.append(", attributeCount=" + theAttributes.size());
        builder.append("\n");
        /*for( AttributeHandle attributeHandle : theAttributes.keySet() )
        {
			builder.append( "\tattributeHandle=" );

			if( attributeHandle.equals(federate.flavHandle) )
			{
				builder.append( attributeHandle );
				builder.append( " (Flavor)    " );
				builder.append( ", attributeValue=" );
				builder.append( decodeFlavor(theAttributes.get(attributeHandle)) );
			}
			else if( attributeHandle.equals(federate.cupsHandle) )
			{
				builder.append( attributeHandle );
				builder.append( " (NumberCups)" );
				builder.append( ", attributeValue=" );
				builder.append( decodeNumCups(theAttributes.get(attributeHandle)) );
			}
			else
			{
				builder.append( attributeHandle );
				builder.append( " (Unknown)   " );
			}
			
			builder.append( "\n" );
		}*/

        log(builder.toString());
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
            if (interactionClass.equals(KlienciFederate.rtiamb.getInteractionClassHandle("HLAinteractionRoot.usadowienieKlientow"))) {
                int idKlienta = 0;
                int idStolika = 0;
                for (ParameterHandle parameter : theParameters.keySet()) {
                    if (parameter.equals(KlienciFederate.rtiamb.getParameterHandle(interactionClass, "idKlientow"))) {
                        idKlienta = DecoderUtils.decodeInteger(theParameters.get(parameter));

                    } else if (parameter.equals(KlienciFederate.rtiamb.getParameterHandle(interactionClass, "idStolika"))) {
                        idStolika = DecoderUtils.decodeInteger(theParameters.get(parameter));

                    }
                }
                InterakcjaZMenedzeremEvent event = new InterakcjaZMenedzeremEvent(idKlienta, idStolika, (long) ((HLAfloat64Time) time).getValue());
                federate.interakcjaZMenedzeremEvents.add(event);
                if(idStolika != -1) {
                    log("Grupa klientow usadzona, id:" + idKlienta + ", id stolika:" + idStolika + ", czas:" + ((HLAfloat64Time) time).getValue());
                } else {
                    log("Grupa klientow zbyt duza, id:" + idKlienta + ", czas:" + ((HLAfloat64Time) time).getValue());
                }

            } else if (interactionClass.equals(KlienciFederate.rtiamb.getInteractionClassHandle("HLAinteractionRoot.dostarczenieZamowienia"))) {
                int idStolika = 0;
                String listaPosilkow = "";
                for (ParameterHandle parameter : theParameters.keySet()) {
                    if (parameter.equals(KlienciFederate.rtiamb.getParameterHandle(interactionClass, "listaPosilkow"))) {
                        listaPosilkow = DecoderUtils.decodeString(theParameters.get(parameter));
                    } else if (parameter.equals(KlienciFederate.rtiamb.getParameterHandle(interactionClass, "idStolika"))) {
                        idStolika = DecoderUtils.decodeInteger(theParameters.get(parameter));

                    }
                }
                DostarczenieZamowieniaEvent event = new DostarczenieZamowieniaEvent(idStolika, listaPosilkow, (long) ((HLAfloat64Time) time).getValue());
                federate.dostarczenieZamowieniaEvents.add(event);
                log("Klienci dostali " + listaPosilkow + " do stolika " + idStolika + ", czas" + ((HLAfloat64Time) time).getValue());

            } else if (interactionClass.equals(KlienciFederate.rtiamb.getInteractionClassHandle("HLAinteractionRoot.zamkniecieRestauracji"))){
                log("KONIEC!");
                running = false;
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
        log("Object Removed: handle=" + theObject);
    }
}
