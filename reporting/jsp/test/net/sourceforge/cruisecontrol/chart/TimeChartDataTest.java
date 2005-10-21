package net.sourceforge.cruisecontrol.chart;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.BuildInfo;
import net.sourceforge.cruisecontrol.BuildInfoSummary;

/**
 * @author Jeffrey Fredrick
 */

public class TimeChartDataTest extends TestCase {

    public void testProduceDatasetNoData() throws Exception {
        TimeChartData data = new TimeChartData();
        Map map = new HashMap();

        List buildInfos = new ArrayList();
        BuildInfoSummary buildInfoSummary = new BuildInfoSummary(buildInfos);
        map.put("buildInfo", buildInfoSummary);
        data.produceDataset(map);
    }

    public void testProduceDatasetBuildsInSameMinute() throws Exception {
        TimeChartData data = new TimeChartData();
        Map map = new HashMap();

        List buildInfos = new ArrayList();
        BuildInfo info = new BuildInfo(new File("log20050708100401.log"));
        buildInfos.add(info);
        info = new BuildInfo(new File("log20050708100431.log"));
        buildInfos.add(info);
        BuildInfoSummary buildInfoSummary = new BuildInfoSummary(buildInfos);
        map.put("buildInfo", buildInfoSummary);
        data.produceDataset(map);
    }
}
