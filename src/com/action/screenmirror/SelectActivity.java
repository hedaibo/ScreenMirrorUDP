package com.action.screenmirror;

import com.action.screenmirror.audio.AudioRecord;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class SelectActivity extends Activity {

	private static final String TAG = "SelectActivity";

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.activity_select);

		Button btReceiver = findViewById(R.id.bt_receiver);
		Button btSend = findViewById(R.id.bt_send);
		
		btReceiver.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				SendServer mServer = SendServer.getInstance();
				if (mServer != null && mServer.hasConnect()) {
					Toast.makeText(getApplicationContext(), "Currently in TX mode !", Toast.LENGTH_SHORT).show();
					return;
				}
				startActivity(new Intent(SelectActivity.this, ReceiverActivity.class));
			}
		});

		btSend.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				startActivity(new Intent(SelectActivity.this, SendActivity.class));
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
	}

}
