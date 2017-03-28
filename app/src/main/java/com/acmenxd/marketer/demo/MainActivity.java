package com.acmenxd.marketer.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.acmenxd.marketer.Marketer;

public class MainActivity extends AppCompatActivity {
    /**
     * 默认渠道号
     */
    public static final String DEFAULT_MARKET = "AcmenXD";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Marketer使用演示");
        setContentView(R.layout.activity_main);
    }

    public void getMarketInfoClick(View view) {
        String market = DEFAULT_MARKET;
        market = Marketer.getMarket(this, market);
        Toast.makeText(this, "渠道信息:" + market, Toast.LENGTH_LONG).show();
    }
}
