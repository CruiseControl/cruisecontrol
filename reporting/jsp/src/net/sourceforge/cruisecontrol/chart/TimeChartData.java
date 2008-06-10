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

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import net.sourceforge.cruisecontrol.BuildInfo;
import net.sourceforge.cruisecontrol.BuildInfoSummary;
import net.sourceforge.cruisecontrol.util.TimeNumberFormat;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.StandardXYItemRenderer;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import de.laures.cewolf.ChartPostProcessor;
import de.laures.cewolf.DatasetProduceException;

public class TimeChartData extends AbstractCruiseControlChartData implements ChartPostProcessor {

    private static final long serialVersionUID = -5159867264828131088L;

    public Object produceDataset(Map params) throws DatasetProduceException {
        BuildInfoSummary summary = getBuildInfoSummary(params);
        TimeSeries brokenSeries = new TimeSeries("Broken Builds", Minute.class);
        TimeSeries goodSeries = new TimeSeries("Good Builds", Minute.class);
        for (Iterator iter = summary.iterator(); iter.hasNext();) {
            BuildInfo buildInfo = (BuildInfo) iter.next();
            Date buildTime = buildInfo.getBuildDate();
            double timeValue = extractTimeOfDay(buildTime);
            Minute timePeriod = new Minute(buildTime);
            TimeSeries seriesToAddTo = buildInfo.isSuccessful() ? goodSeries
                                                                : brokenSeries;
            if (seriesToAddTo.getDataPair(timePeriod) == null) {
                seriesToAddTo.add(timePeriod, timeValue);
            } else {
                System.err.println("multiple logs in the same minute; ignoring: " + buildInfo.getLogName());
            }
        }
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(brokenSeries);
        dataset.addSeries(goodSeries);
        return dataset;
    }

    /**
     * @param buildTime
     * @return Only the time part of the Date
     */
    private double extractTimeOfDay(Date buildTime) {
        Calendar buildCalendar = Calendar.getInstance();
        buildCalendar.setTime(buildTime);
        double timeValue = buildCalendar.get(Calendar.HOUR_OF_DAY);
        timeValue = timeValue * 60 + buildCalendar.get(Calendar.MINUTE);
        timeValue = timeValue * 60 + buildCalendar.get(Calendar.SECOND);
        timeValue = timeValue * 1000 + buildCalendar.get(Calendar.MILLISECOND);
        return timeValue;
    }

    public String getProducerId() {
        return "TimeChartData DatasetProducer";
    }


    /**
     * @see ChartPostProcessor#processChart(Object, Map)
     */
    public void processChart(Object chartObject, Map params) {
        JFreeChart chart = (JFreeChart) chartObject;
        XYPlot plot = chart.getXYPlot();
        configurePlotRendererForShapesOnly(plot);
        setXAxisFormat(plot);
        setYAxisFormat(plot);
    }

    /**
     * @param plot
     */
    private void setYAxisFormat(XYPlot plot) {
        NumberAxis yAxis = (NumberAxis) plot.getVerticalAxis();
        NumberFormat yAxisFormat = new TimeNumberFormat();
        yAxis.setNumberFormatOverride(yAxisFormat);
        yAxis.setMinimumAxisValue(0);
        yAxis.setMaximumAxisValue(24 * 60 * 60 * 1000);
        yAxis.setTickUnit(new NumberTickUnit(2 * 60 * 60 * 1000));
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
        StandardXYItemRenderer renderer = new StandardXYItemRenderer(StandardXYItemRenderer.SHAPES);
        renderer.setDefaultShapeFilled(true);
        plot.setRenderer(renderer);
    }
}