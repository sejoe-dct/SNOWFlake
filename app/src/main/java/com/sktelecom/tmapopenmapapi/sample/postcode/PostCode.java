package com.sktelecom.tmapopenmapapi.sample.postcode;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sktelecom.tmapopenmapapi.sample.common.SLHttpRequest;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

public class PostCode {
	
	private Context mContext = null;
	private String mApiKey = null;
	
	public PostCode(Context context, String apiKey) {
		mContext = context;
		mApiKey = apiKey;
	}
	
	public class PostCodeVO {
		public String oldAddr = null;
		public String newAddr = null;
		public String zipcode = null;
		public PostCodeVO(String oldAddr, String newAddr, String zipcode) {
			this.oldAddr = oldAddr;
			this.newAddr = newAddr;
			this.zipcode = zipcode;
		}
	}
	
	/**
	 * 우편번호 검색 리스너
	 * @author JWCha
	 *
	 */
	public interface OnFindPostCodeListener {
		public void onComplete( ArrayList<PostCodeVO> alPostCodeVO );
	}
	
	/**
	 * 우편번호 검색 API
	 */
	public void findPostCode(String address, final OnFindPostCodeListener listener) {
//		removeMarker();
		SLHttpRequest request = new SLHttpRequest("https://api2.sktelecom.com/tmap/geo/postcode"); // SKT
	    request.addParameter("version", "1");
	    request.addParameter("appKey", mApiKey);
	    request.addParameter("addr", address);
	    request.addParameter("coordType", "WGS84GEO");
	    request.addParameter("addressFlag", "F00");
	    request.addParameter("format", "json");
	    request.addParameter("page", "1");
	    request.addParameter("count", "20");
	    request.send(new SLHttpRequest.OnResponseListener() {

			@Override
			public void OnSuccess(String data) {
				// TODO Auto-generated method stub
				try {
					int i;
					ArrayList<PostCode.PostCodeVO> alPostCodeVo = new ArrayList<PostCode.PostCodeVO>();
					
					JSONObject objData = new JSONObject(data).getJSONObject("coordinateInfo");
					JSONArray arrCoordinate = objData.getJSONArray("coordinate");
					int length = arrCoordinate.length();
					JSONObject objCoordinate = null;
					
					String newRoadAddr = "";
					String jibunAddr = "";
					
					for( i=0; i<length; i++ ) {
						objCoordinate = arrCoordinate.getJSONObject(i);
						
						//법정동 마지막 문자 
						String lastLegal = objCoordinate.getString("legalDong").charAt(objCoordinate.getString("legalDong").length()-1) + "";

						// 새주소
						newRoadAddr = objCoordinate.getString("city_do") + ' ' + objCoordinate.getString("gu_gun") + ' '; 

						if(objCoordinate.getString("eup_myun").equals("") && (lastLegal.equals("읍") || lastLegal.equals("면"))){//읍면
							newRoadAddr +=  objCoordinate.getString("legalDong");	
						}else{
							newRoadAddr +=  objCoordinate.getString("eup_myun");
						}
						newRoadAddr += " " +objCoordinate.getString("newRoadName") + " " + objCoordinate.getString("newBuildingIndex");

						// 새주소 법정동 & 건물명 체크
						if( !objCoordinate.getString("legalDong").equals("") && (!lastLegal.equals("읍") && !lastLegal.equals("면")) ){//법정동과 읍면이 같은 경우
							
							if( !objCoordinate.getString("buildingName").equals("") ){//빌딩명 존재하는 경우
								newRoadAddr +=  (" (" + objCoordinate.getString("legalDong") + ", " +objCoordinate.getString("buildingName") + ") ");
							}else{
								newRoadAddr += (" (" + objCoordinate.getString("legalDong") + ')');
							}
						}else if( !objCoordinate.getString("buildingName").equals("") ){//빌딩명만 존재하는 경우
							newRoadAddr +=  (" (" + objCoordinate.getString("buildingName") +") ");
						}

						// 구주소
						jibunAddr = objCoordinate.getString("city_do") + " " + objCoordinate.getString("gu_gun") + ' ' + objCoordinate.getString("legalDong") + " " + objCoordinate.getString("ri") + " " + objCoordinate.getString("bunji");
						//구주소 빌딩명 존재
						if( !objCoordinate.getString("buildingName").equals("") ){//빌딩명만 존재하는 경우
							jibunAddr +=  (" " + objCoordinate.getString("buildingName"));
						}
						
						// 결과 추가
						alPostCodeVo.add(new PostCodeVO(jibunAddr, newRoadAddr, objCoordinate.getString("zipcode")));
					}
					
					listener.onComplete( alPostCodeVo ); // 결과 리스너로 전달
				}catch(Exception e) {
					Log.d("debug", e.toString());
					listener.onComplete( null ); // 결과 리스너로 전달
				}
			}

			@Override
			public void OnFail(int errorCode, String errorMessage) {
				// TODO Auto-generated method stub
				Log.d("debug", "errorMessage :" + errorMessage);
				listener.onComplete( null ); // 결과 리스너로 전달
			}
	    	
	    });
	}
	
	
	/**
	 * 검색 창
	 */
	public void showFindPopup()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setTitle("우편번호 검색");

		final EditText input = new EditText(mContext);
		input.setHint("주소입력 ex)을지로");
		builder.setView(input);

		builder.setPositiveButton("확인", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
//				removeMarker();
				
				// 우편번호 검색 요청
				findPostCode(input.getText().toString(), new OnFindPostCodeListener(){

					@Override
					public void onComplete(ArrayList<PostCodeVO> alPostCodeVO) {
						// TODO Auto-generated method stub
						if (alPostCodeVO != null && alPostCodeVO.size() > 0)
						{					
							// 검색 결과 창 호출
							new PCodeResultDialog(mContext, alPostCodeVO).show();
						}
						else {
							Toast.makeText(mContext, "검색 결과가 없습니다", Toast.LENGTH_SHORT).show();
						}
					}
					
				});
			}
		});
		builder.setNegativeButton("취소", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.cancel();
			}
		});

		builder.show();
	}
}
