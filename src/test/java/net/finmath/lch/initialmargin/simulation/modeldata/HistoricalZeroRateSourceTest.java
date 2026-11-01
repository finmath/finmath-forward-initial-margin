package net.finmath.lch.initialmargin.simulation.modeldata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.Scalar;

public class HistoricalZeroRateSourceTest {

    private static final LocalDate DATE = LocalDate.of(2024, 1, 2);

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void snapshotsOriginalPointsAndInterpolatesWithoutChangingTenorSupport() throws CalculationException {
        StochasticCurve curve = curve(0.25, -0.01, 1.0, 0.02, 2.0, 0.04);
        NavigableMap<LocalDateTime, StochasticCurve> history = history(curve);
        HistoricalZeroRateSource source = new HistoricalZeroRateSource(history);

        assertEquals(-0.01, source.getZeroRate(DATE, 0.1).get(0), 0.0);
        assertEquals(0.005, source.getZeroRate(DATE, 0.625).get(0), 1E-15);
        assertEquals(0.04, source.getZeroRate(DATE, 10.0).get(0), 0.0);
        assertEquals(Arrays.asList(0.25, 1.0, 2.0), Arrays.asList(source.getTenors(DATE).toArray()));

        curve.addRate(0.625, new Scalar(0.99));
        curve.addRate(1.0, new Scalar(0.5));
        history.clear();
        assertEquals(0.005, source.getZeroRate(DATE, 0.625).get(0), 1E-15);
        assertEquals(0.02, source.getZeroRate(DATE, 1.0).get(0), 0.0);
        assertThrows(UnsupportedOperationException.class, () -> source.getTenors(DATE).add(3.0));
        assertThrows(UnsupportedOperationException.class, () -> source.getObservationDates().clear());
    }

    @Test
    public void normalizesMapOrderingAndDoesNotFillMissingDates() throws CalculationException {
        NavigableMap<LocalDateTime, StochasticCurve> history = new TreeMap<>();
        history.put(DATE.atStartOfDay(), curve(1.0, 0.01));
        history.put(DATE.plusDays(2).atStartOfDay(), curve(1.0, 0.03));
        HistoricalZeroRateSource source = new HistoricalZeroRateSource(history.descendingMap());

        assertEquals(DATE, source.getFirstDate());
        assertEquals(DATE.plusDays(2), source.getLastDate());
        assertEquals(Arrays.asList(DATE, DATE.plusDays(2)), Arrays.asList(source.getObservationDates().toArray()));
        assertThrows(CalculationException.class, () -> source.getZeroRate(DATE.plusDays(1), 1.0));
        assertThrows(IllegalArgumentException.class, () -> source.getTenors(DATE.plusDays(1)));
    }

    @Test
    public void allowsRawZeroTenorButRejectsInvalidRequestsAndHistory() throws CalculationException {
        HistoricalZeroRateSource source = new HistoricalZeroRateSource(history(curve(0.0, 0.01, 1.0, 0.03)));
        assertEquals(0.02, source.getZeroRate(DATE, 0.5).get(0), 1E-15);
        for (double tenor : new double[] {0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> source.getZeroRate(DATE, tenor));
        }
        assertThrows(IllegalArgumentException.class, () -> new HistoricalZeroRateSource(new TreeMap<>()));
        assertThrows(IllegalArgumentException.class, () -> new HistoricalZeroRateSource(history(new StochasticCurve())));
        assertThrows(IllegalArgumentException.class,
                () -> new HistoricalZeroRateSource(history(curve(-1.0, 0.01))));
        assertThrows(IllegalArgumentException.class,
                () -> new HistoricalZeroRateSource(history(curve(1.0, Double.NaN))));

        StochasticCurve randomCurve = new StochasticCurve();
        randomCurve.addRate(1.0, new RandomVariableFromDoubleArray(0.0, new double[] {0.01, 0.02}));
        assertThrows(IllegalArgumentException.class, () -> new HistoricalZeroRateSource(history(randomCurve)));
        NavigableMap<LocalDateTime, StochasticCurve> intraday = new TreeMap<>();
        intraday.put(DATE.atTime(16, 0), curve(1.0, 0.01));
        assertThrows(IllegalArgumentException.class, () -> new HistoricalZeroRateSource(intraday));
    }

