package com.sktelecom.tmapopenmapapi.sample;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.skt.Tmap.TMapAutoCompleteV2;
import com.skt.Tmap.TMapCircle;
import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapData.AutoCompleteListenerCallback;
import com.skt.Tmap.TMapData.ConvertGPSToAddressListenerCallback;
import com.skt.Tmap.TMapData.FindAllPOIListenerCallback;
import com.skt.Tmap.TMapData.FindAroundNamePOIListenerCallback;
import com.skt.Tmap.TMapData.FindPathDataAllListenerCallback;
import com.skt.Tmap.TMapData.OnResponseCodeInfoCallback;
import com.skt.Tmap.TMapData.TMapPathType;
import com.skt.Tmap.TMapData.reverseGeocodingListenerCallback;
import com.skt.Tmap.TMapGpsManager;
import com.skt.Tmap.TMapGpsManager.onLocationChangedCallback;
import com.skt.Tmap.TMapInfo;
import com.skt.Tmap.TMapLabelInfo;
import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapPolygon;
import com.skt.Tmap.TMapTapi;
import com.skt.Tmap.TMapView;
import com.skt.Tmap.address_info.TMapAddressInfo;
import com.skt.Tmap.poi_item.TMapPOIItem;
import com.sktelecom.tmapopenmapapi.sample.adapter.AutoCompleteV2Adapter;
import com.sktelecom.tmapopenmapapi.sample.adapter.BaseExpandableAdapter;
import com.sktelecom.tmapopenmapapi.sample.anim.CloseAnimation;
import com.sktelecom.tmapopenmapapi.sample.anim.OpenAnimation;
import com.sktelecom.tmapopenmapapi.sample.common.Common;
import com.sktelecom.tmapopenmapapi.sample.common.LogManager;
import com.sktelecom.tmapopenmapapi.sample.common.PermissionManager;
import com.sktelecom.tmapopenmapapi.sample.common.SLHttpRequest;
import com.sktelecom.tmapopenmapapi.sample.geofence.GeofenceData;
import com.sktelecom.tmapopenmapapi.sample.geofence.Geofencer;
import com.sktelecom.tmapopenmapapi.sample.geofence.Geofencer.OnGeofencingBaseDataReceivedCallback;
import com.sktelecom.tmapopenmapapi.sample.geofence.Geofencer.OnGeofencingPolygonCreatedCallback;
import com.sktelecom.tmapopenmapapi.sample.marker.MarkerOverlay;
import com.sktelecom.tmapopenmapapi.sample.postcode.PostCode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends Activity implements OnClickListener, onLocationChangedCallback, OnGeofencingPolygonCreatedCallback {
    @Override
    public void onLocationChange(Location location) {
        LogManager.printLog("onLocationChange " + location.getLatitude() + " " + location.getLongitude() + " " + location.getSpeed() + " " + location.getAccuracy());

        if (m_bTrackingMode) {
            mMapView.setLocationPoint(location.getLongitude(), location.getLatitude());
            mMapView.setIconVisibility(m_bTrackingMode);
//			Toast.makeText(mContext, "위성수 :" + gps.getSatellite(), Toast.LENGTH_SHORT).show();
        }
    }

    /* slide menu */
    private DisplayMetrics metrics;
    private LinearLayout ll_menuLayout;
    private FrameLayout.LayoutParams leftMenuLayoutPrams;
    private int leftMenuWidth;
    private static boolean isLeftExpanded;
    private Button bt_api;
    private TextView textZoomLevel;
    private ImageView locationBtn;
    private LinearLayout geocodingLayout;
    private LinearLayout routeLayout;
    private TextView geocodingTitle;
    private TextView geocodingSub;
    private TextView routeDistance;
    private TextView routeTime;
    private TextView routeFare;

    private LinearLayout autoCompleteLayout;

    //16.09.26 KMY [ 화면중심좌표 메뉴 수정 ]
    private LinearLayout centerPointLayout;
    private TextView centerLongitude;
    private TextView centerLatitude;

    private ImageView centerPointIcon;

    public static final int MESSAGE_STATE_ZOOM = 1;
    public static final int MESSAGE_STATE_GEOCODING = 2;
    public static final int MESSAGE_STATE_POI = 3;
    public static final int MESSAGE_STATE_POI_DETAIL = 4;
    public static final int MESSAGE_STATE_ROUTE = 5;
    public static final int MESSAGE_STATE_APIKEY = 6;
    public static final int MESSAGE_ERROR = 7;
    public static final int MESSAGE_STATE_POI_AUTO = 8;
    public static final int MESSAGE_STATE_POI_AUTO_V2 = 9;

    private TMapView mMapView = null;

    private Context mContext;
    private static final String mApiKey = "l7xx4b5dd32e30084b73b34f5cb9dea05844";

    private int m_nCurrentZoomLevel = 0;
    private int m_nCurrentMapType = 0;

    // geofencing type save
    private int m_nCurrentGeofencingType = 0;

    private boolean m_bSightVisible = false;
    private boolean m_bTrackingMode = false;
    private boolean m_bReverseLabel = false;
    private boolean m_bReverseGeoCoding = false;
    private boolean m_bTrafficMode = false;
    //16.09.26 KMY [ 화면중심좌표 메뉴 수정 ]
    private boolean m_bCenterPointMode = false;

    ArrayList<String> mArrayID;

    ArrayList<String> mArrayCircleID;
    private static int mCircleID;

    ArrayList<String> mArrayLineID;
    private static int mLineID;

    ArrayList<String> mArrayPolygonID;
    private static int mPolygonID;

    ArrayList<String> mArrayMarkerID;
    private static int mMarkerID;

    ArrayList<String> mArrayGeofenchingID;

    TMapGpsManager gps = null;
    PermissionManager mPermissionManager = null;

    private ExpandableListView mListView;
    private ArrayList<String> mGroupList = null;
    private ArrayList<ArrayList<String>> mChildList = null;

    private String totalDistance = null;
    private String totalTime = null;
    private String totalFare = null;
    private ArrayList<TMapPOIItem> mArrPoiItem = null;
    private TMapPOIItem mPoiItem = null;
    private CharSequence[] items = null;
    private String oldBAddress = null;
    private String newAddress = null;

    private final String reverseLabelID = "ReverseLabel";

    //자동완성 v2
    private LinearLayout autoCompleteV2Layout;
    private EditText autoCompleteV2Edit;
    private ListView autoCompleteV2ListView;
    private AutoCompleteV2Adapter autoCompleteV2Adapter;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mPermissionManager.setResponse(requestCode, grantResults);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mContext = this;

        gps = new TMapGpsManager(MainActivity.this);
        mPermissionManager = new PermissionManager();

        RelativeLayout mMainRelativeLayout = (RelativeLayout) findViewById(R.id.mapview_layout);
        mMapView = new TMapView(this);

        /* 20181123 : 화면사이즈 3분의 2만 차게 만들기 */
        //*********************************************************************************
//		DisplayMetrics dm = getApplicationContext().getResources().getDisplayMetrics();
//		int newWidthInPx = dm.widthPixels;
//		int newHeightInPx = (int)(dm.heightPixels * 0.66);
//		ViewGroup.LayoutParams params = mMainRelativeLayout.getLayoutParams();
//		params.width = newWidthInPx;
//		params.height = newHeightInPx;
//		mMainRelativeLayout.setLayoutParams(params);
        //*********************************************************************************
        mMainRelativeLayout.addView(mMapView);

        mMapView.setZoomLevel(16);
        mMapView.setBufferStep(3);
//		mMapView.setScrollAnimation(0);

        //mMapView.setCenterPoint(126.985003, 37.566413);
        //mMapView.setLocationPoint(mMapView.getLongitude(), mMapView.getLatitude());

        mMapView.setSKTMapApiKey(mApiKey);


        initSildeMenu();
        setMenuItem();

        m_nCurrentZoomLevel = -1;
        m_nCurrentMapType = 2;
        m_bSightVisible = false;

        totalDistance = null;
        totalTime = null;
        totalFare = null;
        mArrPoiItem = null;
        mPoiItem = null;
        items = null;
        oldBAddress = null;
        newAddress = null;

        mArrayID = new ArrayList<String>();

        mArrayCircleID = new ArrayList<String>();
        mCircleID = 0;

        mArrayLineID = new ArrayList<String>();
        mLineID = 0;
        mArrayPolygonID = new ArrayList<String>();

        mPolygonID = 0;

        mArrayMarkerID = new ArrayList<String>();
        mMarkerID = 0;

        mArrayGeofenchingID = new ArrayList<String>();

        //Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.point);
        //mMapView.setIcon(bitmap);

        m_bTrackingMode = false;
        setTrackingMode(false);
        reverseLabel(false);

        //mMapView.setScrollAnimation(TMapView.SCROLL_SENSITIVITY_ZORO);
    }

    public void onClickZoomInBtn(View v) {
        mapZoomIn();
    }

    public void onClickZoomOutBtn(View v) {
        mapZoomOut();
    }

    private void setMenuItem() {
        mGroupList = new ArrayList<String>();
        mChildList = new ArrayList<ArrayList<String>>();
        ArrayList<String> mChildListContent0 = new ArrayList<String>();
        ArrayList<String> mChildListContent1 = new ArrayList<String>();
        ArrayList<String> mChildListContent2 = new ArrayList<String>();
        ArrayList<String> mChildListContent3 = new ArrayList<String>();
        ArrayList<String> mChildListContent4 = new ArrayList<String>();
        ArrayList<String> mChildListContent5 = new ArrayList<String>();
        ArrayList<String> mChildListContent7 = new ArrayList<String>();

        mGroupList.add("초기화");
        mGroupList.add("지도컨트롤");
        mGroupList.add("POI");
        mGroupList.add("Geocoding");
        mGroupList.add("위치트래킹");
        mGroupList.add("경로안내");
        mGroupList.add("Reverse Label");
        mGroupList.add("T map연동");
        mGroupList.add("Geofencing");
        mGroupList.add("App 정보");
        mGroupList.add("교통정보");
        mGroupList.add("https테스트");

        mChildListContent1.add("줌 레벨 선택");
        mChildListContent1.add("화면 중심좌표");
        mChildListContent1.add("지도 타입 선택");
        mChildListContent1.add("서클(원)");
        mChildListContent1.add("직선");
        mChildListContent1.add("폴리곤");
        mChildListContent1.add("지도 회전");
        mChildListContent1.add("마커 회전");


        mChildListContent2.add("POI 통합검색");
        mChildListContent2.add("주변POI 검색");
        mChildListContent2.add("읍면동/도로명 조회");
        mChildListContent2.add("POI 자동완성");
        mChildListContent2.add("POI 초기화");
        mChildListContent2.add("POI 자동완성V2");

        mChildListContent3.add("Reverse Geocoding");
        mChildListContent3.add("Full Text Geocoding");
        mChildListContent3.add("우편번호 검색");

        mChildListContent4.add("트래킹모드");
        mChildListContent4.add("나침반모드 설정");
        mChildListContent4.add("시야표출여부");
        mChildListContent4.add("나침반모드 고정");

        mChildListContent5.add("자동차 경로");

        mChildListContent5.add("보행자 경로");
        mChildListContent5.add("경로정보 전체삭제");

        mChildListContent7.add("T map 실행하기");
        mChildListContent7.add("T map 길안내바로실행");
        mChildListContent7.add("T map 통합검색");
        mChildListContent7.add("T map 설치");

        mChildList.add(mChildListContent0);
        mChildList.add(mChildListContent1);
        mChildList.add(mChildListContent2);
        mChildList.add(mChildListContent3);
        mChildList.add(mChildListContent4);
        mChildList.add(mChildListContent5);
        mChildList.add(mChildListContent0);
        mChildList.add(mChildListContent7);
        mChildList.add(mChildListContent0);
        mChildList.add(mChildListContent0);
        mChildList.add(mChildListContent0);
        mChildList.add(mChildListContent0);

        mListView.setAdapter(new BaseExpandableAdapter(this, mGroupList, mChildList));

        mListView.setOnGroupClickListener(new OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                clickMenuItem(groupPosition, -1);
                if (groupPosition == 0 || groupPosition == 6 || groupPosition == 8 || groupPosition == 9) {
                    menuLeftSlideAnimationToggle();
                }
                return false;
            }
        });

        mListView.setOnChildClickListener(new OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                clickMenuItem(groupPosition, childPosition);
                menuLeftSlideAnimationToggle();
                return false;
            }
        });
    }

    private void clickMenuItem(int groupPosition, int childPosition) {
        switch (groupPosition) {
            case 0: // 초기화
                initMap();
                break;
            case 1: // 지도컨트롤
                switch (childPosition) {
                    case 0: // 줌레벨선택
                        setZoomLevel();
                        break;
                    case 1: // 화면중심좌표
                        //16.09.26 KMY [ 화면중심좌표 메뉴 수정 ]
                        Common.setOnClickSelectListenerCallBack(this, "화면중심좌표 표출 여부", R.array.a_select1, new Common.OnClickSelectListenerCallback() {
                            @Override
                            public boolean onSelectEvent(int item) {
                                if (item == 0) {
                                    m_bCenterPointMode = true;
                                } else {
                                    m_bCenterPointMode = false;
                                }
                                getCenterPoint();
                                return false;
                            }
                        });
                        break;
                    case 2: // 지도 타입 선택
                        setTileType();
                        break;
                    case 3: // 서클
                        Common.setOnClickSelectListenerCallBack(this, "서클(원) 그리기", R.array.a_select2, new Common.OnClickSelectListenerCallback() {
                            @Override
                            public boolean onSelectEvent(int item) {
                                if (item == 0) {
                                    addTMapCircle();
                                } else {
                                    removeTMapCircle();
                                }
                                return false;
                            }
                        });
                        break;
                    case 4: // 직선
                        Common.setOnClickSelectListenerCallBack(this, "직선 그리기", R.array.a_select2, new Common.OnClickSelectListenerCallback() {
                            @Override
                            public boolean onSelectEvent(int item) {
                                if (item == 0) {
                                    drawLine();
                                } else {
                                    erasePolyLine();
                                }
                                return false;
                            }
                        });
                        break;
                    case 5: // 폴리곤
                        Common.setOnClickSelectListenerCallBack(this, "폴리곤 그리기", R.array.a_select2, new Common.OnClickSelectListenerCallback() {
                            @Override
                            public boolean onSelectEvent(int item) {
                                if (item == 0) {
                                    drawPolygon();
                                } else {
                                    removeTMapPolygon();
                                }
                                return false;
                            }
                        });
                        break;

                    case 6: // 지도 회전
                        Common.setOnClickSelectListenerCallBack(this, "지도 회전", R.array.a_select3, new Common.OnClickSelectListenerCallback() {
                            @Override
                            public boolean onSelectEvent(int item) {
                                if (item == 0) {
                                    mMapView.setRotateEnable(true);
                                } else {
                                    mMapView.setRotateEnable(false);
                                    mMapView.setInitRotate();
                                }
                                return false;
                            }
                        });
                        break;
                    case 7: // 지도 회전
                        Common.setOnClickSelectListenerCallBack(this, "마커 회전", R.array.a_select3, new Common.OnClickSelectListenerCallback() {
                            @Override
                            public boolean onSelectEvent(int item) {
                                if (item == 0) {
                                    mMapView.setPOIRotate(false);
                                    //mMapView.setrotate
                                } else {
                                    mMapView.setMarkerRotate(true);
                                }
                                return false;
                            }
                        });
                        break;

                    default:
                        break;
                }

                break;
            case 2: // poi
                switch (childPosition) {
                    case 0: // POI 통합검색
                        findAllPoi();
                        break;
                    case 1: // 주변 POI 검색
                        getAroundBizPoi();
                        break;
                    case 2: // 읍면동/도로명 조회
                        getPoiAreaDataByName();
                        break;
                    case 3: // POI 자동완성
                        autoComplete();
                        break;
                    case 4: // POI 초기화
                        removeMarker();
                        break;
                    case 5: // 자동완성 v2
                        autocompleteV2();
                        break;
                    default:
                        break;
                }
                break;
            case 3: // geocoding
                switch (childPosition) {
                    case 0: // reverse geocoding
                        Common.setOnClickSelectListenerCallBack(this, "Reverse Geocoding", R.array.a_select3, new Common.OnClickSelectListenerCallback() {
                            @Override
                            public boolean onSelectEvent(int item) {
                                if (item == 0) {
                                    reverseGeocoding(true);
                                } else {
                                    reverseGeocoding(false);
                                }
                                return false;
                            }
                        });
                        break;
                    case 1: // Full Text Geocoding
                        fullTextGeocoding();
                        break;
                    case 2: // 우편번호 검색
                        new PostCode(this, mApiKey).showFindPopup();
                        break;
                }
                break;
            case 4: // 위치트래킹
                switch (childPosition) {
                    case 0: // 트래킹 모드
                        Common.setOnClickSelectListenerCallBack(this, "트래킹 모드", R.array.a_select3, new Common.OnClickSelectListenerCallback() {
                            @Override
                            public boolean onSelectEvent(int item) {
                                if (item == 0) {
                                    m_bTrackingMode = true;
                                } else {
                                    m_bTrackingMode = false;
                                }
                                setTrackingMode(m_bTrackingMode);
                                return false;
                            }
                        });
                        break;
                    case 1: // 나침반모드
                        Common.setOnClickSelectListenerCallBack(this, "나침반 모드", R.array.a_select3, new Common.OnClickSelectListenerCallback() {
                            @Override
                            public boolean onSelectEvent(int item) {
                                if (item == 0) {
                                    mMapView.setCompassMode(true);
                                } else {
                                    mMapView.setCompassMode(false);
                                }
                                return false;
                            }
                        });
                        break;
                    case 2: // 시야표출여부
                        Common.setOnClickSelectListenerCallBack(this, "시야표출여부", R.array.a_select3, new Common.OnClickSelectListenerCallback() {
                            @Override
                            public boolean onSelectEvent(int item) {
                                if (item == 0) {
                                    mMapView.setSightVisible(true);
                                } else {
                                    mMapView.setSightVisible(false);
                                }
                                return false;
                            }
                        });
                        break;
                    case 3:
                        Common.setOnClickSelectListenerCallBack(this, "나침반모드고정", R.array.a_select3, new Common.OnClickSelectListenerCallback() {
                            @Override
                            public boolean onSelectEvent(int item) {
                                if (item == 0) {
                                    mMapView.setCompassModeFix(true);
                                } else {
                                    mMapView.setCompassModeFix(false);
                                }
                                return false;
                            }
                        });
                        break;
                    default:
                        break;
                }
                break;
            case 5: // 경로안내
                switch (childPosition) {
                    case 0: // 자동차 경로
                        drawCarPath();
                        break;
                    case 1: // 보행자 경로
                        drawPedestrianPath();
                        break;
                    case 2: // 경로정보전체 삭제
                        removeMapPath();
                        break;
                    default:
                        break;
                }

                break;
            case 6: // reverse label
                Common.setOnClickSelectListenerCallBack(this, "Reverse label", R.array.a_select3, new Common.OnClickSelectListenerCallback() {
                    @Override
                    public boolean onSelectEvent(int item) {
                        if (item == 0) {
                            reverseLabel(true);
                        } else {
                            reverseLabel(false);
                        }
                        return false;
                    }
                });
                break;
            case 7: // tmap 연동
                switch (childPosition) {
                    case 0: // Tmap 실행하기
                        //invokeSetLocation();
                        invokeTmap();
                        break;
                    case 1: // Tmap 길안내 바로가기
                        invokeRoute();
                        break;
                    case 2: // Tmap 통합검색
                        invokeSearchProtal();
                        break;
                    case 3: // Tmap 설치
                        checkTmapApplicationInstalled(); // 티맵 설치여부 확인
                        tmapInstall(); // 티맵 설치 페이지로 이동
                        break;

                    default:
                        break;
                }

                break;
            case 8: // Geofencing
                // 구현
                geofencing();
                break;

            case 9: // App정보
                Common.showAlertDialog(this, "App 정보", getString(R.string.appInfo));
                break;

            case 10: // 교통
                Common.setOnClickSelectListenerCallBack(this, "교통정보", R.array.a_select3, new Common.OnClickSelectListenerCallback() {
                    @Override
                    public boolean onSelectEvent(int item) {
                        if (item == 0) {
                            m_bTrafficMode = true;
                        } else {
                            m_bTrafficMode = false;
                        }
                        mMapView.setTrafficInfoActive(m_bTrafficMode);
                        return false;
                    }
                });
                break;
            case 11: // https 테스트
                testHttps();
                break;

            default:
                break;
        }
    }


    private void autocompleteV2() {
        autoCompleteV2Layout.setVisibility(View.VISIBLE);
    }

    private void testHttps() {
        mMapView.testHttps();
    }

    private void initMap() {
        mMapView.setZoomLevel(16);
        m_bTrackingMode = false;
        setTrackingMode(false);
        mMapView.setTileType(mMapView.TILETYPE_HDTILE);
        removeTMapCircle();
        erasePolyLine();
        // removeTMapPolygon();

        // 지오펜싱 폴리곤 제거
        mMapView.removeAllTMapPolygon();
        // mMapView.addTMapPolygon("POLYGON_GEOFENCE", polygon);

        removeMarker();
        reverseGeocoding(false);

        mMapView.setCompassMode(false);
        mMapView.setSightVisible(false);

        removeMapPath();

        mMapView.removeMarkerItem2(reverseLabelID);
        reverseLabel(false);

        m_nCurrentZoomLevel = -1;
        m_nCurrentMapType = 2;
        m_bSightVisible = false;

        totalDistance = null;
        totalTime = null;
        totalFare = null;
        mArrPoiItem = null;
        mPoiItem = null;
        items = null;
        oldBAddress = null;
        newAddress = null;
    }

    private void initSildeMenu() {
        metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        leftMenuWidth = (int) ((metrics.widthPixels) * 0.5);

        mListView = (ExpandableListView) findViewById(R.id.elv_list);

        ll_menuLayout = (LinearLayout) findViewById(R.id.ll_menuLayout);
        leftMenuLayoutPrams = (FrameLayout.LayoutParams) ll_menuLayout.getLayoutParams();
        leftMenuLayoutPrams.width = leftMenuWidth;
        leftMenuLayoutPrams.leftMargin = -leftMenuWidth;
        ll_menuLayout.setLayoutParams(leftMenuLayoutPrams);

        // init ui
        bt_api = (Button) findViewById(R.id.bt_api);
        bt_api.setOnClickListener(this);

        textZoomLevel = (TextView) findViewById(R.id.zoomlevel_text);
        locationBtn = (ImageView) findViewById(R.id.location_btn);
        geocodingLayout = (LinearLayout) findViewById(R.id.geocoding_layout);
        geocodingTitle = (TextView) findViewById(R.id.geocoding_title);
        geocodingSub = (TextView) findViewById(R.id.geocoding_sub);
        routeLayout = (LinearLayout) findViewById(R.id.route_layout);
        routeDistance = (TextView) findViewById(R.id.route_distance);
        routeTime = (TextView) findViewById(R.id.route_time);
        routeFare = (TextView) findViewById(R.id.route_fare);

        //16.09.26 KMY [ 화면중심좌표 메뉴 수정 ]
        centerPointLayout = (LinearLayout) findViewById(R.id.centerPointLayout);
        centerLongitude = (TextView) findViewById(R.id.centerLongitude);
        centerLatitude = (TextView) findViewById(R.id.centerLatitude);
        centerPointIcon = (ImageView) findViewById(R.id.centerPointIcon);

        mMapView.setOnApiKeyListener(new TMapView.OnApiKeyListenerCallback() {
            @Override
            public void SKTMapApikeySucceed() {
                setTextLevel(MESSAGE_STATE_ZOOM);
            }

            @Override
            public void SKTMapApikeyFailed(String errorMsg) {
                setTextLevel(MESSAGE_STATE_APIKEY);
            }
        });

        mMapView.setOnDisableScrollWithZoomLevelListener(new TMapView.OnDisableScrollWithZoomLevelCallback() {
            @Override
            public void onDisableScrollWithZoomLevelEvent(float zoom, TMapPoint centerPoint) {
                setTextLevel(MESSAGE_STATE_ZOOM);
                if (m_bReverseGeoCoding) {
                    reverseGeocoding(m_bReverseGeoCoding);
                }
            }
        });

        mMapView.setOnClickListenerCallBack(new TMapView.OnClickListenerCallback() {
            @Override
            public boolean onPressUpEvent(ArrayList<TMapMarkerItem> markerlist, ArrayList<TMapPOIItem> poilist, TMapPoint point, PointF pointf) {
                return false;
            }

            @Override
            public boolean onPressEvent(ArrayList<TMapMarkerItem> markerlist, ArrayList<TMapPOIItem> poilist, TMapPoint point, PointF pointf) {
                for (int i = 0; i < markerlist.size(); i++) {
                    if (!markerlist.get(0).getCanShowCallout()) {
                        if (mPoiItem != null) {
                            findPOIDetailInfo(mPoiItem.getPOIID(), mPoiItem.getPOIName());
                        }
                    }
                }

                if (m_bTrackingMode) {
                    m_bTrackingMode = false;
                    setTrackingMode(m_bTrackingMode);
                }
                return false;
            }
        });

        mMapView.setOnCalloutRightButtonClickListener(new TMapView.OnCalloutRightButtonClickCallback() {
            @Override
            public void onCalloutRightButton(TMapMarkerItem markerItem) {
                String strMessage = "";
                strMessage = "ID: " + markerItem.getID() + " " + "Title " + markerItem.getCalloutTitle();
                Common.showAlertDialog(MainActivity.this, "Callout Right Button", strMessage);
            }
        });

        autoCompleteLayout = (LinearLayout) findViewById(R.id.autoComplete_layout);
        Adapter = new ArrayAdapter<String>(this, R.layout.list_layout, arDessert);

        list = (ListView) findViewById(R.id.list);

        list.setAdapter(Adapter);
        list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        list.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                findAllPoi(arDessert.get(position));
            }
        });

        editText = (EditText) findViewById(R.id.edit_text);
        editText.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                final String strData = s.toString();
                TMapData tmapdata = new TMapData();
                tmapdata.autoComplete(strData, new AutoCompleteListenerCallback() {
                    @Override
                    public void onAutoComplete(ArrayList<String> poiItem) {
                        if (poiItem == null && poiItem.size() < 0) {
                            setTextLevel(MESSAGE_ERROR);
                            return;
                        }
                        arDessert.clear();

                        for (int i = 0; i < poiItem.size(); i++) {
                            arDessert.add(poiItem.get(i));
                        }
                        setTextLevel(MESSAGE_STATE_POI_AUTO);
                    }
                });
            }
        });

        TMapData mapData = new TMapData();
        mapData.setResponseCodeInfoCallBack(new OnResponseCodeInfoCallback() {

            @Override
            public void responseCodeInfo(String apiName, int resCode, String url) {
//				Log.d("test", ">>>>>>>>>>>apiName = " + apiName + " / " + resCode + " / " + url);
            }
        });


        //자동완성v2
        autoCompleteV2Layout = findViewById(R.id.autoCompleteV2Layout);
        autoCompleteV2Layout.setVisibility(View.GONE);
        autoCompleteV2Edit = findViewById(R.id.autoCompleteV2Edit);
        autoCompleteV2ListView = findViewById(R.id.autoCompleteV2ListView);
        autoCompleteV2Adapter = new AutoCompleteV2Adapter(this);
        autoCompleteV2ListView.setAdapter(autoCompleteV2Adapter);

        autoCompleteV2Edit.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                String keyword = s.toString();
                TMapData tMapData = new TMapData();
                double centerLat = mMapView.getCenterPoint().getLatitude();
                double centerLon = mMapView.getCenterPoint().getLongitude();
                tMapData.autoCompleteV2(keyword, centerLat, centerLon, 0, 100, new TMapData.AutoCompleteCallbackV2() {
                    @Override
                    public void onAutoComplete(ArrayList<TMapAutoCompleteV2> arrayList) {
                        if (arrayList != null) {
                            autoCompleteV2Adapter.setItemList(arrayList);
                            setTextLevel(MESSAGE_STATE_POI_AUTO_V2);
                        }
                    }
                });

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        autoCompleteV2ListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TMapAutoCompleteV2 item = (TMapAutoCompleteV2) autoCompleteV2Adapter.getItem(position);

                StringBuilder strBuilder = new StringBuilder();
                strBuilder.append("POI : ").append(item.keyword).append("\n")
                        .append("주소 : ").append(item.fullAddress).append("\n")
                        .append("지번 : ").append(item.fullAddressJibun).append("\n")
                        .append("poi id : ").append(item.poiId).append("\n")
                        .append("pkey : ").append(item.pKey).append("\n")
                        .append("위도 : ").append(item.lat).append("\n")
                        .append("경도 : ").append(item.lon);

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("POI정보")
                        .setMessage(strBuilder.toString())
                        .setPositiveButton("확인", null)
                        .create()
                        .show();

            }
        });
    }

    public void hideKeyBoard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(this.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    ArrayList<String> arDessert = new ArrayList<String>();
    ArrayAdapter<String> Adapter;
    ListView list;
    EditText editText;

    private String name = null;
    private String address = null;

    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_ZOOM:
                    textZoomLevel.setText("Lv." + mMapView.getZoomLevel());
                    m_nCurrentZoomLevel = mMapView.getZoomLevel() - 3;
                    break;
                case MESSAGE_STATE_GEOCODING:
                    if (name != null) {
                        geocodingLayout.setVisibility(View.VISIBLE);
                        routeLayout.setVisibility(View.GONE);

                        geocodingTitle.setText(name);
                        geocodingSub.setText(address);
                    } else {
                        //16.09.26 KMY [ 화면중심좌표 메뉴 수정 ]
                        getCenterPoint();
                        geocodingLayout.setVisibility(View.GONE);
                        routeLayout.setVisibility(View.GONE);

                    }
                    break;
                case MESSAGE_STATE_POI:
                    if (autoCompleteLayout.getVisibility() == View.VISIBLE) {
                        autoCompleteLayout.setVisibility(View.GONE);
                        arDessert.clear();
                        editText.setText("");
                        hideKeyBoard();
                    }
                    showPOIListAlert();
                    break;
                case MESSAGE_STATE_POI_DETAIL:
                    showPOIDetailAlert();
                    break;
                case MESSAGE_STATE_POI_AUTO:
                    Adapter.notifyDataSetChanged();
                    break;
                case MESSAGE_STATE_POI_AUTO_V2:
                    autoCompleteV2Adapter.notifyDataSetChanged();
                    break;
                case MESSAGE_STATE_ROUTE:
                    routeLayout.setVisibility(View.VISIBLE);
                    geocodingLayout.setVisibility(View.GONE);

                    int totalSec = Integer.parseInt(totalTime);
                    int day = totalSec / (60 * 60 * 24);
                    int hour = (totalSec - day * 60 * 60 * 24) / (60 * 60);
                    int minute = (totalSec - day * 60 * 60 * 24 - hour * 3600) / 60;
                    String time = null;
                    if (hour > 0) {
                        time = hour + "시간 " + minute + "분";
                    } else {
                        time = minute + "분 ";
                    }

                    double km = Double.parseDouble(totalDistance) / 1000;

                    routeDistance.setText("총 거리 : " + km + "km");
                    routeTime.setText("예상 시간 : 약 " + time);
                    if (totalFare != null) {
                        routeFare.setVisibility(View.VISIBLE);
                        routeFare.setText("유료도로 요금 : " + totalFare + "원");
                    } else {
                        routeFare.setVisibility(View.GONE);
                    }
                    break;
                case MESSAGE_STATE_APIKEY:
                    Toast.makeText(getApplicationContext(), "앱키 인증에 실패 했습니다.", Toast.LENGTH_SHORT).show();
                    break;

                case MESSAGE_ERROR:
                    Toast.makeText(getApplicationContext(), "정보가 없습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void setTextLevel(final int what) {
        new Thread() {
            public void run() {
                Message msg = handler.obtainMessage(what);
                handler.sendMessage(msg);
            }
        }.start();
    }

    /**
     * left menu toggle
     */
    private void menuLeftSlideAnimationToggle() {

        if (!isLeftExpanded) {

            isLeftExpanded = true;

            new OpenAnimation(ll_menuLayout, 0, TranslateAnimation.RELATIVE_TO_SELF, 0.0f, TranslateAnimation.RELATIVE_TO_SELF, 1.0f, 0, 0.0f, 0, 0.0f);

            ((LinearLayout) findViewById(R.id.ll_empty)).setVisibility(View.VISIBLE);

            findViewById(R.id.ll_empty).setEnabled(true);
            findViewById(R.id.ll_empty).setOnTouchListener(new OnTouchListener() {

                @Override
                public boolean onTouch(View arg0, MotionEvent arg1) {
                    menuLeftSlideAnimationToggle();
                    return true;
                }
            });

        } else {
            isLeftExpanded = false;

            new CloseAnimation(ll_menuLayout, 0, TranslateAnimation.RELATIVE_TO_SELF, 0.0f, TranslateAnimation.RELATIVE_TO_SELF, -1.0f, 0, 0.0f, 0, 0.0f);

            ((LinearLayout) findViewById(R.id.ll_empty)).setVisibility(View.GONE);
            findViewById(R.id.ll_empty).setEnabled(false);

        }
    }

    public void onClickLocationBtn(View v) {
        m_bTrackingMode = !m_bTrackingMode;
        if (m_bTrackingMode) {
            mMapView.setIconVisibility(m_bTrackingMode);
        }
        setTrackingMode(m_bTrackingMode);
    }

    /**
     * onClick Event
     */
    @Override
    public void onClick(View v) {
        menuLeftSlideAnimationToggle();
    }

    public TMapPoint randomTMapPoint() {
        double latitude = ((double) Math.random()) * (37.575113 - 37.483086) + 37.483086;
        double longitude = ((double) Math.random()) * (127.027359 - 126.878357) + 126.878357;

        latitude = Math.min(37.575113, latitude);
        latitude = Math.max(37.483086, latitude);

        longitude = Math.min(127.027359, longitude);
        longitude = Math.max(126.878357, longitude);

        LogManager.printLog("randomTMapPoint" + latitude + " " + longitude);

        TMapPoint point = new TMapPoint(latitude, longitude);

        return point;
    }

    public TMapPoint randomTMapPoint2() {
        double latitude = ((double) Math.random()) * (37.770555 - 37.404194) + 37.483086;
        double longitude = ((double) Math.random()) * (127.426043 - 126.770296) + 126.878357;

        latitude = Math.min(37.770555, latitude);
        latitude = Math.max(37.404194, latitude);

        longitude = Math.min(127.426043, longitude);
        longitude = Math.max(126.770296, longitude);

        LogManager.printLog("randomTMapPoint" + latitude + " " + longitude);

        TMapPoint point = new TMapPoint(latitude, longitude);

        return point;
    }

    public void reverseLabel(boolean show) {
        address = null;
        name = null;

        if (show && mMapView.getZoomLevel() < 15) {
            mMapView.setZoomLevel(15);
        }

        m_bReverseLabel = show;
        if (m_bReverseLabel) {
            setTextLevel(MESSAGE_STATE_GEOCODING);
            routeLayout.setVisibility(View.GONE);
            mMapView.setOnClickReverseLabelListener(new TMapView.OnClickReverseLabelListenerCallback() {
                @Override
                public void onClickReverseLabelEvent(TMapLabelInfo findReverseLabel) {
                    mMapView.removeMarkerItem2(reverseLabelID);
                    if (findReverseLabel != null) {

                        double lat = Double.valueOf(findReverseLabel.labelLat);
                        double lon = Double.valueOf(findReverseLabel.labelLon);
                        name = findReverseLabel.labelName;

                        MarkerOverlay marker1 = new MarkerOverlay(mContext, name, "ID: " + findReverseLabel.id);

                        marker1.setID(reverseLabelID);
                        marker1.setTMapPoint(new TMapPoint(lat, lon));
                        mMapView.addMarkerItem2(reverseLabelID, marker1);

                        TMapData tmapdata = new TMapData();
                        tmapdata.convertGpsToAddress(lat, lon, new ConvertGPSToAddressListenerCallback() {
                            @Override
                            public void onConvertToGPSToAddress(String strAddress) {
                                address = strAddress;
                                setTextLevel(MESSAGE_STATE_GEOCODING);
                            }
                        });
                    }
                }
            });
        } else {
            mMapView.setOnClickReverseLabelListener(null);
            mMapView.removeMarkerItem2(reverseLabelID);
            geocodingLayout.setVisibility(View.GONE);

        }
    }

    /**
     * mapZoomIn 지도를 한단계 확대한다.
     */
    public void mapZoomIn() {
        mMapView.MapZoomIn();
    }

    /**
     * mapZoomOut 지도를 한단계 축소한다.
     */
    public void mapZoomOut() {
        mMapView.MapZoomOut();
    }

    /**
     * getZoomLevel 현재 줌의 레벨을 가지고 온다.
     */
    public void getZoomLevel() {
        int nCurrentZoomLevel = mMapView.getZoomLevel();
        Common.showAlertDialog(this, "", "현재 Zoom Level : " + Integer.toString(nCurrentZoomLevel));
    }

    /**
     * setZoomLevel Zoom Level을 설정한다.
     */
    public void setZoomLevel() {
        final String[] arrString = getResources().getStringArray(R.array.a_zoomlevel);
        AlertDialog dlg = new AlertDialog.Builder(this).setIcon(R.drawable.tmark).setTitle("Select Zoom Level")
                .setSingleChoiceItems(R.array.a_zoomlevel, m_nCurrentZoomLevel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        dialog.dismiss();
                        mMapView.setZoomLevel(Integer.parseInt(arrString[item]));
                        m_nCurrentZoomLevel = item;
                    }
                }).show();
    }

    /**
     * seetMapType Map의 Type을 설정한다.
     */
    public void setMapType() {
        AlertDialog dlg = new AlertDialog.Builder(this).setIcon(R.drawable.tmark).setTitle("Select MAP Type").setSingleChoiceItems(R.array.a_maptype, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                LogManager.printLog("Set Map Type " + item);
                dialog.dismiss();
                mMapView.setMapType(item);
            }
        }).show();
    }

    /**
     * getLocationPoint 현재위치로 표시될 좌표의 위도, 경도를 반환한다.
     */
    public void getLocationPoint() {
        TMapPoint point = mMapView.getLocationPoint();

        double Latitude = point.getLatitude();
        double Longitude = point.getLongitude();

        LogManager.printLog("Latitude " + Latitude + " Longitude " + Longitude);

        String strResult = String.format("Latitude = %f Longitude = %f", Latitude, Longitude);

        Common.showAlertDialog(this, "", strResult);
    }

    /**
     * setCompassMode 단말의 방항에 따라 움직이는 나침반모드로 설정한다.
     */
    public void setCompassMode() {
        mMapView.setCompassMode(!mMapView.getIsCompass());
    }

    /**
     * getIsCompass 나침반모드의 사용여부를 반환한다.
     */
    public void getIsCompass() {
        Boolean bGetIsCompass = mMapView.getIsCompass();
        Common.showAlertDialog(this, "", "현재 나침반 모드는 : " + bGetIsCompass.toString());
    }

    /**
     * setSightVisible 시야표출여부를 설정한다.
     */
    public void setSightVisible() {
        m_bSightVisible = !m_bSightVisible;
        mMapView.setLocationPoint(mMapView.getCenterPoint().getLongitude(), mMapView.getCenterPoint().getLatitude());
        mMapView.setSightVisible(m_bSightVisible);
    }

    /**
     * setTrackingMode 화면중심을 단말의 현재위치로 이동시켜주는 트래킹모드로 설정한다.
     */
    public void setTrackingMode(boolean isShow) {
        mMapView.setTrackingMode(isShow);
        if (isShow) {
            mPermissionManager.request(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, new PermissionManager.PermissionListener() {
                @Override
                public void granted() {
                    if (gps != null) {
                        gps.setMinTime(1000);
                        gps.setMinDistance(5);
                        gps.setProvider(gps.GPS_PROVIDER);
                        gps.OpenGps();
                        gps.setProvider(gps.NETWORK_PROVIDER);
                        gps.OpenGps();
                    }
                }

                @Override
                public void denied() {
                    Toast.makeText(MainActivity.this, "위치정보 수신에 동의하지 않으시면 현재위치로 이동할 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
            });

            locationBtn.setBackgroundResource(R.drawable.location_btn_sel);
            mMapView.setCenterPoint(mMapView.getLocationPoint().getLongitude(), mMapView.getLocationPoint().getLatitude());
            mMapView.setSightVisible(true);
        } else {
            if (gps != null) {
                gps.CloseGps();
            }
            locationBtn.setBackgroundResource(R.drawable.location_btn);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 화면 꺼지면 위치정보 수신 종료
        if (gps != null) {
            gps.CloseGps();
        }
        locationBtn.setBackgroundResource(R.drawable.location_btn);
    }

    /**
     * getIsTracking 트래킹모드의 사용여부를 반환한다.
     */
    public void getIsTracking() {
        Boolean bIsTracking = mMapView.getIsTracking();
        Common.showAlertDialog(this, "", "현재 트래킹모드 사용 여부  : " + bIsTracking.toString());
    }

    /**
     * addTMapCircle() 지도에 서클을 추가한다.
     */
    public void addTMapCircle() {
        TMapCircle circle = new TMapCircle();

        circle.setRadius(300);
        circle.setLineColor(Color.BLUE);
        circle.setAreaAlpha(50);
        circle.setCircleWidth((float) 10);
        circle.setRadiusVisible(true);

        TMapPoint point = randomTMapPoint();
        circle.setCenterPoint(point);

        mMapView.setCenterPoint(point.getLongitude(), point.getLatitude());

        String strID = String.format("circle%d", mCircleID++);
        mMapView.addTMapCircle(strID, circle);
        mArrayCircleID.add(strID);
    }

    /**
     * removeTMapCircle 지도상의 해당 서클을 제거한다.
     */
    public void removeTMapCircle() {
        if (mArrayCircleID.size() <= 0)
            return;

        String strCircleID = mArrayCircleID.get(mArrayCircleID.size() - 1);
        mMapView.removeTMapCircle(strCircleID);
        mArrayCircleID.remove(mArrayCircleID.size() - 1);
    }

    public void removeMarker() {
        mArrPoiItem = null;
        mPoiItem = null;
        items = null;
        if (mArrayMarkerID.size() <= 0)
            return;

        String strMarkerID = null;
        for (int i = 0; i < mArrayMarkerID.size(); i++) {
            strMarkerID = mArrayMarkerID.get(i);
            mMapView.removeMarkerItem(strMarkerID);
        }

        mArrayMarkerID.clear();
        mMarkerID = 0;
    }

    /**
     * drawLine 지도에 라인을 추가한다.
     */
    public void drawLine() {
        erasePolyLine();
        TMapPolyLine polyLine = new TMapPolyLine();
        polyLine.setLineColor(Color.BLUE);
        polyLine.setLineWidth(5);

        for (int i = 0; i < 5; i++) {
            TMapPoint point = randomTMapPoint();
            polyLine.addLinePoint(point);
        }

        TMapInfo info = mMapView.getDisplayTMapInfo(polyLine.getLinePoint());
        mMapView.setCenterPoint(info.getTMapPoint().getLongitude(), info.getTMapPoint().getLatitude());

        String strID = String.format("line%d", mLineID++);
        mMapView.addTMapPolyLine(strID, polyLine);
        mArrayLineID.add(strID);
    }

    /**
     * erasePolyLine 지도에 라인을 제거한다.
     */
    public void erasePolyLine() {
        if (mArrayLineID.size() <= 0)
            return;

        String strLineID = mArrayLineID.get(mArrayLineID.size() - 1);
        mMapView.removeTMapPolyLine(strLineID);
        mArrayLineID.remove(mArrayLineID.size() - 1);
    }

    /**
     * drawPolygon 지도에 폴리곤에 그린다.
     */
    public void drawPolygon() {
        int Min = 3;
        int Max = 10;
        int rndNum = (int) (Math.random() * (Max - Min));

        LogManager.printLog("drawPolygon" + rndNum);

        TMapPolygon polygon = new TMapPolygon();
        polygon.setLineColor(Color.BLUE);
        polygon.setAreaColor(Color.RED);
        polygon.setAreaAlpha(50);
        polygon.setPolygonWidth((float) 4);

        TMapPoint point = null;

        if (rndNum < 3) {
            rndNum = rndNum + (3 - rndNum);
        }

        for (int i = 0; i < rndNum; i++) {
            point = randomTMapPoint();
            polygon.addPolygonPoint(point);
        }

        TMapInfo info = mMapView.getDisplayTMapInfo(polygon.getPolygonPoint());
        mMapView.setCenterPoint(info.getTMapPoint().getLongitude(), info.getTMapPoint().getLatitude());

        String strID = String.format("polygon%d", mPolygonID++);
        mMapView.addTMapPolygon(strID, polygon);
        mArrayPolygonID.add(strID);
    }

    /**
     * erasePolygon 지도에 그려진 폴리곤을 제거한다.
     */
    public void removeTMapPolygon() {
        if (mArrayPolygonID.size() <= 0)
            return;

        String strPolygonID = mArrayPolygonID.get(mArrayPolygonID.size() - 1);

        LogManager.printLog("erasePolygon " + strPolygonID);

        mMapView.removeTMapPolygon(strPolygonID);
        mArrayPolygonID.remove(mArrayPolygonID.size() - 1);
    }

    private String getContentFromNode(Element item, String tagName) {
        NodeList list = item.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            if (list.item(0).getFirstChild() != null) {
                return list.item(0).getFirstChild().getNodeValue();
            }
        }
        return null;
    }

    /**
     * removeMapPath 경로 표시를 삭제한다.
     */
    public void removeMapPath() {
        routeLayout.setVisibility(View.GONE);
        mMapView.removeTMapPath();
    }

    public void drawCarPath() {
        findPathDataAllType(TMapPathType.CAR_PATH);
    }

    public void drawPedestrianPath() {
        findPathDataAllType(TMapPathType.PEDESTRIAN_PATH);
    }

    private void findPathDataAllType(final TMapPathType type) {
        totalDistance = null;
        totalTime = null;
        totalFare = null;

        TMapPoint point1 = mMapView.getCenterPoint();
        TMapPoint point2 = null;
        if (type == TMapPathType.CAR_PATH) {
            point2 = randomTMapPoint2();
        } else {
            point2 = randomTMapPoint();
        }
        TMapData tmapdata = new TMapData();

        tmapdata.findPathDataAllType(type, point1, point2, new FindPathDataAllListenerCallback() {
            @Override
            public void onFindPathDataAll(Document doc) {
                TMapPolyLine polyline = new TMapPolyLine();
                polyline.setLineWidth(10);
                if (doc != null) {
                    NodeList list = doc.getElementsByTagName("Document");
                    Element item2 = (Element) list.item(0);
                    totalDistance = getContentFromNode(item2, "tmap:totalDistance");
                    totalTime = getContentFromNode(item2, "tmap:totalTime");
                    if (type == TMapPathType.CAR_PATH) {
                        totalFare = getContentFromNode(item2, "tmap:totalFare");
                    }

                    NodeList list2 = doc.getElementsByTagName("LineString");

                    for (int i = 0; i < list2.getLength(); i++) {
                        Element item = (Element) list2.item(i);
                        String str = getContentFromNode(item, "coordinates");
                        if (str == null) {
                            continue;
                        }

                        String[] str2 = str.split(" ");
                        for (int k = 0; k < str2.length; k++) {
                            try {
                                String[] str3 = str2[k].split(",");
                                TMapPoint point = new TMapPoint(Double.parseDouble(str3[1]), Double.parseDouble(str3[0]));
                                polyline.addLinePoint(point);
                            } catch (Exception e) {

                            }
                        }
                    }

                    TMapInfo info = mMapView.getDisplayTMapInfo(polyline.getLinePoint());
                    int zoom = info.getTMapZoomLevel();
                    if (zoom > 12) {
                        zoom = 12;
                    }

                    mMapView.setZoomLevel(zoom);
                    mMapView.setCenterPoint(info.getTMapPoint().getLongitude(), info.getTMapPoint().getLatitude());

                    mMapView.addTMapPath(polyline);
                    setTextLevel(MESSAGE_STATE_ROUTE);
                }
            }
        });
    }

    /**
     * getCenterPoint 지도의 중심점을 가지고 온다.
     */
    public void getCenterPoint() {
        //16.09.26 KMY [ 화면중심좌표 메뉴 수정 ]
        if (m_bCenterPointMode) {
            centerPointIcon.setVisibility(View.VISIBLE);
            centerPointLayout.setVisibility(View.VISIBLE);
            TMapPoint point = mMapView.getCenterPoint();

            centerLongitude.setText(Double.toString(point.getLongitude()));
            centerLatitude.setText(Double.toString(point.getLatitude()));

        } else {
            centerPointIcon.setVisibility(View.GONE);
            centerPointLayout.setVisibility(View.GONE);
        }
    }

    /**
     * findAllPoi 통합검색 POI를 요청한다.
     */
    public void findAllPoi() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("POI 통합 검색");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                removeMarker();
                findAllPoi(input.getText().toString());
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void findAllPoi(String strData) {
        removeMarker();
        TMapData tmapdata = new TMapData();

        tmapdata.findAllPOI(strData, new FindAllPOIListenerCallback() {
            @Override
            public void onFindAllPOI(ArrayList<TMapPOIItem> poiItem) {
                ArrayList<String> numberList = new ArrayList<String>();
                for (int i = 0; i < poiItem.size(); i++) {
                    TMapPOIItem item = poiItem.get(i);
                    numberList.add(item.getPOIName().toString());

                }

                if (numberList.size() > 0) {
                    mArrPoiItem = poiItem;

                    items = numberList.toArray(new CharSequence[numberList.size()]);
                    setTextLevel(MESSAGE_STATE_POI);
                } else {
                    setTextLevel(MESSAGE_ERROR);
                }
            }
        });
    }

    private void showPOIListAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("POI 통합 검색");

        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                mPoiItem = mArrPoiItem.get(item);

                TMapMarkerItem markerItem = new TMapMarkerItem();

                String strID = String.format("marker%d", mMarkerID++);
                markerItem.setID(strID);
                markerItem.setIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.poi_dot));
                markerItem.setTMapPoint(mPoiItem.getPOIPoint());
                markerItem.setCanShowCallout(true);
                markerItem.setPosition(0.5f, 1.0f);

                mMapView.addMarkerItem(strID, markerItem);
                mArrayMarkerID.add(strID);
                mMapView.setCenterPoint(mPoiItem.getPOIPoint().getLongitude(), mPoiItem.getPOIPoint().getLatitude());
            }
        });
        builder.show();
    }

    private void findPOIDetailInfo(String id, String name) {
        TMapData tmapdata = new TMapData();

        tmapdata.findPOIDetailInfo(id, new FindAllPOIListenerCallback() {
            @Override
            public void onFindAllPOI(ArrayList<TMapPOIItem> poiItem) {
                mPoiItem = poiItem.get(0);

                setTextLevel(MESSAGE_STATE_POI_DETAIL);

            }
        });
    }

    private void showPOIDetailAlert() {
        String info = "";
        if (mPoiItem.getPOIName() != null) {
            info += mPoiItem.getPOIName();
        } else {
            info = "정보없음";
        }
        if (mPoiItem.bizCatName != null) {
            info += "\n" + mPoiItem.bizCatName;
        }
        if (mPoiItem.address != null) {
            info += "\n" + mPoiItem.address + mPoiItem.firstNo + "-" + mPoiItem.secondNo;
        }
        if (mPoiItem.telNo != null) {
            info += "\n" + mPoiItem.telNo;
        }
        if (mPoiItem.zipCode != null) {
            info += "\n" + mPoiItem.zipCode;
        }
        if (mPoiItem.homepageURL != null) {
            info += "\n" + mPoiItem.homepageURL;
        }
        if (mPoiItem.routeInfo != null) {
            info += "\n" + mPoiItem.routeInfo;
        }
        if (mPoiItem.additionalInfo != null) {
            info += "\n" + mPoiItem.additionalInfo;
        }
        if (mPoiItem.desc != null) {
            info += "\n" + mPoiItem.desc;
        }

        Common.showAlertDialog(this, "POI 상세정보", info);
    }

    /**
     * convertToAddress 지도에서 선택한 지점을 주소를 변경요청한다.
     */
    public void reverseGeocoding(boolean show) {
        String strID = "ReverseGeocoding";
        m_bReverseGeoCoding = show;

        oldBAddress = null;
        newAddress = null;

        if (m_bReverseGeoCoding) {
            final TMapPoint point = mMapView.getCenterPoint();

            TMapData tmapdata = new TMapData();

            if (mMapView.isValidTMapPoint(point)) {
                tmapdata.reverseGeocoding(point.getLatitude(), point.getLongitude(), "A10", new reverseGeocodingListenerCallback() {

                    @Override
                    public void onReverseGeocoding(TMapAddressInfo geocodingInfo) {
                        if (geocodingInfo != null) {
                            // 법정동 세팅
                            oldBAddress = "법정동 : ";
                            if (geocodingInfo.strLegalDong != null && !geocodingInfo.strLegalDong.equals("")) {
                                oldBAddress += geocodingInfo.strCity_do + " " + geocodingInfo.strGu_gun + " " + geocodingInfo.strLegalDong;
                                if (geocodingInfo.strRi != null && !geocodingInfo.strRi.equals("")) {
                                    oldBAddress += (" " + geocodingInfo.strRi);
                                }
                                oldBAddress += (" " + geocodingInfo.strBunji);
                            } else {
                                oldBAddress += "-";
                            }

                            // 새주소 세팅
                            newAddress = "도로명 : ";
                            if (geocodingInfo.strRoadName != null && !geocodingInfo.strRoadName.equals("")) {
                                newAddress += geocodingInfo.strCity_do + " " + geocodingInfo.strGu_gun + " " + geocodingInfo.strRoadName + " " + geocodingInfo.strBuildingIndex;
                            } else {
                                newAddress += "-";
                            }

                            // 팝업 띄우기
                            setReverseGeocoding(point);
                        } else {
                            oldBAddress = "";
                            newAddress = "";
                        }
                    }
                });
            }
        } else {
            oldBAddress = null;
            newAddress = null;
            mMapView.setOnClickReverseLabelListener(null);
            mMapView.removeMarkerItem2(strID);
        }
    }

    public void setReverseGeocoding(TMapPoint point) {
        if (oldBAddress != null && newAddress != null) {
            String strID = "ReverseGeocoding";
            MarkerOverlay marker1 = new MarkerOverlay(mContext, oldBAddress, newAddress);

            marker1.setID(strID);
            marker1.setTMapPoint(new TMapPoint(point.getLatitude(), point.getLongitude()));
            mMapView.addMarkerItem2(strID, marker1);
        }
    }

    /**
     * getAroundBizPoi 업종별 주변검색 POI 데이터를 요청한다.
     */
    public void getAroundBizPoi() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("업종별 주변검색 POI");
        builder.setMessage("Ex) 편의점, 약국, 은행 등..");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String strData = input.getText().toString();
                TMapData tmapdata = new TMapData();
                TMapPoint point = mMapView.getCenterPoint();

                tmapdata.findAroundNamePOI(point, strData, 1, 99, new FindAroundNamePOIListenerCallback() {
                    @Override
                    public void onFindAroundNamePOI(ArrayList<TMapPOIItem> poiItem) {
                        removeMarker();
                        if (poiItem != null && poiItem.size() > 0) {
                            String strID = null;

                            ArrayList<TMapPoint> arrPoint = new ArrayList<TMapPoint>();
                            for (int i = 0; i < poiItem.size(); i++) {

                                TMapPOIItem item = poiItem.get(i);
                                /*
                                TMapMarkerItem markerItem = new TMapMarkerItem();

                                strID = String.format("marker%d", mMarkerID++);
                                markerItem.setID(strID);
                                markerItem.setIcon(BitmapFactory.decodeResource(getResources(), R.drawable.poi_dot));
                                markerItem.setTMapPoint(item.getPOIPoint());
                                markerItem.setCalloutTitle(item.getPOIName().toString());
                                markerItem.setCalloutSubTitle(item.getPOIAddress().replace("null", ""));
                                markerItem.setCanShowCallout(true);
                                markerItem.setPosition(0.5f, 1.0f);

                                mMapView.addMarkerItem(strID, markerItem);

                                mArrayMarkerID.add(strID);
                                */

                                arrPoint.add(item.getPOIPoint());


                                mMapView.addTMapPOIItem(poiItem);
                            }

                            TMapInfo info = mMapView.getDisplayTMapInfo(arrPoint);
                            mMapView.setCenterPoint(info.getTMapPoint().getLongitude(), info.getTMapPoint().getLatitude());
                        } else {
                            setTextLevel(MESSAGE_ERROR);
                        }
                    }
                });
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void setTileType() {
        AlertDialog dlg = new AlertDialog.Builder(this).setIcon(R.drawable.tmark).setTitle("Select MAP Tile Type")
                .setSingleChoiceItems(R.array.a_tiletype, m_nCurrentMapType, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        dialog.dismiss();

                        Resources res = getResources();
                        String[] arrTileType = res.getStringArray(R.array.a_tiletype);
                        LogManager.printLog("tile type :" + arrTileType[item]);

                        switch (arrTileType[item]) {
                            case "NORMALTILE":
                                mMapView.setTileType(TMapView.TILETYPE_NORMALTILE);
                                break;
                            case "HDTILE":
                                mMapView.setTileType(TMapView.TILETYPE_HDTILE);
                                break;
                        }

                        m_nCurrentMapType = item;
                    }
                }).show();
    }

    /**
     * 읍면동/도로명 조회
     */
    public void getPoiAreaDataByName() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("읍면동/도로명 조회");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String strData = input.getText().toString();
                TMapData tmapdata = new TMapData();

                tmapdata.findPoiAreaDataByName(10, 1, strData, new FindAllPOIListenerCallback() {
                    @Override
                    public void onFindAllPOI(ArrayList<TMapPOIItem> poiItem) {
                        removeMarker();
                        if (poiItem != null && poiItem.size() > 0) {
                            String strID = null;

                            ArrayList<TMapPoint> arrPoint = new ArrayList<TMapPoint>();
                            for (int i = 0; i < poiItem.size(); i++) {
                                TMapPOIItem item = poiItem.get(i);
                                LogManager.printLog("POI Name: " + item.getPOIName());

                                TMapMarkerItem markerItem = new TMapMarkerItem();

                                strID = String.format("marker%d", mMarkerID++);
                                markerItem.setID(strID);
                                markerItem.setIcon(BitmapFactory.decodeResource(getResources(), R.drawable.poi_dot));
                                markerItem.setTMapPoint(item.getPOIPoint());
                                markerItem.setCalloutTitle(item.getPOIName().toString());
                                markerItem.setCanShowCallout(true);
                                markerItem.setPosition(0.5f, 1.0f);

                                mMapView.addMarkerItem(strID, markerItem);
                                mArrayMarkerID.add(strID);
                                arrPoint.add(item.getPOIPoint());
                            }

                            TMapInfo info = mMapView.getDisplayTMapInfo(arrPoint);
                            mMapView.setCenterPoint(info.getTMapPoint().getLongitude(), info.getTMapPoint().getLatitude());
                            int zoom = info.getTMapZoomLevel();
                            if (zoom > mMapView.getZoomLevel()) {
                                zoom = mMapView.getZoomLevel();
                            }
                            mMapView.setZoomLevel(zoom);
                        } else {
                            setTextLevel(MESSAGE_ERROR);
                        }
                    }
                });
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    // 지오펜싱 다이얼로그 표출 16.06.21
    public void geofencing() {
        final CharSequence regionNames[] = {"시,도 단위", "시,군,구 단위", "법정동", "행정동"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Geofencing").setIcon(R.drawable.tmark);
        final EditText input = new EditText(this);

        builder.setSingleChoiceItems(regionNames, m_nCurrentGeofencingType, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                m_nCurrentGeofencingType = whichButton;
                // 각 리스트를 선택했을때
            }
        }).setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                removeTMapPolygon();
                geofencing(m_nCurrentGeofencingType, input.getText().toString());
            }
        }).setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.setView(input);
        builder.show();
    }

    // 실제 지오펜싱 수행 16.06.21
    public void geofencing(int regionType, String regionName) {
        Geofencer geofencer = new Geofencer(mApiKey);
        geofencer.requestGeofencingBaseData(geofencer.getRegionTypeFromOrder(regionType), regionName, new OnGeofencingBaseDataReceivedCallback() {
            @Override
            public void onReceived(final ArrayList<GeofenceData> datas) {
                if (datas.size() == 1) {
//					Log.d("JSON Test", datas.get(0).getRegionId());
                    // 1개인경우 바로 draw
                    Geofencer geofencer2 = new Geofencer(mApiKey);
                    geofencer2.requestGeofencingPolygon(datas.get(0), MainActivity.this);
                } else if (datas.size() > 1) {
                    // 1개 이상인경우 리스트 표출하여 선택하도록
                    CharSequence[] regionNames = new CharSequence[datas.size()];
                    for (int i = 0; i < datas.size(); i++)
                        regionNames[i] = datas.get(i).getRegionName() + "/" + datas.get(i).getDescription();

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("결과내 선택").setIcon(R.drawable.tmark);

                    builder.setSingleChoiceItems(regionNames, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Geofencer geofencer2 = new Geofencer(mApiKey);
                            geofencer2.requestGeofencingPolygon(datas.get(which), MainActivity.this);
                            dialog.dismiss();
                        }
                    });

                    builder.show();
                } else {
                    Toast.makeText(MainActivity.this, "검색에 실패하였습니다. 확인 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    public void autoComplete() {
        arDessert.clear();
        editText.setText("");
        if (autoCompleteLayout.getVisibility() == View.VISIBLE) {
            autoCompleteLayout.setVisibility(View.GONE);
        } else {
            autoCompleteLayout.setVisibility(View.VISIBLE);
        }
    }

    public void invokeSafeDrive() {
        TMapTapi tmaptapi = new TMapTapi(mContext);
        tmaptapi.invokeSafeDrive();
    }

    public void invokeRoute() {
        final TMapPoint point = mMapView.getCenterPoint();
        TMapData tmapdata = new TMapData();

        if (mMapView.isValidTMapPoint(point)) {
            tmapdata.convertGpsToAddress(point.getLatitude(), point.getLongitude(), new ConvertGPSToAddressListenerCallback() {
                @Override
                public void onConvertToGPSToAddress(String strAddress) {
                    TMapTapi tmaptapi = new TMapTapi(mContext);


                    Log.e("mainactivity", "tt + " + tmaptapi.isTmapApplicationInstalled());

//					float fY = (float) point.getLatitude();
//					float fX = (float) point.getLongitude();
//					tmaptapi.invokeRoute(strAddress, fX, fY);

                    // String url =
                    // "tmap://route?goalx=126.985098&goaly=37.566385&goalname=SKT타워";
                    // Intent intent = new
                    // Intent("android.intent.action.TMAP4_START");
                    // intent.putExtra("url", url);
                    // Tcontext.sendBroadcast(intent);


                    HashMap<String, String> pathInfo = new HashMap<String, String>();
                    pathInfo.put("rStName", "성수동");
                    pathInfo.put("rStY", "37.544523"); //
                    pathInfo.put("rStX", "127.056108");

                    pathInfo.put("rGoName", "자양동");
                    pathInfo.put("rGoX", "127.073858");
                    pathInfo.put("rGoY", "37.536700");//rGoFlag
                    // pathInfo.put("rGoFlag","8");
                    tmaptapi.invokeRoute(pathInfo);

                }
            });
        }
    }

    public void invokeTmap() {
        TMapTapi tmaptapi = new TMapTapi(mContext);
        tmaptapi.invokeTmap();
    }

    public void invokeSetLocation() {
        final TMapPoint point = mMapView.getCenterPoint();
        TMapData tmapdata = new TMapData();

        tmapdata.convertGpsToAddress(point.getLatitude(), point.getLongitude(), new ConvertGPSToAddressListenerCallback() {
            @Override
            public void onConvertToGPSToAddress(String strAddress) {
                TMapTapi tmaptapi = new TMapTapi(mContext);
                float fY = (float) point.getLatitude();
                float fX = (float) point.getLongitude();
                tmaptapi.invokeSetLocation(strAddress, fX, fY);
            }
        });
    }

    public void invokeSearchProtal() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("T MAP 통합 검색");

        final EditText input = new EditText(this);
        input.setHint("검색어 입력");
        builder.setView(input);

        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String strSearch = input.getText().toString();

                new Thread() {
                    @Override
                    public void run() {
                        TMapTapi tmaptapi = new TMapTapi(mContext);
                        if (strSearch.trim().length() > 0)
                            tmaptapi.invokeSearchPortal(strSearch);
                    }
                }.start();
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    public void fullTextGeocoding() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Full Text Geocoding");

        final EditText input = new EditText(this);
        input.setHint("주소 입력");
        builder.setView(input);

        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String strSearch = input.getText().toString();

                new Thread() {
                    @Override
                    public void run() {
                        TMapTapi tmaptapi = new TMapTapi(mContext);
                        if (strSearch.trim().length() > 0) {
                            requestFullAddrGeo(strSearch, new OnCompleteListener() {

                                @Override
                                public void onComplete(FullAddrData fullAddrData) {
                                    // TODO Auto-generated method stub
                                    if (fullAddrData != null) {
                                        drawFullAddrGeo(fullAddrData);
                                    } else {
                                        Toast.makeText(MainActivity.this, "오류가 발생했습니다", Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                        }
                    }
                }.start();
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    // 20170522 JWCha : FullTextGeocoding 기능 추가 - START
    public class FullAddrData {
        public double lon = 0;
        public double lat = 0;
        public double lonEntr = 0;
        public double latEntr = 0;
        public String addr = "";
        public String flag = "";
    }

    public interface OnCompleteListener {
        public void onComplete(FullAddrData fullAddrData);
    }

    public void requestFullAddrGeo(String strAddr, final OnCompleteListener listener) {

        // 지번주소 정확도 순 Flag( 인덱스 작은 수록 정확도 높음 )
        final String[] arrOldMatchFlag = {"M11", "M21", "M12", "M13", "M22", "M23", "M41", "M42", "M31", "M32", "M33"};
        // 도로명주소 정확도 순 Flag( 인덱스 작은 수록 정확도 높음 )
        final String[] arrNewMatchFlag = {"N51", "N52", "N53", "N54", "N55", "N61", "N62"};

        SLHttpRequest request = new SLHttpRequest("https://api2.sktelecom.com/tmap/geo/fullAddrGeo"); // SKT
        request.addParameter("version", "1");
        request.addParameter("appKey", mApiKey);
        request.addParameter("coordType", "WGS84GEO");
        request.addParameter("addressFlag", "F00");
        request.addParameter("fullAddr", strAddr);
        request.send(new SLHttpRequest.OnResponseListener() {

            @Override
            public void OnSuccess(String data) {
                // TODO Auto-generated method stub

                FullAddrData fullAddrData = new FullAddrData();

                // JsonParsing
                try {
                    ArrayList<String> alMatchFlag = new ArrayList<String>(); // MatchFlag 수집
                    int indexMatchFlag = -1;
                    int i, j;

                    JSONObject objData = new JSONObject(data).getJSONObject("coordinateInfo");
                    int length = objData.getInt("totalCount");
                    JSONArray arrCoordinate = objData.getJSONArray("coordinate");
                    JSONObject objCoordinate = null;

                    // 1. matchFlag 수집
                    for (i = 0; i < length; i++) {
                        objCoordinate = arrCoordinate.getJSONObject(i);

                        if (objCoordinate.getString("matchFlag") != null && !objCoordinate.getString("matchFlag").equals("")) {
                            // 지번주소
                            alMatchFlag.add(objCoordinate.getString("matchFlag"));
                        } else if (objCoordinate.getString("newMatchFlag") != null && !objCoordinate.getString("newMatchFlag").equals("")) {
                            // 도로명주소
                            alMatchFlag.add(objCoordinate.getString("newMatchFlag"));
                        }
                    }

                    // 2. < matchFlag 기준으로 더 정확한 항목의 index 결정 >
                    // 2_1. 수집한 matchFlag 중 "M11"(지번주소중 가장 높은정확도) 이 있으면 선택
                    for (i = 0; i < alMatchFlag.size(); i++) {
                        if (alMatchFlag.get(i).equals("M11")) {
                            indexMatchFlag = i;
                            break;
                        }
                    }

                    // 2_2. "M11" 없으면 arrNewMatchFlag(도로명주소) 에서 선택
                    if (indexMatchFlag == -1) {
                        for (i = 0; i < arrNewMatchFlag.length; i++) {
                            for (j = 0; j < alMatchFlag.size(); j++) {
                                if (alMatchFlag.get(j).equals(arrNewMatchFlag[i])) {
                                    indexMatchFlag = j;
                                    break;
                                }
                            }
                            if (indexMatchFlag != -1) {
                                break;
                            }
                        }
                    }
                    // 2_3. 도로명주소 없으면 arrOldMatchFlag(지번주소) 에서 선택
                    if (indexMatchFlag == -1) {
                        for (i = 0; i < arrOldMatchFlag.length; i++) {
                            for (j = 0; j < alMatchFlag.size(); j++) {
                                if (alMatchFlag.get(j).equals(arrOldMatchFlag[i])) {
                                    indexMatchFlag = j;
                                    break;
                                }
                            }
                            if (indexMatchFlag != -1) {
                                break;
                            }
                        }
                    }

                    // 3. 선택된 인덱스의 결과 세팅
                    if (indexMatchFlag != -1) {
                        objCoordinate = arrCoordinate.getJSONObject(indexMatchFlag);
                        if (!objCoordinate.getString("matchFlag").equals("")) {
                            // 지번 주소
                            if (!objCoordinate.getString("lat").equals(""))
                                fullAddrData.lat = Double.parseDouble(objCoordinate.getString("lat"));
                            if (!objCoordinate.getString("lon").equals(""))
                                fullAddrData.lon = Double.parseDouble(objCoordinate.getString("lon"));
                            if (!objCoordinate.getString("latEntr").equals(""))
                                fullAddrData.latEntr = Double.parseDouble(objCoordinate.getString("latEntr"));
                            if (!objCoordinate.getString("lonEntr").equals(""))
                                fullAddrData.lonEntr = Double.parseDouble(objCoordinate.getString("lonEntr"));
                            fullAddrData.addr = objCoordinate.getString("city_do") + " " + objCoordinate.getString("gu_gun") + " " + objCoordinate.getString("eup_myun") + " " + objCoordinate.getString("legalDong") + " " + objCoordinate.getString("ri") + " " + objCoordinate.getString("bunji");
                            fullAddrData.flag = objCoordinate.getString("matchFlag");
                        } else if (!objCoordinate.getString("newMatchFlag").equals("")) {
                            // 도로명 주소
                            if (!objCoordinate.getString("newLat").equals(""))
                                fullAddrData.lat = Double.parseDouble(objCoordinate.getString("newLat"));
                            if (!objCoordinate.getString("newLon").equals(""))
                                fullAddrData.lon = Double.parseDouble(objCoordinate.getString("newLon"));
                            if (!objCoordinate.getString("newLatEntr").equals(""))
                                fullAddrData.latEntr = Double.parseDouble(objCoordinate.getString("newLatEntr"));
                            if (!objCoordinate.getString("newLonEntr").equals(""))
                                fullAddrData.lonEntr = Double.parseDouble(objCoordinate.getString("newLonEntr"));
                            fullAddrData.addr = objCoordinate.getString("city_do") + " " + objCoordinate.getString("gu_gun") + " " + objCoordinate.getString("newRoadName") + " " + objCoordinate.getString("newBuildingIndex") + " " + objCoordinate.getString("newBuildingDong") + " (" + objCoordinate.getString("zipcode") + ")";
                            fullAddrData.flag = objCoordinate.getString("newMatchFlag");
                        }
                    }

                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    Log.d("debug", e.toString());
                }

                listener.onComplete(fullAddrData);
            }

            @Override
            public void OnFail(int errorCode, String errorMessage) {
                // TODO Auto-generated method stub
                Log.d("debug", "errorMessage :" + errorMessage);
                listener.onComplete(null);
            }
        });
    }

    public void drawFullAddrGeo(FullAddrData fullAddrData) {
        String strID = "FullTextGeocoding";

        mMapView.removeAllMarkerItem();

        if (fullAddrData.lat != 0) {
            // 중심좌표 있을 경우
            MarkerOverlay marker1 = new MarkerOverlay(mContext, fullAddrData.addr, (fullAddrData.flag + " : " + getFullAddrGeoFlagInfo(fullAddrData.flag)));
            marker1.setID(strID);
            marker1.setTMapPoint(new TMapPoint(fullAddrData.lat, fullAddrData.lon));
            mMapView.addMarkerItem2(strID, marker1);

            mMapView.setCenterPoint(fullAddrData.lon, fullAddrData.lat, false);
        }

        if (fullAddrData.latEntr != 0) {
            // 입구좌표 있을 경우
            TMapMarkerItem marker2 = new TMapMarkerItem();
            marker2.setTMapPoint(new TMapPoint(fullAddrData.latEntr, fullAddrData.lonEntr));
            marker2.setIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.poi_dot));
            marker2.setCanShowCallout(false);

            mMapView.addMarkerItem(strID + "Entr", marker2);
        }
    }

    public String getFullAddrGeoFlagInfo(String flag) {
        if (flag != null && !flag.equals("")) {
            if (flag.equals("M11")) {
                return "법정동 코드 + 지번이 모두 일치";
            } else if (flag.equals("M12")) {
                return "법정동 코드 + 지번의 주번이 같고 부번이 ±5 이내로 부번과 일치";
            } else if (flag.equals("M13")) {
                return "법정동 코드 + 지번의 주번이 동일하지 않고 ±5 이내로 주번과 일치";
            } else if (flag.equals("M21")) {
                return "행정동 코드 + 지번이 모두 일치";
            } else if (flag.equals("M22")) {
                return "행정동 코드 + 지번의 주번이 같고 부번이 ±5 이내로 부번과 일치";
            } else if (flag.equals("M23")) {
                return "행정동 코드 + 지번의 주번이 동일하지 않고 ±5 이내로 주번과 일치";
            } else if (flag.equals("M31")) {
                return "읍/면/동/리의 중심 매칭";
            } else if (flag.equals("M32")) {
                return "행정동의 중심 매칭";
            } else if (flag.equals("M33")) {
                return "법정동의 중심 매칭";
            } else if (flag.equals("M41")) {
                return "법정동 코드 + 건물명칭이 일치(동일 법정동 내 동일 건물명이 없다는 전제)";
            } else if (flag.equals("M42")) {
                return "법정동 코드 + 건물 동이 매칭";
            } else if (flag.equals("N51")) {
                return "새(도로명) 주소 도로명이 일치하고 건물의 주번/부번이 모두 일치";
            } else if (flag.equals("N52")) {
                return "2차 종속도로에서 주번이 같고 부번이 다름(직전 부번[좌/우 구분]의 끝 좌표를 반환)";
            } else if (flag.equals("N53")) {
                return "1차 종속도로에서 주번이 같고 부번이 다름(직전 부번[좌/구 구분]의 끝 좌표를 반환)";
            } else if (flag.equals("N54")) {
                return "0차 종속도로에서 주번이 같은 것이 없어서 가장 가까운 직전 주번[좌/우 구분]의 가장 끝 좌표";
            } else if (flag.equals("N55")) {
                return "새(도로명) 주소 도로명은 일치하나 주번/부번의 직전 근사값이 없는 경우, 새(도로명) 주소 길 중심 좌표를 반환";
            } else if (flag.equals("N61")) {
                return "새(도로명) 주소 도로명이 틀리나 동일구 내 1개의 건물명과 일치하는 경우, 해당하는 건물 좌표를 반환";
            } else if (flag.equals("N62")) {
                return "새주소 도로명이 틀리나 동일구 내 1개의 건물명과 동명이 일치하는 경우, 해당하는 건물 좌표를 반환";
            } else {
                return "해당 matchFlag 에 대한 설명이 없습니다.";
            }
        } else {
            return "matchFlag 가 비어있습니다.";
        }
    }
    // 20170522 JWCha : FullTextGeocoding 기능 추가 - END


    public void checkTmapApplicationInstalled() {
        TMapTapi tmaptapi = new TMapTapi(mContext);
        boolean isInstalled = tmaptapi.isTmapApplicationInstalled();
        if (isInstalled) {
            Toast.makeText(mContext, "TMap 이 설치되어 있습니다.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(mContext, "TMap 이 설치되어 있지 않습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    public void tmapInstall() {
        new Thread() {
            @Override
            public void run() {
                TMapTapi tmaptapi = new TMapTapi(mContext);
                Uri uri = Uri.parse(tmaptapi.getTMapDownUrl().get(0));
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }

        }.start();
    }

    @Override
    public void onBackPressed() {
        if (isLeftExpanded) {
            menuLeftSlideAnimationToggle();
            return;
        }
        if (autoCompleteLayout.getVisibility() == View.VISIBLE) {
            autoCompleteLayout.setVisibility(View.GONE);
            arDessert.clear();
            editText.setText("");
            hideKeyBoard();
        } else if (autoCompleteV2Layout.getVisibility() == View.VISIBLE) {
            autoCompleteV2Layout.setVisibility(View.GONE);
            autoCompleteV2Adapter.clear();
            autoCompleteV2Edit.setText("");
        } else {
            super.onBackPressed();
        }
    }

    // 지오펜싱 폴리곤 생성 완료 콜백 16.06.22
    @Override
    public void onReceived(ArrayList<TMapPolygon> polygons) {
        if (polygons.size() > 0) {
            double latitudeSum = 0.0;
            double longitudeSum = 0.0;
            int zoomSum = 0;

            mMapView.removeAllTMapPolygon();

            for (int i = 0; i < polygons.size(); i++) {
                mMapView.addTMapPolygon("POLYGON_GEOFENCE" + i, polygons.get(i));
                TMapInfo mapInfo = mMapView.getDisplayTMapInfo(polygons.get(i).getPolygonPoint());
                latitudeSum += mapInfo.getTMapPoint().getLatitude();
                longitudeSum += mapInfo.getTMapPoint().getLongitude();
                zoomSum += mapInfo.getTMapZoomLevel();
            }

            mMapView.setCenterPoint(longitudeSum / polygons.size(), latitudeSum / polygons.size());
            mMapView.setZoomLevel((int) (zoomSum / polygons.size()));
        }
        //Toast.makeText(MainActivity.this, "Polygon " + polygon.getID() + " is drawn.", Toast.LENGTH_SHORT).show();
    }
}
