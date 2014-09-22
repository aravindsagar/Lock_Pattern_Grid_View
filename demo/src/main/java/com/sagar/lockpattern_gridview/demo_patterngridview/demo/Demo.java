package com.sagar.lockpattern_gridview.demo_patterngridview.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class Demo extends Activity {
    private static final int REQUEST_PATTERN_STORE = 1;
    private List<Integer> mPattern;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        findViewById(R.id.button_Store).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(getBaseContext(), StorePattern.class),
                        REQUEST_PATTERN_STORE);
            }
        });
        findViewById(R.id.button_match).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mPattern == null || mPattern.isEmpty()){
                    Toast.makeText(getBaseContext(), "Please save pattern first", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(getBaseContext(), MatchPattern.class);
                    intent.putExtra(StorePattern.EXTRA_PATTERN, (ArrayList)mPattern);
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_PATTERN_STORE){
            if(resultCode == RESULT_OK){
                mPattern = data.getIntegerArrayListExtra(StorePattern.EXTRA_PATTERN);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
