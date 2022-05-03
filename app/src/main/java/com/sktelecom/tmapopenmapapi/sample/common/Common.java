package com.sktelecom.tmapopenmapapi.sample.common;

import com.sktelecom.tmapopenmapapi.sample.R;
import com.sktelecom.tmapopenmapapi.sample.R.drawable;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class Common {
	
	public static boolean isDebugMode = true;
	public static boolean isProgressDialogShow = false;
	
	public static DialogInterface.OnClickListener mListener;
	public static OnClickSelectListenerCallback 	   onClickSelectListener;
	
	
	public static void showAlertDialog(Context ctx, String title, String msg,
			DialogInterface.OnClickListener listener) {
		mListener = listener;
		showAlertDialog(ctx, title, msg);
	}
	
	public interface OnClickSelectListenerCallback {
		public boolean onSelectEvent(int item);
	}
	
	public static void setOnClickSelectListenerCallBack(Context context, String title, int select, OnClickSelectListenerCallback listener) {
		onClickSelectListener = listener;
		showSelectAlertDialog(context, title, select);
	}
	
	public static void showSelectAlertDialog(Context context, String title, int select) {
		AlertDialog alertDialog = new AlertDialog.Builder(context)
				.setIcon(R.drawable.tmark)
				.setTitle(title)
				.setSingleChoiceItems(select, 0, new DialogInterface.OnClickListener() {						
					@Override
					public void onClick(DialogInterface dialog, int item) {
						dialog.dismiss();
						if (onClickSelectListener != null) {				
							onClickSelectListener.onSelectEvent(item);
						}		
					}
				}).show();
	}	
	
	public static void showAlertDialog(Context ctx, String title, String msg) {
		AlertDialog alertDialog = new AlertDialog.Builder(ctx)
			    .setIcon(R.drawable.tmark)
			    .setTitle(title)
				.setMessage(msg)
				.setNeutralButton("확인", new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					if(mListener != null){
						mListener.onClick(dialog, which);
						mListener = null;
					}
				}
							
			}).show();
	}
	
}
