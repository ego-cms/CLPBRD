package com.ego_cms.copypaste;

import android.content.Context;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class ActivityBaseCompat extends AppCompatActivityBase {

	@Override
	protected void attachBaseContext(Context newBase) {
		super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
	}
}
