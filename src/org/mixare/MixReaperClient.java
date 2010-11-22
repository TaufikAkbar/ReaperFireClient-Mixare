package org.mixare;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

 
public class MixReaperClient {
	private ClientConnectionManager clientConnectionManager = null;
	private HttpParams params = null;
	private HttpContext httpContext = null;

	// Set by user, otherwise "Ghost".
	private  String clientId = "Ghost";
	
	// Set by user in client,otherwise VanitySoft.
  	 private String webpageUrl = "www.vanity-soft.com";
 
	 private String serverUrl = "http://173.64.86.5:8143";
 
	 private String username = "mixare";
	 private String password = "04f054fe-ec6a-47d8-b1bf-0354845b4741";
 
	
	private static MixReaperClient mixReaperClient= null;
	
	public static MixReaperClient getInstance(){
		if (  mixReaperClient == null){
			 mixReaperClient = new MixReaperClient();
		}
		return mixReaperClient;
	}

	private MixReaperClient(){
 
		URI url = null;
		try {
			url = new URI(serverUrl);
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
		}
		HttpHost reaperFireServer = new HttpHost(url.getHost(), url.getPort());
		HttpRoute httpRoute = new HttpRoute(reaperFireServer);
		params = new BasicHttpParams();
 
		ConnManagerParams.setMaxTotalConnections(params, 5);
		ConnPerRouteBean connPerRoute = new ConnPerRouteBean(20);
		connPerRoute.setMaxForRoute(httpRoute , 50);
		ConnManagerParams.setMaxConnectionsPerRoute(params, connPerRoute);

		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(
		        new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
 
	   clientConnectionManager = new ThreadSafeClientConnManager(params, schemeRegistry);
		
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(
		    new AuthScope(url.getHost(),url.getPort()), 
		    new UsernamePasswordCredentials(username, password));

		httpContext = new BasicHttpContext();
		httpContext.setAttribute(ClientContext.CREDS_PROVIDER, credsProvider);
	}
	
	public void post(Date time,double bearing,double altitude,double latitude,double longtitude) {
		Date currentDate = new Date(System.currentTimeMillis());
		Log.println(Log.DEBUG, DataView.class.getName(), "Current Date ["
				+ DateFormat.getInstance().format(currentDate) + "]");
	 
		Log.println(Log.DEBUG, DataView.class.getName(),
				"Current Date in Location object ["
						+ DateFormat.getInstance().format(time) + "]");
		
		 
		JSONObject root = new JSONObject();
		JSONArray eventListArray  = new JSONArray();

		try {
			
			JSONObject eventJson = new JSONObject();		
			eventJson.put("dateTime",DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.FULL).format(time));
			eventJson.put("azimuth",  bearing );
			eventJson.put("uuid",  clientId );
			eventJson.put("inclination", altitude );
			eventJson.put("info",  webpageUrl );
			eventJson.put("type", "Point" );
		 					
			JSONArray coordinates = new JSONArray();
				coordinates.put(latitude);
				coordinates.put(longtitude);			
			 eventJson.put("coordinates", coordinates );
			
			eventListArray.put( eventJson );
			
			root.put("eventList",eventListArray);
			 

		} catch (JSONException e1) {
			Log.println(Log.ERROR,MixReaperClient.class.getName(),e1.getMessage());
		}
	 
		HttpClient httpclient = new DefaultHttpClient(clientConnectionManager,
				params);
		HttpPost httpPost = new HttpPost(serverUrl  + "/reaperfire/v1/events.json");
		StringEntity entity = null;
		try {
			entity = new StringEntity(root.toString());

			httpPost.setEntity(entity);

			httpPost.setHeader("Accept", "application/json");
			httpPost.setHeader("Content-type", "application/json");

			HttpResponse response;

			Log.println(Log.DEBUG, DataView.class.getName(),
					"Posting to Reaper Servers" + root.toString() + "...");

			response = httpclient.execute(httpPost, httpContext);
			if (response.getStatusLine().getStatusCode() != 201) {
				Log.println(Log.ERROR, DataView.class.getName(),"did not create" + response
						.getStatusLine().getReasonPhrase()
						+ " code:" + response.getStatusLine().getStatusCode());
			} else {
				Log.println(Log.DEBUG, DataView.class.getName(), "Posted.");
			}

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			httpclient.getConnectionManager().closeExpiredConnections();
		}

	}
	
