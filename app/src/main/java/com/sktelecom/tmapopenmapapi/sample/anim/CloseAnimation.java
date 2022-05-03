package com.sktelecom.tmapopenmapapi.sample.anim;

import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;

public class CloseAnimation extends TranslateAnimation implements
		TranslateAnimation.AnimationListener {

	private LinearLayout mainLayout;
	int panelWidth;

	public CloseAnimation(LinearLayout layout, int width, int fromXType,
			float fromXValue, int toXType, float toXValue, int fromYType,
			float fromYValue, int toYType, float toYValue) {

		super(fromXType, fromXValue, toXType, toXValue, fromYType, fromYValue,
				toYType, toYValue);

		// Initialize
		mainLayout = layout;
		panelWidth = width;
		setDuration(250);
		setFillAfter(false);
		setInterpolator(new AccelerateDecelerateInterpolator());
		setAnimationListener(this);
		mainLayout.startAnimation(this);

	}

	public void onAnimationEnd(Animation animation) {

		// Clear left and right margins
		LayoutParams params = (LayoutParams) mainLayout.getLayoutParams();
		params.leftMargin = -mainLayout.getMeasuredWidth();
		mainLayout.clearAnimation();
		mainLayout.setLayoutParams(params);
		mainLayout.requestLayout();

	}

	public void onAnimationRepeat(Animation animation) {

	}

	public void onAnimationStart(Animation animation) {

	}

}