    @Test
    public void excelImportUsesCalendarDatesAndRejectsDuplicateDays() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            Row dates = sheet.createRow(0);
            dates.createCell(0).setCellValue("Tenor");
            dates.createCell(1).setCellValue(DATE.atTime(17, 30));
            Row rates = sheet.createRow(1);
            rates.createCell(0).setCellValue(0.25);
            rates.createCell(1).setCellValue(-0.01);

            ZeroCurveHistory reader = new ZeroCurveHistory(writeWorkbook(workbook).getPath());
            NavigableMap<LocalDateTime, StochasticCurve> imported = reader.buildZeroCurveHistory();
            assertEquals(DATE.atStartOfDay(), imported.firstKey());
            assertEquals(-0.01, imported.firstEntry().getValue().getRate(0.25, null).get(0), 0.0);

            dates.createCell(2).setCellValue(DATE.atTime(9, 0));
            rates.createCell(2).setCellValue(0.02);
            ZeroCurveHistory duplicateReader = new ZeroCurveHistory(writeWorkbook(workbook).getPath());
            assertThrows(CalculationException.class, duplicateReader::buildZeroCurveHistory);
        }
    }

    @Test
    public void excelImportRejectsDuplicateTenorsAndBlankRates() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            Row dates = sheet.createRow(0);
            dates.createCell(0).setCellValue("Tenor");
            dates.createCell(1).setCellValue(DATE.atStartOfDay());
            Row rates = sheet.createRow(1);
            rates.createCell(0).setCellValue(1.0);
            rates.createCell(1).setCellValue(0.01);
            Row duplicate = sheet.createRow(2);
            duplicate.createCell(0).setCellValue(1.0);
            duplicate.createCell(1).setCellValue(0.02);
            ZeroCurveHistory duplicateReader = new ZeroCurveHistory(writeWorkbook(workbook).getPath());
            assertThrows(CalculationException.class, duplicateReader::buildZeroCurveHistory);

            sheet.removeRow(duplicate);
            rates.getCell(1).setBlank();
            ZeroCurveHistory blankReader = new ZeroCurveHistory(writeWorkbook(workbook).getPath());
            assertThrows(CalculationException.class, blankReader::buildZeroCurveHistory);
        }
    }

    @Test
    public void bundledHistoryPreservesEveryObservationAndOriginalPoint() throws Exception {
        File file = new File("spreadsheets/EUR_EURIBOR_3M_Input_Data.xlsx");
        byte[] originalBytes = Files.readAllBytes(file.toPath());
        NavigableMap<LocalDateTime, StochasticCurve> imported = new ZeroCurveHistory(file.getPath()).buildZeroCurveHistory();
        assertEquals(4200, imported.size());
        assertEquals(LocalDateTime.of(2008, 1, 1, 0, 0), imported.firstKey());
        assertEquals(LocalDateTime.of(2024, 2, 5, 0, 0), imported.lastKey());
        try (Workbook workbook = WorkbookFactory.create(file, null, true)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row dates = sheet.getRow(0);
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row rates = sheet.getRow(rowIndex);
                double tenor = rates.getCell(0).getNumericCellValue();
                for (int column = 1; column < dates.getLastCellNum(); column++) {
                    LocalDateTime date = dates.getCell(column).getLocalDateTimeCellValue().toLocalDate().atStartOfDay();
                    assertEquals(rates.getCell(column).getNumericCellValue(), imported.get(date).getRate(tenor, null).get(0), 0.0);
                }
            }
        }
        assertArrayEquals(originalBytes, Files.readAllBytes(file.toPath()));
    }

    private File writeWorkbook(Workbook workbook) throws Exception {
        File file = temporaryFolder.newFile();
        try (FileOutputStream output = new FileOutputStream(file)) {
            workbook.write(output);
        }
        return file;
    }

    private static NavigableMap<LocalDateTime, StochasticCurve> history(StochasticCurve curve) {
        NavigableMap<LocalDateTime, StochasticCurve> history = new TreeMap<>();
        history.put(DATE.atStartOfDay(), curve);
        return history;
    }

    private static StochasticCurve curve(double... tenorRatePairs) {
        StochasticCurve curve = new StochasticCurve();
        for (int index = 0; index < tenorRatePairs.length; index += 2) {
            curve.addRate(tenorRatePairs[index], new Scalar(tenorRatePairs[index + 1]));
        }
        return curve;
    }
}