	/**
	 * Returns Mixare result JSON
	 * 
	 * @param latitude
	 * @param longtitude
	 * @param radiusMiles
	 * @return
	 */
	public String get(double latitude,double longtitude,double radiusMiles){
		
		JSONObject jsonOject = null;
		try {
			jsonOject = new JSONObject("{\"type\": \"Point\", \"coordinates\": ["+Double.toString(latitude)+","+Double.toString(longtitude)+"],\"radius\":\""+Double.toString(radiusMiles)+ "\"}");
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		//Last 30 days of hits only.
		 Calendar calendar = Calendar.getInstance();
		 Date endDate  = (calendar.getTime());
		 calendar.add(Calendar.DATE, -30);
		 Date startDate = calendar.getTime();

		List<NameValuePair> parameters = new ArrayList<NameValuePair>();
		parameters.add(new BasicNameValuePair("startDateTime",DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.FULL).format(startDate)));
		parameters.add(new BasicNameValuePair("endDateTime",DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.FULL).format(endDate)));
		parameters.add(new BasicNameValuePair("pageNumber","1"));
		parameters.add(new BasicNameValuePair("pageSize","50"));
		parameters.add(new BasicNameValuePair("query",jsonOject.toString()));
		URLEncodedUtils.format(parameters,"UTF-8");
		HttpGet httpGet = new HttpGet(serverUrl  + "/reaperfire/v1/search/radius.json?"+URLEncodedUtils.format(parameters,"UTF-8"));
		String content = null;
		HttpClient httpclient = null;
		try {
			HttpResponse response; 
			httpclient = new DefaultHttpClient(clientConnectionManager,params);
		    response = httpclient.execute(httpGet, httpContext);
		    if ( response.getStatusLine().getStatusCode() != 200 ){
		    	 Log.println(Log.ERROR,MixContext.class.getName(), response.getStatusLine().getReasonPhrase() + " code:" +response.getStatusLine().getStatusCode() );
		    	 return statusNotOk();
		    }
		    
		    InputStream inputStream  = response.getEntity().getContent();
		    Writer writer = new StringWriter();
		    char[] buffer = new char[1024];
		    try{
		    	Reader reader = new BufferedReader(new InputStreamReader(inputStream,"UTF-8"));
		    	int n ;
		    	while(( n=reader.read(buffer)) != -1){
		    		writer.write(buffer,0,n);
		    	}
		    }finally{
		    	inputStream.close();
		    }
		    
		    content  = writer.toString();
		    Log.println(Log.DEBUG,MixContext.class.getName(), "Received " + content);
		    
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			  httpclient.getConnectionManager().closeExpiredConnections();
		}
		
		return covertToMix(content);
	}

	private String statusNotOk() {
	 return "{\"status\": \"NOT OK\"}";
		 
	}

	/**
	 * received
	 * 		{
			executionTime(ms): 263
			totalHitCount: 1
			-eventList: [
							-{
								id: "04f054fe-ec6a-47d8-b1bf-0354845b4741"
								uuid: "sample"
								latitude: 38.9955
								longtitude: -77.2884
								dateTime: "Nov 1, 2010 2:14 PM"
								geoDistance: 0
								distance: 5.5689674378856955
								azimuth: 31
								inclination: 234
								llm: 5.5689674378856955
							}
						]
			} 
	 * @param content
	 * @return
	 */
	private String covertToMix(String content) {
	 
		String mixJson = null;
		 try {
			JSONObject reaperJsonObject = new JSONObject(content);
			JSONObject mixJsonObject = new JSONObject();
			mixJsonObject.put("status","OK");
			mixJsonObject.put("num_results", reaperJsonObject.getInt("totalHitCount"));
			
			JSONArray mixJsonResultArray = new JSONArray();
			JSONArray reaperJsonEventArray = reaperJsonObject.getJSONArray("eventList");
			for(int index=0;index< reaperJsonEventArray.length();index++){
				JSONObject event = reaperJsonEventArray.getJSONObject(index);
				JSONObject mixItem = new JSONObject();
				mixItem.put("id", event.get("id"));
				mixItem.put("lat", event.get("latitude"));
				mixItem.put("lng", event.get("longitude"));
				mixItem.put("elevation", event.get("azimuth"));
				
				BigDecimal bd = new BigDecimal(event.getDouble("distance"));
				bd = bd.setScale(2,BigDecimal.ROUND_HALF_UP);
			 
				mixItem.put("distance", event.getDouble("distance") );
			 
				mixItem.put("title", event.get("uuid") + "(" + bd.doubleValue() + "m), "  +  event.get("dateTime") );
				if ( event.isNull("info") ){
					mixItem.put("has_detail_page", 0);
				}else{
					mixItem.put("has_detail_page", 1);
					mixItem.put("webpage", event.get("metadata"));
				}
				mixJsonResultArray.put(mixItem);
			}
			
			mixJsonObject.put("results", mixJsonResultArray);
			
			mixJson  = mixJsonObject.toString();
			 
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		 
		Log.println(Log.DEBUG,MixContext.class.getName(), "MixJSON from Reaper" + mixJson);
		return mixJson;
	}
	
	
	public String getClientId() {
		return clientId;
	}
	public String getWebpageUrl() {
		return webpageUrl;
	}
	public String getUsername() {
		return username;
	}
	public String getPassword() {
		return password;
	}
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	public void setWebpageUrl(String webpageUrl) {
		this.webpageUrl = webpageUrl;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public void setPassword(String password) {
		this.password = password;
	}
}
