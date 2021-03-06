package com.example.wiinb.smartcrib;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedList;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;


import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

import java.text.Collator;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.google.gson.Gson;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;


import java.lang.ref.WeakReference;
import java.util.Date;

public class HeartRate extends AppCompatActivity {

    // create a dynamodb mapper;
    DynamoDBMapper dynamoDBMapper;
    String endStamp;
    String startStamp;
    GraphView myGraph;
    Switch mySwitch;
    int seriesColor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heart_rate);

        AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient(AWSMobileClient.getInstance().getCredentialsProvider());
        this.dynamoDBMapper = DynamoDBMapper.builder()
                .dynamoDBClient(dynamoDBClient)
                .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                .build();

        Button butt1 = (Button)findViewById(R.id.hourHRButt);
        Button butt2 = (Button)findViewById(R.id.dayHRButt);
        Button butt3 = (Button)findViewById(R.id.monthHRButt);
        Button delete = (Button) findViewById(R.id.trash);

        mySwitch = (Switch) findViewById(R.id.switch2);

        myGraph = (GraphView) findViewById(R.id.myGraph);

        myGraph.getViewport().setYAxisBoundsManual(true);
        myGraph.getViewport().setMinY(0);
        myGraph.getViewport().setMaxY(250);
        myGraph.getViewport().setScalable(true);

        myGraph.getGridLabelRenderer().setGridColor(Color.WHITE);
        myGraph.getGridLabelRenderer().setHorizontalLabelsColor(Color.rgb(255,215,0));
        myGraph.getGridLabelRenderer().setVerticalLabelsColor(Color.rgb(255,215,0));


        myGraph.getViewport().setBackgroundColor(Color.BLACK);
        myGraph.getViewport().setDrawBorder(true);
        myGraph.getViewport().setBorderColor(Color.WHITE);

        butt1.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                seriesColor = 1;
                endStamp = buildTimestamp(null);
                startStamp = buildTimestamp("hour");

                new getData(dynamoDBMapper, myGraph, endStamp, startStamp, mySwitch, seriesColor).execute();
            }
        });
        butt2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                seriesColor = 2;
                endStamp = buildTimestamp(null);
                startStamp = buildTimestamp("day");

                new getData(dynamoDBMapper, myGraph, endStamp, startStamp, mySwitch, seriesColor).execute();
            }
        });
        butt3.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                seriesColor = 3;
                endStamp = buildTimestamp(null);
                startStamp = buildTimestamp("month");

                new getData(dynamoDBMapper, myGraph, endStamp, startStamp, mySwitch, seriesColor).execute();
            }
        });
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myGraph.removeAllSeries();
            }
        });
    }
    //creates the timestamp and also processes the timestampRange.
    private String buildTimestamp(String timestampRange){
        String now;

        Date date = Calendar.getInstance().getTime();
        DateFormat formatter = new SimpleDateFormat("yyyy:MM:dd:HH:mm:s");
        now = formatter.format(date);
        now = subtractDate(now, timestampRange);

        return now;
    }
    //this function modifies the date in however you want. This is used to convert timezones, and
    //get a start date and end date.
    private String subtractDate(String date, String timestampRange) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy:MM:dd:HH:mm:s");
        String returnValue = "";
        if (timestampRange == "hour") {
            try {
                Date newDate = formatter.parse(date);
                Calendar cal = Calendar.getInstance();
                cal.setTime(newDate);
                cal.add(Calendar.HOUR, -1);
                Date minusOne = cal.getTime();
                returnValue = formatter.format(minusOne);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (timestampRange == "day") {
            try {
                Date newDate = formatter.parse(date);
                Calendar cal = Calendar.getInstance();
                cal.setTime(newDate);
                cal.add(Calendar.DAY_OF_MONTH, -1);
                Date minusOne = cal.getTime();
                returnValue = formatter.format(minusOne);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (timestampRange == "month") {
            try {
                Date newDate = formatter.parse(date);
                Calendar cal = Calendar.getInstance();
                cal.setTime(newDate);
                cal.add(Calendar.MONTH, -1);
                Date minusOne = cal.getTime();
                returnValue = formatter.format(minusOne);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else {
            try {
                Date newDate = formatter.parse(date);
                Calendar cal = Calendar.getInstance();
                cal.setTime(newDate);
                Date minusOne = cal.getTime();
                returnValue = formatter.format(minusOne);
            } catch (Exception e) {
            }
        }
        return returnValue;
    }


    private static class getData extends AsyncTask<BiometricsDO, Void, ArrayList<Float>> {

        ArrayList<Float> resultList = new ArrayList<Float>();
        DynamoDBMapper dynamoDBMapper;
        String endStamp;
        String startStamp;
        GraphView myGraph;
        LineGraphSeries<DataPoint> mySeries;
        Switch mySwitch;
        int seriesColor;

        getData(DynamoDBMapper dynamoDBMapper, GraphView myGraph, String endStamp, String startStamp,
                Switch mySwitch, int seriesColor){
            this.dynamoDBMapper = dynamoDBMapper;
            this.endStamp = endStamp;
            this.startStamp = startStamp;
            this.myGraph = myGraph;
            this.mySwitch = mySwitch;
            this.seriesColor = seriesColor;
        }
        @Override
        protected ArrayList<Float> doInBackground(BiometricsDO... biometricsDOS) {

            BiometricsDO bio = new BiometricsDO();
            bio.setDatatype("heartrate");

            Condition rangeKeyCondition = new Condition()
                    .withComparisonOperator(ComparisonOperator.BETWEEN)
                    .withAttributeValueList(
                            new AttributeValue().withS(startStamp),
                            new AttributeValue().withS(endStamp));

            DynamoDBQueryExpression queryExpression = new DynamoDBQueryExpression()
                    .withHashKeyValues(bio)
                    .withRangeKeyCondition("timestamp", rangeKeyCondition)
                    .withConsistentRead(false);

            PaginatedList<BiometricsDO> resultQuery = dynamoDBMapper.query(BiometricsDO.class, queryExpression);

            Gson gson = new Gson();
            // Loop through query results
            try {
                for (int i = 0; i < resultQuery.size(); i++) {
                    resultList.add(Float.parseFloat(resultQuery.get(i).getData()));
                }
            }catch(ArrayIndexOutOfBoundsException exception){

            }

            if (resultQuery.isEmpty()) {
                System.out.println("There were no items matching your query.");
            }

            return resultList;
        }

        //use onPostExecute to send data from queries to be used in another method.
        @Override
        protected void onPostExecute(ArrayList<Float> resultList) {
            int flag = 0;
            mySeries = new LineGraphSeries<>();

            if(mySwitch.isChecked())
                flag = 1;

            if (flag == 1){
                for (int i = 0; i < resultList.size(); i++) {
                    if (resultList.get(i) < 70 | resultList.get(i) > 160)
                        mySeries.appendData(new DataPoint(i, resultList.get(i)), true, resultList.size());
                }
            }else
            {
                for (int i = 0; i < resultList.size(); i++) {
                    mySeries.appendData(new DataPoint(i, resultList.get(i)), true, resultList.size());
                }
            }
            mySeries.setDrawDataPoints(true);
            mySeries.setAnimated(true);
            myGraph.getViewport().setMaxX(resultList.size());
            //adjusts the color of each series according to timestamp.
            if (seriesColor ==1){
                mySeries.setColor(Color.parseColor("#c91f96"));
            }
            else if (seriesColor == 2){
                mySeries.setColor(Color.parseColor("#3bbc2f"));

            }else {
                mySeries.setColor(Color.parseColor("#34aab3"));
            }
            //adds the series to the graph
            myGraph.addSeries(mySeries);


        }
    }

}
