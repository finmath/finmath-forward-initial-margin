package net.finmath.lch.initialmargin.simulation.modeldata;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import net.finmath.exception.CalculationException;
import net.finmath.stochastic.Scalar;


/**
 * Used for transforming Excel-based input data (double) to stochastic curves (Scalar).
 * The input file consists of curve fixing points in the first column, dates in the first row in descending order, and for each date and fixing point the zero rate value of that date.
 * Excel dates are interpreted as local calendar dates and normalized to midnight,
 * independently of the machine's time zone. Duplicate days or tenors are rejected.
 * 
 * @author Raphael Prandtl
 *
 */
public class ZeroCurveHistory {

	private final String 					inputDataFilePath;
	
	
	public ZeroCurveHistory(final String inputDataFilePath) throws CalculationException {
		this.inputDataFilePath = Objects.requireNonNull(inputDataFilePath, "inputDataFilePath");
	}
	
	
	/**
	 * Transforms the input data to a time series of StochasticCurve.
	 * The historical data is of course not stochastic, the RandomVariable type is chosen to create a seamless transition of historical rates and model generated rates.
	 * @return The historical curve time series
	 * @throws CalculationException
	 */
	public NavigableMap<LocalDateTime, StochasticCurve> buildZeroCurveHistory() throws CalculationException { 
		NavigableMap<LocalDateTime, StochasticCurve> zeroCurves = new TreeMap<>();
		try (FileInputStream fileInput = new FileInputStream(new File(inputDataFilePath));
				Workbook workbook = WorkbookFactory.create(fileInput)) {
			
			Sheet sheet = workbook.getSheetAt(0);
	        ArrayList<LocalDateTime> dates = getDays(sheet);
	        Set<Double> maturities = new HashSet<>();
	        if (sheet.getLastRowNum() < 1) {
                throw new IllegalArgumentException("The history contains no zero-rate rows.");
	        }
	        // first row contains the date, following rows contain the rate series
	        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
	            Row rateSeries = sheet.getRow(rowIndex);
	            double maturity = getNumericValue(rateSeries, 0);
	            if (maturity < 0.0) {
                    throw new IllegalArgumentException("Historical tenor must be nonnegative in row " + (rowIndex + 1));
	            }
	            maturity = maturity == 0.0 ? 0.0 : maturity;
	            if (!maturities.add(maturity)) {
                    throw new IllegalArgumentException("Duplicate historical tenor: " + maturity);
	            }
		        addRatesToCurves(zeroCurves, maturity, rateSeries, dates);		      
			}	

		} catch (IOException | RuntimeException e) {
	        throw new CalculationException("Failed to build rate data from input file.", e);
		} 
		return zeroCurves;
	}


	private void addRatesToCurves(NavigableMap<LocalDateTime, StochasticCurve> zeroCurves, double maturity, Row rateSeries, ArrayList<LocalDateTime> dates) {
		for (int dayIndex = 0; dayIndex < dates.size(); dayIndex++) {
            double rate = getNumericValue(rateSeries, dayIndex + 1);
            zeroCurves.computeIfAbsent(dates.get(dayIndex), k -> new StochasticCurve()).addRate(maturity, new Scalar(rate));
        }
	}
	
	
	public ArrayList<LocalDateTime> getDays(Sheet sheet) {
		ArrayList<LocalDateTime> dates = new ArrayList<>();
		Set<LocalDateTime> uniqueDates = new HashSet<>();
		Row dateRow = sheet.getRow(0);
		if (dateRow == null || dateRow.getLastCellNum() < 2) {
			throw new IllegalArgumentException("The history contains no observation dates.");
		}
		// first column contains header
        for (int colIndex = 1; colIndex < dateRow.getLastCellNum(); colIndex++) {
            Cell dateCell = dateRow.getCell(colIndex);
            LocalDateTime cellDate = dateCell == null ? null : dateCell.getLocalDateTimeCellValue();
            if (cellDate == null) {
                throw new IllegalArgumentException("Missing historical date in column " + (colIndex + 1));
            }
            LocalDateTime localDate = cellDate.toLocalDate().atStartOfDay();
            if (!uniqueDates.add(localDate)) {
                throw new IllegalArgumentException("Duplicate historical observation day: " + localDate.toLocalDate());
            }
            dates.add(localDate); 
        }
        return dates;
	}

	private double getNumericValue(Row row, int columnIndex) {
		if (row == null || row.getCell(columnIndex) == null) {
			throw new IllegalArgumentException("Missing historical numeric value in column " + (columnIndex + 1));
		}
		Cell cell = row.getCell(columnIndex);
		if (cell.getCellType() != CellType.NUMERIC && (cell.getCellType() != CellType.FORMULA || cell.getCachedFormulaResultType() != CellType.NUMERIC)) {
			throw new IllegalArgumentException("Historical value must be numeric in row " + (row.getRowNum() + 1) + ", column " + (columnIndex + 1));
		}
		double value = cell.getNumericCellValue();
		if (!Double.isFinite(value)) {
			throw new IllegalArgumentException("Historical numeric value must be finite in row " + (row.getRowNum() + 1) + ", column " + (columnIndex + 1));
		}
		return value;
	}
	
}