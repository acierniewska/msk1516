package ieee1516e;

import hla.rti1516e.RtiFactoryFactory;
import hla.rti1516e.encoding.*;
import hla.rti1516e.exceptions.RTIinternalError;

public class DecoderUtils {

    public static Integer decodeInteger(byte[] bytes) {
        HLAinteger32BE value = null;
        try {
            EncoderFactory encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();
            value = encoderFactory.createHLAinteger32BE();
            value.decode(bytes);
        } catch (RTIinternalError rtIinternalError) {
            rtIinternalError.printStackTrace();
        } catch (DecoderException de) {
            return -1;
        }

        return value != null ? value.getValue() : -1;
    }

    public static Long decodeLong(byte[] bytes) {
        HLAinteger64BE value = null;
        try {
            EncoderFactory encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();
            value = encoderFactory.createHLAinteger64BE();
            value.decode(bytes);
        } catch (RTIinternalError rtIinternalError) {
            rtIinternalError.printStackTrace();
        } catch (DecoderException de) {
            return -1L;
        }

        return value != null ? value.getValue() : -1;
    }

    public static boolean decodeBoolean(byte[] bytes){
        HLAboolean value = null;
        try {
            EncoderFactory encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();
            value = encoderFactory.createHLAboolean();
            value.decode(bytes);
        } catch (RTIinternalError rtIinternalError) {
            rtIinternalError.printStackTrace();
        } catch (DecoderException de) {
            return false;
        }

        return value != null && value.getValue();
    }

    public static String decodeString(byte[] bytes) {
        HLAunicodeString value = null;
        try {
            EncoderFactory encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();
            value = encoderFactory.createHLAunicodeString();
            value.decode(bytes);
        } catch (RTIinternalError rtIinternalError) {
            rtIinternalError.printStackTrace();
        } catch (DecoderException de) {
            return "";
        }

        return value != null ? value.getValue() : "";

    }
}
