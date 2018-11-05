package net.finmath.initialmargin.isdasimm;

import java.io.File;
import java.util.Scanner;

import org.junit.Test;

import com.google.gson.Gson;

import net.finmath.xva.initialmargin.SIMMParameter;

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
