package nl.fah.test;

import nl.fah.stimulator.Validator;

/**
 * Created by Haulussy on 15-11-2014.
 */
public class testValidator {

    public static void main(String[] args) throws Exception {

        StringBuilder m = new StringBuilder();
        Validator.Validate("src/main/resources/templates/model1/geloof.xml","src/main/resources/data.xsd", m );
        System.out.println("validator message=" + m);

    }

}
