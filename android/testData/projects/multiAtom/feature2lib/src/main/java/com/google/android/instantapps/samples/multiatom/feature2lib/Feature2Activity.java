package com.google.android.instantapps.samples.multiatom.feature2lib;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * A simple activity that allows the user to switch to an equally simple activity.
 */
public class Feature2Activity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_feature2);

    Button goToFeature1Button = (Button) findViewById(R.id.feature1_button);
    goToFeature1Button.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent feature1Intent = new Intent(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setData(Uri.parse("https://multiatom.samples.androidinstantapps.com/feature1"));
        startActivity(feature1Intent);
      }
    });
  }
}
