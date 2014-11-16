package nl.fah.stimulator;

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.IOException;

/**
 * Created by Haulussy on 14-11-2014.
 */
public class Validator {

    public static boolean Validate(String xml, String xsd, StringBuilder message){
        Source schemaFile = new StreamSource(new File(xsd));
        Source xmlFile = new StreamSource(new File(xml));
        SchemaFactory schemaFactory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = null;
        try {
            schema = schemaFactory.newSchema(schemaFile);
        } catch (SAXException e) {
            e.printStackTrace();
        }
        javax.xml.validation.Validator validator = schema.newValidator();
        try {
            validator.validate(xmlFile);
        } catch (SAXException e) {
            message.append(e.getLocalizedMessage());
            return false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        message.append("xml is valid");
        return true;
    }


}
