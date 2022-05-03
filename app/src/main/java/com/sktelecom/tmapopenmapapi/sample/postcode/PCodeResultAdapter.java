package com.sktelecom.tmapopenmapapi.sample.postcode;

import java.util.ArrayList;

import com.sktelecom.tmapopenmapapi.sample.R;
import com.sktelecom.tmapopenmapapi.sample.postcode.PostCode.PostCodeVO;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class PCodeResultAdapter extends BaseAdapter {
	
	private Context mContext = null;
	
	// Adapter에 추가된 데이터를 저장하기 위한 ArrayList
    private ArrayList<PostCodeVO> alPostCodeVO = null;

    // ListViewAdapter의 생성자
    public PCodeResultAdapter( Context context, ArrayList<PostCodeVO> alPostCodeVO ) {
    	this.mContext = context;
    	this.alPostCodeVO = alPostCodeVO;
    }

    // Adapter에 사용되는 데이터의 개수를 리턴. : 필수 구현
    @Override
    public int getCount() {
    	int count = 0;
    	if( alPostCodeVO != null ) {
    		count = alPostCodeVO.size();
    	}
        return count;
    }
    
    private class ViewHolder {
        public TextView txtNewAddr = null;
        public TextView txtOldAddr = null;
        public TextView txtZipCode = null;
    }

    // position에 위치한 데이터를 화면에 출력하는데 사용될 View를 리턴. : 필수 구현
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        
        if( convertView == null ) {
            holder = new ViewHolder();

            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.pcode_result_item, null);
            holder.txtNewAddr = (TextView) convertView.findViewById(R.id.txtNewAddr);
            holder.txtOldAddr = (TextView) convertView.findViewById(R.id.txtOldAddr);
            holder.txtZipCode = (TextView) convertView.findViewById(R.id.txtZipCode);

            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder)convertView.getTag();
        }

        PostCodeVO postCodeVO = alPostCodeVO.get(position);

        // 아이템 내 각 위젯에 데이터 반영
        holder.txtNewAddr.setText(postCodeVO.newAddr);
        holder.txtOldAddr.setText(postCodeVO.oldAddr);
        holder.txtZipCode.setText(postCodeVO.zipcode);

        return convertView;
    }

    // 지정한 위치(position)에 있는 데이터와 관계된 아이템(row)의 ID를 리턴. : 필수 구현
    @Override
    public long getItemId(int position) {
        return position;
    }

    // 지정한 위치(position)에 있는 데이터 리턴 : 필수 구현
    @Override
    public Object getItem(int position) {
    	Object obj = null;
    	if( alPostCodeVO != null ) {
    		obj = alPostCodeVO.get(position);
    	}
        return obj;
    }
}
