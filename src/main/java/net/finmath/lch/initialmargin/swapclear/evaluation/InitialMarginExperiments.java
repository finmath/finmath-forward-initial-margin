package net.finmath.lch.initialmargin.swapclear.evaluation;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import net.finmath.lch.initialmargin.simulation.modeldata.RandomVariableSeries;
import net.finmath.lch.initialmargin.simulation.modeldata.TenorGridFactory;
import net.finmath.lch.initialmargin.simulation.modeldata.ZeroRateModel;
import net.finmath.lch.initialmargin.simulation.modeldata.TenorGridFactory.GridType;
import net.finmath.lch.initialmargin.simulation.scenarios.CurveScenarios;
import net.finmath.lch.initialmargin.simulation.scenarios.ScenarioFactory;
import net.finmath.lch.initialmargin.simulation.scenarios.ScenarioFactory.Simulation;
import net.finmath.lch.initialmargin.swapclear.aggregationscheme.SchemeStorage;
import net.finmath.lch.initialmargin.swapclear.aggregationscheme.PairsInitialMargin;
import net.finmath.lch.initialmargin.swapclear.aggregationscheme.SchemeStorage.Currency;
import net.finmath.lch.initialmargin.swapclear.aggregationscheme.SchemeStorage.CurveName;
import net.finmath.lch.initialmargin.swapclear.evaluation.MarginValuationAdjustment.InitialMargin;
import net.finmath.lch.initialmargin.swapclear.libormodels.ModelFactory;
import net.finmath.lch.initialmargin.swapclear.products.LchSwap;
import net.finmath.lch.initialmargin.swapclear.products.LocalPortfolio;
import net.finmath.lch.initialmargin.swapclear.products.components.LchSwapLeg;
import net.finmath.lch.initialmargin.swapclear.sensitivities.DiscountSensitivities;
import net.finmath.lch.initialmargin.swapclear.sensitivities.ForwardAndDiscountSensitivities;
import net.finmath.lch.initialmargin.swapclear.sensitivities.ForwardSensitivities;
import net.finmath.lch.initialmargin.swapclear.sensitivities.Sensitivities;
import net.finmath.lch.initialmargin.swapclear.sensitivities.SensitivityComponentsForSwapLeg;
import net.finmath.lch.initialmargin.swapclear.sensitivities.SensitivityMatrix;
import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.FloatingpointDate;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar;



public class InitialMarginExperiments {
	
	
	// Sensitivity buckets of a 5y swap
	private static final double[] BUCKETS = new double[] {1/365.0, 7/365.0, 14/365.0, 21/365.0, 30/365.0, 60/365.0, 91/365.0, 121/365.0, 152/365.0, 182/365.0, 
			212/365.0, 243/365.0, 273/365.0, 304/365.0, 334/365.0, 365/365.0, 456/365.0, 547/365.0, 638/365.0, 730/365.0, 1095/365.0, 1460/365.0, 1825/365.0, 2190/365.0};
	
