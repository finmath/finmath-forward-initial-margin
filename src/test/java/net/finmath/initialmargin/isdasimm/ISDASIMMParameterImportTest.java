package net.finmath.initialmargin.isdasimm;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import net.finmath.xva.initialmargin.SIMMParameter;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Scanner;

import static java.lang.System.exit;

public class ISDASIMMParameterImportTest {

    @Test
    public void testDataRetrievableISDASIMMParameterSet(){

        try {

            Gson gson = new Gson();


            String content = new Scanner(new File("simm.json")).next();
            SIMMParameter parameter = new SIMMParameter(content);
            String[] maturityBuckets = parameter.IRMaturityBuckets;

        }
        catch(Exception e){
            System.out.println(e);
        }
    }


}
