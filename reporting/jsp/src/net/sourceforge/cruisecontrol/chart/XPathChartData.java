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

    private static final long serialVersionUID = 8456361615017707874L;

    private final List<XPathData> xpaths = new LinkedList<XPathData>();

    /**
     * Creates a new instance of XPathChartData
     */
    public XPathChartData() {
    }

    public void add(final String name, final String expression) throws JDOMException {
        xpaths.add(new XPathData(name, expression));
    }

    public Object produceDataset(final Map params) throws DatasetProduceException {
        final BuildInfoSummary summary = getBuildInfoSummary(params);
        final TimeSeriesCollection dataset = new TimeSeriesCollection();
        final Map<XPathData, TimeSeries> timeSeries = new LinkedHashMap<XPathData, TimeSeries>(xpaths.size());
        for (final XPathData data : xpaths) {
            final TimeSeries serie = new TimeSeries(data.getName(), Minute.class);
            timeSeries.put(data, serie);
            dataset.addSeries(serie);
        }
        for (final Iterator<BuildInfo> iter = summary.iterator(); iter.hasNext();) {
            final BuildInfo buildInfo = iter.next();
            final Date buildTime = buildInfo.getBuildDate();
            final Minute timePeriod = new Minute(buildTime);
            try {
                final Document log = buildInfo.getLogFile().asDocument();
                for (final Map.Entry<XPathData, TimeSeries> pair : timeSeries.entrySet()) {
                    final TimeSeries series = pair.getValue();
                    final Number result = pair.getKey().evaluate(log);
                    series.addOrUpdate(timePeriod, result);
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
    public void processChart(final Object chartObject, final Map params) {
        final JFreeChart chart = (JFreeChart) chartObject;
        final XYPlot plot = chart.getXYPlot();
        configurePlotRendererForShapesOnly(plot);
        setXAxisFormat(plot);
        //setYAxisFormat(plot);
    }

    /**
     * @param plot XYPlot
     */
    private void setXAxisFormat(final XYPlot plot) {
        final DateAxis xAxis = (DateAxis) plot.getHorizontalValueAxis();
        xAxis.setDateFormatOverride(new SimpleDateFormat("dd/MM"));
    }

    /**
     * @param plot XYPlot
     */
    private void configurePlotRendererForShapesOnly(final XYPlot plot) {
        final StandardXYItemRenderer renderer = new StandardXYItemRenderer(StandardXYItemRenderer.LINES);
        renderer.setDefaultShapeFilled(true);
        plot.setRenderer(renderer);
    }
}
