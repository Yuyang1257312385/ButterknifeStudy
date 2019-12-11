package com.yu.butterknife;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.yu.butterknife_annotation.BindView;
import com.yu.knife.ButterKnife;


public class MainActivity extends AppCompatActivity {

    @BindView(R.id.tv_1)
    TextView mTv1;
    @BindView(R.id.tv_2)
    TextView mTv2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mTv1.setText("this is tv1");
        mTv2.setText("this is tv2");
    }
}
