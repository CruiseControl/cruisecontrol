/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 200 E. Randolph, 25th Floor
 * Chicago, IL 60601 USA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     + Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     + Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
 *       names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/
package net.sourceforge.cruisecontrol.chart;

import de.laures.cewolf.ChartPostProcessor;
import de.laures.cewolf.DatasetProduceException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.sourceforge.cruisecontrol.BuildInfo;
import net.sourceforge.cruisecontrol.BuildInfoSummary;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.StandardXYItemRenderer;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

/**
 *
 * @author hack
 */
public class XPathChartData extends AbstractCruiseControlChartData implements ChartPostProcessor {

    private List xpaths = new LinkedList();

    /**
     * Creates a new instance of XPathChartData
     */
    public XPathChartData() {
    }

    public void add(String name, String expression) throws JDOMException {
        xpaths.add(new XPathData(name, expression));
    }

    public Object produceDataset(Map params) throws DatasetProduceException {
        BuildInfoSummary summary = getBuildInfoSummary(params);
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        Map timeSeries = new LinkedHashMap(xpaths.size());
        for (Iterator i = xpaths.iterator(); i.hasNext(); ) {
            XPathData data = (XPathData) i.next();
            TimeSeries serie = new TimeSeries(data.getName(), Minute.class);
            timeSeries.put(data, serie);
            dataset.addSeries(serie);
        }
        for (Iterator iter = summary.iterator(); iter.hasNext();) {
            BuildInfo buildInfo = (BuildInfo) iter.next();
            Date buildTime = buildInfo.getBuildDate();
            Minute timePeriod = new Minute(buildTime);
            try {
                Document log = buildInfo.getLogFile().asDocument();
                for (Iterator i = timeSeries.entrySet().iterator(); i.hasNext(); ) {
                    Map.Entry pair = (Map.Entry) i.next();
                    TimeSeries serie = (TimeSeries) pair.getValue();
                    Number result = ((XPathData) pair.getKey()).evaluate(log);
                    serie.addOrUpdate(timePeriod, result);
                }
            } catch (JDOMException jex) {
                throw new DatasetProduceException(jex.getMessage());
            } catch (IOException ioex) {
                throw new DatasetProduceException(ioex.getMessage());
            }
        }
        return dataset;
    }

    public String getProducerId() {
        return "XPathChartData DatasetProducer";
    }

    /**
     * @see ChartPostProcessor#processChart(Object, Map)
     */
    public void processChart(Object chartObject, Map params) {
        JFreeChart chart = (JFreeChart) chartObject;
        XYPlot plot = chart.getXYPlot();
        configurePlotRendererForShapesOnly(plot);
        setXAxisFormat(plot);
        //setYAxisFormat(plot);
    }

    /**
     * @param plot
     */
    private void setXAxisFormat(XYPlot plot) {
        DateAxis xAxis = (DateAxis) plot.getHorizontalValueAxis();
        xAxis.setDateFormatOverride(new SimpleDateFormat("dd/MM"));
    }

    /**
     * @param plot
     */
    private void configurePlotRendererForShapesOnly(XYPlot plot) {
        StandardXYItemRenderer renderer = new StandardXYItemRenderer(StandardXYItemRenderer.LINES);
        renderer.setDefaultShapeFilled(true);
        plot.setRenderer(renderer);
    }
}
