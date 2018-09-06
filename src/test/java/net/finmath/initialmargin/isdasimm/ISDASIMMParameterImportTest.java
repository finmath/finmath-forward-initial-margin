package net.finmath.initialmargin.isdasimm;

import com.google.gson.Gson;
import net.finmath.xva.initialmargin.SIMMParameter;
import org.junit.Test;

import java.io.File;
import java.util.Scanner;

public class ISDASIMMParameterImportTest {

	@Test
	public void testDataRetrievableISDASIMMParameterSet() {

		try {

			Gson gson = new Gson();

			String content = new Scanner(new File("simm.json")).next();
			SIMMParameter parameter = new SIMMParameter(content);
			String[] maturityBuckets = parameter.IRMaturityBuckets;
		} catch (Exception e) {
			System.out.println(e);
		}
	}
}
