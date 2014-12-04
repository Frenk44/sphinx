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

    public static boolean Validate(String xmlFile, String xsdFile, StringBuilder message){
        Source schemaFile = new StreamSource(new File(xsdFile));
        Source xmlSrc = new StreamSource(new File(xmlFile));
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
            validator.validate(xmlSrc);
        } catch (SAXException e) {
            message.append(e.getLocalizedMessage());
            return false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        message.append("xml is valid");
        return true;
    }

    public static boolean ValidateSource(String xmlString, String xsdFile, StringBuilder message){
        Source schemaFile = new StreamSource(new File(xsdFile));
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
            Source src = new StreamSource(new java.io.StringReader(xmlString));
            validator.validate(src);
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