	/*
	 * Last date of available zero rate data
	 * LMM's of ModelFactory are calibrated to the same date
	 */
	private static final LocalDateTime REFERENCE_DATE = ModelFactory.REFERENCE_DATE; 

	
	public enum SensitivityType 	{FORWARD, DISCOUNT, FORWARD_AND_DISCOUNT};
	public enum SensitivityOrder 	{DELTA, GAMMA};

	
	public static void main (String[] args) throws CalculationException {
				
		
		LchSwap swap1 = PortfolioFactory.create15YPayerSwap();
		LchSwap swap2 = PortfolioFactory.create15YReceiverSwap();

		LocalPortfolio localPortfolio = PortfolioFactory.getSwapPortfolio(swap1).getLocalPortfolio(Currency.EUR); //, swap10Y, swap10YPlusSpread);	
		LocalPortfolio localPortfolio2 = PortfolioFactory.getSwapPortfolio(swap2).getLocalPortfolio(Currency.EUR); //, swap10Y, swap10YPlusSpread);	

		boolean movingWindow = true;
		boolean pathWiseScenarios = false;
		
//		getInitialMarginPaths(localPortfolio, "D:/Finanzmathematik/Masterthesis/Arbeit/Results/15yPayerSwap/IM 15yPayerSwap - Pairs Fixed Average.xlsx", "Paths", InitialMargin.PAIRS, false, false);
//		getInitialMarginPaths(localPortfolio, "D:/Finanzmathematik/Masterthesis/Arbeit/Results/15yPayerSwap/IM 15yPayerSwap - Pairs Moving Average.xlsx", "Paths", InitialMargin.PAIRS, false, true);
//		getInitialMarginPaths(localPortfolio, "D:/Finanzmathematik/Masterthesis/Arbeit/Results/15yPayerSwap/IM 15yPayerSwap - Pairs Fixed Pathwise.xlsx", "Paths", InitialMargin.PAIRS, true, false);
//		getInitialMarginPaths(localPortfolio, "D:/Finanzmathematik/Masterthesis/Arbeit/Results/15yPayerSwap/IM 15yPayerSwap - Pairs Moving Pathwise.xlsx", "Paths", InitialMargin.PAIRS, true, true);

//		getInitialMarginPaths(localPortfolio2, "D:/Finanzmathematik/Masterthesis/Arbeit/Results/15yReceiverSwap/IM 15yReceiverSwap - Pairs Fixed Average.xlsx", "Paths", InitialMargin.PAIRS, false, false);
//		getInitialMarginPaths(localPortfolio2, "D:/Finanzmathematik/Masterthesis/Arbeit/Results/15yReceiverSwap/IM 15yReceiverSwap - Pairs Moving Average.xlsx", "Paths", InitialMargin.PAIRS, false, true);
		getInitialMarginPaths(localPortfolio2, "D:/Finanzmathematik/Masterthesis/Arbeit/Results/15yReceiverSwap/IM 15yReceiverSwap - Pairs Fixed Pathwise.xlsx", "Paths", InitialMargin.PAIRS, true, false);
		getInitialMarginPaths(localPortfolio2, "D:/Finanzmathematik/Masterthesis/Arbeit/Results/15yReceiverSwap/IM 15yReceiverSwap - Pairs Moving Pathwise.xlsx", "Paths", InitialMargin.PAIRS, true, true);

//		getInitialMarginPaths(localPortfolio, "D:/Finanzmathematik/Masterthesis/Arbeit/Results/5yPayer + 15yReceiver/IM 5yPayer + 15yReceiver - Base Fixed Average.xlsx", "Paths", InitialMargin.BASE, false, false);
//		getInitialMarginPaths(localPortfolio, "D:/Finanzmathematik/Masterthesis/Arbeit/Results/5yPayer + 15yReceiver/IM 5yPayer + 15yReceiver - Base Moving Average.xlsx", "Paths", InitialMargin.BASE, false, true);
//		getInitialMarginPaths(localPortfolio, "D:/Finanzmathematik/Masterthesis/Arbeit/Results/5yPayer + 15yReceiver/IM 5yPayer + 15yReceiver - Base Fixed Pathwise.xlsx", "Paths", InitialMargin.BASE, true, false);
//		getInitialMarginPaths(localPortfolio, "D:/Finanzmathematik/Masterthesis/Arbeit/Results/5yPayer + 15yReceiver/IM 5yPayer + 15yReceiver - Base Moving Pathwise.xlsx", "Paths", InitialMargin.BASE, true, true);
		
//		getSwapValuePaths("D:/Finanzmathematik/Masterthesis/Arbeit/Results/SwapTests/LegTest.xlsx", "Classic", false);
//		getSwapValuePaths("D:/Finanzmathematik/Masterthesis/Arbeit/Results/SwapTests/LegTest.xlsx", "Zero", true);

		
//		getPnLFromTaylor("D:/Finanzmathematik/Masterthesis/Arbeit/Results/SwapTests/10ySwap PnL Taylor vs Full future ES.xlsx", "Taylor");
//		getPnLFromFullRevaluation("D:/Finanzmathematik/Masterthesis/Arbeit/Results/SwapTests/10ySwap PnL Taylor vs Full future ES.xlsx", "Full");
	

		
	}
	
	
	
	
	public static void getInitialMarginPaths(LocalPortfolio localPortfolio, String filePath, String sheetName, InitialMargin type, boolean pathWiseEvaluation, boolean movingScenarioWindow) throws CalculationException {
		final LIBORModelMonteCarloSimulationModel liborModel = ModelFactory.getModel(CurveName.EUR_EURIBOR_3M);
		final ZeroRateModel zeroRateModel = new ZeroRateModel(Currency.EUR, CurveName.EUR_EURIBOR_3M, liborModel, "spreadsheets/EUR_EURIBOR_3M_Input_Data.xlsx");
		
	    ArrayList<LocalDateTime> dates = getDates(localPortfolio.getLastPaymentDate(), SchemeStorage.getLocalCalendar(Currency.EUR));
	    
		try {
            // Check if file exists
            if (!Files.exists(Paths.get(filePath))) {
                System.out.println("File does not exist.");
                return;
            }
            FileInputStream fileIn = new FileInputStream(filePath);
            Workbook workbook = WorkbookFactory.create(fileIn);
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
            	// Create a new sheet if it doesn't exist
                sheet = workbook.createSheet(sheetName); 
            }
            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd"));

            for (int i = 0; i < dates.size(); i++) {
                Row row = sheet.createRow(i);
                Cell dateCell = row.createCell(0);
                dateCell.setCellValue(dates.get(i));
                dateCell.setCellStyle(dateStyle);

                double[] paths = null;
        		switch (type) {
        		case FLOOR:
        			paths = localPortfolio.getFloorInitialMargin(dates.get(i), zeroRateModel, pathWiseEvaluation, movingScenarioWindow).getRealizations();
        			break;
        		case BASE:
        			paths = localPortfolio.getBaseInitialMargin(dates.get(i), zeroRateModel, pathWiseEvaluation, movingScenarioWindow).getRealizations();
        			break;
        		case PAIRS:
        			paths = localPortfolio.getPairsInitialMargin(dates.get(i), zeroRateModel, pathWiseEvaluation, movingScenarioWindow).getRealizations();
        			break;
        		default:
        			throw new IllegalArgumentException("Initial Margin type not defined.");
        		}
        		if (paths == null) {
        			row.createCell(1).setCellValue(0);
        		} else {
	                for (int j = 0; j < paths.length; j++) {
	                    row.createCell(j + 1).setCellValue(paths[j]);
	                }
                }	
            }
            // Close the input stream
            fileIn.close();

            // Write the changes back to the file
            FileOutputStream fileOut = new FileOutputStream(filePath);
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }		
	}
	
	
	public static void getSwapValuePaths(String filePath, String sheetName, boolean valueUnderZeroModel) throws CalculationException {
		final LIBORModelMonteCarloSimulationModel model = ModelFactory.getModel(CurveName.EUR_EURIBOR_3M);
		final ZeroRateModel zeroRateModel = new ZeroRateModel(Currency.EUR, CurveName.EUR_EURIBOR_3M, model, "spreadsheets/EUR_EURIBOR_3M_Input_Data.xlsx");

	    LchSwapLeg swap = PortfolioFactory.create10YReceiverSwap().getLegReceiver();
	    ArrayList<LocalDateTime> dates = getDates(swap.getLastPaymentDate(), SchemeStorage.getLocalCalendar(Currency.EUR));

		try {
            // Check if file exists
            if (!Files.exists(Paths.get(filePath))) {
                System.out.println("File does not exist.");
                return;
            }
            FileInputStream fileIn = new FileInputStream(filePath);
            Workbook workbook = WorkbookFactory.create(fileIn);
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
            	// Create a new sheet if it doesn't exist
                sheet = workbook.createSheet(sheetName); 
            }
            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd"));

            for (int i = 0; i < dates.size(); i++) {
                Row row = sheet.createRow(i);
                Cell dateCell = row.createCell(0);
                dateCell.setCellValue(dates.get(i));
                dateCell.setCellStyle(dateStyle);

                double[] paths;
                if (valueUnderZeroModel) {
                    paths = swap.getValue(dates.get(i), zeroRateModel, null, zeroRateModel, null, null).getRealizations();
                } else {
                	paths = swap.getValue(FloatingpointDate.getFloatingPointDateFromDate(REFERENCE_DATE, dates.get(i)), model).getRealizations();
                }
                for (int j = 0; j < paths.length; j++) {
                    row.createCell(j + 1).setCellValue(paths[j]);
                }
            }
            // Close the input stream
            fileIn.close();

            // Write the changes back to the file
            FileOutputStream fileOut = new FileOutputStream(filePath);
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }		
	}
	
	
	public static void getSensitivitiesAggregated(String filePath, String sheetName, SensitivityType type, SensitivityOrder order) throws CalculationException {
		final LIBORModelMonteCarloSimulationModel model = ModelFactory.getModel(CurveName.EUR_EURIBOR_3M);
		final ZeroRateModel zeroRateModel = new ZeroRateModel(Currency.EUR, CurveName.EUR_EURIBOR_3M, model, "spreadsheets/EUR_EURIBOR_3M_Input_Data.xlsx");

	    LchSwap swap = PortfolioFactory.create5YPayerSwap();
	    LocalPortfolio localPortfolio = PortfolioFactory.getSwapPortfolio(swap).getLocalPortfolio(Currency.EUR);
	    ArrayList<LocalDateTime> dates = getDates(swap.getLastPaymentDate(), SchemeStorage.getLocalCalendar(Currency.EUR));

		SensitivityComponentsForSwapLeg sensitivityComponents = new SensitivityComponentsForSwapLeg(localPortfolio.getSwaps(), zeroRateModel, zeroRateModel);
	    
		try {
            // Check if file exists
            if (!Files.exists(Paths.get(filePath))) {
                System.out.println("File does not exist.");
                return;
            }
            FileInputStream fileIn = new FileInputStream(filePath);
            Workbook workbook = WorkbookFactory.create(fileIn);
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
            	// Create a new sheet if it doesn't exist
                sheet = workbook.createSheet(sheetName); 
            }
            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd"));

            for (int i = 0; i < dates.size(); i++) {
                Row row = sheet.createRow(i);
                Cell dateCell = row.createCell(0);
                dateCell.setCellValue(dates.get(i));
                dateCell.setCellStyle(dateStyle);

                double bpFactor = 10000;
                if (order.equals(SensitivityOrder.GAMMA)) {
                	bpFactor *= 10000;
                }
                
                SensitivityMatrix sensitivityMatrix = getSensitivityCurve(dates.get(i), zeroRateModel, sensitivityComponents, type, order);
                
				RandomVariable sensitivity = new RandomVariableFromDoubleArray(0.0);
				for (Map.Entry<Double, TreeMap<Double, RandomVariable>> sensitivityRow : sensitivityMatrix.getSensitivityMatrix().entrySet()) {
					for (Map.Entry<Double, RandomVariable> sensitivityColumn : sensitivityRow.getValue().entrySet()) {
						if (sensitivityColumn.getValue() != null) {
							sensitivity = sensitivity.add(sensitivityColumn.getValue());
						}
					}
				}
				double[] paths = sensitivity.mult(bpFactor).getRealizations(); 
				for (int j = 0; j < paths.length; j++) { // 100 paths
					row.createCell(j + 1).setCellValue(paths[j]);
				}
            }
            // Close the input stream
            fileIn.close();

            // Write the changes back to the file
            FileOutputStream fileOut = new FileOutputStream(filePath);
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	
	public static void getSensitivitiesBucketWise(String filePath, String sheetName, SensitivityType type, SensitivityOrder order) throws CalculationException {
		final LIBORModelMonteCarloSimulationModel model = ModelFactory.getModel(CurveName.EUR_EURIBOR_3M);
		final ZeroRateModel zeroRateModel = new ZeroRateModel(Currency.EUR, CurveName.EUR_EURIBOR_3M, model, "spreadsheets/EUR_EURIBOR_3M_Input_Data.xlsx");

	    LchSwap swap = PortfolioFactory.create5YPayerSwap();
	    LocalPortfolio localPortfolio = PortfolioFactory.getSwapPortfolio(swap).getLocalPortfolio(Currency.EUR);
	    ArrayList<LocalDateTime> dates = getDates(swap.getLastPaymentDate(), SchemeStorage.getLocalCalendar(Currency.EUR));

		SensitivityComponentsForSwapLeg sensitivityComponents = new SensitivityComponentsForSwapLeg(localPortfolio.getSwaps(), zeroRateModel, zeroRateModel);	    
		
		try {
            // Check if file exists
            if (!Files.exists(Paths.get(filePath))) {
                System.out.println("File does not exist.");
                return;
            }
            FileInputStream fileIn = new FileInputStream(filePath);
            Workbook workbook = WorkbookFactory.create(fileIn);
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
            	// Create a new sheet if it doesn't exist
                sheet = workbook.createSheet(sheetName); 
            }
            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd"));

            for (int i = 0; i < dates.size(); i++) {
                Row row = sheet.createRow(i+1);
                Cell dateCell = row.createCell(0);
                dateCell.setCellValue(dates.get(i));
                dateCell.setCellStyle(dateStyle);

                int index = 0;
                double bpFactor = 10000;
                if (order.equals(SensitivityOrder.GAMMA)) {
                	bpFactor *= 10000;
                }
                
                SensitivityMatrix sensitivityMatrix = getSensitivityCurve(dates.get(i), zeroRateModel, sensitivityComponents, type, order);
				for (Map.Entry<Double, TreeMap<Double, RandomVariable>> sensitivityRow : sensitivityMatrix.getSensitivityMatrix().entrySet()) {
					for (double bucket : BUCKETS) {
						if (sensitivityRow.getValue().get(bucket) == null) {
							for (int j = 0; j < 100; j++) { // 100 paths
			                    row.createCell(index + j + 1).setCellValue(0);
							}
						} else {
							double[] pathsPerBucket = sensitivityRow.getValue().get(bucket).mult(bpFactor).getRealizations(); 
							for (int j = 0; j < pathsPerBucket.length; j++) {
								row.createCell(index+ j + 1).setCellValue(pathsPerBucket[j]);
							}
						}
						index += 101;
					}				
				}              
            }
            // Close the input stream
            fileIn.close();

            // Write the changes back to the file
            FileOutputStream fileOut = new FileOutputStream(filePath);
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }	
	}
	
	
	public static void getPnLFromTaylor(String filePath, String sheetName) throws CalculationException {
		final LIBORModelMonteCarloSimulationModel liborModel = ModelFactory.getModel(CurveName.EUR_EURIBOR_3M);
		final ZeroRateModel zeroRateModel = new ZeroRateModel(Currency.EUR, CurveName.EUR_EURIBOR_3M, liborModel, "spreadsheets/EUR_EURIBOR_3M_Input_Data.xlsx");
		
	    LchSwap swap = PortfolioFactory.create10YPayerSwap();
	    LocalPortfolio localPortfolio = PortfolioFactory.getSwapPortfolio(swap).getLocalPortfolio(Currency.EUR); //, swap10Y, swap10YPlusSpread);		
		SensitivityComponentsForSwapLeg sensitivityComponents = new SensitivityComponentsForSwapLeg(localPortfolio.getSwaps(), zeroRateModel, zeroRateModel);	    
		ForwardAndDiscountSensitivities sensitivities = new ForwardAndDiscountSensitivities(sensitivityComponents, TenorGridFactory.getTenorGrid(GridType.INITIAL_MARGIN_RISK_GRID));
		HashMap<LocalDateTime, RandomVariable> pnL = getPnL(REFERENCE_DATE.plusDays(1250), zeroRateModel, sensitivities, Simulation.EXPECTED_SHORTFALL_6, false).getSeries();
	    
		try {
            // Check if file exists
            if (!Files.exists(Paths.get(filePath))) {
                System.out.println("File does not exist.");
                return;
            }
            FileInputStream fileIn = new FileInputStream(filePath);
            Workbook workbook = WorkbookFactory.create(fileIn);
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
            	// Create a new sheet if it doesn't exist
                sheet = workbook.createSheet(sheetName); 
            }
            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd"));

            int i = 0;
            for (Map.Entry<LocalDateTime, RandomVariable> entry : pnL.entrySet()) {
                Row row = sheet.createRow(i);
                Cell dateCell = row.createCell(0);
                dateCell.setCellValue(entry.getKey());
                dateCell.setCellStyle(dateStyle);
                double value = entry.getValue().getAverage();
                row.createCell(1).setCellValue(value);
                i += 1;
            }
            // Close the input stream
            fileIn.close();

            // Write the changes back to the file
            FileOutputStream fileOut = new FileOutputStream(filePath);
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }		
	}
	
	private static void getPnLFromFullRevaluation(String filePath, String sheetName) throws CalculationException {
		final LIBORModelMonteCarloSimulationModel liborModel = ModelFactory.getModel(CurveName.EUR_EURIBOR_3M);
		final ZeroRateModel zeroRateModel = new ZeroRateModel(Currency.EUR, CurveName.EUR_EURIBOR_3M, liborModel, "spreadsheets/EUR_EURIBOR_3M_Input_Data.xlsx");
		
	    LchSwap swap = PortfolioFactory.create10YPayerSwap();
	    LocalPortfolio localPortfolio = PortfolioFactory.getSwapPortfolio(swap).getLocalPortfolio(Currency.EUR); //, swap10Y, swap10YPlusSpread);		
        CurveScenarios curveScenarios = PairsInitialMargin.getCurveScenarios(
                new ScenarioFactory(zeroRateModel, Simulation.EXPECTED_SHORTFALL_6), REFERENCE_DATE,
                TenorGridFactory.getTenorGrid(GridType.INITIAL_MARGIN_RISK_GRID));
        List<LocalDateTime> dates = new ArrayList<>(curveScenarios.getCurveScenarios().values()
                .iterator().next().getSeries().keySet());
        dates.sort(LocalDateTime::compareTo);
        List<RandomVariable> pnL = PairsInitialMargin.getRevaluationLosses(REFERENCE_DATE.plusDays(1250),
                localPortfolio, zeroRateModel, curveScenarios, dates, null);

		try {
            // Check if file exists
            if (!Files.exists(Paths.get(filePath))) {
                System.out.println("File does not exist.");
                return;
            }
            FileInputStream fileIn = new FileInputStream(filePath);
            Workbook workbook = WorkbookFactory.create(fileIn);
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
            	// Create a new sheet if it doesn't exist
                sheet = workbook.createSheet(sheetName); 
            }
            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd"));

            int i = 0;
            for (int j = 0; j < dates.size(); j++) {
                Row row = sheet.createRow(i);
                Cell dateCell = row.createCell(0);
                dateCell.setCellValue(dates.get(j));
                dateCell.setCellStyle(dateStyle);
                double value = pnL.get(j).getAverage();
                row.createCell(1).setCellValue(value);
                i += 1;
            }
            // Close the input stream
            fileIn.close();

            // Write the changes back to the file
            FileOutputStream fileOut = new FileOutputStream(filePath);
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }	
		
	}
	
	
	public static void getWorstScenarioDates(String filePath, String sheetName, Simulation simulation, boolean pathWiseEvaluation, boolean movingScenarioWindow) throws CalculationException {
		final LIBORModelMonteCarloSimulationModel model = ModelFactory.getModel(CurveName.EUR_EURIBOR_3M);
		final ZeroRateModel zeroRateModel = new ZeroRateModel(Currency.EUR, CurveName.EUR_EURIBOR_3M, model, "spreadsheets/EUR_EURIBOR_3M_Input_Data.xlsx");

	    LchSwap swap = PortfolioFactory.create5YPayerSwap();
	    LocalPortfolio localPortfolio = PortfolioFactory.getSwapPortfolio(swap).getLocalPortfolio(Currency.EUR);
	    ArrayList<LocalDateTime> dates = getDates(swap.getLastPaymentDate(), SchemeStorage.getLocalCalendar(Currency.EUR));

		SensitivityComponentsForSwapLeg sensitivityComponents = new SensitivityComponentsForSwapLeg(localPortfolio.getSwaps(), zeroRateModel, zeroRateModel);	    
		ForwardAndDiscountSensitivities sensitivities = new ForwardAndDiscountSensitivities(sensitivityComponents, TenorGridFactory.getTenorGrid(GridType.INITIAL_MARGIN_RISK_GRID));
		
		try {
            // Check if file exists
            if (!Files.exists(Paths.get(filePath))) {
                System.out.println("File does not exist.");
                return;
            }
            FileInputStream fileIn = new FileInputStream(filePath);
            Workbook workbook = WorkbookFactory.create(fileIn);
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
            	// Create a new sheet if it doesn't exist
                sheet = workbook.createSheet(sheetName); 
            }
            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd"));

            if (pathWiseEvaluation) {
                Row row = sheet.createRow(0);
                int pathIndex = 0;
				for(int paths = 0; paths < 100; paths++) {
					row.createCell(pathIndex+1).setCellValue(paths+1);
					pathIndex += 21;
				}
            }
            for (int i = 0; i < dates.size(); i++) {
                Row row = sheet.createRow(i+1);
                Cell dateCell = row.createCell(0);
                dateCell.setCellValue(dates.get(i));
                dateCell.setCellStyle(dateStyle);
				
        		RandomVariableSeries portfolioPnLs = getPnL(dates.get(i), zeroRateModel, sensitivities, simulation, movingScenarioWindow);
				if (pathWiseEvaluation) {
					int index = 0;
					for (int pathNumber = 0; pathNumber < zeroRateModel.getNumberOfPaths(); pathNumber++) {
						List<LocalDateTime> worstScenarioDates = portfolioPnLs.getNLowestValues(20, pathNumber);
						for (int j = 0; j < worstScenarioDates.size(); j++) {
							row.createCell(index + j + 1).setCellValue(worstScenarioDates.get(j));
						}
						index += 21;
					}
				} else {
	        		List<LocalDateTime> worstScenarioDates = portfolioPnLs.getNLowestValues(20, null);
					for (int j = 0; j < worstScenarioDates.size(); j++) {
						row.createCell(j + 1).setCellValue(worstScenarioDates.get(j));
					}
				}
            }
            // Close the input stream
            fileIn.close();

            // Write the changes back to the file
            FileOutputStream fileOut = new FileOutputStream(filePath);
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }		
	}
	
	
	// Revalues portfolio under all scenarios to check if PnL actually filters for the 20 worst scenarios
	public static void getIMWithFullRevaluation(String filePath, String sheetName, Simulation simulation, boolean pathWiseEvaluation) throws CalculationException {
		final LIBORModelMonteCarloSimulationModel model = ModelFactory.getModel(CurveName.EUR_EURIBOR_3M);
		final ZeroRateModel zeroRateModel = new ZeroRateModel(Currency.EUR, CurveName.EUR_EURIBOR_3M, model, "spreadsheets/EUR_EURIBOR_3M_Input_Data.xlsx");

	    LchSwap swap = PortfolioFactory.create5YPayerSwap();
	    LocalPortfolio localPortfolio = PortfolioFactory.getSwapPortfolio(swap).getLocalPortfolio(Currency.EUR);
	    ArrayList<LocalDateTime> dates = getDates(swap.getLastPaymentDate(), SchemeStorage.getLocalCalendar(Currency.EUR));

		try {
            // Check if file exists
            if (!Files.exists(Paths.get(filePath))) {
                System.out.println("File does not exist.");
                return;
            }
            FileInputStream fileIn = new FileInputStream(filePath);
            Workbook workbook = WorkbookFactory.create(fileIn);
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
            	// Create a new sheet if it doesn't exist
                sheet = workbook.createSheet(sheetName); 
            }
            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd"));

            if (pathWiseEvaluation) {
                Row row = sheet.createRow(0);
                int pathIndex = 0;
				for(int paths = 0; paths < 100; paths++) {
					row.createCell(pathIndex+1).setCellValue(paths+1);
					pathIndex += 21;
				}
            }
            for (int i = 0; i < dates.size(); i++) {
                Row row = sheet.createRow(i+1);
                Cell dateCell = row.createCell(0);
                dateCell.setCellValue(dates.get(i));
                dateCell.setCellStyle(dateStyle);
				
                CurveScenarios scenarios = PairsInitialMargin.getCurveScenarios(
                        new ScenarioFactory(zeroRateModel, simulation), dates.get(i),
                        TenorGridFactory.getTenorGrid(GridType.INITIAL_MARGIN_RISK_GRID));
                RandomVariable initialMargin = PairsInitialMargin.getInitialMarginWithFullRevaluation(
                        dates.get(i), zeroRateModel, localPortfolio, scenarios, simulation, pathWiseEvaluation);
                double[] initialMarginRealizations = new double[zeroRateModel.getNumberOfPaths()];
                for (int path = 0; path < initialMarginRealizations.length; path++) {
                    initialMarginRealizations[path] = initialMargin.get(path);
                }
				for (int j = 0; j < initialMarginRealizations.length; j++) {
					row.createCell(j + 1).setCellValue(initialMarginRealizations[j]);
				}
            }
            // Close the input stream
            fileIn.close();

            // Write the changes back to the file
            FileOutputStream fileOut = new FileOutputStream(filePath);
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }		
	}
		
	
	private static SensitivityMatrix getSensitivityCurve(LocalDateTime evaluationDate, ZeroRateModel zeroRateModel, SensitivityComponentsForSwapLeg sensitivityComponents, SensitivityType type, SensitivityOrder order) throws CalculationException {
		Sensitivities sensitivities;
		switch (type) {
		case FORWARD:
			sensitivities = new ForwardSensitivities(sensitivityComponents, TenorGridFactory.getTenorGrid(GridType.INITIAL_MARGIN_RISK_GRID)); // Risk is sampled on the "fixed grid in zero space"
			break;
		case DISCOUNT:
			sensitivities = new DiscountSensitivities(sensitivityComponents, TenorGridFactory.getTenorGrid(GridType.INITIAL_MARGIN_RISK_GRID)); // Risk is sampled on the "fixed grid in zero space"
			break;
		case FORWARD_AND_DISCOUNT:
			sensitivities = new ForwardAndDiscountSensitivities(sensitivityComponents, TenorGridFactory.getTenorGrid(GridType.INITIAL_MARGIN_RISK_GRID)); // Risk is sampled on the "fixed grid in zero space"
			break;
		default:
			throw new IllegalArgumentException("Invalid sensitivity type");
		}

		switch (order) {
		case DELTA:
			return sensitivities.getDeltaSensitivities(evaluationDate);
		case GAMMA:
			return sensitivities.getGammaSensitivities(evaluationDate);
		default:
			throw new IllegalArgumentException("Invalid sensitivity order");
		}
	}
	
	
	private static RandomVariableSeries getPnL(LocalDateTime evaluationDate, ZeroRateModel zeroRateModel,
            ForwardAndDiscountSensitivities sensitivities, Simulation simulation, boolean movingScenarioWindow)
            throws CalculationException {
        LocalDateTime scenarioDate = movingScenarioWindow ? evaluationDate : zeroRateModel.getReferenceDate();
        return PairsInitialMargin.getScenarioData(sensitivities.getDeltaSensitivities(evaluationDate),
                sensitivities.getGammaSensitivities(evaluationDate), new ScenarioFactory(zeroRateModel, simulation),
                scenarioDate, simulation, TenorGridFactory.getTenorGrid(GridType.INITIAL_MARGIN_RISK_GRID))
                .getPnlSeries(simulation);
    }

	public static ArrayList<LocalDateTime> getDates(double years, BusinessdayCalendar calendar) throws CalculationException {
		LocalDateTime lastDate = FloatingpointDate.getDateFromFloatingPointDate(REFERENCE_DATE, years + 5/365.0); // Additional 5 days
		ArrayList<LocalDateTime> dates = new ArrayList<>();
		for (LocalDateTime date = REFERENCE_DATE; date.isBefore(lastDate) ; date = date.plusDays(1)) {
			if (calendar.isBusinessday(date.toLocalDate())) {
				dates.add(date);
			}
		}
		return dates;
	}
	
	
}
