package net.sourceforge.cruisecontrol.chart;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.laures.cewolf.DatasetProduceException;

import net.sourceforge.cruisecontrol.BuildInfo;
import net.sourceforge.cruisecontrol.BuildInfoSummary;
import net.sourceforge.cruisecontrol.taglib.BuildInfoTag;
import junit.framework.TestCase;

public class AbstractCruiseControlChartDataTest extends TestCase {

    private Map cewolfChartParameters;
    private AbstractCruiseControlChartData chartData;

    protected void setUp() throws Exception {
        chartData = new AbstractCruiseControlChartData() {
            public String getProducerId() {
                throw new UnsupportedOperationException();
            }
            public Object produceDataset(Map arg0) throws DatasetProduceException {
                throw new UnsupportedOperationException();
            }            
        };
        
        cewolfChartParameters = new HashMap();
        List buildInfos = new ArrayList();
        BuildInfo info = new BuildInfo(new File("log20050708100401.xml"));
        buildInfos.add(info);
        BuildInfoSummary buildInfoSummary = new BuildInfoSummary(buildInfos);
        cewolfChartParameters.put(BuildInfoTag.INFO_ATTRIBUTE, buildInfoSummary);
    }

    protected void tearDown() throws Exception {
        cewolfChartParameters = null;
    }

    public void testHasExpiredShouldBeFalseWhenNoNewBuild() throws ParseException {
        Date dateOfCachedData = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US).parse("12/12/2006");
        assertFalse(chartData.hasExpired(cewolfChartParameters, dateOfCachedData));
    }

    public void testHasExpiredShouldBeTrueWhenNewBuildExists() throws ParseException {
        Date dateOfCachedData = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US).parse("01/01/2004");
        assertTrue(chartData.hasExpired(cewolfChartParameters, dateOfCachedData));
    }

    public void testHasExpiredShouldReturnFalseWhenNoBuildsExist() {
        List buildInfos = new ArrayList();
        BuildInfoSummary buildInfoSummary = new BuildInfoSummary(buildInfos);
        cewolfChartParameters.put(BuildInfoTag.INFO_ATTRIBUTE, buildInfoSummary);
        
        assertFalse(chartData.hasExpired(cewolfChartParameters, new Date()));
    }
    
}
