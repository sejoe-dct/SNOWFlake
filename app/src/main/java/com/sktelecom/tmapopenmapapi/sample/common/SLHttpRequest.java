package com.sktelecom.tmapopenmapapi.sample.common;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

import android.os.AsyncTask;

/**
 * Created by 이은성 on 2017-02-01.
 */

public class SLHttpRequest {
    public enum RequestType { GET, POST, MULTIPART };
    private static final String LINE_FEED = "\r\n";

    private String boundary;

    private RequestType requestType = RequestType.GET;
    private String charSet = "UTF-8";
    private String urlString;
    private HashMap<String, Object> parameters;

	protected boolean isSuccess;
	protected StringBuffer resultData;
	protected OnResponseListener responseListener;

    public SLHttpRequest(String url) {
        this.urlString = url;
        this.parameters = new HashMap<String, Object>();
    }

	/**
     * 요청 전송
     */
    public void send(OnResponseListener onResponseListener) {
	    this.responseListener = onResponseListener;
	    new HttpRequestTask().execute();
    }

	/**
	 * 실제 요청 전송
	 */
	protected void doSend() {
		this.isSuccess = true;
		this.resultData = new StringBuffer();

		if (this.requestType == RequestType.GET) {
			URLConnection urlConnection = null;
			Scanner scanner = null;

			try {
				URL url = new URL(this.urlString + makeParamString(true));
				urlConnection = url.openConnection();

				urlConnection.setUseCaches(false);

				scanner = new Scanner(urlConnection.getInputStream());
				while (scanner.hasNext()) {
					String str = scanner.nextLine();
					resultData.append(str);
				}

				scanner.close();
			}
			catch (Exception e) {
				e.printStackTrace();
				this.isSuccess = false;
			}
		}
		else if (this.requestType == RequestType.POST) {
			URLConnection urlConnection = null;
			Scanner scanner = null;

			try {
				URL url = new URL(this.urlString);
				urlConnection = url.openConnection();

				urlConnection.setDoOutput(true);
				urlConnection.setUseCaches(false);
				urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

				DataOutputStream dataOutputStream = null;

				try {
					dataOutputStream = new DataOutputStream(urlConnection.getOutputStream());
					dataOutputStream.writeBytes(makeParamString(false));
					dataOutputStream.flush();
				}
				finally {
					if (dataOutputStream != null) {
						dataOutputStream.close();
					}
				}

				InputStream inputStream = urlConnection.getInputStream();
				scanner = new Scanner(inputStream);

				while (scanner.hasNext()) {
					String str = scanner.nextLine();
					resultData.append(str);
				}

				scanner.close();
			}
			catch (Exception e) {
				e.printStackTrace();
				this.isSuccess = false;
			}
		}
		else if (this.requestType == RequestType.MULTIPART) {
			HttpURLConnection httpConn = null;
			PrintWriter printWriter = null;

			try {
				this.boundary = "===" + System.currentTimeMillis() + "===";
				URL url = new URL(this.urlString);
				httpConn = (HttpURLConnection) url.openConnection();
				httpConn.setUseCaches(false);
				httpConn.setDoOutput(true);
				httpConn.setDoInput(true);

				httpConn.setRequestMethod("POST");
				httpConn.setRequestProperty("Connection", "Keep-Alive");
				httpConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + this.boundary);

				OutputStream outputStream = httpConn.getOutputStream();
				printWriter = new PrintWriter(new OutputStreamWriter(outputStream, charSet), true);

				// 파라미터
				Iterator<String> iterator = this.parameters.keySet().iterator();
				while (iterator.hasNext()) {
					String key = iterator.next();
					Object value = this.parameters.get(key);

					if (value != null && !value.equals("null")) {
						String className = value.getClass().getSimpleName();
						if (className.equals("Integer") || className.equals("Float") || className.equals("Double") || className.equals("String")) {
							// 파라미터가 숫자일 경우
							// 파라미터가 문자열일 경우
							printWriter.append(this.LINE_FEED).append("--" + this.boundary).append(this.LINE_FEED);
							printWriter.append("Content-Disposition: form-data; name=\"" + key + "\"").append(this.LINE_FEED);
							printWriter.append("Content-Type: text/plain; charset=" + this.charSet).append(this.LINE_FEED);
							printWriter.append(LINE_FEED);
							printWriter.append(value.toString().trim());
							printWriter.flush();
						} else if (className.equals("FileParam")) {
							// 파라미터가 파일일 경우
							FileParam fileParam = (FileParam) value;
							printWriter.append(this.LINE_FEED).append("--" + this.boundary).append(this.LINE_FEED);
							printWriter.append("Content-Disposition: form-data; name=\"" + key + "\"; filename=\"" + fileParam.filename + "\"").append(this.LINE_FEED);
							printWriter.append("Content-Type: " + fileParam.fileType).append(LINE_FEED);
							printWriter.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
							printWriter.append(LINE_FEED);
							printWriter.flush();

							outputStream.write(fileParam.data);
							outputStream.flush();
						}
					}
				}

				printWriter.append(this.LINE_FEED).flush();
				printWriter.append("--" + this.boundary + "--").append(this.LINE_FEED);
				printWriter.flush();
				printWriter.close();

				// 응답 처리
				int status = httpConn.getResponseCode();
				if (status == HttpURLConnection.HTTP_OK) {
					BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
					String line = null;
					while ((line = bufferedReader.readLine()) != null) {
						resultData.append(line);
					}

					bufferedReader.close();
				}

				httpConn.disconnect();
			}
			catch (Exception e) {
				e.printStackTrace();
				this.isSuccess = false;
			}
		}
	}

	/**
	 * 응답 처리
	 */
	protected void doResponse() {
		if (this.responseListener != null) {
			if (isSuccess) {
				this.responseListener.OnSuccess(this.resultData.toString());
			}
			else {
				this.responseListener.OnFail(0, "");
			}
		}
	}

	/**
	 * 요청 타입 설정
	 * @param requestType
	 */
	public void setRequestType(RequestType requestType) {
		this.requestType = requestType;
	}

	/**
     * 요청 파라미터 한 개 항목 추가
	 * 처리가능한 클래스 타입은 Integer,Double,Float,String
	 * 파일의 경우 FileParam클래스를 반드시 사용
     * @param name
     * @param value
     */
    public void addParameter(String name, Object value) {
        this.parameters.put(name, value);
    }

	/**
     * 요청 파라미터 묶음 추가
     * @param params
     */
    public void addParameters(HashMap<String, Object> params) {
        this.parameters.putAll(params);
    }

	/**
	 * 파라미터에 파일 추가시 사용
	 */
	public static class FileParam {
		public String filename;
		public String fileType; // 파일 타입 URLConnection.guessContentTypeFromName(fileName) 이걸로 되는듯
		public byte[] data; // 바이너리 데이터
	}

	/**
	 * 파라미터 문자열 생성 (?name=value&...)
	 * @param urlEncode 인코딩 여부
	 * @return
	 */
	protected String makeParamString(boolean urlEncode) {
		StringBuffer stringBuffer = new StringBuffer();

		Iterator<String> iterator = this.parameters.keySet().iterator();
		while (iterator.hasNext()) {
			String key = iterator.next();
			Object value = this.parameters.get(key);

			if (stringBuffer.length() == 0)
				stringBuffer.append("?");
			else
				stringBuffer.append("&");

			if (urlEncode) {
				try {
					String encodeString = URLEncoder.encode(value.toString(), this.charSet);
					stringBuffer.append(key + "=" + encodeString);
				}
				catch (Exception e) {

				}
			}
			else {
				stringBuffer.append(key + "=" + value);
			}
		}

		return stringBuffer.toString();
	}

	public interface OnResponseListener {
		void OnSuccess(String data);
		void OnFail(int errorCode, String errorMessage);
	}

	protected class HttpRequestTask extends AsyncTask<String, String, String> {
		@Override
		protected String doInBackground(String... params) {
			doSend();
			return null;
		}

		@Override
		protected void onPostExecute(String s) {
			super.onPostExecute(s);
			doResponse();
		}
	}
}
