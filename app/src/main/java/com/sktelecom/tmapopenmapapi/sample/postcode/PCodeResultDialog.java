package com.sktelecom.tmapopenmapapi.sample.postcode;

import java.util.ArrayList;

import com.sktelecom.tmapopenmapapi.sample.R;
import com.sktelecom.tmapopenmapapi.sample.postcode.PostCode.PostCodeVO;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ListView;

public class PCodeResultDialog extends Dialog {
	
	private Context mContext = null;
	
	private ListView lvResult = null;
	private ArrayList<PostCodeVO> alPostCodeVO = null;
	
	
	public PCodeResultDialog(Context context, ArrayList<PostCodeVO> alPostCodeVO) { 
		super(context);
		mContext = context;
		this.alPostCodeVO = alPostCodeVO;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		// 다이얼로그 외부 화면 흐리게 표현
        WindowManager.LayoutParams lpWindow = new WindowManager.LayoutParams();
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        lpWindow.dimAmount = 0.8f;
        getWindow().setAttributes(lpWindow);
        
        setContentView(R.layout.pcode_result_dialog);		
		
		lvResult = (ListView)findViewById(R.id.lvResult);
		lvResult.setAdapter(new PCodeResultAdapter(mContext, alPostCodeVO));
	}
	
	

}
