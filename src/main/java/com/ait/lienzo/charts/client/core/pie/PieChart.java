/*
   Copyright (c) 2014,2015,2016 Ahome' Innovation Technologies. All rights reserved.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.ait.lienzo.charts.client.core.pie;

import static com.ait.lienzo.client.core.animation.AnimationProperties.toPropertyList;
import static com.ait.lienzo.client.core.animation.AnimationProperty.Properties.ALPHA;
import static com.ait.lienzo.client.core.animation.AnimationTweener.LINEAR;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.ait.lienzo.charts.client.ChartData;
import com.ait.lienzo.charts.client.core.AbstractChart;
import com.ait.lienzo.charts.client.core.ChartAttribute;
import com.ait.lienzo.charts.client.core.ChartNodeType;
import com.ait.lienzo.charts.client.core.ColorPalette;
import com.ait.lienzo.charts.client.core.factory.IChartDataBuilder;
import com.ait.lienzo.charts.client.core.factory.IChartDataBuilderAggregator;
import com.ait.lienzo.charts.client.core.factory.IChartDataHandler;
import com.ait.lienzo.charts.client.core.factory.IBuildedChart;
import com.ait.lienzo.charts.client.core.legend.ChartLegend;
import com.ait.lienzo.charts.client.core.model.DataTable;
import com.ait.lienzo.charts.client.core.model.PieChartData;
import com.ait.lienzo.charts.client.core.model.actions.AddDataAction;
import com.ait.lienzo.charts.client.core.model.actions.DataAction;
import com.ait.lienzo.charts.client.core.pie.animation.AddDataAnimation;
import com.ait.lienzo.charts.client.core.pie.animation.PieChartResizeAnimation;
import com.ait.lienzo.charts.client.core.pie.event.DataReloadedEvent;
import com.ait.lienzo.charts.client.core.pie.event.DataReloadedEventHandler;
import com.ait.lienzo.charts.client.core.pie.event.ValueSelectedEvent;
import com.ait.lienzo.charts.client.core.pie.event.ValueSelectedHandler;
import com.ait.lienzo.charts.client.core.resizer.ChartResizeEvent;
import com.ait.lienzo.charts.shared.core.types.palettes.PatternFlyPalette;
import com.ait.lienzo.client.core.animation.AnimationTweener;
import com.ait.lienzo.client.core.event.NodeMouseClickEvent;
import com.ait.lienzo.client.core.event.NodeMouseClickHandler;
import com.ait.lienzo.client.core.event.NodeMouseEnterEvent;
import com.ait.lienzo.client.core.event.NodeMouseEnterHandler;
import com.ait.lienzo.client.core.event.NodeMouseExitEvent;
import com.ait.lienzo.client.core.event.NodeMouseExitHandler;
import com.ait.lienzo.client.core.event.NodeMouseMoveEvent;
import com.ait.lienzo.client.core.event.NodeMouseMoveHandler;
import com.ait.lienzo.client.core.shape.Group;
import com.ait.lienzo.client.core.shape.IContainer;
import com.ait.lienzo.client.core.shape.Node;
import com.ait.lienzo.client.core.shape.Slice;
import com.ait.lienzo.client.core.shape.Text;
import com.ait.lienzo.client.core.shape.json.validators.ValidationContext;
import com.ait.lienzo.client.core.shape.json.validators.ValidationException;
import com.ait.lienzo.client.core.types.BoundingBox;
import com.ait.lienzo.shared.core.types.Color;
import com.ait.lienzo.shared.core.types.ColorName;
import com.ait.lienzo.shared.core.types.IColor;
import com.ait.lienzo.shared.core.types.TextBaseLine;
import com.ait.tooling.nativetools.client.collection.NFastArrayList;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.json.client.JSONObject;

public class PieChart extends AbstractChart<PieChart> implements IBuildedChart
{
    private Group                  m_slices;

    private Group                  m_labels;

    private List<Text>             m_texts        = new LinkedList<>();

    private List<PieSlice>         m_pieSlices    = new LinkedList<>();

    private PieChartTooltip        m_tooltip;

    private DataHandler            m_dataHandler;

    private ColorPalette           m_colorPalette;

    private boolean                m_showPieTexts = false;

    private NFastArrayList<Double> m_percents     = new NFastArrayList<>();

    protected PieChart(JSONObject node, ValidationContext ctx) throws ValidationException
    {
        super(ChartNodeType.PIE_CHART, node, ctx);
        loadDefaultProperties();
    }

    public PieChart()
    {
        super(ChartNodeType.PIE_CHART);
        loadDefaultProperties();
    }

    private void loadDefaultProperties()
    {
        addNodeMouseMoveHandler(new NodeMouseMoveHandler()
        {
            @Override
            public void onNodeMouseMove(NodeMouseMoveEvent event)
            {
                if (m_tooltip != null && m_tooltip.isVisible())
                {
                    m_tooltip.show(event.getX() - 90, event.getY() - 90);
                }
            }
        });

        m_colorPalette = new PatternFlyPalette();
        setAlpha(1);
    }

    @Override
    public PieChart init()
    {
        return PieChartAnimationHelper.create(this, LINEAR, getDefaultAnimationDuration());
    }

    @Override
    public PieChart init(final double duration)
    {
        return PieChartAnimationHelper.create(this, LINEAR, Math.max(duration, 1));
    }

    @Override
    public PieChart init(final AnimationTweener tweener, final double duration)
    {
        return PieChartAnimationHelper.create(this, ((tweener != null) ? tweener : LINEAR), Math.max(duration, 1));
    }

    public PieChart reload(PieChartData data)
    {
        return PieChartAnimationHelper.reload(this, data, LINEAR, getDefaultAnimationDuration());
    }

    public PieChart reload(PieChartData data, double duration)
    {
        return PieChartAnimationHelper.reload(this, data, LINEAR, Math.max(duration, 1));
    }

    public PieChart reload(PieChartData data, AnimationTweener tweener, double duration)
    {
        return PieChartAnimationHelper.reload(this, data, ((tweener != null) ? tweener : LINEAR), Math.max(duration, 1));
    }

    public HandlerRegistration addDataReloadedHandler(DataReloadedEventHandler handler)
    {
        return addEnsureHandler(DataReloadedEvent.TYPE, handler);
    }

    public HandlerRegistration addValueSelectedHandler(ValueSelectedHandler handler)
    {
        return addEnsureHandler(ValueSelectedEvent.TYPE, handler);
    }

    @Override
    public void doDraw()
    {
        PieChartData data = getData();

        if (getRadius(getChartWidth(), getChartHeight()) <= 0 || (null == data) || (data.size() < 1))
        {
            return;
        }
        m_slices = new Group();

        m_labels = new Group();
        
        _build(data);

        // Tooltip.
        buildTooltip();
    }

    @Override
    protected void moveAreas(double x, double y)
    {
        super.moveAreas(x, y);

        // Center slices and texts.
        if (m_slices != null)
        {
            m_slices.setX(getChartWidth() / 2).setY(getChartHeight() / 2);
        }

        if (m_labels != null)
        {
            BoundingBox bb = m_labels.getBoundingBox();
            bb.getWidth();
          //  m_labels.setX(0).setY(getChartHeight() / 2);
        }
    }

    private void _build(PieChartData data)
    {
        final DataTable dataTable = data.getDataTable();
        final String[] categories = dataTable.getColumn(getData().getCategoriesProperty()).getStringValues();
        final Double[] values = dataTable.getColumn(getData().getValuesProperty()).getNumericValues();

        m_labels.setListening(false);

        //double sofar = 0;

        double total = 0;

        for (int i = 0; i < values.length; i++)
        {
            total += values[i];
        }
        for (int i = 0; i < values.length; i++)
        {
            final double value = values[i] / total;

            addSlice(categories, values, i, value);
        }
        
       // chartArea.add(m_labels);
        
      //  addOnAreaChartCentered(m_labels);

        addOnAreaChartCentered(m_slices);
    }

    public void addSlice(int i, final double value)
    {
        final DataTable dataTable = getData().getDataTable();
        final String[] categories = dataTable.getColumn(getData().getCategoriesProperty()).getStringValues();
        final Double[] values = dataTable.getColumn(getData().getValuesProperty()).getNumericValues();

        addSlice(categories, values, i, value);
        refreshTexts();
    }

    private void addSlice(final String[] categories, final Double[] values, int i, final double value)
    {
        final PieSlice slice = new PieSlice(0, 0, 0);

        final int index = i;

        m_percents.set(i, value * 100);

        slice.addNodeMouseClickHandler(new NodeMouseClickHandler()
        {
            @Override
            public void onNodeMouseClick(NodeMouseClickEvent event)
            {
                // GWT.log("PieChart - filtering on "  + categories[index] + "/" + index);
                PieChart.this.fireEvent(new ValueSelectedEvent(getData().getCategoriesProperty(), index));
            }
        });
        final int _i = i;
        final String category = categories[i];
        final double tv = values[i];
        slice.addNodeMouseEnterHandler(new NodeMouseEnterHandler()
        {
            @Override
            public void onNodeMouseEnter(NodeMouseEnterEvent event)
            {
                // Animate other slices.
                alphaToOtherSlices(slice.getID(), 0.7);

                m_tooltip.setValues(category, getValue(tv), Color.fromColorString(slice.getFillColor()), getValue(m_percents.get(_i)) + "%");

                m_tooltip.show(event.getX() - 90, event.getY() - 90);

                if (m_showPieTexts)
                {
                    Text text = m_texts.get(_i);
                    if (text != null)
                    {
                        text.animate(LINEAR, toPropertyList(ALPHA(0)), getDefaultAnimationDuration());
                    }
                }
            }
        });

        slice.addNodeMouseExitHandler(new NodeMouseExitHandler()
        {
            @Override
            public void onNodeMouseExit(NodeMouseExitEvent event)
            {
                // Animate other slices.
                alphaToOtherSlices(slice.getID(), 1);

                // Hide tooltip.
                if (m_tooltip != null)
                {
                    m_tooltip.hide();
                }

                if (m_showPieTexts)
                {
                    Text text = m_texts.get(_i);
                    if (text != null)
                    {
                        text.animate(LINEAR, toPropertyList(ALPHA(1)), getDefaultAnimationDuration());
                    }
                }
            }
        });

        slice.setFillColor(getColor(i)).setStrokeColor(m_colorPalette.getBordersColor()).setStrokeWidth(1);

        slice.setID("pieSlice" + i);
        m_pieSlices.add(slice);
        m_slices.add(slice);

        if (m_showPieTexts)
        {
            Text text = new Text(getLabel(value * 100), getFontFamily(), getFontStyle(), getFontSize());
            text.setFillColor(ColorName.BLACK).setTextBaseLine(TextBaseLine.MIDDLE).setAlpha(0);
            m_texts.add(text);
        }

        // ?????????
        Text text = new Text(getLabel(value * 100), getFontFamily(), getFontStyle(), getFontSize());

        text.setFillColor(ColorName.BLACK).setTextBaseLine(TextBaseLine.MIDDLE).setAlpha(0);

        m_labels.add(text);        
    }

    public void refreshTexts()
    {
        double total = 0;

        final Double[] values = getData().getDataTable().getColumn(getData().getValuesProperty()).getNumericValues();
        for (int i = 0; i < values.length; i++)
        {
            total += values[i];
        }

        for (int i = 0; i < values.length; i++)
        {
            final double value = (values[i] / total) * 100;

            if (m_showPieTexts)
            {
                getTexts().get(i).setText(getLabel(value));
            }

            m_percents.set(i, value);
        }

        buildLegend();
    }

    public Group getSlices()
    {
        return m_slices;
    }

    public Group getLabels()
    {
        return m_labels;
    }

    public List<Text> getTexts()
    {
        return m_texts;
    }

    public List<PieSlice> getPieSlices()
    {
        return m_pieSlices;
    }

    public PieChartTooltip getTooltip()
    {
        return m_tooltip;
    }

    @Override
    public void clear()
    {
        m_texts.clear();
        m_labels.removeFromParent();
        m_pieSlices.clear();
        m_slices.removeFromParent();
        if (m_tooltip != null)
        {
            m_tooltip.removeFromParent();
        }
        super.clear();
    }

    @Override
    protected void onChartResize(ChartResizeEvent event)
    {
        boolean apply = event.isApply();

        if (apply)
        {
            final double w = event.getWidth() - getMarginLeft() - getMarginRight();
            final double h = event.getHeight() - getMarginTop() - getMarginBottom();
            // Apply resize to bar chart.
            new PieChartResizeAnimation(this, w, h, LINEAR, getDefaultAnimationDuration(), null).run();
        }
        super.onChartResize(event);
    }

    protected void alphaToOtherSlices(String sliceID, double alpha)
    {
        for (PieSlice slice : m_pieSlices)
        {
            if (!slice.getID().equals(sliceID))
            {
                GWT.log(slice.getID() + " From: " + slice.getAlpha() + " To: " + alpha);

                // test
                slice.setAlpha(slice.getAlpha());

                if (slice.getAlpha() != alpha)
                {
                    slice.animate(LINEAR, toPropertyList(ALPHA(alpha)), getDefaultAnimationDuration());
                }
                else
                {

                }
            }
        }
    }

    protected void setColorPalette(ColorPalette palette)
    {
        m_colorPalette = palette;
    }

    @Override
    protected void buildTitle()
    {
        super.buildTitle();

        topArea.add(chartTitle);
    }

    @Override
    protected void buildLegend()
    {
        if (legend != null)
        {
            legend.removeAll();

            legend.removeFromParent();
        }
      
            super.buildLegend();
      
        
        // Set legend entries.
        PieChartData data = getData();
        if (legend != null && getData().getDataTable() != null)
        {
            String catAxisProp = data.getCategoriesProperty();
            String[] values = getData().getDataTable().getColumn(catAxisProp).getStringValues();
            if (values != null && values.length > 0)
            {
                for (int x = 0; x < values.length; x++)
                {
                    String value = values[x];
                    legend.add(new ChartLegend.ChartLegendEntry(value, getColor(x)));
                }
            }  
            
            legend.build();
            
            double legendWidth = legend.getWidth();
            double middle = (getX() + getWidth()) / 2;
            double position = middle - (legendWidth/2);
            GWT.log("Legend position: "+ legend.getX() + ", "+ legend.getY());
            legend.setX(position);       
        } 
    }

    private void buildTooltip()
    {
        m_tooltip = new PieChartTooltip();

        addOnAreaChartCentered(m_tooltip);
    }

    protected void addOnAreaChartCentered(Group group)
    {
        chartArea.add(group);
        group.setX(getChartWidth() / 2);
        group.setY(getChartHeight() / 2);
        group.setAlpha(1d);
    }

    protected IColor getColor(int position)
    {
        return m_colorPalette.getColor(position);
    }

    public final PieChart setData(PieChartData data)
    {
        if (null != data)
        {
            getAttributes().put(ChartAttribute.PIE_CHART_DATA.getProperty(), data.getJSO());
        }
        PieChart.this.fireEvent(new DataReloadedEvent(this));

        return this;
    }

    public final PieChartData getData()
    {
        if (getAttributes().isDefined(ChartAttribute.PIE_CHART_DATA))
        {
            PieChartData.PieChartDataJSO jso = getAttributes().getObject(ChartAttribute.PIE_CHART_DATA.getProperty()).cast();

            return new PieChartData(jso);
        }
        return null;
    }

    public final double getRadius(double chartWidth, double chartHeight)
    {
        double forSize = chartHeight;

        if (getChartWidth() < forSize) forSize = chartWidth;

        return forSize / 2;
    }

    private final native String getLabel(double perc)
    /*-{
    	var numb = perc;
    
    	return numb.toFixed(2) + "%";
    }-*/;

    private final native String getValue(double valu)
    /*-{
        var numb = valu;
    
        return numb.toFixed(2) + "";
    }-*/;

    public static class PieChartFactory extends ChartFactory<PieChart>
    {
        public PieChartFactory()
        {
            super(ChartNodeType.PIE_CHART);

            addAttribute(ChartAttribute.RADIUS, true);

            addAttribute(ChartAttribute.PIE_CHART_DATA, true);
        }

        @Override
        public boolean addNodeForContainer(IContainer<?, ?> container, Node<?> node, ValidationContext ctx)
        {
            return false;
        }

        @Override
        protected PieChart container(JSONObject node, ValidationContext ctx) throws ValidationException
        {
            return new PieChart(node, ctx);
        }
    }

    public static class PieSlice extends Slice
    {
        public PieSlice(double radius, double sofar, double value)
        {
            super(radius, Math.PI * (-0.5 + 2 * sofar), Math.PI * (-0.5 + 2 * (sofar + value)), false);
        }

        public static double buildStartAngle(double sofar)
        {
            return Math.PI * (-0.5 + 2 * sofar);
        }

        public static double buildEndAngle(double sofar, double value)
        {
            return Math.PI * (-0.5 + 2 * (sofar + value));
        }
    }

    @Override
    public IBuildedChart using(ChartData data)
    {
        setData(new PieChartData(data.getData(), data.getCategoriesAxis(), data.getValuesAxis()));
        return this;
    }

    @Override
    public IBuildedChart withColorPalette(ColorPalette palette)
    {
        setColorPalette(palette);
        return this;
    }

    @Override
    public IChartDataHandler getDataHandler()
    {
        if (m_dataHandler == null)
        {
            m_dataHandler = new DataHandler(getData().getDataTable(), this);
        }

        return m_dataHandler;
    }

    class DataHandler implements IChartDataHandler
    {
        private final DataTable                  m_dataTable;

        private final NFastArrayList<DataAction> m_pendingActions;

        private final PieChart                   m_pieChart;

        public DataHandler(DataTable dataTable, PieChart pieChart)
        {
            m_pendingActions = new NFastArrayList<>();
            m_dataTable = dataTable;
            m_pieChart = pieChart;
        }

        @Override
        public IChartDataBuilder add(String someData)
        {
            if (m_dataTable.size() == 0)
            {
                // TODO: add column to an empty table
            }
            else
            {
                String firstColumnHeader = m_dataTable.getColumn(0).getId();
                AddDataAction<String> action = new AddDataAction<>(firstColumnHeader, someData);
                m_pendingActions.add(action);
            }

            return new ChartDataBuilder(m_pendingActions, m_pieChart, m_pieChart.getDefaultAnimationDuration());
        }

        @Override
        public void applyChanges()
        {
            for (DataAction dataAction : m_pendingActions)
            {
                dataAction.apply(m_dataTable);
            }

            m_pendingActions.clear();
        }
    }

    final class ChartDataBuilder implements IChartDataBuilder
    {
        private final NFastArrayList<DataAction> m_pendingActions;

        private final PieChart                   m_pieChart;

        private final double                     m_animationDuration;

        ChartDataBuilder(NFastArrayList<DataAction> pendingActions, PieChart pieChart, double duration)
        {
            m_pendingActions = pendingActions;
            m_pieChart = pieChart;
            m_animationDuration = duration;
        }

        @Override
        public IChartDataBuilderAggregator with(Double value, String header)
        {
            // Now the "with" should get adds 'value' at column 'header' in the same row
            // returned after the previous action was executed.   
            AddDataAction<Double> action = new AddDataAction<>(header, value);
            return addAndCreateAnimation(action);
        }

        @Override
        public IChartDataBuilderAggregator with(String value, String header)
        {
            AddDataAction<String> action = new AddDataAction<>(header, value);
            return addAndCreateAnimation(action);
        }

        @Override
        public IChartDataBuilderAggregator with(Date value, String header)
        {
            AddDataAction<Date> action = new AddDataAction<>(header, value);
            return addAndCreateAnimation(action);
        }

        private IChartDataBuilderAggregator addAndCreateAnimation(DataAction action)
        {
            action.setAnimation(buildAddAnimation());
            m_pendingActions.add(action);
            return new ChartDataBuilderAggregator(m_pendingActions);
        }

        private AddDataAnimation buildAddAnimation()
        {
            return new AddDataAnimation(m_pieChart, m_pieChart.getChartWidth(), m_pieChart.getChartHeight(), AnimationTweener.EASE_IN, m_animationDuration, null);
        }
    }

    class ChartDataBuilderAggregator implements IChartDataBuilderAggregator
    {
        private final NFastArrayList<DataAction> m_pendingActions;

        ChartDataBuilderAggregator(NFastArrayList<DataAction> pendingActions)
        {
            m_pendingActions = pendingActions;
        }

        @Override
        public IChartDataBuilderAggregator and(Double value, String header)
        {
            AddDataAction<Double> action = new AddDataAction<>(header, value);
            m_pendingActions.add(action);
            return this;
        }

        @Override
        public IChartDataBuilderAggregator and(String value, String header)
        {
            AddDataAction<String> action = new AddDataAction<>(header, value);
            m_pendingActions.add(action);
            ;
            return this;
        }

        @Override
        public IChartDataBuilderAggregator and(Date value, String header)
        {
            AddDataAction<Date> action = new AddDataAction<>(header, value);
            m_pendingActions.add(action);
            return this;
        }
    }

    @Override
    public void applyDataChanges()
    {
        getDataHandler().applyChanges();

    }
}